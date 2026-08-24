/**
 * A oficina, nas duas peças que podem estragar trabalho: a ordem da fila e a escrita.
 *
 * A escrita é a que assusta. O `correcoes.json` tem dois mil setecentos e sete nomes lá
 * dentro, juntados ao longo de meses, e **uma escrita mal feita apaga-os sem dar erro** — só
 * se descobre na construção seguinte, quando o catálogo volta a ter nomes de laboratório em
 * inglês. É por isso que a função que decide é pura e está aqui.
 */
import { test } from "node:test";
import assert from "node:assert/strict";

import { montarFila, comparar, segmentos, porTraduzir } from "./fila.mjs";
import { aplicar } from "./decisoes.mjs";

const alimento = (id, campos = {}) => ({ id, namePt: id, nameEn: id, kcal: 100, ...campos });

test("a fila põe à frente o que se come, e não o que tem mais achados", () => {
  const fila = montarFila({
    alimentos: [alimento("pao"), alimento("shiitake")],
    achados: [
      { id: "shiitake", tipo: "escala", gravidade: "suspeita" },
      { id: "shiitake", tipo: "atwater", gravidade: "suspeita" },
    ],
    historico: { pao: 240 },
  });

  assert.equal(fila[0].id, "pao");
  assert.equal(fila[0].registos, 240);
  assert.equal(fila[1].achados.length, 2);
});

test("sem histórico a fila cai para o número de achados, em vez de deixar de existir", () => {
  const fila = montarFila({
    alimentos: [alimento("a"), alimento("b")],
    achados: [{ id: "b", tipo: "massa", gravidade: "contradicao" }],
  });

  assert.equal(fila[0].id, "b");
});

test("dois empatados não trocam de lugar entre execuções", () => {

  // Sem o terceiro critério, quem estivesse a trabalhar na fila perdia o sítio de cada vez
  // que a recarregasse.
  const x = { id: "x", registos: 3, achados: [] };
  const y = { id: "y", registos: 3, achados: [] };

  assert.ok(comparar(x, y) < 0);
  assert.ok(comparar(y, x) > 0);
});

test("o nome parte-se nos segmentos por que as fontes o escrevem", () => {
  assert.deepEqual(segmentos("Rice, wild, raw"), ["Rice", "wild", "raw"]);
  assert.deepEqual(segmentos("Pastis"), ["Pastis"]);
  assert.deepEqual(segmentos(null), []);
});

test("meio nome traduzido conta como meio, e não como traduzido", () => {

  // É o caso comum e o mais difícil de ver a olho numa lista de oito mil.
  assert.deepEqual(porTraduzir("Rice, wild, raw", "Arroz, wild, cru"), ["wild"]);
  assert.deepEqual(porTraduzir("Rice, wild, raw", "Arroz selvagem cru"), []);
  assert.deepEqual(porTraduzir("Rice, wild, raw", "Rice, wild, raw"), ["Rice", "wild", "raw"]);
});

test("escrever um nome não mexe nos outros dois mil", () => {
  const antes = { nomes: { a: "Pão", b: "Leite" }, liquidos: ["b"], podados: [] };

  const depois = aplicar(antes, { id: "c", tipo: "nome", valor: "Arroz" });

  assert.deepEqual(depois.nomes, { a: "Pão", b: "Leite", c: "Arroz" });
  assert.deepEqual(depois.liquidos, ["b"]);
});

test("a função não altera o que recebeu", () => {

  // Uma cópia rasa deixava as listas partilhadas, e quem guardasse o estado anterior via-o
  // mudar debaixo dos pés. É a falha que uma função pura existe para não ter.
  const antes = { nomes: { a: "Pão" }, liquidos: [], podados: [] };

  aplicar(antes, { id: "b", tipo: "liquido", valor: true });

  assert.deepEqual(antes.liquidos, []);
  assert.deepEqual(antes.nomes, { a: "Pão" });
});

test("um nome vazio apaga a entrada em vez de gravar vazio", () => {

  // É o único modo de desfazer um nome mal posto: sem entrada, fica o que a fonte diz.
  const depois = aplicar({ nomes: { a: "Enganei-me" } }, { id: "a", tipo: "nome", valor: "  " });

  assert.deepEqual(depois.nomes, {});
});

test("as listas ficam ordenadas, para o diff dizer o que mudou", () => {
  const depois = aplicar({ liquidos: ["c", "a"] }, { id: "b", tipo: "liquido", valor: true });

  assert.deepEqual(depois.liquidos, ["a", "b", "c"]);
});

test("desligar um interruptor tira o alimento da lista", () => {
  const depois = aplicar({ podados: ["a", "b"] }, { id: "a", tipo: "podar", valor: false });

  assert.deepEqual(depois.podados, ["b"]);
});

test("uma porção sem gramas não se guarda a meio", () => {
  const comPorcao = { porcoes: { a: { nome: "uma fatia", gramas: 30 } } };

  assert.deepEqual(aplicar(comPorcao, { id: "a", tipo: "porcao", nome: "uma fatia" }).porcoes, {});
  assert.deepEqual(aplicar(comPorcao, { id: "a", tipo: "porcao", gramas: 30 }).porcoes, {});
  assert.deepEqual(
    aplicar({}, { id: "a", tipo: "porcao", nome: "um copo", gramas: "200" }).porcoes,
    { a: { nome: "um copo", gramas: 200 } },
  );
});

test("uma decisão que a oficina não conhece não escreve nada", () => {
  assert.throws(() => aplicar({}, { id: "a", tipo: "apagar-tudo" }), /desconhecida/);
  assert.throws(() => aplicar({}, { tipo: "nome", valor: "x" }), /sem alimento/);
});
