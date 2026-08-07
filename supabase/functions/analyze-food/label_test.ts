
import { assertEquals } from 'jsr:@std/assert@1';
import { labelDraft, labelIsCoherent } from './index.ts';

Deno.test('coerência: rótulo normal passa', () => {

  assertEquals(labelIsCoherent(539, 6.3, 57.5, 30.9), true);
});

Deno.test('coerência: dígito mal lido na gordura é apanhado', () => {

  assertEquals(labelIsCoherent(539, 6.3, 57.5, 40.9), true);

  assertEquals(labelIsCoherent(539, 6.3, 57.5, 60.9), false);
});

Deno.test('coerência: sem energia não se julga', () => {
  assertEquals(labelIsCoherent(0, 10, 10, 10), true);
});

Deno.test('rascunho: valores convertidos de por-dose levam aviso', () => {
  const d = labelDraft({
    name_original: 'Cereal',
    serving_g: 55,
    kcal_100: 418,
    protein_100: 7.3,
    carbs_100: 80,
    fat_100: 5.5,
    basis: 'converted_from_serving',
    warnings: [],
  });
  assertEquals(d.warnings.includes('LABEL_CONVERTED'), true);
  assertEquals(d.draft.per100g.kcal, 418);
});

Deno.test('rascunho: incoerência entre kcal e macros é avisada', () => {
  const d = labelDraft({
    kcal_100: 100,
    protein_100: 20,
    carbs_100: 20,
    fat_100: 20,
    warnings: [],
  });
  assertEquals(d.warnings.includes('LABEL_INCONSISTENT'), true);
});

Deno.test('rascunho: sal vira sódio e micros vazios ficam null', () => {
  const d = labelDraft({ kcal_100: 100, salt_g_100: 1.25, micros: {}, warnings: [] });
  assertEquals(d.draft.per100g.sodiumMg, 500);
  assertEquals(d.draft.micros, null);
});

Deno.test('rascunho: micros declarados passam, zeros e nulos não', () => {
  const d = labelDraft({
    kcal_100: 100,
    micros: { calcium_mg: 120, vitC_mg: 0, iron_mg: null },
    warnings: [],
  });
  assertEquals(d.draft.micros, { calcium_mg: 120 });
});

Deno.test('rascunho: sem energia continua a avisar que está incompleto', () => {
  const d = labelDraft({ warnings: [] });
  assertEquals(d.warnings.includes('LABEL_INCOMPLETE'), true);
});
