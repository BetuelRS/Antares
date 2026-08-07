
export type Micros = Record<string, number>;

export const USDA_MICRO_IDS: Record<number, string> = {
  1106: "vitA_ug",
  1165: "vitB1_mg",
  1166: "vitB2_mg",
  1167: "vitB3_mg",
  1170: "vitB5_mg",
  1175: "vitB6_mg",
  1177: "vitB9_ug",
  1178: "vitB12_ug",
  1162: "vitC_mg",
  1114: "vitD_ug",
  1109: "vitE_mg",
  1185: "vitK_ug",
  1087: "calcium_mg",
  1089: "iron_mg",
  1090: "magnesium_mg",
  1091: "phosphorus_mg",
  1092: "potassium_mg",
  1095: "zinc_mg",
  1098: "copper_mg",
  1100: "iodine_ug",
  1101: "manganese_mg",
  1103: "selenium_ug",
};

export const USDA_MACRO_IDS = {
  kcal: 1008,
  protein: 1003,
  fat: 1004,
  carbs: 1005,
  sugars: 2000,
  satFat: 1258,
  fiber: 1079,
  sodiumMg: 1093,
} as const;

export const OFF_MICRO_KEYS: Record<string, string> = {
  "vitamin-a": "vitA_ug",
  "vitamin-b1": "vitB1_mg",
  "vitamin-b2": "vitB2_mg",
  "vitamin-pp": "vitB3_mg",
  "vitamin-b3": "vitB3_mg",
  "pantothenic-acid": "vitB5_mg",
  "vitamin-b6": "vitB6_mg",
  "vitamin-b9": "vitB9_ug",
  "folates": "vitB9_ug",
  "vitamin-b12": "vitB12_ug",
  "vitamin-c": "vitC_mg",
  "vitamin-d": "vitD_ug",
  "vitamin-e": "vitE_mg",
  "vitamin-k": "vitK_ug",
  "calcium": "calcium_mg",
  "iron": "iron_mg",
  "magnesium": "magnesium_mg",
  "phosphorus": "phosphorus_mg",
  "potassium": "potassium_mg",
  "zinc": "zinc_mg",
  "copper": "copper_mg",
  "iodine": "iodine_ug",
  "manganese": "manganese_mg",
  "selenium": "selenium_ug",
};

export function factorForKey(key: string): number {
  if (key.endsWith("_ug")) return 1_000_000;
  if (key.endsWith("_mg")) return 1_000;
  return 1;
}

export function cleanMicros(raw: Record<string, number | null | undefined>): Micros | undefined {
  const out: Micros = {};
  for (const [k, v] of Object.entries(raw)) {
    if (v == null || !Number.isFinite(v) || v <= 0) continue;
    out[k] = Math.round(v * 1000) / 1000;
  }
  return Object.keys(out).length ? out : undefined;
}

export function usdaMicros(foodNutrients: any[]): Micros | undefined {
  const raw: Record<string, number> = {};
  for (const n of foodNutrients ?? []) {
    const key = USDA_MICRO_IDS[n?.nutrientId];
    if (!key) continue;
    const v = typeof n?.value === "number" ? n.value : Number(n?.amount);
    if (Number.isFinite(v)) raw[key] = v;
  }
  return cleanMicros(raw);
}

export function offMicros(nutriments: any): Micros | undefined {
  if (!nutriments) return undefined;
  const raw: Record<string, number> = {};
  for (const [suffix, key] of Object.entries(OFF_MICRO_KEYS)) {
    const v = nutriments[`${suffix}_100g`];
    if (typeof v !== "number" || !Number.isFinite(v)) continue;
    raw[key] = v * factorForKey(key);
  }
  return cleanMicros(raw);
}
