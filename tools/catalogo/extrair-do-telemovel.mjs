/**
 * Tira do telemóvel o que os dezoito passos do `FoodSeeder` já produziram.
 *
 * Corre-se **uma vez**, e o resultado é versionado. A razão é que cinco desses passos
 * corrigiram nomes à mão ao longo de meses, e reconstruir o catálogo a partir das fontes
 * devolvia-lhes o nome de laboratório em inglês. O que está aqui é a memória dessas
 * correções: o oleoduto reconstrói tudo o resto, e por cima aplica isto.
 *
 * Não é uma cópia do catálogo. Guarda só o que não se deriva das fontes — o nome
 * apresentado, se é líquido, e se está verificado — e só quando difere do que a fonte diz.
 *
 * Antes de correr: instalar a compilação de depuração, abrir a app, esperar que semeie, e
 *
 *     adb shell am force-stop com.antares.app
 *     adb exec-out run-as com.antares.app cat databases/antares.db > extracao/antares.db
 *
 * O ficheiro da base não entra no git; o que sai daqui entra.
 */
import { DatabaseSync } from "node:sqlite";
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const RAIZ = join(HERE, "..", "..");
const DB = join(RAIZ, "extracao", "antares.db");
const SEEDS = join(RAIZ, "composeApp", "src", "commonMain", "composeResources", "files");
const OUT = join(HERE, "correcoes.json");

if (!existsSync(DB)) {
  console.error(`Falta ${DB} — ver o cabeçalho deste ficheiro.`);
  process.exit(1);
}

const db = new DatabaseSync(DB);

// Sem as marcas todas, a extração apanha o catálogo a meio de uma correção e grava um
// estado que nunca existiu em telemóvel nenhum.
const MARCAS = [
  "seed_foods_imported", "seed_foods_pt_imported", "seed_foods_pt2_imported",
  "seed_foods_pt3_imported", "seed_foods_tca_imported", "usda_names_cleaned",
  "drinks_marked", "usda_names_cleaned_v2", "fts_deduped", "curated_names_restored",
  "curated_dupes_tca_pruned", "curated_dupes_pruned", "analysed_verified",
  "curated_micros_enriched", "usda_names_translated", "micros_widened",
  "catalogue_rebuilt_day", "micros_indexed",
];
const postas = new Set(db.prepare("select key from db_info").all().map((r) => r.key));
const emFalta = MARCAS.filter((m) => !postas.has(m));
if (emFalta.length) {
  console.error(`A sementeira não acabou. Faltam as marcas: ${emFalta.join(", ")}`);
  process.exit(1);
}

// O que a fonte diz, para só guardarmos as diferenças.
const daFonte = new Map();
for (const f of ["seed_foods.json", "seed_foods_pt.json", "seed_foods_pt2.json",
                 "seed_foods_pt3.json", "seed_foods_tca.json"]) {
  const p = join(SEEDS, f);
  if (!existsSync(p)) continue;
  for (const e of JSON.parse(readFileSync(p, "utf8"))) daFonte.set(e.id, e);
}

const linhas = db.prepare(
  "select id, namePt, nameEn, isLiquid, verified from foods where deleted = 0 order by id",
).all();

const nomes = {};
const liquidos = [];
const verificados = [];
let iguais = 0;

for (const l of linhas) {
  const fonte = daFonte.get(l.id);
  if (!fonte || fonte.namePt !== l.namePt) nomes[l.id] = l.namePt;
  else iguais++;
  if (l.isLiquid) liquidos.push(l.id);
  if (l.verified && !(fonte && fonte.verified)) verificados.push(l.id);
}

// Quem foi apagado pelos três passos de poda. Sem esta lista, o oleoduto ressuscitava
// duplicados que já tinham sido decididos.
const vivos = new Set(linhas.map((l) => l.id));
const podados = [...daFonte.keys()].filter((id) => !vivos.has(id)).sort();

const saida = {
  _: "Gerado por extrair-do-telemovel.mjs. Não editar à mão.",
  extraidoDe: "2.3.0",
  total: linhas.length,
  nomes: Object.fromEntries(Object.entries(nomes).sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))),
  liquidos: liquidos.sort(),
  verificados: verificados.sort(),
  podados,
};

writeFileSync(OUT, JSON.stringify(saida, null, 2) + "\n");
console.log(`alimentos no telemóvel: ${linhas.length}`);
console.log(`  nomes iguais à fonte: ${iguais}`);
console.log(`  nomes corrigidos:     ${Object.keys(nomes).length}`);
console.log(`  marcados como líquido: ${liquidos.length}`);
console.log(`  verificados além da fonte: ${verificados.length}`);
console.log(`  podados pelas três podas: ${podados.length}`);
console.log(`\nescrito → ${OUT}`);
