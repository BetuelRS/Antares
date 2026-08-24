/**
 * Põe cada alimento de acordo consigo próprio, antes de o motor de qualidade o julgar.
 *
 * As doze contradições que o catálogo trazia eram a mesma coisa dita de três maneiras: **uma
 * parte maior do que o todo**. O açúcar a passar os hidratos, as três gorduras a passarem a
 * gordura, a água mais os macros a passarem cem gramas. Nenhuma delas é uma discordância entre
 * fontes — é uma incompatibilidade dentro da mesma linha, e resolve-se com aritmética.
 *
 * **A parte cede ao todo, e não o contrário.** Em todos os doze casos a energia declarada
 * confirma os macros ao decigrama: o paio de lombo diz 361 kcal e 22,8 g de proteína com 30 g
 * de gordura, o que dá exactamente 361. O que não bate é a soma das três frações de gordura,
 * que dá 34. Quando duas medições se contradizem e uma terceira confirma uma delas, é a outra
 * que cede.
 *
 * **Nada aqui inventa um número.** Corta-se, escala-se ou apaga-se; nunca se escreve um valor
 * que não estivesse já implícito no que a fonte publicou. Onde não há como corrigir sem
 * inventar — a água de um alimento que soma cento e cinco gramas —, **apaga-se**: não saber é
 * uma resposta, e um número impossível não é.
 */

import { LIMITES } from "./qualidade.mjs";

const GRAMAS_NUM_ALIMENTO = 100;

/**
 * As folgas são **as do motor de qualidade**, e não umas suas.
 *
 * É o que faz esta correção arrumar exactamente o que ele chamaria contradição, e nada mais.
 * Com folga zero, o arredondamento de três valores a uma casa decimal já punha duzentos e
 * nove alimentos a serem escalados por um décimo de grama que ninguém mediu — trabalho a
 * fingir, e um `git diff` de duzentas linhas a esconder as que interessam.
 *
 * Dois números diferentes seria pior ainda: um a corrigir o que o outro não acusa, ou um a
 * acusar o que o outro já corrigiu.
 */
const FOLGA_DA_SOMA = LIMITES.somaFolga;
const FOLGA_DA_MASSA = LIMITES.massaFolga;

/** As três frações da gordura, quando existem. Vivem no mapa de micronutrientes. */
const FRACOES_DA_GORDURA = ["fatMono_g", "fatPoly_g"];

const numero = (v) => (typeof v === "number" && Number.isFinite(v) ? v : null);
const arredondar = (v) => Math.round(v * 100) / 100;

/**
 * O açúcar não pode passar os hidratos.
 *
 * O queijo de cabra seco declara 5,3 g de açúcares e 0 g de hidratos; a energia dele bate
 * certo **sem** hidratos nenhuns — 30,5 g de proteína e 35,6 de gordura dão as 440 kcal
 * declaradas. Os hidratos estão certos, e é o açúcar que sobra.
 *
 * O corte é para os hidratos e não para zero: onde houver hidratos, o açúcar pode ser todos
 * eles, e é o máximo que se pode afirmar.
 */
export function acucarCabeNosHidratos(e) {
  const hidratos = numero(e.carbsG);
  const acucares = numero(e.sugarsG);
  if (hidratos == null || acucares == null || acucares <= hidratos + FOLGA_DA_SOMA) return false;

  e.sugarsG = hidratos;
  return true;
}

/**
 * As três frações da gordura não podem passar a gordura toda.
 *
 * A gordura total mede-se por gravimetria e as frações por cromatografia — são métodos
 * diferentes, e é por isso que discordam. A energia declarada é calculada a partir da total,
 * e confirma-a: no paio, 22,8 × 4 mais 30 × 9 dá 361, que é o que a tabela diz.
 *
 * Escala-se as três para somarem a total, o que mantém as proporções entre elas — que é a
 * informação que elas trazem — e tira a contradição. Escalar é preferível a cortar a maior:
 * o erro está distribuído pelas três e não há razão para o pôr todo numa.
 */
export function gordurasCabemNaGordura(e) {
  const total = numero(e.fatG);
  if (total == null || total <= 0) return false;

  const micros = e.micros ?? {};
  const partes = [
    ["satFatG", numero(e.satFatG)],
    ...FRACOES_DA_GORDURA.map((k) => [k, numero(micros[k])]),
  ].filter(([, v]) => v != null);

  const soma = partes.reduce((s, [, v]) => s + v, 0);
  if (soma <= total + FOLGA_DA_SOMA || soma <= 0) return false;

  const factor = total / soma;
  for (const [chave, valor] of partes) {
    const escalado = arredondar(valor * factor);
    if (chave === "satFatG") e.satFatG = escalado;
    else micros[chave] = escalado;
  }
  return true;
}

/**
 * A água mais os macros não podem passar cem gramas.
 *
 * Aqui não há como corrigir sem inventar: qualquer dos cinco números pode estar alto, e
 * escalá-los todos era mexer em medições que a energia confirma. **Apaga-se a água**, que é a
 * única parcela que a energia não confirma — ela não entra na conta das calorias, e por isso
 * é a única sobre a qual as outras não dizem nada.
 *
 * O que se perde é a água que aquele alimento traz, num punhado de alimentos. O que se ganha é
 * não publicar um alimento que soma cento e cinco gramas em cem.
 */
export function aguaCabeNoAlimento(e) {
  const micros = e.micros;
  const agua = numero(micros?.water_g);
  if (agua == null) return false;

  const soma = agua + (numero(e.proteinG) ?? 0) + (numero(e.carbsG) ?? 0) + (numero(e.fatG) ?? 0);
  if (soma <= GRAMAS_NUM_ALIMENTO + FOLGA_DA_MASSA) return false;

  delete micros.water_g;
  return true;
}

/** Corre as três sobre o catálogo todo e diz quantas vezes cada uma tocou em alguma coisa. */
export function porDeAcordoConsigo(alimentos) {
  const contagens = { acucares: 0, gorduras: 0, agua: 0 };
  for (const e of alimentos) {
    if (acucarCabeNosHidratos(e)) contagens.acucares++;
    if (gordurasCabemNaGordura(e)) contagens.gorduras++;
    if (aguaCabeNoAlimento(e)) contagens.agua++;
  }
  return contagens;
}
