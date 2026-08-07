
import { adminClient, json } from '../_shared/gate.ts';

let lastAttemptAt = 0;

Deno.serve(async (req) => {
  if (req.method !== 'POST') return json({ error: 'method not allowed' }, 405);

  const jwt = (req.headers.get('Authorization') ?? '').replace('Bearer ', '').trim();
  if (!jwt) return json({ error: 'missing token', code: 'NO_AUTH' }, 401);

  let body: { code?: string; enable?: boolean };
  try {
    body = await req.json();
  } catch {
    return json({ error: 'invalid json' }, 400);
  }
  if (typeof body.code !== 'string' || typeof body.enable !== 'boolean') {
    return json({ error: 'bad body' }, 400);
  }

  const now = Date.now();
  if (now - lastAttemptAt < 1000) return json({ error: 'slow down', code: 'RATE_LIMIT' }, 429);
  lastAttemptAt = now;

  const admin = adminClient();
  const { data: userData, error: userErr } = await admin.auth.getUser(jwt);
  const userId = userData?.user?.id;
  if (userErr || !userId) return json({ error: 'invalid token', code: 'NO_AUTH' }, 401);

  const expected = Deno.env.get('ADMIN_CODE');

  if (!expected || body.code !== expected) {
    return json({ error: 'forbidden', code: 'BAD_CODE' }, 403);
  }

  const { error } = await admin.from('ai_admin').upsert({
    user_id: userId,
    unlimited: body.enable,
    updated_at: Date.now(),
  });
  if (error) return json({ error: error.message, code: 'DB_ERROR' }, 500);

  return json({ unlimited: body.enable }, 200);
});
