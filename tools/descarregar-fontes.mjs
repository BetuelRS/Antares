/**
 * Descarrega as tabelas públicas do USDA de que o oleoduto precisa.
 *
 *     node tools/descarregar-fontes.mjs
 *
 * **As fontes não se versionam.** É a mesma regra que já vale para a CIQUAL e para a TCA: o
 * repositório guarda o que se deriva delas, não uma cópia delas. A diferença é que estas têm
 * endereço directo, e por isso não é preciso pedir a ninguém que vá a um portal buscá-las.
 *
 * Todas de domínio público, e todas do governo dos Estados Unidos:
 *
 * - **Table of Nutrient Retention Factors, Release 6** — quanto de cada nutriente sobrevive a
 *   cada preparação. 26 nutrientes, 270 preparações, 13 grupos de alimento.
 * - **Table of Cooking Yields for Meat and Poultry, Release 2** — quanto peso se perde. Só
 *   carne e aves, que é o que existe medido.
 * - **FoodData Central, SR Legacy** — de onde já vinham os alimentos `usda-*`. Traz-se a
 *   tabela de categorias, que diz a que grupo pertence cada alimento, e a de porções, que é
 *   a única fonte de pesos domésticos com correspondência exacta ao que já temos.
 *
 * Nada disto se estima: se um endereço deixar de responder, o comando falha e diz qual. Um
 * ficheiro em falta a passar por ficheiro vazio era o oleoduto a deixar de encontrar coisas
 * sem ninguém saber porquê.
 */
import { writeFileSync, mkdirSync, existsSync, rmSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));

const FICHEIROS = [
  {
    url: "https://ndownloader.figshare.com/files/44488754",
    destino: join(HERE, "confecao", "data", "retencao.csv"),
    o_que: "retenção de nutrientes (USDA, Release 6)",
  },
  {
    url: "https://ndownloader.figshare.com/files/44488757",
    destino: join(HERE, "confecao", "data", "retencao-dicionario.csv"),
    o_que: "dicionário da retenção",
  },
  {
    url: "https://ndownloader.figshare.com/files/43365402",
    destino: join(HERE, "confecao", "data", "rendimentos.csv"),
    o_que: "rendimentos de confeção (USDA, Release 2)",
  },
  {
    url: "https://ndownloader.figshare.com/files/43365405",
    destino: join(HERE, "confecao", "data", "rendimentos-dicionario.csv"),
    o_que: "dicionário dos rendimentos",
  },
];

/** O pacote do SR Legacy, de onde só se aproveitam três ficheiros dos dezanove. */
const PACOTE = {
  url: "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip",
  pasta: "FoodData_Central_sr_legacy_food_csv_2018-04",
  extrair: [
    { nome: "food.csv", destino: join(HERE, "confecao", "data") },
    { nome: "food_category.csv", destino: join(HERE, "confecao", "data") },

    // Os 36 MB da tabela de nutrientes. E de la que saem a agua, o fosforo, o
    // colesterol e o alcool que a extracao antiga da USDA nao trouxe.
    { nome: "food_nutrient.csv", destino: join(HERE, "confecao", "data") },
    { nome: "nutrient.csv", destino: join(HERE, "confecao", "data") },
    { nome: "food_portion.csv", destino: join(HERE, "porcoes", "data") },
    { nome: "measure_unit.csv", destino: join(HERE, "porcoes", "data") },
  ],
};

async function baixar(url, destino, o_que) {
  mkdirSync(dirname(destino), { recursive: true });
  const r = await fetch(url, { redirect: "follow" });
  if (!r.ok) throw new Error(`${o_que}: ${url} respondeu ${r.status}`);
  const bytes = Buffer.from(await r.arrayBuffer());
  if (!bytes.length) throw new Error(`${o_que}: veio vazio`);
  writeFileSync(destino, bytes);
  console.log(`  ${(bytes.length / 1024).toFixed(0).padStart(6)} kB  ${o_que}`);
  return bytes.length;
}

console.log("a descarregar as tabelas públicas do USDA…\n");

for (const f of FICHEIROS) await baixar(f.url, f.destino, f.o_que);

const zip = join(HERE, "sr-legacy.zip");
await baixar(PACOTE.url, zip, "FoodData Central, SR Legacy (pacote)");

for (const { nome, destino } of PACOTE.extrair) {
  mkdirSync(destino, { recursive: true });

  // O `unzip` do sistema, e não uma biblioteca: são seis ficheiros de um pacote de seis
  // megabytes, e uma dependência nova para isto era pagar caro por pouco.
  execFileSync("unzip", ["-o", "-j", zip, `${PACOTE.pasta}/${nome}`, "-d", destino], {
    stdio: "ignore",
  });
  if (!existsSync(join(destino, nome))) throw new Error(`o pacote não trazia ${nome}`);
  console.log(`          extraído  ${nome}`);
}

rmSync(zip);
console.log("\nfeito. Agora: node tools/confecao/construir.mjs && node tools/catalogo/construir.mjs");
