/**
 * As colisões de nome e as fusões.
 *
 * A regra que estes testes guardam é a que separa o oleoduto de quem come: **detectar é do
 * oleoduto, decidir é do dono.** Qual dos dois alimentos fica é uma decisão sobre comida, e
 * um programa que a tome sozinho apaga trabalho de curadoria sem ninguém ver.
 *
 * O terceiro teste existe por causa de um defeito que aconteceu: sem fusões nenhumas, o
 * [aplicarFusoes] devolvia a **mesma** lista que recebeu, quem chamava esvaziava-a para a
 * voltar a encher, e o catálogo saiu com zero alimentos — sem erro nenhum.
 */
import { test } from "node:test";
import assert from "node:assert/strict";

import { normalizar, colisoes, aplicarFusoes } from "./colisoes.mjs";

const alimento = (id, namePt, kcal = 100, origin = "TESTE") => ({ id, namePt, kcal, origin });

test("o mesmo nome escrito por duas mãos é o mesmo nome", () => {
  assert.equal(
    normalizar("Vinho maduro tinto, teor alcoólico ≥12,5% vol."),
    normalizar("vinho  maduro TINTO teor alcoolico 12 5  vol"),
  );
});

test("duas linhas com o mesmo nome são uma colisão", () => {
  const c = colisoes([alimento("a", "Sangria", 89), alimento("b", "sangria", 120)]);

  assert.equal(c.length, 1);
  assert.deepEqual(c[0].alimentos.map((x) => x.id), ["a", "b"]);
});

test("a colisão que engana é a que discorda na energia", () => {

  // Duas linhas com os mesmos números são uma duplicação inofensiva: escolha-se qualquer
  // uma e o dia fica igual. A sangria a 89 contra 120 é outra coisa.
  const iguais = colisoes([alimento("a", "Arroz", 130), alimento("b", "Arroz", 132)]);
  const diferentes = colisoes([alimento("a", "Sangria", 89), alimento("b", "Sangria", 120)]);

  assert.equal(iguais[0].discordam, false);
  assert.equal(diferentes[0].discordam, true);

  // E as que enganam vêm primeiro, porque as outras podem esperar.
  const juntas = colisoes([
    alimento("a", "Arroz", 130), alimento("b", "Arroz", 132),
    alimento("c", "Sangria", 89), alimento("d", "Sangria", 120),
  ]);
  assert.equal(juntas[0].nome, "sangria");
});

test("sem fusões decididas, a lista sai como entrou", () => {

  // O defeito que isto guarda: a lista devolvida era a mesma que entrou, e quem a esvaziava
  // para a voltar a encher ficava com o catálogo vazio e sem erro nenhum.
  const entrada = [alimento("a", "Arroz"), alimento("b", "Feijão")];

  const saida = aplicarFusoes(entrada, {});

  assert.deepEqual(saida.vivos.map((x) => x.id), ["a", "b"]);
  assert.equal(saida.fundidos, 0);
  assert.deepEqual(saida.lapides, []);
});

test("uma fusão tira o perdedor e deixa uma lápide", () => {
  const entrada = [alimento("ciqual-1017", "Sangria", 89), alimento("ptx3_sangria", "Sangria", 120)];

  const saida = aplicarFusoes(entrada, { "ciqual-1017": "ptx3_sangria" });

  assert.deepEqual(saida.vivos.map((x) => x.id), ["ptx3_sangria"]);
  assert.deepEqual(saida.lapides, [{ id: "ciqual-1017", sucessor: "ptx3_sangria" }]);
});

test("uma fusão para um vencedor que não existe não se faz", () => {

  // Sem isto, quem tinha o perdedor nos favoritos seguia a lápide para um alimento apagado.
  // É preferível ficar com o duplicado do que mandar alguém para lado nenhum.
  const entrada = [alimento("a", "Sangria")];

  const saida = aplicarFusoes(entrada, { a: "nao-existe" });

  assert.deepEqual(saida.vivos.map((x) => x.id), ["a"]);
  assert.deepEqual(saida.lapides, []);
});
