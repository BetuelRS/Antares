# Dados e licenças

A app traz dentro dela um catálogo de alimentos e um de exercícios. Não são nossos, e cada um vem
com condições diferentes.

## O código

**GPL-3.0.** Ver [LICENSE](../../LICENSE).

Em resumo, e sem substituir o texto: podes usar, estudar, modificar e redistribuir. Se
distribuíres uma versão modificada, tens de a distribuir também sob a GPL-3.0 e disponibilizar o
código-fonte.

## Os dados dentro da app

O catálogo de alimentos não vem de uma fonte: vem de quatro, sobrepostas por ordem de
autoridade. A francesa dá a base europeia, a americana enriquece-a com micronutrientes que ela
não mede, e a portuguesa manda em tudo o que é comida portuguesa.

| Fonte | O que dá | Licença | O que obriga |
|---|---|---|---|
| [CIQUAL 2025 · ANSES](https://ciqual.anses.fr/) (França) | **a base do catálogo** — 3329 alimentos europeus | [Licence Ouverte / Etalab 2.0](https://www.etalab.gouv.fr/licence-ouverte-open-licence/) | citar a fonte e a data da versão |
| [USDA FoodData Central (SR Legacy)](https://fdc.nal.usda.gov/) | 2944 alimentos, e os micronutrientes que a CIQUAL não mede | domínio público (CC0) | atribuição pedida, não exigida |
| [INSA — Tabela de Composição de Alimentos](https://portfir.insa.min-saude.pt/pt/) | 1372 alimentos portugueses, com nomes portugueses reais | **com direitos de autor** | a fonte tem de aparecer **visivelmente onde os dados são mostrados** |
| [EFSA](https://www.efsa.europa.eu/) | valores de referência dietéticos, para o painel de micronutrientes | dados públicos da autoridade europeia | citar a fonte |
| [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | catálogo e imagens de exercícios | Unlicense (domínio público) | nada |
| Inter, Space Grotesk | tipos de letra | SIL Open Font License | manter o aviso de licença |

As contagens acima foram tiradas do próprio `seed_foods.json`, pelo campo `origin`. As licenças
dos tipos de letra estão em `composeApp/src/commonMain/composeResources/files/licenses`.

**A app declara todas estas fontes no ecrã de atribuições**, em Definições → Atribuições e dados.
Esse ecrã — e não este documento — é o que cumpre as obrigações de referenciação.

### A obrigação do INSA, e como a app a cumpre

É a fonte com as condições mais apertadas. A app cumpre em dois sítios:

1. **No ecrã de atribuições**
   (`composeApp/src/commonMain/kotlin/pt/antares/app/feature/settings/AttributionsScreen.kt`),
   com a citação completa exigida, incluindo a versão e a data de consulta.
2. **Em cada alimento**, que mostra de onde vieram os seus valores.

Quem fizer *fork* e mudar isto fica em incumprimento. Não é uma formalidade do repositório: é uma
condição de uso dos dados.

## O que é consultado em linha

| Fonte | Quando | Licença | Redistribuído? |
|---|---|---|---|
| [Open Food Facts](https://world.openfoodfacts.org/) | ao ler um código de barras ou procurar um produto, e só com a pesquisa em linha ligada — ver o interruptor da 2.2.0 | [ODbL](https://opendatacommons.org/licenses/odbl/) | não — os produtos ficam só no telemóvel de quem os leu |

A Open Food Facts exige que quem chama se identifique no `User-Agent`, com nome e contacto, sob
pena de bloquear. A app monta esse cabeçalho a partir da versão e de um endereço de contacto — ver
`composeApp/src/commonMain/kotlin/pt/antares/app/core/network/off/OffApi.kt`.

## Os ficheiros que a app carrega

Estão em `composeApp/src/commonMain/composeResources/files/`:

| Ficheiro | Origem |
|---|---|
| `catalogo.json` | as quatro fontes já fundidas, via `tools/catalogo/construir.mjs` |
| `seed_exercises.json` | free-exercise-db, via `tools/exercise-importer/` |
| `seed_efsa_drv.csv` | valores de referência da EFSA |

Até à 2.3.0 eram cinco ficheiros de alimentos, semeados por ordem, mais treze correções que
corriam no telemóvel a cada arranque. Desde a 2.4.0 é um só, construído fora da app.

**O identificador diz a origem**, e é por ele que se conta: 3329 `ciqual-`, 2944 `usda-`,
1372 `tca-`, 274 `ptx` — os portugueses curados — e 13 `pt-`, os extras escritos à mão.
São 7932 alimentos.

## O vocabulário dos nutrientes

Cada nutriente tem **uma chave e uma só**, declarada em
[`tools/catalogo/vocabulario.csv`](../../tools/catalogo/vocabulario.csv): a chave, o nome
internacional do INFOODS, a unidade, o grupo, a referência da EFSA quando existe, e se essa
referência é uma meta a atingir ou um tecto a não passar.

São **42 chaves**, 40 em uso, e a mediana é de **20 por alimento**. As duas que não se usam —
o açúcar e a gordura saturada — vivem em coluna na linha do alimento, e a app junta-as ao mapa
ao ler.

**Um importador que emita uma chave não declarada chumba a construção.** É o que impede um
nutriente de passar a existir escrito de duas maneiras: dois nomes para a mesma coisa são dois
nutrientes, cada um com metade dos alimentos, e as barras de ambos ficam a meio sem razão
nenhuma.

Desde a v28, **o sódio e a fibra vivem só no vocabulário**. Tinham meta da EFSA e coluna
própria ao mesmo tempo, e em 29 alimentos os dois números discordavam — a coluna guardava um
inteiro arredondado e o mapa as casas decimais que a fonte publicou.

**Oitenta e três alimentos que a CIQUAL declara não entram**, e estão nomeados um a um em
[`tools/catalogo/desvios.json`](../../tools/catalogo/desvios.json) com a razão: a fonte não
determinou a energia nem um dos macronutrientes, e derivar um sem o outro seria inventar o
número. A construção chumba se aparecer um desvio que não esteja nessa lista.

Os conjuntos em bruto de que partem **não estão no repositório** — são grandes e voltam a
descarregar-se. Ver [tools/README.md](../../tools/README.md).
