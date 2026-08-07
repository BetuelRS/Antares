
import { createReadStream, readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { createInterface } from "node:readline";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const DATA = join(HERE, "data", "FoodData_Central_sr_legacy_food_csv_2018-04");
const OUT = join(HERE, "..", "..", "composeApp", "src", "commonMain", "composeResources", "files", "seed_foods.json");

function parseCsvLine(line) {
  const out = [];
  let cur = "";
  let inQ = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (inQ) {
      if (ch === '"') {
        if (line[i + 1] === '"') { cur += '"'; i++; } else inQ = false;
      } else cur += ch;
    } else if (ch === '"') inQ = true;
    else if (ch === ",") { out.push(cur); cur = ""; }
    else cur += ch;
  }
  out.push(cur);
  return out;
}

function readCsv(file) {
  const text = readFileSync(join(DATA, file), "utf8");
  const lines = text.split(/\r?\n/).filter((l) => l.length > 0);
  const header = parseCsvLine(lines[0]);
  return lines.slice(1).map((l) => {
    const cells = parseCsvLine(l);
    const row = {};
    header.forEach((h, i) => (row[h] = cells[i]));
    return row;
  });
}

const EXCLUDED_CATEGORIES = new Set(["3", "21", "24", "25", "26", "27"]);

const BRAND_RE = /\b(PILLSBURY|KRAFT|KELLOGG|GENERAL MILLS|POST|QUAKER|NABISCO|HEINZ|CAMPBELL|MCDONALD|BURGER KING|SUBWAY|PIZZA HUT|TACO BELL|KFC|WENDY|DENNY|APPLEBEE|OLIVE GARDEN|CRACKER BARREL|T\.G\.I\.|HOT POCKETS|LEAN POCKETS|OSCAR MAYER|LOUIS RICH|HORMEL|SMITHFIELD|TYSON|PERDUE|BANQUET|STOUFFER|HEALTHY CHOICE|MARIE CALLENDER|DIGIORNO|TOMBSTONE|RED BARON|TOTINO|JIMMY DEAN|BOB EVANS|SARA LEE|ENTENMANN|HOSTESS|LITTLE DEBBIE|DOLLY MADISON|TASTYKAKE|drumstick|KEEBLER|SUNSHINE|PEPPERIDGE FARM|ARCHWAY|MURRAY|FAMOUS AMOS|CHIPS AHOY|OREO|NILLA|RITZ|TRISCUIT|WHEAT THINS|CHEEZ-IT|GOLDFISH|LANCE|SNYDER|ROLD GOLD|FRITO|LAY'S|RUFFLES|DORITOS|TOSTITOS|CHEETOS|PRINGLES|CAPE COD|UTZ|HERR|WISE|GAMESA|MISSION|ORTEGA|OLD EL PASO|CHI-CHI|PACE|TOSTADA|GOYA|LA CHOY|CHUN KING|NISSIN|MARUCHAN|LIPTON|NESTLE|NESQUIK|CARNATION|OVALTINE|SWISS MISS|HERSHEY|MARS|M&M|SNICKERS|TWIX|MILKY WAY|REESE|KIT KAT|BUTTERFINGER|BABY RUTH|ALMOND JOY|MOUNDS|YORK|TWIZZLERS|SKITTLES|STARBURST|LIFE SAVERS|JELL-O|COOL WHIP|DREAM WHIP|MIRACLE WHIP|HELLMANN|BEST FOODS|WISH-BONE|KEN'S|HIDDEN VALLEY|NEWMAN|SILK|ALMOND BREEZE|RICE DREAM|VITASOY|MORNINGSTAR|BOCA|GARDENBURGER|LOMA LINDA|WORTHINGTON|House Foods|Mori-Nu|AZUMAYA|VITASOY|SO DELICIOUS|TOFUTTI|BEN & JERRY|HAAGEN|BREYER|EDY|DREYER|KLONDIKE|POPSICLE|GOOD HUMOR|ESKIMO|WEIGHT WATCHERS|SLIM FAST|ATKINS|SOUTH BEACH|ZONE|POWERBAR|CLIF|LUNA|BALANCE|GATORADE|POWERADE|PROPEL|SOBE|SNAPPLE|ARIZONA|HAWAIIAN PUNCH|HI-C|KOOL-AID|TANG|CRYSTAL LIGHT|COCA-COLA|PEPSI|SPRITE|FANTA|MOUNTAIN DEW|DR PEPPER|7 UP|7-UP|A&W|CANADA DRY|SCHWEPPES|SUNKIST|SHASTA|RC COLA|FOSTERS|BUDWEISER|COORS|MILLER|HEINEKEN|CORONA|GUINNESS|SAM ADAMS|SIERRA NEVADA|SMIRNOFF|BACARDI|ABSOLUT|JACK DANIEL|JIM BEAM|JOHNNIE WALKER|CONTINENTAL MILLS|KRUSTEAZ|BISQUICK|AUNT JEMIMA|MRS|USDA Commodity|DIGIORNO|CELESTE|JENO|SCHOOL)/i;

const foods = readCsv("food.csv")
  .filter((f) => f.data_type === "sr_legacy_food")
  .filter((f) => !EXCLUDED_CATEGORIES.has(f.food_category_id))
  .filter((f) => !BRAND_RE.test(f.description));

console.log(`foods after filter: ${foods.length}`);

const FIELD_BY_ID = {
  1008: "kcal", 1003: "proteinG", 1005: "carbsG", 2000: "sugarsG",
  1004: "fatG", 1258: "satFatG", 1079: "fiberG", 1093: "sodiumMg",
  1106: "vitA_ug", 1165: "vitB1_mg", 1166: "vitB2_mg", 1167: "vitB3_mg",
  1175: "vitB6_mg", 1177: "vitB9_ug", 1178: "vitB12_ug", 1162: "vitC_mg",
  1114: "vitD_ug", 1109: "vitE_mg", 1185: "vitK_ug",
  1087: "calcium_mg", 1089: "iron_mg", 1090: "magnesium_mg", 1095: "zinc_mg",
  1092: "potassium_mg", 1098: "copper_mg", 1103: "selenium_ug",
};
const idToField = new Map(Object.entries(FIELD_BY_ID).map(([id, f]) => [id, f]));

const wanted = new Set(foods.map((f) => f.fdc_id));
const byFood = new Map();

await new Promise((resolve) => {
  const rl = createInterface({ input: createReadStream(join(DATA, "food_nutrient.csv")) });
  let first = true;
  rl.on("line", (line) => {
    if (first) { first = false; return; }

    const cells = parseCsvLine(line);
    const fdcId = cells[1];
    if (!wanted.has(fdcId)) return;
    const field = idToField.get(cells[2]);
    if (!field) return;
    const amount = parseFloat(cells[3]);
    if (Number.isNaN(amount)) return;
    let rec = byFood.get(fdcId);
    if (!rec) { rec = {}; byFood.set(fdcId, rec); }
    rec[field] = amount;
  });
  rl.on("close", resolve);
});

console.log(`byFood entries: ${byFood.size}; idToField size: ${idToField.size}; sample idToField:`, [...idToField.entries()].slice(0, 3));

const DICT = JSON.parse(readFileSync(join(HERE, "pt-dictionary.json"), "utf8"));

const DICT_KEYS = Object.keys(DICT).sort((a, b) => b.length - a.length);

function translate(desc) {
  let out = desc;
  for (const key of DICT_KEYS) {

    const re = new RegExp(`(?<![\\p{L}])${key.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}(?![\\p{L}])`, "giu");
    out = out.replace(re, (m) => {
      const t = DICT[key];

      return m[0] === m[0].toUpperCase() ? t[0].toUpperCase() + t.slice(1) : t;
    });
  }
  return out;
}

const round1 = (v) => (v == null ? null : Math.round(v * 10) / 10);
const MICRO_FIELDS = Object.values(FIELD_BY_ID).filter((f) =>
  !["kcal", "proteinG", "carbsG", "sugarsG", "fatG", "satFatG", "fiberG", "sodiumMg"].includes(f));

const entries = [];
for (const f of foods) {
  const n = byFood.get(f.fdc_id) ?? {};
  if (n.kcal == null || n.proteinG == null || n.carbsG == null || n.fatG == null) continue;
  const micros = {};
  let hasMicros = false;
  for (const mf of MICRO_FIELDS) {
    if (n[mf] != null) { micros[mf] = round1(n[mf]); hasMicros = true; }
  }
  entries.push({
    id: `usda-${f.fdc_id}`,
    source: "SEED",
    sourceRef: f.fdc_id,
    nameEn: f.description,
    namePt: translate(f.description),
    brand: null,
    kcal: Math.round(n.kcal),
    proteinG: round1(n.proteinG),
    carbsG: round1(n.carbsG),
    sugarsG: round1(n.sugarsG),
    fatG: round1(n.fatG),
    satFatG: round1(n.satFatG),
    fiberG: round1(n.fiberG),
    sodiumMg: n.sodiumMg == null ? null : Math.round(n.sodiumMg),
    micros: hasMicros ? micros : null,
    servingName: null,
    servingGrams: null,
    verified: false,
  });
}

const extras = JSON.parse(readFileSync(join(HERE, "pt-extras.json"), "utf8"));
for (const e of extras) entries.push(e);

entries.sort((a, b) => a.id.localeCompare(b.id));
mkdirSync(dirname(OUT), { recursive: true });
writeFileSync(OUT, JSON.stringify(entries));
console.log(`wrote ${entries.length} foods -> ${OUT} (${(JSON.stringify(entries).length / 1024 / 1024).toFixed(2)} MB)`);
