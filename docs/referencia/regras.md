# Regras de trabalho

As trinta regras que governam a produção da Antares. Escolhidas item a item
pelo dono a **2026-08-18**, antes de a produção começar.

Cada uma diz **quem a verifica**, porque uma regra que ninguém verifica é
decoração:

| | Quem verifica |
|---|---|
| **teste** | há um teste-guarda que a cobra — ver [Testes-guarda](testes-guarda.md) |
| **hook** | um *hook* do git trava a ação — ver `.githooks/`
| **por automatizar** | podia ter defesa, e não tem |
| **eu** | disciplina de quem escreve o código |
| **tu** | só o dono pode decidir se foi cumprida |

---

## A · Como se decide o que se faz

| # | Regra | Verifica |
|---|---|:---:|
| **A1** | **Nada começa sem conversa.** Nenhuma linha de código sai do plano sem o dono mandar, versão a versão. **O plano não é autorização.** | tu |
| **A2** | **Cada versão abre com perguntas.** Antes de qualquer trabalho: se se faz de todo — o «não» é resposta aceite e passa-se à seguinte — como se faz, e quanto se faz. As perguntas estão pré-escritas em cada versão do plano. | tu |
| **A3** | **Uma de cada vez, publicada e usada.** Cada versão vai para o telemóvel e vive lá umas semanas antes da seguinte. O que se aprende a usar muda o que vem a seguir. | tu |
| **A4** | **Uma versão recusada não renumera as seguintes.** Um «não» à 2.9.0 mata o número; a seguinte continua a ser 2.10.0. Mantém estáveis as referências do plano e do git, e deixa rasto de que houve ali uma decisão. | eu |
| **A5** | **Nada entra a meio sem sair outra coisa.** Uma ideia nova durante a produção é **troca**, não adição. É o que impede 51 versões de virarem 70. | tu |

> A regra que **foi retirada** a 2026-08-18: *«saltar é normal»*. O plano deixou de
> ser um menu e passou a fazer-se por ordem. Dizer «não» a uma versão continua a
> ser aceite (A2) — o que acabou foi andar para trás e para a frente a escolher.

---

## B · Como se corta o trabalho

| # | Regra | Verifica |
|---|---|:---:|
| **B1** | **Versões pequenas.** Cada uma são poucos serões. Uma que demore um mês foi mal cortada. | eu |
| **B2** | **Versão que cresce a meio parte-se.** Quando uma se revela grande já a caminho, corta-se em duas e a segunda entra a seguir — em vez de arrastar. | eu |
| **B3** | **Um conceito novo de cada vez.** Nunca dois em aberto. São nove no plano, marcados ⭐ e espaçados de propósito: nunca dois seguidos. | eu |
| **B4** | **A ordem baixa o custo.** As primeiras versões tornam a app mais barata de mexer. Construir por cima de dívida é pagar duas vezes. | eu |
| **B5** | **Estrutura antes de conteúdo.** Dentro de um bloco, primeiro decide-se **o que existe**, só depois se enche. É a que impede a curadoria manual de se fazer duas vezes. | eu |
| **B6** | **Partir a tarefa em partes verdes.** Uma tarefa nunca se faz inteira de uma vez: parte-se, e **cada parte deixa a suite verde**. | teste |

---

## C · Antes de agir

| # | Regra | Verifica |
|---|---|:---:|
| **C1** | **Confirmar o achado antes de o corrigir.** Seis achados do plano anterior estavam errados, e foram verificados no código antes de alguém agir. | eu |
| **C2** | **Ler os comentários do ficheiro antes de o mudar.** Neste código os comentários são o registo de decisões já tomadas. Sete auto-correções do estudo vieram de não os ter lido — várias vezes descreviam exatamente o defeito que se ia reportar. | eu |
| **C3** | **Nunca escrever um número que não se contou nessa resposta.** | eu |
| **C4** | **Número do catálogo que se cita, reconta-se.** Estende a C3 aos documentos. Citá-los desatualizados é exatamente o que pôs este repositório no estado que obrigou ao estudo. | teste |
| **C5** | **Antes de mexer numa área, abrir os documentos dela em `estudo/`** — o de área, o esboço quando existe, e os do motor ou do sistema que a versão tocar. O plano diz o que a versão traz; o estudo diz porquê e com que forma. **Treze versões saíram sem isto**, e a rota está em [`a-divida-com-o-estudo.md`](a-divida-com-o-estudo.md). Verifica-se por escrito: o registo da versão no plano **nomeia os ficheiros abertos**. | eu |

---

## D · Provar que funciona

| # | Regra | Verifica |
|---|---|:---:|
| **D1** | **Ler o relatório dos testes, não a última linha do Gradle.** Com `--rerun-tasks` para `.md`, recursos e esquemas. | eu |
| **D2** | **Um teste-guarda a falhar é uma decisão desfeita**, não um teste para arranjar. A pergunta certa nunca é como o fazer passar. | teste |
| **D3** | **Correr a app, não só os testes.** Nenhuma versão fecha sem a app correr no telemóvel. As duas últimas correções saíram de a usar, e a suite verde não as tinha apanhado. | tu |
| **D4** | **CI verde antes de fechar a versão.** Verificar no GitHub, não só a suite local. Esteve vermelho meses sem ninguém saber. | por automatizar |
| **D5** | **Migração de esquema exige um teste com dados reais.** Nenhuma migração entra sem um teste que a corra sobre uma base cheia. | teste |
| **D6** | **Varrer antes de publicar, não depois.** Antes de etiquetar: os testes-guarda novos estão documentados? Os números que os documentos citam ainda são verdade? Os comandos dos guias ainda são os que o CI corre? **Um defeito que pertence a esta versão e fica por corrigir não passa a pertencer à seguinte.** | eu |

---

## E · O que fica escrito

| # | Regra | Verifica |
|---|---|:---:|
| **E1** | **Um commit por tarefa, em português, com o problema antes da solução.** O `git log` é o registo do que foi decidido e do que foi rejeitado. | eu |
| **E2** | **Comentários acrescentam um facto que não está na linha.** Densidade alta, filtro absoluto. Nada de narrar o percurso nem de repetir o código. | eu |
| **E3** | **Dúvidas nunca ficam no código.** Vão para uma lista à parte, para o dono responder de uma vez. | teste |
| **E4** | **As respostas às perguntas de abertura ficam escritas.** Uma linha por decisão em [`docs/explicacao/decisoes/`](../explicacao/decisoes/). São 51 versões ao longo de meses: sem registo, as decisões evaporam-se. | eu |
| **E5** | **O que se recusa fica escrito, com a razão.** Sem registo do «não» e do porquê, uma ideia recusada volta daqui a seis meses e discute-se do zero. | eu |
| **E6** | **O CHANGELOG escreve-se dentro da versão**, nunca depois — o `CHANGELOG.md` e o `AppChangelog.kt` no mesmo trabalho. | teste |

---

## F · Segurança

| # | Regra | Verifica |
|---|---|:---:|
| **F1** | **Os dois greps de segredos correm antes de qualquer push** — chaves de API e tokens JWT, em todos os ficheiros seguidos pelo git. Vive em `.githooks/pre-push`; instala-se com `git config core.hooksPath .githooks`. | hook |
| **F2** | **O código de administração nunca entra em ficheiro nenhum.** Pede-se ao dono quando for preciso. | eu |

---

## O que falta automatizar

Quatro das cinco foram feitas a 2026-08-18, antes de a produção começar. Sobra uma:

| Regra | O que a defenderia |
|---|---|
| **D4** · CI verde | um passo no guia de lançamento que consulte o estado pelo `gh` |

Fica por fazer porque **é a única que depende de uma coisa fora do repositório** — o
estado de uma execução no GitHub, que um teste local não consegue consultar sem rede.

## Porquê tantas

Trinta é muito, e o risco é conhecido: uma lista longa lê-se uma vez e nunca
mais. A defesa é a coluna da direita.

Este repositório já provou qual é o filtro. Há **64 testes-guarda** documentados em
[Testes-guarda](testes-guarda.md), e foram eles — não os documentos — que fizeram
as decisões sobreviver a meses de trabalho. As regras marcadas **teste** cuidam-se
sozinhas. As marcadas **tu** são decisões, e por isso não podem ser automatizadas.
As marcadas **eu** são as frágeis, e a única defesa que têm é serem poucas e
concretas.
