
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const DATA = join(HERE, "data");
const SEED_DIR = join(HERE, "..", "..", "composeApp", "src", "commonMain", "composeResources", "files");
const OUT = join(SEED_DIR, "seed_foods.json");

const MACRO_BY_CODE = {
  328: "kcal",
  25000: "proteinG",
  31000: "carbsG",
  32000: "sugarsG",
  40000: "fatG",
  40302: "satFatG",
  34100: "fiberG",
  10110: "sodiumMg",
};

const MICRO_BY_CODE = {
  10120: "magnesium_mg", 10150: "phosphorus_mg", 10190: "potassium_mg",
  10200: "calcium_mg", 10251: "manganese_mg", 10260: "iron_mg",
  10290: "copper_mg", 10300: "zinc_mg", 10340: "selenium_ug",
  10530: "iodine_ug",
  51104: "vitA_ug", 52100: "vitD_ug", 53100: "vitE_mg", 54101: "vitK_ug",
  55100: "vitC_mg", 56100: "vitB1_mg", 56200: "vitB2_mg", 56310: "vitB3_mg",
  56400: "vitB5_mg", 56500: "vitB6_mg", 56600: "vitB12_ug", 56700: "vitB9_ug",

  400: "water_g", 60000: "alcohol_g", 75100: "cholesterol_mg",
  40303: "fatMono_g", 40304: "fatPoly_g",

  10170: "chloride_mg",

  // Os ácidos gordos ficam nas gramas em que a CIQUAL os publica, e a chave diz `_g` por
  // isso. Convertê-los para miligramas aqui era o erro de mil vezes que nenhum teste apanha
  // depois de gravado.
  41833: "omega3_g", 41826: "omega6_g", 42053: "epa_g", 42263: "dha_g",

  33110: "starch_g", 32410: "lactose_g", 34000: "polyols_g",

  // Não somam ao `vitA_ug`, que já é o equivalente de retinol: são a resposta a de onde
  // veio, e essa não se tira do total.
  51200: "retinol_ug", 51330: "betaCarotene_ug",
};

function parseBlocks(xml, tag) {
  const out = [];
  const re = new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`, "g");
  for (const m of xml.matchAll(re)) {
    const obj = {};
    for (const f of m[1].matchAll(/<(\w+)>([\s\S]*?)<\/\1>/g)) {
      obj[f[1]] = decode(f[2].trim());
    }
    out.push(obj);
  }
  return out;
}

function decode(s) {
  return s
    .replace(/&lt;/g, "<").replace(/&gt;/g, ">")
    .replace(/&apos;/g, "'").replace(/&quot;/g, '"')
    .replace(/&amp;/g, "&");
}

function teneur(raw, { tracesAsZero = false } = {}) {
  if (raw == null) return null;
  const s = String(raw).trim();
  if (!s || s === "-") return null;
  if (s === "traces" || s.startsWith("<")) return tracesAsZero ? 0 : null;
  const n = parseFloat(s.replace(/\s/g, "").replace(",", "."));
  return Number.isFinite(n) ? n : null;
}

function atwater(m) {
  if (m.proteinG == null || m.carbsG == null || m.fatG == null) return null;
  return 4 * m.proteinG + 4 * m.carbsG + 9 * m.fatG;
}

const round = (v, d = 1) => (v == null ? null : Math.round(v * 10 ** d) / 10 ** d);

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

function matchKey(name) {
  const words = name
    .toLowerCase()
    .normalize("NFD").replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9\s]/g, " ")
    .split(/\s+/)
    .filter(Boolean)
    .map(singular)
    .filter((w) => w.length > 2 && !STOP.has(w));
  return words.sort().join(" ");
}

const LAB_MARKERS = /separable|trimmed to|all grades|bone-?in|boneless|denuded|frenched|untrimmed|choice|select|prime grade|composite of|includes usda|retail cuts?|refuse|yield from|raw, ?nfs/i;

function isLabDescriptor(name) {
  const commas = (name.match(/,/g) || []).length;
  return LAB_MARKERS.test(name) || commas >= 4 || name.length > 90;
}

const SANITY = 0.4;
function kcalAgrees(a, b) {
  if (!a || !b) return false;
  return Math.abs(a - b) / b <= SANITY;
}

for (const f of ["alim.xml", "compo.xml"]) {
  if (!existsSync(join(DATA, f))) {
    console.error(`Falta ${join(DATA, f)} — descarregar da CIQUAL; ver tools/README.md.`);
    process.exit(1);
  }
}

console.log("a ler CIQUAL…");
const alims = parseBlocks(readFileSync(join(DATA, "alim.xml"), "utf8"), "ALIM");
const compos = parseBlocks(readFileSync(join(DATA, "compo.xml"), "utf8"), "COMPO");
console.log(`  ${alims.length} alimentos, ${compos.length} valores de composição`);

const byFood = new Map();
for (const c of compos) {
  const code = c.alim_code;
  const constCode = Number(c.const_code);
  const macro = MACRO_BY_CODE[constCode];
  const micro = MICRO_BY_CODE[constCode];
  if (!macro && !micro) continue;
  const v = teneur(c.teneur, { tracesAsZero: Boolean(macro) });
  if (v == null) continue;
  let rec = byFood.get(code);
  if (!rec) { rec = { macros: {}, micros: {} }; byFood.set(code, rec); }
  if (macro) rec.macros[macro] = v;
  if (micro) rec.micros[micro] = v;
}

const entries = [];
let skippedNoMacros = 0;

for (const a of alims) {
  const rec = byFood.get(a.alim_code);
  if (!rec) { skippedNoMacros++; continue; }
  const m = rec.macros;
  if (m.kcal == null) m.kcal = atwater(m);

  if (m.kcal == null || m.proteinG == null || m.carbsG == null || m.fatG == null) {
    skippedNoMacros++;
    continue;
  }
  const micros = {};
  for (const [k, v] of Object.entries(rec.micros)) {
    const r = round(v, 3);
    if (r != null && r > 0) micros[k] = r;
  }
  const nameEn = a.alim_nom_eng || a.alim_nom_fr;
  entries.push({
    id: `ciqual-${a.alim_code}`,
    source: "SEED",
    sourceRef: a.alim_code,
    nameEn,

    namePt: nameEn,
    brand: null,
    kcal: Math.round(m.kcal),
    proteinG: round(m.proteinG),
    carbsG: round(m.carbsG),
    sugarsG: round(m.sugarsG),
    fatG: round(m.fatG),
    satFatG: round(m.satFatG),
    fiberG: round(m.fiberG),
    sodiumMg: m.sodiumMg == null ? null : Math.round(m.sodiumMg),
    micros: Object.keys(micros).length ? micros : null,
    servingName: null,
    servingGrams: null,

    verified: true,
    origin: "CIQUAL",
  });
}
console.log(`  ${entries.length} utilizáveis (${skippedNoMacros} sem macros suficientes)`);

const usdaPath = join(HERE, "usda-source.json");
let usda = [];
if (existsSync(usdaPath)) {
  usda = JSON.parse(readFileSync(usdaPath, "utf8"));
} else {
  console.log("  (sem usda-source.json — a saltar enriquecimento)");
}

const usdaByKey = new Map();
const usdaByShort = new Map();
for (const u of usda) {
  const k = matchKey(u.nameEn);
  if (!k) continue;
  if (!usdaByKey.has(k)) usdaByKey.set(k, u);
  const short = k.split(" ").slice(0, 2).join(" ");
  if (short && !usdaByShort.has(short)) usdaByShort.set(short, u);
}

let enriched = 0, filled = 0;
const usedUsda = new Set();

for (const e of entries) {
  const key = matchKey(e.nameEn);
  const short = key.split(" ").slice(0, 2).join(" ");
  const cand = usdaByKey.get(key) ?? usdaByShort.get(short);
  if (!cand || !kcalAgrees(cand.kcal, e.kcal)) continue;
  usedUsda.add(cand.id);
  let touched = false;

  if (cand.micros) {
    const micros = e.micros ? { ...e.micros } : {};
    for (const [k, v] of Object.entries(cand.micros)) {
      if (micros[k] == null && v > 0) { micros[k] = v; filled++; touched = true; }
    }
    if (Object.keys(micros).length) e.micros = micros;
  }
  for (const f of ["sugarsG", "satFatG", "fiberG", "sodiumMg"]) {
    if (e[f] == null && cand[f] != null) { e[f] = cand[f]; touched = true; }
  }
  if (touched) enriched++;
}
console.log(`  enriquecidos ${enriched} alimentos (${filled} micros preenchidos pelo USDA)`);

const ciqualKeys = new Set(entries.map((e) => matchKey(e.nameEn)));

const curatedNames = new Set();
for (const f of ["seed_foods_pt.json", "seed_foods_pt2.json", "seed_foods_pt3.json"]) {
  const p = join(SEED_DIR, f);
  if (!existsSync(p)) continue;
  for (const e of JSON.parse(readFileSync(p, "utf8"))) {
    if (e.namePt) curatedNames.add(e.namePt.trim().toLowerCase());
  }
}

let tail = 0;
let ptDropped = 0;
for (const u of usda) {

  const curated = String(u.id).startsWith("pt-");
  if (curated && curatedNames.has(String(u.namePt ?? "").trim().toLowerCase())) {
    ptDropped++;
    continue;
  }
  if (!curated) {
    if (usedUsda.has(u.id)) continue;
    if (isLabDescriptor(u.nameEn)) continue;
    if (ciqualKeys.has(matchKey(u.nameEn))) continue;
  }

  entries.push({
    ...u,
    verified: !curated,
    origin: curated ? "PT_EXTRA" : "USDA",
  });
  tail++;
}
console.log(`  cauda USDA mantida: ${tail} (nomes de laboratório descartados)`);
console.log(`  pt-extras descartados por duplicarem um curado: ${ptDropped}`);

let curated = 0;
for (const f of ["seed_foods_pt.json", "seed_foods_pt2.json", "seed_foods_pt3.json"]) {
  const p = join(SEED_DIR, f);
  if (!existsSync(p)) continue;
  for (const e of JSON.parse(readFileSync(p, "utf8"))) {
    entries.push({ ...e, origin: e.origin || "PT" });
    curated++;
  }
}
console.log(`  camada PT curada: ${curated} (ficheiros próprios, não duplicados aqui)`);

entries.sort((a, b) => String(a.id).localeCompare(String(b.id)));

const catalogue = entries.filter((e) => e.origin !== "PT");
const coverOf = (list) => {
  const c = {};
  for (const e of list) for (const k of Object.keys(e.micros || {})) c[k] = (c[k] || 0) + 1;
  return c;
};
const ciqualOnly = catalogue.filter((e) => e.origin === "CIQUAL");
const cover = coverOf(catalogue);
const coverCiq = coverOf(ciqualOnly);

console.log(`\n--- cobertura de micros (catálogo ${catalogue.length}; CIQUAL ${ciqualOnly.length}) ---`);
console.log("           total | só CIQUAL");
for (const [k, n] of Object.entries(cover).sort((a, b) => b[1] - a[1])) {
  const a = ((n / catalogue.length) * 100).toFixed(1).padStart(5);
  const b = (((coverCiq[k] || 0) / ciqualOnly.length) * 100).toFixed(1).padStart(5);
  console.log(`  ${a}% | ${b}%   ${k}`);
}

const withName = catalogue.filter((e) => !isLabDescriptor(e.nameEn)).length;
console.log(`\nnomes apresentáveis: ${((withName / catalogue.length) * 100).toFixed(1)}%`);

const output = entries.filter((e) => e.origin !== "PT");
writeFileSync(OUT, JSON.stringify(output));
console.log(`\nescrito ${output.length} alimentos → ${OUT}`);
console.log(`(${(JSON.stringify(output).length / 1024 / 1024).toFixed(2)} MB)`);
