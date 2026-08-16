
import { type SupabaseClient } from 'jsr:@supabase/supabase-js@2';
import { assertEquals } from 'jsr:@std/assert@1';
import {
  clientIp,
  gate,
  GLOBAL_TRIAL_DAILY_DEFAULT,
  hashIp,
  isDayPlausible,
  isProEntitlement,
  PRO_DAILY_LIMIT,
  TRIAL_IP_DAILY,
  TRIAL_LIMIT,
  type GateErr,
  type GateOk,
} from './gate.ts';

class FakeDb {
  trialUsed = 0;
  dailyCount = 0;
  entitlement: { status: string; expires_at: number | null } | null = null;
  unlimited = false;
  requests: { fn: string; status: string; ip_hash?: string; tier?: string }[] = [];
  recentRequests = 0;
  ipTrialRequests = 0;
  globalTrialRequests = 0;
  pruned = 0;
  user: string | null = 'user-1';

  auth = {
    getUser: (jwt: string) =>
      Promise.resolve(
        this.user && jwt !== 'bad'
          ? { data: { user: { id: this.user } }, error: null }
          : { data: { user: null }, error: { message: 'invalid' } },
      ),
  };

  from(table: string) {
    // O encadeamento devolve sempre o mesmo objeto, e os métodos precisam de chegar aos
    // contadores do duplo. Uma seta guardaria o `this` sozinha, mas partia o encadeamento:
    // o `chain` tem de ser um objeto literal com métodos que devolvem `chain`.
    // deno-lint-ignore no-this-alias
    const self = this;
    const filters: Record<string, unknown> = {};

    const count = () => {
      if (filters.user_id) return self.recentRequests;
      if (filters.ip_hash) return self.ipTrialRequests;
      return self.globalTrialRequests;
    };

    const chain = {
      select: (_c: string, _opts?: { count?: string; head?: boolean }) => chain,
      eq: (col: string, val: unknown) => {
        filters[col] = val;
        return chain;
      },
      gte: () => Promise.resolve({ count: count() }),
      maybeSingle: () =>
        Promise.resolve({
          data: table === 'ai_admin' ? { unlimited: self.unlimited } : self.entitlement,
        }),
      insert: (row: { fn: string; status: string }) => {
        if (table === 'ai_requests') self.requests.push(row);
        return Promise.resolve({ error: null });
      },
    };
    return chain;
  }

  rpc(name: string, args: Record<string, number>) {
    switch (name) {
      case 'ai_trial_increment':
        if (this.trialUsed >= args.p_limit) return Promise.resolve({ data: -1, error: null });
        this.trialUsed++;
        return Promise.resolve({ data: this.trialUsed, error: null });
      case 'ai_trial_decrement':
        this.trialUsed = Math.max(0, this.trialUsed - 1);
        return Promise.resolve({ data: null, error: null });
      case 'ai_usage_increment':
        if (this.dailyCount >= args.p_limit) return Promise.resolve({ data: -1, error: null });
        this.dailyCount++;
        return Promise.resolve({ data: this.dailyCount, error: null });
      case 'ai_usage_decrement':
        this.dailyCount = Math.max(0, this.dailyCount - 1);
        return Promise.resolve({ data: null, error: null });
      case 'ai_requests_prune':
        this.pruned++;
        return Promise.resolve({ data: 0, error: null });
    }
    return Promise.resolve({ data: null, error: { message: 'rpc?' } });
  }
}

const today = () => new Date().toISOString().slice(0, 10);
const req = (token = 'jwt', ip = '198.51.100.7') =>
  new Request('http://x', {
    headers: { Authorization: `Bearer ${token}`, 'x-forwarded-for': ip },
  });

const run = (db: FakeDb, day = today()) => gate(req(), 'analyze-food', day, db as unknown as SupabaseClient);

Deno.test('sem token → 401', async () => {
  const db = new FakeDb();

  const r = (await gate(new Request('http://x'), 'analyze-food', today(), db as unknown as SupabaseClient)) as GateErr;
  assertEquals(r.ok, false);
  assertEquals(r.status, 401);
});

Deno.test('token inválido → 401 (não gasta quota)', async () => {
  const db = new FakeDb();

  const r = (await gate(req('bad'), 'analyze-food', today(), db as unknown as SupabaseClient)) as GateErr;
  assertEquals(r.status, 401);
  assertEquals(db.trialUsed, 0);
});

Deno.test('trial esgota exatamente aos 10', async () => {
  const db = new FakeDb();
  for (let i = 1; i <= TRIAL_LIMIT; i++) {
    const r = (await run(db)) as GateOk;
    assertEquals(r.ok, true);
    assertEquals(r.access.kind, 'trial');
    assertEquals(r.access.used, i);
  }
  const r = (await run(db)) as GateErr;
  assertEquals(r.ok, false);
  assertEquals(r.status, 402);
  assertEquals(r.code, 'TRIAL_EXHAUSTED');
  assertEquals(db.trialUsed, TRIAL_LIMIT);
});

Deno.test('Pro tem 30/dia (e não gasta o trial)', async () => {
  const db = new FakeDb();
  db.entitlement = { status: 'ACTIVE', expires_at: null };
  for (let i = 1; i <= PRO_DAILY_LIMIT; i++) {
    const r = (await run(db)) as GateOk;
    assertEquals(r.access.kind, 'pro');
    assertEquals(r.access.used, i);
  }
  const r = (await run(db)) as GateErr;
  assertEquals(r.status, 429);
  assertEquals(r.code, 'QUOTA_DAILY');
  assertEquals(db.trialUsed, 0);
});

Deno.test('admin ilimitado: passa sem gastar quota nem trial (F5)', async () => {
  const db = new FakeDb();
  db.unlimited = true;
  db.dailyCount = PRO_DAILY_LIMIT;
  db.ipTrialRequests = TRIAL_IP_DAILY;
  for (let i = 0; i < 50; i++) {
    const r = (await run(db)) as GateOk;
    assertEquals(r.ok, true);
    assertEquals(r.access.kind, 'unlimited');
  }

  assertEquals(db.dailyCount, PRO_DAILY_LIMIT);
  assertEquals(db.trialUsed, 0);
});

Deno.test('quota do dia seguinte reseta (contador é por dia)', async () => {
  const db = new FakeDb();
  db.entitlement = { status: 'ACTIVE', expires_at: null };
  db.dailyCount = PRO_DAILY_LIMIT;
  assertEquals(((await run(db)) as GateErr).code, 'QUOTA_DAILY');
  db.dailyCount = 0;
  assertEquals(((await run(db)) as GateOk).ok, true);
});

Deno.test('GRACE ainda é Pro; EXPIRED cai para trial', async () => {
  const grace = new FakeDb();
  grace.entitlement = { status: 'GRACE', expires_at: null };
  assertEquals(((await run(grace)) as GateOk).access.kind, 'pro');

  const expired = new FakeDb();
  expired.entitlement = { status: 'EXPIRED', expires_at: null };
  assertEquals(((await run(expired)) as GateOk).access.kind, 'trial');
});

Deno.test('rate limit: 10/min → 429 sem gastar quota', async () => {
  const db = new FakeDb();
  db.recentRequests = 10;
  const r = (await run(db)) as GateErr;
  assertEquals(r.status, 429);
  assertEquals(r.code, 'RATE_LIMIT');
  assertEquals(db.trialUsed, 0);
});

Deno.test('refund devolve a unidade (falha dura do modelo)', async () => {
  const db = new FakeDb();
  const r = (await run(db)) as GateOk;
  assertEquals(db.trialUsed, 1);
  await r.refund();
  assertEquals(db.trialUsed, 0);
});

Deno.test('day implausível → 400 (senão dava quota infinita)', async () => {
  const db = new FakeDb();
  const r = (await run(db, '2019-01-01')) as GateErr;
  assertEquals(r.status, 400);
  assertEquals(r.code, 'BAD_DAY');
  assertEquals(db.trialUsed, 0);
});

Deno.test('isDayPlausible aceita ontem/hoje/amanhã e recusa lixo', () => {
  const d = (offsetDays: number) =>
    new Date(Date.now() + offsetDays * 86_400_000).toISOString().slice(0, 10);
  assertEquals(isDayPlausible(d(0)), true);
  assertEquals(isDayPlausible(d(-1)), true);
  assertEquals(isDayPlausible(d(1)), true);
  assertEquals(isDayPlausible(d(5)), false);
  assertEquals(isDayPlausible('ontem'), false);
  assertEquals(isDayPlausible('2026-13-45'), false);
});

Deno.test('teto por IP: 20 análises de oferta por dia → 429 IP_LIMIT', async () => {
  const db = new FakeDb();
  db.ipTrialRequests = TRIAL_IP_DAILY;
  const r = (await run(db)) as GateErr;
  assertEquals(r.status, 429);
  assertEquals(r.code, 'IP_LIMIT');
  assertEquals(db.trialUsed, 0);
});

Deno.test('teto por IP não se aplica a Pro (já pagou)', async () => {
  const db = new FakeDb();
  db.entitlement = { status: 'ACTIVE', expires_at: null };
  db.ipTrialRequests = 10_000;
  db.globalTrialRequests = 10_000;
  const r = (await run(db)) as GateOk;
  assertEquals(r.ok, true);
  assertEquals(r.access.kind, 'pro');
});

Deno.test('teto global: oferta em pausa → 503 GLOBAL_LIMIT', async () => {
  const db = new FakeDb();
  db.globalTrialRequests = GLOBAL_TRIAL_DAILY_DEFAULT;
  const r = (await run(db)) as GateErr;

  assertEquals(r.status, 503);
  assertEquals(r.code, 'GLOBAL_LIMIT');
  assertEquals(db.trialUsed, 0);
});

Deno.test('abaixo dos tetos, a oferta passa na mesma', async () => {
  const db = new FakeDb();
  db.ipTrialRequests = TRIAL_IP_DAILY - 1;
  db.globalTrialRequests = GLOBAL_TRIAL_DAILY_DEFAULT - 1;
  const r = (await run(db)) as GateOk;
  assertEquals(r.ok, true);
  assertEquals(r.access.kind, 'trial');
});

Deno.test('cada linha de telemetria leva tier e hash de IP (nunca o IP)', async () => {
  const db = new FakeDb();
  await run(db);
  const row = db.requests.at(-1)!;
  assertEquals(row.status, 'ok');
  assertEquals(row.tier, 'trial');
  assertEquals(row.ip_hash?.length, 32);
  assertEquals(JSON.stringify(db.requests).includes('198.51.100.7'), false);
});

Deno.test('clientIp: cf-connecting-ip manda (o x-forwarded-for é o que o cliente diz)', () => {
  const r = new Request('http://x', {
    headers: { 'x-forwarded-for': '1.2.3.4', 'cf-connecting-ip': '9.9.9.9' },
  });
  assertEquals(clientIp(r), '9.9.9.9');
  const only = new Request('http://x', { headers: { 'x-forwarded-for': '1.2.3.4, 5.6.7.8' } });
  assertEquals(clientIp(only), '1.2.3.4');
});

Deno.test('hashIp: mesmo IP → mesmo hash; salt diferente → hash diferente', async () => {
  const a = await hashIp('1.2.3.4', 's1');
  const b = await hashIp('1.2.3.4', 's1');
  const c = await hashIp('1.2.3.5', 's1');
  const d = await hashIp('1.2.3.4', 's2');
  assertEquals(a, b);
  assertEquals(a === c, false);
  assertEquals(a === d, false);
});

Deno.test('isProEntitlement respeita expires_at', () => {
  const now = 1_000_000;
  assertEquals(isProEntitlement(null, now), false);
  assertEquals(isProEntitlement({ status: 'ACTIVE', expires_at: null }, now), true);
  assertEquals(isProEntitlement({ status: 'ACTIVE', expires_at: now + 1 }, now), true);
  assertEquals(isProEntitlement({ status: 'ACTIVE', expires_at: now - 1 }, now), false);
  assertEquals(isProEntitlement({ status: 'GRACE', expires_at: now + 1 }, now), true);
  assertEquals(isProEntitlement({ status: 'EXPIRED', expires_at: null }, now), false);
});
