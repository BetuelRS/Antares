
import { readSheet } from './xlsx.mjs';
import { writeFileSync } from 'node:fs';

const XLSX = 'tools/tca-importer/data/insa_tca.xlsx';
const OUT = 'composeApp/src/commonMain/composeResources/files/seed_foods_tca.json';

const CONSULTED_ON = '2026-07-29';
const TCA_VERSION = '7.1';

const COLUMNS = {
  code: 0,
  name: 1,
  group1: 2,
  group2: 3,
  group3: 4,
  kcal: 5,
  fat: 7,
  protein: 19,
  carbs: 13,
};

const MICROS = {
  8: 'satFat_g',
  9: 'fatMono_g',
  10: 'fatPoly_g',
  12: 'fatTrans_g',
  14: 'sugars_g',
  18: 'fiber_g',
  20: 'alcohol_g',
  21: 'water_g',
  23: 'cholesterol_mg',
  24: 'vitA_ug',
  32: 'vitD_ug',
  33: 'vitE_mg',
  34: 'vitB1_mg',
  35: 'vitB2_mg',
  36: 'vitB3_mg',
  39: 'vitB6_mg',
  40: 'vitB12_ug',
  41: 'vitC_mg',
  42: 'vitB9_ug',
  44: 'sodium_mg',
  45: 'potassium_mg',
  46: 'calcium_mg',
  47: 'phosphorus_mg',
  48: 'magnesium_mg',
  49: 'iron_mg',
  50: 'zinc_mg',
  51: 'selenium_ug',
  52: 'iodine_ug',
};

const RECUSADAS = {
  6: 'Energia [kJ] — a app usa kcal; converter seria guardar o mesmo duas vezes.',
  11: 'Ácido linoleico — o esquema não tem entrada para ácidos gordos individuais.',
  15: 'Oligossacáridos — sem chave canónica e sem referência dietética.',
  16: 'Amido — idem.',
  17: 'Sal [g] — redundante: o sódio vem medido na coluna 44, e derivar sal de sódio é multiplicar por 2,5.',
  22: 'Ácidos orgânicos — sem chave canónica.',
  25: 'Equivalentes de β-caroteno — já contabilizados na vitamina A (col. 24); somar seria contar duas vezes.',
  26: 'α-caroteno — carotenoide individual, sem chave nem DRV.',
  27: 'β-caroteno total — idem (e ver col. 25).',
  28: 'β-criptoxantina — idem.',
  29: 'Licopeno — idem.',
  30: 'Luteína — idem.',
  31: 'Zeaxantina — idem.',
  37: 'Equivalentes de niacina — usa-se a niacina simples (col. 36) por ser o que o resto do catálogo (CIQUAL, USDA) contém; misturar as duas medidas na mesma chave daria somas incomparáveis.',
  38: 'Triptofano/60 — parte do cálculo dos equivalentes de niacina, não é nutriente autónomo.',
  43: 'Cinza — resíduo analítico, não é nutriente.',
};

const SEM_DADOS_NA_TCA = ['vitK_ug', 'vitB5_mg', 'copper_mg', 'manganese_mg'];

function num(v) {
  if (v === '' || v == null) return null;
  const s = String(v).replace(',', '.').replace(/[^0-9.\-eE]/g, '');
  if (s === '' || s === '-') return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

function cleanName(s) {
  return String(s).replace(/\s+/g, ' ').trim();
}

const linhas = readSheet(XLSX);
const dados = linhas.slice(2).filter((r) => r[COLUMNS.code] && r[COLUMNS.name]);

const foods = [];
let comMicros = 0;
for (const r of dados) {
  const kcal = num(r[COLUMNS.kcal]);
  if (kcal == null) continue;

  const micros = {};
  for (const [col, key] of Object.entries(MICROS)) {
    const v = num(r[col]);

    if (v != null) micros[key] = v;
  }
  if (Object.keys(micros).length > 0) comMicros++;

  const nome = cleanName(r[COLUMNS.name]);
  foods.push({
    id: `tca-${String(r[COLUMNS.code]).trim()}`,
    source: "SEED",
    sourceRef: `INSA TCA v${TCA_VERSION}`,

    namePt: nome,
    nameEn: nome,
    kcal: Math.round(kcal),
    proteinG: num(r[COLUMNS.protein]) ?? 0,
    carbsG: num(r[COLUMNS.carbs]) ?? 0,
    fatG: num(r[COLUMNS.fat]) ?? 0,
    sugarsG: micros["sugars_g"] ?? null,
    satFatG: micros["satFat_g"] ?? null,
    fiberG: micros["fiber_g"] ?? null,
    sodiumMg: micros["sodium_mg"] != null ? Math.round(micros["sodium_mg"]) : null,
    micros,
    verified: true,
  });
}

const out = foods;

writeFileSync(OUT, JSON.stringify(out, null, 1));

console.log(`alimentos: ${foods.length}`);
console.log(`com micros: ${comMicros} (${Math.round((comMicros / foods.length) * 100)}%)`);
console.log(`colunas mapeadas: ${Object.keys(MICROS).length}`);
console.log(`colunas recusadas: ${Object.keys(RECUSADAS).length}`);
console.log(`sem dados na TCA: ${SEM_DADOS_NA_TCA.join(', ')}`);
console.log(`escrito: ${OUT}`);
