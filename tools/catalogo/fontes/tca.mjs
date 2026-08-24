/**
 * A Tabela de Composição de Alimentos do INSA — a única fonte portuguesa medida.
 *
 * Lê a folha de cálculo tal como o INSA a publica. As colunas que **não** entram estão
 * listadas em [RECUSADAS] com a razão de cada uma: sem essa lista, quem olhar para o mapa
 * daqui a um ano não distingue uma coluna recusada de uma coluna esquecida.
 */
import { readSheet } from "../../tca-importer/xlsx.mjs";
import { familiaDeTca } from "../../confecao/familias.mjs";

const VERSAO_TCA = "7.1";

// O `nivel1` e a classificacao FoodEx2 que o INSA publica em coluna propria. Nao vai para o
// catalogo como texto: e o que diz a que familia de confecao o alimento pertence.
const COLUNAS = { code: 0, name: 1, nivel1: 2, kcal: 5, fat: 7, protein: 19, carbs: 13 };

const MICROS = {
  8: "satFat_g", 9: "fatMono_g", 10: "fatPoly_g", 12: "fatTrans_g",
  14: "sugars_g", 18: "fiber_g", 20: "alcohol_g", 21: "water_g",
  23: "cholesterol_mg", 24: "vitA_ug", 32: "vitD_ug", 33: "vitE_mg",
  34: "vitB1_mg", 35: "vitB2_mg", 36: "vitB3_mg", 39: "vitB6_mg",
  40: "vitB12_ug", 41: "vitC_mg", 42: "vitB9_ug", 44: "sodium_mg",
  45: "potassium_mg", 46: "calcium_mg", 47: "phosphorus_mg", 48: "magnesium_mg",
  49: "iron_mg", 50: "zinc_mg", 51: "selenium_ug", 52: "iodine_ug",
};

export const RECUSADAS = {
  6: "Energia [kJ] — a app usa kcal; converter seria guardar o mesmo duas vezes.",
  11: "Ácido linoleico — o esquema não tem entrada para ácidos gordos individuais.",
  15: "Oligossacáridos — sem chave canónica e sem referência dietética.",
  16: "Amido — idem.",
  17: "Sal [g] — redundante: o sódio vem medido na coluna 44, e derivar sal de sódio é multiplicar por 2,5.",
  22: "Ácidos orgânicos — sem chave canónica.",
  25: "Equivalentes de β-caroteno — já contabilizados na vitamina A (col. 24); somar seria contar duas vezes.",
  26: "α-caroteno — carotenoide individual, sem chave nem DRV.",
  27: "β-caroteno total — idem (e ver col. 25).",
  28: "β-criptoxantina — idem.",
  29: "Licopeno — idem.",
  30: "Luteína — idem.",
  31: "Zeaxantina — idem.",
  37: "Equivalentes de niacina — usa-se a niacina simples (col. 36) por ser o que o resto do catálogo (CIQUAL, USDA) contém; misturar as duas medidas na mesma chave daria somas incomparáveis.",
  38: "Triptofano/60 — parte do cálculo dos equivalentes de niacina, não é nutriente autónomo.",
  43: "Cinza — resíduo analítico, não é nutriente.",
};

export const SEM_DADOS_NA_TCA = ["vitK_ug", "vitB5_mg", "copper_mg", "manganese_mg"];

function num(v) {
  if (v === "" || v == null) return null;
  const s = String(v).replace(",", ".").replace(/[^0-9.\-eE]/g, "");
  if (s === "" || s === "-") return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

const LINHAS_DE_CABECALHO = 2;

export function lerTca(caminho) {
  const linhas = readSheet(caminho);
  const dados = linhas.slice(LINHAS_DE_CABECALHO)
    .filter((r) => r[COLUNAS.code] && r[COLUNAS.name]);

  const alimentos = [];
  const foraDeAlcance = [];

  for (const r of dados) {
    const nome = String(r[COLUNAS.name]).replace(/\s+/g, " ").trim();
    const kcal = num(r[COLUNAS.kcal]);
    if (kcal == null) {
      foraDeAlcance.push({
        id: `tca-${String(r[COLUNAS.code]).trim()}`, nome,
        porque: "a TCA nao publica energia para esta linha",
      });
      continue;
    }

    const micros = {};
    for (const [col, chave] of Object.entries(MICROS)) {
      const v = num(r[col]);
      if (v != null) micros[chave] = v;
    }

    // O açúcar e a gordura saturada ficam só na coluna. São lidos daqui para a preencher,
    // logo abaixo, e depois saem do mapa: não têm referência da EFSA, não aparecem nos
    // ecrãs de micronutrientes, e guardá-los nos dois sítios era escrever o mesmo número
    // duas vezes em mil trezentos e setenta e seis linhas.
    const soColuna = { sugars_g: micros.sugars_g, satFat_g: micros.satFat_g };

    alimentos.push({
      id: `tca-${String(r[COLUNAS.code]).trim()}`,
      source: "SEED",
      sourceRef: `INSA TCA v${VERSAO_TCA}`,
      nameEn: nome,
      namePt: nome,
      brand: null,
      kcal: Math.round(kcal),
      proteinG: num(r[COLUNAS.protein]) ?? 0,
      carbsG: num(r[COLUNAS.carbs]) ?? 0,
      fatG: num(r[COLUNAS.fat]) ?? 0,
      sugarsG: soColuna.sugars_g ?? null,
      satFatG: soColuna.satFat_g ?? null,
      fiberG: micros.fiber_g ?? null,

      // Sem arredondar: até à v27 o sódio era um inteiro na linha do alimento e um decimal
      // no mapa de micronutrientes, e 29 alimentos mostravam números diferentes conforme o
      // ecrã que os lia. Agora há uma casa só, e é a que a fonte publicou.
      sodiumMg: micros.sodium_mg ?? null,
      micros: (() => {
        delete micros.sugars_g;
        delete micros.satFat_g;
        return Object.keys(micros).length ? micros : null;
      })(),
      servingName: null,
      servingGrams: null,
      verified: true,
      origin: "TCA",
      familia: familiaDeTca(r[COLUNAS.nivel1], r[COLUNAS.name]),
      derivado: null,
    });
  }

  return { declarados: dados.length, alimentos, foraDeAlcance };
}
