
import { assertEquals } from 'jsr:@std/assert@1';
import { cleanMicros, factorForKey, offMicros, usdaMicros } from './nutrients.ts';

Deno.test('usdaMicros: lê os micros que vinham na mesma resposta', () => {
  const nutrients = [
    { nutrientId: 1008, value: 165 },
    { nutrientId: 1087, value: 15 },
    { nutrientId: 1178, value: 0.34 },
    { nutrientId: 1100, value: 7 },
    { nutrientId: 9999, value: 42 },
  ];
  const m = usdaMicros(nutrients)!;
  assertEquals(m.calcium_mg, 15);
  assertEquals(m.vitB12_ug, 0.34);
  assertEquals(m.iodine_ug, 7);
  assertEquals(m.kcal, undefined);
  assertEquals(m['9999'], undefined);
});

Deno.test('usdaMicros: zeros e ausências não entram', () => {

  const m = usdaMicros([{ nutrientId: 1087, value: 0 }, { nutrientId: 1089, value: null }]);
  assertEquals(m, undefined);
});

Deno.test('offMicros: converte gramas para a unidade da chave', () => {

  const n = {
    'calcium_100g': 0.12,
    'vitamin-c_100g': 0.03,
    'vitamin-b12_100g': 0.0000004,
    'iodine_100g': 0.00002,
  };
  const m = offMicros(n)!;
  assertEquals(m.calcium_mg, 120);
  assertEquals(m.vitC_mg, 30);
  assertEquals(m.vitB12_ug, 0.4);
  assertEquals(m.iodine_ug, 20);
});

Deno.test('offMicros: ficha sem micros devolve undefined, não objeto vazio', () => {
  assertEquals(offMicros({ proteins_100g: 12 }), undefined);
  assertEquals(offMicros(null), undefined);
});

Deno.test('factorForKey: a unidade vem do sufixo', () => {
  assertEquals(factorForKey('calcium_mg'), 1_000);
  assertEquals(factorForKey('vitB12_ug'), 1_000_000);
  assertEquals(factorForKey('seja_o_que_for'), 1);
});

Deno.test('cleanMicros: descarta não-finitos e negativos', () => {
  const m = cleanMicros({ a_mg: NaN, b_mg: -1, c_mg: Infinity, d_mg: 2.5 })!;
  assertEquals(m, { d_mg: 2.5 });
});
