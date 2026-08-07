
import { readFileSync } from "node:fs";
import { join } from "node:path";

const SEED = join("..", "..", "composeApp", "src", "commonMain", "composeResources", "files");
const read = (f) => JSON.parse(readFileSync(join(SEED, f), "utf8"));

export const catalogue = read("seed_foods.json");
export const curated = [
  ...read("seed_foods_pt.json"),
  ...read("seed_foods_pt2.json"),
  ...read("seed_foods_pt3.json"),
].filter((e) => !String(e.id).startsWith("pt-"));

const NOISE = new Set([
  "and", "or", "with", "without", "the", "of", "in", "type", "average", "all",
  "prepacked", "commercially", "prepared", "includes", "foods", "usda", "food",
  "distribution", "program", "added", "salt", "no", "var", "kind", "whole",
]);

const sing = (w) =>
  w.length > 3 && w.endsWith("ies") ? w.slice(0, -3) + "y"
  : w.length > 3 && w.endsWith("s") && !w.endsWith("ss") ? w.slice(0, -1)
  : w;

export const words = (s) =>
  s.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9\s]/g, " ").split(/\s+/).filter(Boolean)
    .map(sing).filter((w) => w.length > 2 && !NOISE.has(w));

const COOKED = /\b(cooked|boiled|baked|roasted|grilled|fried|steamed|stewed|braised|toasted)\b/i;
const RAW = /\b(raw|fresh|uncooked|unprepared|sprouted|dried)\b/i;

function prepCompatible(a, b) {
  const aC = COOKED.test(a), aR = RAW.test(a);
  const bC = COOKED.test(b), bR = RAW.test(b);
  if (aC && bR && !bC) return false;
  if (aR && bC && !bR) return false;
  return true;
}

const FORTIFIED = /fortified|enriched|with added vitamin/i;

const kcalOk = (a, b, tol = 0.25) => a > 0 && b > 0 && Math.abs(a - b) / a <= tol;

function macrosOk(c, d) {
  const f = (x, y) => Math.abs((x ?? 0) - (y ?? 0));
  return f(c.proteinG, d.proteinG) <= 4 && f(c.carbsG, d.carbsG) <= 8 && f(c.fatG, d.fatG) <= 5;
}

export function macroDistance(c, d) {
  const f = (x, y) => Math.abs((x ?? 0) - (y ?? 0));
  return f(c.proteinG, d.proteinG) + f(c.carbsG, d.carbsG) + f(c.fatG, d.fatG) +
    Math.abs(c.kcal - d.kcal) / 20;
}

export const donors = catalogue.filter((e) => e.micros && Object.keys(e.micros).length);
const byWord = new Map();
for (const d of donors) {
  d._w = new Set(words(d.nameEn));
  for (const w of d._w) {
    if (!byWord.has(w)) byWord.set(w, []);
    byWord.get(w).push(d);
  }
}

export function propose(c) {
  const cw = words(c.nameEn);
  if (cw.length === 0) return null;

  const rarest = cw.map((w) => byWord.get(w) ?? []).sort((a, b) => a.length - b.length)[0] ?? [];
  return rarest
    .filter((d) =>
      cw.every((w) => d._w.has(w)) &&
      prepCompatible(c.nameEn, d.nameEn) &&
      kcalOk(c.kcal, d.kcal) &&
      macrosOk(c, d) &&
      !FORTIFIED.test(d.nameEn)
    )
    .map((d) => ({ d, dist: macroDistance(c, d) }))
    .sort((a, b) => a.dist - b.dist)[0] ?? null;
}

if (import.meta.url.endsWith(process.argv[1]?.replace(/\\/g, "/") ?? "")) {
  const showAll = process.argv.includes("--all");
  let ok = 0;
  const missing = [];
  for (const c of curated) {
    if (c.micros) continue;
    const p = propose(c);
    if (!p) { missing.push(c); continue; }
    ok++;
    if (showAll) {
      console.log(`${c.id}\t${c.namePt} (${c.kcal})\t->\t${p.d.id}\t${p.d.nameEn} (${p.d.kcal})\td=${p.dist.toFixed(1)}`);
    }
  }
  console.log(`\npropostos: ${ok} | sem proposta: ${missing.length} | total ${ok + missing.length}`);
  if (!showAll) {
    console.log("\n--- sem proposta ---");
    for (const m of missing) console.log(`  ${m.id}\t${m.namePt}\t${m.nameEn}\t${m.kcal}`);
  }
}
