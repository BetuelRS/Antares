# Importadores

Geram os ficheiros que a app semeia na primeira abertura. Correm-se à mão, quando há dados novos,
e o resultado é versionado — a app nunca corre nada disto.

Os conjuntos de dados em bruto **não estão no repositório**: são grandes, não são nossos, e
voltam a descarregar-se. O `.gitignore` exclui os diretórios `data/`. Este ficheiro é o que diz
de onde vêm.

| Importador | Lê | Escreve |
|---|---|---|
| `seed-generator/` | `data/FoodData_Central_sr_legacy_food_csv_2018-04/` — USDA FoodData Central, SR Legacy | `seed_foods.json` |
| `tca-importer/` | `data/insa_tca.xlsx` — Tabela de Composição de Alimentos do INSA | `seed_foods_tca.json` |
| `exercise-importer/` | `data/exercises.json` — [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | `seed_exercises.json` |
| `food-curated/` | nada; são listas escritas à mão | `seed_foods_pt2.json`, `seed_foods_pt3.json` |
| `ciqual-importer/` | os dados acima, já descarregados | `seed_pt_micros.json` e correções de nomes |

Tudo o que é escrito vai para `composeApp/src/commonMain/composeResources/files/`, e as licenças
de cada fonte estão em `composeApp/src/commonMain/composeResources/files/licenses`.

O `exercise-importer` guarda nomes de ficheiro de imagem, e não endereços: as imagens dos
exercícios vêm da free-exercise-db e a app monta o endereço a partir de uma base que sabe mudar
sem reinstalar o catálogo.

```bash
node tools/seed-generator/generate.mjs
```

O `SeederOrderTest` garante que nenhum semeador lê o ficheiro de seed antes de verificar a marca
que diz se já foi semeado — é o que impede a app de ler megabytes em todos os arranques.
