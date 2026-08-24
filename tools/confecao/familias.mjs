/**
 * A que família de confeção pertence cada alimento.
 *
 * As tabelas do USDA publicam retenção e rendimento **por grupo de alimento**, e não por
 * alimento: o que sobrevive a ferver é uma propriedade do que se está a ferver, não daquele
 * pedaço concreto. Para lhes chegar é preciso saber a que grupo pertence cada um dos oito mil.
 *
 * **Nenhuma família é adivinhada por semelhança de nome, com uma excepção declarada.** A
 * CIQUAL publica uma árvore de subgrupos e a USDA publica a categoria de cada alimento, e é
 * de lá que vem quase tudo. A excepção são as carnes da CIQUAL, que caem todas em dois
 * subgrupos — «cozinhadas» e «cruas» — sem dizerem de que animal são; essas separam-se pelo
 * nome, e é a única leitura de nome que aqui se faz.
 *
 * **Uma família nula quer dizer «não se cozinha isto».** Um pão já foi ao forno, um gelado não
 * vai, um refrigerante também não. Nulo não é «não sabemos»: é a resposta certa, e a app não
 * oferece confeção nenhuma a esses.
 */

/** As famílias, e o grupo do USDA de onde lhes vêm os números. */
export const FAMILIAS = {
  lacticinios_ovos: 1,
  aves: 5,
  fruta: 9,
  porco: 10,
  legumes: 11,
  frutos_secos: 12,
  vaca: 13,
  bebidas_alcoolicas: 14,
  peixe: 15,
  leguminosas: 16,
  borrego: 17,
  cereais: 20,
};

/** O subgrupo da CIQUAL e a família. O que não está aqui não se cozinha. */
const POR_SUBGRUPO_CIQUAL = {
  "0201": "legumes",
  "0202": "legumes",
  "0203": "leguminosas",
  "0204": "fruta",
  "0205": "frutos_secos",
  "0301": "cereais",
  "0304": "cereais",
  "0405": "peixe",
  "0406": "peixe",
  "0407": "peixe",
  "0408": "peixe",
  "0410": "lacticinios_ovos",
  "0501": "lacticinios_ovos",
  "0502": "lacticinios_ovos",
  "0503": "lacticinios_ovos",
  "0504": "lacticinios_ovos",
  "0603": "bebidas_alcoolicas",
};

/** Os dois subgrupos de carne da CIQUAL — cozinhadas e cruas —, que não dizem de que animal. */
const CARNES_CIQUAL = new Set(["0401", "0402"]);

/**
 * De que animal é a carne, pelo nome inglês da CIQUAL.
 *
 * A ordem importa: «veal» tem de ser visto antes de «veau»/«beef» porque a vitela vive no
 * grupo do borrego e da caça no USDA, não no do bovino.
 */
const ANIMAIS = [
  [/\b(veal|lamb|mutton|goat|venison|rabbit|hare|game)\b/i, "borrego"],
  [/\b(pork|ham|bacon|pig|piglet|boar)\b/i, "porco"],
  [/\b(chicken|turkey|duck|goose|poultry|guinea fowl|capon|quail|pigeon)\b/i, "aves"],
  [/\b(beef|veal|ox|steer|cow|bovine)\b/i, "vaca"],
];

/** A categoria do USDA e a família. As que faltam são as que não se cozinham. */
const POR_CATEGORIA_USDA = Object.fromEntries(
  Object.entries(FAMILIAS).map(([nome, grupo]) => [String(grupo), nome]),
);

export function familiaDeCiqual(subgrupo, nomeEn) {
  if (CARNES_CIQUAL.has(subgrupo)) {
    for (const [padrao, familia] of ANIMAIS) if (padrao.test(nomeEn ?? "")) return familia;

    // Carne que não diz de que animal é. Fica sem família em vez de ir para a mais comum:
    // um rendimento de bovino aplicado a um pato está errado e não parece.
    return null;
  }
  return POR_SUBGRUPO_CIQUAL[subgrupo] ?? null;
}

export function familiaDeUsda(categoria) {
  return POR_CATEGORIA_USDA[String(categoria)] ?? null;
}

/**
 * O nível 1 da classificação FoodEx2, que a TCA do INSA publica em coluna própria.
 *
 * Os «pratos compostos» — 238 dos 1 378 — ficam de fora com os doces e os molhos: já são
 * comida feita, e aplicar-lhes um rendimento de cozedura era cozinhar uma refeição outra vez.
 */
const POR_NIVEL1_TCA = {
  "Peixes, mariscos, anfíbios, répteis e invertebrados": "peixe",
  "Anfíbios, répteis, e invertebrados terrestres": "peixe",
  "Leite e produtos lácteos": "lacticinios_ovos",
  "Ovos e ovoprodutos": "lacticinios_ovos",
  "Cereais e produtos à base de cereais": "cereais",
  "Produtos hortícolas e derivados": "legumes",
  "Raízes amiláceas ou tubérculos e seus produtos, plantas sacarinas": "legumes",
  "Frutos e produtos derivados de frutos": "fruta",
  "Leguminosas, frutos de casca rija, sementes oleaginosas e especiarias": "leguminosas",
  "Leguminosas, nozes, oleaginosas e especiarias.": "leguminosas",
  "Bebidas alcoólicas": "bebidas_alcoolicas",
};

/** Os animais, em português, para as 245 linhas de «Carne e produtos cárneos». */
const ANIMAIS_PT = [
  // O cavalo entra aqui porque é onde o USDA o põe: o grupo 17 é «borrego, vitela e caça».
  [/\b(borrego|cordeiro|ovino|carneiro|cabrito|caprino|coelho|lebre|ca[çc]a|vitela|cavalo)\b/i, "borrego"],
  [/\b(porco|su[íi]no|presunto|bacon|chouri[çc]o|fiambre|toucinho|entrecosto|leit[ãa]o|paio|salpic[ãa]o|farinheira|alheira|morcela)\b/i, "porco"],
  [/\b(frango|galinha|peru|pato|ganso|codorniz|perdiz|ave|aves|capão)\b/i, "aves"],
  [/\b(vaca|bovino|novilho|boi|vitel[ãa]o)\b/i, "vaca"],
];

export function familiaDeTca(nivel1, nome) {
  if (String(nivel1 ?? "").startsWith("Carne")) {
    for (const [padrao, familia] of ANIMAIS_PT) if (padrao.test(nome ?? "")) return familia;
    return null;
  }
  return POR_NIVEL1_TCA[String(nivel1 ?? "").trim()] ?? null;
}
