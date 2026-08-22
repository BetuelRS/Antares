/**
 * Corre a verificação toda e diz o que interessa em cinco linhas.
 *
 *     node tools/verificar.mjs
 *     node tools/verificar.mjs --rapido    (salta o lint, que é o mais lento)
 *
 * Existe por causa da regra D1: **o que conta é o relatório de testes, não a última linha do
 * Gradle.** O Gradle diz `BUILD SUCCESSFUL` quando a tarefa de testes foi saltada por estar
 * actualizada, e diz `BUILD FAILED` sem dizer quantos falharam. Quem lê a última linha
 * acredita em qualquer uma das duas.
 *
 * Lê os XML que o Gradle escreve e conta: quantos testes correram, quantos foram saltados, e
 * quantos falharam — com os nomes dos que falharam, que é a única coisa que se quer ver.
 * Corre também as funções do servidor e as duas buscas de segredos que o gancho de envio faz.
 */
import { spawnSync } from "node:child_process";
import { readFileSync, readdirSync, existsSync, statSync, rmSync } from "node:fs";
import { join } from "node:path";

const rapido = process.argv.includes("--rapido");
const raiz = process.cwd();

/**
 * O caminho inteiro, e não `./gradlew`: o node abre a linha de comandos do sistema, que no
 * Windows não é a mesma consola de onde isto costuma ser chamado — e lá o `./` não quer dizer
 * nada. O erro que daí vinha lia-se como «os testes não correram».
 */
const gradlew = join(raiz, process.platform === "win32" ? "gradlew.bat" : "gradlew");

const linhas = [];
let vermelho = false;

function correr(comando, args, opcoes = {}) {
  return spawnSync(comando, args, {
    cwd: raiz,
    encoding: "utf8",
    shell: process.platform === "win32",
    maxBuffer: 64 * 1024 * 1024,
    ...opcoes,
  });
}

// ------------------------------------------------------------------ os testes Kotlin

const pastaDosResultados = join(raiz, "composeApp", "build", "test-results", "testDebugUnitTest");

/**
 * A hora de arranque, para só contar relatórios escritos depois dela.
 *
 * Sem isto, uma execução que nem chegue a começar deixa este script a somar os resultados da
 * vez passada e a dizer que está tudo bem sobre código que ninguém testou. Aconteceu na
 * primeira execução deste ficheiro: leu quatro testes de uma sonda já apagada e deu-os por
 * verdade.
 *
 * Apagar a pasta antes de correr seria mais directo, e não serve: no Windows o serviço do
 * Gradle mantém-na aberta e a remoção falha com falta de permissão.
 */
const arranque = Date.now();

// Apagar os relatórios um a um, e não a pasta: a pasta **é** a saída da tarefa, e sem ela o
// Gradle repete os testes em vez de os dar por actualizados. No Windows o serviço do Gradle
// mantém a pasta aberta e removê-la inteira falha por falta de permissão; os ficheiros lá
// dentro saem.
if (existsSync(pastaDosResultados)) {
  for (const f of readdirSync(pastaDosResultados).filter((x) => x.endsWith(".xml"))) {
    try {
      rmSync(join(pastaDosResultados, f));
    } catch {
      // Um relatório preso não impede a execução: a hora de arranque acima trata dele.
    }
  }
}

// Sem `--rerun-tasks`: forçar a repetição de tudo fazia o Gradle reempacotar recursos, e no
// Windows isso choca com ficheiros que outro processo tem abertos — a verificação falhava
// por uma razão que não tem nada a ver com o código.
const tarefas = [":composeApp:testDebugUnitTest", "detekt"];
if (!rapido) tarefas.push(":composeApp:lintDebug");

process.stderr.write("a correr o Gradle…\n");
const gradle = correr(gradlew, tarefas);
const saidaGradle = (gradle.stdout || "") + (gradle.stderr || "");

// O Gradle escreve sempre uma destas. Não escrever nenhuma quer dizer que nem arrancou — e
// aí tudo o que se lesse a seguir seria silêncio a passar por bom.
if (!/BUILD SUCCESSFUL|BUILD FAILED/.test(saidaGradle)) {
  console.error("o Gradle não chegou a correr:");
  console.error(saidaGradle.split("\n").filter(Boolean).slice(-6).join("\n"));
  process.exit(1);
}

const erros = saidaGradle.split("\n").filter((l) => l.startsWith("e: "));
// Só os achados do detekt. As linhas de aviso do compilador têm a mesma forma — ficheiro,
// linha, coluna — e contá-las era chumbar a versão por causa de ruído que o Kotlin escreve
// em todas as compilações.
const detekt = saidaGradle
  .split("\n")
  .filter((l) => /\.kt:\d+:\d+:/.test(l) && !/warning:|exception:/.test(l));

// O relatório, e não a última linha: é a regra D1 escrita em código.
const pasta = pastaDosResultados;
let testes = 0;
let saltados = 0;
let falhas = 0;
const falharam = [];

if (existsSync(pasta)) {
  for (const f of readdirSync(pasta).filter((x) => x.endsWith(".xml"))) {
    // Só os desta execução: um relatório velho conta testes que não correram agora.
    if (statSync(join(pasta, f)).mtimeMs < arranque) continue;
    const xml = readFileSync(join(pasta, f), "utf8");
    const m = xml.match(/tests="(\d+)" skipped="(\d+)" failures="(\d+)" errors="(\d+)"/);
    if (!m) continue;
    testes += Number(m[1]);
    saltados += Number(m[2]);
    falhas += Number(m[3]) + Number(m[4]);
    for (const t of xml.matchAll(/<testcase name="([^"]+)" classname="([^"]+)"[^/>]*>\s*<(failure|error)/g)) {
      falharam.push(`${t[2].split(".").pop()} > ${t[1]}`);
    }
  }
}

if (erros.length) {
  vermelho = true;
  linhas.push(`compilação: ${erros.length} erros`);
  for (const e of erros.slice(0, 5)) linhas.push(`    ${e.replace(/^e: file:\/\/\/?/, "").slice(0, 140)}`);
} else if (!testes) {
  vermelho = true;
  linhas.push("testes Kotlin: nenhum relatório — a tarefa nem chegou a correr");
  for (const l of saidaGradle.split("\n").filter((x) => /Task :composeApp:test|FAILED|error/i.test(x)).slice(-4)) {
    linhas.push(`    ${l.trim().slice(0, 140)}`);
  }
} else {
  if (falhas) vermelho = true;
  linhas.push(`testes Kotlin: ${testes}, ${saltados} saltados, ${falhas} a falhar`);
  for (const f of falharam.slice(0, 10)) linhas.push(`    ${f}`);
}

if (detekt.length) {
  vermelho = true;
  linhas.push(`detekt/lint: ${detekt.length} achados`);
  for (const d of detekt.slice(0, 5)) linhas.push(`    ${d.replace(raiz, "").slice(0, 140)}`);
} else {
  linhas.push(`detekt${rapido ? "" : " e lint"}: limpos`);
}

// ------------------------------------------------------------------ as funções do servidor

const deno = correr("deno", ["test", "--allow-all", "supabase/functions"]);
const saidaDeno = (deno.stdout || "") + (deno.stderr || "");
const passou = saidaDeno.match(/(\d+) passed \| (\d+) failed/);
if (deno.error) {
  linhas.push("testes Deno: o `deno` não está no caminho desta sessão — por correr");
} else if (passou) {
  if (Number(passou[2])) vermelho = true;
  linhas.push(`testes Deno: ${passou[1]}, ${passou[2]} a falhar`);
} else {
  vermelho = true;
  linhas.push("testes Deno: não se percebeu a saída");
}

// ------------------------------------------------------------------ os segredos

const ficheiros = correr("git", ["ls-files"]).stdout.split("\n").filter(Boolean);
const padroes = [
  [/sk-ant-(api|admin)[A-Za-z0-9_-]{10,}/, "chave da Anthropic"],
  [/eyJ[A-Za-z0-9_-]{40,}/, "credencial JWT"],
];
const apanhados = [];
for (const f of ficheiros) {
  let texto;
  try {
    texto = readFileSync(join(raiz, f), "utf8");
  } catch {
    continue; // binário ou apagado; o gancho de envio faz o mesmo
  }
  for (const [padrao, nome] of padroes) {
    if (padrao.test(texto)) apanhados.push(`${f} — ${nome}`);
  }
}
if (apanhados.length) {
  vermelho = true;
  linhas.push(`segredos: ${apanhados.length} ficheiros`);
  for (const a of apanhados.slice(0, 5)) linhas.push(`    ${a}`);
} else {
  linhas.push(`segredos: nenhum em ${ficheiros.length} ficheiros`);
}

// ------------------------------------------------------------------ a resposta

console.log("");
for (const l of linhas) console.log(l);
console.log("");
console.log(vermelho ? "VERMELHO — não publicar" : "verde");
process.exit(vermelho ? 1 : 0);
