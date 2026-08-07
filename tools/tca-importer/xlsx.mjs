
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';

function unzipEntry(zipPath, entry) {
  return execFileSync('unzip', ['-p', zipPath, entry], {
    maxBuffer: 256 * 1024 * 1024,
    encoding: 'utf8',
  });
}

function decodeEntities(s) {
  return s
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, d) => String.fromCharCode(+d))
    .replace(/&amp;/g, '&');
}

function sharedStrings(zipPath) {
  let xml;
  try {
    xml = unzipEntry(zipPath, 'xl/sharedStrings.xml');
  } catch {
    return [];
  }
  const out = [];

  for (const si of xml.split('<si>').slice(1)) {
    const partes = [...si.matchAll(/<t[^>]*>([\s\S]*?)<\/t>/g)].map((m) => m[1]);
    out.push(decodeEntities(partes.join('')));
  }
  return out;
}

function colIndex(ref) {
  const letras = ref.match(/^[A-Z]+/)[0];
  let n = 0;
  for (const ch of letras) n = n * 26 + (ch.charCodeAt(0) - 64);
  return n - 1;
}

export function readSheet(zipPath, sheetFile = 'xl/worksheets/sheet1.xml') {
  const strings = sharedStrings(zipPath);
  const xml = unzipEntry(zipPath, sheetFile);
  const linhas = [];
  for (const row of xml.split(/<row[ >]/).slice(1)) {
    const celulas = [];
    for (const m of row.matchAll(/<c r="([A-Z]+\d+)"([^>]*)>([\s\S]*?)<\/c>/g)) {
      const [, ref, attrs, corpo] = m;
      const tipo = (attrs.match(/t="([^"]+)"/) || [])[1];
      let valor;
      if (tipo === 's') {
        const i = +(corpo.match(/<v>(\d+)<\/v>/) || [])[1];
        valor = strings[i] ?? '';
      } else if (tipo === 'inlineStr') {
        valor = decodeEntities(
          [...corpo.matchAll(/<t[^>]*>([\s\S]*?)<\/t>/g)].map((x) => x[1]).join(''),
        );
      } else {
        valor = decodeEntities(((corpo.match(/<v>([\s\S]*?)<\/v>/) || [])[1] ?? '').trim());
      }
      celulas[colIndex(ref)] = valor;
    }

    linhas.push(Array.from(celulas, (v) => v ?? ''));
  }
  return linhas;
}

export function sheetNames(zipPath) {
  const xml = unzipEntry(zipPath, 'xl/workbook.xml');
  return [...xml.matchAll(/<sheet name="([^"]*)"/g)].map((m) => decodeEntities(m[1]));
}
