/**
 * Põe o catálogo construído ao lado do que o telemóvel tem, e diz onde diferem.
 *
 * É a medição que decide se os dezoito passos do `FoodSeeder` se podem apagar. Enquanto
 * houver diferença que não esteja explicada — um alimento novo, um nome corrigido — os
 * passos ficam, porque apagá-los seria apagar decisões que ninguém volta a tomar.
 *
 * Corre-se depois de `extrair-do-telemovel.mjs` e de `construir.mjs`.
 */
import { DatabaseSync } from "node:sqlite";
import { readFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const RAIZ = join(HERE, "..", "..");
const DB = join(RAIZ, "extracao", "antares.db");
const CATALOGO = join(RAIZ, "composeApp", "src", "commonMain", "composeResources", "files", "catalogo.json");

if (!existsSync(DB)) { console.error(`Falta ${DB}.`); process.exit(1); }

const db = new DatabaseSync(DB);
const noTelemovel = new Map();
for (const l of db.prepare(
  "select id, namePt, nameEn, kcal, proteinG, carbsG, fatG, isLiquid, verified, microsJson " +
  "from foods where deleted = 0",
).all()) noTelemovel.set(l.id, l);

const { alimentos } = JSON.parse(readFileSync(CATALOGO, "utf8"));
const noFicheiro = new Map(alimentos.map((a) => [a.id, a]));

const novos = alimentos.filter((a) => !noTelemovel.has(a.id));
const perdidos = [...noTelemovel.keys()].filter((id) => !noFicheiro.has(id));

console.log(`no telemóvel: ${noTelemovel.size}`);
console.log(`no ficheiro:  ${noFicheiro.size}`);
console.log(`  novos:    ${novos.length}`);
console.log(`  perdidos: ${perdidos.length}`);

if (perdidos.length) {
  console.log("\n-- perdidos (estavam no telemóvel e não vêm no ficheiro) --");
  for (const id of perdidos.slice(0, 30)) {
    const l = noTelemovel.get(id);
    console.log(`  ${id}  ${l.namePt}`);
  }
  if (perdidos.length > 30) console.log(`  … e mais ${perdidos.length - 30}`);
}

const CAMPOS = ["namePt", "nameEn", "kcal", "proteinG", "carbsG", "fatG", "isLiquid", "verified"];
const diferencas = {};
const exemplos = {};

for (const a of alimentos) {
  const l = noTelemovel.get(a.id);
  if (!l) continue;
  for (const c of CAMPOS) {
    const noF = c === "isLiquid" || c === "verified" ? Number(Boolean(a[c])) : a[c];
    const noT = c === "isLiquid" || c === "verified" ? Number(l[c]) : l[c];
    const igual = typeof noF === "number" && typeof noT === "number"
      ? Math.abs(noF - noT) < 0.001
      : noF === noT;
    if (igual) continue;
    diferencas[c] = (diferencas[c] || 0) + 1;
    (exemplos[c] ??= []).push(`${a.id}: telemóvel=${JSON.stringify(noT)} ficheiro=${JSON.stringify(noF)}`);
  }
}

console.log("\n-- diferenças campo a campo, nos que existem dos dois lados --");
if (!Object.keys(diferencas).length) console.log("  nenhuma.");
for (const [c, n] of Object.entries(diferencas).sort((a, b) => b[1] - a[1])) {
  console.log(`  ${c}: ${n}`);
  for (const e of exemplos[c].slice(0, 5)) console.log(`      ${e}`);
}

const semMicrosNoFicheiro = alimentos.filter((a) => a.micros == null).length;
const semMicrosNoTelemovel = [...noTelemovel.values()].filter((l) => l.microsJson == null).length;
console.log(`\nsem micros — telemóvel ${semMicrosNoTelemovel}, ficheiro ${semMicrosNoFicheiro}`);
