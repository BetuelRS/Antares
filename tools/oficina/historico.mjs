/**
 * Quantas vezes cada alimento foi registado, tirado do telemóvel.
 *
 *     adb shell am force-stop com.antares.app
 *     adb exec-out run-as com.antares.app cat databases/antares.db > extracao/antares.db
 *     node tools/oficina/historico.mjs
 *
 * É o que dá ordem à fila da oficina, e é a única coisa que a torna útil: sem ele, as
 * primeiras horas de curadoria caem em cogumelos shiitake enlatados em vez de caírem no pão
 * que se come todos os dias.
 *
 * **Não entra no git, e isso não é descuido.** São só contagens — nem o que se comeu nem
 * quando —, mas uma contagem por alimento diz na mesma o que a pessoa come, e este
 * repositório é público. Fica em `.gitignore`, como a base de onde saiu.
 *
 * A oficina funciona sem ele: a fila cai para o número de achados. O que se perde é a ordem
 * boa, não a ferramenta.
 */
import { DatabaseSync } from "node:sqlite";
import { writeFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const RAIZ = join(HERE, "..", "..");
const DB = join(RAIZ, "extracao", "antares.db");
export const SAIDA = join(HERE, "historico.json");

if (!existsSync(DB)) {
  console.error(`Falta ${DB} — ver o cabeçalho deste ficheiro.`);
  process.exit(1);
}

const db = new DatabaseSync(DB);

// Os apagados contam à mesma. Um alimento registado e depois apagado foi na mesma um alimento
// que alguém procurou e encontrou, e é isso que a ordem da fila está a medir.
const linhas = db
  .prepare("SELECT foodId, COUNT(*) AS vezes FROM food_log GROUP BY foodId")
  .all();

const contagens = {};
for (const l of linhas) if (l.foodId) contagens[String(l.foodId)] = Number(l.vezes);

// Por ordem de identificador, para o `git diff` dizer o que mudou e não onde mudou.
const ordenado = {};
for (const k of Object.keys(contagens).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0))) {
  ordenado[k] = contagens[k];
}

writeFileSync(SAIDA, JSON.stringify(ordenado, null, 2) + "\n");
console.log(`${Object.keys(ordenado).length} alimentos registados → ${SAIDA}`);
