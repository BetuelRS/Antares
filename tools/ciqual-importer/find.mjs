
import { catalogue, words } from "./match-pt.mjs";

const q = process.argv[2] ?? "";
const target = process.argv[3] ? Number(process.argv[3]) : null;
const qw = words(q);

const hits = catalogue
  .filter((e) => e.micros && Object.keys(e.micros).length)
  .filter((d) => {
    const dw = new Set(words(d.nameEn));
    return qw.every((w) => dw.has(w));
  })
  .sort((a, b) => a.nameEn.length - b.nameEn.length || a.id.localeCompare(b.id));

console.log(`${hits.length} dadores para "${q}"`);
for (const h of hits.slice(0, 25)) {
  const drift = target ? `  ${(((h.kcal - target) / target) * 100).toFixed(0)}%` : "";
  console.log(`  ${h.id}  ${h.kcal} kcal${drift}  ${h.nameEn}`);
}
