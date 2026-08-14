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

```bash
./gradlew build
cd supabase && deno test --allow-env --allow-net functions/_shared/
```

O `build` inclui o `DocumentationHonestyTest`, que falha se qualquer um dos passos 2 a 4 ficou
por fazer: a versão do changelog, o cartaz do README e a fórmula do `versionCode` têm de bater
todos.

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
