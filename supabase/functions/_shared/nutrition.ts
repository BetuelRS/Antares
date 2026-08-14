
import { offMicros, USDA_MACRO_IDS, usdaMicros } from './nutrients.ts';

export type Macros = {
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
};

export type Per100g = Macros & {
  sugars?: number;
  satFat?: number;
  fiber?: number;
  sodiumMg?: number;
  micros?: Record<string, number>;
};

export type ModelItem = {
  name_en: string;
  name_original: string;
  grams: number;
  prep?: string;
  expected_kcal_per_100g: number;

  expected_protein_per_100g?: number;
  expected_carbs_per_100g?: number;
  expected_fat_per_100g?: number;

  assumption?: string | null;
  confidence: number;

  components?: Array<{ name_en: string; grams: number }>;
};

export type ResolvedItem = Macros & {
  name: string;
  matchedSource: 'CACHE' | 'USDA' | 'OFF' | 'MODEL' | 'RECIPE';
  grams: number;
  sugars?: number;
  satFat?: number;
  fiber?: number;
  sodiumMg?: number;
  micros?: Record<string, number>;
  confidence: number;
  estimated: boolean;

  assumption?: string | null;
};

export function normalizeKey(nameEn: string): string {
  const noAccents = nameEn
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '');
  const words = noAccents
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, ' ')
    .split(/\s+/)
    .filter(Boolean)
    .map((w) => {
      if (w.length > 3 && w.endsWith('ies')) return `${w.slice(0, -3)}y`;
      if (w.length > 3 && w.endsWith('ses')) return w.slice(0, -2);
      if (w.length > 3 && w.endsWith('s') && !w.endsWith('ss')) return w.slice(0, -1);
      return w;
    });
  return words.join(' ');
}

export const SANITY_TOLERANCE = 0.4;

export function isSane(candidateKcalPer100g: number, expectedKcalPer100g: number): boolean {
  if (expectedKcalPer100g <= 0) return true;
  const drift = Math.abs(candidateKcalPer100g - expectedKcalPer100g) / expectedKcalPer100g;
  return drift <= SANITY_TOLERANCE;
}

export function hasConsistentMacros(p: Per100g, expected?: Macros): boolean {
  if (p.kcal <= 0) return false;
  const fromMacros = 4 * p.protein + 4 * p.carbs + 9 * p.fat;

  if (fromMacros > 2 * p.kcal) return false;

  const candidateHasNoMacros = p.protein + p.carbs + p.fat < 0.5;
  const expectedHasMacros = expected != null &&
    expected.protein + expected.carbs + expected.fat >= 1;
  if (candidateHasNoMacros && expectedHasMacros) return false;

  return true;
}

function expectedMacros(item: ModelItem): Macros | undefined {
  if (
    item.expected_protein_per_100g == null ||
    item.expected_carbs_per_100g == null ||
    item.expected_fat_per_100g == null
  ) {
    return undefined;
  }
  return {
    kcal: item.expected_kcal_per_100g,
    protein: item.expected_protein_per_100g,
    carbs: item.expected_carbs_per_100g,
    fat: item.expected_fat_per_100g,
  };
}

export function scale(per100g: Per100g, grams: number): Omit<Per100g, 'kcal'> & { kcal: number } {
  const f = grams / 100;
  const round = (n: number) => Math.round(n * 10) / 10;

  const round3 = (n: number) => Math.round(n * 1000) / 1000;
  const micros = per100g.micros
    ? Object.fromEntries(Object.entries(per100g.micros).map(([k, v]) => [k, round3(v * f)]))
    : undefined;
  const opt = (v: number | undefined) => (v == null ? undefined : round(v * f));
  return {
    kcal: Math.round(per100g.kcal * f),
    protein: round(per100g.protein * f),
    carbs: round(per100g.carbs * f),
    fat: round(per100g.fat * f),
    sugars: opt(per100g.sugars),
    satFat: opt(per100g.satFat),
    fiber: opt(per100g.fiber),
    sodiumMg: per100g.sodiumMg == null ? undefined : Math.round(per100g.sodiumMg * f),
    micros,
  };
}

export function saltToSodiumMg(saltG: number): number {
  return Math.round((saltG / 2.5) * 1000);
}

export const IU_PER_UG: Record<string, number> = {
  vitD_ug: 40,
  vitA_ug: 3.33,
};

export function iuToMicrograms(key: string, iu: number): number | null {
  const factor = IU_PER_UG[key];
  if (factor === undefined) return null;
  return iu / factor;
}

export function looksLikeInternationalUnits(key: string, valuePer100g: number): boolean {

  const ceiling: Record<string, number> = { vitD_ug: 250, vitA_ug: 6000 };
  const limit = ceiling[key];
  return limit !== undefined && valuePer100g > limit;
}

export type Fetcher = (url: string, init?: RequestInit) => Promise<Response>;

export type Sources = {
  cacheGet: (key: string) => Promise<Per100g | null>;
  cachePut: (key: string, per100g: Per100g, source: 'USDA' | 'OFF') => Promise<void>;
  fetcher: Fetcher;
  usdaKey: string | null;
};

export async function usdaLookup(
  item: ModelItem,
  s: Sources,
): Promise<Per100g | null> {
  if (!s.usdaKey) return null;
  const url = `https://api.nal.usda.gov/fdc/v1/foods/search?query=${encodeURIComponent(item.name_en)}` +
    `&dataType=Foundation,SR%20Legacy&pageSize=5&api_key=${s.usdaKey}`;
  const res = await s.fetcher(url, { signal: AbortSignal.timeout(LOOKUP_TIMEOUT_MS) });
  if (!res.ok) return null;
  const body = await res.json();
  const foods: unknown[] = body?.foods ?? [];

  for (const food of foods) {
    const per100g = usdaToPer100g(food);
    if (
      per100g && isSane(per100g.kcal, item.expected_kcal_per_100g) &&
      hasConsistentMacros(per100g, expectedMacros(item))
    ) {
      return per100g;
    }
  }
  return null;
}

export function usdaToPer100g(food: any): Per100g | null {
  const nutrients: any[] = food?.foodNutrients ?? [];
  const pick = (id: number) => nutrients.find((n) => n?.nutrientId === id)?.value;
  const kcal = pick(USDA_MACRO_IDS.kcal);
  if (typeof kcal !== 'number') return null;
  return {
    kcal,
    protein: pick(USDA_MACRO_IDS.protein) ?? 0,
    carbs: pick(USDA_MACRO_IDS.carbs) ?? 0,
    fat: pick(USDA_MACRO_IDS.fat) ?? 0,

    sugars: pick(USDA_MACRO_IDS.sugars),
    satFat: pick(USDA_MACRO_IDS.satFat),
    fiber: pick(USDA_MACRO_IDS.fiber),
    sodiumMg: pick(USDA_MACRO_IDS.sodiumMg),
    micros: usdaMicros(nutrients),
  };
}

/**
 * Quem a Open Food Facts vê a chamar. Identifica o servidor e não a app: quem faz este
 * pedido é a Edge Function, e ela não sabe de que versão do telemóvel veio o pedido —
 * qualquer versão aqui seria inventada e envelheceria sozinha.
 *
 * A Open Food Facts exige nome e contacto, sob pena de bloquear.
 */
export const OFF_USER_AGENT = 'Antares-Server/1 (betuel801@gmail.com)';

export const LOOKUP_TIMEOUT_MS = 6_000;

export async function offLookup(
  item: ModelItem,
  s: Sources,
): Promise<Per100g | null> {
  const url = 'https://world.openfoodfacts.org/cgi/search.pl?search_terms=' +
    `${encodeURIComponent(item.name_en)}&search_simple=1&action=process&json=1&page_size=5`;
  const res = await s.fetcher(url, {
    headers: { 'User-Agent': OFF_USER_AGENT, Accept: 'application/json' },
    signal: AbortSignal.timeout(LOOKUP_TIMEOUT_MS),
  });
  if (!res.ok) return null;
  const body = await res.json();
  const products: unknown[] = body?.products ?? [];
  for (const p of products) {
    const per100g = offToPer100g(p);
    if (
      per100g && isSane(per100g.kcal, item.expected_kcal_per_100g) &&
      hasConsistentMacros(per100g, expectedMacros(item))
    ) {
      return per100g;
    }
  }
  return null;
}

export function offToPer100g(product: any): Per100g | null {
  const n = product?.nutriments;
  const kcal = n?.['energy-kcal_100g'];
  if (typeof kcal !== 'number') return null;

  const sodiumMg = typeof n?.sodium_100g === 'number'
    ? n.sodium_100g * 1000
    : typeof n?.salt_100g === 'number'
    ? saltToSodiumMg(n.salt_100g)
    : undefined;
  const num = (v: unknown) => (typeof v === 'number' && Number.isFinite(v) ? v : undefined);
  return {
    kcal,
    protein: n?.proteins_100g ?? 0,
    carbs: n?.carbohydrates_100g ?? 0,
    fat: n?.fat_100g ?? 0,
    sugars: num(n?.sugars_100g),
    satFat: num(n?.['saturated-fat_100g']),
    fiber: num(n?.fiber_100g),
    sodiumMg,
    micros: offMicros(n),
  };
}

const MIN_COMPONENTS_RESOLVED = 2;

const MIN_COMPONENT_COVERAGE = 0.6;

export async function resolveFromComponents(
  item: ModelItem,
  s: Sources,
): Promise<Omit<Per100g, 'kcal'> & { kcal: number } | null> {
  const parts = item.components ?? [];
  if (parts.length === 0 || item.grams <= 0) return null;

  const totals = { kcal: 0, protein: 0, carbs: 0, fat: 0 };
  const extras: Record<string, number> = {};
  const micros: Record<string, number> = {};
  let resolved = 0;
  let gramsCovered = 0;

  for (const part of parts) {
    if (!(part.grams > 0)) continue;

    const sub: ModelItem = {
      name_en: part.name_en,
      name_original: part.name_en,
      grams: part.grams,
      expected_kcal_per_100g: 0,
      confidence: item.confidence,
    };
    const key = normalizeKey(part.name_en);
    let per100g = await s.cacheGet(key).catch(() => null);
    if (!per100g || !hasConsistentMacros(per100g)) {
      per100g = await usdaLookup(sub, s).catch(() => null);
      if (per100g) await s.cachePut(key, per100g, 'USDA').catch(() => {});
    }
    if (!per100g) {
      per100g = await offLookup(sub, s).catch(() => null);
      if (per100g) await s.cachePut(key, per100g, 'OFF').catch(() => {});
    }
    if (!per100g) continue;

    const scaled = scale(per100g, part.grams);
    totals.kcal += scaled.kcal;
    totals.protein += scaled.protein;
    totals.carbs += scaled.carbs;
    totals.fat += scaled.fat;
    for (const f of ['sugars', 'satFat', 'fiber', 'sodiumMg'] as const) {
      const v = scaled[f];
      if (typeof v === 'number') extras[f] = (extras[f] ?? 0) + v;
    }
    for (const [k, v] of Object.entries(scaled.micros ?? {})) {
      micros[k] = (micros[k] ?? 0) + v;
    }
    resolved++;
    gramsCovered += part.grams;
  }

  if (resolved < MIN_COMPONENTS_RESOLVED) return null;
  if (gramsCovered / item.grams < MIN_COMPONENT_COVERAGE) return null;

  const round = (n: number) => Math.round(n * 10) / 10;
  const out = {
    kcal: Math.round(totals.kcal),
    protein: round(totals.protein),
    carbs: round(totals.carbs),
    fat: round(totals.fat),
  } as Omit<Per100g, 'kcal'> & { kcal: number };
  for (const f of ['sugars', 'satFat', 'fiber'] as const) {
    if (extras[f] != null) out[f] = round(extras[f]);
  }
  if (extras.sodiumMg != null) out.sodiumMg = Math.round(extras.sodiumMg);
  if (Object.keys(micros).length) {
    out.micros = Object.fromEntries(
      Object.entries(micros).map(([k, v]) => [k, Math.round(v * 1000) / 1000]),
    );
  }
  return out;
}

/**
 * Transforma o que o modelo identificou em números de verdade. A ordem das tentativas é a
 * da confiança: cache, tabela americana, Open Food Facts, decomposição em ingredientes, e
 * só em último recurso a estimativa do próprio modelo.
 *
 * É isto que faz a análise por AI não ser um palpite: o modelo diz *o que* é a comida e
 * quanto pesa; os valores nutricionais vêm de tabelas analisadas. `matchedSource` regista
 * qual delas ganhou, e a app mostra-o.
 *
 * Cada passo tem `.catch(() => null)` porque uma fonte indisponível deve fazer descer ao
 * degrau seguinte, e não falhar a análise inteira.
 */
export async function resolveItem(item: ModelItem, s: Sources): Promise<ResolvedItem> {
  const key = normalizeKey(item.name_en);
  const expected = item.expected_kcal_per_100g;

  // A cache é verificada contra o que o modelo esperava: nomes iguais podem descrever
  // comidas diferentes — "salada" com e sem molho — e um valor guardado que não bate certo
  // com a estimativa é descartado em vez de aceite.
  const cached = await s.cacheGet(key).catch(() => null);
  if (cached && hasConsistentMacros(cached, expectedMacros(item))) {
    return { ...scale(cached, item.grams), name: item.name_original, matchedSource: 'CACHE', grams: item.grams, confidence: item.confidence, estimated: false, assumption: item.assumption ?? null };
  }

  const usda = await usdaLookup(item, s).catch(() => null);
  if (usda) {
    await s.cachePut(key, usda, 'USDA').catch(() => {});
    return { ...scale(usda, item.grams), name: item.name_original, matchedSource: 'USDA', grams: item.grams, confidence: item.confidence, estimated: false, assumption: item.assumption ?? null };
  }

  const off = await offLookup(item, s).catch(() => null);
  if (off) {
    await s.cachePut(key, off, 'OFF').catch(() => {});
    return { ...scale(off, item.grams), name: item.name_original, matchedSource: 'OFF', grams: item.grams, confidence: item.confidence, estimated: false, assumption: item.assumption ?? null };
  }

  const recipe = await resolveFromComponents(item, s).catch(() => null);
  if (recipe) {
    return {
      ...recipe,
      name: item.name_original,
      matchedSource: 'RECIPE',
      grams: item.grams,
      confidence: item.confidence,

      estimated: true,
      assumption: item.assumption ?? null,
    };
  }

  const per100g: Per100g = {
    kcal: expected,
    protein: item.expected_protein_per_100g ?? (expected * 0.20) / 4,
    carbs: item.expected_carbs_per_100g ?? (expected * 0.50) / 4,
    fat: item.expected_fat_per_100g ?? (expected * 0.30) / 9,
  };
  return {
    ...scale(per100g, item.grams),
    name: item.name_original,
    matchedSource: 'MODEL',
    grams: item.grams,
    confidence: item.confidence,
    estimated: true,
    assumption: item.assumption ?? null,
  };
}
