/**
 * O USDA SR Legacy, e os alimentos portugueses escritos à mão que viajam no mesmo ficheiro.
 *
 * Faz dois trabalhos, e são diferentes: **enriquecer** — dar micronutrientes a alimentos da
 * CIQUAL que os não trazem — e **encher a cauda**, com aquilo que a CIQUAL não tem de todo.
 *
 * A cauda é a parte perigosa. O USDA publica nomes de laboratório — «Beef, chuck, arm pot
 * roast, separable lean and fat, trimmed to 1/8" fat, all grades, raw» — que ninguém escreve
 * numa caixa de pesquisa. O [isLabDescriptor] deita-os fora, e é por isso que entram menos de
 * metade dos registos.
 */
import { readFileSync, existsSync } from "node:fs";

const STOP = new Set([
  "raw", "cooked", "boiled", "baked", "roasted", "grilled", "fried", "steamed",
  "canned", "drained", "prepacked", "average", "or", "and", "with", "without",
  "the", "a", "of", "in", "type", "unprepared", "prepared", "fresh", "frozen",
  "all", "types", "kind", "commercial", "commercially", "home", "made",
]);

function singular(w) {
  if (w.length > 3 && w.endsWith("ies")) return w.slice(0, -3) + "y";
  if (w.length > 3 && w.endsWith("ses")) return w.slice(0, -2);
  if (w.length > 3 && w.endsWith("s") && !w.endsWith("ss")) return w.slice(0, -1);
  return w;
}

/**
 * A chave com que dois nomes de fontes diferentes se reconhecem um ao outro: minúsculas,
 * sem acentos, sem palavras de ligação, no singular, e por ordem alfabética — para que
 * «tomato, raw» e «raw tomatoes» dêem a mesma coisa.
 */
export function chaveDeNome(nome) {
  const palavras = String(nome)
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9\s]/g, " ")
    .split(/\s+/)
    .filter(Boolean)
    .map(singular)
    .filter((w) => w.length > 2 && !STOP.has(w));
  return palavras.sort().join(" ");
}

const MARCAS_DE_LABORATORIO =
  /separable|trimmed to|all grades|bone-?in|boneless|denuded|frenched|untrimmed|choice|select|prime grade|composite of|includes usda|retail cuts?|refuse|yield from|raw, ?nfs/i;

// Quatro vírgulas num nome de alimento não é pontuação: é uma ficha de laboratório com o
// corte, o grau e o aparo todos no mesmo campo.
const VIRGULAS_A_MAIS = 4;
const NOME_COMPRIDO = 90;

export function isLabDescriptor(nome) {
  const virgulas = (nome.match(/,/g) || []).length;
  return MARCAS_DE_LABORATORIO.test(nome) ||
    virgulas >= VIRGULAS_A_MAIS ||
    nome.length > NOME_COMPRIDO;
}

// Duas medições do mesmo alimento não dão o mesmo número, mas dão o mesmo intervalo. Acima
// de quatro décimos de diferença não é o mesmo alimento — é uma correspondência errada.
const DESVIO_ACEITE = 0.4;

export function energiaConcorda(a, b) {
  if (!a || !b) return false;
  return Math.abs(a - b) / b <= DESVIO_ACEITE;
}

export function lerUsda(caminho) {
  if (!existsSync(caminho)) {
    throw new Error(`Falta ${caminho} — é a extração do USDA SR Legacy; ver tools/README.md.`);
  }
  const registos = JSON.parse(readFileSync(caminho, "utf8"));

  const porChave = new Map();
  const porChaveCurta = new Map();
  for (const u of registos) {
    const k = chaveDeNome(u.nameEn);
    if (!k) continue;
    if (!porChave.has(k)) porChave.set(k, u);
    const curta = k.split(" ").slice(0, 2).join(" ");
    if (curta && !porChaveCurta.has(curta)) porChaveCurta.set(curta, u);
  }

  return { declarados: registos.length, registos, porChave, porChaveCurta };
}
