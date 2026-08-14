
import { createClient } from 'jsr:@supabase/supabase-js@2';

/**
 * Apaga a conta do lado do servidor, a pedido do utilizador. O que a app tem no telemóvel
 * é apagado pelo `PrivacyRepository`; isto trata do que ficou aqui.
 *
 * As tabelas listadas são as das migrações antigas, de quando a app sincronizava. Já não
 * recebem escritas nenhumas — o `NoSyncTest` garante-o —, mas quem tenha usado uma versão
 * anterior pode ter lá linhas, e o direito ao apagamento cobre-as na mesma.
 */
const TABLES = [
  'user_profile', 'weight_log', 'daily_target_override',
  'food', 'food_log', 'water_log',
  'recipe', 'recipe_ingredient',
  'exercise_log', 'exercise',
  'routine', 'routine_item', 'routine_schedule',
  'workout_session', 'workout_set',
  'fasting_protocol', 'fasting_session',
  'run',
  'coach_report',
  'meal_template', 'meal_template_item',
  'body_measurement_log', 'goal_history',
];

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

    for (const table of ['entitlements', 'purchase_events']) {
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
