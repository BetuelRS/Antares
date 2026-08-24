# Como continuar

Este ficheiro é o que se lê ao começar uma sessão nova. Substitui a mensagem que antes era
reconstruída de memória a cada vez — e que, sendo reconstruída, envelhecia sem ninguém notar.

**Actualiza-se ao fim de cada versão**, no mesmo commit que a publica.

## Onde estamos

- **2.6.0 é a última publicada.** Esquema da base **v28**. Oito versões feitas.
- A **2.7.0 está escrita e por lançar**, e leva o bloco D inteiro. Árvore verde: 1472 testes
  Kotlin, 40 das ferramentas, detekt e lint limpos. Falta-lhe
  **correr no aparelho** — instalação limpa e actualização a partir da 2.6.0, que salta da v28
  para a v30 — e depois publicar. Não havia telemóvel nem emulador ligado.
- O catálogo no repositório é a **versão 4** e a 2.6.0 publicada traz a 2. A release da 2.7.0
  tem de levar o `catalogo.json` **e** o `manifesto.json` anexados — ver o passo 9 de
  [`lancar-uma-versao.md`](../guias/lancar-uma-versao.md). Sem eles, o botão de actualizar o
  catálogo não encontra nada, porque o `latest` do GitHub passa a apontar para uma release
  que não os tem.
- A **2.5.1 foi fechada sem sair.** A razão está no plano e não se reabre sem a ler.

## O bloco D faz-se em três corridas

Por decisão do dono, a 2026-08-23. Uma corrida é um plano, uma execução e **um lançamento** —
em vez de uma versão de cada vez. A cerimónia por versão era perto de um terço do trabalho.

| Corrida | O quê | Estado |
|---|---|---|
| **1 · o encanamento** | 2.7.0, com a 2.8.0 absorvida | **feita** |
| **2 · as ferramentas** | 2.9.0 + 2.10.0 | **feita, e não se lança** |
| **3 · o conteúdo** | 2.11.0 a 2.15.0 | **feita, absorvida na 2.7.0** |

**O bloco D sai todo numa versão: a 2.7.0.** As três corridas foram feitas seguidas, e partir
o que já está escrito em cinco lançamentos era cerimónia sem nada por baixo — a razão de as
corridas existirem.

Os números **2.8.0 a 2.15.0 ficam consumidos ou por usar**, e a próxima versão a sair depois
da 2.7.0 é a **2.16.0**. Saltar números não custa nada: o `versionCode` deriva do nome e
continua a crescer. Da corrida 2 nada se lança — o motor de qualidade e a oficina são
ferramentas do repositório, e o passo 1 do guia diz que essas não se lançam.

**O que não se junta é o conteúdo com o código.** As decisões sobre nomes, fusões e porções
são do dono; tomá-las em lote sem ele as ver é o modo de falhar deste bloco — um alimento com
o nome trocado não rebenta, não dá erro, e custa uma versão a corrigir.

**O que não se batcha nunca:** a medição no início de cada peça. Cinco versões abertas, cinco
premissas do plano erradas, e as cinco apareceram a medir — não a ler.

## O que falta no bloco D

Está tudo escrito e verde. Falta **o aparelho e o lançamento**:

1. Instalação limpa do APK de lançamento, e actualização a partir da 2.6.0 — a migração salta
   três versões de esquema, da v28 para a v30. Foi numa instalação limpa, e só aí, que
   apareceram dois dos três defeitos da 2.1.0.
2. O botão «Procurar» tem de dizer **«está em dia»** — o catálogo do APK é a versão 4, e a
   release traz a mesma. Um «não deu para chegar lá» aqui quer dizer que os ficheiros não
   foram anexados à release.
3. Abrir um alimento que se cozinhe — carne, peixe, legumes — e ver o cartão «e se for
   cozinhado?». Metade do catálogo não o tem, e é de propósito.
4. Lançar, com os quatro APK **e** os dois ficheiros do catálogo.

## As decisões que ficaram para o dono

São de conteúdo, e a oficina é onde se tomam (`node tools/oficina/servidor.mjs`):

- **65 colisões de nome**, 23 delas a discordar na energia: a batata assada a 91 kcal numa
  fonte e 159 noutra, a sangria a 89 contra 120. O separador «duplicados» mostra as duas
  linhas lado a lado; escolher uma escreve a fusão e deixa lápide para os favoritos seguirem.
- **3 767 alimentos ainda em inglês.** O separador «segmentos» mostra o que falta traduzir por
  quantos alimentos cada um desbloqueia — traduzir um segmento arruma todos os que esperam
  por ele. Medido: 930 termos dão 30% dos nomes, 2 500 dão 65%, 3 500 dão 84%.
- **252 suspeitas do motor de qualidade**, das quais 16 são duas fontes a discordar sobre o
  mesmo alimento — o arroz selvagem cru a 344 kcal aqui e 101 na USDA.
- **12 contradições aceites**: números impossíveis que a construção deixa passar por estarem
  declarados. Cada uma é um alimento por corrigir.

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
