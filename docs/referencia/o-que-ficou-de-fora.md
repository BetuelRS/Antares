# O que ficou de fora, versão a versão

Companheiro de [`a-divida-com-o-estudo.md`](a-divida-com-o-estudo.md). Aquele diz **o que
abrir**; este diz **o que já se sabe que falta**.

**Lê-se com uma desconfiança:** eu escrevi-o sem ter aberto a maior parte do estudo. Por isso
está partido em duas metades que não se misturam.

| | O que é | O que vale |
|---|---|---|
| **Parte A** | medido no código a 2026-08-28, com o comando ao lado | é verdade hoje |
| **Parte B** | o que ainda não foi lido | é uma lista de trabalho, **não** um veredicto |

Uma linha da Parte B nunca se cita como se fosse um achado. Enquanto o documento do estudo
não for aberto, a única coisa honesta que se pode dizer é «não sei».

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

**Uma por verificar: a instrução da cintura.** O estudo diz que «ao nível do umbigo» está
errado para mulheres e invalida o erro-padrão que a app declara. Hoje o
`bodycomp_measure_hint` diz *«Mede de manhã, relaxado, com a fita justa mas sem apertar»* —
**já não fala do umbigo, e também não distingue sexo**. Pode ser a correção certa ou pode ser
a instrução a perder a referência anatómica toda.

→ Decide-se com o `estudo/areas/16-perfil-corpo-e-metas.md` aberto. **Não decidir sem isso.**

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

→ **Reabrir com o `estudo/dados/02-perder-tudo.md` aberto.** É o item mais urgente desta
lista, porque é o único já publicado.

## 2.2.0 · O que sai daqui

**Ficou de fora, e por decisão escrita: a frase do arranque.** O registo no plano di-lo:
«a frase do arranque fica para a 2.40.0, por decisão do dono», com a consequência assumida —
**o primeiro ecrã continua a dizer «Nada sai do telemóvel» até lá**, e a app contacta a Open
Food Facts.

Não é uma falha minha; é uma dívida com data. Tem casa: a 2.40.0.

## 2.3.0 · Uma linguagem só

**Nada ficou de fora segundo o registo** — entraram as seis peças e a migração foi toda de
uma vez. O esboço `20-sistema-de-desenho` **não foi aberto**, e é a versão que ele desenha.

## 2.6.0 · O vocabulário — **o maior buraco medido**

Duas metades, e só uma entrou.

**Entrou, e bem:** o `tools/catalogo/vocabulario.mjs` existe, o `construir.mjs` chumba se um
importador emitir uma chave não declarada, e as referências da EFSA são cruzadas com o
`seed_efsa_drv.csv` em vez de escritas de cabeça — o próprio ficheiro conta que isso já
tinha falhado uma vez, com o zinco.

**Não entrou: os oito nutrientes novos.** Nenhum deles.

| Prometido | No catálogo |
|---|---|
| energia em **kJ** — obrigatória nos rótulos da UE | não |
| **sal**, distinto do sódio, que é o que se lê na embalagem | não — só `sodium_mg` |
| **biotina** | não |
| **crómio** | não |
| **molibdénio** | não |
| **flúor** | não |
| **cafeína** | não |
| **frutose** | não |

Medido: o catálogo tem **40 chaves de micronutriente** em 7 932 alimentos, e o
`Nutrients.kt` declara **42 chaves** — que é exactamente o número que o plano registou como
o **«hoje»**, antes da versão. O contador não se moveu.

## 2.7.0 · A ausência tipada

**Quatro dos seis estados do EuroFIR estão representados, dois não.**

| Estado | Como está |
|---|---|
| `medido` | o número nu — é o formato compacto que o plano descreve |
| `nao_medido` | a ausência da chave |
| `vestigios` | `EstadoDeNutriente.Vestigios` |
| `abaixo_do_limite` | `EstadoDeNutriente.AbaixoDoLimite(limite)` |
| **`nao_se_aplica`** | **em falta** |
| **`assumido_zero`** | **em falta** |

Os dois que faltam são precisamente os que distinguem «este alimento não tem esta coisa por
natureza» de «ninguém mediu». Hoje caem os dois na mesma ausência — que é a forma de falhar
que a versão existia para acabar.

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

**Nada medido em falta** contra as linhas do plano. O esboço `03-adicionar-comida` **não foi
aberto** — e é a versão que ele desenha.

## 2.17.0 · A revisão da IA

**Nada em falta** contra as seis promessas. Duas notas:

- O `estudo/areas/04-comida-por-ia.md` **não foi aberto**, e esta versão não cita esboço —
  é o documento de área que decide.
- A fotografia do prato ficou fora da cópia de segurança. Ver a 2.1.0 acima.

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

O `estudo/areas/05-receitas-e-modelos.md` **continua por abrir** — só li o esboço.

---

# Parte B — por ler

Nada aqui é um achado. É a lista dos documentos que decidem cada versão já publicada e que
nunca foram abertos.

| Versão | Por abrir |
|---|---|
| 2.0.3 · 2.0.4 | `estudo/transversal/02-robustez.md`, `estudo/motor/01-metabolismo-e-metas.md`, `estudo/motor/03-peso-tendencia-e-projecao.md`, `estudo/areas/16-perfil-corpo-e-metas.md` |
| 2.1.0 · 2.2.0 | `estudo/dados/01-o-que-sai-do-telemovel.md`, `estudo/dados/02-perder-tudo.md`, `estudo/dados/03-sincronizacao-caseira.md`, `estudo/transversal/04-longevidade.md` |
| 2.3.0 | `estudo/areas/20-navegacao-e-sistema-de-desenho.md` + esboço `20` |
| 2.4.0 → 2.8.0 | `estudo/dados/04-as-fontes-de-dados.md`, `estudo/propostas/02-o-catalogo.md` + esboço `22` |
| 2.16.0 | `estudo/areas/03-adicionar-comida.md` + esboço `03` |
| 2.17.0 | `estudo/areas/04-comida-por-ia.md`, `estudo/sistema/02-servidor-e-custo.md` |
| 2.18.0 · 2.18.1 | `estudo/areas/05-receitas-e-modelos.md` (o esboço já foi lido) |
| **todas** | `estudo/propostas/00-o-custo-de-mudar.md` — o estudo diz para o ler **antes de decidir o que fazer**, e nunca foi aberto |

---

# O que fica em aberto, junto

Só o que está na Parte A. Cada linha precisa de autorização para virar trabalho (A1).

| O quê | Onde nasceu | Casa provável |
|---|---|---|
| **A foto do prato fora da cópia de segurança** | 2.17.0, minha decisão | reabrir já, com `estudo/dados/02` |
| **Os oito nutrientes** — kJ, sal, biotina, crómio, molibdénio, flúor, cafeína, frutose | 2.6.0 | número próprio; toca no catálogo e no vocabulário |
| **`nao_se_aplica` e `assumido_zero`** | 2.7.0 | número próprio, pequeno |
| **«Uma lista só»** e o sítio próprio das refeições | 2.18.0, contra o esboço | 2.18.2, ou a seguir à releitura |
| **Editar uma refeição guardada** | 2.18.0, omissão | com a anterior |
| **Multiplicador em chips**, origem na linha | 2.18.0, contra o esboço | com a anterior |
| **A instrução da cintura por sexo** | 2.0.4, por verificar | ler `estudo/areas/16` antes de decidir se é dívida |
| **2 909 alimentos em inglês** | 2.13.0 | trabalho de meses, na oficina |
| **73,7 % do catálogo sem porção** | 2.14.0 | idem |
| **111 exercícios que não se registam** | dez do estudo, #7 | 2.22.0, já no plano |
| **A retenção do ciclo não sai do ecrã** | dez do estudo, #8 | 2.34.0, já no plano |
| **Centro de treino 7 → 16** | dez do estudo, #9 | 2.20.0, já no plano |
| **Imagens dos exercícios num repositório de terceiros** | dez do estudo, #10 | provavelmente 2.27.0 — confirmar com `estudo/areas/09-treino-biblioteca.md` |
| **A frase do arranque** | 2.2.0, adiada pelo dono | 2.40.0, com data |

**Não se corrige nada desta tabela por iniciativa minha.** Ela existe para o dono escolher, e
a regra A5 continua a valer: o que entra a meio é troca, não adição.

---

## Como manter isto honesto

- Uma linha só passa da Parte B para a Parte A **depois de o documento ser aberto**.
- Um número escrito aqui reconta-se antes de se voltar a citar (C3, C4).
- Quando uma versão nova sair, o que ela fechar risca-se daqui **no mesmo commit** — senão
  este documento envelhece exactamente como o `como-continuar.md` envelhecia antes de existir.
