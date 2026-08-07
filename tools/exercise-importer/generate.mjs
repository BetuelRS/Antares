
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const IMAGE_BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/";
const OUT = join(here, "..", "..", "composeApp", "src", "commonMain", "composeResources", "files", "seed_exercises.json");

const src = JSON.parse(readFileSync(join(here, "data", "exercises.json"), "utf8"));
const terms = JSON.parse(readFileSync(join(here, "pt-terms.json"), "utf8"));

const esc = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

function buildRules(map) {
    return Object.entries(map)
        .sort((a, b) => b[0].length - a[0].length)
        .map(([en, pt]) => ({ re: new RegExp(`\\b${esc(en)}\\b`, "gi"), pt }));
}
const phraseRules = buildRules(terms.phrases);
const wordRules = buildRules(terms.words);

function toPt(nameEn) {
    let out = nameEn;
    for (const r of phraseRules) out = out.replace(r.re, r.pt);
    for (const r of wordRules) out = out.replace(r.re, r.pt);
    return out.replace(/\s{2,}/g, " ").trim();
}

const exercises = src.map((x) => ({
    id: x.id,
    nameEn: x.name,
    namePt: toPt(x.name),
    category: x.category ?? "strength",
    force: x.force ?? null,
    mechanic: x.mechanic ?? null,
    equipment: x.equipment ?? null,
    level: x.level ?? "beginner",
    primaryMuscles: x.primaryMuscles ?? [],
    secondaryMuscles: x.secondaryMuscles ?? [],
    instructionsEn: x.instructions ?? [],
    instructionsPt: [],
    images: x.images ?? [],
    verified: false,
}));

const out = { version: 1, imageBaseUrl: IMAGE_BASE, exercises };
writeFileSync(OUT, JSON.stringify(out));
console.log(`seed_exercises.json: ${exercises.length} exercicios -> ${OUT}`);

for (const i of [0, 100, 300, 500, 700]) {
    console.log(`  ${exercises[i].nameEn}  ->  ${exercises[i].namePt}`);
}
