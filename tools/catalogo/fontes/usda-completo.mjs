/**
 * Os nutrientes da USDA que a extração antiga não trouxe.
 *
 * O `usda-source.json` traz dezoito micronutrientes dos **quarenta e dois** que a app conhece:
 * os minerais e as vitaminas, e mais nada. Ficaram de fora a **água**, o **fósforo**, o
 * **colesterol**, as gorduras mono e poli-insaturadas, o **álcool** e os polióis — que não são
 * nutrientes de segunda linha:
 *
 * - Sem água, o balanço de massa não se pode verificar em 2 948 alimentos, que é um terço do
 *   catálogo. E a água que a comida traz conta para a meta de hidratação.
 * - Sem álcool, todas as bebidas alcoólicas da USDA falham a conta de Atwater — a energia
 *   está lá e os macros não a explicam, porque falta a parcela que a produz.
 * - Sem os polióis, os doces sem açúcar fazem o mesmo.
 * - Sem as gorduras mono e poli, a verificação das partes contra o todo não corre.
 *
 * Lê-se do `food_nutrient.csv` do próprio SR Legacy, que é a mesma fonte de onde a extração
 * saiu — não é uma fonte nova, é a mesma lida por inteiro.
 */
import { readFileSync, existsSync } from "node:fs";
import { join } from "node:path";

import { lerCsv } from "../../confecao/tabelas.mjs";

/**
 * O identificador do nutriente na USDA e a chave por que a app o conhece.
 *
 * Só o que faltava: os dezoito que a extração já traz ficam como estão, porque reescrevê-los
 * era arriscar mudar números que já foram revistos sem nada a ganhar.
 *
 * O ómega-3 e o ómega-6 saem dos ácidos gordos que os representam nas tabelas — o 18:3 e o
 * 18:2 —, que é a mesma convenção que a CIQUAL usa.
 */
export const EM_FALTA = {
  1051: "water_g",
  1091: "phosphorus_mg",
  1253: "cholesterol_mg",
  1292: "fatMono_g",
  1293: "fatPoly_g",
  1257: "fatTrans_g",
  1018: "alcohol_g",
  1086: "polyols_g",
  1101: "manganese_mg",
  1100: "iodine_ug",
  1088: "chloride_mg",
  1270: "omega3_g",
  1269: "omega6_g",
  1278: "epa_g",
  1272: "dha_g",
  1009: "starch_g",
  1013: "lactose_g",
  1105: "retinol_ug",
  1107: "betaCarotene_ug",
};

/**
 * Lê os nutrientes em falta para os alimentos que interessam.
 *
 * Recebe o conjunto de identificadores a procurar em vez de ler tudo: são um milhão e meio de
 * linhas para dois mil e novecentos alimentos, e guardar o resto era encher a memória com o
 * que ninguém vai perguntar.
 */
export function lerNutrientesEmFalta(ficheiro, fdcIds) {
  if (!existsSync(ficheiro)) {
    throw new Error(`Falta ${ficheiro} — corre 'node tools/descarregar-fontes.mjs'.`);
  }

  const porAlimento = new Map();
  for (const l of lerCsv(readFileSync(ficheiro, "utf8")).slice(1)) {
    if (l.length < 4) continue;
    const [, fdc, nutriente, quantidade] = l;
    if (!fdcIds.has(fdc)) continue;

    const chave = EM_FALTA[Number(nutriente)];
    if (!chave) continue;

    const valor = Number(quantidade);

    // Zero não se guarda, como no resto do oleoduto: um nutriente declarado a zero não é o
    // alimento ser rico nele, e enchia o mapa de linhas que nenhuma pergunta quer.
    if (!Number.isFinite(valor) || valor <= 0) continue;

    if (!porAlimento.has(fdc)) porAlimento.set(fdc, {});
    porAlimento.get(fdc)[chave] = Math.round(valor * 1000) / 1000;
  }
  return porAlimento;
}

/**
 * Acrescenta o que faltava, **sem tocar no que já lá estava**.
 *
 * A extração antiga já foi revista e corrigida à mão ao longo de meses. O que aqui entra é o
 * que falta, e nada mais — um valor que já exista fica como está, mesmo que a leitura completa
 * traga outro.
 */
export function completar(alimentos, ficheiro) {
  const fdcIds = new Set(
    alimentos.filter((a) => a.id.startsWith("usda-")).map((a) => a.id.slice("usda-".length)),
  );
  if (!fdcIds.size) return { alimentos: 0, valores: 0 };

  const encontrados = lerNutrientesEmFalta(ficheiro, fdcIds);
  let tocados = 0;
  let valores = 0;

  for (const a of alimentos) {
    if (!a.id.startsWith("usda-")) continue;
    const novos = encontrados.get(a.id.slice("usda-".length));
    if (!novos) continue;

    const micros = a.micros ?? {};
    let mexeu = false;
    for (const [chave, valor] of Object.entries(novos)) {
      if (micros[chave] != null) continue;
      micros[chave] = valor;
      valores++;
      mexeu = true;
    }
    if (mexeu) {
      a.micros = micros;
      tocados++;
    }
  }

  return { alimentos: tocados, valores };
}
