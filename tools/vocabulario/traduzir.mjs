/**
 * Traduz nomes de alimentos **segmento a segmento**, com concordância.
 *
 * Os nomes das tabelas não são frases: são uma base e uma lista de qualificadores separados
 * por vírgulas — «Rice, wild, raw», «Beef, shoulder, braised». Os mesmos qualificadores
 * repetem-se por milhares de alimentos, e é por isso que se traduz o **vocabulário** e não os
 * nomes: `raw` traduz-se uma vez, não seiscentas e vinte e nove.
 *
 * **A concordância não é um luxo.** Sem ela sai «Carne, cru» e «Feijões, cozido», que se lê
 * como uma tradução automática e mina a confiança em tudo o resto do catálogo. O género e o
 * número vêm da **base** — o primeiro segmento —, e os qualificadores seguem-na. É assim que
 * a própria TCA do INSA escreve os nomes dela.
 *
 * **Um nome só é aplicado quando está inteiro.** Meio traduzido — «Arroz, wild, cru» — é pior
 * do que em inglês: parece um defeito, e ninguém sabe se o alimento é o que diz ser. Os
 * segmentos que faltam ficam listados, e é essa lista que enche a fila da oficina.
 */
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));

/**
 * Lê o dicionário. Cada linha é `ingles;portugues;tipo;genero;numero`, com ponto e vírgula
 * porque as vírgulas fazem parte dos segmentos que se traduzem.
 *
 * - `nome` — a base. Traz género (`m`/`f`) e número (`s`/`p`), e é ela que manda nos outros.
 * - `nome-de` — uma base que além disso compõe com «de»: farinha, sumo, caldo, molho. É a
 *   marca que separa «chestnut flour» → «farinha de castanha» de «rice cake» → **não** «bolo
 *   de arroz», que é outra coisa. Sem ela, a composição inventava com confiança.
 * - `qualificador` — concorda com a base. Guarda-se no masculino singular; as outras três
 *   formas derivam-se, porque em português a flexão do adjetivo é regular.
 * - `expressao` — não flete. «sem sal», «pronto a comer», nomes próprios.
 */
export function lerVocabulario(ficheiro = join(HERE, "segmentos.csv")) {
  const mapa = new Map();
  for (const linha of readFileSync(ficheiro, "utf8").split(/\r?\n/)) {
    const texto = linha.trim();
    if (!texto || texto.startsWith("#")) continue;
    const [ingles, portugues, tipo = "expressao", genero = "", numero = "s"] = texto.split(";");
    if (!ingles || !portugues) continue;
    mapa.set(ingles.trim().toLowerCase(), {
      portugues: portugues.trim(),
      tipo: tipo.trim(),
      genero: genero.trim(),
      numero: numero.trim(),
    });
  }
  return mapa;
}

/**
 * Flexiona um qualificador para o género e número da base.
 *
 * As regras são as da gramática, e são regulares para a esmagadora maioria: só os adjetivos
 * terminados em `-o` marcam género, e o plural depende da última letra. Um qualificador que
 * fuja a isto escreve-se no dicionário já com as duas formas, separadas por barra.
 */
export function flexionar(palavra, genero, numero) {
  if (palavra.includes("/")) {
    const [masculino, feminino] = palavra.split("/");
    return pluralizar(genero === "f" ? feminino : masculino, numero);
  }

  // A flexão aplica-se à última palavra: «cozido em água» faz «cozida em água» e não
  // «cozido em águas».
  const partes = palavra.split(" ");
  const ultima = partes.length === 1 ? partes[0] : partes[0];
  const resto = partes.length === 1 ? "" : ` ${partes.slice(1).join(" ")}`;

  const comGenero = genero === "f" && ultima.endsWith("o")
    ? `${ultima.slice(0, -1)}a`
    : ultima;

  return pluralizar(comGenero, numero) + resto;
}

function pluralizar(palavra, numero) {
  if (numero !== "p") return palavra;
  if (/[aeiouãõ]$/.test(palavra)) return `${palavra}s`;
  if (palavra.endsWith("l")) return `${palavra.slice(0, -1)}is`;
  if (palavra.endsWith("m")) return `${palavra.slice(0, -1)}ns`;
  if (/[rz]$/.test(palavra)) return `${palavra}es`;

  // Terminado em `s` já pode ser plural — «simples», «grátis». Não se mexe.
  return palavra;
}

/**
 * As preposições que abrem um segmento, e o que se lhes segue.
 *
 * «with vegetables» é «com produtos hortícolas»: a preposição traduz-se e o resto do segmento
 * volta a passar por aqui. É uma regra e não uma lista, e é o que evita escrever à mão as
 * duzentas e quarenta e duas combinações que começam por `with`.
 */
const PREPOSICOES = {
  with: "com",
  without: "sem",
  in: "em",
  from: "de",
  for: "para",
  of: "de",
};

/**
 * Um segmento de duas palavras que o dicionário não tem, mas cujas duas partes tem.
 *
 * **O inglês compõe ao contrário do português.** «Chicken broth» é «caldo de frango» e não
 * «frango caldo»: o núcleo vem à frente e o que o qualifica vem atrás, com preposição quando
 * é um nome e sem ela quando é um adjetivo.
 *
 * Aplica-se **só** quando as duas palavras estão no dicionário e o tipo de cada uma é
 * conhecido. Adivinhar uma delas era exactamente o que este ficheiro existe para não fazer.
 */
export function compor(segmento, vocabulario, profundidade = 0) {
  // Duas voltas chegam: «with sauce» e «in tomato sauce». Mais do que isso é o sinal de que
  // o segmento precisa de ser escrito à mão, não composto.
  if (profundidade > 2) return null;

  const palavras = segmento.trim().split(/\s+/);

  const preposicao = PREPOSICOES[palavras[0]?.toLowerCase()];
  if (preposicao && palavras.length > 1) {
    const resto = palavras.slice(1).join(" ");
    const traduzido = vocabulario.get(resto.toLowerCase())?.portugues
      ?? compor(resto, vocabulario, profundidade + 1);
    return traduzido ? `${preposicao} ${traduzido}` : null;
  }

  // «Lentils» quando o dicionário só tem «lentil». O plural inglês é regular o suficiente
  // para se desfazer, e o português já se sabe formar — ver [pluralizar].
  if (palavras.length === 1) {
    const singular = singularIngles(palavras[0].toLowerCase());
    const entrada = singular && vocabulario.get(singular);
    return entrada?.tipo.startsWith("nome") ? pluralizar(entrada.portugues, "p") : null;
  }

  if (palavras.length !== 2) return null;

  const [primeira, segunda] = palavras.map((p) => vocabulario.get(p.toLowerCase()));
  if (!primeira || !segunda || !segunda.tipo.startsWith("nome")) return null;

  // «Frankfurter sausage» daria «salsicha de salsicha de Frankfurt»: o inglês repete o
  // núcleo que o português já traz dentro da outra palavra. Quando uma tradução já contém a
  // outra, a composição não acrescenta nada — acrescenta ruído.
  if (contem(primeira.portugues, segunda.portugues)) return primeira.portugues;
  if (contem(segunda.portugues, primeira.portugues)) return segunda.portugues;

  // «dark chocolate» → «chocolate escuro». O adjetivo concorda com o núcleo, que agora está
  // à frente dele.
  if (primeira.tipo === "qualificador") {
    return `${segunda.portugues} ${flexionar(primeira.portugues, segunda.genero, segunda.numero)}`;
  }

  /**
   * «chicken broth» → «caldo de frango». Sem artigo: os nomes das tabelas não o levam.
   *
   * **Só quando o núcleo está marcado como `nome-de`.** Um composto de dois nomes em inglês
   * não quer dizer «o segundo do primeiro»: «rice cake» não é um bolo de arroz — é uma
   * tortita —, e a regra produzia-o com toda a confiança. Os núcleos marcados são aqueles em
   * que a construção «X de Y» é a portuguesa: farinha, sumo, caldo, óleo, molho.
   *
   * A precisão importa mais do que o alcance: um nome traduzido a mais e mal é pior do que
   * um nome por traduzir, porque o primeiro ninguém volta a olhar.
   */
  if (primeira.tipo.startsWith("nome") && segunda.tipo === "nome-de") {
    return `${segunda.portugues} de ${primeira.portugues}`;
  }

  return null;
}

/** O singular de um plural inglês, ou nulo se a palavra já é singular. */
function singularIngles(palavra) {
  if (palavra.endsWith("ies")) return `${palavra.slice(0, -3)}y`;
  if (/(ches|shes|sses|xes|oes)$/.test(palavra)) return palavra.slice(0, -2);
  if (palavra.endsWith("s") && !palavra.endsWith("ss")) return palavra.slice(0, -1);
  return null;
}

const contem = (maior, menor) =>
  maior.toLowerCase().split(" ").includes(menor.toLowerCase()) ||
  maior.toLowerCase().startsWith(`${menor.toLowerCase()} `);

/**
 * O género e o número do núcleo de um segmento composto.
 *
 * Sem isto, «Mixed vegetables, frozen, raw» dava «Produtos hortícolas misturados, congelado,
 * cru»: a base era composta, ninguém sabia que era masculina plural, e os qualificadores
 * ficavam todos no masculino singular. O núcleo de uma composição é sempre a **segunda**
 * palavra em inglês, que é a primeira em português.
 */
function concordanciaDe(segmento, vocabulario) {
  const exacta = vocabulario.get(segmento.trim().toLowerCase());
  if (exacta) return { genero: exacta.genero || "m", numero: exacta.numero || "s" };

  const palavras = segmento.trim().split(/\s+/);
  const ultima = palavras[palavras.length - 1]?.toLowerCase();
  const nucleo = vocabulario.get(ultima);
  if (nucleo) return { genero: nucleo.genero || "m", numero: nucleo.numero || "s" };

  // Desfeito o plural inglês, o número passa a ser plural — e é ele que faz «Lentilhas,
  // cruas» em vez de «Lentilhas, cru».
  const singular = ultima && singularIngles(ultima);
  const doSingular = singular && vocabulario.get(singular);
  if (doSingular) return { genero: doSingular.genero || "m", numero: "p" };

  return { genero: "m", numero: "s" };
}

/**
 * Traduz um nome inteiro. Devolve sempre o que conseguiu **e** o que faltou, porque quem
 * chama tem de poder decidir entre aplicar e mandar para a fila.
 */
export function traduzirNome(nomeEn, vocabulario) {
  const segmentos = String(nomeEn ?? "").split(",").map((s) => s.trim()).filter(Boolean);
  if (!segmentos.length) return { nome: null, completo: false, porTraduzir: [] };

  const { genero, numero } = concordanciaDe(segmentos[0], vocabulario);

  const porTraduzir = [];
  const traduzidos = segmentos.map((s, i) => {
    const entrada = vocabulario.get(s.toLowerCase());
    if (!entrada) {
      // A composição é a segunda tentativa, e não a primeira: uma entrada escrita à mão
      // ganha sempre à regra, porque a língua tem mais excepções do que regras.
      const composto = compor(s, vocabulario);
      if (composto) return composto;
      porTraduzir.push(s);
      return s;
    }
    if (i === 0 || entrada.tipo !== "qualificador") return entrada.portugues;
    return flexionar(entrada.portugues, genero, numero);
  });

  const nome = maiusculaInicial(traduzidos.join(", "));
  return { nome, completo: porTraduzir.length === 0, porTraduzir };
}

function maiusculaInicial(texto) {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}
