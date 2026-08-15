# Testes-guarda

Testes que não verificam uma conta. Verificam que **uma decisão não foi desfeita sem querer**.

Quando um destes falha, a pergunta certa não é como o fazer passar. É *que decisão foi desfeita*.

| Teste | O que impede | Se falhar |
|---|---|---|
| `NoSyncTest` | que a sincronização volte — pelas dependências, pelo login com Google, pelo manifesto, ou por um texto que a prometa a quem usa a app | alguém acrescentou o `postgrest`, ou um ecrã voltou a falar de conta. Ver [a decisão](../explicacao/decisoes/0001-a-app-nao-sincroniza.md) |
| `TombstoneCollisionTest` | que uma escrita por dia volte a colidir com uma lápide | falta um `byDayForWrite` num caminho novo |
| `AdaptiveTargetsOfflineTest` | que uma chamada de rede entre no relatório semanal | o relatório deixou de ser calculado no telemóvel |
| `DeadCodeSweepTest` | código sem chamador, que descreve funcionalidades inexistentes | há código órfão: apagar, não comentar |
| `KoinGraphTest` | uma dependência em falta rebentar no ecrã em vez de no teste | falta um `single` no módulo. Atenção: percorre uma **lista escrita à mão** de repositórios, e não o grafo todo — um serviço novo só é coberto se for acrescentado à lista |
| `GdprTableParityTest` | uma tabela de dados da pessoa ficar de fora da **exportação** | criaste uma tabela e esqueceste o `sources` do `CoreModule`. Não verifica o apagamento: verifica que nada é apagado sem antes poder ser exportado |
| `HealthPermissionsParityTest` | pedir ao Health Connect o que o manifesto não declara, e declarar o que ninguém pede | o pedido e o manifesto discordam |
| `ManifestPermissionsTest` | a app **herdar** de uma dependência as permissões de que precisa, em vez de as declarar; e a localização em segundo plano voltar | falta uma das sete permissões obrigatórias, ou entrou `ACCESS_BACKGROUND_LOCATION` |
| `StringResourcesTest` | os textos português e inglês descolarem: chaves diferentes, argumentos de formato diferentes, ou numerados com saltos | acrescentaste uma chave numa língua só, ou mudaste os argumentos de uma delas |
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
| `ProveniencaHonestaTest` | a app prometer uma fonte de dados que não existe, e o INSA deixar de ser nomeado onde os dados dele aparecem | mudaste um texto de origem. A licença do INSA obriga a identificar a fonte **junto dos dados** |
| `DicionarioUsdaTest` | a tradução dos nomes americanos descer sem ninguém dar por isso | tiraste entradas do dicionário, ou o catálogo foi regenerado com nomes diferentes |
| `IndicesQuentesTest` | perder um índice de que uma consulta repetida depende | apagaste um `Index` a pensar que sobrava. A app continua certa e passa a varrer a tabela toda — o custo só aparece a quem já tem anos de dados |
| `SeedFalhadoDeixaRastoTest` | um catálogo que falha a semear ficar vazio em silêncio | a leitura do seed deixou de registar no `CrashStore` |
| `EcraVazioTemEstadoTest` | um ecrã de lista que não diz nada quando a lista está vazia — no primeiro dia de quem instala a app é um ecrã em branco | fizeste uma lista nova sem ramo para o vazio. Há três maneiras honestas de o tratar, e as três contam; a exceção escreve-se no teste com a razão |
| `UmaConvencaoDeCorTest` | a cor voltar a dizer duas coisas: categoria nos macros e estado ao mesmo tempo, ou uma barra de micro curta ser carência e falta de dados sem se distinguir | alguém pintou um macro de vermelho por estar alto, ou tirou o tracejado das medidas incertas |
| `TodaRotaTemEcraTest` | uma rota declarada sem ecrã, um ficheiro de rotas que ninguém chama, ou a mesma rota registada em duas áreas | navega-se para a rota e fica um ecrã em branco. Nenhuma das três dá erro de compilação, e só se descobre a usar |
| `EspelhoDeNutrientesTest` | as chaves dos nutrientes descolarem entre o `Nutrients` e o `supabase/functions/_shared/nutrients.ts` do servidor | acrescentaste um nutriente de um lado só. Quem lê um código de barras deixa de o ver, e sem erro nenhum: o servidor grava uma chave que o telemóvel não reconhece e deita fora |
| `BackupImportRollbackTest` | uma importação falhada deixar a pessoa sem os dados velhos e com metade dos novos | o restauro saiu da transação. Ver [a decisão](../explicacao/decisoes/0002-lapides-e-indices-unicos.md) |

## Onde estão

A maior parte em `composeApp/src/androidUnitTest/kotlin/pt/antares/app/core/`. Os que não precisam
de Android estão em `composeApp/src/commonTest/`.

## Porque é que isto existe

Este repositório foi esvaziado uma vez de todos os documentos e comentários, porque descreviam
funcionalidades que já tinham sido removidas. Quem os lia ficava a descrever a app errada com toda
a confiança.

Um teste-guarda é a resposta a esse problema: uma afirmação sobre o projeto que **não pode
apodrecer em silêncio**, porque falha a compilação no dia em que deixa de ser verdade.
