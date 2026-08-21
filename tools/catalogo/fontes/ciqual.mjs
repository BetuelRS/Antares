/**
 * A CIQUAL, da ANSES — a base do catálogo.
 *
 * Lê `alim.xml` e `compo.xml` tal como vêm da fonte, e devolve alimentos já nas unidades
 * da app. Não escreve nada: quem escreve é o `construir.mjs`.
 *
 * **A escada de recuperação.** A CIQUAL publica `-` quando um valor não foi determinado, e
 * isso não é zero. O importador antigo deitava fora o alimento inteiro quando lhe faltava
 * um macro, e foi assim que se perderam noventa e nove. Aqui tenta-se por degraus, do mais
 * fiável ao menos, e cada alimento fica a saber por que degrau passou:
 *
 * 1. `medido` — o valor está publicado.
 * 2. `atwater` — a energia sai dos três macros, que é a fórmula com que ela é calculada.
 * 3. `atwater-invertido` — falta um macro e a energia está publicada, portanto o macro
 *    sai da mesma equação ao contrário. É derivação do que a fonte diz, não invenção.
 * 4. sem degrau — o alimento fica de fora e o `construir.mjs` exige que esteja declarado.
 */
import { readFileSync, existsSync } from "node:fs";
import { join } from "node:path";

const MACRO_BY_CODE = {
  328: "kcal", 25000: "proteinG", 31000: "carbsG", 32000: "sugarsG",
  40000: "fatG", 40302: "satFatG", 34100: "fiberG", 10110: "sodiumMg",
};

const MICRO_BY_CODE = {
  10120: "magnesium_mg", 10150: "phosphorus_mg", 10190: "potassium_mg",
  10200: "calcium_mg", 10251: "manganese_mg", 10260: "iron_mg",
  10290: "copper_mg", 10300: "zinc_mg", 10340: "selenium_ug",
  10530: "iodine_ug",
  51104: "vitA_ug", 52100: "vitD_ug", 53100: "vitE_mg", 54101: "vitK_ug",
  55100: "vitC_mg", 56100: "vitB1_mg", 56200: "vitB2_mg", 56310: "vitB3_mg",
  56400: "vitB5_mg", 56500: "vitB6_mg", 56600: "vitB12_ug", 56700: "vitB9_ug",
  400: "water_g", 60000: "alcohol_g", 75100: "cholesterol_mg",
  40303: "fatMono_g", 40304: "fatPoly_g",
  10170: "chloride_mg",
  41833: "omega3_g", 41826: "omega6_g", 42053: "epa_g", 42263: "dha_g",
  33110: "starch_g", 32410: "lactose_g", 34000: "polyols_g",
  51200: "retinol_ug", 51330: "betaCarotene_ug",
};

export function parseBlocks(xml, tag) {
  const out = [];
  const re = new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`, "g");
  for (const m of xml.matchAll(re)) {
    const obj = {};
    for (const f of m[1].matchAll(/<(\w+)>([\s\S]*?)<\/\1>/g)) obj[f[1]] = decode(f[2].trim());
    out.push(obj);
  }
  return out;
}

function decode(s) {
  return s
    .replace(/&lt;/g, "<").replace(/&gt;/g, ">")
    .replace(/&apos;/g, "'").replace(/&quot;/g, '"')
    .replace(/&amp;/g, "&");
}

function teneur(raw, { tracesAsZero = false } = {}) {
  if (raw == null) return null;
  const s = String(raw).trim();
  if (!s || s === "-") return null;
  if (s === "traces" || s.startsWith("<")) return tracesAsZero ? 0 : null;
  const n = parseFloat(s.replace(/\s/g, "").replace(",", "."));
  return Number.isFinite(n) ? n : null;
}

const round = (v, d = 1) => (v == null ? null : Math.round(v * 10 ** d) / 10 ** d);

const KCAL_PROTEINA = 4;
const KCAL_HIDRATOS = 4;
const KCAL_GORDURA = 9;

function atwater(m) {
  if (m.proteinG == null || m.carbsG == null || m.fatG == null) return null;
  return KCAL_PROTEINA * m.proteinG + KCAL_HIDRATOS * m.carbsG + KCAL_GORDURA * m.fatG;
}

function atwaterInvertido(m) {
  const emFalta = ["proteinG", "carbsG", "fatG"].filter((k) => m[k] == null);
  if (m.kcal == null || emFalta.length !== 1) return null;
  const falta = emFalta[0];
  const factor = falta === "fatG" ? KCAL_GORDURA : KCAL_PROTEINA;
  const outros =
    (falta === "proteinG" ? 0 : KCAL_PROTEINA * m.proteinG) +
    (falta === "carbsG" ? 0 : KCAL_HIDRATOS * m.carbsG) +
    (falta === "fatG" ? 0 : KCAL_GORDURA * m.fatG);
  const gramas = (m.kcal - outros) / factor;
  if (!Number.isFinite(gramas) || gramas < 0) return null;
  return { chave: falta, valor: round(gramas, 2) };
}

export function lerCiqual(dataDir) {
  for (const f of ["alim.xml", "compo.xml"]) {
    if (!existsSync(join(dataDir, f))) {
      throw new Error(`Falta ${join(dataDir, f)} — descarregar da CIQUAL; ver tools/README.md.`);
    }
  }

  const alims = parseBlocks(readFileSync(join(dataDir, "alim.xml"), "utf8"), "ALIM");
  const compos = parseBlocks(readFileSync(join(dataDir, "compo.xml"), "utf8"), "COMPO");

  const porAlimento = new Map();
  for (const c of compos) {
    const codigo = Number(c.const_code);
    const macro = MACRO_BY_CODE[codigo];
    const micro = MICRO_BY_CODE[codigo];
    if (!macro && !micro) continue;
    const v = teneur(c.teneur, { tracesAsZero: Boolean(macro) });
    if (v == null) continue;
    let rec = porAlimento.get(c.alim_code);
    if (!rec) { rec = { macros: {}, micros: {} }; porAlimento.set(c.alim_code, rec); }
    if (macro) rec.macros[macro] = v;
    if (micro) rec.micros[micro] = v;
  }

  const alimentos = [];
  const foraDeAlcance = [];
  const degraus = { medido: 0, atwater: 0, "atwater-invertido": 0 };

  for (const a of alims) {
    const nomeEn = a.alim_nom_eng || a.alim_nom_fr;
    const rec = porAlimento.get(a.alim_code);
    if (!rec) {
      foraDeAlcance.push({
        id: `ciqual-${a.alim_code}`, nome: nomeEn,
        porque: "sem valores de composicao",
      });
      continue;
    }
    const m = { ...rec.macros };
    let degrau = "medido";
    let derivado = null;

    if (m.kcal == null) {
      const kcal = atwater(m);
      if (kcal != null) { m.kcal = kcal; degrau = "atwater"; derivado = "kcal"; }
    }
    if (["proteinG", "carbsG", "fatG"].some((k) => m[k] == null)) {
      const inv = atwaterInvertido(m);
      if (inv) { m[inv.chave] = inv.valor; degrau = "atwater-invertido"; derivado = inv.chave; }
    }

    const aindaFalta = ["kcal", "proteinG", "carbsG", "fatG"].filter((k) => m[k] == null);
    if (aindaFalta.length) {
      foraDeAlcance.push({
        id: `ciqual-${a.alim_code}`, nome: nomeEn,
        porque: `a CIQUAL nao determinou ${aindaFalta.join(" nem ")}`,
      });
      continue;
    }
    degraus[degrau]++;

    const micros = {};
    for (const [k, v] of Object.entries(rec.micros)) {
      const r = round(v, 3);
      if (r != null && r > 0) micros[k] = r;
    }

    alimentos.push({
      id: `ciqual-${a.alim_code}`,
      source: "SEED",
      sourceRef: a.alim_code,
      nameEn: nomeEn,
      namePt: nomeEn,
      brand: null,
      kcal: Math.round(m.kcal),
      proteinG: round(m.proteinG),
      carbsG: round(m.carbsG),
      sugarsG: round(m.sugarsG),
      fatG: round(m.fatG),
      satFatG: round(m.satFatG),
      fiberG: round(m.fiberG),
      sodiumMg: m.sodiumMg == null ? null : Math.round(m.sodiumMg),
      micros: Object.keys(micros).length ? micros : null,
      servingName: null,
      servingGrams: null,
      verified: true,
      origin: "CIQUAL",
      derivado,
    });
  }

  return { declarados: alims.length, alimentos, foraDeAlcance, degraus };
}
