/**
 * O tradutor de nomes, que é a peça que mais pode estragar em silêncio.
 *
 * **Um nome trocado não rebenta, não dá erro, e vai para a loja.** É o modo de falhar de todo
 * o bloco de curadoria, e é por isso que estas regras — concordância, composição, plural —
 * são exercidas uma a uma em vez de se confiar no resultado sobre o catálogo inteiro.
 *
 * O que se prova aqui não é que traduz muito: é que **não inventa**. Um segmento que o
 * dicionário não tem, e que as regras não conseguem compor, deixa o nome incompleto — e um
 * nome incompleto não é aplicado.
 */
import { test } from "node:test";
import assert from "node:assert/strict";

import { lerVocabulario, traduzirNome, flexionar, compor } from "./traduzir.mjs";

const voc = lerVocabulario();

test("o dicionário abre e traz o que se espera de um dicionário", () => {
  assert.ok(voc.size > 300, `só ${voc.size} segmentos`);
  assert.equal(voc.get("raw").tipo, "qualificador");
  assert.equal(voc.get("beef").genero, "f");
});

test("os qualificadores concordam com a base, em género e em número", () => {

  // «Carne» é feminino e «espinafres» é masculino plural. Sem concordância sai «Carne, cru»
  // e «Espinafres, cozinhado», que se lê como tradução automática e mina a confiança em
  // todo o resto do catálogo.
  assert.equal(traduzirNome("Beef, neck, raw", voc).nome, "Carne de vaca, pescoço, crua");
  assert.equal(traduzirNome("Spinach, cooked", voc).nome, "Espinafres, cozinhados");
  assert.equal(traduzirNome("Rice, raw", voc).nome, "Arroz, cru");
});

test("a flexão irregular escreve-se, a regular deriva-se", () => {

  // Só os adjetivos em «-o» marcam género, e é regra. O «cru» não é, e por isso está no
  // dicionário com as duas formas.
  assert.equal(flexionar("cozido", "f", "s"), "cozida");
  assert.equal(flexionar("cozido", "m", "p"), "cozidos");
  assert.equal(flexionar("cru/crua", "f", "p"), "cruas");

  // Terminados em «-e» e em consoante não marcam género; o plural segue a última letra.
  assert.equal(flexionar("doce", "f", "p"), "doces");
  assert.equal(flexionar("natural", "m", "p"), "naturais");
  assert.equal(flexionar("comum", "f", "p"), "comuns");
});

test("o inglês compõe ao contrário, e o português leva preposição", () => {
  assert.equal(compor("chestnut flour", voc), "farinha de castanha");
  assert.equal(compor("dark chocolate", voc), "chocolate escuro");
  assert.equal(compor("with vegetables", voc), "com produtos hortícolas");
});

test("uma composição que se repete a si própria não se faz", () => {

  // «Frankfurter sausage» dava «salsicha de salsicha de Frankfurt»: o inglês repete o núcleo
  // que o português já traz dentro da outra palavra.
  assert.equal(compor("frankfurter sausage", voc), "salsicha de Frankfurt");
});

test("o plural inglês desfaz-se, e o português volta a fazer-se", () => {
  assert.equal(traduzirNome("Lentils, raw", voc).nome, "Lentilhas, cruas");
  assert.equal(traduzirNome("Tomatoes, canned", voc).nome, "Tomates, em conserva");
  assert.equal(traduzirNome("Cherries, sweet, raw", voc).nome, "Cerejas, doces, cruas");
});

test("o que o dicionário não tem fica em inglês, e diz-se", () => {
  const r = traduzirNome("Rice, wild, xpto-inventado", voc);

  assert.equal(r.completo, false);
  assert.deepEqual(r.porTraduzir, ["xpto-inventado"]);
});

test("meio traduzido nunca conta como traduzido", () => {

  // É a regra que impede «Arroz, wild, cru» de chegar ao catálogo. Um nome incompleto parece
  // um defeito, e quem o lê não sabe se o alimento é o que diz ser.
  const r = traduzirNome("Cheese, xyzabc, raw", voc);

  assert.equal(r.completo, false);
  assert.ok(r.nome.includes("xyzabc"), "o que faltou tem de continuar visível");
});

test("uma composição só se faz com as duas palavras conhecidas", () => {

  // Adivinhar metade de um composto é exactamente o que este ficheiro existe para não fazer.
  assert.equal(compor("xyzabc flour", voc), null);
  assert.equal(compor("chestnut xyzabc", voc), null);
});

test("um nome vazio não devolve um nome vazio", () => {
  assert.equal(traduzirNome("", voc).nome, null);
  assert.equal(traduzirNome(null, voc).completo, false);
});
