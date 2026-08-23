# Como continuar

Este ficheiro é o que se lê ao começar uma sessão nova. Substitui a mensagem que antes era
reconstruída de memória a cada vez — e que, sendo reconstruída, envelhecia sem ninguém notar.

**Actualiza-se ao fim de cada versão**, no mesmo commit que a publica.

## Onde estamos

- **2.6.0 é a última publicada.** Esquema da base **v28**. Oito versões feitas.
- A **2.7.0 está a meio, comitada e por lançar.** A árvore está verde: 1433 testes, detekt
  limpo, nada por empurrar. O catálogo no repositório já é a **versão 3**, e a 2.6.0 publicada
  traz a 2 — não se lança nada a partir daqui sem acabar a versão.
- A **2.5.1 foi fechada sem sair.** A razão está no plano e não se reabre sem a ler.

## O bloco D faz-se em três corridas

Por decisão do dono, a 2026-08-23. Uma corrida é um plano, uma execução e **um lançamento** —
em vez de uma versão de cada vez. A cerimónia por versão era perto de um terço do trabalho.

| Corrida | O quê | Estado |
|---|---|---|
| **1 · o encanamento** | 2.7.0, com a 2.8.0 absorvida | **a meio** |
| **2 · as ferramentas** | 2.9.0 + 2.10.0 | por abrir |
| **3 · o conteúdo** | 2.11.0 a 2.15.0 | por abrir |

**O que não se junta é o conteúdo com o código.** As decisões sobre nomes, fusões e porções
são do dono; tomá-las em lote sem ele as ver é o modo de falhar deste bloco — um alimento com
o nome trocado não rebenta, não dá erro, e custa uma versão a corrigir.

**O que não se batcha nunca:** a medição no início de cada peça. Cinco versões abertas, cinco
premissas do plano erradas, e as cinco apareceram a medir — não a ler.

## O que falta na corrida 1

A ausência tipada está feita. Falta **o catálogo que se atualiza sozinho**, e as quatro
decisões já estão tomadas:

1. **Alojado nas releases do GitHub**, ao lado da versão que o produziu.
2. **Só desce a pedido, num botão.** A app nunca vai à rede por causa do catálogo sem alguém
   lhe pedir.
3. **Não avisa que um alimento mudou de números.** O diário copia a nutrição no momento do
   registo: os dias passados não mudam.
4. **Entra no ecrã «O que sai daqui»**, com o que se envia — o endereço IP e a versão
   instalada.

O passo a passo está no plano, debaixo da 2.8.0. Depois disso: changelog, changelog da app,
subir a versão para 2.7.0, aparelho, e lançar.

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

Uma chamada: testes Kotlin com os nomes dos que falham, detekt, lint, funções do servidor e as
duas buscas de segredos. Cinco linhas e um veredicto. Apaga os relatórios antigos antes de
correr — sem isso, uma execução que nem arranca deixa a anterior a passar por verdade.

## O catálogo

Construído fora da app:

```bash
node tools/catalogo/construir.mjs
```

Determinístico, e **chumba se perder um alimento** que a fonte declare e não esteja em
`desvios.json`. A versão sobe à mão em **dois** sítios — no `construir.mjs` e no `FoodSeeder`
— e há um teste-guarda a exigir que sejam a mesma. Ver [`tools/README.md`](../../tools/README.md).

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
