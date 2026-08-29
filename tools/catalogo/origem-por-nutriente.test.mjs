/**
 * A origem de cada nutriente, e porque só a excepção é escrita.
 *
 *     node --test tools/
 *
 * **Nasce do esboço 22**, que desenha a ficha do alimento com a origem ao lado de cada
 * número — «Cálcio 30 mg · INSA · medido», «Iodo 32 µg · CIQUAL · medido» — e escreve a razão
 * na anotação: *«a origem é por nutriente, e o iodo pode vir do CIQUAL mesmo num alimento do
 * INSA — que é o que a fusão por prioridade faz»*. Até à v36 do esquema a app mostrava uma
 * origem por alimento, que é a de quem lhe deu o nome e as calorias, e calava a dos outros.
 *
 * O que este teste guarda é o **formato**, e não o conteúdo: o oleoduto escreve a origem só
 * onde ela diverge da do alimento, e a ausência quer dizer «veio de onde veio o alimento».
 * Trocar isso por um mapa completo multiplicaria a coluna por oito mil linhas e inverteria o
 * significado da ausência — que é o género de mudança que passa despercebida num `git diff`
 * de cinco megabytes.
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

/** As que o oleoduto sabe marcar hoje. Uma marca fora desta lista é um acidente. */
const ORIGENS = new Set(["CIQUAL", "TCA", "USDA"]);

function catalogo() {
  return JSON.parse(readFileSync(CATALOGO, "utf8"));
}

/** A origem que a app deduz do identificador, e que por isso não se escreve outra vez. */
function origemDoAlimento(a) {
  if (a.id.startsWith("ciqual-")) return "CIQUAL";
  if (a.id.startsWith("tca-")) return "TCA";
  if (a.id.startsWith("usda-")) return "USDA";
  return null;
}

test("a origem por nutriente existe, e é a excepção e não a regra", (t) => {
  if (!existsSync(CATALOGO)) return t.skip("catálogo por construir");

  const { alimentos } = catalogo();
  const comOrigem = alimentos.filter((a) => a.microsOrigem);

  assert.ok(
    comOrigem.length > 0,
    "nenhum alimento traz a origem por nutriente — o oleoduto deixou de a marcar",
  );

  // O caso comum é não ter marca nenhuma. Se isto se inverter, alguém passou a escrever o
  // mapa inteiro e a coluna cresceu sem ninguém decidir isso.
  assert.ok(
    comOrigem.length < alimentos.length / 2,
    `${comOrigem.length} de ${alimentos.length} alimentos trazem marcas — ` +
      "isto devia ser a excepção, e passou a ser a regra",
  );
});

test("nenhuma marca repete a origem do próprio alimento", (t) => {
  if (!existsSync(CATALOGO)) return t.skip("catálogo por construir");

  const { alimentos } = catalogo();
  const redundantes = [];
  for (const a of alimentos) {
    if (!a.microsOrigem) continue;
    const propria = origemDoAlimento(a);
    for (const [chave, origem] of Object.entries(a.microsOrigem)) {
      if (origem === propria) redundantes.push(`${a.id}.${chave}`);
    }
  }

  assert.deepEqual(
    redundantes.slice(0, 5),
    [],
    `${redundantes.length} marcas dizem a origem que a app já deduz do identificador`,
  );
});

test("uma marca aponta sempre para um nutriente que o alimento tem", (t) => {
  if (!existsSync(CATALOGO)) return t.skip("catálogo por construir");

  const { alimentos } = catalogo();
  const orfas = [];
  for (const a of alimentos) {
    if (!a.microsOrigem) continue;
    for (const chave of Object.keys(a.microsOrigem)) {
      if (!a.micros || a.micros[chave] == null) orfas.push(`${a.id}.${chave}`);
    }
  }

  // A coerência apaga a água de quem não fecha o balanço de massa, e a fusão faz o perdedor
  // desaparecer. Uma marca que sobreviva a isso aponta para um número que já não existe.
  assert.deepEqual(
    orfas.slice(0, 5),
    [],
    `${orfas.length} marcas apontam para nutrientes que o alimento não tem`,
  );
});

test("as origens marcadas são as que a app sabe ler", (t) => {
  if (!existsSync(CATALOGO)) return t.skip("catálogo por construir");

  const { alimentos } = catalogo();
  const desconhecidas = new Set();
  for (const a of alimentos) {
    if (!a.microsOrigem) continue;
    for (const origem of Object.values(a.microsOrigem)) {
      if (!ORIGENS.has(origem)) desconhecidas.add(origem);
    }
  }

  assert.deepEqual(
    [...desconhecidas],
    [],
    "o oleoduto emitiu uma origem que o `FoodProvenance` não conhece — a app deita-a fora " +
      "em silêncio, e a linha fica sem marca",
  );
});
