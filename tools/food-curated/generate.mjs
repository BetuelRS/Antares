
import { writeFileSync } from "node:fs";

const F = [

  ["Banana", "Banana", 89, 1.1, 23, 0.3, 12, 2.6, 1, "unidade", 120],
  ["Maçã", "Apple", 52, 0.3, 14, 0.2, 10, 2.4, 1, "unidade", 150],
  ["Laranja", "Orange", 47, 0.9, 12, 0.1, 9, 2.4, 0, "unidade", 130],
  ["Pera", "Pear", 57, 0.4, 15, 0.1, 10, 3.1, 1, "unidade", 150],
  ["Uvas", "Grapes", 69, 0.7, 18, 0.2, 16, 0.9, 2, "cacho pequeno", 100],
  ["Morangos", "Strawberries", 32, 0.7, 8, 0.3, 5, 2, 1, "chávena", 150],
  ["Melancia", "Watermelon", 30, 0.6, 8, 0.2, 6, 0.4, 1, "fatia", 200],
  ["Melão", "Melon", 34, 0.8, 8, 0.2, 8, 0.9, 16, "fatia", 160],
  ["Manga", "Mango", 60, 0.8, 15, 0.4, 14, 1.6, 1, "unidade", 200],
  ["Ananás (abacaxi)", "Pineapple", 50, 0.5, 13, 0.1, 10, 1.4, 1, "fatia", 120],
  ["Kiwi", "Kiwi", 61, 1.1, 15, 0.5, 9, 3, 3, "unidade", 75],
  ["Pêssego", "Peach", 39, 0.9, 10, 0.3, 8, 1.5, 0, "unidade", 150],
  ["Ameixa", "Plum", 46, 0.7, 11, 0.3, 10, 1.4, 0, "unidade", 70],
  ["Cerejas", "Cherries", 63, 1, 16, 0.2, 13, 2.1, 0, "chávena", 140],
  ["Framboesas", "Raspberries", 52, 1.2, 12, 0.7, 4, 6.5, 1, "chávena", 120],
  ["Mirtilos", "Blueberries", 57, 0.7, 14, 0.3, 10, 2.4, 1, "chávena", 140],
  ["Abacate", "Avocado", 160, 2, 9, 15, 0.7, 7, 7, "meia unidade", 100],
  ["Tangerina", "Tangerine", 53, 0.8, 13, 0.3, 11, 1.8, 2, "unidade", 90],
  ["Figo", "Fig", 74, 0.8, 19, 0.3, 16, 2.9, 1, "unidade", 50],
  ["Romã", "Pomegranate", 83, 1.7, 19, 1.2, 14, 4, 3, "unidade", 200],
  ["Papaia (mamão)", "Papaya", 43, 0.5, 11, 0.3, 8, 1.7, 8, "fatia", 150],
  ["Maracujá", "Passion fruit", 97, 2.2, 23, 0.7, 11, 10, 28, "unidade", 40],
  ["Limão", "Lemon", 29, 1.1, 9, 0.3, 2.5, 2.8, 2, "unidade", 60],
  ["Toranja", "Grapefruit", 42, 0.8, 11, 0.1, 7, 1.6, 0, "meia unidade", 150],
  ["Tâmaras", "Dates", 282, 2.5, 75, 0.4, 63, 8, 2, "unidade", 24],
  ["Passas de uva", "Raisins", 299, 3.1, 79, 0.5, 59, 3.7, 11, "mão-cheia", 40],
  ["Coco (polpa)", "Coconut", 354, 3.3, 15, 33, 6, 9, 20, "pedaço", 45],
  ["Alperce (damasco)", "Apricot", 48, 1.4, 11, 0.4, 9, 2, 1, "unidade", 35],

  ["Tomate", "Tomato", 18, 0.9, 3.9, 0.2, 2.6, 1.2, 5, "unidade", 120],
  ["Cenoura", "Carrot", 41, 0.9, 10, 0.2, 4.7, 2.8, 69, "unidade", 80],
  ["Alface", "Lettuce", 15, 1.4, 2.9, 0.2, 0.8, 1.3, 28, "porção", 50],
  ["Brócolos", "Broccoli", 34, 2.8, 7, 0.4, 1.7, 2.6, 33, "porção", 90],
  ["Couve-flor", "Cauliflower", 25, 1.9, 5, 0.3, 1.9, 2, 30, "porção", 90],
  ["Espinafres", "Spinach", 23, 2.9, 3.6, 0.4, 0.4, 2.2, 79, "porção", 90],
  ["Courgette (abobrinha)", "Zucchini", 17, 1.2, 3.1, 0.3, 2.5, 1, 8, "unidade", 150],
  ["Beringela", "Eggplant", 25, 1, 6, 0.2, 3.5, 3, 2, "porção", 100],
  ["Pepino", "Cucumber", 15, 0.7, 3.6, 0.1, 1.7, 0.5, 2, "porção", 100],
  ["Pimento vermelho", "Red bell pepper", 31, 1, 6, 0.3, 4.2, 2.1, 4, "unidade", 120],
  ["Cebola", "Onion", 40, 1.1, 9, 0.1, 4.2, 1.7, 4, "unidade", 110],
  ["Alho", "Garlic", 149, 6.4, 33, 0.5, 1, 2.1, 17, "dente", 5],
  ["Batata-doce cozida", "Sweet potato, cooked", 86, 1.6, 20, 0.1, 4.2, 3, 27, "unidade", 130],
  ["Abóbora", "Pumpkin", 26, 1, 6.5, 0.1, 2.8, 0.5, 1, "porção", 120],
  ["Feijão-verde", "Green beans", 31, 1.8, 7, 0.2, 3.3, 3.4, 6, "porção", 100],
  ["Ervilhas", "Green peas", 81, 5, 14, 0.4, 6, 5, 5, "porção", 100],
  ["Milho cozido", "Corn, cooked", 96, 3.4, 21, 1.5, 4.5, 2.4, 1, "espiga", 100],
  ["Cogumelos", "Mushrooms", 22, 3.1, 3.3, 0.3, 2, 1, 5, "porção", 80],
  ["Couve", "Kale", 49, 4.3, 9, 0.9, 2.3, 3.6, 38, "porção", 70],
  ["Repolho", "Cabbage", 25, 1.3, 6, 0.1, 3.2, 2.5, 18, "porção", 90],
  ["Beterraba", "Beetroot", 43, 1.6, 10, 0.2, 7, 2.8, 78, "porção", 80],
  ["Rúcula", "Arugula", 25, 2.6, 3.7, 0.7, 2, 1.6, 27, "porção", 30],
  ["Espargos", "Asparagus", 20, 2.2, 3.9, 0.1, 1.9, 2.1, 2, "porção", 90],
  ["Alho-francês", "Leek", 61, 1.5, 14, 0.3, 3.9, 1.8, 20, "unidade", 90],
  ["Nabo", "Turnip", 28, 0.9, 6.4, 0.1, 3.8, 1.8, 67, "unidade", 120],
  ["Pimento verde", "Green bell pepper", 20, 0.9, 4.6, 0.2, 2.4, 1.7, 3, "unidade", 120],

  ["Amendoins", "Peanuts", 567, 26, 16, 49, 4, 8.5, 18, "mão-cheia", 30],
  ["Amêndoas", "Almonds", 579, 21, 22, 50, 4, 12.5, 1, "mão-cheia", 30],
  ["Nozes", "Walnuts", 654, 15, 14, 65, 2.6, 6.7, 2, "mão-cheia", 30],
  ["Caju", "Cashews", 553, 18, 30, 44, 6, 3.3, 12, "mão-cheia", 30],
  ["Avelãs", "Hazelnuts", 628, 15, 17, 61, 4, 10, 0, "mão-cheia", 30],
  ["Pistácios", "Pistachios", 560, 20, 28, 45, 8, 10, 1, "mão-cheia", 30],
  ["Sementes de girassol", "Sunflower seeds", 584, 21, 20, 51, 2.6, 9, 9, "colher de sopa", 15],
  ["Sementes de abóbora", "Pumpkin seeds", 559, 30, 11, 49, 1.4, 6, 7, "colher de sopa", 15],
  ["Sementes de chia", "Chia seeds", 486, 17, 42, 31, 0, 34, 16, "colher de sopa", 12],
  ["Castanhas assadas", "Roasted chestnuts", 245, 3.2, 53, 2.2, 11, 5.1, 2, "mão-cheia", 50],
  ["Manteiga de amendoim", "Peanut butter", 588, 25, 20, 50, 9, 6, 17, "colher de sopa", 20],

  ["Peito de peru grelhado", "Turkey breast, grilled", 135, 30, 0, 1, 0, 0, 60, "posta", 120],
  ["Carne picada de vaca cozinhada", "Ground beef, cooked", 250, 26, 0, 17, 0, 0, 70, "porção", 120],
  ["Costeleta de porco grelhada", "Pork chop, grilled", 231, 26, 0, 13, 0, 0, 62, "unidade", 130],
  ["Bacon frito", "Bacon, fried", 541, 37, 1.4, 42, 0, 0, 1717, "fatia", 15],
  ["Presunto", "Cured ham", 241, 26, 0.5, 14, 0, 0, 1200, "fatia", 20],
  ["Borrego grelhado", "Lamb, grilled", 258, 25, 0, 17, 0, 0, 72, "posta", 120],
  ["Coelho estufado", "Rabbit, stewed", 173, 33, 0, 3.5, 0, 0, 45, "porção", 150],
  ["Almôndegas", "Meatballs", 197, 14, 5, 13, 1, 0.5, 380, "porção (4)", 120],
  ["Hambúrguer de vaca grelhado", "Beef burger patty, grilled", 254, 25, 0.6, 17, 0, 0, 80, "unidade", 100],
  ["Frango assado (com pele)", "Roast chicken, with skin", 197, 27, 0, 9.5, 0, 0, 90, "porção", 150],

  ["Pescada cozida", "Hake, boiled", 90, 18, 0, 1.8, 0, 0, 100, "posta", 150],
  ["Dourada grelhada", "Sea bream, grilled", 96, 20, 0, 2, 0, 0, 90, "unidade", 200],
  ["Robalo grelhado", "Sea bass, grilled", 97, 18, 0, 2.5, 0, 0, 80, "unidade", 200],
  ["Cavala", "Mackerel", 205, 19, 0, 14, 0, 0, 90, "unidade", 150],
  ["Carapau grelhado", "Horse mackerel, grilled", 118, 20, 0, 4, 0, 0, 90, "unidade", 120],
  ["Polvo cozido", "Octopus, boiled", 82, 15, 2.2, 1, 0, 0, 230, "porção", 150],
  ["Camarão cozido", "Shrimp, boiled", 99, 24, 0.2, 0.3, 0, 0, 111, "porção", 100],
  ["Lulas grelhadas", "Squid, grilled", 92, 15, 3, 1.4, 0, 0, 44, "porção", 120],
  ["Mexilhões", "Mussels", 86, 12, 4, 2.2, 0, 0, 286, "porção", 100],
  ["Truta grelhada", "Trout, grilled", 148, 21, 0, 7, 0, 0, 52, "unidade", 150],
  ["Salmão fumado", "Smoked salmon", 117, 18, 0, 4.3, 0, 0, 1880, "fatia", 30],
  ["Gambas", "Prawns", 99, 24, 0.2, 0.3, 0, 0, 111, "porção", 100],

  ["Ovo mexido", "Scrambled egg", 148, 10, 1.6, 11, 1.3, 0, 140, "porção (2 ovos)", 120],
  ["Clara de ovo", "Egg white", 52, 11, 0.7, 0.2, 0.7, 0, 166, "unidade", 33],
  ["Iogurte grego natural", "Greek yogurt, plain", 97, 9, 4, 5, 4, 0, 35, "unidade", 170],
  ["Iogurte de aromas", "Flavoured yogurt", 95, 4, 15, 2, 14, 0, 50, "unidade", 125],
  ["Queijo mozzarella", "Mozzarella cheese", 280, 28, 3, 17, 1, 0, 630, "fatia", 30],
  ["Queijo parmesão", "Parmesan cheese", 392, 36, 4, 26, 0.9, 0, 1600, "colher de sopa", 10],
  ["Queijo cheddar", "Cheddar cheese", 402, 25, 1.3, 33, 0.5, 0, 621, "fatia", 30],
  ["Queijo cottage", "Cottage cheese", 98, 11, 3.4, 4.3, 2.7, 0, 364, "porção", 100],
  ["Natas (35%)", "Cream (35%)", 340, 2, 3, 35, 3, 0, 30, "colher de sopa", 15],
  ["Natas culinárias (18%)", "Cooking cream (18%)", 195, 2.5, 3.5, 18, 3.5, 0, 40, "colher de sopa", 15],
  ["Leite condensado", "Condensed milk", 321, 8, 54, 9, 54, 0, 127, "colher de sopa", 20],
  ["Kefir", "Kefir", 41, 3.3, 4.5, 1, 4.5, 0, 40, "copo", 200],

  ["Pão de centeio", "Rye bread", 259, 9, 48, 3.3, 3.9, 6, 603, "fatia", 40],
  ["Baguete", "Baguette", 270, 9, 55, 1.5, 2.5, 2.7, 590, "pedaço", 60],
  ["Flocos de milho (corn flakes)", "Corn flakes", 357, 7, 84, 0.9, 8, 3, 729, "porção", 30],
  ["Muesli", "Muesli", 363, 10, 66, 6, 26, 7, 30, "porção", 45],
  ["Granola", "Granola", 471, 10, 64, 20, 24, 7, 30, "porção", 45],
  ["Quinoa cozida", "Quinoa, cooked", 120, 4.4, 21, 1.9, 0.9, 2.8, 7, "porção", 150],
  ["Cuscuz cozido", "Couscous, cooked", 112, 3.8, 23, 0.2, 0.1, 1.4, 5, "porção", 150],
  ["Bulgur cozido", "Bulgur, cooked", 83, 3, 19, 0.2, 0.1, 4.5, 5, "porção", 150],
  ["Farinha de trigo", "Wheat flour", 364, 10, 76, 1, 0.3, 2.7, 2, "colher de sopa", 15],
  ["Wrap / tortilha", "Tortilla wrap", 310, 8, 50, 7, 2.5, 3, 620, "unidade", 50],
  ["Esparguete cozido", "Spaghetti, cooked", 158, 5.8, 31, 0.9, 0.6, 1.8, 1, "porção", 200],

  ["Tofu", "Tofu", 76, 8, 1.9, 4.8, 0.6, 0.3, 7, "porção", 100],
  ["Edamame", "Edamame", 121, 12, 9, 5, 2.2, 5, 6, "porção", 100],
  ["Húmus", "Hummus", 166, 8, 14, 10, 0.3, 6, 379, "colher de sopa", 30],
  ["Soja cozida", "Soybeans, cooked", 173, 17, 10, 9, 3, 6, 1, "porção", 120],
  ["Seitan", "Seitan", 141, 25, 14, 2, 0, 1, 320, "porção", 100],

  ["Óleo vegetal", "Vegetable oil", 884, 0, 0, 100, 0, 0, 0, "colher de sopa", 10],
  ["Margarina", "Margarine", 717, 0.2, 0.7, 80, 0, 0, 751, "porção", 10],
  ["Maionese", "Mayonnaise", 680, 1, 1, 75, 1, 0, 635, "colher de sopa", 15],
  ["Ketchup", "Ketchup", 112, 1.2, 26, 0.1, 22, 0.3, 907, "colher de sopa", 15],
  ["Mostarda", "Mustard", 66, 4, 6, 3, 1, 3, 1100, "colher de chá", 5],
  ["Molho de soja", "Soy sauce", 53, 8, 5, 0.6, 0.4, 0.8, 5493, "colher de sopa", 15],
  ["Creme de avelã e cacau", "Hazelnut cocoa spread", 539, 6, 57, 31, 56, 3.4, 41, "colher de sopa", 20],
  ["Compota de fruta", "Fruit jam", 250, 0.4, 60, 0.1, 60, 1, 30, "colher de sopa", 20],
  ["Pesto", "Pesto", 450, 4, 6, 46, 2, 2, 800, "colher de sopa", 15],

  ["Água", "Water", 0, 0, 0, 0, 0, 0, 2, "copo", 250],
  ["Sumo de maçã", "Apple juice", 46, 0.1, 11, 0.1, 10, 0.2, 4, "copo", 200],
  ["Vinho branco", "White wine", 82, 0.1, 2.6, 0, 1, 0, 5, "copo", 150],
  ["Bebida de aveia", "Oat drink", 45, 0.3, 6.7, 1.5, 3.3, 0.8, 40, "copo", 200],
  ["Bebida de soja", "Soy drink", 42, 3.3, 2.5, 1.8, 2.5, 0.6, 40, "copo", 200],
  ["Bebida de amêndoa", "Almond drink", 22, 0.5, 3, 1.1, 2, 0.4, 60, "copo", 200],
  ["Refrigerante de laranja", "Orange soda", 45, 0, 11, 0, 11, 0, 8, "lata", 330],
  ["Água tónica", "Tonic water", 34, 0, 8.8, 0, 8.8, 0, 12, "copo", 200],
  ["Bebida energética", "Energy drink", 45, 0.4, 11, 0, 11, 0, 105, "lata", 250],
  ["Chá com açúcar", "Sweetened tea", 30, 0, 7.5, 0, 7.5, 0, 3, "chávena", 200],

  ["Chocolate negro (70%)", "Dark chocolate (70%)", 546, 8, 46, 38, 24, 11, 20, "quadrado", 10],
  ["Bolacha água e sal", "Cream cracker", 435, 9, 70, 14, 2, 2.5, 700, "unidade", 8],
  ["Bolachas com pepitas de chocolate", "Chocolate chip cookies", 480, 6, 64, 22, 35, 2, 350, "unidade", 15],
  ["Batatas fritas de pacote", "Potato chips", 536, 7, 53, 35, 0.6, 4.4, 525, "saco pequeno", 30],
  ["Pipocas", "Popcorn", 387, 12, 78, 4, 0.5, 15, 8, "taça", 30],
  ["Gelado de baunilha", "Vanilla ice cream", 207, 3.5, 24, 11, 21, 0.7, 80, "bola", 60],
  ["Bolo de chocolate", "Chocolate cake", 371, 5, 50, 16, 35, 1.8, 315, "fatia", 80],
  ["Croissant", "Croissant", 406, 8, 46, 21, 11, 2.6, 424, "unidade", 60],
  ["Donut", "Donut", 452, 5, 51, 25, 23, 1.6, 326, "unidade", 60],
  ["Barra de cereais", "Cereal bar", 400, 6, 70, 10, 30, 4, 200, "unidade", 25],
  ["Gomas", "Gummy candy", 343, 6.9, 77, 0.2, 46, 0, 40, "mão-cheia", 30],
  ["Muffin", "Muffin", 377, 6, 51, 16, 27, 1.5, 330, "unidade", 70],
  ["Waffle", "Waffle", 291, 8, 33, 14, 6, 1.5, 511, "unidade", 75],
  ["Bolo de arroz", "Rice cake (Portuguese)", 361, 6, 52, 14, 22, 1, 300, "unidade", 65],
  ["Tarte de maçã", "Apple pie", 265, 2.4, 37, 12, 18, 1.6, 210, "fatia", 100],
  ["Barra de chocolate com leite", "Milk chocolate bar", 535, 7.6, 59, 30, 56, 2, 79, "unidade", 45],

  ["Coxinha", "Coxinha (BR)", 250, 8, 28, 12, 1, 1.2, 480, "unidade", 80],
  ["Pastel frito (BR)", "Fried pastel (BR)", 300, 8, 30, 16, 2, 1.5, 450, "unidade", 90],
  ["Açaí (polpa)", "Açaí pulp", 110, 1, 15, 5, 2, 3, 10, "taça", 200],
  ["Misto quente", "Ham & cheese toastie", 280, 14, 28, 12, 3, 1.5, 700, "unidade", 120],
  ["Bitoque (com ovo)", "Steak with egg & fries", 210, 14, 12, 12, 1, 1.5, 320, "prato", 300],
  ["Bacalhau com natas", "Salt cod with cream", 178, 10, 12, 10, 2, 1, 450, "porção", 250],
  ["Jardineira", "Meat & vegetable stew", 110, 7, 10, 4.5, 2, 2, 350, "porção", 250],
  ["Arroz de marisco", "Seafood rice", 130, 7, 18, 3, 0.5, 1, 400, "porção", 250],
  ["Empada de frango", "Chicken pie", 300, 8, 30, 16, 2, 1.2, 450, "unidade", 90],
  ["Beijinho (BR)", "Coconut fudge (BR)", 330, 4, 55, 11, 52, 1.5, 60, "unidade", 20],
];

const slug = (s) =>
  "ptx2_" +
  s.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "_").replace(/^_|_$/g, "");

const out = F.map(([namePt, nameEn, kcal, p, c, f, sugars, fiber, sodium, sv, sg]) => {
  const o = {
    id: slug(namePt),
    source: "SEED",
    nameEn,
    namePt,
    kcal,
    proteinG: p,
    carbsG: c,
    fatG: f,
    verified: false,
  };
  if (sugars != null) o.sugarsG = sugars;
  if (fiber != null) o.fiberG = fiber;
  if (sodium != null) o.sodiumMg = sodium;
  if (sv != null) o.servingName = sv;
  if (sg != null) o.servingGrams = sg;
  return o;
});

const ids = new Set();
for (const o of out) {
  if (ids.has(o.id)) throw new Error("id duplicado: " + o.id);
  ids.add(o.id);
}

const target = "../../composeApp/src/commonMain/composeResources/files/seed_foods_pt2.json";
writeFileSync(new URL(target, import.meta.url), JSON.stringify(out, null, 0));
console.log("Gerados", out.length, "alimentos curados →", target);
