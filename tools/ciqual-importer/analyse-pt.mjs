
import { readFileSync } from "node:fs";
import { join } from "node:path";

const SEED = join("..", "..", "composeApp", "src", "commonMain", "composeResources", "files");
const read = (f) => JSON.parse(readFileSync(join(SEED, f), "utf8"));

const catalogue = read("seed_foods.json");
const curated = [
  ...read("seed_foods_pt.json"),
  ...read("seed_foods_pt2.json"),
  ...read("seed_foods_pt3.json"),
].filter((e) => !String(e.id).startsWith("pt-"));

const STOP = new Set([
  "raw", "cooked", "boiled", "baked", "roasted", "grilled", "fried", "steamed",
  "canned", "drained", "prepacked", "average", "and", "or", "with", "without",
  "the", "of", "in", "type", "fresh", "frozen",
]);

const sing = (w) =>
  w.length > 3 && w.endsWith("ies") ? w.slice(0, -3) + "y"
  : w.length > 3 && w.endsWith("s") && !w.endsWith("ss") ? w.slice(0, -1)
  : w;

const words = (s) =>
  s.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9\s]/g, " ").split(/\s+/).filter(Boolean)
    .map(sing).filter((w) => w.length > 2 && !STOP.has(w));

const key = (s) => words(s).sort().join(" ");

const kcalOk = (a, b, tol = 0.4) => a > 0 && b > 0 && Math.abs(a - b) / a <= tol;

function macrosOk(c, d) {
  const f = (x, y) => Math.abs((x ?? 0) - (y ?? 0));
  return f(c.proteinG, d.proteinG) <= 4 && f(c.carbsG, d.carbsG) <= 8 && f(c.fatG, d.fatG) <= 5;
}

function macroDistance(c, d) {
  const f = (x, y) => Math.abs((x ?? 0) - (y ?? 0));
  return f(c.proteinG, d.proteinG) + f(c.carbsG, d.carbsG) + f(c.fatG, d.fatG) +
    Math.abs(c.kcal - d.kcal) / 20;
}

const donors = catalogue.filter((e) => e.micros && Object.keys(e.micros).length);
const exact = new Map();
for (const d of donors) {
  const k = key(d.nameEn);
  if (k && !exact.has(k)) exact.set(k, d);
}

const byWord = new Map();
for (const d of donors) {
  for (const w of new Set(words(d.nameEn))) {
    if (!byWord.has(w)) byWord.set(w, []);
    byWord.get(w).push(d);
  }
}

function headNoun(nameEn) {
  return words(nameEn)[0] ?? "";
}

function headMatches(head, donorWords) {
  if (!head) return false;

  return donorWords.includes(head);
}

const FORTIFIED = /fortified|enriched|with added vitamin|vitamin[- ]enriched/i;

function bestMatch(c) {
  const head = headNoun(c.nameEn);
  const pool = new Set();
  for (const [dw, list] of byWord) {
    if (headMatches(head, [dw])) for (const d of list) pool.add(d);
  }
  return [...pool]
    .filter((d) =>
      kcalOk(c.kcal, d.kcal, 0.25) &&
      macrosOk(c, d) &&
      headMatches(head, words(d.nameEn)) &&
      !FORTIFIED.test(d.nameEn)
    )
    .map((d) => ({ d, dist: macroDistance(c, d) }))
    .sort((a, b) => a.dist - b.dist)[0];
}

let viaExact = 0, viaFuzzy = 0;
const orphans = [];

for (const c of curated) {
  if (c.micros) continue;
  const k = key(c.nameEn);
  const hit = exact.get(k);
  if (hit && kcalOk(c.kcal, hit.kcal)) { viaExact++; continue; }

  const best = bestMatch(c);

  if (best) { viaFuzzy++; continue; }
  orphans.push(c);
}

const total = curated.filter((c) => !c.micros).length;
console.log(`curados sem micros: ${total}`);
console.log(`  casam exato : ${viaExact}`);
console.log(`  casam aprox.: ${viaFuzzy}`);
console.log(`  sem gémeo   : ${orphans.length}`);
console.log(`\ncobertura possível por emparelhamento: ${(((viaExact + viaFuzzy) / total) * 100).toFixed(1)}%`);
console.log("\n--- órfãos (precisam de receita) ---");
for (const o of orphans) console.log(`  ${o.id}  |  ${o.namePt}  |  ${o.nameEn}`);

console.log("\n--- amostra de emparelhamentos aproximados ---");
let shown = 0;
for (const c of curated) {
  if (c.micros || shown >= 25) continue;
  const k = key(c.nameEn);
  const hit = exact.get(k);
  if (hit && kcalOk(c.kcal, hit.kcal)) continue;
  const best = bestMatch(c);
  if (!best) continue;
  shown++;
  console.log(`  ${c.namePt}  (${c.kcal})  ->  ${best.d.nameEn}  (${best.d.kcal})   [d=${best.dist.toFixed(1)}]`);
}
