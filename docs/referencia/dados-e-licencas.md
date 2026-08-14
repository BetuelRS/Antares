# Dados e licenças

A app traz dentro dela um catálogo de alimentos e um de exercícios. Não são nossos, e cada um vem
com condições diferentes.

## O código

**GPL-3.0.** Ver [LICENSE](../../LICENSE).

Em resumo, e sem substituir o texto: podes usar, estudar, modificar e redistribuir. Se
distribuíres uma versão modificada, tens de a distribuir também sob a GPL-3.0 e disponibilizar o
código-fonte.

## Os dados dentro da app

| Fonte | O que dá | Licença | O que obriga |
|---|---|---|---|
| [INSA — Tabela de Composição de Alimentos](https://portfir.insa.min-saude.pt/pt/) | 1376 alimentos portugueses, com micronutrientes | **com direitos de autor** | a fonte tem de aparecer **visivelmente onde os dados são mostrados** |
| [USDA FoodData Central (SR Legacy)](https://fdc.nal.usda.gov/) | minerais, vitamina E, folato, selénio | domínio público (CC0) | atribuição pedida, não exigida |
| [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | catálogo e imagens de exercícios | Unlicense (domínio público) | nada |
| Inter, Space Grotesk | tipos de letra | SIL Open Font License | manter o aviso de licença |

As licenças dos tipos de letra estão em
`composeApp/src/commonMain/composeResources/files/licenses`.

### A obrigação do INSA, e como a app a cumpre

É a única fonte com condições a sério. A app cumpre em dois sítios:

1. **No ecrã de atribuições** (`composeApp/src/commonMain/kotlin/pt/antares/app/feature/settings/AttributionsScreen.kt`),
   com a citação completa exigida.
2. **Em cada alimento**, que mostra de onde vieram os seus valores.

Quem fizer *fork* e mudar isto fica em incumprimento. Não é uma formalidade do repositório: é uma
condição de uso dos dados.

## O que é consultado em linha

| Fonte | Quando | Redistribuído? |
|---|---|---|
| [Open Food Facts](https://world.openfoodfacts.org/) | ao ler um código de barras ou procurar um produto | não — só consultado |

A Open Food Facts exige que quem chama se identifique no `User-Agent`, com nome e contacto, sob
pena de bloquear. A app monta esse cabeçalho a partir da versão e de um endereço de contacto — ver
`composeApp/src/commonMain/kotlin/pt/antares/app/core/network/off/OffApi.kt`.

## Os ficheiros que a app carrega

Estão em `composeApp/src/commonMain/composeResources/files/`:

| Ficheiro | Origem |
|---|---|
| `seed_foods.json` | USDA, via `tools/seed-generator/` |
| `seed_foods_tca.json` | INSA, via `tools/tca-importer/` |
| `seed_foods_pt.json`, `seed_foods_pt2.json`, `seed_foods_pt3.json` | listas escritas à mão, via `tools/food-curated/` |
| `seed_pt_micros.json` | micronutrientes portugueses, via `tools/ciqual-importer/` |
| `seed_exercises.json` | free-exercise-db, via `tools/exercise-importer/` |
| `seed_efsa_drv.csv` | valores de referência da EFSA |

Os conjuntos em bruto de que partem **não estão no repositório** — são grandes e voltam a
descarregar-se. Ver [tools/README.md](../../tools/README.md).
