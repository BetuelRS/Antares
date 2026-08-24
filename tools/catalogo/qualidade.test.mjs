/**
 * O motor de qualidade, contra alimentos escritos à mão.
 *
 *     node --test tools/
 *
 * O que se prova aqui não é que ele encontra coisas — o catálogo verdadeiro já o mostra. É que
 * **não encontra o que não devia**, que é o modo de falhar de um verificador: uma fila de
 * revisão com duzentos falsos positivos é uma fila que ninguém abre, e a partir daí os
 * verdadeiros também lá ficam.
 */
import { test } from "node:test";
import assert from "node:assert/strict";

import { atwater, massa, partes, foraDeEscala, discordancia } from "./qualidade.mjs";

/** Um alimento coerente: 100 g de nada de especial, com as contas todas a fechar. */
function alimento(campos = {}) {
  return {
    id: "teste-1",
    nameEn: "Comida de teste",
    origin: "TESTE",
    kcal: 100,
    proteinG: 5,
    carbsG: 10,
    fatG: 5,
    ...campos,
  };
}

test("um alimento coerente não dá achado nenhum", () => {
  const a = alimento();
  assert.equal(atwater(a), null);
  assert.equal(massa(a), null);
  assert.deepEqual(partes(a), []);
});

test("a energia que não bate com os macros é suspeita, não contradição", () => {
  const achado = atwater(alimento({ kcal: 400 }));

  assert.equal(achado.tipo, "atwater");

  // Não chumba a construção. As fontes medem a energia por bomba calorimétrica e os macros
  // por outros métodos, e as duas coisas não fecham ao cêntimo.
  assert.equal(achado.gravidade, "suspeita");
});

test("a fibra pode estar dentro ou fora dos hidratos, e nenhuma das leituras acusa", () => {

  // 5 g de proteína, 10 de hidratos, 5 de gordura = 105 kcal. Com 10 g de fibra a 2 kcal são
  // 125. As duas contas têm de passar: a CIQUAL publica hidratos disponíveis e a USDA
  // publica-os por diferença, com a fibra já lá dentro.
  assert.equal(atwater(alimento({ kcal: 105, fiberG: 10 })), null);
  assert.equal(atwater(alimento({ kcal: 125, fiberG: 10 })), null);
});

test("os polióis também, e foi por isso que os rebuçados sem açúcar apareciam", () => {
  const semAcucar = { micros: { polyols_g: 50 } };

  assert.equal(atwater(alimento({ kcal: 105, ...semAcucar })), null);
  assert.equal(atwater(alimento({ kcal: 225, ...semAcucar })), null);
});

test("um estado de texto não entra nas contas como se fosse número", () => {

  // Um `NaN` numa comparação é sempre falso, e o verificador passava a não acusar nada sem
  // dar erro. É a falha mais perigosa deste ficheiro, porque se parece com estar tudo bem.
  const oleo = alimento({ fatG: 100, kcal: 900, micros: { water_g: "<0.03" } });

  assert.equal(massa(oleo), null);
});

test("mais de cem gramas em cem gramas é contradição", () => {
  const achado = massa(alimento({ carbsG: 8, fatG: 0, proteinG: 0, micros: { water_g: 95.4 } }));

  assert.equal(achado.tipo, "massa");
  assert.equal(achado.gravidade, "contradicao");
});

test("o álcool não se soma à água que já o contém", () => {

  // Um vinho: 89.7 g de água medida por secagem — que leva o álcool com ela — mais 12.9 g de
  // álcool e 0.9 de hidratos. Somar os três dava 103.5 g, e três vinhos da TCA foram
  // acusados assim antes de isto existir.
  const vinho = alimento({
    kcal: 96, proteinG: 0, carbsG: 0.9, fatG: 0,
    micros: { water_g: 89.7, alcohol_g: 12.9 },
  });

  assert.equal(massa(vinho), null);
});

test("as partes não podem passar o todo", () => {
  const gordo = alimento({ fatG: 10, satFatG: 6, micros: { fatMono_g: 5, fatPoly_g: 2 } });
  const doce = alimento({ carbsG: 5, sugarsG: 9 });

  assert.equal(partes(gordo)[0].tipo, "gorduras");
  assert.equal(partes(gordo)[0].gravidade, "contradicao");
  assert.equal(partes(doce)[0].tipo, "acucares");
});

test("o arredondamento de três números somados não conta como contradição", () => {

  // 3.4 + 4.3 + 2.4 = 10.1 contra 10.0. É o último algarismo de cada um, não um erro.
  const gordo = alimento({ fatG: 10, satFatG: 3.4, micros: { fatMono_g: 4.3, fatPoly_g: 2.4 } });

  assert.deepEqual(partes(gordo), []);
});

test("fora de escala mede-se contra o grupo, e um grupo pequeno não chega para julgar", () => {
  const grupo = (n, kcal) => alimento({ id: `g-${n}`, kcal, grupo: "0407" });
  const tipicos = Array.from({ length: 12 }, (_, i) => grupo(i, 100 + i));

  const comIntruso = foraDeEscala([...tipicos, grupo("intruso", 900)]);
  assert.equal(comIntruso.length, 1);
  assert.equal(comIntruso[0].id, "g-intruso");
  assert.equal(comIntruso[0].gravidade, "suspeita");

  // Com quatro alimentos no grupo não há escala nenhuma para se estar fora dela.
  assert.deepEqual(foraDeEscala([...tipicos.slice(0, 4), grupo("intruso", 900)]), []);
});

test("um alimento sem grupo não é comparado com o mundo", () => {

  // Só a CIQUAL publica uma árvore de grupos. Agrupar as outras fontes pelo nome punha o
  // leite de coco ao pé do leite e passava a acusar os dois.
  const semGrupo = Array.from({ length: 12 }, (_, i) => alimento({ id: `s-${i}`, kcal: 100 }));

  assert.deepEqual(foraDeEscala([...semGrupo, alimento({ id: "s-x", kcal: 900 })]), []);
});

test("a discordância entre fontes não escolhe qual delas tem razão", () => {
  const achado = discordancia({
    alimento: alimento({ kcal: 344, nameEn: "Rice, wild, raw" }),
    outraFonte: "USDA",
    outraEnergia: 101,
  });

  assert.equal(achado.tipo, "discordancia");
  assert.equal(achado.gravidade, "suspeita");
  assert.match(achado.mensagem, /344/);
  assert.match(achado.mensagem, /101/);
});
