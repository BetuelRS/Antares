/**
 * A oficina de curadoria. Uma página local, um alimento de cada vez.
 *
 *     node tools/oficina/servidor.mjs
 *     → http://127.0.0.1:4173
 *
 * As novecentas traduções, as arbitragens e as mil e quinhentas porções **não se fazem num
 * editor de texto**. Fazem-se ao longo de meses, e só acontecem se forem agradáveis de fazer.
 * É essa a única razão de esta ferramenta existir.
 *
 * **Só escuta em 127.0.0.1**, e é de propósito: escreve num ficheiro do repositório, e não há
 * nenhuma versão disto que deva estar ao alcance da rede local. Não vai para a app, não vai
 * para o servidor, não tem autenticação nenhuma porque não precisa de ter.
 */
import { createServer } from "node:http";
import { readFileSync, writeFileSync, appendFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { montarFila, segmentos, porTraduzir } from "./fila.mjs";
import { aplicar } from "./decisoes.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const RAIZ = join(HERE, "..", "..");
const CATALOGO = join(RAIZ, "composeApp", "src", "commonMain", "composeResources", "files", "catalogo.json");
const QUALIDADE = join(RAIZ, "tools", "catalogo", "qualidade.json");
const CORRECOES = join(RAIZ, "tools", "catalogo", "correcoes.json");
const HISTORICO = join(HERE, "historico.json");
const POR_TRADUZIR = join(RAIZ, "tools", "vocabulario", "por-traduzir.json");
const SEGMENTOS = join(RAIZ, "tools", "vocabulario", "segmentos.csv");
const COLISOES = join(RAIZ, "tools", "catalogo", "colisoes.json");
const FUSOES = join(RAIZ, "tools", "catalogo", "fusoes.json");
const PAGINA = join(HERE, "oficina.html");

const PORTA = 4173;

// Chega para uma sessão de trabalho e não pendura o browser com oito mil cartões.
const LIMITE_DA_FILA = 300;

const ler = (p, omissao) => (existsSync(p) ? JSON.parse(readFileSync(p, "utf8")) : omissao);

const catalogo = ler(CATALOGO, { alimentos: [] });
const qualidade = ler(QUALIDADE, { contradicoes: [], suspeitas: [] });
const historico = ler(HISTORICO, {});

const achados = [...qualidade.contradicoes, ...qualidade.suspeitas];
const porId = new Map(catalogo.alimentos.map((a) => [a.id, a]));

const fila = montarFila({ alimentos: catalogo.alimentos, achados, historico });

console.log(`oficina: ${fila.length} alimentos, ${achados.length} achados, ` +
  `${Object.keys(historico).length} com histórico`);
if (!Object.keys(historico).length) {
  console.log("  sem histórico do telemóvel — a fila vem ordenada por número de achados.");
  console.log("  para a ordenar pelo que comes: node tools/oficina/historico.mjs");
}

/**
 * O cartão de um alimento: tudo o que é preciso para decidir, sem ir buscar mais nada.
 *
 * Os achados vêm já em palavras, do motor de qualidade. A oficina não repete as contas dele:
 * duas implementações da mesma verificação divergem, e a que se vê no ecrã passaria a ser
 * diferente da que chumba a construção.
 */
function cartao(id) {
  const a = porId.get(id);
  if (!a) return null;
  const correcoes = ler(CORRECOES, {});
  return {
    alimento: a,
    achados: achados.filter((x) => x.id === id),
    registos: historico[id] ?? 0,
    segmentosEn: segmentos(a.nameEn),
    segmentosPt: segmentos(a.namePt),
    porTraduzir: porTraduzir(a.nameEn, a.namePt),
    decidido: {
      nome: correcoes.nomes?.[id] ?? null,
      porcao: correcoes.porcoes?.[id] ?? null,
      liquido: (correcoes.liquidos ?? []).includes(id),
      verificado: (correcoes.verificados ?? []).includes(id),
      podado: (correcoes.podados ?? []).includes(id),
    },
  };
}

const TIPOS = ["nome", "qualificador", "expressao"];

/**
 * Três alimentos que esperam por este segmento. Sem eles, quem traduz está a decidir às
 * cegas: «club» quer dizer coisas diferentes num sanduíche e numa bebida, e a única maneira
 * de saber qual é olhar para os nomes onde aparece.
 */
function exemplosDe(segmento) {
  const alvo = segmento.toLowerCase();
  const encontrados = [];
  for (const a of catalogo.alimentos) {
    if (!a.nameEn.toLowerCase().split(",").some((s) => s.trim() === alvo)) continue;
    encontrados.push(a.nameEn);
    if (encontrados.length === EXEMPLOS) break;
  }
  return encontrados;
}

const EXEMPLOS = 3;

function responder(res, corpo, tipo = "application/json; charset=utf-8") {
  res.writeHead(200, { "Content-Type": tipo });
  res.end(typeof corpo === "string" ? corpo : JSON.stringify(corpo));
}

const servidor = createServer((req, res) => {
  const url = new URL(req.url, `http://127.0.0.1:${PORTA}`);

  if (url.pathname === "/") {
    return responder(res, readFileSync(PAGINA, "utf8"), "text/html; charset=utf-8");
  }

  if (url.pathname === "/api/fila") {
    const so = url.searchParams.get("achados") === "1";
    const lista = (so ? fila.filter((f) => f.achados.length) : fila).slice(0, LIMITE_DA_FILA);
    return responder(res, { total: fila.length, lista });
  }

  if (url.pathname === "/api/alimento") {
    const c = cartao(url.searchParams.get("id"));
    if (!c) {
      res.writeHead(404, { "Content-Type": "application/json" });
      return res.end(JSON.stringify({ erro: "não há alimento com esse identificador" }));
    }
    return responder(res, c);
  }

  /**
   * A fila dos segmentos por traduzir, por quantos alimentos cada um desbloqueia.
   *
   * **É aqui que está a alavanca.** Traduzir um alimento arruma um alimento; traduzir o
   * segmento que lhe falta arruma todos os que esperam pelo mesmo. A cauda dos nomes é
   * longa — quatro mil segmentos distintos —, e percorrê-la por ordem alfabética era gastar
   * as primeiras horas nos que ninguém procura.
   */
  if (url.pathname === "/api/segmentos") {
    const j = ler(POR_TRADUZIR, { segmentos: {} });
    const lista = Object.entries(j.segmentos)
      .slice(0, LIMITE_DA_FILA)
      .map(([segmento, alimentos]) => ({ segmento, alimentos, exemplos: exemplosDe(segmento) }));
    return responder(res, { ...j, lista });
  }

  if (url.pathname === "/api/segmento" && req.method === "POST") {
    let corpo = "";
    req.on("data", (p) => { corpo += p; });
    req.on("end", () => {
      try {
        const { ingles, portugues, tipo = "expressao", genero = "", numero = "s" } = JSON.parse(corpo);
        if (!ingles || !portugues) throw new Error("faltam o inglês ou o português");
        if (!TIPOS.includes(tipo)) throw new Error(`tipo desconhecido: ${tipo}`);

        // Acrescenta-se no fim e não se reescreve o ficheiro: as seiscentas linhas que lá
        // estão são trabalho de horas, e um `writeFileSync` mal feito apaga-as todas.
        appendFileSync(
          SEGMENTOS,
          `${ingles.trim().toLowerCase()};${portugues.trim()};${tipo};${genero};${numero}\n`,
        );
        responder(res, { guardado: true });
      } catch (e) {
        res.writeHead(400, { "Content-Type": "application/json; charset=utf-8" });
        res.end(JSON.stringify({ erro: String(e.message ?? e) }));
      }
    });
    return undefined;
  }

  /**
   * As colisões de nome, com as que discordam na energia à frente.
   *
   * Duas linhas com os mesmos números são uma duplicação inofensiva: escolha-se qualquer uma
   * e o dia fica igual. A sangria a 89 kcal numa e a 120 na outra é outra coisa — é a app a
   * dar duas respostas à mesma pergunta conforme a linha que a pessoa tocar.
   */
  if (url.pathname === "/api/colisoes") {
    const j = ler(COLISOES, { colisoes: [] });
    const decididas = ler(FUSOES, { fusoes: {} }).fusoes ?? {};
    return responder(res, {
      ...j,
      colisoes: j.colisoes.map((c) => ({
        ...c,
        decidido: c.alimentos.find((a) => decididas[a.id])?.id ?? null,
      })),
    });
  }

  if (url.pathname === "/api/fusao" && req.method === "POST") {
    let corpo = "";
    req.on("data", (p) => { corpo += p; });
    req.on("end", () => {
      try {
        const { perdedor, vencedor } = JSON.parse(corpo);
        if (!perdedor || !vencedor) throw new Error("falta o perdedor ou o vencedor");
        if (perdedor === vencedor) throw new Error("um alimento não se funde consigo próprio");

        const ficheiro = ler(FUSOES, { fusoes: {} });
        const fusoes = { ...(ficheiro.fusoes ?? {}) };

        // Uma cadeia de fusões — A vai para B e B vai para C — deixava quem seguisse a
        // primeira lápide num alimento que também já não existe.
        if (fusoes[vencedor]) throw new Error(`o vencedor já foi fundido em ${fusoes[vencedor]}`);

        fusoes[perdedor] = vencedor;
        writeFileSync(FUSOES, JSON.stringify({ ...ficheiro, fusoes }, null, 2) + "\n");
        responder(res, { guardado: true, total: Object.keys(fusoes).length });
      } catch (e) {
        res.writeHead(400, { "Content-Type": "application/json; charset=utf-8" });
        res.end(JSON.stringify({ erro: String(e.message ?? e) }));
      }
    });
    return undefined;
  }

  if (url.pathname === "/api/decisao" && req.method === "POST") {
    let corpo = "";
    req.on("data", (p) => { corpo += p; });
    req.on("end", () => {
      try {
        const antes = ler(CORRECOES, {});
        const depois = aplicar(antes, JSON.parse(corpo));

        // O ficheiro tem 2 707 nomes lá dentro. Escrever menos do que se leu é o modo de
        // falhar desta ferramenta, e é silencioso até à construção seguinte.
        const perdidos = Object.keys(antes.nomes ?? {}).length -
          Object.keys(depois.nomes ?? {}).length;
        if (perdidos > 1) throw new Error(`a escrita ia perder ${perdidos} nomes`);

        writeFileSync(CORRECOES, JSON.stringify(depois, null, 2) + "\n");
        responder(res, { guardado: true, decidido: cartao(JSON.parse(corpo).id)?.decidido });
      } catch (e) {
        res.writeHead(400, { "Content-Type": "application/json; charset=utf-8" });
        res.end(JSON.stringify({ erro: String(e.message ?? e) }));
      }
    });
    return undefined;
  }

  res.writeHead(404);
  return res.end();
});

servidor.listen(PORTA, "127.0.0.1", () => {
  console.log(`\n  http://127.0.0.1:${PORTA}\n`);
});
