# Lançar uma versão

Por ordem. Cada passo tem um teste ou um comando que o confirma.

## 1. Escolher o número

Três números, nunca quatro. A regra de qual sobe está em
[Versionamento](../referencia/versionamento.md); em resumo:

- **MAJOR** — quem atualizar perde uma coisa com que contava;
- **MINOR** — ganha alguma coisa;
- **PATCH** — só se corrige.

Se a alteração é só arrumação, testes ou documentação, **não se lança nada**. Fica em
`[Unreleased]` no [CHANGELOG.md](../../CHANGELOG.md).

## 2. Mudar a versão num sítio só

Em `composeApp/build.gradle.kts`:

```kotlin
val appVersion = "1.1.0"
```

O `versionCode` deriva daqui. Não se toca nele.

## 3. Acompanhar o changelog da app

Em `composeApp/src/commonMain/kotlin/pt/antares/app/feature/about/AppChangelog.kt`:

- pôr `CURRENT` no valor novo;
- acrescentar a entrada no topo da lista, **em português e em inglês, com o mesmo número de
  linhas nas duas**.

Regra do que entra: nunca se anuncia o nascimento de uma coisa que já não existe. Uma
funcionalidade removida aparece uma vez, como remoção, na versão em que saiu.

## 4. Escrever a entrada no CHANGELOG.md

Passar o `[Unreleased]` para a versão nova, com a data em `AAAA-MM-DD` e as secções do
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) — `Added`, `Changed`, `Fixed`,
`Removed`, `Security`.

Se a base de dados mudou, dizer qual é a versão do esquema. É isso que diz a quem atualiza se a
migração é indolor.

## 5. Correr tudo

Os mesmos quatro comandos do CI. **Não `./gradlew build`**: ele puxa o
`testReleaseUnitTest`, onde os testes de interface não correm.

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:assembleRelease
./gradlew :composeApp:detekt :composeApp:lintDebug
cd supabase && deno test -A functions/_shared/
```

O `testDebugUnitTest` inclui o `DocumentationHonestyTest`, que falha se qualquer um dos passos
2 a 4 ficou por fazer: a versão do changelog, o cartaz do README e a fórmula do `versionCode`
têm de bater todos.

E antes de etiquetar, **a varredura**: os testes-guarda novos estão documentados? Os números que
os documentos citam ainda são verdade? Um defeito que pertence a esta versão e fica por corrigir
não passa a pertencer à seguinte — regra D6.

**Ler os relatórios**, e não a última linha — ver [Correr os testes](correr-os-testes.md).

## 6. Verificar que não vai nenhum segredo

```bash
git ls-files | xargs grep -lIE "sk-ant-(api|admin)[A-Za-z0-9_-]{10,}"
git ls-files | xargs grep -lIE "eyJ[A-Za-z0-9_-]{40,}"
```

Os dois têm de vir vazios. O CI corre o mesmo, mas isto é mais barato do que descobri-lo depois de
publicar.

## 7. Compilar e guardar o APK

```bash
./gradlew :composeApp:assembleRelease
```

Os APKs ficam fora do repositório, em `apks/`, que o `.gitignore` exclui.

**Não se renomeiam artefactos já compilados.** O nome do ficheiro é a única prova do que ele é.

## 8. Etiquetar

```bash
git tag -a v1.1.0 -m "1.1.0"
git push origin v1.1.0
```

## 9. Publicar a release e anexar os APKs

A etiqueta sozinha não dá nada a descarregar a quem chega ao repositório: cria uma página de
release e põe-lhe os ficheiros.

```bash
gh release create v2.3.0 \
  composeApp/build/outputs/apk/release/*.apk \
  --title "2.3.0 — o título desta versão" \
  --notes-file notas-desta-versao.md
```

Os quatro ficheiros saem já com o nome certo — `Antares-2.3.0-arm64-v8a.apk` e companhia —
porque o `build.gradle.kts` os renomeia a partir da versão. Não é preciso copiá-los para lado
nenhum antes de os anexar.

O `gh auth login` é interativo e abre o browser — tem de ser corrido por uma pessoa, uma vez
por máquina.

## 10. Olhar para o CI depois de publicar

**O verde local não diz nada sobre o CI.** Ele escolhe as suas tarefas, e já esteve vermelho
meses sem ninguém dar por isso — corria o `testReleaseUnitTest`, onde os testes de interface
não podem correr por falta do manifesto que só a variante debug tem.

```bash
gh run list --limit 3
```

Se a corrida terminar em menos de dois minutos, os testes vieram da cache do Gradle
(`FROM-CACHE`) e não chegaram a correr. Com as mesmas entradas isso é legítimo — mas não é
uma execução, e não serve de prova quando se mudou alguma coisa.
