/**
 * A fila da oficina: por que ordem se olha para oito mil alimentos.
 *
 * **Por quantas vezes o alimento foi registado.** Um alimento que se come todas as semanas
 * vale mil que ninguém procura, e a curadoria só acontece se as primeiras horas de trabalho
 * caírem nos alimentos que alguém come. Ordenar pelo identificador, ou pela gravidade do
 * achado, punha a primeira semana de trabalho em cogumelos shiitake enlatados.
 *
 * O histórico vem do telemóvel e pode não estar lá. Quando não está, a fila cai para a
 * segunda ordem — quantos achados o alimento tem — em vez de se recusar a existir: uma
 * ferramenta que precisa de uma extração para arrancar é uma ferramenta que não se usa.
 */

/** Um alimento na fila, com tudo o que o cartão precisa de mostrar sem ir buscar mais nada. */
export function montarFila({ alimentos, achados, historico = {} }) {
  const porId = new Map();
  for (const a of achados) {
    if (!porId.has(a.id)) porId.set(a.id, []);
    porId.get(a.id).push(a);
  }

  const fila = alimentos.map((a) => ({
    id: a.id,
    namePt: a.namePt,
    nameEn: a.nameEn,
    kcal: a.kcal,
    registos: historico[a.id] ?? 0,
    achados: porId.get(a.id) ?? [],
  }));

  fila.sort(comparar);
  return fila;
}

/**
 * Três critérios, por esta ordem: quantas vezes foi registado, quantos achados tem, e o
 * identificador.
 *
 * O terceiro não é arrumação: sem ele, dois alimentos empatados trocam de lugar entre
 * execuções conforme a ordem em que o motor os encontrou, e quem estiver a trabalhar na
 * fila perde o sítio onde ia.
 */
export function comparar(x, y) {
  if (x.registos !== y.registos) return y.registos - x.registos;
  if (x.achados.length !== y.achados.length) return y.achados.length - x.achados.length;
  return x.id < y.id ? -1 : x.id > y.id ? 1 : 0;
}

/**
 * O nome partido nos segmentos por que as fontes o escrevem.
 *
 * «Rice, wild, raw» não é uma frase: é uma base, uma variedade e um estado, e as três
 * repetem-se por milhares de alimentos. Traduzir segmento a segmento é o que torna as
 * novecentas traduções possíveis — «raw» traduz-se uma vez, não novecentas.
 *
 * Quem decide o vocabulário dos segmentos é o dono. Isto só os separa.
 */
export function segmentos(nome) {
  return String(nome ?? "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

/**
 * O que ainda está por traduzir: os segmentos do nome inglês que não aparecem no português.
 *
 * A comparação é por segmento e não pelo nome inteiro, porque o caso comum é meio nome
 * traduzido — «Arroz, wild, cru» — e um nome meio traduzido não se distingue de um traduzido
 * a olho numa lista de oito mil.
 */
export function porTraduzir(nameEn, namePt) {
  if (!namePt || namePt === nameEn) return segmentos(nameEn);
  const emPortugues = segmentos(namePt).map((s) => s.toLowerCase());
  return segmentos(nameEn).filter((s) => emPortugues.includes(s.toLowerCase()));
}
