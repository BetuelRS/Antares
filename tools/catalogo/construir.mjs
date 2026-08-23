/**
 * Constrói o catálogo inteiro, a partir das fontes, num ficheiro só.
 *
 * Antes disto, o catálogo era cinco ficheiros semeados por ordem e treze correções que
 * corriam no telemóvel a cada arranque — dezoito passos que não se podiam reordenar nem
 * fundir, e a que cada correção nova acrescentava mais um. **Corrigir um alimento custava
 * uma versão na Play Store.** Passa a custar uma execução disto.
 *
 *     node tools/catalogo/construir.mjs
 *     node tools/catalogo/construir.mjs --aceitar-desvios
 *
 * Sem a segunda opção, um alimento que a fonte declare e que não chegue ao ficheiro **chumba
 * a construção**. Com ela, os desvios são reescritos em `desvios.json` para alguém os ler no
 * `git diff` antes de os aceitar. É a diferença entre perder um alimento e decidir perdê-lo.
 *
 * Determinístico: duas execuções seguidas dão bytes idênticos. Nada de datas, nada de
 * ordens que dependam da tabela de dispersão — é o que torna o `git diff` legível e o que
 * permite ao [CatalogoDeterministicoTest] verificar que ninguém partiu isso.
 */
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { lerCiqual } from "./fontes/ciqual.mjs";
import { lerUsda, chaveDeNome, isLabDescriptor, energiaConcorda } from "./fontes/usda.mjs";
import { lerTca } from "./fontes/tca.mjs";
import { lerCurados, lerMicrosCurados } from "./fontes/curados.mjs";
import { lerVocabulario, conferirComAEfsa } from "./vocabulario.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const RAIZ = join(HERE, "..", "..");
const DADOS = join(HERE, "dados");
const DESVIOS = join(HERE, "desvios.json");
const CORRECOES = join(HERE, "correcoes.json");
const SAIDA = join(RAIZ, "composeApp", "src", "commonMain", "composeResources", "files", "catalogo.json");
// Fora dos recursos de propósito: o manifesto é para a release, e um ficheiro a mais dentro
// do APK é peso que ninguém lá vai buscar.
const MANIFESTO = join(RAIZ, "tools", "catalogo", "manifesto.json");

/**
 * A versão do catálogo. **Sobe à mão, e só quando o conteúdo muda.**
 *
 * É o número por que o `FoodSeeder` decide se importa: se o que veio no ficheiro for maior
 * do que o que está gravado, importa; se não, não lê o ficheiro sequer. Deixá-la para trás
 * numa alteração de conteúdo é distribuir um catálogo que ninguém recebe.
 */
const VERSAO = 3;

const aceitarDesvios = process.argv.includes("--aceitar-desvios");

// ---------------------------------------------------------------- as fontes

const ciqual = lerCiqual(join(RAIZ, "tools", "ciqual-importer", "data"));
const usda = lerUsda(join(RAIZ, "tools", "ciqual-importer", "usda-source.json"));
const tca = lerTca(join(RAIZ, "tools", "tca-importer", "data", "insa_tca.xlsx"));
const curados = lerCurados(DADOS);
const microsCurados = lerMicrosCurados(DADOS);

console.log("fontes:");
console.log(`  CIQUAL  ${ciqual.declarados} declarados → ${ciqual.alimentos.length} (degraus: ${JSON.stringify(ciqual.degraus)})`);
console.log(`  TCA     ${tca.declarados} declarados → ${tca.alimentos.length}`);
console.log(`  curados ${curados.declarados} declarados → ${curados.alimentos.length}`);
console.log(`  USDA    ${usda.declarados} registos, usados a enriquecer e a encher a cauda`);

// ------------------------------------------------- enriquecer o que a CIQUAL não mediu

const usadosDoUsda = new Set();
let enriquecidos = 0;
let microsPreenchidos = 0;

for (const e of ciqual.alimentos) {
  const chave = chaveDeNome(e.nameEn);
  const curta = chave.split(" ").slice(0, 2).join(" ");
  const par = usda.porChave.get(chave) ?? usda.porChaveCurta.get(curta);
  if (!par || !energiaConcorda(par.kcal, e.kcal)) continue;
  usadosDoUsda.add(par.id);

  let tocado = false;
  if (par.micros) {
    const micros = e.micros ? { ...e.micros } : {};
    for (const [k, v] of Object.entries(par.micros)) {
      if (micros[k] == null && v > 0) { micros[k] = v; microsPreenchidos++; tocado = true; }
    }
    if (Object.keys(micros).length) e.micros = micros;
  }
  for (const campo of ["sugarsG", "satFatG", "fiberG", "sodiumMg"]) {
    if (e[campo] == null && par[campo] != null) { e[campo] = par[campo]; tocado = true; }
  }
  if (tocado) enriquecidos++;
}

// O último degrau da escada: um macro que a CIQUAL não determinou, tirado de uma segunda
// fonte publicada. Exige o nome inteiro a bater — sem energia não há como desconfiar de uma
// correspondência à pressa, e a chave curta de duas palavras casaria coisas diferentes.
let recuperadosPelaUsda = 0;
const aindaFora = [];
for (const f of ciqual.foraDeAlcance) {
  const par = usda.porChave.get(chaveDeNome(f.nome));
  const completo = par && ["kcal", "proteinG", "carbsG", "fatG"].every((k) => par[k] != null);
  if (!completo) { aindaFora.push(f); continue; }
  ciqual.alimentos.push({
    ...par,
    id: f.id,
    sourceRef: f.id.replace("ciqual-", ""),
    nameEn: f.nome,
    namePt: f.nome,
    verified: true,
    origin: "CIQUAL",
    derivado: "usda",
  });
  usadosDoUsda.add(par.id);
  recuperadosPelaUsda++;
}
ciqual.foraDeAlcance = aindaFora;

console.log(`\nenriquecidos ${enriquecidos} alimentos (${microsPreenchidos} micros vindos do USDA)`);
console.log(`recuperados pela USDA: ${recuperadosPelaUsda}`);

// ------------------------------------------------------------------- a cauda do USDA

const chavesDaCiqual = new Set(ciqual.alimentos.map((e) => chaveDeNome(e.nameEn)));
const nomesCurados = new Set(
  curados.alimentos.map((e) => String(e.namePt ?? "").trim().toLowerCase()),
);

const cauda = [];
let extrasDescartados = 0;
for (const u of usda.registos) {
  const escritoAMao = String(u.id).startsWith("pt-");
  if (escritoAMao) {
    // O mesmo prato escrito duas vezes: uma nos extras, outra na camada curada. A curada
    // ganha, por ser a que se continua a editar.
    if (nomesCurados.has(String(u.namePt ?? "").trim().toLowerCase())) { extrasDescartados++; continue; }
  } else {
    if (usadosDoUsda.has(u.id)) continue;
    if (isLabDescriptor(u.nameEn)) continue;
    if (chavesDaCiqual.has(chaveDeNome(u.nameEn))) continue;
  }
  cauda.push({
    ...u,
    verified: !escritoAMao,
    origin: escritoAMao ? "PT_EXTRA" : "USDA",
    derivado: null,
  });
}
console.log(`cauda do USDA: ${cauda.length} (extras descartados por duplicarem um curado: ${extrasDescartados})`);

// ------------------------------------------------------- juntar, e dar micros aos curados

const tudo = [...ciqual.alimentos, ...cauda, ...curados.alimentos, ...tca.alimentos];

let curadosComMicros = 0;
for (const e of tudo) {
  const tabela = microsCurados[e.id];
  if (!tabela || !Object.keys(tabela).length) continue;
  if (e.micros != null) continue;
  e.micros = tabela;
  curadosComMicros++;
}
console.log(`curados que ganharam micros da tabela: ${curadosComMicros}`);

// ------------------------------------------------ o que os dezoito passos já tinham decidido

const correcoes = JSON.parse(readFileSync(CORRECOES, "utf8"));
const podados = new Set(correcoes.podados);
const liquidos = new Set(correcoes.liquidos);

const vivos = tudo.filter((e) => !podados.has(e.id));
let nomesAplicados = 0;
for (const e of vivos) {
  const nome = correcoes.nomes[e.id];
  if (nome != null && nome !== e.namePt) { e.namePt = nome; nomesAplicados++; }
  e.isLiquid = liquidos.has(e.id);
}
console.log(`\npodados por decisão anterior: ${tudo.length - vivos.length}`);
console.log(`nomes corrigidos aplicados:   ${nomesAplicados}`);
console.log(`marcados como líquido:        ${vivos.filter((e) => e.isLiquid).length}`);

// --------------------------------------------------------------------- o vocabulário

const vocabulario = lerVocabulario(join(HERE, "vocabulario.csv"));

const desacordos = conferirComAEfsa(
  vocabulario,
  join(RAIZ, "composeApp", "src", "commonMain", "composeResources", "files", "seed_efsa_drv.csv"),
);
if (desacordos.length) {
  console.error("\nO VOCABULÁRIO E AS REFERÊNCIAS DA EFSA DISCORDAM:");
  for (const d of desacordos) console.error(`  ${d}`);
  console.error("\nSão os mesmos números em dois ficheiros. Enquanto for assim, manda o que a app lê.");
  process.exit(1);
}

// Uma chave emitida e não declarada é um nutriente escrito de duas maneiras — e a app
// mostra os dois, cada um com metade dos alimentos, sem que nada dê erro.
// Sobre o que sai, e não sobre o que está a meio: o sódio e a fibra só entram no mapa no
// momento de escrever, e uma verificação feita antes disso não via as chaves que interessam.
const emitidas = new Set();
for (const e of vivos) for (const k of Object.keys(microsCom(e) || {})) emitidas.add(k);

const naoDeclaradas = [...emitidas].filter((k) => !vocabulario.has(k)).sort();
if (naoDeclaradas.length) {
  console.error(`\nCHAVES POR DECLARAR: ${naoDeclaradas.join(", ")}`);
  console.error("Ou se acrescentam ao `vocabulario.csv`, ou o importador que as emite está errado.");
  process.exit(1);
}

const semUso = [...vocabulario.keys()].filter((k) => !emitidas.has(k)).sort();
console.log(`\nvocabulário: ${vocabulario.size} chaves, ${emitidas.size} em uso`);
if (semUso.length) console.log(`  declaradas e nunca emitidas: ${semUso.join(", ")}`);

// --------------------------------------------------------------------- a cobertura

const foraDeAlcance = [
  ...ciqual.foraDeAlcance.map((f) => ({ ...f, fonte: "CIQUAL" })),
  ...tca.foraDeAlcance.map((f) => ({ ...f, fonte: "TCA" })),
];
foraDeAlcance.sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));

const declaradosNoFicheiro = existsSync(DESVIOS)
  ? JSON.parse(readFileSync(DESVIOS, "utf8"))
  : { desvios: {} };

const novos = foraDeAlcance.filter((f) => declaradosNoFicheiro.desvios[f.id] == null);

if (aceitarDesvios) {
  const desvios = {};
  for (const f of foraDeAlcance) desvios[f.id] = `${f.nome} — ${f.porque}`;
  writeFileSync(DESVIOS, JSON.stringify({
    _: "Alimentos que a fonte declara e que o catálogo não leva, com a razão de cada um. " +
       "Escrito por `construir.mjs --aceitar-desvios`; ler o diff antes de aceitar.",
    total: foraDeAlcance.length,
    desvios,
  }, null, 2) + "\n");
  console.log(`\ndesvios reescritos: ${foraDeAlcance.length} → ${DESVIOS}`);
} else if (novos.length) {
  console.error(`\nCOBERTURA CHUMBA: ${novos.length} alimentos que a fonte declara não chegaram ao catálogo e não estão declarados:`);
  for (const f of novos.slice(0, 20)) console.error(`  ${f.id}  ${f.nome} — ${f.porque}`);
  if (novos.length > 20) console.error(`  … e mais ${novos.length - 20}`);
  console.error("\nOu se corrige a leitura da fonte, ou se corre com --aceitar-desvios e se lê o diff.");
  process.exit(1);
}

// ------------------------------------------------------------------------- escrever

// Por código de caracteres, e não por `localeCompare`: esse depende da localização
// da máquina que constrói, e duas máquinas dariam ficheiros diferentes com o mesmo
// conteúdo — que é exactamente o que "determinístico" existe para impedir. É também
// a ordem que o `sorted()` do Kotlin usa, e há um teste-guarda a comparar as duas.
vivos.sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));

/**
 * Junta ao mapa de micronutrientes os dois que até à v27 viviam em coluna.
 *
 * O que já está no mapa ganha à coluna, e não o contrário: a coluna do sódio era um inteiro
 * arredondado, e é por isso que 29 alimentos mostravam dois valores diferentes conforme o
 * ecrã. Fica o número com casas decimais, que é o que a fonte publicou.
 *
 * O açúcar e a gordura saturada **continuam em coluna** e não entram aqui: não têm referência
 * da EFSA, não aparecem nos ecrãs de micronutrientes, e duplicá-los era escrever o mesmo
 * número duas vezes em oito mil linhas por nada.
 */
function microsCom(e) {
  const micros = { ...(e.micros ?? {}) };
  if (micros.sodium_mg == null && e.sodiumMg != null) micros.sodium_mg = e.sodiumMg;
  if (micros.fiber_g == null && e.fiberG != null) micros.fiber_g = e.fiberG;
  return Object.keys(micros).length ? ordenar(micros) : null;
}

/** Por ordem de chave, para o `git diff` do catálogo dizer o que mudou e não onde mudou. */
function ordenar(mapa) {
  const saida = {};
  for (const k of Object.keys(mapa).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0))) saida[k] = mapa[k];
  return saida;
}

const alimentos = vivos.map((e) => ({
  id: e.id,
  source: e.source ?? "SEED",
  sourceRef: e.sourceRef ?? null,
  nameEn: e.nameEn,
  namePt: e.namePt,
  brand: e.brand ?? null,
  kcal: e.kcal,
  proteinG: e.proteinG,
  carbsG: e.carbsG,
  sugarsG: e.sugarsG ?? null,
  fatG: e.fatG,
  satFatG: e.satFatG ?? null,

  // O sódio e a fibra deixaram de ter coluna na v28: têm referência da EFSA, são
  // micronutrientes a sério, e viver nos dois sítios era a app poder mostrar dois números
  // para o mesmo alimento — já acontecia em 29 deles, porque a coluna arredondava.
  micros: microsCom(e),
  servingName: e.servingName ?? null,
  servingGrams: e.servingGrams ?? null,
  isLiquid: Boolean(e.isLiquid),
  verified: Boolean(e.verified),
}));

const repetidos = alimentos.length - new Set(alimentos.map((a) => a.id)).size;
if (repetidos) {
  console.error(`\nO catálogo tem ${repetidos} identificadores repetidos — duas fontes a dar o mesmo id.`);
  process.exit(1);
}

const texto = JSON.stringify({ versao: VERSAO, alimentos });
writeFileSync(SAIDA, texto);

/**
 * O manifesto — o que a app pede antes de decidir se vale a pena descarregar cinco
 * megabytes. Sai daqui e não da mão de ninguém: escrito à mão, o resumo estaria errado à
 * primeira correção, e um resumo errado faz a app recusar um catálogo que está bom.
 *
 * O `latest` é do GitHub: aponta sempre para a release mais recente, e por isso o endereço
 * não muda de versão para versão. É o mesmo que está no `ApiDoCatalogo`.
 */
writeFileSync(MANIFESTO, JSON.stringify({
  versao: VERSAO,
  url: "https://github.com/BetuelRS/Antares/releases/latest/download/catalogo.json",
  sha256: createHash("sha256").update(texto).digest("hex"),
  alimentos: alimentos.length,
  nota: `${alimentos.length} alimentos, catálogo v${VERSAO}.`,
}, null, 2) + "\n");

const porOrigem = {};
for (const e of vivos) porOrigem[e.origin] = (porOrigem[e.origin] || 0) + 1;

console.log(`\ncatálogo v${VERSAO}: ${alimentos.length} alimentos`);
console.log(`  por origem: ${JSON.stringify(porOrigem)}`);
console.log(`  desvios declarados: ${foraDeAlcance.length}`);
console.log(`  ${(texto.length / 1024 / 1024).toFixed(2)} MB → ${SAIDA}`);
console.log(`  manifesto → ${MANIFESTO}`);
