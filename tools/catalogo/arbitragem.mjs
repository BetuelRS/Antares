/**
 * As 65 colisões de nome, arbitradas uma a uma.
 *
 *     node tools/catalogo/arbitragem.mjs
 *
 * Escreve as fusões em `fusoes.json` e os nomes desambiguados em `correcoes.json`. Corre-se
 * uma vez; o que produz é que fica. Existe como ficheiro — e não como uma sessão na oficina —
 * porque **as razões de cada decisão têm de ficar escritas**, e uma lista de identificadores
 * num JSON não as guarda.
 *
 * ## As três pilhas
 *
 * **Fundir.** O mesmo alimento medido duas vezes, ou escrito à mão a partir de outro. Um dos
 * dois sai e deixa lápide. É a maior das pilhas e a mais fácil: quando os números batem
 * certo, qual fica quase não importa — o que importa é não haver dois.
 *
 * **Renomear.** Dois alimentos **diferentes** cujos nomes colidiram, quase sempre por a
 * tradução ter aproximado o que as fontes distinguiam: «Cornmeal» e «Corn flour» são as duas
 * farinha de milho, e não são a mesma coisa. Aqui fundir seria apagar comida. O que se corrige
 * é o nome.
 *
 * **Deixar.** As que a normalização juntou por engano e já não junta — os vinhos de «≥12,5%»
 * e «<12,5%» eram três colisões que desapareceram quando o normalizador deixou de deitar fora
 * os sinais de comparação.
 *
 * ## Qual fica, quando é para fundir
 *
 * Por esta ordem, e a razão de cada degrau:
 *
 * 1. **Uma medição ganha a uma estimativa.** Os alimentos escritos à mão foram calculados a
 *    partir de receitas; os das tabelas foram analisados em laboratório.
 * 2. **Quando as duas são medições e concordam, fica a portuguesa.** A TCA do INSA mediu
 *    produto português — a variedade, o corte e o modo de preparar daqui. Numa app portuguesa
 *    é a que descreve o que a pessoa tem no prato.
 * 3. **Quando as duas são medições e discordam, uma terceira desempata.** A USDA está cá e é
 *    independente das outras duas. Na abóbora, no alho, na cebola e na cenoura ela fica do
 *    lado da CIQUAL; nos espinafres fica do lado da TCA. Duas tabelas independentes a
 *    concordarem contra uma valem mais do que a preferência por qualquer uma delas.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const FUSOES = join(HERE, "fusoes.json");
const CORRECOES = join(HERE, "correcoes.json");

/**
 * O que sai, para onde vai, e porquê. O `porque` não é comentário: é o que permite rever uma
 * destas decisões daqui a um ano sem a ter de tomar outra vez do zero.
 */
const FUNDIR = [
  // --- uma medição ganha a uma estimativa escrita à mão -------------------------------
  ["ptx3_queijo_azul", "usda-172175", "os números do escrito à mão são cópia dos da USDA"],
  ["ptx3_gin", "ciqual-1002", "gin é gin; a CIQUAL mediu, o extra foi estimado"],
  ["ptx3_vodka", "ciqual-1008", "idem"],
  ["ptx3_whisky", "ciqual-1005", "idem"],
  ["ptx_vinho_tinto", "ciqual-5214", "o extra declara 2,6 g de hidratos num vinho tinto seco"],
  ["ptx2_mostarda", "ciqual-11013", "a estimativa dá metade da gordura de uma mostarda"],
  ["ptx2_salmao_fumado", "ciqual-26037", "a estimativa dá 4,3 g de gordura a um salmão fumado"],
  ["ptx3_sangria", "ciqual-1017", "a estimativa não tem análise por trás; a CIQUAL tem"],

  // --- a mesma tabela a ter o mesmo alimento duas vezes -------------------------------
  ["usda-172776", "usda-171853", "«whole wheat» e «whole-wheat»: a USDA repetiu-se num hífen"],
  ["ciqual-26062", "ciqual-26248", "um linguado «frito» com 1 g de gordura e menos calorias do que o grelhado contradiz-se"],

  // --- duas medições que concordam: fica a portuguesa ---------------------------------
  ["ciqual-31016", "tca-503", "açúcar branco: 399 contra 397 kcal"],
  ["ciqual-9102", "tca-401", "arroz integral cru: 350 contra 351"],
  ["ciqual-20057", "tca-550", "brócolos crus: 32 contra 32"],
  ["ciqual-10007", "tca-971", "camarão cozido: 91 contra 99"],
  ["ciqual-26051", "tca-820", "cavala crua: 198 contra 202"],
  ["ciqual-9621", "tca-1900000075", "farelo de trigo: 270 contra 293"],
  ["ciqual-9550", "tca-1900000070", "farinha de cevada: 337 contra 330"],
  ["ciqual-36017", "tca-11", "peito de frango sem pele cru: 110 contra 108"],
  ["ciqual-11027", "tca-1900000031", "hortelã fresca: 58 contra 51"],
  ["ciqual-53672", "tca-1900000059", "inhame cru: 111 contra 114"],
  ["ciqual-19050", "tca-27", "leite magro UHT: 34 contra 35"],
  ["ciqual-19041", "tca-25", "leite meio-gordo UHT: 48 contra 47"],
  ["ciqual-26058", "tca-845", "linguado cru: 77 contra 82"],
  ["ciqual-13111", "tca-665", "maçã seca: 247 contra 257"],
  ["ciqual-31008", "tca-504", "mel: 331 contra 314"],
  ["ciqual-2043", "tca-748", "néctar de alperce: 59 contra 56"],
  ["ciqual-2374", "tca-749", "néctar de ananás: 52 contra 46"],
  ["ciqual-2375", "tca-750", "néctar de laranja: 42 contra 43"],
  ["ciqual-2076", "tca-751", "néctar de maçã: 45 contra 48"],
  ["ciqual-2371", "tca-753", "néctar de pêssego: 50 contra 51"],
  ["ciqual-2054", "tca-752", "néctar de pera: 63 contra 48 — a TCA tem a série toda dos néctares"],
  ["ciqual-17040", "tca-388", "óleo de amendoim: 899 contra 887"],
  ["ciqual-17440", "tca-390", "óleo de girassol: 900 contra 896"],
  ["ciqual-17420", "tca-393", "óleo de soja: 900 contra 887"],
  ["ciqual-10011", "tca-911", "ostra crua: 67 contra 65"],
  ["ciqual-28800", "tca-357", "presunto: a CIQUAL dá-lhe 4,3 g de hidratos, que um presunto não tem"],
  ["ciqual-30350", "tca-359", "salame: 457 contra 422"],
  ["ciqual-11018", "tca-314", "vinagre: 23 contra 22"],
  ["ciqual-6560", "tca-2120000006", "pá de vitela crua: 124 contra 138"],
  ["usda-169988", "tca-959", "aipo cru: 14 contra 15"],
  ["usda-173430", "tca-386", "manteiga sem sal: 717 contra 750, e a portuguesa tem 83 g de gordura"],
  ["usda-168820", "tca-1900000068", "melaço: 290 contra 269"],
  ["usda-174270", "tca-539", "soja em grão seca: a USDA conta os hidratos por diferença"],

  // --- duas medições que discordam: a USDA desempata ----------------------------------
  ["tca-579", "ciqual-20044", "abóbora crua: 11 contra 20, e a USDA diz 26"],
  ["tca-8", "ciqual-11000", "alho cru: 72 contra 109, e a USDA diz 149"],
  ["tca-597", "ciqual-20034", "cebola crua: 20 contra 39, e a USDA diz 40"],
  ["tca-600", "ciqual-20009", "cenoura crua: 25 contra 30, e a USDA diz 41"],
  ["ciqual-20059", "tca-608", "espinafres crus: 33 contra 27, e a USDA diz 23"],

  // --- discordam pouco e não há terceira fonte: fica a portuguesa ---------------------
  ["ciqual-20039", "tca-581", "alho-francês cru: 30 contra 26"],
  ["ciqual-20026", "tca-602", "chicória crua: 17 contra 14"],
  ["ciqual-20116", "tca-552", "couve branca crua: 35 contra 28"],

  /*
   * --- a segunda volta: o que a tradução revelou --------------------------------------
   *
   * Estas quinze não existiam antes de o vocabulário crescer. Eram o mesmo alimento
   * escondido em duas línguas — «Watercress, raw» e «Agrião cru» só colidem depois de o
   * primeiro passar a agrião. **A tradução não cria duplicados: mostra os que já lá
   * estavam**, e é por isso que vale a pena traduzir antes de arbitrar.
   */
  ["ciqual-20022", "tca-580", "agrião cru: 16 contra 29"],
  ["ciqual-20016", "tca-556", "couve-flor crua: 25 contra 34"],
  ["ciqual-20017", "tca-557", "couve-flor cozida: 21 contra 21"],
  ["ciqual-27016", "tca-831", "enguia crua: 240 contra 303"],
  ["ciqual-10030", "tca-922", "lagostim cru: 68 contra 89"],
  ["ciqual-58100", "tca-1900000014", "quiabo cru: 31 contra 37"],
  ["ciqual-20053", "tca-619", "beringela crua: 23 contra 21"],
  ["ciqual-26113", "tca-817", "carapau cru: 116 contra 105"],
  ["ciqual-20058", "tca-554", "couve-de-bruxelas crua: 44 contra 50"],
  ["ciqual-9480", "tca-1900000077", "farinha de espelta: 352 contra 339"],
  ["ciqual-28812", "tca-357", "presunto: 230 contra 215, e a TCA já tinha ganho o outro"],
  ["ciqual-26055", "tca-891", "solha crua: 89 contra 90"],

  // Sem tabela portuguesa, fica a europeia: o espelto e a quinoa são grãos que a CIQUAL
  // mede em produto do mesmo lado do Atlântico.
  ["usda-169745", "ciqual-9001", "espelta crua: 338 contra 344"],
  ["usda-168874", "ciqual-9340", "quinoa crua: 368 contra 358"],

  /*
   * --- a terceira volta: mais vocabulário, mais duplicados à vista -----------------------
   *
   * Todas do mesmo feitio — a CIQUAL e a TCA a terem o mesmo alimento, e o nome francês a
   * deixar de o esconder. **Cada lote de tradução traz outra leva**, e não é sinal de que a
   * tradução esteja a criar problemas: é o inventário a ficar legível.
   */
  ["ciqual-26082", "tca-834", "espadarte cru: 134 contra 97"],
  ["ciqual-26130", "tca-848", "maruca crua: 81 contra 70"],
  ["ciqual-30789", "tca-352", "mortadela: 310 contra 379"],
  ["ciqual-20045", "tca-614", "rabanete cru: 11 contra 15"],
  ["ciqual-26052", "tca-870", "raia crua: 92 contra 58"],
  ["ciqual-11014", "tca-6", "salsa fresca: 43 contra 20"],
  ["ciqual-11024", "tca-1226", "salsa seca: 291 contra 244"],
  ["ciqual-11005", "tca-1193", "caril em pó: 301 contra 342"],
  ["ciqual-20346", "tca-558", "couve-galega crua: 35 contra 32"],
  ["ciqual-13013", "tca-652", "figo seco: 261 contra 276"],
  ["ciqual-10001", "tca-914", "lula crua: 77 contra 71"],
  ["ciqual-24520", "tca-485", "merengue: 396 contra 353"],
  ["ciqual-26018", "tca-893", "tamboril cru: 67 contra 73"],
];

/**
 * Dois alimentos diferentes com o mesmo nome. **Não se funde: corrige-se o nome.**
 *
 * Quase todos são a tradução a aproximar o que as fontes distinguiam, ou o nome de uma fonte
 * a não dizer o que a distingue da outra. Fundi-los era apagar comida que existe.
 */
const RENOMEAR = [
  ["ciqual-4026", "Batata, assada no forno, sem gordura", "0,1 g de gordura contra 4,8 g da outra"],
  ["tca-587", "Batata assada no forno, com gordura", "idem"],
  ["tca-847", "Linguado frito", "6,2 g de gordura, e é o frito a sério"],
  ["ciqual-6510", "Vitela, costeleta com gordura, crua", "12,9 g de gordura contra 4,5 g"],
  ["tca-223", "Vitela, costeleta aparada, crua", "idem"],
  ["ciqual-5009", "Cerveja de trigo", "7,6 g de hidratos: é uma «blanche», não a branca daqui"],
  ["tca-726", "Cerveja branca", "0,5 g de hidratos e 3,7 % de álcool"],
  ["ciqual-4101", "Batata doce, crua", "77,3 g de água contra 67,2 g: variedades diferentes"],
  ["tca-593", "Batata doce roxa, crua", "idem"],
  ["ciqual-10021", "Camarão, cru", "74,3 g de água contra 79,2 g"],
  ["tca-969", "Camarão branco, cru", "idem"],
  ["ciqual-20189", "Tomate seco", "257 kcal contra 296"],
  ["tca-230001", "Tomate seco em azeite", "3,9 g de gordura"],
  ["usda-169697", "Farinha de milho grossa, integral, amarela", "«cornmeal» é grossa"],
  ["usda-170290", "Farinha de milho, integral, amarela", "«corn flour» é fina"],
  ["usda-169750", "Farinha de milho grossa, integral, branca", "idem"],
  ["usda-169748", "Farinha de milho, integral, branca", "idem"],
  ["usda-169932", "Pêssego desidratado, sulfurado, cru", "325 kcal: é o de baixa humidade"],
  ["usda-169934", "Pêssego seco, sulfurado, cru", "239 kcal"],
  ["usda-171247", "Queijo parmesão ralado", "28,4 g de proteína"],
  ["usda-173431", "Queijo parmesão em lascas", "37,9 g de proteína"],
  ["usda-172809", "Pão de glúten, torrado", "a torragem tira água e concentra"],
  ["usda-174917", "Pão de glúten", "245 kcal contra 270"],
  ["usda-171853", "Panquecas integrais, preparado seco", "os dois eram o mesmo com um hífen a menos"],
];

// ------------------------------------------------------------------------------ escrever

const fusoes = JSON.parse(readFileSync(FUSOES, "utf8"));
fusoes.fusoes = Object.fromEntries(FUNDIR.map(([perdedor, vencedor]) => [perdedor, vencedor]));
fusoes.porque = Object.fromEntries(FUNDIR.map(([perdedor, , porque]) => [perdedor, porque]));

const vencedores = new Set(Object.values(fusoes.fusoes));
const emCadeia = Object.keys(fusoes.fusoes).filter((p) => vencedores.has(p));
if (emCadeia.length) {
  console.error(`Fusões em cadeia — o perdedor de uma é o vencedor de outra: ${emCadeia}`);
  process.exit(1);
}

writeFileSync(FUSOES, JSON.stringify(fusoes, null, 2) + "\n");

const correcoes = JSON.parse(readFileSync(CORRECOES, "utf8"));
const antes = Object.keys(correcoes.nomes).length;
for (const [id, nome] of RENOMEAR) correcoes.nomes[id] = nome;

/**
 * Um nome corrigido para um alimento que foi fundido é peso morto.
 *
 * O alimento já não existe, e a linha fica no ficheiro a apontar para lado nenhum — a somar
 * ao número que o teste-guarda conta e a fazer parecer que há mais curadoria do que há.
 * Tira-se aqui e não à mão, para não voltar na arbitragem seguinte.
 */
let mortos = 0;
for (const [perdedor] of FUNDIR) {
  if (correcoes.nomes[perdedor] != null) {
    delete correcoes.nomes[perdedor];
    mortos++;
  }
}

// O ficheiro tem 2 704 nomes juntados ao longo de meses. Escrever menos do que se leu é a
// forma de os perder sem dar erro.
const depois = Object.keys(correcoes.nomes).length;
if (depois < antes - mortos) {
  console.error(`A escrita ia perder ${antes - depois - mortos} nomes a mais do que os fundidos.`);
  process.exit(1);
}
writeFileSync(CORRECOES, JSON.stringify(correcoes, null, 2) + "\n");

console.log(`arbitragem: ${FUNDIR.length} fusões, ${RENOMEAR.length} nomes desambiguados`);
console.log(`  nomes em correcoes.json: ${antes} → ${depois} (${mortos} eram de alimentos fundidos)`);
