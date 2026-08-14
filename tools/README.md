# Importadores

Geram os ficheiros que a app semeia na primeira abertura. Correm-se à mão, quando há dados novos,
e o resultado é versionado — a app nunca corre nada disto.

Os conjuntos de dados em bruto **não estão no repositório**: são grandes, não são nossos, e
voltam a descarregar-se. O `.gitignore` exclui os diretórios `data/`. Este ficheiro é o que diz
de onde vêm.

| Importador | Lê | Escreve |
|---|---|---|
| `ciqual-importer/` | `data/alim.xml` e `data/compo.xml` — [CIQUAL, da ANSES](https://ciqual.anses.fr/) — mais `usda-source.json` para enriquecer | `seed_foods.json`, `seed_pt_micros.json` |
| `tca-importer/` | `data/insa_tca.xlsx` — [Tabela de Composição de Alimentos do INSA](https://portfir.insa.min-saude.pt/pt/) | `seed_foods_tca.json` |
| `exercise-importer/` | `data/exercises.json` — [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | `seed_exercises.json` |
| `food-curated/` | nada; são listas escritas à mão | `seed_foods_pt2.json`, `seed_foods_pt3.json` |
| `seed-generator/` | `data/FoodData_Central_sr_legacy_food_csv_2018-04/` — [USDA SR Legacy](https://fdc.nal.usda.gov/) | `seed_foods.json` |

**O `seed_foods.json` que a app traz hoje vem do `ciqual-importer`**, com a CIQUAL como base e o
USDA a preencher os micronutrientes em falta. O `seed-generator` é o caminho antigo, de quando o
catálogo era só americano; escreve o mesmo ficheiro e por isso não se correm os dois.

Para saber de que origem é cada alimento, o campo `origin` de cada registo di-lo — hoje são 3385
`CIQUAL` e 2940 `USDA`.

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
