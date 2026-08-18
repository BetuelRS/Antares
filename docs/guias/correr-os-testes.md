# Correr os testes

Há duas suites: a da app, em Kotlin, e a das Edge Functions, em Deno. Correm-se em separado.

## A app

```bash
./gradlew :composeApp:testDebugUnitTest
```

Um teste só, ou um ficheiro:

```bash
./gradlew :composeApp:testDebugUnitTest --tests '*NomeDoTeste'
```

Correr tudo o que o CI corre — e **não** `./gradlew build`, que puxa o
`testReleaseUnitTest`: os testes de interface não podem correr na release, por falta do
manifesto que só a debug traz, e dão «Unable to resolve activity».

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:assembleRelease
./gradlew :composeApp:detekt :composeApp:lintDebug
cd supabase && deno test -A functions/_shared/
```

Os relatórios ficam em `composeApp/build/test-results/testDebugUnitTest/` (XML, um por classe) e
em `composeApp/build/reports/tests/`(HTML).

> **Ler o relatório, e não a última linha.** Um `BUILD SUCCESSFUL` diz que o Gradle terminou, não
> que os testes correram — se a tarefa estiver `UP-TO-DATE`, nenhum correu. Para ter a certeza,
> `--rerun-tasks`, e confirmar a hora em que os XML foram escritos.

## Os dois analisadores

Além dos testes, o CI corre o **detekt** sobre o Kotlin e o **lint do Android** sobre o módulo:

```bash
./gradlew :composeApp:detekt
./gradlew :composeApp:lintDebug
```

Os dois assentam numa **linha de base**: um ficheiro com tudo o que já estava assinalado no dia em
que foram ligados. O que lá está não faz o CI falhar; só código novo é que faz.

| | Configuração | Linha de base |
|---|---|---|
| detekt | `config/detekt/detekt.yml` | `config/detekt/baseline.xml` |
| lint | bloco `lint` do `composeApp/build.gradle.kts` | `composeApp/lint-baseline.xml` |

A configuração do detekt assenta na de omissão e só escreve o que se afasta dela, com a razão de
cada afastamento. Duas notas que valem a pena saber:

- Escrever `excludes` numa regra **substitui** a lista de omissão, não a acrescenta. Foi como o
  `MagicNumber` passou a assinalar os testes por engano, e por isso a lista repõe as pastas de
  teste antes de acrescentar as dos ecrãs.
- Um número sem nome num ecrã é uma medida; numa função pura é um limiar por explicar. Só os
  ecrãs saem da regra.

### Refazer uma linha de base

**Refazer uma linha de base apaga o registo do que estava por corrigir.** Faz-se quando se
corrigiu de facto o que lá estava, e nunca para calar um aviso novo — para esse, ou se corrige o
código, ou se escreve a razão na configuração.

```bash
./gradlew :composeApp:detektBaseline

rm composeApp/lint-baseline.xml && ./gradlew :composeApp:lintDebug
```

O lint **falha de propósito** na corrida em que cria a linha de base — «Aborting build since new
baseline file was created». Correr outra vez passa.

## As Edge Functions

```bash
cd supabase
deno test --allow-env --allow-net functions/_shared/
```

**As flags não são opcionais.** Sem elas há testes que falham com
`NotCapable: Requires env access` — é o Deno a recusar permissões ao próprio teste, e não código
partido. O CI passa `-A` e por isso nunca dá sinal disto; quem corre o comando óbvio à mão conclui
o contrário do que se passa.

## Duas armadilhas que fazem os testes mentir

### O `MockEngine` do Ktor responde fora do tempo virtual

Num teste com `runTest`, o `advanceUntilIdle()` devolve com o pedido HTTP **ainda a decorrer**. As
afirmações correm sobre listas por preencher e o teste passa por engano.

A espera tem de ser sobre o estado, e não sobre o relógio:

```kotlin
vm.setQuery(texto)
dispatcher.scheduler.advanceUntilIdle()
vm.state.first { !it.searching && !it.searchingOnline }
```

Dentro de uma função `suspend`, o `advanceUntilIdle()` não está ao alcance — usa-se
`dispatcher.scheduler.advanceUntilIdle()`.

### O aviso do Robolectric não é uma falha

```
[Robolectric] WARN: Android SDK 36 requires Java 21 (have Java 17)
```

É normal: o Robolectric recua para uma versão que sabe correr. Não é preciso fazer nada.

## Escrever um teste de ViewModel

O `ViewModelHarness` monta uma base de dados em memória, um `DataStore` temporário e um
despachante de teste. Estende-o e pede o que precisas:

```kotlin
@RunWith(RobolectricTestRunner::class)
class ProfileSettingsViewModelTest : ViewModelHarness() {

    @Test
    fun `mudar de objetivo repoe o ritmo por omissao desse objetivo`() = runTest(dispatcher) {
        val vm = ProfileSettingsViewModel(profileRepository(), prefs)
        vm.state.first { !it.loading }

        vm.setGoal(GoalType.GAIN)
        advanceUntilIdle()

        assertEquals(GoalType.GAIN, db.userProfileDao().get()?.goalType)
    }
}
```

Dois pormenores que custam tempo a descobrir:

- `db.foodDao().upsert()` **não** alimenta a pesquisa. É `upsertWithFts(food, texto)`; sem o
  segundo argumento o alimento existe e não se encontra.
- O `FoodLogDao` não tem `byDay`. É `dayLogs(epochDay)`.

## Escrever um teste de fluxo pela interface

O `FluxoUiHarness` monta o que um ecrã precisa para correr a sério: base em memória,
`DataStore` temporário, os recursos do Compose e um Koin mínimo. Todos os ecrãs aceitam o
ViewModel por parâmetro, e é assim que se lhes dá o do teste em vez do da app.

```kotlin
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = "w411dp-h891dp")
class TreinoSerieUiTest : FluxoUiHarness() {

    @Test
    fun `gravar uma serie poe o descanso a andar`() = runComposeUiTest {
        arrancaKoin()
        val vm = WorkoutSessionViewModel(...)
        setContent { WorkoutSessionScreen(routineId = "r1", ..., viewModel = vm) }

        waitUntil("o exercício nunca apareceu", 5_000) { ... }
    }
}
```

Cinco coisas que custam uma tarde a descobrir:

- **A janela por omissão do Robolectric tem 320 dp**, mais estreita do que qualquer
  telemóvel atual. Numa linha larga, o último botão fica com **zero de largura** e o
  `performClick` não faz nada — sem erro nenhum. Daí o `qualifiers = "w411dp-h891dp"`.
- **O rótulo de uma caixa de texto é um nó ao lado dela.** `onNodeWithText(rótulo)` encontra
  o rótulo, e escrever nele não escreve no campo. Procurar por `hasSetTextAction()`.
- **O despachante é o `Unconfined`, não um de teste.** O `runComposeUiTest` traz o seu
  próprio relógio, e dois relógios a controlar as mesmas corrotinas travam-se um ao outro.
- **O Room recusa o `Unconfined`** — pede-lhe `limitedParallelism`. A base do harness fica
  com o `Default`, e por isso as afirmações sobre ela usam `waitUntil` em vez de assumir.
- **Afirmar sobre texto visível, nunca sobre posição.** E ler o texto com `Textos` dentro do
  `setContent`: um teste que escreva a frase à mão passa a falhar quando alguém lhe corrigir
  uma vírgula.

Para uma base de dados a sério montada como na app — as 26 fontes de exportação incluídas —
troca-se só a base no Koin, como no `RestaurarBackupTest`: a verdadeira usa SQLite nativo,
que não existe num teste de JVM.

## Como se escrevem os nomes

Frases em português, a afirmar o comportamento que se perde se partir:

```
o ritmo escrito no ecra segue o sinal do objetivo, e nao o do numero
escolher nao sei limpa o perfil, mas o historico do dia guarda o que ja la estava
```

E não `testSetWeeklyRate`, que não diz nada a quem lê o relatório de uma falha.

## Onde os testes vivem

| | |
|---|---|
| `composeApp/src/commonTest` | contas puras: nutrição, treino, datas, texto. Sem Android |
| `composeApp/src/androidUnitTest` | base de dados, ViewModels e as guardas. Com Robolectric |

Se um teste precisa de Robolectric só para chamar uma fórmula, a fórmula está no sítio errado —
ver [Arquitetura](../explicacao/arquitetura.md).

Para saber o que cada teste-guarda defende, e o que fazer quando um falha, vê
[Testes-guarda](../referencia/testes-guarda.md).
