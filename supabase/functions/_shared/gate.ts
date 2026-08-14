
import { createClient, type SupabaseClient } from 'jsr:@supabase/supabase-js@2';

/**
 * O porteiro das funções de AI. Cada chamada custa dinheiro real ao dono, e é aqui que se
 * decide se ela acontece.
 *
 * Há quatro tetos independentes, e não um só, porque protegem de coisas diferentes: o da
 * conta trava o uso normal, o do minuto trava um cliente em ciclo, o do IP trava quem cria
 * contas anónimas em série, e o global trava a fatura do mês inteira.
 */

// Saldo total da experiência — não renova com o dia, ao contrário do limite Pro.
export const TRIAL_LIMIT = 10;
export const PRO_DAILY_LIMIT = 30;
export const RATE_PER_MIN = 10;

// As contas são anónimas e criam-se sozinhas: sem um teto por IP, apagar e reinstalar a
// app dava saldo de experiência infinito.
export const TRIAL_IP_DAILY = 20;

// Travão de último recurso sobre a oferta toda. Atingido, a experiência entra em pausa e
// quem já paga continua a funcionar.
export const GLOBAL_TRIAL_DAILY_DEFAULT = 2_000;

// O dia vem do cliente e serve para contar o uso diário; esta margem aceita qualquer fuso
// do mundo mais um pouco, sem deixar passar uma data inventada para renovar o saldo.
export const DAY_SKEW_MS = 26 * 60 * 60 * 1000;

export type Access =
  | { kind: 'unlimited'; used: number; limit: number }
  | { kind: 'pro'; used: number; limit: number }
  | { kind: 'trial'; used: number; limit: number };

export type GateOk = {
  ok: true;
  userId: string;
  admin: SupabaseClient;
  access: Access;

  // A utilização é cobrada antes de o modelo correr, e devolvida se ele falhar. O
  // contrário — cobrar depois — deixava a porta aberta a pedidos em paralelo que nunca
  // chegavam a ser contados.
  refund: () => Promise<void>;
};

export type GateErr = { ok: false; status: number; error: string; code: string };

export const json = (body: unknown, status: number) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

export function adminClient(): SupabaseClient {
  return createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
    { auth: { persistSession: false } },
  );
}

export function isDayPlausible(day: string, now: number = Date.now()): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) return false;
  const t = Date.parse(`${day}T12:00:00Z`);
  if (Number.isNaN(t)) return false;
  return Math.abs(t - now) <= DAY_SKEW_MS + 12 * 60 * 60 * 1000;
}

export function isProEntitlement(
  row: { status?: string; expires_at?: number | null } | null,
  now: number = Date.now(),
): boolean {
  if (!row) return false;
  const alive = row.status === 'ACTIVE' || row.status === 'GRACE';
  if (!alive) return false;
  if (row.expires_at != null && row.expires_at < now) return false;
  return true;
}

export function clientIp(req: Request): string {
  // O cabeçalho da Cloudflare primeiro, que é o único que o cliente não consegue forjar
  // aqui; `x-forwarded-for` é a alternativa, e dele só o primeiro endereço interessa.
  const cf = req.headers.get('cf-connecting-ip')?.trim();
  if (cf) return cf;
  const fwd = req.headers.get('x-forwarded-for') ?? '';
  return fwd.split(',')[0].trim() || 'unknown';
}

/**
 * O IP nunca é guardado: só o resumo com sal. Chega para contar quantas contas vêm do
 * mesmo sítio e não permite voltar atrás para o endereço.
 */
export async function hashIp(ip: string, salt: string): Promise<string> {
  const data = new TextEncoder().encode(`${salt}:${ip}`);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(digest))
    .slice(0, 16)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

export async function gate(
  req: Request,
  fn: string,
  day: string,

  client?: SupabaseClient,
): Promise<GateOk | GateErr> {
  const jwt = (req.headers.get('Authorization') ?? '').replace('Bearer ', '').trim();
  if (!jwt) return { ok: false, status: 401, error: 'missing token', code: 'NO_AUTH' };

  const admin = client ?? adminClient();
  const { data: userData, error: userErr } = await admin.auth.getUser(jwt);
  const userId = userData?.user?.id;
  if (userErr || !userId) return { ok: false, status: 401, error: 'invalid token', code: 'NO_AUTH' };

  if (!isDayPlausible(day)) {
    return { ok: false, status: 400, error: 'implausible day', code: 'BAD_DAY' };
  }

  const salt = Deno.env.get('AI_IP_SALT') ?? Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? 'antares';
  const ipHash = await hashIp(clientIp(req), salt);

  const { data: ent } = await admin
    .from('entitlements')
    .select('status, expires_at')
    .eq('user_id', userId)
    .maybeSingle();
  const pro = isProEntitlement(ent);

  const { data: adm } = await admin
    .from('ai_admin')
    .select('unlimited')
    .eq('user_id', userId)
    .maybeSingle();
  const unlimited = adm?.unlimited === true;

  const tier = unlimited ? 'unlimited' : (pro ? 'pro' : 'trial');

  const log = (status: string) =>
    admin.from('ai_requests').insert({ user_id: userId, fn, status, ip_hash: ipHash, tier });

  const since = new Date(Date.now() - 60_000).toISOString();
  const { count: recent } = await admin
    .from('ai_requests')
    .select('id', { count: 'exact', head: true })
    .eq('user_id', userId)
    .gte('created_at', since);
  if ((recent ?? 0) >= RATE_PER_MIN) {
    await log('rate');
    return { ok: false, status: 429, error: 'too many requests', code: 'RATE_LIMIT' };
  }

  let access: Access;
  let refund: () => Promise<void>;

  if (unlimited) {

    access = { kind: 'unlimited', used: 0, limit: -1 };
    refund = async () => {};
  } else if (pro) {

    const { data: used, error } = await admin.rpc('ai_usage_increment', {
      p_user: userId,
      p_day: day,
      p_limit: PRO_DAILY_LIMIT,
    });
    if (error) return { ok: false, status: 500, error: error.message, code: 'QUOTA_ERROR' };
    if (used === null || used === -1) {
      await log('quota');
      return { ok: false, status: 429, error: 'daily quota reached', code: 'QUOTA_DAILY' };
    }
    access = { kind: 'pro', used: used as number, limit: PRO_DAILY_LIMIT };
    refund = async () => {
      await admin.rpc('ai_usage_decrement', { p_user: userId, p_day: day });
    };
  } else {

    const dayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();

    const { count: fromIp } = await admin
      .from('ai_requests')
      .select('id', { count: 'exact', head: true })
      .eq('tier', 'trial')
      .eq('status', 'ok')
      .eq('ip_hash', ipHash)
      .gte('created_at', dayAgo);
    if ((fromIp ?? 0) >= TRIAL_IP_DAILY) {
      await log('ip_limit');
      return { ok: false, status: 429, error: 'too many free analyses from here', code: 'IP_LIMIT' };
    }

    const globalLimit = Number(Deno.env.get('AI_GLOBAL_TRIAL_DAILY') ?? GLOBAL_TRIAL_DAILY_DEFAULT);
    const { count: globalToday } = await admin
      .from('ai_requests')
      .select('id', { count: 'exact', head: true })
      .eq('tier', 'trial')
      .eq('status', 'ok')
      .gte('created_at', dayAgo);
    if ((globalToday ?? 0) >= globalLimit) {
      await log('global_limit');

      return { ok: false, status: 503, error: 'free analyses paused', code: 'GLOBAL_LIMIT' };
    }

    const { data: used, error } = await admin.rpc('ai_trial_increment', {
      p_user: userId,
      p_limit: TRIAL_LIMIT,
    });
    if (error) return { ok: false, status: 500, error: error.message, code: 'QUOTA_ERROR' };
    if (used === null || used === -1) {
      await log('quota');
      return { ok: false, status: 402, error: 'trial exhausted', code: 'TRIAL_EXHAUSTED' };
    }
    access = { kind: 'trial', used: used as number, limit: TRIAL_LIMIT };
    refund = async () => {
      await admin.rpc('ai_trial_decrement', { p_user: userId });
    };
  }

  await log('ok');

  if (Math.random() < 0.005) {
    await admin.rpc('ai_requests_prune', { p_days: 7 });
  }

  return { ok: true, userId, admin, access, refund };
}
