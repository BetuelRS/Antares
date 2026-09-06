# Base de dados

Room, no telemóvel e mais lado nenhum. Esquema na **versão 40**, com **35 tabelas**, **37
migrações automáticas** e **duas escritas à mão** — a 26→27 e a 27→28.

> **Este cabeçalho esteve onze versões de esquema atrasado** — dizia v26, 30 tabelas e 25
> migrações — e nada o apanhou: o `DocumentationHonestyTest` verifica caminhos e a versão da
> app, não afirmações. Corrigido a 2026-08-31, outra vez a 2026-09-02, e **outra vez a
> 2026-09-03**.
>
> A correcção de 09-02 trocou um erro por outro: baixou as migrações automáticas de 39 para
> **35** e são **36** — a cadeia é 1→2 … 25→26, que são vinte e cinco, mais 28→29 … 38→39,
> que são onze. E as tabelas eram **34** e não 33: a contagem saltou o `db_info`, que é a
> única entidade cujo nome de classe não acaba em `Entity`. Contado com
> `grep -c "::class"` na lista de entidades do `AntaresDb.kt` e nos `tableName` do
> `composeApp/schemas/…/39.json`, que dão os dois o mesmo número.

Os esquemas exportados estão em `composeApp/schemas/`, um ficheiro JSON por versão. São eles que
permitem ao Room gerar as migrações e aos testes verificá-las.

## Regra para quem mexe nas entidades

**Uma coluna nova nasce anulável ou com valor por omissão.**

Quase todas as migrações são automáticas — as duas excepções estão em `Migracao26Para27.kt` e
`Migracao27Para28.kt`. Uma coluna obrigatória sem omissão obriga a escrever a migração à
mão, e sem ela a app rebenta ao abrir em cima de dados antigos.

## Como o esquema cresceu

Cada linha é uma versão do esquema e o que ela acrescentou. É o registo mais fiável da ordem em
que a app foi construída.

| Versão | Tabelas novas | Colunas novas |
|---|---|---|
| 1 | `db_info` | |
| 2 | `user_profile`, `weight_log`, `daily_target_override` | |
| 3 | `foods`, `foods_fts`, `food_log`, `water_log` | |
| 4 | `recipe`, `recipe_ingredient` | |
| 5 | `exercise_log` | |
| 6 | `exercise`, `routine`, `routine_item`, `workout_session`, `workout_set` | |
| 7 | `fasting_protocol`, `fasting_session` | |
| 8 | `run`, `track_point` | |
| 9 | `routine_schedule` | |
| 10 | `sync_meta` | |
| 11 | | `routine_schedule.deleted` |
| 12 | `coach_report` | |
| 13 | | `weight_log.source`, `weight_log.sourceRef` |
| 14 | | `foods.lastAmountG` |
| 15 | `meal_template`, `meal_template_item` | |
| 16 | | `isLiquid` em `foods`, `food_log`, `meal_template_item` |
| 17 | | `user_profile`: `goalWeightKg`, `bodyFatPct`, `bodyFatSource`, `waistCm`, `neckCm`, `hipCm` |
| 18 | `body_measurement_log` | `user_profile`: `bmrFormulaOverride`, `goalBodyFatPct`, `heightConfirmedEpochDay`, `trendWindowDays` |
| 19 | `goal_history`, `progress_photo`, `search_miss` | `body_measurement_log`: `armCm`, `thighCm`, `chestCm` |
| 20 | `cycle_log` | `user_profile.lifeStage` |
| 21 | **remove** `sync_meta` | |
| 22 | | `food_log.eatenAtMin` — a que horas se comeu |
| 23 | `food_nutrient` | |
| 24 | | **remove** `dirty` de 23 tabelas |
| 25 | | `recipe.servings` — quantas doses rende a receita |
| 26 | | **remove** `user_profile.energyUnit` |
| 27 | `food_marca` | |
| 28 | | **remove** `sodiumMg` e `fiberG`: passam a viver só no vocabulário |
| 29 | | `familia` — a família de confeção do alimento |
| 30 | | `porcoesJson` |
| 31 | | `metodo` — como o prato foi cozinhado |
| 32 | | `densidade` |
| 33 | | `imagemUrl` |
| 34 | | `photoPath` — a fotografia do prato |
| 35 | `recipe_step` | `texto`, `posicao` — os passos de preparação |
| 36 | | `microsOrigemJson` — a origem por nutriente |
| 37 | | `startedAtMin` — a que horas o exercício começou |
| 38 | `session_exercise_note` | `note` — a nota de um exercício **neste** treino |
| 39 | `exercise_load` | `bodyweightPercent`, e a coluna `workout_set.bodyweightKg` — quanto da carga veio do corpo |
| 40 | `exercise_marca` | `exerciseId` — a estrela de favorito, fora da linha de catálogo |

*Da v27 à v37 esta tabela foi reconstruída a 2026-08-31 comparando os esquemas exportados uns
com os outros, e não a partir do `CHANGELOG.md`: é o que a v27 tem e a v26 não, linha a linha.*

**Três destas não são acrescentos, e todas pela mesma razão: descreviam algo que a app não
faz.** O `sync_meta` guardava estado de sincronização e a `dirty` marcava linhas por enviar; a
app deixou de sincronizar — ver
[a decisão](../explicacao/decisoes/0001-a-app-nao-sincroniza.md). O `energyUnit` guardava uma
escolha entre kcal e kJ que nunca chegou a existir: não havia opção que a escrevesse nem um
sítio que a lesse.

Uma coluna que sai não parte as cópias de segurança antigas: o importador ignora campos que já
não conhece, e o `CopiaAntigaAindaAbreTest` guarda essa tolerância.

## Apagar não apaga

Quase todas as tabelas têm `deleted` e são apagadas por marcação, não por remoção.

Várias têm também um **índice único no dia** (`epochDay`), para não haver duas pesagens no mesmo
dia. As duas regras juntas produzem uma armadilha que não dá erro — explicada em
[Lápides e índices únicos](../explicacao/decisoes/0002-lapides-e-indices-unicos.md).

Quem escrever um caminho novo que grave por dia tem de usar os métodos `byDayForWrite`, que vêem
as lápides. O `TombstoneCollisionTest` defende isto.

## A coluna `dirty`, que já não existe

Marcava linhas por enviar para um servidor. Era escrita em 23 tabelas e lida em sítio nenhum —
sobrava da sincronização, que saiu na v21. **Apagada na v24**, com um `@DeleteColumn` por tabela.

Apagar uma coluna no SQLite obriga o Room a recriar a tabela e a copiar as linhas, e por isso o
`Migration2to3Test` abre uma base da v2 com o esquema de agora e conta as linhas do outro lado.

## O que é congelado e o que é vivo

| | |
|---|---|
| **Vivo** | uma receita: muda um ingrediente e todos os números dela mudam |
| **Congelado** | um registo do diário: o que comeste ontem não muda porque hoje corrigiste a ficha do alimento |

O `FoodLogEntity` guarda os macros **já multiplicados pela quantidade** e os micronutrientes **por
100 g**. É a diferença entre somar um dia e escalar uma ficha.

## Sementeira

O catálogo de alimentos e o de exercícios são semeados na primeira abertura, a partir de
`composeApp/src/commonMain/composeResources/files/`. A marca de que já foi feito vive na tabela
`db_info`.

O `SeederOrderTest` garante que nenhum semeador lê o ficheiro antes de verificar a marca — é o que
impede a app de ler megabytes em todos os arranques.
