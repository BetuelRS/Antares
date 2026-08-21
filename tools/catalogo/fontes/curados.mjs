/**
 * Os alimentos escritos à mão, e a tabela de micronutrientes que os acompanha.
 *
 * São a única fonte sem origem externa: pratos portugueses que nem a CIQUAL nem a TCA têm
 * com o nome por que se procuram — «francesinha», «bifana», «café com leite». Por isso
 * moram em `tools/catalogo/dados/` e não numa pasta `data/` fora do git: **são a fonte, e
 * não uma cópia dela.**
 *
 * `verified` fica falso de propósito. Foram estimados a partir de receitas, não medidos, e
 * a app usa esse campo para dizer a quem procura de onde vem o número.
 */
import { readFileSync, existsSync } from "node:fs";
import { join } from "node:path";

export const FICHEIROS = ["seed_foods_pt.json", "seed_foods_pt2.json", "seed_foods_pt3.json"];

export function lerCurados(dadosDir) {
  const alimentos = [];
  const repetidos = [];
  const vistos = new Set();

  for (const f of FICHEIROS) {
    const p = join(dadosDir, f);
    if (!existsSync(p)) throw new Error(`Falta ${p} — é fonte, não é derivado.`);
    for (const e of JSON.parse(readFileSync(p, "utf8"))) {
      if (vistos.has(e.id)) { repetidos.push({ id: e.id, ficheiro: f }); continue; }
      vistos.add(e.id);
      alimentos.push({
        brand: null,
        sourceRef: null,
        sugarsG: null,
        satFatG: null,
        fiberG: null,
        sodiumMg: null,
        micros: null,
        servingName: null,
        servingGrams: null,
        verified: false,
        ...e,
        origin: e.origin || "PT",
        derivado: null,
      });
    }
  }

  return { declarados: alimentos.length + repetidos.length, alimentos, repetidos };
}

/**
 * A tabela de micronutrientes dos curados, medida à parte e ligada por identificador.
 * O que já vem no alimento ganha a esta tabela: ela só preenche buracos.
 */
export function lerMicrosCurados(dadosDir) {
  const p = join(dadosDir, "seed_pt_micros.json");
  if (!existsSync(p)) throw new Error(`Falta ${p}.`);
  return JSON.parse(readFileSync(p, "utf8"));
}
