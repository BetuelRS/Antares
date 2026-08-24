/**
 * O que a oficina escreve, e onde.
 *
 * Tudo vai para o mesmo sítio — `tools/catalogo/correcoes.json` —, que é o ficheiro por onde
 * o oleoduto já aplica o que os dezoito passos do semeador tinham decidido. Uma decisão de
 * curadoria não é diferente de uma correção antiga: é uma coisa que se sabe sobre um alimento
 * e que a fonte não diz.
 *
 * **A função é pura de propósito.** Recebe as correções e a decisão, devolve correções novas,
 * e não toca no disco. É o que a torna verificável: o modo de falhar de uma ferramenta que
 * escreve num ficheiro de 2 707 nomes é apagar 2 706 deles sem ninguém reparar até à
 * construção seguinte.
 */

/** As decisões que a oficina sabe tomar. Uma de cada vez, sobre um alimento de cada vez. */
export const DECISOES = ["nome", "porcao", "liquido", "podar", "verificado"];

export function aplicar(correcoes, decisao) {
  const { id, tipo } = decisao;
  if (!id) throw new Error("decisão sem alimento");
  if (!DECISOES.includes(tipo)) throw new Error(`decisão desconhecida: ${tipo}`);

  // Cópia rasa mais cópia de cada colecção que se toca. Sem isto, a função devolvia um objecto
  // novo com as mesmas listas lá dentro, e quem guardasse o anterior via-o mudar debaixo dos
  // pés — que é a falha que uma função pura existe para não ter.
  const saida = {
    ...correcoes,
    nomes: { ...(correcoes.nomes ?? {}) },
    porcoes: { ...(correcoes.porcoes ?? {}) },
    liquidos: [...(correcoes.liquidos ?? [])],
    podados: [...(correcoes.podados ?? [])],
    verificados: [...(correcoes.verificados ?? [])],
  };

  switch (tipo) {
    case "nome":
      escreverNome(saida, id, decisao.valor);
      break;
    case "porcao":
      escreverPorcao(saida, id, decisao);
      break;
    case "liquido":
      alternar(saida.liquidos, id, decisao.valor);
      break;
    case "verificado":
      alternar(saida.verificados, id, decisao.valor);
      break;
    case "podar":
      alternar(saida.podados, id, decisao.valor);
      break;
  }

  return saida;
}

function escreverNome(saida, id, valor) {
  const nome = String(valor ?? "").trim();

  // Apagar a entrada, e não gravar vazio: sem nome escrito, o alimento fica com o que a fonte
  // diz — que é o comportamento certo e o único modo de desfazer um nome mal posto.
  if (!nome) delete saida.nomes[id];
  else saida.nomes[id] = nome;
}

function escreverPorcao(saida, id, { nome, gramas }) {
  const g = Number(gramas);
  if (!nome || !Number.isFinite(g) || g <= 0) {
    delete saida.porcoes[id];
    return;
  }
  saida.porcoes[id] = { nome: String(nome).trim(), gramas: g };
}

/** Entra ou sai da lista, conforme o valor. As listas ficam ordenadas, para o `git diff` servir. */
function alternar(lista, id, ligado) {
  const i = lista.indexOf(id);
  if (ligado && i === -1) {
    lista.push(id);
    lista.sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  }
  if (!ligado && i !== -1) lista.splice(i, 1);
}
