
import { assertEquals, assertRejects } from 'jsr:@std/assert@1';
import { callClaude, ModelError, MODEL_ANALYSIS } from './claude.ts';
import { clampMet, resolveExercise, type ModelExercise } from './exercise.ts';

const call = {
  model: MODEL_ANALYSIS,
  system: 'sys',
  maxTokens: 100,
  schema: {},
  content: [{ type: 'text' as const, text: 'oi' }],
};

const reply = (text: string, status = 200) =>
  () => Promise.resolve(new Response(JSON.stringify({ content: [{ type: 'text', text }] }), { status }));

Deno.test('devolve o JSON do structured output', async () => {
  const r = await callClaude<{ items: number[] }>(call, reply('{"items":[1,2]}'), 'k');
  assertEquals(r.items, [1, 2]);
});

Deno.test('sem chave → erro DURO (a quota tem de voltar)', async () => {
  const e = await assertRejects(() => callClaude(call, reply('{}'), undefined), ModelError);
  assertEquals((e as ModelError).hard, true);
});

Deno.test('500 da Anthropic → erro duro; 400 nosso → erro mole', async () => {
  const boom = (status: number) => () => Promise.resolve(new Response('boom', { status }));
  const hard = await assertRejects(() => callClaude(call, boom(500), 'k'), ModelError);
  assertEquals((hard as ModelError).hard, true);
  const soft = await assertRejects(() => callClaude(call, boom(400), 'k'), ModelError);
  assertEquals((soft as ModelError).hard, false);

  const rate = await assertRejects(() => callClaude(call, boom(429), 'k'), ModelError);
  assertEquals((rate as ModelError).hard, true);
});

Deno.test('resposta que não é JSON → erro duro (nunca parsear prosa)', async () => {
  const e = await assertRejects(
    () => callClaude(call, reply('Claro! Aqui vão os itens:'), 'k'),
    ModelError,
  );
  assertEquals((e as ModelError).hard, true);
});

Deno.test('a chave nunca vai no corpo, vai no header x-api-key', async () => {
  let seen: RequestInit | undefined;
  await callClaude(call, (_u, init) => {
    seen = init;
    return reply('{}')();
  }, 'sk-ant-secreta');
  const headers = seen!.headers as Record<string, string>;
  assertEquals(headers['x-api-key'], 'sk-ant-secreta');
  assertEquals(String(seen!.body).includes('sk-ant'), false);
});

const ex = (over: Partial<ModelExercise> = {}): ModelExercise => ({
  activity_en: 'running',
  activity_original: 'corrida',
  duration_min: 30,
  distance_km: null,
  met: 9.8,
  confidence: 0.9,
  warnings: [],
  ...over,
});

Deno.test('corrida por distância: ~1,0 × kg × km', () => {
  const r = resolveExercise(ex({ distance_km: 5, duration_min: 30 }), 80);
  assertEquals(r.kcal, 400);
  assertEquals(r.estimated, true);
});

Deno.test('sem distância: MET × kg × horas', () => {
  const r = resolveExercise(ex({ activity_en: 'cycling', met: 6.8, duration_min: 60 }), 80);
  assertEquals(r.kcal, 544);
});

Deno.test('sem duração nem distância → kcal null + NO_DURATION', () => {
  const r = resolveExercise(ex({ duration_min: null, distance_km: null }), 80);
  assertEquals(r.kcal, null);
  assertEquals(r.warnings.includes('NO_DURATION'), true);
});

Deno.test('MET absurdo do modelo é travado', () => {
  assertEquals(clampMet(999), 23);
  assertEquals(clampMet(-4), 1);
  assertEquals(clampMet(Number.NaN), 1);

  assertEquals(resolveExercise(ex({ met: 999, activity_en: 'yoga', duration_min: 60 }), 80).kcal, 1840);
});
