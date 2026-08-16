
import { createClient } from 'jsr:@supabase/supabase-js@2';

/**
 * Apaga a conta do lado do servidor, a pedido do utilizador. O que a app tem no telemóvel
 * é apagado pelo `PrivacyRepository`; isto trata do que ficou aqui.
 *
 * O que ficou aqui é pouco, e é essa a intenção: a app não sincroniza, e o telemóvel não
 * manda cópias do que se regista. Sobra o que o serviço precisa de saber para funcionar —
 * quantos pedidos à IA foram feitos, e o que foi comprado.
 */
// As tabelas de onde há o que apagar: todas as que guardam alguma coisa **por pessoa**.
//
// As 23 por utilizador que aqui estavam foram largadas na migração 0009 — a app deixou de
// sincronizar, e o que o telemóvel escreve fica no telemóvel. Sobra o que o serviço precisa
// de saber para funcionar: quantos pedidos à IA foram feitos, se o período de experiência já
// foi usado, e o que foi comprado.
//
// **Esta lista esteve errada nos dois sentidos.** Faltavam-lhe a `ai_requests`, a `ai_usage`,
// a `ai_profile` e a `ai_admin`, que têm `user_id` e ficavam para trás. E tinha a
// `purchase_events`, que **não tem** `user_id`: filtrar por uma coluna que não existe devolve
// 400, e a função rebentava antes de chegar a apagar a conta. Quem pedisse o apagamento
// recebia um erro e ficava com tudo.
const TABLES = ['ai_requests', 'ai_usage', 'ai_profile', 'ai_admin', 'entitlements'];

// A `purchase_events` fica de fora de propósito. É o registo em bruto do que o serviço de
// pagamentos enviou, sem ligação à conta da app a não ser dentro do JSON, e é o comprovativo
// de uma transação — não se apaga a pedido pela mesma razão que uma fatura não se apaga.
// Se algum dia precisar de ser apagada, é por uma via própria e não por este laço.

const json = (body: unknown, status: number) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });

Deno.serve(async (req) => {
  const authHeader = req.headers.get('Authorization') ?? '';
  const jwt = authHeader.replace('Bearer ', '').trim();
  if (!jwt) return json({ error: 'missing bearer token' }, 401);

  const url = Deno.env.get('SUPABASE_URL')!;
  const serviceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
  const admin = createClient(url, serviceKey);

  const { data: userData, error: userErr } = await admin.auth.getUser(jwt);
  if (userErr || !userData?.user) return json({ error: 'invalid token' }, 401);
  const userId = userData.user.id;

  try {

    for (const table of TABLES) {
      const { error } = await admin.from(table).delete().eq('user_id', userId);
      if (error) throw new Error(`${table}: ${error.message}`);
    }

    const { error: delErr } = await admin.auth.admin.deleteUser(userId);
    if (delErr) throw new Error(`auth: ${delErr.message}`);

    return json({ deleted: true, userId }, 200);
  } catch (e) {
    return json({ error: String(e instanceof Error ? e.message : e) }, 500);
  }
});
