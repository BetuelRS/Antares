# Versionamento

A Antares segue [Semantic Versioning 2.0.0](https://semver.org/). Três números, nunca quatro.

## Quando sobe cada número

| | Critério | Exemplo |
|---|---|---|
| **MAJOR** | quem atualiza perde uma coisa com que contava | a conta e a sincronização saíram, na 1.0.0 |
| **MINOR** | ganha alguma coisa | o separador Progresso, na 0.13.0 |
| **PATCH** | só se corrige | — |

O critério do MAJOR não é «está pronto» nem «é importante». É uma pergunta: *alguém que atualize
perde uma coisa com que contava?*

Arrumar código, acrescentar testes ou escrever documentação **não sobe número nenhum**. Fica em
`[Unreleased]` no [CHANGELOG.md](../../CHANGELOG.md).

## Uma versão que se parte a meio

A regra B2 manda partir uma versão que cresce depois de aberta. Quando isso acontece, **a
segunda metade fica no terceiro número** — 2.5.0 e 2.5.1 — mesmo quando traz capacidade nova
e o critério de cima pediria um MINOR.

É uma excepção, e a razão é aritmética: inserir um MINOR a meio obriga a deslocar todas as
versões seguintes do plano, e são mais de cem referências cruzadas. Cada número reescrito é um
número que passa a ter de ser defendido, e a regra C3 diz que não se escreve um número que não
se contou. O ganho de pureza não paga o risco.

**O que não muda:** as duas metades são uma versão só no changelog de quem usa a app, e a
segunda diz de onde veio. Um MAJOR nunca se parte assim — quem perde uma coisa com que contava
tem de o ler no primeiro número.

Usada duas vezes:

| | Quando | O que aconteceu |
|---|---|---|
| **2.5.1** | 2026-08-22 | a segunda metade foi aberta, medida e **fechada sem sair** — a excepção de numeração foi usada, o número não chegou a ser publicado |
| **2.18.1** | 2026-08-28 | a segunda metade saiu, e **saiu no mesmo lançamento que a primeira** |

**A segunda vez mostra o limite da excepção.** A 2.18.0 e a 2.18.1 foram para o mesmo APK: o
`versionName` saltou de `2.17.0` para `2.18.1`, e não há etiqueta `v2.18.0`. **Partir uma
versão é adiar a segunda metade, não renomear a primeira** — a B2 diz «a segunda entra a
seguir». Quando as duas metades saem juntas, o que existiu foi uma versão só, e o número do
meio é uma entrada de changelog sem artefacto por baixo.

## A fórmula do versionCode

O Android exige um inteiro que cresça sempre. Deriva do nome, em `composeApp/build.gradle.kts`:

```kotlin
versionCode = major * 10_000 + minor * 100 + patch
```

| versionName | versionCode |
|---|---|
| `1.0.0` | `10000` |
| `1.2.3` | `10203` |
| `2.0.0` | `20000` |

Lê-se ao contrário sem consultar nada, e cabem 99 versões em cada casa. O `build.gradle.kts`
recusa uma versão que não tenha três segmentos numéricos, ou cujo `minor`/`patch` não caiba em
duas casas.

Antes da 1.0.0 o `versionCode` era contado à mão e ia em 66. Como `10000 > 66`, a mudança de
esquema manteve a subida monótona — que é a única coisa que o Android exige.

## Os dois changelogs

| Ficheiro | Para quem | Conteúdo |
|---|---|---|
| [CHANGELOG.md](../../CHANGELOG.md) | quem lê o repositório | histórico completo, com a versão do esquema da base |
| `composeApp/src/commonMain/kotlin/pt/antares/app/feature/about/AppChangelog.kt` | quem usa a app | bilingue, curado, teto de 12 versões |

**Regra que decide o que entra em ambos:** nunca se anuncia o nascimento de uma coisa que já não
existe. Uma funcionalidade que nasceu e morreu aparece uma vez, como remoção, na versão em que
saiu.

O teto de 12 é imposto pelo `AppChangelogTest`. O histórico completo é trabalho do
`CHANGELOG.md`, não de uma lista dentro da app.

## O que é verificado automaticamente

O `DocumentationHonestyTest` falha se:

- a versão do topo do `CHANGELOG.md` não for o `AppChangelog.CURRENT`;
- o `AppChangelog.CURRENT` não for o `versionName` do build;
- o `versionCode` não for o que a fórmula dá;
- a versão tiver quatro segmentos;
- o cartaz de versão do `README.md` estiver desatualizado;
- uma versão que a app mostra não existir no `CHANGELOG.md`.

## Os artefactos

Os APKs ficam em `apks/`, que o `.gitignore` exclui: são dezenas de ficheiros de centenas de
megabytes, e não são código.

Os anteriores à 1.0.0 têm a numeração antiga, de quatro segmentos, e **não correspondem uma a uma**
às versões deste histórico — ver a nota no fim do [CHANGELOG.md](../../CHANGELOG.md). Não foram
renomeados: renomear um artefacto já compilado é perder a única prova do que ele é.
