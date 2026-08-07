
import fs from 'node:fs';

const props = Object.fromEntries(
  fs.readFileSync('C:/Users/Betuel/Antares/local.properties', 'utf8')
    .split(/\r?\n/)
    .filter(l => l.includes('=') && !l.startsWith('#'))
    .map(l => { const i = l.indexOf('='); return [l.slice(0, i).trim(), l.slice(i + 1).trim()]; })
);
const URL_ = props.SUPABASE_URL;
const ANON = props.SUPABASE_ANON_KEY;
if (!URL_ || !ANON) { console.error('faltam chaves'); process.exit(1); }

let pass = 0, fail = 0;
const ok = (name, cond, detail = '') => {
  if (cond) { pass++; console.log(`  PASS  ${name}`); }
  else { fail++; console.log(`  FAIL  ${name}  ${detail}`); }
};

async function signInAnon() {
  const r = await fetch(`${URL_}/auth/v1/signup`, {
    method: 'POST',
    headers: { apikey: ANON, 'Content-Type': 'application/json' },
    body: JSON.stringify({}),
  });
  const j = await r.json();
  if (!j.access_token) throw new Error('anon sign-in falhou: ' + JSON.stringify(j).slice(0, 300));
  return { token: j.access_token, uid: j.user.id };
}

const H = (u) => ({
  apikey: ANON,
  Authorization: `Bearer ${u.token}`,
  'Content-Type': 'application/json',
});

async function push(u, table, rows) {
  const r = await fetch(`${URL_}/rest/v1/${table}?on_conflict=user_id,id`, {
    method: 'POST',
    headers: { ...H(u), Prefer: 'resolution=merge-duplicates,return=representation' },
    body: JSON.stringify(rows),
  });
  return { status: r.status, body: await r.text() };
}

async function pull(u, table, cursor = 0, limit = 1000) {
  const q = `updated_at=gt.${cursor}&order=updated_at.asc&limit=${limit}&select=*`;
  const r = await fetch(`${URL_}/rest/v1/${table}?${q}`, { headers: H(u) });
  const t = await r.text();
  return { status: r.status, rows: r.ok ? JSON.parse(t) : [], body: t };
}

const row = (uid, id, updatedAt, deleted, data) => ({
  id, user_id: uid, updated_at: updatedAt, deleted, data,
});

(async () => {
  console.log(`Supabase: ${URL_}\n`);

  console.log('== auth anónima ==');
  const A = await signInAnon();
  const B = await signInAnon();
  ok('utilizador anónimo A criado', !!A.uid);
  ok('utilizador anónimo B criado', !!B.uid && B.uid !== A.uid);
  console.log(`  A=${A.uid.slice(0, 8)}  B=${B.uid.slice(0, 8)}`);

  const T = 'weight_log';
  const base = Date.now();

  console.log('\n== push / pull (8.9) ==');
  let r = await push(A, T, [row(A.uid, 'sync-t1', base + 1000, false, { id: 'sync-t1', weightKg: 80.0, updatedAt: base + 1000 })]);
  ok('A faz upsert de 1 linha', r.status === 201 || r.status === 200, `status=${r.status} ${r.body.slice(0, 200)}`);

  let p = await pull(A, T, 0);
  ok('A recebe a sua linha no pull (cursor 0)', p.rows.length === 1 && p.rows[0].id === 'sync-t1', JSON.stringify(p.body).slice(0, 200));
  ok('payload jsonb volta intacto', p.rows[0]?.data?.weightKg === 80.0);

  console.log('\n== isolamento RLS (8.3) ==');
  p = await pull(B, T, 0);
  ok('B NÃO vê as linhas de A', p.rows.length === 0, `viu ${p.rows.length}`);

  r = await push(B, T, [row(A.uid, 'sync-hack', base + 1000, false, { id: 'sync-hack' })]);
  ok('B NÃO consegue escrever com user_id de A (with_check)', r.status >= 400, `status=${r.status}`);

  r = await fetch(`${URL_}/rest/v1/${T}?id=eq.sync-t1`, { method: 'DELETE', headers: H(A) });
  const delRows = await fetch(`${URL_}/rest/v1/${T}?id=eq.sync-t1&select=id`, { headers: H(A) }).then(x => x.json());
  ok('DELETE não apaga (sem policy de delete → só tombstones)', delRows.length === 1, `restaram ${delRows.length}`);

  r = await push(A, 'entitlements', [{ user_id: A.uid, status: 'ACTIVE', product_id: 'pirata', updated_at: base }]);
  ok('A NÃO consegue escrever entitlements (só service role)', r.status >= 400, `status=${r.status}`);

  const ent = await fetch(`${URL_}/rest/v1/entitlements?user_id=eq.${A.uid}&select=*`, { headers: H(A) });
  const entRows = ent.ok ? await ent.json() : null;
  ok('A LÊ o próprio entitlement (vazio = sem Pro)', ent.status === 200 && entRows.length === 0, `status=${ent.status}`);

  const entB = await fetch(`${URL_}/rest/v1/entitlements?select=*`, { headers: H(B) });
  const entBRows = entB.ok ? await entB.json() : [];
  ok('B não vê entitlements de ninguém', entBRows.length === 0);

  console.log('\n== upsert idempotente + LWW (8.9) ==');
  r = await push(A, T, [row(A.uid, 'sync-t1', base + 1000, false, { id: 'sync-t1', weightKg: 80.0 })]);
  p = await pull(A, T, 0);
  ok('re-push do mesmo id NÃO duplica (on_conflict=user_id,id)', p.rows.length === 1, `linhas=${p.rows.length}`);

  r = await push(A, T, [row(A.uid, 'sync-t1', base + 2000, false, { id: 'sync-t1', weightKg: 79.0 })]);
  p = await pull(A, T, 0);
  ok('escrita mais recente substitui (merge-duplicates)', p.rows[0]?.data?.weightKg === 79.0 && p.rows[0]?.updated_at === base + 2000);

  console.log('\n== tombstone + cursor (8.9) ==');
  await push(A, T, [row(A.uid, 'sync-t1', base + 3000, true, { id: 'sync-t1', weightKg: 79.0, deleted: true })]);
  p = await pull(A, T, 0);
  ok('tombstone propaga no pull (deleted=true)', p.rows[0]?.deleted === true && p.rows[0]?.data?.deleted === true);

  await push(A, T, [row(A.uid, 'sync-t2', base + 4000, false, { id: 'sync-t2', weightKg: 78.0 })]);
  p = await pull(A, T, base + 3000);
  ok('cursor retoma: só traz o que é > cursor', p.rows.length === 1 && p.rows[0].id === 'sync-t2', `trouxe ${p.rows.map(x => x.id)}`);

  p = await pull(A, T, base + 4000);
  ok('cursor no fim: pull vazio (sem repetir)', p.rows.length === 0, `trouxe ${p.rows.length}`);

  p = await pull(A, T, 0);
  ok('ordenação ASC por updated_at', p.rows.map(x => x.updated_at).join() === [base + 3000, base + 4000].join(), p.rows.map(x => x.updated_at).join());

  console.log('\n== tabelas-espelho ==');
  for (const t of [
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

    'body_measurement_log',
  ]) {
    const q = await pull(A, t, 0);
    ok(`tabela "${t}" existe e é legível`, q.status === 200, q.body.slice(0, 120));
  }

  console.log(`\n===== ${pass} passaram, ${fail} falharam =====`);
  process.exit(fail === 0 ? 0 : 1);
})();
