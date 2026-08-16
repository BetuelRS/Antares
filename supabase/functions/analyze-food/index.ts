
import { type SupabaseClient } from 'jsr:@supabase/supabase-js@2';
import { gate, json } from '../_shared/gate.ts';
import { callClaude, ModelError, MODEL_ANALYSIS, type ContentBlock } from '../_shared/claude.ts';
import {
  SCHEMA_FOOD,
  SCHEMA_LABEL,
  SYSTEM_FOOD_LABEL,
  SYSTEM_FOOD_PHOTO,
  SYSTEM_FOOD_TEXT,
} from '../_shared/prompts.ts';
import {
  resolveItem,
  saltToSodiumMg,
  type ModelItem,
  type Per100g,
  type ResolvedItem,
  type Sources,
} from '../_shared/nutrition.ts';

/**
 * O cliente do Supabase com chave de serviço, com o tipo que a própria biblioteca publica.
 * Era `any`, e com `any` um nome de método trocado só se descobria em produção — o
 * `deno check` não tinha nada a que se agarrar.
 */
type Admin = SupabaseClient;

// Limites verificados antes de a chamada ao modelo ser cobrada: o custo cresce com o
// tamanho da entrada, e uma imagem enorme é a maneira mais fácil de gastar a fatura de
// alguém. Uma refeição fotografada cabe muito abaixo disto.
const MAX_IMAGE_BYTES = 1_500_000;

const MAX_TEXT_CHARS = 2_000;

type Body = {
  mode?: 'text' | 'photo' | 'label';
  text?: string;
  imageBase64?: string;
  imageMime?: string;
  lang?: string;
  day?: string;
};

type ModelFood = { items: ModelItem[]; warnings: string[] };
type ModelLabel = {
  name_original?: string | null;
  serving_g?: number | null;
  kcal_100?: number | null;
  protein_100?: number | null;
  carbs_100?: number | null;
  fat_100?: number | null;
  sugars_100?: number | null;
  satfat_100?: number | null;
  fiber_100?: number | null;
  salt_g_100?: number | null;
  basis?: 'printed_100g' | 'converted_from_serving' | null;
  micros?: Record<string, number | null> | null;
  warnings: string[];
};

function sources(admin: Admin): Sources {
  return {
    usdaKey: Deno.env.get('USDA_API_KEY') ?? null,
    fetcher: fetch,
    cacheGet: async (key) => {
      const { data } = await admin.from('food_cache').select('nutrition').eq('key', key).maybeSingle();
      if (!data) return null;

      await Promise.resolve(admin.rpc('food_cache_hit', { p_key: key })).catch(() => {});
      return data.nutrition as Per100g;
    },
    cachePut: async (key, per100g, source) => {
      await admin.from('food_cache').upsert({
        key,
        nutrition: per100g,
        source,
        updated_at: Date.now(),
      });
    },
  };
}

Deno.serve(async (req) => {
  if (req.method !== 'POST') return json({ error: 'method not allowed' }, 405);

  let body: Body;
  try {
    body = await req.json();
  } catch {
    return json({ error: 'invalid json' }, 400);
  }

  const mode = body.mode ?? 'text';
  const day = body.day ?? '';
  const lang = body.lang ?? 'pt';

  if (mode === 'text' && !body.text?.trim()) return json({ error: 'empty text' }, 400);
  if ((body.text?.length ?? 0) > MAX_TEXT_CHARS) {
    return json({ error: 'text too long', code: 'TEXT_TOO_LONG' }, 413);
  }
  if ((mode === 'photo' || mode === 'label') && !body.imageBase64) {
    return json({ error: 'missing image' }, 400);
  }
  // O fator 0,75 converte o comprimento do base64 no tamanho real dos bytes, sem ter de
  // descodificar a imagem para a medir.
  if (body.imageBase64 && body.imageBase64.length * 0.75 > MAX_IMAGE_BYTES) {
    return json({ error: 'image too large', code: 'IMAGE_TOO_LARGE' }, 413);
  }

  const g = await gate(req, 'analyze-food', day);
  if (!g.ok) return json({ error: g.error, code: g.code }, g.status);

  const usage = { used: g.access.used, limit: g.access.limit, trial: g.access.kind === 'trial' };

  try {
    if (mode === 'label') {
      const label = await callClaude<ModelLabel>({
        model: MODEL_ANALYSIS,
        system: SYSTEM_FOOD_LABEL,
        maxTokens: 2048,
        schema: SCHEMA_LABEL as unknown as Record<string, unknown>,
        content: [
          imageBlock(body.imageBase64!, body.imageMime),
          { type: 'text', text: `Read this nutrition label. Reply in ${lang} for the product name.` },
        ],
      });
      return json({ ...labelDraft(label), usage }, 200);
    }

    const system = mode === 'photo' ? SYSTEM_FOOD_PHOTO : SYSTEM_FOOD_TEXT;
    const content: ContentBlock[] = mode === 'photo'
      ? [
        imageBlock(body.imageBase64!, body.imageMime),
        { type: 'text', text: `What food is in this photo? Reply with name_original in ${lang}.` },
      ]
      : [{ type: 'text', text: `Language: ${lang}\nMeal: ${body.text}` }];

    const parsed = await callClaude<ModelFood>({
      model: MODEL_ANALYSIS,
      system,
      maxTokens: 2048,
      schema: SCHEMA_FOOD as unknown as Record<string, unknown>,
      content,
    });

    const s = sources(g.admin);
    const items: ResolvedItem[] = await Promise.all(
      (parsed.items ?? []).map((item) => resolveItem(item, s)),
    );

    const totalKcal = items.reduce((sum, i) => sum + i.kcal, 0);
    return json({ items, totalKcal, warnings: parsed.warnings ?? [], usage }, 200);
  } catch (e) {

    if (e instanceof ModelError && e.hard) {
      await g.refund();
      await g.admin.from('ai_requests').insert({
        user_id: g.userId,
        fn: 'analyze-food',
        status: 'model_error',
      });
      return json({ error: 'model unavailable', code: 'MODEL_DOWN' }, 502);
    }
    return json({ error: String(e), code: 'ERROR' }, 500);
  }
});

function imageBlock(base64: string, mime?: string): ContentBlock {
  return {
    type: 'image',
    source: { type: 'base64', media_type: mime ?? 'image/jpeg', data: base64 },
  };
}

export function labelIsCoherent(kcal: number, protein: number, carbs: number, fat: number): boolean {
  if (kcal <= 0) return true;
  const fromMacros = 4 * protein + 4 * carbs + 9 * fat;
  return Math.abs(fromMacros - kcal) / kcal <= 0.25;
}

export function labelDraft(l: ModelLabel) {
  const sodiumMg = l.salt_g_100 != null ? saltToSodiumMg(l.salt_g_100) : null;
  const micros = l.micros
    ? Object.fromEntries(
      Object.entries(l.micros).filter(([, v]) => typeof v === 'number' && v > 0),
    )
    : null;

  const warnings = [...(l.warnings ?? [])];

  if (l.kcal_100 == null) warnings.push('LABEL_INCOMPLETE');

  if (l.basis === 'converted_from_serving') warnings.push('LABEL_CONVERTED');
  if (
    l.kcal_100 != null && l.protein_100 != null && l.carbs_100 != null && l.fat_100 != null &&
    !labelIsCoherent(l.kcal_100, l.protein_100, l.carbs_100, l.fat_100)
  ) {
    warnings.push('LABEL_INCONSISTENT');
  }

  return {
    draft: {
      name: l.name_original ?? null,
      servingG: l.serving_g ?? null,
      per100g: {
        kcal: l.kcal_100 ?? null,
        protein: l.protein_100 ?? null,
        carbs: l.carbs_100 ?? null,
        fat: l.fat_100 ?? null,
        sugars: l.sugars_100 ?? null,
        satFat: l.satfat_100 ?? null,
        fiber: l.fiber_100 ?? null,
        sodiumMg,
      },
      micros: micros && Object.keys(micros).length ? micros : null,
    },
    warnings,
  };
}
