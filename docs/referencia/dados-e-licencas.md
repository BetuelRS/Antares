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
| [CIQUAL 2025 · ANSES](https://ciqual.anses.fr/) (França) | **a base do catálogo** — 3385 alimentos europeus | [Licence Ouverte / Etalab 2.0](https://www.etalab.gouv.fr/licence-ouverte-open-licence/) | citar a fonte e a data da versão |
| [USDA FoodData Central (SR Legacy)](https://fdc.nal.usda.gov/) | 2940 alimentos, e os micronutrientes que a CIQUAL não mede | domínio público (CC0) | atribuição pedida, não exigida |
| [INSA — Tabela de Composição de Alimentos](https://portfir.insa.min-saude.pt/pt/) | 1376 alimentos portugueses, com nomes portugueses reais | **com direitos de autor** | a fonte tem de aparecer **visivelmente onde os dados são mostrados** |
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
| [Open Food Facts](https://world.openfoodfacts.org/) | ao ler um código de barras ou procurar um produto | [ODbL](https://opendatacommons.org/licenses/odbl/) | não — os produtos ficam só no telemóvel de quem os leu |

A Open Food Facts exige que quem chama se identifique no `User-Agent`, com nome e contacto, sob
pena de bloquear. A app monta esse cabeçalho a partir da versão e de um endereço de contacto — ver
`composeApp/src/commonMain/kotlin/pt/antares/app/core/network/off/OffApi.kt`.

## Os ficheiros que a app carrega

Estão em `composeApp/src/commonMain/composeResources/files/`:

| Ficheiro | Origem |
|---|---|
| `seed_foods.json` | **CIQUAL, enriquecida com USDA**, via `tools/ciqual-importer/` |
| `seed_foods_tca.json` | INSA, via `tools/tca-importer/` |
| `seed_foods_pt.json`, `seed_foods_pt2.json`, `seed_foods_pt3.json` | listas escritas à mão, via `tools/food-curated/` |
| `seed_pt_micros.json` | micronutrientes portugueses, via `tools/ciqual-importer/` |
| `seed_exercises.json` | free-exercise-db, via `tools/exercise-importer/` |
| `seed_efsa_drv.csv` | valores de referência da EFSA |

O `tools/seed-generator/` também escreve `seed_foods.json`, a partir do USDA em bruto. É o caminho
antigo, anterior à CIQUAL passar a ser a base — o `seed_foods.json` que a app traz hoje veio do
`ciqual-importer`, e vê-se pelo campo `origin` de cada registo.

Os conjuntos em bruto de que partem **não estão no repositório** — são grandes e voltam a
descarregar-se. Ver [tools/README.md](../../tools/README.md).
