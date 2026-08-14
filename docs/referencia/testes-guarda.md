# Testes-guarda

Testes que não verificam uma conta. Verificam que **uma decisão não foi desfeita sem querer**.

Quando um destes falha, a pergunta certa não é como o fazer passar. É *que decisão foi desfeita*.

| Teste | O que impede | Se falhar |
|---|---|---|
| `NoSyncTest` | que a sincronização volte pela porta das dependências | alguém acrescentou o `postgrest`. Ver [a decisão](../explicacao/decisoes/0001-a-app-nao-sincroniza.md) |
| `TombstoneCollisionTest` | que uma escrita por dia volte a colidir com uma lápide | falta um `byDayForWrite` num caminho novo |
| `AdaptiveTargetsOfflineTest` | que uma chamada de rede entre no relatório semanal | o relatório deixou de ser calculado no telemóvel |
| `DeadCodeSweepTest` | código sem chamador, que descreve funcionalidades inexistentes | há código órfão: apagar, não comentar |
| `KoinGraphTest` | uma dependência em falta rebentar no ecrã em vez de no teste | falta um `single` ou um `viewModel` no módulo |
| `GdprTableParityTest` | uma tabela nova ficar de fora da exportação e do apagamento | criaste uma tabela e esqueceste o `DataExporter` ou o `PrivacyRepository` |
| `HealthPermissionsParityTest` | pedir ao Health Connect o que o manifesto não declara | o pedido e o manifesto discordam |
| `ManifestPermissionsTest` | pedir ao sistema mais do que a app usa | há uma permissão declarada sem uso |
| `StringResourcesTest` | texto visível escrito à mão em vez de vir dos recursos | há uma cadeia literal num ecrã |
| `AccessibilityTest` | elementos sem descrição para o leitor de ecrã | falta um `contentDescription` |
| `ThemeAwareColorsTest` | cores fixas que quebram no tema claro | há uma cor escrita à mão |
| `KeyboardInsetsTest` | o teclado tapar os botões | falta o tratamento de *insets* num ecrã novo |
| `SeederOrderTest` | ler megabytes de seed em todos os arranques | um semeador lê o ficheiro antes de verificar a marca |
| `AppChangelogTest` | a versão da app descolar do changelog | falta o passo 3 de [Lançar uma versão](../guias/lancar-uma-versao.md) |
| `DocumentationHonestyTest` | a documentação apontar para ficheiros que não existem, e a versão descolar | mudaste um caminho e não o documento |
| `BackupReachableTest` | o backup ficar sem caminho na interface | o ecrã ou a rota mudaram de nome |
| `BackupRulesTest` | a cópia de segurança do Android levar o que não deve | as regras XML mudaram |
| `NumericFieldsTest` | campos numéricos abrirem o teclado errado | falta o `KeyboardType` |
| `WeightDisplayTest` | o peso aparecer na unidade errada | a conversão saiu do sítio |

## Onde estão

A maior parte em `composeApp/src/androidUnitTest/kotlin/pt/antares/app/core/`. Os que não precisam
de Android estão em `composeApp/src/commonTest/`.

## Porque é que isto existe

Este repositório foi esvaziado uma vez de todos os documentos e comentários, porque descreviam
funcionalidades que já tinham sido removidas. Quem os lia ficava a descrever a app errada com toda
a confiança.

Um teste-guarda é a resposta a esse problema: uma afirmação sobre o projeto que **não pode
apodrecer em silêncio**, porque falha a compilação no dia em que deixa de ser verdade.
