/**
 * O motor de qualidade: perguntas que um alimento tem de conseguir responder sobre si próprio.
 *
 * Nenhuma destas verificações vai à fonte confirmar nada. Todas comparam o alimento **consigo
 * mesmo** — a energia com os macros que a produzem, a massa com os cem gramas que ela tem de
 * caber, as partes com o todo. É o que as torna baratas e é o que as torna certas: um alimento
 * que se contradiz está errado independentemente do que a fonte diga.
 *
 * **Duas gravidades, e a diferença não é de grau.**
 *
 * - `contradicao` — o número é impossível. A saturada não pode ser mais do que a gordura toda;
 *   cento e dez gramas não cabem em cem. Isto **chumba a construção**, a menos que esteja
 *   declarado em `qualidade.json`, como os desvios.
 * - `suspeita` — o número é improvável. A energia não bate com os macros, o alimento está fora
 *   de escala no seu grupo. Não chumba nada: enche a fila da oficina.
 *
 * A razão de a suspeita não chumbar é que ela mede uma **discordância**, não um erro. As fontes
 * publicam energias medidas por bomba calorimétrica e macros medidos por outros métodos, e as
 * duas coisas não fecham ao cêntimo. Chumbar a construção por isso era não poder publicar até
 * a ANSES corrigir a tabela dela.
 */

const KCAL_PROTEINA = 4;
const KCAL_HIDRATOS = 4;
const KCAL_GORDURA = 9;
const KCAL_ALCOOL = 7;
const KCAL_FIBRA = 2;

/**
 * Os polióis, ao factor do regulamento 1169/2011 da UE. Estão fora dos hidratos declarados e
 * dão energia à mesma — sem eles, a conta acusava os rebuçados e as pastilhas sem açúcar de
 * declararem duzentas kcal saídas do nada. Eram quinze dos cento e vinte primeiros achados.
 */
const KCAL_POLIOIS = 2.4;

/** Cem gramas mais o que os métodos deixam por fechar. Ver [massa]. */
const GRAMAS_NUM_ALIMENTO = 100;

export const LIMITES = {
  /**
   * A energia é dada a **dois algarismos** na maior parte das fontes, e os macros a um. Só o
   * arredondamento já dá alguns kcal de folga num alimento denso. A percentagem é o que manda
   * acima das 100 kcal; o piso absoluto é o que impede um alimento de 12 kcal de ser acusado
   * por 4 kcal de diferença.
   */
  atwaterPercentagem: 0.15,
  atwaterMinimo: 20,

  /**
   * Só se olha para o que passa dos cem gramas. Ficar **abaixo** é o normal: falta a cinza, que
   * nenhuma das fontes que lemos publica, e faltam os ácidos orgânicos.
   */
  massaFolga: 3,

  /** As partes contra o todo. A folga é o arredondamento de três números somados. */
  somaFolga: 0.6,

  /**
   * Fora de escala no grupo: quantas vezes a distância mediana ao valor mediano do subgrupo.
   * Não é desvio-padrão porque a distribuição de energia dentro de um subgrupo não é normal —
   * um único óleo põe a média do grupo dos molhos onde ela não descreve nada.
   */
  escalaVezes: 6,
  escalaMinimoNoGrupo: 8,
};

/**
 * O valor, se for mesmo um valor.
 *
 * Desde a v29 um micronutriente pode ser um estado em vez de um número — `"vestigios"`,
 * `"<0.03"` —, e um estado numa conta não dá erro: dá `NaN`, e `NaN > 100` é falso. As
 * verificações passavam a não acusar nada e ninguém dava por isso. Aconteceu ao contrário na
 * primeira medição, que acusou catorze óleos e pães de somarem `NaN` gramas.
 */
function numero(v) {
  return typeof v === "number" && Number.isFinite(v) ? v : null;
}

/** Um achado. O `id` e o `campo` são o que a oficina precisa para abrir o alimento certo. */
function achado(alimento, tipo, gravidade, mensagem, campo) {
  return {
    id: alimento.id,
    nome: alimento.nameEn ?? alimento.namePt,
    origem: alimento.origin ?? "?",
    tipo,
    gravidade,
    campo,
    mensagem,
  };
}

const arredondar = (v, casas = 1) => Math.round(v * 10 ** casas) / 10 ** casas;

/**
 * A energia contra os macros que a produzem.
 *
 * **Duas contas, e fica a que estiver mais perto.** As fontes não concordam sobre o que
 * «hidratos» quer dizer: a CIQUAL publica os hidratos *disponíveis*, e a fibra conta à parte a
 * 2 kcal; a USDA publica hidratos *por diferença*, que já têm a fibra lá dentro e não a podem
 * voltar a somar. Aplicar uma só fórmula ao catálogo inteiro acusava milhares de alimentos de
 * uma discordância que é nossa e não deles.
 */
export function atwater(a) {
  const kcal = numero(a.kcal);
  const prot = numero(a.proteinG);
  const hidratos = numero(a.carbsG);
  const gordura = numero(a.fatG);
  if (kcal == null || prot == null || hidratos == null || gordura == null) return null;

  const base =
    KCAL_PROTEINA * prot + KCAL_HIDRATOS * hidratos + KCAL_GORDURA * gordura +
    KCAL_ALCOOL * (numero(a.micros?.alcohol_g) ?? 0);

  /**
   * Os dois termos que podem estar dentro dos hidratos ou fora deles, conforme a fonte. A
   * fibra está fora nos hidratos *disponíveis* da CIQUAL e dentro nos hidratos *por
   * diferença* da USDA; os polióis seguem a mesma regra e foram medidos: somá-los sempre
   * levou os achados de 106 para 113, o que só se explica por já estarem lá dentro na maior
   * parte dos casos.
   *
   * Todas as combinações, e fica a que estiver mais perto do que a fonte declara. Não é
   * benevolência: é não acusar um alimento de uma discordância que é da nossa fórmula.
   */
  const opcionais = [
    KCAL_FIBRA * (numero(a.fiberG) ?? 0),
    KCAL_POLIOIS * (numero(a.micros?.polyols_g) ?? 0),
  ];
  const candidatos = [base];
  for (const extra of opcionais) {
    for (const c of [...candidatos]) candidatos.push(c + extra);
  }

  const diferenca = candidatos
    .map((c) => kcal - c)
    .reduce((melhor, d) => (Math.abs(d) < Math.abs(melhor) ? d : melhor));

  const tolerancia = Math.max(LIMITES.atwaterMinimo, kcal * LIMITES.atwaterPercentagem);
  if (Math.abs(diferenca) <= tolerancia) return null;

  return achado(
    a,
    "atwater",
    "suspeita",
    `energia declarada ${kcal} kcal, ${arredondar(diferenca)} kcal acima do que os macros dao`,
    "kcal",
  );
}

/**
 * O que está declarado tem de caber em cem gramas.
 *
 * Passar dos cem não é improvável, é impossível — e quando acontece é quase sempre uma unidade
 * trocada, que é o erro que mais estraga uma refeição contada.
 */
export function massa(a) {
  const agua = numero(a.micros?.water_g);
  if (agua == null) return null;

  /**
   * **Só a água e os três macros.** A fibra e o álcool ficam de fora porque cada um deles
   * pode já estar dentro de outra parcela, e as duas sobreposições são conhecidas:
   *
   * - A fibra está dentro dos hidratos sempre que a fonte os publica *por diferença*, como a
   *   USDA e a TCA fazem — e fora deles quando os publica *disponíveis*, como a CIQUAL. É a
   *   mesma ambiguidade que obriga o [atwater] a fazer duas contas.
   * - O álcool está dentro da água sempre que a humidade foi medida por secagem, porque o
   *   álcool também evapora com ela. Foi o que pôs três vinhos da TCA a somarem mais de cem
   *   gramas na primeira medição.
   *
   * Somar as quatro parcelas certas e deixar de fora as duas ambíguas dá a **menor** leitura
   * que os números declarados permitem. Uma contradição tem de ser indefensável — não uma
   * escolha infeliz de fórmula.
   */
  const soma = agua + (numero(a.proteinG) ?? 0) + (numero(a.carbsG) ?? 0) +
    (numero(a.fatG) ?? 0);
  if (soma <= GRAMAS_NUM_ALIMENTO + LIMITES.massaFolga) return null;

  return achado(
    a,
    "massa",
    "contradicao",
    `agua e macros somam ${arredondar(soma)} g em 100 g`,
    "water_g",
  );
}

/** As partes contra o todo: as três gorduras não podem passar a gordura, o açúcar os hidratos. */
export function partes(a) {
  const achados = [];

  const gordura = numero(a.fatG);
  const saturada = numero(a.satFatG);
  const mono = numero(a.micros?.fatMono_g);
  const poli = numero(a.micros?.fatPoly_g);
  if (gordura != null && (saturada != null || mono != null || poli != null)) {
    const soma = (saturada ?? 0) + (mono ?? 0) + (poli ?? 0);
    if (soma > gordura + LIMITES.somaFolga) {
      achados.push(achado(
        a,
        "gorduras",
        "contradicao",
        `saturada, mono e poli somam ${arredondar(soma)} g e a gordura total e ${gordura} g`,
        "fatG",
      ));
    }
  }

  const hidratos = numero(a.carbsG);
  const acucares = numero(a.sugarsG);
  if (hidratos != null && acucares != null && acucares > hidratos + LIMITES.somaFolga) {
    achados.push(achado(
      a,
      "acucares",
      "contradicao",
      `acucares ${acucares} g e hidratos ${hidratos} g`,
      "sugarsG",
    ));
  }

  return achados;
}

/**
 * Fora de escala dentro do próprio subgrupo.
 *
 * Só apanha o que tem subgrupo, que hoje é o que vem da CIQUAL: é a única das quatro fontes
 * que publica uma árvore de grupos. Dizer isto é preferível a inventar grupos por nome — um
 * agrupamento adivinhado põe o leite de coco ao pé do leite e passa a acusar os dois.
 *
 * A medida é a distância à mediana, em unidades da distância mediana à mediana. Não é o
 * desvio-padrão porque a energia dentro de um subgrupo não se distribui em sino: um óleo no
 * meio dos molhos leva a média para onde ela não descreve nada, e a partir daí nada é fora
 * de escala.
 */
export function foraDeEscala(alimentos) {
  const porGrupo = new Map();
  for (const a of alimentos) {
    if (a.grupo == null || a.kcal == null) continue;
    if (!porGrupo.has(a.grupo)) porGrupo.set(a.grupo, []);
    porGrupo.get(a.grupo).push(a);
  }

  const achados = [];
  for (const [, membros] of porGrupo) {
    if (membros.length < LIMITES.escalaMinimoNoGrupo) continue;
    const energias = membros.map((m) => m.kcal);
    const centro = mediana(energias);
    const dispersao = mediana(energias.map((e) => Math.abs(e - centro)));
    if (dispersao === 0) continue;

    for (const m of membros) {
      const vezes = Math.abs(m.kcal - centro) / dispersao;
      if (vezes <= LIMITES.escalaVezes) continue;
      achados.push(achado(
        m,
        "escala",
        "suspeita",
        `${m.kcal} kcal contra ${arredondar(centro)} kcal tipicas do subgrupo ${m.grupo}`,
        "kcal",
      ));
    }
  }
  return achados;
}

function mediana(numeros) {
  const ordenados = [...numeros].sort((x, y) => x - y);
  const meio = Math.floor(ordenados.length / 2);
  return ordenados.length % 2 ? ordenados[meio] : (ordenados[meio - 1] + ordenados[meio]) / 2;
}

/**
 * Duas tabelas publicadas a discordarem sobre o mesmo alimento.
 *
 * É a única verificação que não olha para dentro do alimento, e é a que apanha o que nenhuma
 * das outras apanha: um número **coerente consigo mesmo e errado à mesma**. A sangria a 89
 * contra 120 e o iogurte magro a 63 contra 42 fecham todas as contas internas — só não
 * fecham com a outra fonte.
 *
 * Não decide qual das duas tem razão. Não há como decidir isso daqui, e fingir que há era
 * escolher uma fonte à sorte e escrever o resultado como se fosse medido.
 */
export function discordancia({ alimento, outraFonte, outraEnergia }) {
  const kcal = numero(alimento.kcal);
  if (kcal == null || numero(outraEnergia) == null) return null;
  return achado(
    alimento,
    "discordancia",
    "suspeita",
    `${kcal} kcal aqui e ${outraEnergia} kcal na ${outraFonte}, para o mesmo nome`,
    "kcal",
  );
}

/**
 * Corre tudo e devolve os achados por ordem fixa.
 *
 * A ordem é a do identificador e depois a do tipo, e não a da gravidade: o ficheiro é lido em
 * `git diff`, e uma lista que se reordena sozinha quando um alimento muda de gravidade não se
 * consegue rever.
 */
export function verificar(alimentos, paresDiscordantes = []) {
  const achados = [];
  for (const a of alimentos) {
    const umAUm = [atwater(a), massa(a), ...partes(a)];
    for (const r of umAUm) if (r) achados.push(r);
  }
  achados.push(...foraDeEscala(alimentos));

  // Só os que sobreviveram à poda: um alimento que já não vai para o catálogo não precisa
  // de arbitragem nenhuma.
  const vivos = new Set(alimentos.map((a) => a.id));
  for (const par of paresDiscordantes) {
    if (!vivos.has(par.alimento.id)) continue;
    const r = discordancia(par);
    if (r) achados.push(r);
  }

  achados.sort((x, y) =>
    x.id < y.id ? -1 : x.id > y.id ? 1 : x.tipo < y.tipo ? -1 : x.tipo > y.tipo ? 1 : 0);
  return achados;
}
