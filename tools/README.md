# Ferramentas

Constroem o que a app traz dentro dela. Correm-se à mão, quando há dados novos, e o resultado é
versionado — **a app nunca corre nada disto.**

Os conjuntos de dados em bruto **não estão no repositório**: são grandes, não são nossos, e voltam
a descarregar-se. O `.gitignore` exclui os diretórios `data/`. Este ficheiro é o que diz de onde
vêm.

## O catálogo de alimentos

```bash
node tools/catalogo/construir.mjs
```

Uma execução, um ficheiro: `composeApp/src/commonMain/composeResources/files/catalogo.json`.

Até à 2.3.0 eram cinco ficheiros semeados por ordem e treze correções que corriam no telemóvel a
cada arranque — dezoito passos que não podiam ser fundidos nem reordenados, e a que cada alimento
mal escrito acrescentava mais um. **Corrigir um alimento custava uma versão na Play Store.** Passa
a custar uma execução disto.

| Peça | O que faz |
|---|---|
| `catalogo/fontes/ciqual.mjs` | lê `alim.xml` e `compo.xml` da [CIQUAL, da ANSES](https://ciqual.anses.fr/) |
| `catalogo/fontes/usda.mjs` | lê `usda-source.json` — enriquece a CIQUAL e enche a cauda |
| `catalogo/fontes/tca.mjs` | lê `insa_tca.xlsx` da [Tabela do INSA](https://portfir.insa.min-saude.pt/pt/) |
| `catalogo/fontes/curados.mjs` | lê as listas portuguesas escritas à mão, em `catalogo/dados/` |
| `catalogo/correcoes.json` | os nomes que os dezoito passos corrigiram, para não regredirem |
| `catalogo/desvios.json` | o que a fonte declara e o catálogo não leva, com a razão de cada um |

**A construção é determinística**: duas execuções dão bytes idênticos. Nada de datas, e a ordem é
por código de caracteres e não por `localeCompare`, que depende da localização da máquina.

**A cobertura chumba.** Um alimento que a fonte declare e que não chegue ao ficheiro faz a
construção falhar. Para o aceitar, corre-se com `--aceitar-desvios`, que reescreve o
`desvios.json` — e o `git diff` mostra exactamente o que se está a aceitar perder.

**A versão sobe à mão**, no `construir.mjs` e no `FoodSeeder`, e há um teste-guarda a exigir que
as duas sejam a mesma. Sem isso, o catálogo novo viaja dentro do APK e não entra em telemóvel
nenhum.

### As correções, e porque estão num ficheiro

Cinco dos dezoito passos arrumaram nomes americanos ao longo de meses. Reconstruir das fontes
devolvia-lhes o nome de laboratório em inglês, e nada os tornaria a limpar. O
`extrair-do-telemovel.mjs` tirou-os de uma instalação com os dezoito passos corridos até ao fim,
e o `comparar-com-o-telemovel.mjs` verificou, alimento a alimento, que a reconstrução dá o mesmo
— **zero diferenças em 7 995 alimentos**, mais dezasseis que o importador antigo perdia.

Correm-se assim, com a compilação de depuração instalada e a app aberta uma vez:

```bash
adb shell am force-stop com.antares.app
adb exec-out run-as com.antares.app cat databases/antares.db > extracao/antares.db
node tools/catalogo/extrair-do-telemovel.mjs
node tools/catalogo/comparar-com-o-telemovel.mjs
```

## O resto

| Ferramenta | Lê | Escreve |
|---|---|---|
| `exercise-importer/` | `data/exercises.json` — [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | `seed_exercises.json` |
| `food-curated/` | nada; são listas escritas à mão | `catalogo/dados/seed_foods_pt2.json`, `…pt3.json` |
| `ciqual-importer/build-pt-micros.mjs` | o catálogo construído e `pt-micro-overrides.json` | `catalogo/dados/seed_pt_micros.json` |
| `ciqual-importer/analyse-pt.mjs`, `match-pt.mjs`, `find.mjs` | o catálogo construído | nada; são medições |

O `usda-source.json` é uma extração do [USDA SR Legacy](https://fdc.nal.usda.gov/), guardada tal
como está e **fora do git** por ter 4 MB. Traz também os 27 alimentos portugueses do
`pt-extras.json`, que está ao lado por ser a única cópia versionada deles.

O `exercise-importer` guarda nomes de ficheiro de imagem, e não endereços: as imagens vêm da
free-exercise-db e a app monta o endereço a partir de uma base que sabe mudar sem reinstalar o
catálogo.

**O `build-pt-micros.mjs` lê o catálogo que ele próprio ajuda a construir.** Correr às cegas
muda a tabela de micronutrientes portugueses — na 2.4.0, 123 entradas — e isso é uma decisão de
conteúdo, não um efeito lateral. Ler o `git diff` antes de aceitar.

As licenças de cada fonte estão em `composeApp/src/commonMain/composeResources/files/licenses`.
