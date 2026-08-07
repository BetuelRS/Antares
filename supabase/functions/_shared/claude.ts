
export const MODEL_ANALYSIS = 'claude-haiku-4-5';
export const MODEL_COACH = 'claude-sonnet-5';

export type ContentBlock =
  | { type: 'text'; text: string }
  | { type: 'image'; source: { type: 'base64'; media_type: string; data: string } };

export type ClaudeCall = {
  model: string;
  system: string;
  content: ContentBlock[];
  schema: Record<string, unknown>;
  maxTokens: number;
};

export type Fetcher = (url: string, init?: RequestInit) => Promise<Response>;

export class ModelError extends Error {
  constructor(message: string, readonly hard: boolean) {
    super(message);
  }
}

export async function callClaude<T>(
  call: ClaudeCall,
  fetcher: Fetcher = fetch,
  apiKey = Deno.env.get('ANTHROPIC_API_KEY'),
): Promise<T> {
  if (!apiKey) throw new ModelError('missing ANTHROPIC_API_KEY', true);

  const res = await fetcher('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-api-key': apiKey,
      'anthropic-version': '2023-06-01',
    },
    body: JSON.stringify({
      model: call.model,
      max_tokens: call.maxTokens,
      system: call.system,
      messages: [{ role: 'user', content: call.content }],
      output_config: { format: { type: 'json_schema', schema: call.schema } },
    }),
  });

  if (!res.ok) {
    const body = await res.text();

    const hard = res.status >= 500 || res.status === 429;
    throw new ModelError(`anthropic ${res.status}: ${body.slice(0, 200)}`, hard);
  }

  const body = await res.json();
  const text = body?.content?.find((b: { type: string }) => b.type === 'text')?.text;
  if (!text) throw new ModelError('empty model response', true);

  try {
    return JSON.parse(text) as T;
  } catch {
    throw new ModelError('model did not return valid json', true);
  }
}
