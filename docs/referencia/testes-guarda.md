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
| `TipografiaCompletaTest` | um dos quinze estilos do Material 3 ficar sem fonte declarada, e cair no Roboto do sistema | acrescentaste um estilo sem `fontFamily`. Não dá erro nem aviso: a letra fica diferente e mais nada. Foi assim que 176 utilizações ficaram fora da fonte da app |
| `NumerosDoCatalogoTest` | um número de alimentos citado no `README.md` ou em `docs/referencia/dados-e-licencas.md` descolar do catálogo | o catálogo mudou e o documento não. Reconta o catálogo e cobra a cada documento que nomeie a origem. O `CHANGELOG.md` fica de fora: as entradas antigas descrevem o que era verdade na versão delas |
| `PodaDoCatalogoTest` | a substituição do catálogo apagar um alimento que a pessoa está a usar | uma das seis condições caiu da consulta. As receitas e as refeições-tipo faltavam lá até à 2.4.0, e um ingrediente podia desaparecer sem erro nenhum |
| `CatalogoTemVersaoTest` | o catálogo novo viajar dentro do APK e não entrar em telemóvel nenhum | a versão do `construir.mjs` e a do `FoodSeeder` deixaram de ser a mesma. Verifica também a ordem e a unicidade, que são o que torna a construção determinística visível no `git diff` |
| `CatalogoNaoRegrideTest` | os nomes corrigidos à mão voltarem ao nome de laboratório | a reconstrução esqueceu-se de aplicar o `correcoes.json`. Passa em todos os outros testes: tem os alimentos todos, na ordem certa, com a nutrição certa — só os nomes é que voltam atrás |
| `MarcaForaDoAlimentoTest` | os favoritos, os recentes e as porções guardadas voltarem para dentro da linha do alimento | na linha, são apagados pela actualização do catálogo **e** não vão na cópia de segurança, porque do catálogo só se exportam os alimentos criados pela pessoa. Lê o esquema exportado, não o código: a coluna pode nascer de uma anotação ou de uma migração |
| `MigracaoDeMarcasTest` | a mudança das marcas de casa perder favoritos, recentes ou porções | uma migração que perde linhas **não rebenta**: a app abre, a pesquisa funciona, e a pessoa é que dias depois vê os favoritos vazios. Conta os dois lados sobre uma base construída a partir do esquema da v26 |
| `MarcasNaCopiaTest` | os favoritos e os recentes voltarem a ficar de fora da cópia de segurança | a ligação que os deixava de fora não está escrita em código nenhum: do catálogo só se exportam os alimentos criados pela pessoa, e o favorito vivia lá dentro. A cópia parecia completa e restaurá-la apagava tudo o que estava marcado |
| `DuvidasForaDoCodigoTest` | uma dúvida ficar marcada no código com `TODO`, `FIXME`, `HACK` ou `DÚVIDA` | adiaste uma decisão dentro de um ficheiro. Vai para a lista à parte, que é onde o dono a responde |
| `MigracaoComDadosTest` | uma migração nova perder linhas de quem já tem a app | constrói a base da v3 a partir do esquema exportado, enche todas as tabelas e abre com o Room. Não sabe nada do esquema de propósito — uma versão nova fica coberta sem ninguém lhe tocar |
| `DocumentationHonestyTest` | a documentação apontar para ficheiros que não existem, e a versão descolar | mudaste um caminho e não o documento |
| `BackupReachableTest` | o backup ficar sem caminho na interface | o ecrã ou a rota mudaram de nome |
| `SemCopiaNaNuvemTest` | o `allowBackup` voltar a `true`, ou a cópia local que o substituiu desaparecer | alguém repôs a cópia da Google, ou tirou a cópia automática sem repor outra coisa. Substituiu o `BackupRulesTest` na 2.1.0, que verificava exatamente o contrário |
| `CopiaAutomaticaTest` | a rotação apagar a cópia errada | o nome deixou de se ordenar pela data. É só disso que a rotação depende: ordena por nome e deita fora tudo menos as cinco últimas |
| `NomesDeCopiaUnicosTest` | duas cópias seguidas pedirem o mesmo nome ao sistema | tiraste os segundos do nome. O MediaStore resolve a colisão sozinho gravando «… (1).zip», e esse nome já não se ordena pela data |
| `ResumoDaCopiaTest` | o resumo mostrado antes de importar mentir sobre o ficheiro | mudaste o cabeçalho da exportação. Substituir é irreversível e é este resumo que o decide |
| `CartaoDaCopiaUiTest` | o aviso de cópia em atraso aparecer todos os dias, ou nunca | perdeu-se a condição do atraso. Um alarme que aparece sempre deixa de ser lido, e é assim que se volta ao defeito da cópia da Google: invisível |
| `PlateauHonestyTest` | a app chamar «adaptação metabólica» a um planalto de quem não registou os dias | a média de registos por semana deixou de ser exigida antes do diagnóstico. Um planalto sobre uma semana com três dias registados é um planalto nos dados, não no corpo |
| `GoalGuardrailsTest` | um peso-alvo abaixo do saudável passar sem aviso | o limiar saiu do `BodyComposition`. Só avisa em baixo: o limite de cima não é assunto deste aviso |
| `TargetBreakdownSweepTest` | o «mostra-me a conta» não fechar na meta que a app mostra | um passo passou a começar noutro sítio que não o fim do anterior. Varre perfis por sexo, atividade, ritmo, idade, altura e massa gorda — o erro aparecia só em combinações raras |
| `NomeDaCorridaTest` | a madrugada partir-se ao dar a volta à meia-noite | alguém escreveu `hora in 22..4`, que é um intervalo vazio: todas as horas cairiam noutro ramo, sem erro nenhum a avisar |
| `CopiaVaziaTest` | a app escrever uma cópia de segurança antes de haver o que copiar | perdeu-se a exigência do arranque feito. Uma cópia vazia é pior do que nenhuma: **cala o aviso** sem proteger nada. Foi assim que a 2.1.0 saiu |
| `ImportacaoDeCopiaVaziaTest` | «substituir» aceitar uma cópia sem uma linha e apagar tudo | a recusa saiu de antes da transação. O ficheiro vazio que a 2.1.0 escrevia tinha a assinatura toda e passava por cópia legítima |
| `InterruptorDaPesquisaTest` | haver caminho até à Open Food Facts que não passe pelo interruptor | alguém usou o `OffApi` fora do repositório, ou chamou o caminho interno de fora. Um interruptor de privacidade que se pode contornar por esquecimento não é um interruptor |
| `DestinosDeclaradosTest` | a app contactar um endereço que o ecrã dos destinos não declara | entrou um `https://` novo no código. A lista tem de crescer com ele, ou a decisão de o esconder passa a ser deliberada e escrita |
| `MovimentoDeTodasAsRotasTest` | uma rota nova ficar sem movimento escolhido | o `MovimentoDasRotas.de()` devolve `MAIS_FUNDO` ao que não conhece, para a app nunca ficar parada — e sem este teste esse valor por omissão seria a maneira mais fácil de o sistema apodrecer. Lê o `Routes.kt` e exige a lista completa |
| `CartaoUnicoTest` | o `Card` e o `ListItem` do Material voltarem a coexistir com o `AntaresCard` | eram usados porque o nosso não aceitava clique. Resolvida a causa na 2.3.0, este guarda o efeito — com duas excepções nomeadas, porque uma regra sem excepções escritas é uma que alguém contorna em silêncio |
| `LarguraDeLeituraEmTodoOEcraTest` | um ecrã esticar-se por 1200 dp num tablet | não limita a linha nem entrega a largura a uma `ListaAdaptavel`. Não dá erro nem aviso, e num telemóvel — que é onde se desenvolve — parece perfeito |
| `AndaimeUnicoTest` | um ecrã usar o `Scaffold` do Material e perder o `imePadding` | o teclado passa a tapar o conteúdo em vez de o empurrar. Eram trinta e dois ecrãs assim antes da 2.3.0, e o problema não dá erro nenhum: só se vê a escrever num campo em baixo |
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
| `JanelaTest` e `JanelaLargaUiTest` | as fronteiras da janela deixarem de ser as do Material (600, 840 e 480 dp), ou a navegação não sair de baixo quando a janela é larga ou baixa | alguém mexeu num `>=`, ou o `ProvedorDaJanela` deixou de estar acima de tudo e passou a medir uma coluna em vez da janela |
| `ListaAdaptavelUiTest` | uma lista voltar a ser uma coluna só num tablet, ou um cabeçalho ficar encavalitado ao lado do primeiro resultado | trocaste a `ListaAdaptavel` por uma `LazyColumn`, ou um `linhaInteira` por um `item` |
| `GrelhaDeCartoesUiTest` | os cartões do Hoje voltarem a empilhar-se numa coluna com meio ecrã vazio ao lado | a grelha deixou de alternar entre as duas colunas |
| `LarguraDeLeituraUiTest` | um formulário esticar-se por 1200 dp, ou ficar encostado a um lado em vez de ao meio | falta o `larguraDeLeitura()` num ecrã novo, ou a cadeia de modificadores perdeu o `wrapContentWidth` |
| `ListaEDetalheUiTest` | o detalhe deixar de abrir ao lado numa janela larga, ou tentar abrir ao lado num telemóvel | o `cabeDetalheAoLado` deixou de olhar ao modo de esquema |

## Onde estão

A maior parte em `composeApp/src/androidUnitTest/kotlin/pt/antares/app/core/`. Os que não precisam
de Android estão em `composeApp/src/commonTest/`. Os do esquema adaptável estão em
`composeApp/src/androidUnitTest/kotlin/pt/antares/app/ui/`, porque precisam de compor a interface
com uma janela de tamanho conhecido — o tamanho vem dos qualificadores do Robolectric
(`@Config(qualifiers = "w1280dp-h800dp")`) e não de um `Modifier.size`, que a janela do teste
aperta.

## Porque é que isto existe

Este repositório foi esvaziado uma vez de todos os documentos e comentários, porque descreviam
funcionalidades que já tinham sido removidas. Quem os lia ficava a descrever a app errada com toda
a confiança.

Um teste-guarda é a resposta a esse problema: uma afirmação sobre o projeto que **não pode
apodrecer em silêncio**, porque falha a compilação no dia em que deixa de ser verdade.
