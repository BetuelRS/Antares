/**
 * O vocabulário dos nutrientes: as chaves que podem existir, e o que cada uma quer dizer.
 *
 * Existe porque um nutriente escrito de duas maneiras é dois nutrientes. Ninguém dá por isso
 * — a app mostra os dois, cada um com metade dos alimentos, e a barra de cada um fica a meio
 * sem razão nenhuma. O `construir.mjs` chumba se um importador emitir uma chave que não esteja
 * declarada aqui, que é a única forma de isso não acontecer em silêncio.
 *
 * **As referências da EFSA não se escrevem aqui de cabeça.** Vêm do `seed_efsa_drv.csv`, que é
 * o ficheiro que a app lê, e há uma verificação a exigir que os dois digam o mesmo. Escrever
 * um valor de referência de memória é a forma mais rápida de a app passar a mentir sobre uma
 * meta diária — já aconteceu uma vez, com o zinco, ao escrever este ficheiro.
 *
 * **O `tagname` é o nome internacional do INFOODS**, e serve para ligar uma fonte nova sem
 * adivinhar correspondências — que foi o trabalho feito à mão para a CIQUAL e para a TCA. Duas
 * células estão vazias de propósito: são as que eu não sabia sem inventar. **Uma célula vazia
 * é honesta; um tagname errado é uma armadilha** para quem ligar a próxima fonte.
 */
import { readFileSync } from "node:fs";

/** As colunas, por ordem. Mudar a ordem aqui é mudar o ficheiro. */
const COLUNAS = ["chave", "tagname", "unidade", "grupo", "drv_homem", "drv_mulher", "limite", "nota"];

export function lerVocabulario(caminho) {
  const linhas = readFileSync(caminho, "utf8").split(/\r?\n/).filter((l) => l.trim());
  const cabecalho = linhas[0].split(",");
  if (cabecalho.join(",") !== COLUNAS.join(",")) {
    throw new Error(`o cabeçalho do vocabulário mudou: ${cabecalho.join(",")}`);
  }

  const porChave = new Map();
  for (const linha of linhas.slice(1)) {
    // A nota é a última coluna e pode ter vírgulas; o resto não pode.
    const partes = linha.split(",");
    const registo = {};
    COLUNAS.forEach((c, i) => {
      registo[c] = i === COLUNAS.length - 1 ? partes.slice(i).join(",") : partes[i];
    });
    if (porChave.has(registo.chave)) throw new Error(`chave repetida no vocabulário: ${registo.chave}`);
    porChave.set(registo.chave, registo);
  }
  return porChave;
}

/**
 * Compara o vocabulário com as referências que a app lê, e devolve o que não bate.
 *
 * São dois ficheiros com os mesmos números, e é isso que os faz poder discordar. Enquanto o
 * `seed_efsa_drv.csv` for o que a app lê, é ele que manda: aqui só se verifica que ninguém os
 * deixou a dizer coisas diferentes.
 */
export function conferirComAEfsa(vocabulario, caminhoDaEfsa) {
  const efsa = new Map();
  for (const linha of readFileSync(caminhoDaEfsa, "utf8").split(/\r?\n/).slice(1).filter((l) => l.trim())) {
    const [chave, homem, mulher] = linha.split(",");
    efsa.set(chave, { homem, mulher });
  }

  const desacordos = [];
  for (const [chave, e] of efsa) {
    const v = vocabulario.get(chave);
    if (!v) { desacordos.push(`${chave}: tem referência da EFSA e não está no vocabulário`); continue; }
    if (v.drv_homem !== e.homem || v.drv_mulher !== e.mulher) {
      desacordos.push(`${chave}: vocabulário diz ${v.drv_homem}/${v.drv_mulher}, EFSA diz ${e.homem}/${e.mulher}`);
    }
  }
  for (const [chave, v] of vocabulario) {
    if (v.drv_homem && !efsa.has(chave)) {
      desacordos.push(`${chave}: tem referência no vocabulário e a EFSA não a tem`);
    }
  }
  return desacordos;
}
