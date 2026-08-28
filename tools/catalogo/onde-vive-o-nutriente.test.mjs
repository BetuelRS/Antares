/**
 * Onde vive cada nutriente, e porque isso não pode mudar em silêncio.
 *
 *     node --test tools/
 *
 * **Nasce da §3 do `estudo/dados/04-as-fontes-de-dados.md`**, que se chama «o mesmo facto em
 * dois sítios». A queixa dele era concreta: a fibra estava na coluna para os alimentos do
 * CIQUAL e da USDA e dentro do JSON de micros só para os 1 376 do INSA — e a tabela
 * `food_nutrient`, que responde ao «alimentos ricos em», constrói-se **só** do JSON. Tinha
 * fibra para mil e trezentos alimentos quando sete mil e oitocentos traziam o número.
 *
 * Metade disso está resolvido e a outra metade não. O oleoduto de hoje põe a fibra e o
 * sódio dentro dos micros — as colunas deles nem existem na entidade —, e deixa os açúcares
 * e a gordura saturada só na coluna. **Não é um defeito visível**: nenhum dos dois tem
 * referência da EFSA, e o ecrã do «rico em» só oferece as chaves que têm. É uma armadilha:
 * qualquer coisa futura que leia a `food_nutrient` à procura de açúcares encontra zero
 * linhas em 7 932 alimentos, sem erro nenhum a assinalá-lo.
 *
 * Qual das duas metades deve ganhar é uma decisão do dono com custo permanente — são 81
 * usos em Kotlin e nas ferramentas, duas migrações e o mapeador da Open Food Facts, onde os
 * açúcares e os saturados são campos de rótulo a sério. Este teste não a toma. **Escreve o
 * que é verdade hoje e chumba quando isso mudar por acidente**, que é o que faltava para a
 * armadilha deixar de ser silenciosa.
 */
import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync, existsSync } from "node:fs";
import { join } from "node:path";

const CATALOGO = join(
  import.meta.dirname,
  "..",
  "..",
  "composeApp",
  "src",
  "commonMain",
  "composeResources",
  "files",
  "catalogo.json",
);

/**
 * Os quatro nutrientes de rótulo — o `Nutrients.LABEL` do lado de lá — e onde cada um vive.
 *
 * Se um destes trocar de lado, é porque alguém mexeu no oleoduto: ou a decisão foi tomada e
 * esta lista tem de a acompanhar, ou foi um acidente e o teste apanhou-o.
 */
const ONDE_VIVE = {
  fiber_g: "micros",
  sodium_mg: "micros",
  sugarsG: "coluna",
  satFatG: "coluna",
};

function catalogo() {
  const bruto = JSON.parse(readFileSync(CATALOGO, "utf8"));
  return Array.isArray(bruto) ? bruto : Object.values(bruto).find(Array.isArray);
}

test("os nutrientes de rótulo continuam onde estão documentados", (t) => {
  // O catálogo é derivado e pode não estar construído numa árvore acabada de clonar.
  if (!existsSync(CATALOGO)) return t.skip("catálogo por construir");

  const alimentos = catalogo();
  assert.ok(alimentos.length > 0, "o catálogo abriu vazio");

  for (const [chave, onde] of Object.entries(ONDE_VIVE)) {
    const nosMicros = alimentos.filter((a) => a.micros && a.micros[chave] != null).length;
    const naColuna = alimentos.filter((a) => a[chave] != null).length;

    if (onde === "micros") {
      assert.ok(nosMicros > 0, `${chave} devia estar nos micros e não está em alimento nenhum`);
      assert.equal(naColuna, 0, `${chave} passou a estar também na coluna — dois sítios outra vez`);
    } else {
      assert.ok(naColuna > 0, `${chave} devia estar na coluna e não está em alimento nenhum`);
      assert.equal(nosMicros, 0, `${chave} passou a estar também nos micros — dois sítios outra vez`);
    }
  }
});

/**
 * O que a `food_nutrient` vê, dito em número.
 *
 * Não chumba por os açúcares darem zero — é o que está documentado acima. Chumba se a fibra
 * ou o sódio caírem para perto de zero, que é o defeito original a voltar.
 */
test("a fibra e o sódio chegam à tabela que responde ao «rico em»", (t) => {
  if (!existsSync(CATALOGO)) return t.skip("catálogo por construir");

  const alimentos = catalogo();
  const comValor = (chave) => alimentos.filter((a) => a.micros && a.micros[chave] > 0).length;

  // Metade do catálogo, à vontade. O corte é folgado de propósito: o que se guarda aqui é a
  // ordem de grandeza, e não um número que muda a cada lote de curadoria.
  const minimo = alimentos.length / 2;
  assert.ok(
    comValor("fiber_g") > minimo,
    `só ${comValor("fiber_g")} alimentos levam fibra à food_nutrient, de ${alimentos.length}`,
  );
  assert.ok(
    comValor("sodium_mg") > minimo,
    `só ${comValor("sodium_mg")} alimentos levam sódio à food_nutrient, de ${alimentos.length}`,
  );
});
