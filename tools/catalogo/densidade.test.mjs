import { test } from "node:test";
import assert from "node:assert/strict";
import { densidadeDe, pareceSolido, POR_NOME } from "./densidade.mjs";

const alimento = (namePt, nameEn = "", familia = null) => ({ namePt, nameEn, familia });

test("o azeite e menos denso do que a agua", () => {
  assert.equal(densidadeDe(alimento("Azeite virgem extra")), 0.918);
  assert.ok(densidadeDe(alimento("Azeite")) < 1);
});

test("le tambem o nome ingles, que 2909 alimentos ainda tem", () => {
  assert.equal(densidadeDe(alimento("Oil, olive", "Oil, olive")), 0.918);
});

test("o mel e muito mais denso do que a agua", () => {
  assert.ok(densidadeDe(alimento("Mel")) > 1.4);
});

test("a agua vale exactamente um", () => {
  assert.equal(densidadeDe(alimento("Água mineral")), 1.0);
});

test("o leite magro pesa mais do que o gordo", () => {
  const magro = densidadeDe(alimento("Leite magro"));
  const gordo = densidadeDe(alimento("Leite inteiro"));
  assert.ok(magro > gordo, `magro ${magro} devia ser maior do que gordo ${gordo}`);
});

test("um alimento sem padrao nem familia nao tem densidade", () => {
  assert.equal(densidadeDe(alimento("Bacalhau à Brás")), null);
});

test("a familia serve o que o nome nao apanhou", () => {
  assert.equal(densidadeDe(alimento("Kir royal", "", "bebidas_alcoolicas")), 0.99);
});

test("todas as densidades sao plausiveis", () => {
  for (const [padrao, valor, fonte] of POR_NOME) {
    assert.ok(valor > 0.7 && valor < 1.5, `${padrao} tem densidade impossivel: ${valor}`);
    assert.ok(typeof fonte === "string" && fonte.length > 10, `${padrao} sem fonte escrita`);
  }
});

test("um solido cozido em agua nao e um liquido", () => {
  assert.ok(pareceSolido(alimento("Lavagante, cozido em água")));
  assert.ok(pareceSolido(alimento("Mussels", "Mussels, boiled/cooked in water")));
  assert.ok(pareceSolido(alimento("Leite em pó, magro")));
});

test("uma bebida a serio nao parece solida", () => {
  assert.ok(!pareceSolido(alimento("Sumo de laranja")));
  assert.ok(!pareceSolido(alimento("Cerveja")));
});

/**
 * O padrao mais especifico tem de vir antes do mais geral.
 *
 * «Leite magro» tem de casar antes de «leite», ou o magro apanhava a densidade do gordo — e
 * o teste anterior passava a comparar o mesmo numero consigo proprio sem ninguem notar.
 */
test("os padroes estao ordenados do especifico para o geral", () => {
  const iMagro = POR_NOME.findIndex(([p]) => p.source.includes("magro"));
  const iLeite = POR_NOME.findIndex(([p]) => p.source.includes("leite|milk"));
  assert.ok(iMagro >= 0 && iLeite >= 0, "os dois padroes do leite tem de existir");
  assert.ok(iMagro < iLeite, "o leite magro tem de ser testado antes do leite");
});
