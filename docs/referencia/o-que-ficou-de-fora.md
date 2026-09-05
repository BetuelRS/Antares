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
| ~~**Os nomes das sete rotinas semeadas**~~ | 2.20.0 | **feito na 2.25.0** — vêm dos recursos, e o `Template` recebe um `StringResource`, portanto um literal já não compila |
| **O `LoggingStreak.current` sem chamador** — a sequência estrita, que o comentário dava como a dos marcos e dos troféus | 2.25.0, ao alargar a varredura ao `composeApp/src/commonMain/kotlin/pt/antares/app/core/calc/` | decisão de produto: ligar os troféus a ela, ou apagá-la. O `estudo/motor/07` trata as duas sequências como uma escolha |
| **Descarregar as imagens dos exercícios** — o estudo chama-lhe «a única deste documento que eu faria já» | `estudo/transversal/04` | número próprio; resolve também o offline e a fuga a terceiros |
| **2 909 alimentos em inglês** | 2.13.0 | trabalho de meses, na oficina |
| **73,7 % do catálogo sem porção** | 2.14.0 | idem |
| **A retenção do ciclo não sai do ecrã** | dez do estudo, #8 | 2.34.0, já no plano |
| **O chip «Todas» e o `MET 7,0` em cada linha** | dez do estudo — não; `estudo/areas/13`, «o que é inútil» | **2.19.1**, que está no plano e não saiu |
| **A frase do arranque** | 2.2.0, adiada pelo dono | 2.40.0, com data — e envelheceu bem: hoje está menos errada do que quando foi adiada |
| **O `food_cache` sem expiração, o custo invisível, o modelo fixado no código** | `estudo/sistema/02` | são do servidor, e o modelo fixado é o ponto fraco da longevidade |
| **A confiança e o intervalo publicado por alimento** — «confiança A · 124–136» | esboço 22, terceira passagem | **não se faz**: nenhuma das três fontes o publica de forma que o oleoduto traga, e inventá-lo é o contrário do que este catálogo faz |
| ~~**«Ainda não treinaste» com treinos na mesma semana por baixo**~~ | 2.20.0, visto a correr a 2026-09-05 | **feito na 2.26.0**, por decisão do dono — o `Convite` passa a saber se já houve treinos, e diz «ainda não treinaste **com uma rotina**» |
| ~~**O `today_no_profile` sem saída**~~ | 2.0.x, reconfirmado a 2026-09-05 | **feito na 2.26.0** — o ecrã ganhou o botão «Responder agora», que leva ao arranque |
| ~~**O `ShareCard` regista a camada em cada composição**~~ | 2026-09-05 | **feito na 2.26.0** — a gravação saiu para o `rememberCartaoPartilhavel` e só acontece ao carregar em partilhar |
| **Os nomes dos exercícios cortados na lista da sessão a 200 %** de escala de letra — «Agachamento com» | visto a correr a 2026-09-05, na corrida da 2.26.0 | é o ecrã da área 08; cortar um nome numa lista não é o mesmo que cortar letras a meio, e por isso não entrou por troca |
| **O recorde sem o número de onde veio** — o esboço 10 §3 escreve «1RM estimado subiu de 76 para 78 kg», e a app escreve «novo recorde» | 2.26.0, decidido | é uma leitura a mais por recorde; cabe onde os recordes forem trabalhados |
| **«O teu maior volume de sempre»** — o «o que está mal» ponto 5 da `estudo/areas/10` | 2.26.0, decidido | é comparação com a história toda e não com a rotina; casa com os recordes |
| **Os nomes dos exercícios saem em inglês** quando o catálogo não tem `namePt` — «Alternating Floor Press» nos recordes, com a app em português | 2.20.0 nomeia a convenção `namePt.ifBlank { nameEn }`; visto nos recordes a 2026-09-05 | é conteúdo do `seed_exercises.json`, e o `estudo/areas/09` trata da biblioteca — **2.27.0** |

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

## 2.26.0 · O resumo pós-treino

**Construída com a área 10 e o esboço 10 §3 abertos.** As três propostas da secção 3 estão
feitas, e as duas revisões da D7 estão no registo da versão, no plano.

- **A versão abriu pelas três correcções que o dono mandou entrar**, e as três estavam nesta
  tabela: o «Ainda não treinaste» com treinos livres na semana, o `ShareCard` a gravar a camada
  sempre, e o «Hoje» sem perfil sem saída. Foi adição e não troca, e está escrito como tal no
  plano — a A5 pede que algo saia, e não saiu nada.
- **Dois defeitos de 200 % de escala de letra, ambos anteriores a esta versão**, corrigidos de
  caminho: o título da barra do topo passava a duas linhas e a segunda era cortada a meio das
  letras — corrigido no `AntaresTopBar`, e vale para a app inteira —, e a linha do alvo da sessão
  saía uma letra por linha. É a terceira aparição da família que o `estudo/transversal/03` §3.1
  nomeia: 2.19.0, 2.25.0, e agora.
- **E não há teste de composição que os apanhe.** O Robolectric não tem tipos de letra: mede a
  frase «Corpo inteiro A · terminado» como 27 dp de largura, e por isso ela nunca muda de linha
  lá dentro por muito que se lhe suba a escala — medido, ao tentar escrever o guarda como um
  `runComposeUiTest`. O `EscalaDeLetraTest` que ficou guarda a fonte, não a forma; a forma
  continua a encontrar-se **no aparelho** (D3).
- **O que ficou de fora está nas linhas novas da tabela acima**, e é decisão: o recorde escrito
  por extenso como o esboço o desenha, «o teu maior volume de sempre», e os nomes dos exercícios
  cortados na lista da sessão a 200 %.

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
alta que menciona é a ~~2.9.0~~ **2.19.0** *(recontado a 2026-09-04: o corpo dele declara a
2.18.1 publicada e a 2.19.0 como «a seguir»)*. O `estudo/PLANO-DE-PRODUCAO.md` diz, na secção
do versionamento, **«atual: 2.0.2»**.

Nenhum dos dois está no git — são da pasta `estudo/`, que o `.gitignore` exclui —, e por
isso nenhum teste-guarda os podia ter apanhado. Fica escrito aqui, que é o sítio que está.

---

# A varredura de 2026-09-04

Feita depois de ler o `estudo/` inteiro — os 63 documentos e os dezassete esboços — e de
correr a 2.24.0 de lançamento no emulador, **actualizada por cima da 2.20.1 com dados lá
dentro**, que é o que prova a migração v37 → v39 num salto. Cada linha diz onde foi medida.

## O que se confirmou, e não é achado

A 2.24.0 está publicada e verde: `main` em `e6f03c6`, etiqueta `v2.24.0`, release com os
quatro APKs mais o `catalogo.json` e o `manifesto.json`, **CI verde nesse commit**. O
`latest` do GitHub responde com HTTP 200 aos dois ficheiros e o `sha256` do catálogo bate com
o do manifesto — que é a verificação que a app faz antes de trocar o catálogo. Localmente:
**1742 testes Kotlin, 58 das ferramentas, 68 Deno, detekt e lint limpos**, e nenhum segredo em
1033 ficheiros.

Recontados no catálogo construído, e todos certos: **7 932 alimentos** · 3 329 `ciqual-`,
2 944 `usda-`, 1 372 `tca-`, 274 `ptx`, 13 `pt-` · **2 090 com porção (26,3 %)** · 4 153 com
família de confeção · 773 líquidos · **42 chaves declaradas, 40 em uso**. E no esquema: **34
tabelas, 36 migrações automáticas, versão 39**, contadas na lista de entidades do `AntaresDb`
e nos `tableName` do `39.json`, que dão o mesmo número.

## Um defeito visto a correr, e é o quinto de uma família que a 2.24.0 deu por fechada

**«5 exercises · ~4236 min last time», no cartão de destaque do centro de treino.**

O `workout_hub_last_duration` é `~%1$d min` e o `resumoDaRotina` passa-lhe os minutos crus
(`WorkoutScreen.kt:376`). A 2.24.0 diz, no registo e no CHANGELOG, que o `%d min` sem
conversão *«existia em quatro sítios e não num»* e que **os quatro** passaram pelo
`formatDurationMin`. São cinco: este ficou.

E vê-se ao lado do que a versão corrigiu — no mesmo aparelho, a linha do histórico do mesmo
treino diz **«70h 36m»** e o cartão de destaque diz **«~4236 min»**. É a mesma queixa que
fechou a 2.24.0, duas linhas acima da correcção.

**Como escapou:** a correcção foi feita a partir da string `workout_hub_minutes`, e esta é
outra chave. Quem procurou o nome encontrou quatro; quem procurasse o formato `%1$d min`
encontrava cinco.

## Doze importações mortas na área que a 2.25.0 vai abrir

O defeito concreto 1 da `estudo/areas/10` são importações que não se usam. A 2.24.0 apagou as
seis dos três ficheiros que reescreveu — histórico, detalhe e resumo — e o módulo do treino
tem mais. Contadas à mão, uma a uma, confirmando que o nome não aparece fora da linha do
`import`:

| Ficheiro | Importações sem uso |
|---|---|
| `WorkoutStatsScreen.kt` | `weightUnitLabel`, `roundToInt` |
| `WorkoutHistoryViewModels.kt` | `SharingStarted`, `stateIn` |
| `RoutineEditScreen.kt` | `Icons.Default.Add`, `Icons.Default.Delete`, `KeyboardArrowDown` |
| `ExerciseLibraryScreen.kt` | `AntaresCard`, `clickable`, `Role` |
| `ExerciseLibraryViewModel.kt` | `map` |
| `WorkoutSessionScreen.kt` | `Icons.Default.Delete` |

As duas primeiras linhas são os ficheiros da **2.25.0**; as outras quatro são vizinhas e não
são do conteúdo dela. **Nem o detekt nem o lint as apanham** — os dois vieram limpos nesta
mesma varredura, e é por isso que a família sobrevive de versão em versão.

## Duas semanas diferentes dentro do mesmo separador

O `WorkoutStatsViewModel` calcula `weekAgo = agora − 7 dias` e pede o volume por músculo a
partir daí: é uma **janela móvel de sete dias**, e o ecrã escreve-o — «Volume by muscle
(7 days)», visto no aparelho.

O cartão «Esta semana» do painel de treino, dois toques atrás, conta a **semana ISO**: a
2.20.0 decidiu-o e escreveu porquê — *«é o que faz este cartão, o relatório do treinador e a
grelha do progresso concordarem»*.

Não é um número errado; é a mesma palavra a querer dizer duas coisas no mesmo separador, e o
seletor de período da **2.25.0** é onde isso se decide.

## Três números dos documentos, recontados hoje

Corrigidos no mesmo commit (C4):

| Documento | Dizia | É |
|---|---|---|
| `docs/referencia/regras.md` | 73 testes-guarda documentados | **74** — 73 classes de teste em Kotlin mais o `tools/catalogo/origem-por-nutriente.test.mjs`; os 74 ficheiros existem |
| `docs/referencia/dados-e-licencas.md` | mediana de 20 chaves por alimento | **25** — contada no `catalogo.json` construído, sobre os 7 932 |
| `docs/referencia/como-continuar.md` | «o formatador da app já diz ontem» | **não diz** — o `dayShortDated` dá sempre o dia da semana com a data. O plano corrigiu a sua cópia a 2026-09-02 e esta ficou para trás |

E, dentro deste ficheiro, a linha do relatório vivo dizia que a versão mais alta que ele
menciona é a 2.9.0. **É a 2.19.0.** Corrigida acima.

## O que continua aberto e foi reconfirmado no código

Nenhum destes mudou, e nenhum se toca sem autorização (A1):

- **Os nomes das sete rotinas semeadas** são literais no `RoutineTemplateSeeder.kt:41-45` —
  «Full Body A», «Full Body B», «Push», «Pull» e **«Pernas»**, quatro em inglês e um em
  português, e vê-se assim no ecrã com a app em inglês.
- **As imagens dos exercícios** continuam a sair para o `raw.githubusercontent.com`
  (`ExerciseSeeder.kt:136`).
- **O `FoodRow` continua a usar `Card` + `ListItem` do Material**
  (`FoodSearchScreen.kt:896` e `:909`), que é o último resto das duas linguagens de cartão.
- **O `estudo/relatorio.html` está seis versões atrasado** — declara a 2.18.1 publicada, o
  esquema v35, o catálogo v5, 30 regras e 62 testes-guarda. Hoje são a 2.24.0, v39, v6, 31 e
  74. Está fora do git, e é por isso que nada o apanha.

## 2.25.0 · As estatísticas do treino

**Construída com o estudo aberto**, e por isso esta entrada é curta: o que ficou de fora ficou
por decisão. As duas revisões da D7 estão no registo da versão, no plano.

- **Três das linhas da tabela do fim deste ficheiro fecharam**, e riscam-se aqui no mesmo
  commit: o **`%d min` do cartão de destaque** (era o quinto sítio, não o quarto), as **doze
  importações mortas** do módulo do treino, e os **nomes das sete rotinas semeadas**, que eram
  literais e em duas línguas — hoje vêm dos recursos, e viu-se «Corpo inteiro A» e «Empurrar»
  numa instalação limpa com a app em português.
- **A faixa de 10 a 20 séries semanais entra**, decisão delegada, desenhada como faixa e com a
  frase que diz que é uma orientação da literatura e não um alvo calculado para esta pessoa.
- **A barra abaixo da faixa é âmbar e não vermelha**, contra a letra do esboço 10 e com a razão
  medida: o `error` do tema é `#FF6B6B` e a primária `#FF5A4A`, e lado a lado não se distinguem.
- **A frequência cobre o período escolhido e não doze semanas fixas**, contra a área 10. As duas
  coisas que o estudo pede colidem — «o período governa o ecrã inteiro» é a exigência dele —, e
  um gráfico de doze semanas ao lado de um seletor era o seletor a não governar nada.
- **A comparação com a semana anterior** — «o que está mal» ponto 3 da área 10 — é a **2.26.0**,
  que a promete no resumo pós-treino.

### Uma coisa que a versão abriu e não fechou

**O `LoggingStreak.current` não tem chamador na app**, e o comentário dela dizia «é a que os
marcos e os troféus usam» — o ecrã do Hoje usa a `currentWithFreeze` e a `longest`. Medido a
2026-09-04, ao alargar a varredura de código morto ao `composeApp/src/commonMain/kotlin/pt/antares/app/core/calc/` inteiro.

O comentário passou a dizer a verdade, e a função ficou declarada na varredura com a razão.
**Ligar os troféus à sequência estrita ou apagá-la é uma decisão de produto** que o
`estudo/motor/07` descreve — «estrita para os troféus, perdoada para o ecrã» — e não é do ecrã
das estatísticas. Fica na tabela do fim.

---

# A varredura de 2026-09-05

Feita depois de ler o `estudo/` inteiro — os documentos e os dezassete esboços — e de correr a
**2.25.0 de lançamento** no emulador. Cada linha diz onde foi medida.

## O que se confirmou, e não é achado

A 2.25.0 está publicada e verde: `main` e a etiqueta `v2.25.0` em `abc998d`, release com os
quatro APKs mais o `catalogo.json` e o `manifesto.json`, **CI verde nesse commit** (`gh run
list`). Localmente, com o `verificar.mjs`: **1769 testes Kotlin**, 58 das ferramentas, 68 Deno,
detekt e lint limpos, nenhum segredo em 1037 ficheiros.

Recontados, e todos certos: **75 testes-guarda** documentados — 74 classes em Kotlin mais o
`tools/catalogo/origem-por-nutriente.test.mjs`, e os 75 ficheiros existem · **31 regras** ·
esquema **v39**, **34 tabelas**, **36 migrações automáticas** · catálogo **v6**, **7 932
alimentos** (3 329 `ciqual-`, 2 944 `usda-`, 1 372 `tca-`, 274 `ptx`, 13 `pt-`), **2 090 com
porção (26,3 %)**, 4 153 com família de confeção, 773 líquidos, **42 chaves declaradas, 40 em
uso, mediana de 25** · **41 ficheiros** no `composeApp/src/commonMain/kotlin/pt/antares/app/core/calc/` · **51 ecrãs**.

## Dois defeitos vistos a correr, e nenhum é da versão que acabou de fechar

**1 · O painel de treino diz «Ainda não treinaste» com quatro treinos na mesma semana, dois
centímetros abaixo.**

Visto no emulador com a 2.25.0 de lançamento: o cartão de destaque mostra o terceiro estado —
«Ainda não treinaste · Marca as rotinas pelos dias» — e o cartão da semana, logo por baixo, diz
**«9720 kg de volume · 18 séries»** com três dias pintados.

Medido: o histórico só tem **«Treino livre»**, e o `destaque()` do `WorkoutHubRepository` escolhe
o `Convite` quando nenhuma **rotina** foi treinada — o `observeLastDoneByRoutine` ignora, e bem,
as sessões com `routineId` nulo. O cartão da semana conta **todas** as sessões terminadas. Os dois
números estão certos e a frase que os acompanha não: o KDoc do `Convite` diz «sem plano e **sem
histórico**», e há histórico.

Não é do conteúdo da 2.25.0 — nasceu com os três estados da 2.20.0, e o esboço 06 não desenha
este caso. **Quem só faça treinos livres vê este ecrã sempre.**

**2 · O `today_no_profile` continua a ser um beco sem saída**, que é o defeito concreto 3 da
`estudo/areas/01-hoje.md`. `TodayScreen.kt:121-127`: sem perfil, o ecrã desenha um `Text` e faz
`return` — sem botão, sem caminho para o arranque. Visto no ecrã, e é o primeiro que aparece nesse
estado.

## E uma coisa a saber antes de a 2.26.0 abrir

O **`ShareCard` do Progresso regista a camada em cada composição** — `ProgressScreen.kt:199-201`,
`camada.record { … }` dentro do `drawWithContent`. É o defeito concreto 3 da
`estudo/areas/14-progresso.md`, e interessa agora porque a 2.26.0 promete partilhar o resumo
**com o partilhador que já existe**: copiar o padrão copia o defeito.

**Foi o que aconteceu, ao contrário:** a 2.26.0 abriu por aqui. A gravação saiu para o
`rememberCartaoPartilhavel`, num sítio só, e passou a acontecer apenas quando alguém carrega em
partilhar — e é esse o cartão que o resumo do treino usa.

## O relatório vivo estava sete versões atrasado

Declarava a 2.18.1 publicada, esquema v35, catálogo v5, 30 regras e 62 testes-guarda. **Republicado
a 2026-09-05** com os números acima. Continua fora do git, e é por isso que nenhum teste-guarda o
apanha — o que o mantém honesto é esta linha.

E o `estudo/PLANO-DE-PRODUCAO.md` dizia, na secção do versionamento, «atual: **2.24.0**».
Corrigido para 2.25.0, e a versão ganhou a linha de publicação que lhe faltava.

---

## Como manter isto honesto

- Uma linha só passa da Parte B para a Parte A **depois de o documento ser aberto**.
- Um número escrito aqui reconta-se antes de se voltar a citar (C3, C4).
- Quando uma versão nova sair, o que ela fechar risca-se daqui **no mesmo commit** — senão
  este documento envelhece exactamente como o `como-continuar.md` envelhecia antes de existir.
