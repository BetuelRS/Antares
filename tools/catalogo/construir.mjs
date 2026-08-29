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
import { lerUsda, chaveDeNome, chaveDeIdentidade, isLabDescriptor, energiaConcorda } from "./fontes/usda.mjs";
import { lerTca } from "./fontes/tca.mjs";
import { lerCurados, lerMicrosCurados } from "./fontes/curados.mjs";
import { lerVocabulario, conferirComAEfsa } from "./vocabulario.mjs";
import { verificar, LIMITES } from "./qualidade.mjs";
import { colisoes, aplicarFusoes } from "./colisoes.mjs";
import { porDeAcordoConsigo } from "./coerencia.mjs";
import { completar } from "./fontes/usda-completo.mjs";
import { familiaDeUsda } from "../confecao/familias.mjs";
import { densidadeDe, pareceSolido } from "./densidade.mjs";
import { candidatosALiquido } from "./liquidos.mjs";
import { lerCsv } from "../confecao/tabelas.mjs";
import { lerVocabulario as lerVocabularioDeNomes, traduzirNome } from "../vocabulario/traduzir.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const RAIZ = join(HERE, "..", "..");
const DADOS = join(HERE, "dados");
const DESVIOS = join(HERE, "desvios.json");
const CORRECOES = join(HERE, "correcoes.json");
const QUALIDADE = join(HERE, "qualidade.json");
const LIQUIDOS_POR_DECIDIR = join(HERE, "liquidos-por-decidir.json");
const COLISOES = join(HERE, "colisoes.json");
const FUSOES = join(HERE, "fusoes.json");
const POR_TRADUZIR = join(RAIZ, "tools", "vocabulario", "por-traduzir.json");
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
const VERSAO = 6;

const aceitarDesvios = process.argv.includes("--aceitar-desvios");
const aceitarQualidade = process.argv.includes("--aceitar-qualidade");

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

/**
 * Os pares que se encontraram pelo nome e discordaram na energia.
 *
 * São o combustível da verificação de discordância entre fontes, e ficam de fora do
 * enriquecimento pela mesma razão: **duas tabelas publicadas a dizerem coisas diferentes
 * sobre o mesmo alimento é o achado**, não um problema a resolver escolhendo uma delas.
 * Nada disto entra no catálogo — só na fila da oficina.
 */
const discordancias = [];

for (const e of ciqual.alimentos) {
  const chave = chaveDeNome(e.nameEn);
  const curta = chave.split(" ").slice(0, 2).join(" ");
  const par = usda.porChave.get(chave) ?? usda.porChaveCurta.get(curta);
  if (!par) continue;
  if (!energiaConcorda(par.kcal, e.kcal)) {
    /*
     * Só quando são **o mesmo alimento**, e não apenas o mesmo ingrediente.
     *
     * A chave de nome deita fora a preparação para poder casar tabelas, e por isso punha o
     * arroz selvagem cru a 344 kcal a «discordar» do cozido a 101. Não discordam: são coisas
     * diferentes, e a diferença é a água. Eram oito dos dezasseis achados — metade da fila
     * a apontar para um problema que não existe é a fila inteira a perder credibilidade.
     */
    const mesmoAlimento = chaveDeIdentidade(e.nameEn) === chaveDeIdentidade(par.nameEn);
    if (mesmoAlimento && par.kcal != null && e.kcal != null) {
      discordancias.push({ alimento: e, outraFonte: "USDA", outraEnergia: par.kcal });
    }
    continue;
  }
  usadosDoUsda.add(par.id);

  let tocado = false;
  if (par.micros) {
    const micros = e.micros ? { ...e.micros } : {};
    for (const [k, v] of Object.entries(par.micros)) {
      if (micros[k] == null && v > 0) {
        micros[k] = v;
        marcarOrigem(e, k, "USDA");
        microsPreenchidos++;
        tocado = true;
      }
    }
    if (Object.keys(micros).length) e.micros = micros;
  }
  for (const campo of ["sugarsG", "satFatG", "fiberG", "sodiumMg"]) {
    if (e[campo] == null && par[campo] != null) {
      e[campo] = par[campo];
      // A fibra e o sódio acabam dentro dos micros — ver `microsCom`. Os outros dois ficam
      // em coluna e não têm onde levar a marca, que é a armadilha que o
      // `onde-vive-o-nutriente.test.mjs` já vigia.
      if (campo === "fiberG") marcarOrigem(e, "fiber_g", "USDA");
      if (campo === "sodiumMg") marcarOrigem(e, "sodium_mg", "USDA");
      tocado = true;
    }
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

// A chave de identidade e nao a de nome: esta guarda o modo de preparacao, e sem ela o
// arroz selvagem cozido era descartado por existir o cru. Ver [chaveDeIdentidade].
const chavesDaCiqual = new Set(ciqual.alimentos.map((e) => chaveDeIdentidade(e.nameEn)));
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
    if (chavesDaCiqual.has(chaveDeIdentidade(u.nameEn))) continue;
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
  // Estes micros não são do alimento: são de um alimento equivalente da CIQUAL, escolhido à
  // mão. É o que o ecrã já dizia em prosa — «micronutrientes de um alimento equivalente da
  // CIQUAL» — e passa a estar na linha de cada nutriente, que é onde se lê o número.
  for (const k of Object.keys(tabela)) marcarOrigem(e, k, "CIQUAL");
  curadosComMicros++;
}
console.log(`curados que ganharam micros da tabela: ${curadosComMicros}`);

// ------------------------------------------------ o que os dezoito passos já tinham decidido

const correcoes = JSON.parse(readFileSync(CORRECOES, "utf8"));
const podados = new Set(correcoes.podados);
const liquidos = new Set(correcoes.liquidos);

// As porções escritas na oficina. Nenhuma das quatro fontes publica «uma fatia» nem «um
// copo», e é essa a unidade por que as pessoas comem — pesar a comida antes de a comer é o
// que faz um diário ser abandonado à segunda semana.
const porcoes = correcoes.porcoes ?? {};

// As porcoes domesticas do FoodData Central, ligadas por identificador exacto. Ver
// `tools/porcoes/construir.mjs`.
const PORCOES_USDA = join(RAIZ, "tools", "porcoes", "porcoes.json");
const porcoesDoUsda = existsSync(PORCOES_USDA) ? JSON.parse(readFileSync(PORCOES_USDA, "utf8")) : {};

const vivos = tudo.filter((e) => !podados.has(e.id));
let nomesAplicados = 0;
let comDensidade = 0;
let semDensidade = 0;
let desmarcados = 0;
let porcoesAplicadas = 0;
for (const e of vivos) {
  const nome = correcoes.nomes[e.id];
  if (nome != null && nome !== e.namePt) { e.namePt = nome; nomesAplicados++; }
  // A marca de liquido, e a densidade que a torna util. Um nome que diga «cozido em
  // agua» ou «em po» perde a marca: sao solidos que a lista antiga trouxe por engano, e
  // marcados assim ofereciam mililitros para uma ameijoa.
  e.isLiquid = liquidos.has(e.id) && !pareceSolido(e);
  if (e.isLiquid) {
    const d = densidadeDe(e);
    if (d != null) { e.densidade = d; comDensidade++; } else { semDensidade++; }
  }
  if (liquidos.has(e.id) && !e.isLiquid) desmarcados++;

  /**
   * A porção decidida na oficina ganha à tabela: é uma pessoa a dizer como come aquilo, e a
   * tabela é o USDA a dizer como se mede.
   */
  const daOficina = porcoes[e.id];
  const daTabela = porcoesDoUsda[e.id];

  if (daOficina?.nome && daOficina.gramas > 0) {
    e.servingName = daOficina.nome;
    e.servingGrams = daOficina.gramas;
    porcoesAplicadas++;
  } else if (daTabela?.length) {
    e.servingName = daTabela[0].nome;
    e.servingGrams = daTabela[0].gramas;
    porcoesAplicadas++;
  }

  // As restantes ficam à mão, para a linha de atalhos as poder oferecer sem escrever
  // números. A primeira sai da lista: já está no `servingName`.
  const restantes = (daTabela ?? []).filter(
    (p) => p.nome !== e.servingName || p.gramas !== e.servingGrams,
  );
  if (restantes.length) e.porcoes = restantes;
}
console.log(`\npodados por decisão anterior: ${tudo.length - vivos.length}`);
console.log(`nomes corrigidos aplicados:   ${nomesAplicados}`);
console.log(`porções (oficina + tabela):   ${porcoesAplicadas}`);
console.log(`  desmarcados por serem sólidos: ${desmarcados}`);

// Os que parecem liquidos e nao estao marcados. Nao se marcam sozinhos — ver o cabecalho de
// `liquidos.mjs`: as tres regras que tentei marcaram comida solida, e um solido em
// mililitros nao rebenta nada, so mente em silencio.
const porDecidir = candidatosALiquido(vivos, pareceSolido);
writeFileSync(LIQUIDOS_POR_DECIDIR, JSON.stringify(porDecidir, null, 2) + "\n");
console.log(`  parecem líquidos e não estão marcados: ${porDecidir.length} → fila`);

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

// ------------------------------------------------------------------------ os nomes

/**
 * Traduz os nomes de laboratório, segmento a segmento, e **só quando o nome fica inteiro**.
 *
 * Meio traduzido — «Arroz, wild, cru» — é pior do que em inglês: parece um defeito, e quem o
 * lê não sabe se o alimento é o que diz ser. O que fica por traduzir fica em inglês, é
 * contado aqui, e vai para a fila da oficina.
 *
 * Corre **depois** das correções à mão: um nome escrito por uma pessoa ganha sempre ao
 * vocabulário, e não há caso em que valha a pena o contrário.
 */
const vocabularioDeNomes = lerVocabularioDeNomes();
let traduzidos = 0;
const segmentosEmFalta = {};

/**
 * Só as fontes que publicam em inglês. A TCA e os curados escrevem em português, e nesses o
 * `namePt` ser igual ao `nameEn` não quer dizer «por traduzir» — quer dizer que não há nada
 * para traduzir. Sem esta linha, o oleoduto pedia a tradução de «porco» e de «vaca».
 */
const EM_INGLES = /^(ciqual|usda)-/;

for (const e of vivos) {
  if (e.namePt !== e.nameEn || !EM_INGLES.test(e.id)) continue;
  const r = traduzirNome(e.nameEn, vocabularioDeNomes);
  if (r.completo && r.nome) {
    e.namePt = r.nome;
    traduzidos++;
  } else {
    for (const s of r.porTraduzir) {
      const k = s.toLowerCase();
      segmentosEmFalta[k] = (segmentosEmFalta[k] ?? 0) + 1;
    }
  }
}

const porTraduzir = vivos.filter((e) => e.namePt === e.nameEn && EM_INGLES.test(e.id)).length;
writeFileSync(POR_TRADUZIR, JSON.stringify({
  vocabulario: vocabularioDeNomes.size,
  traduzidosAgora: traduzidos,
  alimentosPorTraduzir: porTraduzir,

  // Por quantos alimentos cada segmento desbloqueia, que é a ordem por que vale a pena
  // traduzi-los: um segmento em vinte e quatro nomes vale vinte e quatro por um.
  segmentos: Object.fromEntries(
    Object.entries(segmentosEmFalta).sort((a, b) => b[1] - a[1] || (a[0] < b[0] ? -1 : 1)),
  ),
}, null, 2) + "\n");

console.log(`\nvocabulário de nomes: ${vocabularioDeNomes.size} segmentos`);
console.log(`  nomes traduzidos por inteiro: ${traduzidos}`);
console.log(`  ainda em inglês:              ${porTraduzir}`);
console.log(`  segmentos por traduzir:       ${Object.keys(segmentosEmFalta).length} → ${POR_TRADUZIR}`);

// ------------------------------------------------------------------------- as famílias

/**
 * A que família de confeção pertence cada alimento — o que decide o que lhe acontece quando
 * se cozinha. A CIQUAL e a TCA já a trazem da sua própria árvore de grupos; a USDA vem daqui,
 * porque a categoria de cada alimento está numa tabela à parte.
 *
 * **O alimento que não tem família não ganha uma por parecença.** Um pão já foi ao forno, um
 * gelado não vai, e um prato composto é comida feita: a resposta certa para esses é nenhuma
 * família, e a app não lhes oferece confeção.
 */
const categoriasUsda = new Map();
for (const l of lerCsv(readFileSync(join(HERE, "..", "confecao", "data", "food.csv"), "utf8")).slice(1)) {
  if (l.length >= 4) categoriasUsda.set(`usda-${l[0]}`, l[3]);
}

let comFamilia = 0;
for (const e of vivos) {
  if (e.familia == null && e.id.startsWith("usda-")) {
    e.familia = familiaDeUsda(categoriasUsda.get(e.id));
  }
  if (e.familia) comFamilia++;
}
console.log(`famílias de confeção:          ${comFamilia} de ${vivos.length}`);

// ---------------------------------------------------------- completar a USDA

/**
 * O que a extração antiga da USDA não trouxe: a água, o fósforo, o colesterol, o álcool.
 *
 * Corre **antes** da coerência e do motor de qualidade de propósito. Sem água, o balanço de
 * massa não podia sequer correr num terço do catálogo; sem álcool nem polióis, as bebidas e
 * os doces sem açúcar falhavam a conta de Atwater por lhes faltar a parcela que produz a
 * energia que declaram. Verificar antes de completar era acusar o alimento do que a leitura
 * tinha deixado de fora.
 */
const completados = completar(vivos, join(HERE, "..", "confecao", "data", "food_nutrient.csv"));
console.log(`\nUSDA completada: ${completados.valores} valores em ${completados.alimentos} alimentos`);

// ------------------------------------------------------------------------ a coerência

/**
 * Cada alimento posto de acordo consigo próprio, **antes** de o motor de qualidade o julgar.
 *
 * A ordem importa: se corresse depois, o motor acusava contradições que a coerência já sabia
 * resolver, e a lista de doze aceites nunca encolhia. A correr antes, o que sobra é o que
 * nenhuma regra sabe arrumar — e é isso que uma contradição declarada deve ser.
 */
const arrumados = porDeAcordoConsigo(vivos);
console.log(`\ncoerência: ${arrumados.acucares} açúcares acima dos hidratos, ` +
  `${arrumados.gorduras} somas de gordura acima do total, ${arrumados.agua} águas acima de 100 g`);

// ------------------------------------------------------------------------ as colisões

/**
 * Dois alimentos com o mesmo nome, e o que se decidiu sobre eles.
 *
 * A deduplicação era um passo que correu uma vez, em 2025; passa a ser uma regra da
 * construção. **Detectar é do oleoduto, decidir é de quem come:** qual dos dois fica é uma
 * decisão sobre comida, e vive em `fusoes.json`.
 */
const fusoes = existsSync(FUSOES) ? JSON.parse(readFileSync(FUSOES, "utf8")).fusoes ?? {} : {};
const fundido = aplicarFusoes(vivos, fusoes);

// A cópia antes de esvaziar não é zelo: sem fusões nenhumas, o [aplicarFusoes] devolve a
// **mesma** lista que recebeu, e `vivos.length = 0` apagava-a antes de a voltar a encher. O
// catálogo saiu com zero alimentos, e a construção não deu erro nenhum.
const restantes = [...fundido.vivos];
vivos.length = 0;
vivos.push(...restantes);

/**
 * O catálogo não pode encolher de repente, e a construção não pode escrever nada até isso
 * estar verificado.
 *
 * **Aconteceu:** uma aliasing de lista deixou o `vivos` vazio, e a construção seguiu em
 * frente a escrever um catálogo de zero alimentos, um manifesto do vazio e um
 * `qualidade.json` sem contradição nenhuma — que na execução seguinte fez as doze de sempre
 * passarem por novas e chumbarem a construção. Nenhum dos passos deu erro.
 *
 * O número é folgado de propósito: o que se quer apanhar é um desastre, não uma variação.
 */
const MINIMO_PLAUSIVEL = 7000;
if (vivos.length < MINIMO_PLAUSIVEL) {
  console.error(`\nSó ${vivos.length} alimentos vivos — o catálogo tinha 8 011.`);
  console.error("Alguma coisa se partiu antes de aqui chegar. Não se escreve nada.");
  process.exit(1);
}

const encontradas = colisoes(vivos);
const discordantes = encontradas.filter((c) => c.discordam);

writeFileSync(COLISOES, JSON.stringify({
  contagens: {
    colisoes: encontradas.length,
    discordantes: discordantes.length,
    alimentos: encontradas.reduce((s, c) => s + c.alimentos.length, 0),
    fundidos: fundido.fundidos,
  },
  colisoes: encontradas,
}, null, 2) + "\n");

console.log(`\ncolisões de nome: ${encontradas.length} (${discordantes.length} discordam na energia)`);
if (fundido.fundidos) console.log(`  fundidos por decisão: ${fundido.fundidos}`);

// **Depois das fusoes, e nao antes.** Contado antes, o relatorio dizia 782 liquidos e o
// ficheiro levava 773: nove eram perdedores de uma fusao. Um numero no ecra que nao existe
// no ficheiro e a forma mais silenciosa de uma ferramenta mentir sobre si propria.
const liquidosFinais = vivos.filter((e) => e.isLiquid);
console.log(`marcados como líquido:        ${liquidosFinais.length}`);
console.log(`  com densidade medida:        ${liquidosFinais.filter((e) => e.densidade != null).length}`);
console.log(`  → ${COLISOES}`);

// ---------------------------------------------------------------------- a qualidade

/**
 * As perguntas que cada alimento tem de conseguir responder sobre si próprio.
 *
 * Corre sobre o `vivos`, e não sobre o que vai ser escrito: aqui os alimentos ainda trazem o
 * subgrupo e a origem, que o catálogo não leva e de que as verificações precisam.
 *
 * **As contradições chumbam; as suspeitas enchem a fila da oficina.** Um número impossível é
 * uma mentira que se publica; um número improvável é uma discordância entre métodos de
 * medição, e chumbar por isso era não poder publicar até a fonte se corrigir.
 */
const achados = verificar(vivos, discordancias);
const contradicoes = achados.filter((a) => a.gravidade === "contradicao");
const suspeitas = achados.filter((a) => a.gravidade === "suspeita");

const aceitesAntigas = existsSync(QUALIDADE)
  ? JSON.parse(readFileSync(QUALIDADE, "utf8")).contradicoes ?? []
  : [];
const jaAceite = new Set(aceitesAntigas.map((a) => `${a.id}:${a.tipo}`));
const novasContradicoes = contradicoes.filter((a) => !jaAceite.has(`${a.id}:${a.tipo}`));

if (novasContradicoes.length && !aceitarQualidade) {
  console.error(`\n${novasContradicoes.length} contradições novas — números impossíveis:\n`);
  for (const a of novasContradicoes.slice(0, 20)) {
    console.error(`  ${a.id}  ${a.tipo}: ${a.mensagem}  (${a.nome})`);
  }
  if (novasContradicoes.length > 20) console.error(`  … e mais ${novasContradicoes.length - 20}`);
  console.error("\nOu se corrige em correcoes.json, ou se corre com --aceitar-qualidade e se lê o diff.");
  process.exit(1);
}

writeFileSync(QUALIDADE, JSON.stringify({
  limites: LIMITES,
  contagens: { contradicoes: contradicoes.length, suspeitas: suspeitas.length },

  /**
   * O que a coerência arrumou antes de o motor olhar.
   *
   * Sem este número, uma verificação a zero não se distingue de uma verificação apagada — e
   * as três das contradições passaram todas a zero **porque a coerência as corrige**, não
   * porque o catálogo tenha melhorado sozinho. É aqui que o teste-guarda vai buscar a prova
   * de que elas continuam a fazer alguma coisa.
   */
  corrigidas: arrumados,
  contradicoes,
  suspeitas,
}, null, 2) + "\n");

console.log(`\nqualidade: ${contradicoes.length} contradições, ${suspeitas.length} suspeitas → ${QUALIDADE}`);
for (const tipo of [...new Set(achados.map((a) => a.tipo))].sort()) {
  console.log(`  ${tipo}: ${achados.filter((a) => a.tipo === tipo).length}`);
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

/**
 * De onde veio **este** nutriente, quando não veio da fonte do alimento.
 *
 * O esboço 22 pede a origem por nutriente, e a razão é a fusão por prioridade: um alimento do
 * INSA pode levar o iodo da CIQUAL, e um da CIQUAL pode levar metade dos micros do USDA. Uma
 * origem por alimento diz a de quem lhe deu o nome e as calorias, e cala a dos outros.
 *
 * **Só se escreve a excepção.** O caso comum — o nutriente vem de onde vem o alimento — não
 * ocupa um byte, e a app resolve-o em leitura. É a mesma escolha de formato da ausência
 * tipada: o comum fica nu, e só o que diverge se marca.
 */
function marcarOrigem(e, chave, origem) {
  e.microsOrigem ??= {};
  e.microsOrigem[chave] = origem;
}

/**
 * As origens que sobrevivem à emissão: só as dos nutrientes que ficaram mesmo no alimento.
 *
 * A `coerencia` apaga a água de quem não fecha o balanço de massa, e a fusão faz desaparecer
 * o alimento perdedor. Uma marca de origem para um nutriente que já não existe é ruído que a
 * app teria de aprender a ignorar.
 */
function origensCom(e, micros) {
  if (!e.microsOrigem || !micros) return null;
  const saida = {};
  for (const [k, v] of Object.entries(e.microsOrigem)) {
    if (micros[k] != null && v !== origemDoAlimento(e)) saida[k] = v;
  }
  return Object.keys(saida).length ? ordenar(saida) : null;
}

/** A origem que a app deduz do alimento, e que por isso não vale a pena escrever de novo. */
function origemDoAlimento(e) {
  if (e.id.startsWith("ciqual-")) return "CIQUAL";
  if (e.id.startsWith("tca-")) return "TCA";
  if (e.id.startsWith("usda-")) return "USDA";
  return null;
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

  // De onde veio cada nutriente que **não** veio da fonte do alimento. Ausente no caso
  // comum, que é a esmagadora maioria — ver `marcarOrigem`.
  ...(origensCom(e, microsCom(e)) ? { microsOrigem: origensCom(e, microsCom(e)) } : {}),

  // A família de confeção. Nula quer dizer «não se cozinha isto» e não «não sabemos»: é o
  // que faz a app não oferecer «e se for cozido?» a um gelado.
  familia: e.familia ?? null,
  servingName: e.servingName ?? null,
  servingGrams: e.servingGrams ?? null,

  // As outras maneiras de medir o mesmo alimento — a chávena, a colher, a fatia —, para a
  // linha de atalhos as oferecer sem ninguém escrever um número.
  porcoes: e.porcoes ?? null,
  isLiquid: Boolean(e.isLiquid),
  // Gramas por mililitro. Ausente quer dizer que ninguém a mediu para este alimento, e
  // a app trata-o como 1,00 — que é o que já fazia antes de esta coluna existir.
  ...(e.densidade != null ? { densidade: e.densidade } : {}),
  verified: Boolean(e.verified),
}));

const repetidos = alimentos.length - new Set(alimentos.map((a) => a.id)).size;
if (repetidos) {
  console.error(`\nO catálogo tem ${repetidos} identificadores repetidos — duas fontes a dar o mesmo id.`);
  process.exit(1);
}

const texto = JSON.stringify({ versao: VERSAO, alimentos, lapides: fundido.lapides });
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
