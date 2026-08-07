
import { readFileSync, writeFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { catalogue, curated, propose, words } from "./match-pt.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const OUT = join(HERE, "..", "..", "composeApp", "src", "commonMain",
  "composeResources", "files", "seed_pt_micros.json");

const overrides = JSON.parse(readFileSync(join(HERE, "pt-micro-overrides.json"), "utf8"));
const byId = new Map(catalogue.map((e) => [e.id, e]));
const curatedById = new Map(curated.map((e) => [e.id, e]));

let fatal = 0;
const fail = (msg) => { console.error("  ERRO: " + msg); fatal++; };
const warnings = [];
const warn = (msg) => warnings.push(msg);

for (const [cid, did] of Object.entries(overrides.map ?? {})) {
  if (!curatedById.has(cid)) fail(`curado inexistente: ${cid}`);
  const d = byId.get(did);
  if (!d) fail(`dador inexistente: ${cid} -> ${did}`);
  else if (!d.micros || !Object.keys(d.micros).length) fail(`dador sem micros: ${cid} -> ${did}`);
}
for (const [cid, spec] of Object.entries(overrides.proxy ?? {})) {
  if (!curatedById.has(cid)) fail(`curado inexistente (proxy): ${cid}`);
  const d = byId.get(spec.donor);
  if (!d) fail(`dador inexistente (proxy): ${cid} -> ${spec.donor}`);
  else if (!d.micros) fail(`dador sem micros (proxy): ${cid} -> ${spec.donor}`);
  if (!spec.why) fail(`proxy sem justificação: ${cid}`);
}
if (fatal) {
  console.error(`\n${fatal} erro(s) nas correções à mão — nada foi escrito.`);
  process.exit(1);
}

const recipes = JSON.parse(readFileSync(join(HERE, "pt-recipes.json"), "utf8"));

const withMicros = catalogue.filter((e) => e.micros && Object.keys(e.micros).length);

function resolveIngredient(query) {
  const qw = words(query);
  if (!qw.length) return null;
  const hits = withMicros.filter((d) => {
    const dw = new Set(words(d.nameEn));
    return qw.every((w) => dw.has(w));
  });
  if (!hits.length) return null;

  return hits.sort((a, b) =>
    a.nameEn.length - b.nameEn.length || a.id.localeCompare(b.id))[0];
}

const RECIPE_KCAL_TOLERANCE = 0.3;

const WATER = "__agua__";

function buildFromRecipe(c, spec) {
  const parts = [];
  let waterG = 0;
  for (const ing of spec.ingredients) {
    if (ing.q === WATER) { waterG += ing.g; continue; }
    const d = resolveIngredient(ing.q);
    if (!d) { fail(`receita ${c.id}: ingrediente não resolvido "${ing.q}"`); return null; }
    parts.push({ d, g: ing.g });
  }
  const totalG = parts.reduce((s, p) => s + p.g, 0) + waterG;
  if (totalG <= 0) { fail(`receita ${c.id}: gramas totais inválidas`); return null; }

  const kcal100 = parts.reduce((s, p) => s + p.d.kcal * p.g, 0) / totalG;
  const drift = Math.abs(kcal100 - c.kcal) / c.kcal;
  if (drift > RECIPE_KCAL_TOLERANCE) {
    fail(`receita ${c.id} (${c.namePt}): ${Math.round(kcal100)} kcal/100g vs ` +
      `${c.kcal} declaradas (${(drift * 100).toFixed(0)}% de desvio)`);
    return null;
  }

  const micros = {};
  for (const p of parts) {
    for (const [k, v] of Object.entries(p.d.micros)) {
      micros[k] = (micros[k] ?? 0) + (v * p.g) / 100;
    }
  }

  const f = 100 / totalG;
  for (const k of Object.keys(micros)) {
    micros[k] = Math.round(micros[k] * f * 1000) / 1000;
  }
  return micros;
}

const out = {};
const source = {};
const rejected = new Set(Object.keys(overrides.reject ?? {}));

for (const c of curated) {
  if (c.micros) continue;
  if (rejected.has(c.id)) continue;

  const q = overrides.query?.[c.id];
  if (q) {
    const d = resolveIngredient(q);
    if (!d) { warn(`não resolve: ${c.namePt} ("${q}")`); continue; }
    const drift = Math.abs(d.kcal - c.kcal) / c.kcal;
    if (drift > 0.3) {
      warn(`kcal fora: ${c.namePt} (${c.kcal}) vs ${d.nameEn} (${d.kcal}) — ${(drift * 100).toFixed(0)}%`);
      continue;
    }
    out[c.id] = d.micros;
    source[c.id] = { donor: d.id, name: d.nameEn, how: "query" };
    continue;
  }

  const recipe = recipes[c.id];
  if (recipe) {
    const micros = buildFromRecipe(c, recipe);
    if (micros && Object.keys(micros).length) {
      out[c.id] = micros;
      source[c.id] = { how: "recipe", name: `${recipe.ingredients.length} ingredientes` };
    }
    continue;
  }

  let donorId = overrides.map?.[c.id] ?? overrides.proxy?.[c.id]?.donor ?? null;
  let how = donorId ? (overrides.map?.[c.id] ? "manual" : "proxy") : null;

  if (!donorId) {
    const p = propose(c);
    if (!p) continue;
    donorId = p.d.id;
    how = "auto";
  }
  const d = byId.get(donorId);
  if (!d?.micros) continue;

  out[c.id] = d.micros;
  source[c.id] = { donor: donorId, name: d.nameEn, how };
}

if (fatal) {
  console.error(`\n${fatal} erro(s) nas receitas — nada foi escrito.`);
  process.exit(1);
}

writeFileSync(OUT, JSON.stringify(out));

const total = curated.filter((c) => !c.micros).length;
const by = (h) => Object.values(source).filter((s) => s.how === h).length;
console.log(`curados sem micros : ${total}`);
console.log(`  resolvidos       : ${Object.keys(out).length}` +
  `  (auto ${by("auto")} · à mão ${by("manual")} · proxy ${by("proxy")})`);
console.log(`  recusados à mão  : ${rejected.size}`);
console.log(`  ainda por tratar : ${total - Object.keys(out).length - rejected.size}`);
console.log(`\ncobertura dos curados: ${((Object.keys(out).length / total) * 100).toFixed(1)}%`);
console.log(`escrito -> ${OUT}`);

if (warnings.length) {
  console.log(`\n--- ${warnings.length} propostas recusadas pela validação ---`);
  for (const w of warnings) console.log("  · " + w);
}
