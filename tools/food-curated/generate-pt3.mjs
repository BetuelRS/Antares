
import { writeFileSync } from "node:fs";

const F = [

  ["Bacalhau à Gomes de Sá", "Cod Gomes de Sá", 168, 11, 12, 8.5, 1, 1.2, 480, "porção", 300],
  ["Sardinha assada", "Grilled sardines", 208, 25, 0, 12, 0, 0, 90, "porção (3)", 150],
  ["Febras grelhadas", "Grilled pork loin steaks", 187, 29, 0, 8, 0, 0, 70, "porção", 150],
  ["Entremeada grelhada", "Grilled pork belly", 320, 22, 0, 26, 0, 0, 80, "porção", 150],
  ["Alheira", "Alheira sausage", 290, 14, 18, 18, 0.5, 1, 780, "unidade", 120],
  ["Farinheira", "Farinheira sausage", 400, 9, 20, 32, 0.5, 1, 900, "unidade", 100],
  ["Morcela", "Blood sausage", 379, 15, 3, 34, 0.5, 0.5, 680, "porção", 80],
  ["Alcatra assada", "Roast beef (alcatra)", 217, 30, 0, 11, 0, 0, 60, "posta", 150],
  ["Frango de churrasco", "Piri-piri grilled chicken", 215, 27, 0, 12, 0, 0, 260, "porção", 200],
  ["Arroz de frango", "Chicken rice", 145, 8, 20, 3.5, 0.5, 0.8, 380, "porção", 300],
  ["Caldeirada de peixe", "Fish stew", 95, 9, 6, 3.5, 1.5, 1, 400, "porção", 300],
  ["Açorda de marisco", "Seafood bread stew", 130, 7, 15, 4.5, 1, 1, 450, "porção", 300],
  ["Migas", "Migas (bread & garlic)", 210, 5, 25, 10, 1, 2, 500, "porção", 200],
  ["Dobrada com feijão", "Tripe with beans", 130, 10, 8, 6.5, 1, 2, 480, "porção", 300],
  ["Sopa da pedra", "Stone soup (bean & meat)", 85, 5, 10, 2.8, 1.5, 2.5, 400, "tigela", 300],
  ["Canja de galinha", "Chicken & rice soup", 45, 3.5, 5, 1.2, 0.3, 0.3, 350, "tigela", 300],
  ["Ovos mexidos com alheira", "Scrambled eggs with alheira", 250, 14, 8, 18, 0.5, 0.8, 520, "porção", 200],
  ["Prego no pão", "Steak sandwich (prego)", 250, 15, 26, 9, 2, 1.5, 520, "unidade", 180],
  ["Pica-pau", "Pork bites in sauce (pica-pau)", 190, 18, 3, 12, 1, 0.5, 480, "porção", 200],

  ["Feijoada brasileira", "Brazilian feijoada", 200, 12, 14, 11, 0.5, 5, 500, "porção", 300],
  ["Estrogonofe de frango", "Chicken stroganoff", 170, 12, 8, 10, 2, 0.5, 420, "porção", 250],
  ["Escondidinho", "Escondidinho (meat & cassava)", 150, 8, 15, 6.5, 1, 1.5, 400, "porção", 300],
  ["Moqueca de peixe", "Fish moqueca", 120, 11, 4, 7, 1.5, 1, 420, "porção", 300],
  ["Virado à paulista", "Virado à paulista", 180, 9, 20, 8, 1, 3, 450, "porção", 300],
  ["Baião de dois", "Baião de dois", 165, 7, 24, 5, 0.5, 2.5, 420, "porção", 250],
  ["Arroz carreteiro", "Carreteiro rice", 175, 9, 22, 6, 0.5, 1, 430, "porção", 300],
  ["Pão de queijo (BR)", "Cheese bread ball", 300, 6, 38, 14, 1, 0.5, 450, "unidade", 40],
  ["Coxinha de frango", "Chicken coxinha", 250, 8, 28, 12, 1, 1.2, 480, "unidade", 80],
  ["Pastel de feira", "Fried pastel (BR)", 300, 8, 30, 16, 2, 1.5, 450, "unidade", 90],
  ["Feijão tropeiro", "Feijão tropeiro", 190, 10, 18, 9, 0.5, 4, 450, "porção", 200],
  ["Vinagrete (molho)", "Brazilian vinaigrette salsa", 35, 1, 5, 1.2, 3, 1, 200, "colher de sopa", 30],

  ["Pizza Margherita", "Margherita pizza", 260, 11, 33, 9, 3, 2, 560, "fatia", 100],
  ["Pizza de pepperoni", "Pepperoni pizza", 298, 12, 30, 14, 3, 2, 700, "fatia", 100],
  ["Cheeseburger", "Cheeseburger", 270, 15, 26, 12, 6, 1.5, 560, "unidade", 150],
  ["Hambúrguer completo", "Loaded hamburger", 295, 16, 24, 15, 6, 2, 550, "unidade", 200],
  ["Nuggets de frango", "Chicken nuggets", 296, 15, 18, 18, 0.5, 1, 550, "porção (6)", 100],
  ["Batata frita de restaurante", "Restaurant french fries", 312, 3.4, 41, 15, 0.3, 3.8, 210, "porção", 150],
  ["Cachorro-quente", "Hot dog", 247, 10, 25, 12, 4, 1, 700, "unidade", 120],
  ["Kebab (frango)", "Chicken kebab wrap", 215, 14, 22, 8, 3, 2, 600, "unidade", 250],
  ["Sanduíche de fiambre e queijo", "Ham & cheese sandwich", 250, 13, 28, 9, 3, 1.5, 720, "unidade", 130],
  ["Sushi (variado)", "Assorted sushi", 145, 5, 28, 1.5, 4, 1, 400, "porção (6)", 120],
  ["Caril de frango", "Chicken curry", 150, 12, 8, 8, 2, 1, 420, "porção", 250],
  ["Lasanha à bolonhesa", "Beef lasagna", 165, 9, 15, 7.5, 2, 1.2, 400, "porção", 300],
  ["Esparguete à bolonhesa", "Spaghetti bolognese", 145, 7, 18, 5, 2, 1.5, 380, "porção", 300],
  ["Empadão de carne", "Meat & potato pie", 140, 7, 14, 6.5, 1, 1.2, 380, "porção", 300],
  ["Rojões", "Fried pork chunks (rojões)", 260, 22, 2, 18, 0.5, 0.3, 420, "porção", 180],

  ["Papas de aveia (com leite)", "Oat porridge with milk", 95, 4, 14, 2.5, 6, 1.5, 45, "tigela", 250],
  ["Leite achocolatado", "Chocolate milk", 83, 3.2, 12, 2.5, 11, 0.5, 60, "copo", 200],
  ["Iogurte líquido", "Drinkable yogurt", 71, 2.8, 12, 1.5, 11, 0, 45, "garrafa", 160],
  ["Smoothie de fruta", "Fruit smoothie", 60, 1, 14, 0.3, 12, 1.2, 10, "copo", 250],
  ["Cereais com chocolate", "Chocolate cereal", 383, 6, 78, 5, 30, 5, 300, "porção", 30],
  ["Pão com manteiga", "Bread with butter", 290, 8, 45, 9, 2.5, 2, 480, "fatia", 50],
  ["Torrada com doce", "Toast with jam", 265, 6, 52, 3.5, 20, 2, 350, "fatia", 50],
  ["Croissant misto", "Ham & cheese croissant", 360, 12, 33, 20, 4, 1.5, 620, "unidade", 90],
  ["Folhado de salsicha", "Sausage roll", 330, 9, 28, 20, 2, 1.2, 600, "unidade", 90],
  ["Bolo de laranja", "Orange cake", 360, 5, 52, 15, 30, 1, 300, "fatia", 70],
  ["Bolo de cenoura", "Carrot cake", 390, 5, 50, 19, 32, 1.5, 320, "fatia", 80],
  ["Pão de ló", "Sponge cake (pão de ló)", 300, 7, 55, 6, 35, 0.8, 200, "fatia", 60],
  ["Queque", "Cupcake / queque", 380, 6, 50, 17, 26, 1, 330, "unidade", 60],
  ["Bolacha recheada", "Sandwich biscuit", 480, 5, 68, 21, 38, 2, 350, "unidade", 12],
  ["Wafer de chocolate", "Chocolate wafer", 510, 6, 62, 27, 40, 1.5, 120, "unidade", 20],
  ["Chocolate branco", "White chocolate", 539, 6, 59, 30, 59, 0.2, 90, "quadrado", 10],
  ["Panqueca americana", "American pancake", 227, 6, 28, 9, 6, 1, 430, "unidade", 60],

  ["Mousse de chocolate", "Chocolate mousse", 220, 4, 25, 12, 22, 1, 60, "taça", 100],
  ["Leite-creme", "Portuguese custard (leite-creme)", 150, 3.5, 22, 5, 20, 0, 50, "taça", 120],
  ["Pudim flan", "Crème caramel (pudim)", 145, 3.5, 24, 4, 23, 0, 60, "fatia", 100],
  ["Gelatina", "Jelly", 62, 1.2, 14, 0, 14, 0, 40, "taça", 120],
  ["Aletria", "Sweet vermicelli (aletria)", 160, 4, 28, 3.5, 18, 0.5, 45, "porção", 120],
  ["Salame de chocolate", "Chocolate salami", 450, 6, 50, 25, 40, 2, 80, "fatia", 40],
  ["Doce de ovos", "Sweet egg cream (doce de ovos)", 290, 6, 45, 9, 44, 0, 40, "colher de sopa", 25],
  ["Tiramisu", "Tiramisu", 240, 4.5, 25, 13, 20, 0.5, 55, "porção", 100],
  ["Cheesecake", "Cheesecake", 321, 6, 26, 22, 22, 0.5, 320, "fatia", 100],
  ["Brownie", "Brownie", 466, 6, 50, 28, 38, 2.5, 280, "unidade", 60],

  ["Skyr", "Skyr", 63, 11, 4, 0.2, 4, 0, 45, "unidade", 150],
  ["Ricotta", "Ricotta cheese", 174, 11, 3, 13, 0.3, 0, 84, "porção", 50],
  ["Mascarpone", "Mascarpone", 429, 4.6, 4.8, 44, 3, 0, 30, "colher de sopa", 20],
  ["Queijo da Serra", "Serra da Estrela cheese", 364, 20, 1, 31, 0.5, 0, 700, "fatia", 30],
  ["Queijo de cabra", "Goat cheese", 364, 22, 2.5, 30, 2.5, 0, 515, "fatia", 30],
  ["Queijo azul", "Blue cheese", 353, 21, 2.3, 29, 0.5, 0, 1395, "fatia", 30],
  ["Queijo ralado", "Grated cheese", 402, 28, 3, 30, 0.5, 0, 700, "colher de sopa", 10],
  ["Leite de cabra", "Goat milk", 69, 3.6, 4.5, 4.1, 4.5, 0, 50, "copo", 200],
  ["Manteiga de amendoim light", "Light peanut butter", 520, 24, 26, 36, 8, 6, 350, "colher de sopa", 20],
  ["Iogurte proteico", "High-protein yogurt", 60, 10, 4, 0.2, 4, 0, 50, "unidade", 150],

  ["Dióspiro", "Persimmon", 70, 0.6, 18, 0.2, 13, 3.6, 1, "unidade", 150],
  ["Nêspera", "Loquat", 47, 0.4, 12, 0.2, 9, 1.7, 1, "unidade", 30],
  ["Meloa", "Cantaloupe", 34, 0.8, 8, 0.2, 8, 0.9, 16, "fatia", 160],
  ["Lima", "Lime", 30, 0.7, 11, 0.2, 1.7, 2.8, 2, "unidade", 60],
  ["Amoras", "Blackberries", 43, 1.4, 10, 0.5, 5, 5.3, 1, "chávena", 140],
  ["Groselhas", "Currants", 56, 1.4, 14, 0.2, 8, 4.3, 1, "chávena", 110],
  ["Banana-da-terra frita", "Fried plantain", 152, 1.3, 40, 0.4, 18, 2.3, 4, "porção", 100],
  ["Ameixa seca", "Prune", 240, 2.2, 64, 0.4, 38, 7.1, 2, "unidade", 10],
  ["Figo seco", "Dried fig", 249, 3.3, 64, 0.9, 48, 9.8, 10, "unidade", 20],
  ["Alperce seco", "Dried apricot", 241, 3.4, 63, 0.5, 53, 7.3, 10, "unidade", 8],

  ["Grão-de-bico assado", "Roasted chickpeas", 164, 9, 27, 2.6, 5, 8, 240, "porção", 60],
  ["Azeitonas", "Olives", 145, 1, 6, 15, 0, 3.3, 1556, "porção", 30],
  ["Tremoços", "Lupin beans", 119, 16, 10, 2.9, 0, 3, 400, "porção", 60],
  ["Favas", "Broad beans", 88, 8, 12, 0.7, 2, 5, 5, "porção", 100],
  ["Mandioca cozida", "Boiled cassava", 160, 1.4, 38, 0.3, 1.7, 1.8, 14, "porção", 100],
  ["Batata palha", "Shoestring potato", 560, 5, 55, 35, 0.5, 4, 400, "porção", 30],
  ["Acelga", "Chard", 19, 1.8, 3.7, 0.2, 1.1, 1.6, 213, "porção", 90],
  ["Agrião", "Watercress", 11, 2.3, 1.3, 0.1, 0.2, 0.5, 41, "porção", 30],
  ["Chuchu", "Chayote", 19, 0.8, 4.5, 0.1, 1.7, 1.7, 2, "unidade", 150],
  ["Palmito", "Heart of palm", 28, 2.5, 4.6, 0.6, 0, 2.4, 426, "porção", 60],

  ["Cappuccino", "Cappuccino", 40, 2, 4, 1.8, 4, 0, 30, "chávena", 180],
  ["Chocolate quente", "Hot chocolate", 90, 3.5, 12, 3, 11, 0.8, 60, "chávena", 200],
  ["Chá gelado", "Iced tea", 30, 0, 7.5, 0, 7.5, 0, 8, "copo", 250],
  ["Limonada", "Lemonade", 40, 0.1, 10, 0, 9, 0, 4, "copo", 250],
  ["Água de coco", "Coconut water", 19, 0.7, 3.7, 0.2, 2.6, 1.1, 105, "copo", 200],
  ["Batido de proteína (pronto)", "Ready protein shake", 60, 8, 5, 1, 3, 0.5, 80, "garrafa", 330],
  ["Sangria", "Sangria", 120, 0.3, 13, 0, 12, 0, 8, "copo", 200],
  ["Whisky", "Whisky", 250, 0, 0, 0, 0, 0, 1, "dose", 40],
  ["Gin", "Gin", 263, 0, 0, 0, 0, 0, 1, "dose", 40],
  ["Vodka", "Vodka", 231, 0, 0, 0, 0, 0, 1, "dose", 40],

  ["Molho bolonhesa", "Bolognese sauce", 90, 6, 6, 4.5, 4, 1.2, 400, "porção", 100],
  ["Molho de tomate", "Tomato sauce", 32, 1.4, 6, 0.3, 4, 1.5, 380, "colher de sopa", 30],
  ["Molho branco (béchamel)", "Béchamel sauce", 150, 4, 9, 11, 4, 0.2, 350, "porção", 60],
  ["Piri-piri (molho)", "Piri-piri hot sauce", 30, 1, 4, 1, 2, 1, 900, "colher de chá", 5],
  ["Guacamole", "Guacamole", 150, 2, 8, 13, 1, 6, 300, "colher de sopa", 30],
  ["Tahini", "Tahini", 595, 17, 21, 54, 0.5, 9, 35, "colher de sopa", 15],
  ["Molho tártaro", "Tartar sauce", 300, 1, 8, 30, 3, 0.2, 600, "colher de sopa", 15],
  ["Xarope de ácer", "Maple syrup", 260, 0, 67, 0.1, 60, 0, 12, "colher de sopa", 20],
  ["Natas para bater", "Whipping cream", 292, 2.3, 3, 30, 3, 0, 30, "colher de sopa", 15],
  ["Leite em pó", "Powdered milk", 496, 26, 38, 27, 38, 0, 371, "colher de sopa", 15],
];

const slug = (s) =>
  "ptx3_" +
  s.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "_").replace(/^_|_$/g, "");

const out = F.map(([namePt, nameEn, kcal, p, c, f, sugars, fiber, sodium, sv, sg]) => {
  const o = { id: slug(namePt), source: "SEED", nameEn, namePt, kcal, proteinG: p, carbsG: c, fatG: f, verified: false };
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

const target = "../catalogo/dados/seed_foods_pt3.json";
writeFileSync(new URL(target, import.meta.url), JSON.stringify(out, null, 0));
console.log("Gerados", out.length, "alimentos curados (parte 2) →", target);
