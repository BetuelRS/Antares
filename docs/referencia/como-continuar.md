# Como continuar

Este ficheiro é o que se lê ao começar uma sessão nova. Substitui a mensagem que antes era
reconstruída de memória a cada vez — e que, sendo reconstruída, envelhecia sem ninguém notar.

**Actualiza-se ao fim de cada versão**, no mesmo commit que a publica.

## Onde estamos

- **2.16.0 é a última publicada**, a 2026-08-27, e abre o bloco E. Esquema da base **v33**,
  catálogo **v5**. Onze versões feitas. Árvore verde: 1539 testes Kotlin, 52 das ferramentas,
  68 Deno, detekt e lint limpos.
- **Volta-se a fazer versão a versão**, por decisão do dono a 2026-08-27. O bloco D foi a
  excepção e não o modelo: as corridas existiram porque a cerimónia por versão era um terço
  do trabalho num bloco de doze, e o bloco E tem quatro.
- **O bloco D está fechado a sério.** A 2.7.0 levou-o quase todo e deixou seis promessas por
  cumprir; a 2.8.0 fecha-as. Nenhuma delas rebentava nada — davam números errados em
  silêncio, e só apareceram ao reler o plano promessa a promessa contra o código.
- A release levou os quatro APK **e** o `catalogo.json` e o `manifesto.json`, e o botão
  «Procurar» no aparelho passou a dizer **«está em dia»**. Antes dela dizia «não deu para
  chegar lá», porque o `latest` do GitHub apontava para a 2.6.0, que não os trazia — ver o
  passo 9 de [`lancar-uma-versao.md`](../guias/lancar-uma-versao.md). **A release seguinte tem
  de os levar outra vez**: uma que não os traga não mantém os anteriores, apaga o caminho.
- **A próxima é a 2.17.0**, a revisão da IA. Os números do plano entre a 2.9.0 e a 2.15.0
  ficam livres: o conteúdo deles saiu na 2.7.0 e na 2.8.0, ou não se lança de todo.
- A **2.5.1 foi fechada sem sair.** A razão está no plano e não se reabre sem a ler.

## O bloco D fez-se em três corridas

Por decisão do dono, a 2026-08-23. Uma corrida é um plano, uma execução e **um lançamento** —
em vez de uma versão de cada vez. A cerimónia por versão era perto de um terço do trabalho.

| Corrida | O quê | Estado |
|---|---|---|
| **1 · o encanamento** | 2.7.0, com a 2.8.0 absorvida | **feita e lançada** |
| **2 · as ferramentas** | 2.9.0 + 2.10.0 | **feita, e não se lança** |
| **3 · o conteúdo** | 2.11.0 a 2.15.0 | **feita, saiu na 2.7.0** |

**O bloco D saiu todo numa versão: a 2.7.0.** As três corridas foram feitas seguidas, e partir
o que já estava escrito em cinco lançamentos era cerimónia sem nada por baixo — a razão de as
corridas existirem.

O **conteúdo** que o plano tinha em 2.8.0 a 2.15.0 saiu aqui, ou não se lança de todo: o motor
de qualidade e a oficina são ferramentas do repositório, e o passo 1 do guia diz que essas não
se lançam. Os **números** ficam livres, e a 2.8.0 leva o que faltou. Saltar números não custa
nada: o `versionCode` deriva do nome e continua a crescer.

**A 2.7.0 não fechou o bloco.** Seis promessas do plano ficaram por cumprir e nenhuma delas
rebentava nada — davam números errados em silêncio, que é o modo de falhar deste bloco. Só
apareceram ao reler o plano promessa a promessa contra o código, e é isso que a 2.8.0 fecha.

**O que não se junta é o conteúdo com o código.** As decisões sobre nomes, fusões e porções
são do dono; tomá-las em lote sem ele as ver é o modo de falhar deste bloco — um alimento com
o nome trocado não rebenta, não dá erro, e custa uma versão a corrigir.

**O que não se batcha nunca:** a medição no início de cada peça. Cinco versões abertas, cinco
premissas do plano erradas, e as cinco apareceram a medir — não a ler.

## O que a corrida no aparelho mostrou

Emulador Android 16, x86_64, antes de lançar:

1. **A actualização a partir da 2.6.0 passa.** A migração salta três versões de esquema, da
   v28 para a v30, e o dia registado na 2.6.0 — 1 050 kcal em dois registos — aparece igual
   depois de instalar por cima. Nada de re-onboarding, nada no `logcat`.
2. **A instalação limpa passa**, com o catálogo v4 semeado do APK.
3. **O cartão «e se for cozinhado?» aparece** onde tem de aparecer: em «Frango, carne, cru»,
   109 kcal passam a 139 no grelhado, com «100 g cru dão 79 g» e o campo para quem pesou
   depois de cozinhar. Metade do catálogo não o tem, e é de propósito.
4. **O botão «Procurar» dizia «não deu para chegar lá»** — e estava certo: a release mais
   recente era então a 2.6.0, que **não** trazia o `manifesto.json`, e o endereço respondia
   404, confirmado à mão. Depois de publicar a 2.7.0 com os dois ficheiros, o mesmo botão
   passou a dizer **«está em dia»**. É o sintoma do passo 9 do guia, visto dos dois lados.

E mostrou dois defeitos, corrigidos aqui: o ecrã de boas-vindas e o de atribuições prometiam
**1376** alimentos do INSA quando o catálogo tem **1372** — quatro foram fundidos noutra
medição —, e a linha das origens em [`dados-e-licencas.md`](dados-e-licencas.md) ainda contava
3401/2937/1376/284. O `NumerosDoCatalogoTest` não via nenhum dos dois: só olhava para linhas
que dizem CIQUAL, USDA ou INSA por extenso, e não para as que contam pelo identificador nem
para os textos da app. Agora vê, e o Gradle já não dá a tarefa por actualizada quando **só**
o documento muda.

## O estado do catálogo

**7 932 alimentos.** As decisões de conteúdo do bloco D foram tomadas e estão escritas com a
razão de cada uma em [`arbitragem.mjs`](../../tools/catalogo/arbitragem.mjs).

| | |
|---|---|
| colisões de nome | **zero** — 97 fusões e 24 nomes desambiguados |
| contradições | **zero** — as três regras da coerência corrigem-nas no oleoduto |
| suspeitas na fila | **90** — 28 de Atwater, 60 fora de escala, 2 discordâncias |
| nomes em português | **2 131**, de 1 362 segmentos de vocabulário |
| ainda em inglês | **2 909** |
| com porção | 2 090 |
| com família de confeção | 4 153 |
| com água declarada | 7 019 |
| líquidos | **773**, dos quais 696 com densidade medida |
| na fila dos líquidos | 55 — parecem líquidos e ninguém decidiu ainda |

**A regra de arbitragem, em três degraus.** Uma medição ganha a uma estimativa escrita à mão.
Duas medições que concordam resolvem-se a favor da portuguesa — a TCA mediu produto daqui.
Duas que discordam desempatam-se por uma terceira, e a USDA está cá e é independente das
outras duas.

**Nem toda a colisão é um duplicado.** Vinte e quatro eram dois alimentos diferentes com o
mesmo nome: a batata assada da CIQUAL tem 0,1 g de gordura e a da TCA tem 4,8. Aí fundir era
apagar comida, e o que se corrige é o nome.

## O que falta, e é trabalho de meses

- **2 909 alimentos ainda em inglês.** O separador «segmentos» da oficina mostra o que falta
  por quantos alimentos cada um desbloqueia — traduzir um segmento arruma todos os que
  esperam por ele. Medido: 1 362 segmentos dão 42 % dos nomes; para 65 % são precisos cerca
  de 2 500, e para 84 % cerca de 3 500.
- **90 suspeitas.** O que resta do Atwater são as fontes que usam factores específicos por
  alimento — o cacau, os frutos secos, as leguminosas — e isso é legítimo. O que resta de
  «fora de escala» são outliers verdadeiros: o álcool puro é mesmo o mais calórico do grupo
  das bebidas.
- **Cada lote de vocabulário traz outra leva de duplicados à superfície**, e isso não é a
  tradução a criar problemas: é o inventário a ficar legível. Foram seis voltas de arbitragem
  até aqui, e a seguinte virá com o lote seguinte.
- **55 candidatos a líquido**, em `tools/catalogo/liquidos-por-decidir.json`. Quase todos
  óleos, e não se marcam sozinhos: as três regras que tentei marcaram comida sólida — «Olive,
  black, in oil», «Milk chocolate, bar». O nome inglês é a armadilha.

## As ferramentas da corrida 2

Duas, e nenhuma delas aparece na app. Estão documentadas em
[`tools/README.md`](../../tools/README.md); em resumo:

- **O motor de qualidade** corre dentro do `construir.mjs`. Cinco verificações, todas a
  comparar o alimento consigo mesmo. Uma **contradição** — um número impossível — chumba a
  construção a menos que esteja declarada em `qualidade.json`. Uma **suspeita** não chumba
  nada e vai para a fila. Hoje: 12 contradições e 252 suspeitas em 8 011 alimentos.
- **A oficina** é uma página local (`node tools/oficina/servidor.mjs`). Escreve em
  `correcoes.json`, e a fila vem ordenada por quantas vezes cada alimento foi registado — o
  histórico sai do telemóvel e **não entra no git**.

- **O vocabulário dos nomes** (`tools/vocabulario/`) traduz segmento a segmento, com
  concordância: o género e o número vêm da base e os qualificadores seguem-na. Um nome só é
  aplicado quando fica **inteiro** — meio traduzido parece um defeito.

## As regras


[`regras.md`](regras.md) é a fonte única. São 29, em grupos A a F, e cada uma diz quem a
verifica. As que mais mandam no dia a dia:

| | |
|---|---|
| **A1** | nada começa sem o dono mandar, versão a versão. O plano não é autorização. |
| **A2** | cada versão abre com perguntas. Estão pré-escritas no plano. |
| **A5** | nada entra a meio sem sair outra coisa. |
| **B2** | versão que cresce a meio parte-se. Já aconteceu uma vez, e a excepção de numeração está em [`versionamento.md`](versionamento.md). |
| **C1** | confirmar o achado no código antes de lhe tocar. Já apanhou seis achados errados, quatro deles vindos do próprio estudo. |
| **C3** | nunca escrever um número que não se contou. Já se escreveu uma versão inteira à volta de um número que era um `sleep`. |
| **D1** | o que conta é o relatório de testes, não a última linha do Gradle. |
| **D3** | correr a app, não só os testes. |
| **D6** | varrer **antes** de publicar, não depois. |

**Nomes de classes e ficheiros são substantivos, não frases.** Regra do dono, dada depois de
um ecrã se ter chamado `OQueSaiDaquiScreen`. Português nos nomes é aceite; frases não.

## Verificar

```bash
node tools/verificar.mjs
```

Uma chamada: testes Kotlin com os nomes dos que falham, detekt, lint, os testes das
ferramentas, as funções do servidor e as duas buscas de segredos. Seis linhas e um veredicto.
Apaga os relatórios antigos antes de correr — sem isso, uma execução que nem arranca deixa a
anterior a passar por verdade.

## O catálogo

Construído fora da app:

```bash
node tools/catalogo/construir.mjs
```

Determinístico, e **chumba** em duas situações: se perder um alimento que a fonte declare e
não esteja em `desvios.json`, e se aparecer uma **contradição** nova — um número impossível —
que não esteja em `qualidade.json`. Nos dois casos há uma opção para aceitar e ler o `git
diff` (`--aceitar-desvios`, `--aceitar-qualidade`).

O que **não** chumba são as suspeitas: energia que não bate com os macros, alimento fora de
escala no seu grupo, duas fontes a discordarem. Essas vão para a fila da oficina. A razão de
não chumbarem é que medem uma discordância entre métodos de medição, e chumbar por isso era
não poder publicar até a ANSES corrigir a tabela dela.

A versão sobe à mão em **dois** sítios — no `construir.mjs` e no `FoodSeeder` — e há um
teste-guarda a exigir que sejam a mesma. Ver [`tools/README.md`](../../tools/README.md).

## Armadilhas já pagas

- **Os heredocs desta consola comem um nível de barras invertidas**, mesmo com o delimitador
  entre plicas. Já partiram uma expressão regular, um `replace` e um ficheiro inteiro.
  Ficheiros com barras escrevem-se com a ferramenta de escrita.
- **Os ficheiros estão em `CRLF`.** Uma substituição de texto com `\n` não encontra nada, e um
  teste que procure `"…\n"` acusa quem mexeu no ficheiro em vez de quem partiu a regra.
- **Não reescrever Kotlin com expressões regulares.** Três tentativas, duas produziram código
  partido.
- **`MSYS_NO_PATHCONV=1` em todo o comando `adb` que toque em `/sdcard`.** Sem isso o Git Bash
  reescreve o caminho e lêem-se ficheiros velhos sem dar por isso.
- **`adb uninstall` não garante que os dados desaparecem.** Estado limpo é `pm clear`, mais
  apagar `/sdcard/Documents/Antares` à mão.
- **Ficheiros apagados continuam dentro do APK.** A pasta intermédia de recursos do Gradle não
  se limpa com `--rerun-tasks`. Ver o passo 7 de [`lancar-uma-versao.md`](../guias/lancar-uma-versao.md).
- **Verificar sempre a partir de uma instalação limpa do APK de lançamento.** Foi aí, e só aí,
  que apareceram dois dos três defeitos da 2.1.0.
- **O motor SQLite empacotado não carrega na máquina virtual dos testes.** Uma pergunta sobre
  o comportamento do Room com o motor definido não se responde em teste unitário nesta
  máquina — responde-se no emulador.

## Quem faz o quê

O que decide **o que é verdade sobre um alimento**, ou **o que a app faz quando não sabe**,
fica no modelo mais capaz, com esforço alto: a abertura de cada versão, a medição C1, e as
versões de conteúdo. A execução mecânica — renomear em N sítios, converter testes, escrever
DAOs — vai para um modelo mais barato, com a instrução já escrita.

A razão é o modo de falhar: **um erro de encanamento rebenta num teste; um erro de conteúdo
fica calado e vai para a Play Store.**

## O plano e o relatório

O plano vive na pasta **estudo**, fora do git por decisão do dono — o registo
durável é o `git log`. As respostas às perguntas de abertura ficam escritas por baixo de cada
versão, e o que se aprendeu a construir fica por baixo dessas.

O relatório vivo republica-se ao fim de cada versão, com os números, o texto **e** as barras.
