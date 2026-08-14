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

Compilar e correr tudo, que é o que o CI faz:

```bash
./gradlew build
```

Os relatórios ficam em `composeApp/build/test-results/testDebugUnitTest/` (XML, um por classe) e
em `composeApp/build/reports/tests/`(HTML).

> **Ler o relatório, e não a última linha.** Um `BUILD SUCCESSFUL` diz que o Gradle terminou, não
> que os testes correram — se a tarefa estiver `UP-TO-DATE`, nenhum correu. Para ter a certeza,
> `--rerun-tasks`, e confirmar a hora em que os XML foram escritos.

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
