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
