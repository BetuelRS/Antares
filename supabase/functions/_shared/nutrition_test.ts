
import { assertEquals } from 'jsr:@std/assert@1';
import {
  hasConsistentMacros,
  isSane,
  iuToMicrograms,
  looksLikeInternationalUnits,
  normalizeKey,
  offToPer100g,
  OFF_USER_AGENT,
  resolveItem,
  saltToSodiumMg,
  scale,
  usdaToPer100g,
  type ModelItem,
  type Per100g,
  type Sources,
} from './nutrition.ts';

const item = (over: Partial<ModelItem> = {}): ModelItem => ({
  name_en: 'grilled chicken breast',
  name_original: 'peito de frango grelhado',
  grams: 150,
  expected_kcal_per_100g: 165,

  expected_protein_per_100g: 31,
  expected_carbs_per_100g: 0,
  expected_fat_per_100g: 3.6,
  confidence: 0.9,
  ...over,
});

function sources(opts: {
  cache?: Per100g | null;
  usda?: unknown;
  off?: unknown;
  usdaOk?: boolean;
  offOk?: boolean;
  usdaKey?: string | null;
}): Sources & { puts: { key: string; source: string }[] } {
  const puts: { key: string; source: string }[] = [];
  return {
    puts,
    usdaKey: opts.usdaKey === undefined ? 'fake-key' : opts.usdaKey,
    cacheGet: () => Promise.resolve(opts.cache ?? null),
    cachePut: (key, _n, source) => {
      puts.push({ key, source });
      return Promise.resolve();
    },
    fetcher: (url: string) => {
      const isUsda = url.includes('nal.usda.gov');
      const ok = isUsda ? (opts.usdaOk ?? true) : (opts.offOk ?? true);
      const body = isUsda ? opts.usda : opts.off;
      return Promise.resolve(
        new Response(JSON.stringify(body ?? {}), { status: ok ? 200 : 503 }),
      );
    },
  };
}

const usdaFood = (kcal: number) => ({
  foods: [{
    foodNutrients: [
      { nutrientId: 1008, value: kcal },
      { nutrientId: 1003, value: 31 },
      { nutrientId: 1005, value: 0 },
      { nutrientId: 1004, value: 3.6 },
    ],
  }],
});

Deno.test('normalizeKey: minúsculas, sem acentos, sem plural, sem pontuação', () => {
  assertEquals(normalizeKey('Chicken Breast'), 'chicken breast');
  assertEquals(normalizeKey('chicken breasts'), 'chicken breast');
  assertEquals(normalizeKey('Crème Brûlée'), 'creme brulee');
  assertEquals(normalizeKey('berries'), 'berry');
  assertEquals(normalizeKey('  olive oil,  extra-virgin '), 'olive oil extra virgin');

  assertEquals(normalizeKey('Eggs'), normalizeKey('egg'));
});

Deno.test('isSane: ±40% das kcal esperadas', () => {
  assertEquals(isSane(165, 165), true);
  assertEquals(isSane(230, 165), true);
  assertEquals(isSane(240, 165), false);
  assertEquals(isSane(100, 165), true);
  assertEquals(isSane(95, 165), false);
  assertEquals(isSane(165, 0), true);
});

Deno.test('scale: por-100g → gramas reais', () => {
  const r = scale({ kcal: 165, protein: 31, carbs: 0, fat: 3.6 }, 150);
  assertEquals(r.kcal, 248);
  assertEquals(r.protein, 46.5);
  assertEquals(r.fat, 5.4);
});

Deno.test('saltToSodiumMg: 1,25 g de sal = 500 mg de sódio', () => {
  assertEquals(saltToSodiumMg(1.25), 500);
});

Deno.test('pipeline: cache primeiro (nem toca na rede)', async () => {
  let calls = 0;
  const s = sources({ cache: { kcal: 165, protein: 31, carbs: 0, fat: 3.6 } });
  const spy: Sources = { ...s, fetcher: (u, i) => { calls++; return s.fetcher(u, i); } };
  const r = await resolveItem(item(), spy);
  assertEquals(r.matchedSource, 'CACHE');
  assertEquals(r.estimated, false);
  assertEquals(r.kcal, 248);
  assertEquals(calls, 0);
});

Deno.test('assumption do modelo passa até ao item resolvido (0.9.7)', async () => {
  const s = sources({ cache: { kcal: 165, protein: 31, carbs: 0, fat: 3.6 } });
  const r = await resolveItem(item({ assumption: 'assumi 1 unidade ≈ 120 g' }), s);
  assertEquals(r.assumption, 'assumi 1 unidade ≈ 120 g');

  const r2 = await resolveItem(item(), s);
  assertEquals(r2.assumption, null);
});

Deno.test('pipeline: USDA quando não há cache (e grava no cache)', async () => {
  const s = sources({ usda: usdaFood(165) });
  const r = await resolveItem(item(), s);
  assertEquals(r.matchedSource, 'USDA');
  assertEquals(r.estimated, false);
  assertEquals(r.kcal, 248);
  assertEquals(s.puts, [{ key: 'grilled chicken breast', source: 'USDA' }]);
});

Deno.test('sanidade: candidato USDA fora dos ±40% é rejeitado → cai para OFF', async () => {
  const s = sources({
    usda: usdaFood(400),
    off: { products: [{ nutriments: { 'energy-kcal_100g': 170, proteins_100g: 30, carbohydrates_100g: 1, fat_100g: 4 } }] },
  });
  const r = await resolveItem(item(), s);
  assertEquals(r.matchedSource, 'OFF');
  assertEquals(r.kcal, 255);
});

Deno.test('pipeline: tudo em baixo → estimativa do modelo com estimated=true', async () => {
  const s = sources({ usdaOk: false, offOk: false });
  const r = await resolveItem(item(), s);
  assertEquals(r.matchedSource, 'MODEL');
  assertEquals(r.estimated, true);
  assertEquals(r.kcal, 248);
  assertEquals(s.puts.length, 0);
});

Deno.test('estimativa usa os macros DO ALIMENTO, não uma repartição fixa', async () => {

  const eggs = item({
    name_en: 'scrambled eggs',
    name_original: 'ovos mexidos',
    grams: 100,
    expected_kcal_per_100g: 155,
    expected_protein_per_100g: 10.5,
    expected_carbs_per_100g: 1.6,
    expected_fat_per_100g: 11.5,
  });
  const r = await resolveItem(eggs, sources({ usdaOk: false, offOk: false }));
  assertEquals(r.matchedSource, 'MODEL');
  assertEquals(r.protein, 10.5);
  assertEquals(r.carbs, 1.6);
  assertEquals(r.fat, 11.5);
});

Deno.test('OFF leva User-Agent — sem ele responde 503 a datacenters', async () => {

  let seenUa: string | undefined;
  const s: Sources = {
    ...sources({ off: { products: [{ nutriments: { 'energy-kcal_100g': 165, proteins_100g: 31, carbohydrates_100g: 0, fat_100g: 3.6 } }] }, usdaKey: null }),
    fetcher: (url, init) => {
      seenUa = (init?.headers as Record<string, string> | undefined)?.['User-Agent'];
      return Promise.resolve(
        new Response(
          JSON.stringify({ products: [{ nutriments: { 'energy-kcal_100g': 165, proteins_100g: 31, carbohydrates_100g: 0, fat_100g: 3.6 } }] }),
          { status: 200 },
        ),
      );
    },
  };
  const r = await resolveItem(item(), s);
  assertEquals(r.matchedSource, 'OFF');
  assertEquals(seenUa, OFF_USER_AGENT);
  assertEquals(seenUa?.includes('Antares'), true);
});

Deno.test('sem chave USDA salta direto para OFF (não rebenta)', async () => {
  const s = sources({
    usdaKey: null,
    off: { products: [{ nutriments: { 'energy-kcal_100g': 165, proteins_100g: 31, carbohydrates_100g: 0, fat_100g: 3.6 } }] },
  });
  const r = await resolveItem(item(), s);
  assertEquals(r.matchedSource, 'OFF');
});

Deno.test('cache com ficha partida é ignorado (não se serve lixo só por estar em cache)', async () => {
  const s = sources({
    cache: { kcal: 185, protein: 0, carbs: 0, fat: 0 },
    usdaKey: null,
    off: { products: [{ nutriments: { 'energy-kcal_100g': 165, proteins_100g: 31, carbohydrates_100g: 0, fat_100g: 3.6 } }] },
  });
  const r = await resolveItem(item(), s);
  assertEquals(r.matchedSource, 'OFF');

  assertEquals(s.puts, [{ key: 'grilled chicken breast', source: 'OFF' }]);
});

Deno.test('ficha vazia é rejeitada; alimento normal passa', () => {
  const frango = { kcal: 165, protein: 31, carbs: 0, fat: 3.6 };
  assertEquals(hasConsistentMacros(frango, frango), true);
  assertEquals(hasConsistentMacros({ kcal: 0, protein: 0, carbs: 0, fat: 0 }), false);

  assertEquals(hasConsistentMacros({ kcal: 100, protein: 20, carbs: 20, fat: 10 }), false);
});

Deno.test('bebidas: cerveja a zero é ficha partida, vodka a zero é correta', () => {

  const cervejaEsperada = { kcal: 43, protein: 0.5, carbs: 3.6, fat: 0 };
  const vodkaEsperada = { kcal: 231, protein: 0, carbs: 0, fat: 0 };

  assertEquals(hasConsistentMacros(cervejaEsperada, cervejaEsperada), true);

  assertEquals(
    hasConsistentMacros({ kcal: 56, protein: 0, carbs: 0, fat: 0 }, cervejaEsperada),
    false,
  );

  assertEquals(
    hasConsistentMacros({ kcal: 231, protein: 0, carbs: 0, fat: 0 }, vodkaEsperada),
    true,
  );
});

Deno.test('candidato com macros a zero é saltado → cai na estimativa do modelo', async () => {
  const s = sources({
    usdaKey: null,
    off: { products: [{ nutriments: { 'energy-kcal_100g': 165, proteins_100g: 0, carbohydrates_100g: 0, fat_100g: 0 } }] },
  });
  const r = await resolveItem(item(), s);

  assertEquals(r.matchedSource, 'MODEL');
});

Deno.test('parsers ignoram respostas sem energia', () => {
  assertEquals(usdaToPer100g({ foodNutrients: [{ nutrientId: 1003, value: 10 }] }), null);
  assertEquals(offToPer100g({ nutriments: { proteins_100g: 10 } }), null);
});

function ingredientSources(table: Record<string, { kcal: number; micros?: Record<string, number> }>): Sources {
  return {
    usdaKey: 'fake-key',
    cacheGet: () => Promise.resolve(null),
    cachePut: () => Promise.resolve(),
    fetcher: (url: string) => {
      const q = decodeURIComponent(url).toLowerCase();
      const hit = Object.entries(table).find(([name]) => q.includes(name));
      if (!hit || !url.includes('nal.usda.gov')) {
        return Promise.resolve(new Response(JSON.stringify({ foods: [], products: [] }), { status: 200 }));
      }
      const [, v] = hit;

      const nutrients = [
        { nutrientId: 1008, value: v.kcal },
        { nutrientId: 1003, value: (v.kcal * 0.2) / 4 },
        { nutrientId: 1005, value: (v.kcal * 0.5) / 4 },
        { nutrientId: 1004, value: (v.kcal * 0.3) / 9 },
        ...Object.entries(v.micros ?? {}).map(([k, val]) => ({
          nutrientId: k === 'calcium' ? 1087 : 1178,
          value: val,
        })),
      ];
      return Promise.resolve(new Response(JSON.stringify({ foods: [{ foodNutrients: nutrients }] }), { status: 200 }));
    },
  };
}

const dish = (over: Partial<ModelItem> = {}): ModelItem => ({
  name_en: 'portuguese boiled dinner',
  name_original: 'cozido à portuguesa',
  grams: 400,
  expected_kcal_per_100g: 150,
  expected_protein_per_100g: 10,
  expected_carbs_per_100g: 10,
  expected_fat_per_100g: 5,
  confidence: 0.7,
  components: [
    { name_en: 'beef', grams: 150 },
    { name_en: 'potato', grams: 150 },
    { name_en: 'cabbage', grams: 100 },
  ],
  ...over,
});

Deno.test('receita: soma ingredientes reais e traz micros verdadeiros', async () => {
  const s = ingredientSources({
    beef: { kcal: 200, micros: { b12: 2 } },
    potato: { kcal: 80, micros: { calcium: 10 } },
    cabbage: { kcal: 25, micros: { calcium: 40 } },
  });
  const r = await resolveItem(dish(), s);

  assertEquals(r.matchedSource, 'RECIPE');

  assertEquals(r.kcal, 445);

  assertEquals(r.micros?.calcium_mg, 55);

  assertEquals(r.estimated, true);
});

Deno.test('receita: um só ingrediente encontrado não chega', async () => {

  const s = ingredientSources({ beef: { kcal: 200 } });
  const r = await resolveItem(dish(), s);
  assertEquals(r.matchedSource, 'MODEL');
});

Deno.test('receita: ingredientes que cobrem pouco peso são rejeitados', async () => {

  const s = ingredientSources({ olive: { kcal: 884 }, salt: { kcal: 0 } });
  const r = await resolveItem(
    dish({ components: [{ name_en: 'olive oil', grams: 30 }, { name_en: 'salt', grams: 30 }] }),
    s,
  );
  assertEquals(r.matchedSource, 'MODEL');
});

Deno.test('receita: alimento que a base conhece direto NÃO passa por ingredientes', async () => {
  const s = sources({ usda: usdaFood(165) });
  const r = await resolveItem(item({ components: [{ name_en: 'chicken', grams: 150 }] }), s);
  assertEquals(r.matchedSource, 'USDA');
});

Deno.test('vitamina D em UI converte-se para microgramas', () => {

  assertEquals(iuToMicrograms('vitD_ug', 400), 10);
});

Deno.test('vitamina A em UI converte-se para microgramas RAE', () => {
  const ug = iuToMicrograms('vitA_ug', 3330)!;
  assertEquals(Math.round(ug), 1000);
});

Deno.test('um nutriente sem fator de UI devolve null', () => {
  assertEquals(iuToMicrograms('calcium_mg', 400), null);
});

Deno.test('400 de vitamina D por 100 g cheira a UI', () => {
  assertEquals(looksLikeInternationalUnits('vitD_ug', 400), true);
});

Deno.test('um valor plausivel em microgramas nao e confundido com UI', () => {

  assertEquals(looksLikeInternationalUnits('vitD_ug', 12), false);
  assertEquals(looksLikeInternationalUnits('vitA_ug', 900), false);
});
