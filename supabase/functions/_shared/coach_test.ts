
import { assertEquals, assertStringIncludes } from 'jsr:@std/assert@1';
import { clampReport, coachUserText, isSparse, sanitizeAggregate } from './coach.ts';
import { SCHEMA_COACH } from './prompts.ts';

const week = {
  weekStartEpochDay: 20_000,
  weekEndEpochDay: 20_006,
  loggedDays: 6,
  avgKcal: 2100,
  targetKcal: 2000,
  daysOnTarget: 4,
  avgProteinG: 120,
  avgCarbsG: 210,
  avgFatG: 70,
  weighIns: 3,
  weightStartKg: 80.0,
  weightEndKg: 79.4,
  weightTrendDeltaKg: -0.4,
  workouts: 3,
  workoutVolumeKg: 18_400,
  exerciseKcal: 900,
  fastingSessions: 2,
  fastingAvgHours: 16,
  runs: 1,
  runDistanceKm: 5.2,
  runMinutes: 31,
};

Deno.test('o diário em bruto não passa daqui', () => {
  const dirty = {
    ...week,

    foodNames: ['francesinha', 'cerveja'],
    notes: 'discuti com o meu chefe',
    userId: 'uuid-do-utilizador',
  };

  const clean = sanitizeAggregate(dirty);
  assertEquals(clean.foodNames, undefined);
  assertEquals(clean.notes, undefined);
  assertEquals(clean.userId, undefined);
  assertEquals(clean.avgKcal, 2100);

  const text = coachUserText(dirty, 'pt');
  assertEquals(text.includes('francesinha'), false);
  assertEquals(text.includes('chefe'), false);
  assertEquals(text.includes('uuid-do-utilizador'), false);
});

Deno.test('campos nulos não entopem o prompt', () => {
  const clean = sanitizeAggregate({ ...week, weightTrendDeltaKg: null, weightStartKg: null });
  assertEquals('weightTrendDeltaKg' in clean, false);
  assertEquals('weightStartKg' in clean, false);
  assertEquals(clean.weighIns, 3);
});

Deno.test('semana esparsa é declarada ao modelo', () => {
  assertEquals(isSparse({ loggedDays: 3 }), true);
  assertEquals(isSparse({ loggedDays: 4 }), false);
  assertEquals(isSparse({}), true);

  const text = coachUserText({ ...week, loggedDays: 2 }, 'pt');
  assertStringIncludes(text, 'SPARSE');
  assertStringIncludes(text, '2 logged days');

  assertEquals(coachUserText(week, 'pt').includes('SPARSE'), false);
});

Deno.test('a meta adaptativa vai como facto, não como pergunta', () => {
  const text = coachUserText(week, 'pt', {
    previousTargetKcal: 2000,
    newTargetKcal: 2015,
    observedTdee: 2550,
  });
  assertStringIncludes(text, '2000 -> 2015 kcal/day');
  assertStringIncludes(text, '2550');
  assertStringIncludes(text, 'Do not propose a different number');
});

Deno.test('sem proposta adaptativa não se fala de metas novas', () => {
  const text = coachUserText(week, 'en');
  assertEquals(text.includes('deterministic engine'), false);
  assertStringIncludes(text, 'User language: en');
});

Deno.test('o relatório é cortado a 3 pontos por secção', () => {
  const r = clampReport({
    wins: ['a', 'b', 'c', 'd', 'e'],
    observations: ['a', 'b', 'c', 'd'],
    adjustments: ['a'],
    focus: 'x',
  });
  assertEquals(r.wins.length, 3);
  assertEquals(r.observations.length, 3);
  assertEquals(r.adjustments.length, 1);
  assertEquals(r.focus, 'x');
});

Deno.test('o schema NÃO leva maxItems — a Anthropic rejeita-o com 400', () => {

  const props = SCHEMA_COACH.properties as Record<string, Record<string, unknown>>;
  for (const key of ['wins', 'observations', 'adjustments']) {
    assertEquals('maxItems' in props[key], false, `${key} não pode ter maxItems`);
  }
});

Deno.test('lacunas de micros chegam ao prompt', () => {
  const text = coachUserText({ ...week, microGaps: { iron_mg: 42, vitD_ug: 12 } }, 'pt');
  assertStringIncludes(text, 'iron_mg 42%');
  assertStringIncludes(text, 'do not suggest supplements');
});

Deno.test('chave inventada nao entra no prompt', () => {

  const clean = sanitizeAggregate({ ...week, microGaps: { unobtainium_mg: 10, iron_mg: 40 } });
  assertEquals(clean.microGaps, { iron_mg: 40 });
});

Deno.test('percentagem fora do intervalo e descartada', () => {
  const clean = sanitizeAggregate({ ...week, microGaps: { iron_mg: 900, zinc_mg: -5, vitC_mg: 50 } });
  assertEquals(clean.microGaps, { vitC_mg: 50 });
});

Deno.test('microGaps que nao e objeto nao rebenta', () => {
  for (const bad of ['x', 42, null, ['iron_mg']]) {
    const clean = sanitizeAggregate({ ...week, microGaps: bad });
    assertEquals(clean.microGaps, undefined);
  }
});

Deno.test('sem lacunas o prompt nao fala do assunto', () => {
  const text = coachUserText(week, 'pt');
  assertEquals(text.includes('Micronutrients that came in low'), false);
});
