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

### As fontes, e porque não estão no git

```bash
node tools/descarregar-fontes.mjs
```

O repositório guarda o que se **deriva** das fontes, não uma cópia delas. A CIQUAL e a TCA
descarregam-se à mão dos respectivos portais; as tabelas do USDA têm endereço directo e o
comando acima vai buscá-las — a retenção de nutrientes, os rendimentos de confeção, e três
ficheiros do SR Legacy (categorias, porções e unidades).

Depois disso, `node tools/confecao/construir.mjs` e `node tools/catalogo/construir.mjs`
reproduzem o catálogo byte a byte.

### A confeção

Duas tabelas do USDA, ambas de domínio público, respondem à pergunta «e se for cozinhado?»:
**quanto peso sobra** e **quanto de cada nutriente sobrevive**. A conta é a do próprio USDA:

    nutriente por 100 g cozinhado = por 100 g cru × retenção ÷ rendimento

**A divisão pelo rendimento é a parte que se esquece.** Cozer espinafres perde vitamina C para
a água *e* perde água; contar só a primeira coisa dá um número mais errado do que não fazer
conta nenhuma, porque o que sobra fica mais concentrado.

A retenção cobre 270 preparações em 13 grupos; o rendimento só carne e aves. **Um método sem
rendimento medido não recebe um rendimento inventado** — a app pede o peso depois de
cozinhar, e essa medição ganha à tabela.

A família de confeção de cada alimento vem de quem a publica, e não de parecença de nome: a
árvore de subgrupos da CIQUAL, a categoria da USDA, a classificação FoodEx2 da TCA. A única
leitura de nome são as carnes da CIQUAL, que caem todas em «cozinhadas» e «cruas» sem dizerem
de que animal são. **Família nula quer dizer «não se cozinha isto»** — um pão já foi ao forno.

### O motor de qualidade

Cinco verificações, todas a comparar o alimento **consigo mesmo**. Não vão à fonte confirmar
nada — um alimento que se contradiz está errado independentemente do que a fonte diga.

| Verificação | O que pergunta |
|---|---|
| **Atwater** | a energia bate com os macros que a produzem? |
| **massa** | água e macros cabem em cem gramas? |
| **somas** | saturada, mono e poli cabem na gordura total? o açúcar cabe nos hidratos? |
| **escala** | está fora de escala no seu subgrupo? *(só o que vem da CIQUAL — é a única fonte com árvore de grupos)* |
| **discordância** | duas fontes dizem energias diferentes para o mesmo nome? |

**Duas gravidades, e a diferença não é de grau.** Uma `contradicao` é um número impossível, e
**chumba a construção** a menos que já esteja declarada em `qualidade.json` — como os desvios.
Uma `suspeita` é um número improvável, não chumba nada, e vai para a fila da oficina. A razão
é que a suspeita mede uma discordância entre métodos de medição: chumbar por isso era não
poder publicar até a ANSES corrigir a tabela dela.

Três armadilhas já pagas, e todas na mesma família — **somar duas vezes o que já lá está**:

- A **fibra** está fora dos hidratos na CIQUAL, que os publica disponíveis, e dentro deles na
  USDA e na TCA, que os publicam por diferença. Os **polióis** seguem a mesma regra: somá-los
  sempre levou os achados de 106 para 113. O Atwater faz as contas todas e fica pela mais
  próxima.
- O **álcool** está dentro da água sempre que a humidade foi medida por secagem. Pôs três
  vinhos da TCA a somarem mais de cem gramas.
- Um **estado de texto** — `"vestigios"`, `"<0.03"` — numa conta dá `NaN`, e `NaN > 100` é
  falso. O verificador passava a não acusar nada, sem dar erro. Catorze óleos e pães.

Os testes estão em `qualidade.test.mjs` e correm com o resto:

```bash
node --test "tools/**/*.test.mjs"
```

Provam que ele **não** acusa o que não devia. O contrário — que ele continua a falar — é do
`MotorDeQualidadeTest`, do lado do Kotlin, porque um verificador partido não dá erro: fica
calado, e isso lê-se como o catálogo ter melhorado.

### A oficina de curadoria

```bash
node tools/oficina/servidor.mjs      # → http://127.0.0.1:4173
```

Uma página local, um alimento de cada vez: os números, o que o motor de qualidade apanhou já
em palavras, o nome partido nos segmentos por que as fontes o escrevem, e os campos que se
decidem — nome, porção, líquido, verificado, tirar do catálogo. O botão escreve em
`tools/catalogo/correcoes.json`, que é por onde o oleoduto já aplicava o que os dezoito passos
do semeador tinham decidido.

**A ordem da fila é o que a torna útil.** Por quantas vezes o alimento foi registado: um que
se come todas as semanas vale mil que ninguém procura, e a curadoria só acontece se as
primeiras horas caírem no pão e não em cogumelos shiitake enlatados. O histórico sai do
telemóvel:

```bash
adb shell am force-stop com.antares.app
adb exec-out run-as com.antares.app cat databases/antares.db > extracao/antares.db
node tools/oficina/historico.mjs
```

Não é obrigatório — sem ele a fila cai para o número de achados. E **não entra no git**: são
só contagens, mas uma contagem por alimento diz o que a pessoa come, e o repositório é
público.

Duas decisões que valem a pena saber:

- **O servidor só escuta em `127.0.0.1`.** Escreve num ficheiro do repositório, e não há
  versão disto que deva estar ao alcance da rede local.
- **A função que decide é pura, e tem testes.** O `correcoes.json` tem 2 707 nomes juntados ao
  longo de meses, e uma escrita mal feita apaga-os sem dar erro — só se descobre na construção
  seguinte, quando o catálogo volta a ter nomes de laboratório em inglês. O servidor recusa
  ainda uma escrita que perca mais do que um nome.

O vocabulário dos segmentos — o que é uma base, o que é um estado, como se escreve cada um em
português — **é decisão do dono**. A ferramenta separa os segmentos e mostra quais é que ainda
estão em inglês; não os traduz.

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
