
import { gate, json } from '../_shared/gate.ts';
import { callClaude, ModelError, MODEL_ANALYSIS } from '../_shared/claude.ts';
import { SCHEMA_EXERCISE, SYSTEM_EXERCISE } from '../_shared/prompts.ts';
import { resolveExercise, type ModelExercise } from '../_shared/exercise.ts';

type Body = { text?: string; weightKg?: number; lang?: string; day?: string };

Deno.serve(async (req) => {
  if (req.method !== 'POST') return json({ error: 'method not allowed' }, 405);

  let body: Body;
  try {
    body = await req.json();
  } catch {
    return json({ error: 'invalid json' }, 400);
  }

  if (!body.text?.trim()) return json({ error: 'empty text' }, 400);

  if (body.text.length > 2_000) {
    return json({ error: 'text too long', code: 'TEXT_TOO_LONG' }, 413);
  }

  const weightKg = body.weightKg && body.weightKg > 0 ? body.weightKg : 70;
  const lang = body.lang ?? 'pt';

  const g = await gate(req, 'analyze-exercise', body.day ?? '');
  if (!g.ok) return json({ error: g.error, code: g.code }, g.status);

  const usage = { used: g.access.used, limit: g.access.limit, trial: g.access.kind === 'trial' };

  try {
    const parsed = await callClaude<ModelExercise>({
      model: MODEL_ANALYSIS,
      system: SYSTEM_EXERCISE,
      maxTokens: 2048,
      schema: SCHEMA_EXERCISE as unknown as Record<string, unknown>,
      content: [{ type: 'text', text: `Language: ${lang}\nExercise: ${body.text}` }],
    });

    return json({ ...resolveExercise(parsed, weightKg), usage }, 200);
  } catch (e) {
    if (e instanceof ModelError && e.hard) {
      await g.refund();
      await g.admin.from('ai_requests').insert({
        user_id: g.userId,
        fn: 'analyze-exercise',
        status: 'model_error',
      });
      return json({ error: 'model unavailable', code: 'MODEL_DOWN' }, 502);
    }
    return json({ error: String(e), code: 'ERROR' }, 500);
  }
});
