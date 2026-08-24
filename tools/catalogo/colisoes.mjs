/**
 * Dois alimentos com o mesmo nome.
 *
 * **A deduplicação era um passo que correu uma vez, em 2025.** Desde então, tudo o que entra
 * em execução — a Open Food Facts, as estimativas por foto, os alimentos criados à mão —
 * entra sem ninguém verificar se já lá estava. Aqui passa a ser uma regra da construção.
 *
 * O que se detecta é a colisão, e **não se decide nada**: qual dos dois fica é uma decisão
 * sobre comida, e essas são de quem come. As decisões vivem em `fusoes.json` e a oficina é
 * onde se tomam.
 *
 * A comparação é sobre o nome **normalizado** — sem acentos, sem maiúsculas, sem pontuação —
 * porque «Vinho maduro tinto, teor alcoólico ≥12,5% vol.» e «Vinho maduro tinto, teor
 * alcoólico ≥ 12,5 % vol» são o mesmo vinho escrito por duas mãos.
 */

/** Sem acentos, sem maiúsculas, sem pontuação, sem espaços a dobrar. */
export function normalizar(nome) {
  return String(nome ?? "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9 ]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/** A partir de que diferença de energia duas linhas com o mesmo nome discordam a sério. */
const DISCORDANCIA_RELEVANTE = 0.15;

/**
 * Agrupa por nome normalizado e devolve os grupos com mais do que um.
 *
 * Cada grupo traz `discordam`, que é o que separa uma duplicação inofensiva — duas linhas com
 * os mesmos números — de uma que engana: a sangria a 89 kcal numa e a 120 na outra, conforme
 * a que a pessoa escolher da lista.
 */
export function colisoes(alimentos) {
  const porNome = new Map();
  for (const a of alimentos) {
    const chave = normalizar(a.namePt);
    if (!chave) continue;
    if (!porNome.has(chave)) porNome.set(chave, []);
    porNome.get(chave).push(a);
  }

  const encontradas = [];
  for (const [nome, membros] of porNome) {
    if (membros.length < 2) continue;

    const energias = membros.map((m) => m.kcal).filter((k) => Number.isFinite(k));
    const menor = Math.min(...energias);
    const maior = Math.max(...energias);
    const discordam = energias.length > 1 && menor > 0 &&
      (maior - menor) / menor > DISCORDANCIA_RELEVANTE;

    encontradas.push({
      nome,
      discordam,
      alimentos: membros
        .map((m) => ({ id: m.id, namePt: m.namePt, kcal: m.kcal, origem: m.origin ?? "?" }))
        .sort((x, y) => (x.id < y.id ? -1 : 1)),
    });
  }

  // Os que discordam primeiro: são os que enganam, e os outros podem esperar.
  return encontradas.sort((a, b) =>
    Number(b.discordam) - Number(a.discordam) || (a.nome < b.nome ? -1 : 1));
}

/**
 * Aplica as fusões decididas: o perdedor sai do catálogo e deixa uma lápide a apontar ao
 * vencedor.
 *
 * **A lápide não é arrumação.** Sem ela, quem tinha o perdedor nos favoritos ou o registou
 * ontem fica com um alimento que deixou de existir — e o diário guarda cópia da nutrição, mas
 * o favorito guarda só o identificador. A lápide é o que o faz seguir para o sucessor.
 */
export function aplicarFusoes(alimentos, fusoes) {
  const perdedores = new Map(Object.entries(fusoes ?? {}));
  if (!perdedores.size) return { vivos: alimentos, lapides: [], fundidos: 0 };

  const existentes = new Set(alimentos.map((a) => a.id));
  const lapides = [];
  const vivos = [];

  for (const a of alimentos) {
    const sucessor = perdedores.get(a.id);

    // Uma fusão para um vencedor que não existe deixaria quem seguisse a lápide sem nada. É
    // preferível manter o duplicado do que mandar alguém para um alimento apagado.
    if (!sucessor || !existentes.has(sucessor)) {
      vivos.push(a);
      continue;
    }
    lapides.push({ id: a.id, sucessor });
  }

  lapides.sort((x, y) => (x.id < y.id ? -1 : 1));
  return { vivos, lapides, fundidos: lapides.length };
}
