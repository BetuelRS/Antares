
// O modelo mais barato e rápido chega para a análise, que é sempre extração estruturada a
// partir de texto ou imagem — não há aqui raciocínio livre a fazer.
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

/**
 * `hard` distingue a falha que é culpa nossa da que é passageira, e decide se a utilização
 * é devolvida ao utilizador: uma chave em falta não lhe deve custar saldo, um modelo que
 * respondeu mal já consumiu a chamada.
 */
export class ModelError extends Error {
  constructor(message: string, readonly hard: boolean) {
    super(message);
  }
}

/**
 * A única porta para a API da Anthropic. A chave vive só aqui, no servidor: é toda a razão
 * de estas funções existirem em vez de a app chamar o modelo diretamente.
 *
 * O `fetcher` é injetável para os testes correrem sem rede nem chave.
 */
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
      // Esquema JSON imposto na saída: o modelo devolve dados com forma garantida em vez
      // de texto que fosse preciso interpretar. É o que permite a app tratar a resposta
      // como qualquer outra fonte de alimentos.
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
