# A dívida com o estudo

Este ficheiro existe porque construí treze versões sem abrir o estudo.

Não é um resumo do estudo. **É o contrário de um resumo**: é a lista do que tem de ser
aberto, quando, e o que fazer com o que lá estiver. Um resumo foi exactamente o que me pôs
aqui — trabalhei a partir das linhas de uma página e chamei-lhe seguir o plano.

---

## O que aconteceu, e como se sabe

A `estudo/` tem **63 documentos**: 20 de área, 17 esboços visuais, 8 do motor, 5 do sistema,
5 transversais, 4 de dados, 3 de propostas, 1 de ausências — mais o `LEIA-ME.md`, o
`metodo.md` e o `PLANO-DE-PRODUCAO.md`.

**Nunca abri nenhum deles**, tirando o plano. Nem um esboço, em nenhuma das treze versões.

A causa não é esquecimento, é desenho — meu:

| Ficheiro | Menções ao estudo ou aos esboços |
|---|---|
| `docs/referencia/como-continuar.md` | **0** |
| `docs/referencia/regras.md` | **0** |
| A memória entre sessões | **0** |

Escrevi para mim próprio um sistema de continuidade que aponta para as linhas-resumo do
plano e não aponta para o estudo. Funcionou como foi construído.

### O caso provado

Único que verifiquei de facto, a 2026-08-28, depois de o dono perguntar. Abri o
[`estudo/esbocos/05-receitas-e-modelos.html`](../../estudo/esbocos/05-receitas-e-modelos.html)
pela primeira vez, **depois** de lançar a 2.18.0, que é a versão que ele desenha.

| O que o esboço propõe | O que eu construí |
|---|---|
| «Uma lista só», com a origem escrita na linha | Duas secções com títulos separados |
| Sítio próprio, alcançável do «Eu» e do diário — **não** dentro da pesquisa | Ficou na pesquisa, com atalho no diário |
| Multiplicador em chips ×0,5 ×1 ×1,5 ×2 | Campo de texto |
| A linha diz a origem: «guardada do diário» | A linha diz a refeição do dia |
| «Não se pode editar um modelo» está na tabela de problemas | Não construí a edição |
| O aviso do rendimento com a tolerância e o tom do aviso do rótulo | Inventei um envelope de métodos |

E o que disse ao dono — «receita deixa de ser palavra errada assim que tiver passos» — o
esboço contradiz: diz que faltam **passos, tempo e fotografia**, e avisa que isso é «uma app
dentro da app».

**As três decisões que ele me delegou nesse dia já estavam respondidas no esboço.** Respondi
duas ao contrário, e apresentei o raciocínio como meu.

---

## A regra que passa a existir

> **C5 · Antes de mexer numa área, abre os documentos dela.** O plano diz o que a versão
> traz; o estudo diz porquê e com que forma. Trabalhar da linha do plano é trabalhar de um
> resumo, e treze versões saíram assim.

Verifica-se por escrito: **o registo da versão em `estudo/PLANO-DE-PRODUCAO.md` nomeia os
ficheiros abertos.** Uma versão cujo registo não os nomeia não foi aberta com o estudo, e
isso vê-se sem confiar na memória de ninguém.

**O que conta como aberto:** ler o documento inteiro, não procurar uma palavra dentro dele.
Um `grep` responde à pergunta que eu já tinha; o documento responde às que eu não sabia
fazer — e foi disso que a 2.18.0 ficou sem.

---

## Rota: o que abrir, para cada coisa

Esta tabela não diz o que os documentos contêm. Diz **quais abrir**. O conteúdo abre-se.

### Sempre, em qualquer versão

| Ficheiro | Porque é sempre |
|---|---|
| [`estudo/PLANO-DE-PRODUCAO.md`](../../estudo/PLANO-DE-PRODUCAO.md), a entrada da versão | traz o conteúdo e as perguntas de abertura |
| [`estudo/propostas/00-o-custo-de-mudar.md`](../../estudo/propostas/00-o-custo-de-mudar.md) | diz quais propostas baixam o custo de mexer e qual tem imposto permanente. **O estudo diz para o ler antes de decidir o que fazer**, e eu nunca o li |
| [`estudo/transversal/02-robustez.md`](../../estudo/transversal/02-robustez.md) e [`03-acessibilidade.md`](../../estudo/transversal/03-acessibilidade.md) | atravessam tudo o que tem ecrã |

### Por bloco do plano

| Bloco | Abrir |
|---|---|
| **A · a dívida**, **B · o risco** | `estudo/transversal/02`, `estudo/transversal/04-longevidade.md`, `estudo/dados/01`, `estudo/dados/02`, `estudo/dados/03` |
| **C · a linguagem** | `estudo/areas/20-navegacao-e-sistema-de-desenho.md` + esboço `20-sistema-de-desenho` |
| **D · o catálogo** | `estudo/dados/04-as-fontes-de-dados.md`, `estudo/propostas/02-o-catalogo.md` + esboço `22-catalogo` |
| **E · a comida** | `estudo/areas/03`, `estudo/areas/04`, `estudo/areas/05`, `estudo/areas/13` + esboços `03`, `05` |
| **F · o treino** | `estudo/areas/06`, `07`, `08`, `09`, `10` + esboços `06`, `07`, `08`, `10`; e `estudo/motor/05-exercicio-e-gasto.md` |
| **G · a corrida** | `estudo/areas/11-corrida.md` + esboço `11`; `estudo/sistema/03-notificacoes-e-fundo.md` |
| **H · os dois ecrãs** | `estudo/areas/01`, `estudo/areas/02` + esboços `01`, `02` |
| **I · o corpo** | `estudo/areas/14`, `estudo/areas/15`, `estudo/areas/16` + esboços `14`, `15`; `estudo/motor/03`, `estudo/motor/04`, `estudo/motor/07` |
| **J · a casa arrumada** | `estudo/areas/16`, `17`, `18`, `19` + esboços `16`, `18`; `estudo/transversal/01-duas-pessoas.md`, `estudo/transversal/05` |
| **K · as ideias novas** | `estudo/areas/12-jejum.md` + esboço `12`; `estudo/motor/06`, `estudo/motor/07`; `estudo/transversal/05-instrumento-pessoal.md` |
| **L · grupos** | `estudo/propostas/01-grupos.md` + esboço `21-grupos`; `estudo/sistema/01-base-de-dados.md`, `estudo/sistema/02-servidor-e-custo.md` |

### Por tipo de trabalho, seja qual for a versão

| Se a versão mexe em… | Abrir |
|---|---|
| uma conta, uma meta, uma projeção | `estudo/motor/00-mapa-do-motor.md` e o documento do motor da conta |
| esquema da base, migração | `estudo/sistema/01-base-de-dados.md` |
| a AI ou uma função de servidor | `estudo/sistema/02-servidor-e-custo.md`, `estudo/areas/04-comida-por-ia.md` |
| notificações, widget, trabalho em fundo | `estudo/sistema/03-notificacoes-e-fundo.md` |
| Health Connect, importações | `estudo/sistema/04-integracoes.md` |
| listas grandes, arranque, memória | `estudo/sistema/05-desempenho.md` |
| exportar, apagar, cópia de segurança | `estudo/dados/01`, `estudo/dados/02`, `estudo/dados/03` |
| o catálogo de alimentos | `estudo/dados/04`, `estudo/propostas/02` |
| qualquer ecrã | o `estudo/areas/NN` correspondente **e o esboço, quando existe** |

### As 19 versões que citam um esboço

Contadas no plano. **Quando a entrada cita um esboço, o esboço é obrigatório**, e é ele que
decide a forma:

`2.3.0` → 20 · `2.10.0` `2.11.0` `2.15.0` → 22 · `2.16.0` → 03 · `2.18.0` → 05 ·
`2.20.0` → 06 · `2.21.0` → 08 · `2.23.0` → 07 · `2.25.0` → 10 · `2.29.0` → 11 ·
`2.32.0` → 01 · `2.33.0` → 02 · `2.36.0` → 14 · `2.37.0` → 15 · `2.39.0` → 16 ·
`2.40.0` → 18 · `2.46.0` → 12 · `3.0.0` → 21

**Uma versão sem esboço citado não fica sem leitura** — fica com o `estudo/areas/NN` da tabela por
bloco. A 2.17.0 é o exemplo: não cita esboço, e tem o `estudo/areas/04-comida-por-ia.md` que eu
também não abri.

---

## O que já se sabe que ficou de fora

A lista por versão, com o que foi medido separado do que ainda não foi lido, está em
[`o-que-ficou-de-fora.md`](o-que-ficou-de-fora.md). Este guia diz o que abrir; esse diz o
que falta.

## A auditoria do que já saiu

Treze versões do plano saíram, mais a metade partida 2.18.1. **Nenhuma foi construída com o
estudo aberto.** Isto é a lista de trabalho, não um veredicto: para cada uma, abrir os
documentos e escrever o que divergiu — sem refazer nada antes de o dono ver a lista.

**A ordem é a do custo de corrigir depois, e não a numérica.** Uma divergência de esquema ou
de conceito fica mais cara a cada versão que passa por cima; uma divergência de ecrã custa o
mesmo hoje e daqui a um ano.

| # | Versão | Abrir | Comparar com |
|---|---|---|---|
| 1 | **2.18.0 · 2.18.1** | esboço `05`, `estudo/areas/05` | `FoodSearchScreen`, `RecipeEditScreen`, `MealTemplateRepository` — **as divergências já estão escritas acima**; falta o `estudo/areas/05` |
| 2 | **2.16.0** | esboço `03`, `estudo/areas/03` | `FoodSearchScreen`, `FoodSearchViewModel` |
| 3 | **2.17.0** | `estudo/areas/04-comida-por-ia.md` | `AiSheet`, `AiViewModel`, `AiRepository` |
| 4 | **2.4.0 → 2.8.0** (bloco D) | `estudo/dados/04`, `estudo/propostas/02`, esboço `22` | `tools/catalogo/`, `FoodSeeder`, `RecipeCalc` |
| 5 | **2.3.0** | esboço `20`, `estudo/areas/20` | `composeApp/src/commonMain/kotlin/pt/antares/app/core/designsystem/` |
| 6 | **2.1.0 · 2.2.0** | `estudo/dados/01`, `estudo/dados/02`, `estudo/dados/03` | `composeApp/src/commonMain/kotlin/pt/antares/app/core/privacy/`, `DestinosScreen` |
| 7 | **2.0.3 · 2.0.4** | `estudo/transversal/02`, `estudo/motor/01`, `estudo/motor/03` | `composeApp/src/commonMain/kotlin/pt/antares/app/core/calc/` |

**Formato de cada entrada**, escrita por baixo da versão no plano:

```
### Releitura com o estudo — AAAA-MM-DD
Abertos: estudo/areas/05-receitas-e-modelos.md, estudo/esbocos/05-...html
Divergências:
  - [forma] o que o estudo propõe → o que está construído → custo de corrigir
  - [omissão] o que o estudo pede e não existe → custo
  - [decidido ao contrário] o que eu decidi contra o estudo, e a razão que dei na altura
Nada a corrigir em: …
```

**«Decidido ao contrário» não é automaticamente um erro.** O estudo é um menu e o dono manda.
O erro foi decidir contra ele **sem saber que estava a decidir contra ele**, e apresentar a
razão como se fosse a única.

---

## As dez correções baratas do estudo — contagem de hoje

O `LEIA-ME.md` fecha com dez correções «que a app precisa e que custam horas». Contei-as no
código a 2026-08-28. **Isto é uma verificação por `grep`, não uma leitura** — serve para
saber o que já não é dívida, e não substitui abrir o documento de cada uma:

| # | O quê | Estado, verificado |
|---|---|---|
| 1 | Tipografia incompleta | **feito** — o `Type.kt` aplica as famílias próprias por estilo |
| 2 | Instrução da cintura por sexo | **por verificar** — o texto já não fala do umbigo, mas também não distingue sexo. Ler `estudo/areas/16` |
| 3 | A voz vai para a pesquisa | **feito** na 2.17.0 |
| 4 | Dois controlos que não fazem nada | **um deles resolvido** — a janela da tendência chega ao `WeightTrend`. O outro por verificar |
| 5 | Fotografias fora da cópia automática | **resolvido de outra maneira** — a cópia da Google está desligada e as fotos de progresso vão no ZIP. **Mas ver o aviso abaixo** |
| 6 | `updateSet` é código morto | **feito** — chamado do `WorkoutSessionScreen` |
| 7 | 111 exercícios não se registam | **aberto** — é a 2.22.0 |
| 8 | A retenção do ciclo não sai do ecrã do ciclo | **aberto** — é a 2.34.0 |
| 9 | Centro de treino 7 → 16 | **aberto** — é a 2.20.0 |
| 10 | Imagens dos exercícios em repositório de terceiros | **aberto** — o `ExerciseSeeder` ainda aponta para `raw.githubusercontent.com` |

### Um aviso que nasceu da minha própria mão

A correção 5 do estudo chama-se **«o único dado irrecuperável é o único desprotegido»**.

Na 2.17.0 pus a fotografia do prato **deliberadamente fora da cópia de segurança**, com a
razão escrita — mil imagens por ano em cinco cópias que rodam. A razão continua a parecer-me
boa. Mas é a mesma forma de risco que o estudo nomeou, criada de novo por mim, sem eu ter
lido a página onde ele a nomeia.

**Reabrir com o `estudo/dados/02-perder-tudo.md` aberto**, e decidir outra vez com o argumento dele
à frente. É a primeira coisa a fazer desta lista, porque é a única que já está publicada.

---

## O que este guia **não** manda fazer

O `LEIA-ME.md` do estudo é explícito, e vale mais do que a minha vontade de corrigir tudo:

- São **215 propostas com veredicto** — 155 «sim», 41 «talvez», 19 «não». As colunas «talvez»
  e «não» existem para não serem todas «sim».
- Implementá-las todas levaria a app de 51 para ~70 ecrãs, e **o modo de falha real de um
  projeto de uma pessoa não é um defeito: é o abandono.**
- O subconjunto que o próprio estudo defenderia são **trinta**, e estão em
  [`estudo/propostas/00`](../../estudo/propostas/00-o-custo-de-mudar.md) com a razão de cada corte.

Portanto:

1. **Nada se refaz sem a lista na mão e o dono a ver.** A auditoria produz uma lista de
   divergências com custo ao lado; o que se corrige é decisão dele, versão a versão (A1).
2. **As correcções entram como versões**, não como remendos entre versões. Uma divergência
   que caiba numa versão do plano corrige-se lá; uma que não caiba pede número próprio, e
   pedir número próprio é pedir autorização.
3. **A5 continua a valer:** uma correcção que entre a meio é troca, não adição.

---

## Como se sabe que isto não volta a acontecer

Três coisas, e nenhuma delas é a minha intenção:

1. **A regra C5** em [`regras.md`](regras.md), com quem a verifica.
2. **O `como-continuar.md` manda abrir o estudo** antes das perguntas de abertura — é o
   ficheiro que se lê ao começar qualquer sessão.
3. **O registo de cada versão nomeia os ficheiros abertos.** Se não os nomear, não foram
   abertos. É verificável por quem vier a seguir, incluindo por mim daqui a uma semana, sem
   depender de eu me lembrar.

O que falhou não foi disciplina no momento — foi um sistema de continuidade que eu próprio
escrevi e onde o estudo não existia.
