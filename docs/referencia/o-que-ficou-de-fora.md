# O que ficou de fora, versão a versão

Companheiro de [`a-divida-com-o-estudo.md`](a-divida-com-o-estudo.md). Aquele diz **o que
abrir**; este diz **o que já se sabe que falta**.

> **Este ficheiro foi escrito antes da auditoria e tinha erros.** A 2.6.0 e a 2.7.0 estavam
> descritas como buracos e eram decisões do dono, registadas no plano — comparei o código
> com o parágrafo da promessa de cada versão e nunca li o bloco «Respondidas a…» que está
> uns parágrafos abaixo. Está corrigido, e a lição ficou escrita por baixo das duas.
>
> **A Parte B esvaziou-se a 2026-08-28**, quando a rota foi lida por inteiro. O que a
> auditoria fechou está riscado daqui; o que ela abriu está na tabela do fim.

**Lê-se com uma desconfiança**, e esta continua a valer: um número aqui reconta-se antes de
se voltar a citar, e uma linha por ler nunca se cita como se fosse um achado.

| | O que é | O que vale |
|---|---|---|
| **Parte A** | medido no código, com o comando ao lado | é verdade hoje |
| **Parte B** | o que ainda não foi lido | é uma lista de trabalho, **não** um veredicto |

---

# Parte A — medido

## 2.0.3 · Os números certos

**Nada ficou de fora.** O registo no plano diz que entraram os cinco defeitos, e que **dois
números do próprio plano estavam errados**: eram 8 estilos de tipografia em falta e não 7,
e 176 usos a cair no Roboto e não 175. O achado «validar peso e altura» estava meio errado —
o peso já era validado.

Confirmado hoje: o `Type.kt` aplica `antaresDisplayFontFamily()` e `antaresBodyFontFamily()`
por estilo. A correção 1 das dez do estudo está fechada.

## 2.0.4 · As mentiras pequenas

**A instrução da cintura estava certa, e eu é que não a tinha visto.** Esta secção dizia que
ela «já não fala do umbigo e também não distingue sexo». Medi uma string — o
`bodycomp_measure_hint` — e concluí do que faltava nela. As instruções por sexo existem ao
lado dela: `bodycomp_waist_hint_male` diz «a cintura ao nível do umbigo»,
`bodycomp_waist_hint_female` diz «no ponto mais estreito do tronco, que costuma ficar acima
do umbigo», e o `bodycomp_neck_hint` diz «abaixo da laringe». É exactamente o que o
`estudo/motor/04-corpo.md` pede — e é lá que ele está, e não no `estudo/areas/16-perfil-corpo-e-metas.md`,
para onde a rota me mandava.

**O que faltava mesmo era o viés por sexo**, que o mesmo documento pede na linha seguinte e
o comentário do `NavyUncertainty` já prometia sem cumprir. Corrigido a 2026-08-29.

O `updateSet` está ligado (`WorkoutSessionScreen.kt` chama-o), e a administração saiu do menu.

## 2.1.0 · A cópia de segurança

**A promessa «fotografias nas regras de cópia automática» resolveu-se por outra via:** o
`allowBackup="false"` desliga a cópia da Google inteira, por decisão do dono depois de
problemas com ela, e o ZIP próprio leva as fotos de progresso.

**Mas o risco que o estudo nomeia voltou pela minha mão.** A correção 5 das dez chama-se
«o único dado irrecuperável é o único desprotegido». Na 2.17.0 pus a fotografia do prato
deliberadamente **fora** da cópia, com razão escrita — mil imagens por ano em cinco cópias
que rodam. A razão continua a parecer boa; a decisão foi tomada sem eu ter lido a página onde
o estudo argumenta o contrário.

→ **Reaberto a 2026-08-28, com o `estudo/dados/02-perder-tudo.md` e o
`estudo/propostas/00-o-custo-de-mudar.md` abertos.** A decisão mantém-se: a foto do prato
fica fora da cópia e continua a durar sessenta dias. O eixo do estudo é a **re-obtenção** —
«um peso pode voltar a medir-se, uma foto de há dois anos não» — e no ficheiro de imagem a
foto do prato está mesmo na mesma categoria da de progresso. O que a distingue é o custo:
três por dia dão perto de mil por ano, e em cinco cópias que rodam enchiam o telemóvel.

**O defeito não era a decisão. Era o silêncio.** O `estudo/dados/02-perder-tudo.md` dá 18 e 19 à robustez e à
longevidade da cópia e **12 à honestidade**, e escreve porquê: «a app protege-se bem e não
conta a ninguém como». Nenhuma linha da app dizia que a foto não ia na cópia, nem que se
apaga aos sessenta dias. O ecrã da cópia passa a dizê-lo, com o número tirado da constante
que manda na varredura.

**E o §4 do `estudo/dados/02-perder-tudo.md` está ultrapassado:** ele descreve a cópia automática do Android como
ligada e bem configurada, com as fotos de progresso de fora, e a correção como «duas linhas
em cada um dos dois ficheiros». Medido hoje: `allowBackup="false"`, e o `backup_rules.xml` e
o `data_extraction_rules.xml` **não existem**. A correção 5 das dez não tem ficheiros onde
ser feita. O caminho escolhido foi outro — e é o item «Segundo» do próprio documento, que
ele punha depois.

## 2.2.0 · O que sai daqui

**Ficou de fora, e por decisão escrita: a frase do arranque.** O registo no plano di-lo:
«a frase do arranque fica para a 2.40.0, por decisão do dono», com a consequência assumida —
**o primeiro ecrã continua a dizer «Nada sai do telemóvel» até lá**, e a app contacta a Open
Food Facts.

Não é uma falha minha; é uma dívida com data. Tem casa: a 2.40.0.

## 2.3.0 · Uma linguagem só

**Nada ficou de fora segundo o registo** — entraram as seis peças e a migração foi toda de
uma vez. O esboço `20-sistema-de-desenho` **não foi aberto**, e é a versão que ele desenha.

## 2.6.0 · O vocabulário — **eu é que estava errado**

**Esta secção dizia «o maior buraco medido». Não era um buraco: era uma decisão do dono,
tomada a 2026-08-22 e escrita no registo da versão, no plano.**

Escrevi-a a comparar o código com o **parágrafo da promessa** da 2.6.0 e nunca li o bloco
**«Respondidas a…»** que está por baixo dele. É o mesmo erro que esta pasta inteira existe
para corrigir, cometido dentro do documento que a devia corrigir — trabalhar de um resumo,
neste caso de meio registo.

**O que o registo diz, e é o que vale:**

- A regra dada pelo dono foi «só os que se conseguem interpretar».
- **Nenhum nutriente novo entra no catálogo.** Contaram-se as fontes: biotina, crómio,
  molibdénio, flúor e cafeína não apareciam na CIQUAL, na TCA nem no USDA. Declará-los dava
  cinco chaves eternamente vazias.
- O **sal** é o sódio ×2,5 e a **energia em kJ** são as kcal ×4,184. Não são medições novas,
  são escalas de números que a app já tem, e entram como conversões no ecrã do alimento.
- A **frutose** só existe na CIQUAL e não tem referência da EFSA: fica declarada de fora.

**Confirmado no código a 2026-08-28:** o `FoodDetailViewModel` tem `previewKj` e
`previewSalG`, com `KJ_POR_KCAL = 4.184` e `SAL_POR_SODIO = 2.5`, e o ecrã do alimento
mostra os dois ao lado do rótulo. Dois dos oito estão feitos, e a tabela que aqui estava
dizia «não» aos dois.

O `Nutrients.kt` declarar 42 chaves é o que o registo prevê — o contador não se moveu porque
foi decidido que não se movia.

### O que **é** novo, e é para o dono decidir

A contagem de 2026-08-22 foi feita contra o `usda-source.json`, que é a extracção antiga e
traz **dezoito** chaves de micronutriente. Desde então o `usda-completo.mjs` passou a ler o
`food_nutrient.csv` do SR Legacy por inteiro, e esse ficheiro está no repositório.

Contado nele a 2026-08-28:

| Nutriente | Identificador USDA | Alimentos com valor |
|---|---|---:|
| **cafeína** | 1057 | **5 215** |
| **flúor** | 1099 | **538** |
| frutose | 1012 | 1 745 — e 1 836 na CIQUAL (código 32210) |
| crómio | 1096 | **0** |
| molibdénio | 1102 | **0** |
| biotina | 1176 | **0** |

Os três que o registo dava por inexistentes e continuam inexistentes: crómio, molibdénio,
biotina. A decisão sobre esses está certa e não se reabre.

**A cafeína e o flúor são outra coisa.** A premissa em que a decisão assentou — «não existem
em fonte nenhuma» — deixou de ser verdade para os dois, não porque a decisão fosse má, mas
porque a fonte que a app lê mudou. A cafeína em 5 215 alimentos é a que interessa: é um
estimulante que se segue, e é dela que se faz a pergunta «quanto café é que eu bebi?».

**Não se acrescenta nada por iniciativa minha** (A1). O que aqui fica é a premissa medida
outra vez, para o dono decidir se a quer reabrir.

## 2.7.0 · A ausência tipada — **também não era um buraco**

Mesma correcção, mesma causa. Esta secção dizia que faltavam dois dos seis estados do
EuroFIR. **Faltam de propósito**, por decisão de 2026-08-23 escrita no registo da versão:

> «Três estados, não seis. Medido, abaixo do limite de deteção, vestígios — mais a ausência,
> que é a falta da célula. "Não se aplica" e "assumido zero" não aparecem em fonte nenhuma
> das três: declará-los era ter estados que nunca acontecem.»

E está escrito outra vez no código, no KDoc do
[`EstadoDeNutriente.kt`](../../composeApp/src/commonMain/kotlin/pt/antares/app/core/nutrition/EstadoDeNutriente.kt),
onde eu o teria lido se tivesse aberto o ficheiro antes de escrever a tabela.

| Estado | Como está |
|---|---|
| `medido` | o número nu — é o formato compacto que o plano descreve |
| `nao_medido` | a ausência da chave |
| `vestigios` | `EstadoDeNutriente.Vestigios` |
| `abaixo_do_limite` | `EstadoDeNutriente.AbaixoDoLimite(limite)` |
| `nao_se_aplica` | **fora, por decisão** — nenhuma das três fontes o emite |
| `assumido_zero` | **fora, por decisão** — idem |

Se um dia entrar uma fonte que os emita, entram com ela. Um estado que nunca acontece é uma
ramificação que ninguém consegue testar.

## 2.6.0 e 2.7.0 · o que isto ensina sobre este documento

As duas secções acima estiveram erradas durante o tempo em que este ficheiro existiu, e
erradas do mesmo modo: **medi o código contra a promessa da versão e não contra o registo
dela.** O registo está no mesmo ficheiro, uns parágrafos abaixo, e responde exactamente à
pergunta que eu estava a fazer.

A regra que daqui sai, e que vale para quem ler isto a seguir:

> **Uma versão do plano tem duas partes: o que ela prometeu e o que ficou decidido.** Ler só
> a primeira produz uma lista de buracos que não existem — e uma lista assim é pior do que
> não haver lista nenhuma, porque manda trabalhar naquilo que já foi decidido não fazer.

## O bloco D contra o `estudo/dados/04-as-fontes-de-dados.md` — achado novo, a 2026-08-28

Abertos: `estudo/dados/04-as-fontes-de-dados.md`, `estudo/propostas/02-o-catalogo.md`.

O documento dá **10 / 20** ao catálogo e a frase dele é «os dados valem 15, o sistema que os
mantém vale 5». Quase tudo o que ele diagnostica foi fechado pelo bloco D: o oleoduto fora
da app, determinístico e a chumbar quando perde um alimento que a fonte declara (§2), o
motor de qualidade (§6), a oficina (§10), o catálogo descarregável (§5), as fusões (§7), a
confeção com rendimentos e retenções (§7). Os 99 alimentos do CIQUAL que se perdiam já não
se perdem.

**O que sobrevive dele é a §3, «o mesmo facto em dois sítios» — e sobrevive meio.**

A queixa original era a fibra: estava na coluna para o CIQUAL e o USDA e dentro do JSON de
micros só para os 1 376 do INSA, e a `food_nutrient` — que responde ao «ricos em» —
constrói-se só do JSON. **Isso está resolvido**: a fibra e o sódio vivem hoje só nos micros,
e as colunas deles nem existem na entidade. Medido: fibra em 7 676 alimentos, sódio em 7 609.

**O que não está:** os açúcares e a gordura saturada ficaram só na coluna. Medido no
catálogo construído — `sugarsG` em 6 999 alimentos e `satFatG` em 7 374, e **zero** dos dois
dentro dos micros. A `food_nutrient` tem zero linhas de açúcares e zero de saturados em
7 932 alimentos.

**Não é um defeito visível hoje:** nenhum dos dois tem referência da EFSA, e o ecrã do «rico
em» só oferece as 25 chaves que têm. É uma armadilha — qualquer coisa futura que leia a
`food_nutrient` à procura de açúcares encontra nada, sem erro nenhum.

**Qual das duas metades ganha é decisão do dono**, e tem custo permanente: são 81 usos em
Kotlin e nas ferramentas, duas migrações de esquema e o mapeador da Open Food Facts, onde os
açúcares e os saturados são campos de rótulo a sério. O que se fez a 2026-08-28 foi tornar a
armadilha barulhenta: o `tools/catalogo/onde-vive-o-nutriente.test.mjs` escreve onde cada um
vive hoje e chumba quando isso mudar por acidente.

## 2.8.0 · O catálogo que se atualiza sozinho

**Nada medido em falta.** Descarrega, verifica por hash, só avança, é atómico.

## Os números mortos do bloco D · 2.9.0 a 2.15.0

O conteúdo saiu na 2.7.0 e na 2.8.0, ou não se lança por ser ferramenta. Medido hoje:

| Número | Prometia | Estado medido |
|---|---|---|
| 2.9.0 | motor de qualidade | **feito** — no `construir.mjs`, 12 contradições e 252 suspeitas |
| 2.10.0 | a oficina de curadoria | **feito** — `tools/oficina/servidor.mjs`, fila ordenada por uso |
| 2.11.0 | a confeção | **feito** — rendimentos e retenções, com o cartão «e se for cozinhado?» |
| 2.12.0 | 81 colisões arbitradas | **feito e ultrapassado** — zero colisões, 97 fusões, 24 nomes desambiguados |
| 2.13.0 | 3 385 nomes em inglês | **a meio** — 2 131 traduzidos, **2 909 ainda em inglês** |
| 2.14.0 | as porções, de 4,5 % | **a meio** — **26,3 %**, 2 090 de 7 932 |
| 2.15.0 | a comida e a incerteza | **feito** — `IncertezaDaComida.kt`, erro relativo por origem, propagado |

**As duas «a meio» não são falhas: são trabalho de meses**, e o estudo di-lo. O que importa
é que ninguém as leia como fechadas.

**Por verificar na 2.14.0:** o plano nomeia quatro fontes de porção. A **densidade** entrou na
2.8.0 e o **histórico** já existia no `UsualPortion.kt`. O **FNDDS** do USDA e o
`porcoes.csv` à mão — não verifiquei se existem. Abrir o `estudo/dados/04-as-fontes-de-dados.md`.

## 2.16.0 · Abrir no que se come

**Releitura a 2026-08-28.** Abertos: `estudo/areas/03-adicionar-comida.md`,
`estudo/esbocos/03-adicionar-comida.html`.

Das oito coisas que a área diz estarem mal, **três já estavam fechadas**: a voz vai para a
AI (2.17.0), os chips de sugestão deixaram de desaparecer quando ficam úteis, e a pesquisa
ordena pela história — marcado primeiro, usado há pouco a seguir. As miniaturas dos produtos
da Open Food Facts também lá estão.

**Fechadas a 2026-08-28:** a duplicação das refeições guardadas em dois sítios, que o estudo
nomeia em «o que é inútil» · o estado vazio que dizia «escreve pelo menos 2 letras» a quem
nunca registou nada · o aviso «estimado» que não levava a lado nenhum · a colher de sopa a
aparecer em todos os alimentos.

**Fica em aberto, e é decisão do dono:** a composição dos três separadores. O estudo propõe
*Tudo · Favoritos · Meus*, com as refeições como secção; a app tem *Procurar · Meus ·
Refeições*, com os favoritos como secção. São três de qualquer maneira — que é a queixa do
estudo, «seis é demasiado» — e trocá-los seria reorganizar a navegação uma terceira vez na
mesma semana.

## 2.17.0 · A revisão da IA

**Nada em falta** contra as seis promessas. Duas notas:

- O `estudo/areas/04-comida-por-ia.md` **não foi aberto**, e esta versão não cita esboço —
  é o documento de área que decide.
- A fotografia do prato ficou fora da cópia de segurança. Ver a 2.1.0 acima.

## 2.19.0 · O exercício avulso

**Construída com o estudo aberto**, e por isso esta entrada é curta: o que ficou de fora
ficou por decisão, não por omissão. Ver as respostas de abertura no plano.

- **A intensidade não entrou** — não porque não valha, mas porque **já lá está**: 21 dos 90
  nomes do `seed_mets.csv` trazem o ritmo ou a velocidade entre parênteses, e a corrida e a
  bicicleta têm cinco graus cada. O «talvez» da área 13 fica respondido com «não».
- **A importação automática do Health Connect não entrou.** Continua onde o
  `estudo/sistema/04-integracoes.md` a põe — «baixo · sim» —, e pede número próprio.
- **Três premissas do plano caíram na medição C1**, e estão escritas lá com o sítio no
  código: o id da atividade já era gravado, o «−5 sem mínimo» não existia, e o catálogo tem
  90 atividades e não as ~150 que o cabeçalho da área 13 diz. **A área 13 continua a dizer
  ~150** — é um número do estudo, e o estudo não se reescreve a partir do código.
- **O que a área 13 propõe e continua aberto:** o chip «Todas», que está sempre lá por ser o
  estado por omissão, e o `MET 7,0` em cada linha da lista, que o documento diz ser ruído
  fora do detalhe. Nenhum dos dois estava no conteúdo desta versão — **ganharam a 2.19.1**, e
  os favoritos e a importação de saúde em fundo ganharam a 2.49.0 e a 2.50.0.
- **Uma proposta do estudo que se mediu e não se aguenta.** O `estudo/transversal/03-acessibilidade.md` §4 pede *«testes
  a 200 % de escala de letra (Robolectric com `fontScale`) — custo baixo, vale sim»*.
  Escreveu-se, e **passa também sobre o código partido**: o Robolectric mede o texto «15» a
  três pixels, e sem fontes a sério nada transborda. Provado a repor a forma partida de
  propósito. O que entrou no lugar foi a exigência da razão escrita ao lado de cada largura
  fixa, no mesmo molde do `contentDescription = null`. **Medir a sério pede um teste
  instrumentado**, e a app não tem `androidTest` nenhum: seria o primeiro.

## 2.18.0 · 2.18.1 · As minhas refeições

**A única versão onde li o estudo — depois de a lançar.** Do esboço
`estudo/esbocos/05-receitas-e-modelos.html`:

| O esboço propõe | O que está construído |
|---|---|
| **«Uma lista só»**, com a origem escrita na linha | duas secções com títulos separados |
| **um sítio próprio**, alcançável do «Eu» e do diário — não dentro da pesquisa | ficou na pesquisa, com atalho no diário |
| multiplicador **em chips** ×0,5 ×1 ×1,5 ×2 | campo de texto |
| a linha diz a origem: «guardada do diário» | a linha diz a refeição do dia |
| **editar um modelo** — está na tabela de problemas dele | não construído |
| o aviso do rendimento **com a tolerância e o tom do aviso do rótulo** | envelope de métodos, inventado por mim |

E o argumento que dei ao dono — «receita deixa de ser palavra errada assim que tiver passos»
— o esboço contradiz: diz que faltam **passos, tempo e fotografia**, e avisa que isso é «uma
app dentro da app». Construí um terço e apresentei o assunto como fechado.

**Aberto a 2026-08-28**, e as seis divergências acima foram fechadas menos uma: a lista
passou a ser uma só com a origem escrita na linha, o multiplicador passou a chips, editar um
modelo passou a existir — mudar o nome e tirar um item —, e o aviso do rendimento ganhou a
rede que lhe faltava.

**O sítio próprio não se construiu, e a razão está no `estudo/propostas/00-o-custo-de-mudar.md`:** os trinta que o
estudo defende não incluem a área 05, e o sítio próprio é o item caro dela. Fica na tabela
do fim, para o dono.

**Dois defeitos concretos da área, que a 2.18.0 não tinha fechado, fecharam agora:** o
`templateApplied` era escrito por quem regista vários alimentos marcados — que não tem
modelo nenhum — e o nome mentia para reaproveitar o caminho de saída; e o aviso do peso
final não existia de todo para receitas de ingredientes sem família de confeção.

**E o aviso do rendimento não pôde seguir o estudo à letra.** Ele pede «a mesma tolerância
do aviso do rótulo», que são 10 %. Medido: a tabela de confeção publica rendimentos de 0,39
a 0,82, portanto perder 40 % do peso a cozinhar é vulgar e ±10 % acusava metade dos
estufados — o próprio exemplo do estudo, 500 g em 1 200 g de ingredientes, dá 0,42 e é
fisicamente possível. O chão passou a sair da própria tabela.

**Sobre os passos de preparação:** a área 05 diz que eles só entram «se o dono quiser que
receita queira mesmo dizer receita», e chama à alternativa «a decisão mais barata e
provavelmente a mais certa». O dono quis, e disse-o. A condição que o estudo põe está
cumprida — o que não estava é eu saber que a estava a pôr.

---

# Parte B — vazia

Era a lista dos documentos que decidem cada versão já publicada e que nunca tinham sido
abertos. **Ficou vazia a 2026-08-28.**

Foram lidos, e as divergências de cada um estão no registo da versão que lhes toca, dentro do
`estudo/PLANO-DE-PRODUCAO.md`:

`estudo/propostas/00-o-custo-de-mudar.md` · `estudo/dados/01-o-que-sai-do-telemovel.md` ·
`estudo/dados/02-perder-tudo.md` · `estudo/dados/03-sincronizacao-caseira.md` ·
`estudo/dados/04-as-fontes-de-dados.md` · `estudo/propostas/02-o-catalogo.md` ·
`estudo/areas/03-adicionar-comida.md` · `estudo/areas/04-comida-por-ia.md` ·
`estudo/areas/05-receitas-e-modelos.md` · `estudo/areas/20-navegacao-e-sistema-de-desenho.md` ·
`estudo/motor/01-metabolismo-e-metas.md` · `estudo/motor/03-peso-tendencia-e-projecao.md` ·
`estudo/motor/04-corpo.md` · `estudo/sistema/02-servidor-e-custo.md` ·
`estudo/transversal/02-robustez.md` · `estudo/transversal/04-longevidade.md` · e os esboços
`03`, `05`, `20` e `22`.

**Isto não quer dizer que o estudo esteja lido.** Ficam por abrir os documentos das áreas que
a app ainda não tocou — treino, corrida, jejum, progresso, arranque —, e esses abrem-se
quando as versões deles chegarem. A rota está em
[`a-divida-com-o-estudo.md`](a-divida-com-o-estudo.md).

**O `estudo/motor/04-corpo.md` não estava nesta tabela e devia estar.** A instrução da
cintura por sexo foi encaminhada para o `estudo/areas/16-perfil-corpo-e-metas.md`, onde não
está — a rota mandava-me para o documento errado, e foi só ao ler que apareceu. Uma rota
também erra.

---

---

# A comparação com os esboços, feita à vista — 2026-08-29

Os esboços foram desenhados no navegador e postos ao lado do emulador, ecrã a ecrã. As
imagens ficaram numa pasta **Antares-provas**, ao lado do repositório e fora dele: são
capturas de ecrã, e capturas de ecrã envelhecem sozinhas dentro do git. **Isto não é uma
leitura do código — é o que aparece no telemóvel.**

## Esboço 05 · «As minhas refeições»

| O esboço desenha | A app faz | |
|---|---|:--:|
| Uma lista só, as duas origens misturadas | uma lista só | ✅ |
| `Almoço de sempre · 4 itens · 640 kcal · guardada do diário` | `Segunda · 1 item · 525 kcal · saved from the diary` | ✅ |
| `Bacalhau à Gomes de Sá · 7 ingredientes · 512 kcal/dose · 4 doses` | `Bacalhau a Gomes de Sa · 2 ingredients · 47 kcal/serving · 4 servings` | ✅ |
| Seta `›` em cada linha | seta em cada linha | ✅ |
| Ao abrir: nome, chips ×0,5 ×1 ×1,5 ×2, itens, registar | igual, com o total ao lado dos chips | ✅ |
| **Um ecrã próprio**, com «As minhas refeições» no cabeçalho | um separador dentro de «Adicionar comida» | ❌ |
| **Uma caixa de procura dentro da lista** | não existe — a caixa de cima procura alimentos | ❌ |
| Um quadrado de imagem à esquerda de cada linha | sem miniatura | ❌ |

**Cinco de oito.** As três que faltam são de arrumação, não de conteúdo: a linha diz tudo o
que o esboço quer que ela diga.

## Esboço 03 · o ecrã de abertura

O esboço numera seis propostas para a secção 1. Contadas contra o que corre:

| # | O que o esboço pede | | |
|---|---|:--:|---|
| 1 | Três separadores em vez de seis | ⚠️ | são três, mas outros: o esboço quer *Tudo · Favoritos · Meus*, a app tem *Procurar · Meus · Refeições* |
| 2 | Abrir no que interessa: **as tuas refeições**, o que comes mais, os recentes | ⚠️ | dois dos três. **As refeições saíram do ecrã de abertura a 2026-08-29, por minha mão** |
| 3 | A porção habitual na linha — «180 g habituais» | ❌ | a linha mostra a porção da fonte, não a habitual |
| 4 | Miniaturas onde a Open Food Facts as tem | ✅ | |
| 5 | Criar alimento sai do botão flutuante e passa a linha no fim dos resultados | ❌ | continua no botão flutuante |
| 6 | A refeição e o dia no cabeçalho — «Almoço · hoje» | ❌ | diz «Adicionar comida» |

**Uma feita, duas a meio, três por fazer.**

### O que a comparação apanhou, e a leitura do código não tinha apanhado

**A linha 2 é um erro meu, e é de hoje.** A área 03 diz, em «o que é inútil», que o separador
«Modelos» duplica a secção «As tuas refeições» do ecrã de abertura. Eu li isso como «há duas
portas, fecha uma» e fechei **a secção**, ficando com o separador. O esboço mostra que a
escolha do estudo é a outra: o ecrã de abertura tem *AS TUAS REFEIÇÕES* em primeiro lugar, e
não há separador nenhum para elas — porque o ponto da área 03 inteira é «abrir no que tu
comes», sem um toque pelo meio.

Ficou registado como divergência deliberada, e não era: era eu a escolher a porta errada
por ter lido só a queixa e não o desenho.

**Não se corrige agora**, e a razão é a mesma que já vale para o resto: mexer na navegação
uma terceira vez na mesma sessão, sem margem para a correr no aparelho de ponta a ponta, é
como se apanhou o defeito da 2.17.0. Fica em cima da mesa, com a imagem ao lado.

## Esboço 03 · secções 2 e 3

**As cinco propostas da revisão da AI estão feitas** — campo de gramas escrevível, tocar no
nome abre a troca, «+ acrescentar», «guardar também como refeição», e o aviso legal só na
revisão. **A voz vai para o interpretador** e não para a pesquisa, que o esboço chama «o
defeito mais caro da app».



---

# Segunda passagem: seguir o esboço à risca — 2026-08-29

A comparação de cima acusou seis pontos por fazer, e um erro meu. Foram todos feitos, e
verificados a correr no emulador. A prova está em **Antares-provas**, com o «depois» ao lado
do «antes».

## Esboço 03 · secção 1, as seis propostas

| # | O que o esboço pede | Estado |
|---|---|:--:|
| 1 | Três separadores — **Tudo · Favoritos · Meus** | ✅ eram *Procurar · Meus · Refeições* |
| 2 | Abrir nas **tuas refeições**, no que comes mais, nos recentes — por essa ordem | ✅ |
| 3 | A porção habitual na linha — «180 g habituais» | ✅ mostra «300 g usual» a partir de três registos |
| 4 | Miniaturas onde a Open Food Facts as tem | ✅ já estava |
| 5 | Criar alimento sai do botão flutuante e passa a linha no fim, com o nome escrito | ✅ «Not there? Create queijo» |
| 6 | A refeição e o dia no cabeçalho | ✅ «Lunch · today» |

**As seis.** O botão flutuante deixou de existir para criar — só regista o que está marcado,
que é a acção frequente. Era ele a oferecer a mais rara no sítio mais visível, e o estudo
apanhou-o.

## Esboço 05 · o sítio próprio

| O esboço pede | Estado |
|---|:--:|
| «As minhas refeições» como ecrã, alcançável do «Eu» e do diário | ✅ entrada nova no «Eu»; do diário chega-se pelo «Saved meal» |
| Cabeçalho com seta de voltar e **＋** | ✅ |
| Caixa de procura dentro da lista | ✅ filtra por nome |
| A lista, com a origem escrita na linha e a seta | ✅ |

**A lista aparece nos dois sítios, e não é a duplicação que o estudo condena.** Dentro do
«Tudo» é para registar depressa o que já se montou; no ecrã próprio é para tratar delas —
mudar o nome, apagar, montar uma nova. O esboço 03 desenha o primeiro e o 05 desenha o
segundo, e os dois estão certos.

## O que fica de fora, e é uma coisa só

**A miniatura quadrada em cada linha.** O desenho do esboço põe um quadrado à esquerda de
todas as linhas; a legenda numerada dele diz «miniaturas **onde a Open Food Facts as tem**».
Segui a legenda: o catálogo que a app traz não tem fotografia de nada, e um quadrado vazio em
sete mil linhas é ruído, não informação. **É a única coisa em que o desenho e a sua própria
nota não dizem o mesmo**, e fica escrito aqui para ser decidido em vez de esquecido.



---

# Terceira passagem: o esboço ao lado da app — 2026-08-30

As duas primeiras compararam o esboço com o **código**, e a segunda com o ecrã a olho. Esta
pôs os dois lado a lado numa página — `estudo/comparacao/index.html` —, com o desenho do
esboço à esquerda e a captura à direita, e um veredicto por proposta numerada.

**A página não cita o esboço: usa o HTML dele.** O desenho é tirado do ficheiro original e
desenhado com a mesma folha de estilo, e por isso não pode divergir do que o esboço diz. Uma
tabela escrita à mão envelhece sozinha; esta não.

**44 propostas conferidas:** 30 batem, 6 batem com desvio escrito, 1 não se faz com a razão
escrita, e 7 são de versões que ainda não abriram — o Hoje e o Diário, onde só o cartão de
destaque entrou.

## O que esta passagem apanhou, e as outras duas não

**A colher de sopa ainda aparece num sólido sem porção nomeada.** A área 03 põe-na em «o que
é inútil» com o argumento «15 g de azeite faz sentido, 15 g de bife não». A correcção da
2.16.0 escondeu-a onde há porção nomeada — e o «Frango inteiro sem pele, cru» não tem
nenhuma, por isso continua a oferecer uma colher de sopa de frango cru.

**A regra resolvia metade do que o estudo pede, e passou a resolver as duas.** É agora *ser
líquido* — a pergunta que a colher faz. Custa a farinha e o açúcar, que são sólidos e se
medem à colher, e esses ganham-na no dia em que alguém lhes escrever uma porção nomeada, que
é o sítio certo para ela. A regra saiu do ecrã para uma função com teste-guarda, porque já
tinha sido corrigida duas vezes.

*As duas leituras anteriores não a viam porque as duas olhavam para a regra escrita no
código — e a regra estava cumprida. O que falhava era a regra, não o código.*

## E uma regressão da 2.17.0, apanhada pela mesma via

**Os ±10 da revisão da AI tinham desaparecido.** O esboço 03 escreve «campo de gramas
escrevível, **com os ±10 como acessório**»: o campo substitui a régua para saltos grandes, e
os botões continuam a servir o ajuste de uma mão, que é como aquela folha se usa. A 2.17.0
pôs o campo e tirou os botões — foi longe de mais na correcção. **Repostos**, a trabalhar
sobre o texto do campo para os dois nunca discordarem.

# O que fica em aberto, junto

Cada linha precisa de autorização para virar trabalho (A1).

| O quê | Onde nasceu | Casa provável |
|---|---|---|
| **A cafeína e o flúor** — a premissa da decisão de 2.6.0 mudou: 5 215 e 538 alimentos no `food_nutrient.csv` que o oleoduto passou a ler | 2.6.0, medido outra vez a 2026-08-28 | só se o dono reabrir a decisão |
| **Onde vivem os açúcares e os saturados** — só na coluna, e a `food_nutrient` tem zero linhas dos dois em 7 932 alimentos | bloco D, `estudo/dados/04` §3 | decisão com custo permanente: 81 usos, duas migrações, o mapeador da OFF |
| **O `FoodRow` com `Card` + `ListItem` do Material** — o último resto das duas linguagens de cartão | 2.3.0, `estudo/areas/20` | precisa de uma corrida no aparelho; é a lista mais usada da app |
| **Os nomes das sete rotinas semeadas** — seis em inglês e «Pernas» em português, literais no código e não recursos | 2.20.0, registado lá como troca por decidir | troca (A5), e é de minutos |
| **Descarregar as imagens dos exercícios** — o estudo chama-lhe «a única deste documento que eu faria já» | `estudo/transversal/04` | número próprio; resolve também o offline e a fuga a terceiros |
| **2 909 alimentos em inglês** | 2.13.0 | trabalho de meses, na oficina |
| **73,7 % do catálogo sem porção** | 2.14.0 | idem |
| **A retenção do ciclo não sai do ecrã** | dez do estudo, #8 | 2.34.0, já no plano |
| **O chip «Todas» e o `MET 7,0` em cada linha** | dez do estudo — não; `estudo/areas/13`, «o que é inútil» | **2.19.1**, que está no plano e não saiu |
| **A frase do arranque** | 2.2.0, adiada pelo dono | 2.40.0, com data — e envelheceu bem: hoje está menos errada do que quando foi adiada |
| **O `food_cache` sem expiração, o custo invisível, o modelo fixado no código** | `estudo/sistema/02` | são do servidor, e o modelo fixado é o ponto fraco da longevidade |
| **A confiança e o intervalo publicado por alimento** — «confiança A · 124–136» | esboço 22, terceira passagem | **não se faz**: nenhuma das três fontes o publica de forma que o oleoduto traga, e inventá-lo é o contrário do que este catálogo faz |

**Não se corrige nada desta tabela por iniciativa minha.** Ela existe para o dono escolher, e
a regra A5 continua a valer: o que entra a meio é troca, não adição.

### O que a auditoria fechou, e não está aqui

A foto do prato fora da cópia (reaberta e decidida, com o silêncio corrigido) · «uma lista
só» e a origem na linha · o multiplicador em chips · editar uma refeição guardada · a
instrução da cintura por sexo, que afinal já estava feita e certa.

**E, do bloco F, três linhas que saíram desta tabela a 2026-09-03** por terem sido feitas e
por ninguém as ter riscado na altura — que é a regra do fim deste ficheiro: os **111
exercícios de peso do corpo** (2.22.0), o **centro de treino 7 → 16** (2.20.0) e os **cinco
separadores**, que deixaram de ser uma decisão em aberto e passaram a ser
`Hoje · Diário · Treino · Progresso · Mais` (2.20.1).

## 2.24.0 · O histórico do treino

**Construída com o estudo aberto**, e por isso esta entrada é curta: o que ficou de fora ficou
por decisão. As duas revisões da D7 estão no registo da versão, no plano.

- **«Recordes com data» saiu por troca (A5)**, para entrar a correcção do «Iniciar treino
  vazio». Os recordes vivem no `WorkoutStatsScreen`, que é o ecrã da **2.25.0**, e o esboço 10
  desenha-os na secção das estatísticas.
- **A 🌟 é calculada e não guardada, contra o que o esboço 10 pede.** Decisão do dono: um
  recorde guardado volta a poder discordar das séries, que é o defeito que a 2.21.0 desfez.
  Custou uma consulta nova e nenhuma coluna — o esquema fica em v39.
- **As secções 2 e 3 do esboço 10** — o seletor de período, as séries por músculo com faixa de
  referência, e o resumo pós-treino comparado — são a **2.25.0** e a **2.26.0**.
- **Fechado de caminho, e não estava no conteúdo da versão:** o `%d min` sem conversão existia
  em **quatro** sítios, não num. O `workout_hub_minutes` deixou de existir e os quatro passam
  pelo `formatDurationMin`.

---

# A varredura de 2026-09-03

Feita depois de ler o estudo inteiro e de correr a app no emulador com a 2.23.1 instalada.
**O que segue foi medido, e cada linha diz onde.**

## O que está publicado e o que não está

**Seis versões estiveram fechadas e dentro de casa, e saíram a 2026-09-03.** Quando esta
varredura começou, a última etiqueta e a última release eram a `v2.19.0`, de 2026-08-30; a
`main` do GitHub estava em `ad479ac` e o local em `5c0796a`, com **seis commits** pelo meio:
2.20.0, 2.20.1, 2.21.0, 2.22.0, 2.23.0 e 2.23.1.

O que isso custou, e não se desfaz publicando:

- **A D4 não foi cumprida em nenhuma das seis.** «CI verde antes de fechar a versão» exige
  uma execução no GitHub, e não houve nenhuma enquanto elas estiveram fechadas: a última
  corrida era a do commit `ad479ac`, anterior a todas. Os registos das seis dizem «detekt e
  lint limpos», que é verdade e é local. O CI só correu sobre elas depois — em `fc908b9`, e
  **veio verde nos três trabalhos**, o que prova o código da 2.23.1 e não prova cada uma das
  cinco anteriores no ponto em que foi fechada.
- **A A3 também não** — «uma de cada vez, publicada e usada, umas semanas antes da
  seguinte»: as seis foram feitas no mesmo dia, 2026-09-02, e nenhuma viveu num telemóvel
  antes de a seguinte começar. Isso não se recupera: o que se aprende a usar é que muda o que
  vem a seguir, e aqui não houve uso pelo meio.

**O que ficou feito a 09-03:** seis etiquetas de `v2.20.0` a `v2.23.1`, e seis releases com
os quatro APKs cada, mais o `catalogo.json` e o `manifesto.json`. Cada APK foi compilado **no
seu próprio commit** — o `appVersion` vive no `build.gradle.kts` e o nome do ficheiro deriva
dele — com as saídas de recursos apagadas antes e o `unzip` conferido depois: **um
`catalogo.json` em cada um**, que é o passo 7 do guia. O `latest` é a `v2.23.1`, e o
manifesto e o catálogo respondem lá com **HTTP 200** e o `sha256` a bater — a verificação que
a própria app faz antes de trocar o catálogo.

## Dois defeitos vistos a correr, e os dois no centro de treino

**1 · «Iniciar treino vazio» leva ao treino que já está a decorrer.**

É o defeito que a 2.20.0 diz ter fechado. O `WorkoutScreen.kt` esconde o cartão de destaque
e o ▶ de cada rotina quando `state.sessaoActivaDesde != null`, com o comentário a explicar
porquê — *«o `startOrResume` devolve a sessão aberta e ignora a rotina que se lhe pede»* —,
e depois desenha o `SecondaryButton` do treino vazio **sem condição nenhuma**.

Verificado no emulador: com um treino «Full Body A» aberto, tocar em «Start empty workout»
abre o «Full Body A». O `WorkoutSessionRepository.startOrResume` faz
`sessionDao.activeSession()?.let { return@withContext it.id }` antes de olhar para o
`routineId`, portanto o comportamento é o mesmo para os dois caminhos.

**O `CentroDeTreinoUiTest` viu este botão e não viu o defeito.** O KDoc dele escreve que
«Começar» é substring de «Começar um treino vazio, que está sempre no ecrã» — a observação
está certa e serviu para escolher a asserção, e o botão de que ela fala ficou por olhar. É a
forma de defeito que o `estudo/transversal/02-robustez.md` §3 nomeia: *«a app identifica o
risco, escreve-o num comentário, e não fecha a saída»*.

**E o CHANGELOG publicado afirma o contrário.** A entrada da 2.20.0 diz «Com um treino a
decorrer, o ecrã oferece retomá-lo **e mais nada**». Oferece mais uma coisa, e essa faz
outra coisa.

**2 · «Retomar o treino · 2618 min».**

O botão de retomar formata com o `workout_hub_minutes`, que é `%1$d min` e não converte para
horas. Um treino aberto há dois dias lê-se «2618 min». A barra da sessão, no mesmo aparelho
e para o mesmo treino, diz `43:39:17` — **duas formas do mesmo facto em dois ecrãs**, e a do
painel é a que não se lê. O comentário do `Retomar` explica porque é que ele conta ao minuto
e não ao segundo; não diz nada sobre o que acontece depois da centésima.

## Três números dos documentos que não batiam

Recontados hoje, e corrigidos no mesmo commit (C4):

| Documento | Dizia | É |
|---|---|---|
| `docs/referencia/regras.md` | 67 testes-guarda documentados | **72** — contados na tabela dos [testes-guarda](testes-guarda.md), e os 72 ficheiros existem |
| `docs/README.md` | as 30 regras da produção | **31** — a D7 entrou a 2026-08-31 e este índice não a apanhou |
| `docs/referencia/base-de-dados.md` | 33 tabelas, e 35 migrações automáticas na nota | **34** e **36** — a contagem de tabelas saltou o `db_info`, e a nota de 09-02 trocou um erro por outro |

## E o relatório vivo está cinco versões atrasado

O [`como-continuar.md`](como-continuar.md) fecha a dizer que *«o relatório vivo republica-se
ao fim de cada versão»*. O `estudo/relatorio.html` foi escrito a 2026-08-29 e a versão mais
alta que menciona é a 2.9.0. O `estudo/PLANO-DE-PRODUCAO.md` diz, na secção do
versionamento, **«atual: 2.0.2»**.

Nenhum dos dois está no git — são da pasta `estudo/`, que o `.gitignore` exclui —, e por
isso nenhum teste-guarda os podia ter apanhado. Fica escrito aqui, que é o sítio que está.

---

## Como manter isto honesto

- Uma linha só passa da Parte B para a Parte A **depois de o documento ser aberto**.
- Um número escrito aqui reconta-se antes de se voltar a citar (C3, C4).
- Quando uma versão nova sair, o que ela fechar risca-se daqui **no mesmo commit** — senão
  este documento envelhece exactamente como o `como-continuar.md` envelhecia antes de existir.
