package pt.antares.app.feature.about

data class AppVersion(
    val name: String,
    val title: String,
    val titleEn: String,
    val highlights: List<String>,
    val highlightsEn: List<String>,
) {
    fun title(english: Boolean): String = if (english) titleEn else title
    fun highlights(english: Boolean): List<String> = if (english) highlightsEn else highlights
}

/**
 * As novidades como a pessoa as lê dentro da app. É a versão curta e bilingue do `CHANGELOG.md`,
 * que é o registo completo — e as duas têm de abrir na mesma versão, coisa que o
 * `AppChangelogTest` verifica.
 *
 * Regra que decide o que entra aqui: **nunca se anuncia o nascimento de uma coisa que já não
 * existe**. Uma novidade sobre uma funcionalidade removida é a forma mais rápida de alguém ficar
 * com a ideia errada do que a app faz — e, ao contrário de um documento, esta lista é lida
 * exatamente por quem está a usá-la.
 */
object AppChangelog {

    const val CURRENT = "2.20.0"

    val versions: List<AppVersion> = listOf(
        AppVersion(
            name = "2.20.0",
            title = "O separador do treino passa a dizer alguma coisa",
            titleEn = "The workout tab starts telling you something",
            highlights = listOf(
                "**O treino de hoje em primeiro**, vindo do teu plano da semana — e um " +
                    "toque para o começar. Antes eram três toques e um percorrer até ao fundo",
                "**Sem plano, o cartão mostra a última rotina que fizeste.** Sem plano e sem " +
                    "histórico, leva-te a marcar a semana em vez de escolher uma por ti",
                "**A semana em sete pontos**, com o volume e as séries que já lá vão",
                "**Cada rotina diz quantos exercícios tem e quando foi feita**, e tem um ▶ " +
                    "que a começa. Tocar no nome continua a abrir o editor",
                "**Os últimos treinos passam a ter quatro dados** — rotina, data, duração e " +
                    "séries. Tinham dois, e dois treinos diferentes ficavam iguais",
                "**Com um treino a decorrer, o ecrã diz há quanto tempo** e oferece retomá-lo " +
                    "— e mais nada: começar outro levava ao que já estava aberto",
                "A biblioteca, as estatísticas, o histórico e o plano da semana passam para " +
                    "o menu do canto: eram cinco botões cinzentos a ocupar metade do ecrã",
            ),
            highlightsEn = listOf(
                "**Today's workout comes first**, from your weekly plan — and one tap starts " +
                    "it. It used to be three taps and a scroll to the bottom",
                "**With no plan, the card shows the last routine you did.** With no plan and " +
                    "no history it takes you to plan the week instead of picking one for you",
                "**The week as seven dots**, with the volume and sets so far",
                "**Every routine says how many exercises it has and when you last did it**, " +
                    "and has a ▶ that starts it. Tapping the name still opens the editor",
                "**Recent workouts now carry four facts** — routine, date, duration and " +
                    "sets. They carried two, and two different workouts looked identical",
                "**With a workout running, the screen says how long it has been going** and " +
                    "offers to resume it — and nothing else: starting another took you to " +
                    "the one already open",
                "The library, statistics, history and weekly plan move into the corner menu: " +
                    "they were five grey buttons taking up half the screen",
            ),
        ),
        AppVersion(
            name = "2.19.0",
            title = "Registar exercício deixa de andar de cinco em cinco",
            titleEn = "Logging exercise stops moving five minutes at a time",
            highlights = listOf(
                "**A duração escreve-se**, com atalhos de 15, 30, 45 e 60 minutos. Uma aula " +
                    "de 50 minutos custava dez toques, e um treino de 22 não se registava",
                "**As atividades que fazes mais aparecem em cima**, antes de procurares",
                "**Tocar num exercício do diário abre-o para corrigir** a duração e a hora, " +
                    "como já acontecia com a comida",
                "**Um exercício passa a guardar a hora a que começou** — a corrida, o treino " +
                    "e o que vem do Health Connect trazem-na de origem",
                "**Os botões de mais e menos passam a dizer de quê** a quem usa leitor de " +
                    "ecrã: diziam «menos cinco», sem dizer cinco de quê",
            ),
            highlightsEn = listOf(
                "**Duration is typed**, with 15, 30, 45 and 60 minute shortcuts. A 50 minute " +
                    "class took ten taps, and a 22 minute workout could not be logged at all",
                "**The activities you do most show up on top**, before you search",
                "**Tapping an exercise in the diary opens it** so you can fix the duration " +
                    "and the time, the way food already worked",
                "**An exercise now keeps the time it started** — runs, workouts and anything " +
                    "from Health Connect bring it along",
                "**The plus and minus buttons now say what of** to screen readers: they said " +
                    "\"minus five\", without saying five of what",
            ),
        ),
        AppVersion(
            name = "2.18.2",
            title = "A app passa a dizer de onde vêm os números",
            titleEn = "The app starts saying where its numbers come from",
            highlights = listOf(
                "**Cada nutriente diz de onde veio**, quando não veio de onde veio o " +
                    "alimento: um alimento medido em Portugal pode levar o iodo da tabela " +
                    "francesa, e a app dizia uma origem só",
                "**A ficha de um alimento diz de quanto é o «cerca de»** dos números dele — " +
                    "uma tabela publica a média de umas amostras, um rótulo tem tolerâncias, " +
                    "uma estimativa é um palpite",
                "**O dia reparte a margem pelas origens.** Saber que 120 das 150 kcal de " +
                    "margem vêm do prato que a AI adivinhou diz o que fazer; «±150» não diz",
                "**A procura abre em Tudo · Favoritos · Meus**, com as tuas refeições em " +
                    "primeiro, a tua porção habitual na linha, e o microfone e a câmara " +
                    "dentro do campo",
                "**Os botões dizem onde é que o registo cai** — «Registar no almoço», " +
                    "«Registar 2 doses no jantar»",
                "**«As minhas refeições» é um ecrã**, alcançável do «Eu» e do diário",
                "**A app ganhou a cor que sempre disse ter**: cartões quase pretos em vez de " +
                    "cinzentos, texto branco quente, e um ar por trás de tudo",
                "**A massa gorda medida à fita é corrigida do desvio por sexo** — isto muda " +
                    "números que já estavam no telemóvel, e o basal acompanha",
            ),
            highlightsEn = listOf(
                "**Every nutrient says where it came from**, when it did not come from where " +
                    "the food did: a food measured in Portugal can carry its iodine from the " +
                    "French table, and the app used to name a single source",
                "**A food now says how wide its \"about\" is** — a table publishes the average " +
                    "of a handful of samples, a label has legal tolerances, an estimate is a " +
                    "guess",
                "**The day splits its margin by source.** Knowing that 120 of the 150 kcal of " +
                    "margin come from the plate the AI guessed tells you what to do; \"±150\" " +
                    "does not",
                "**Search opens in All · Favourites · Mine**, with your meals first, your " +
                    "usual portion on the row, and the mic and camera inside the field",
                "**Buttons say where the entry lands** — \"Log to lunch\", \"Log 2 servings " +
                    "to dinner\"",
                "**\"Your meals\" is a screen**, reachable from \"Me\" and from the diary",
                "**The app got the colour it always said it had**: near-black cards instead " +
                    "of grey, warm white text, and air behind everything",
                "**Body fat measured with a tape is corrected for the bias by sex** — this " +
                    "changes numbers already on your phone, and your basal metabolism follows",
            ),
        ),
        AppVersion(
            name = "2.18.1",
            title = "As refeições guardadas, e a preparação",
            titleEn = "Saved meals, and the method",
            highlights = listOf(
                "**Vês a refeição guardada antes de ela entrar no diário** — o que lá está, " +
                    "com as gramas e as calorias de cada item",
                "**Meia refeição, ou duas.** Um campo diz quantas vezes, e o total muda à vista",
                "**Desfazer ao aplicar.** Ao dia errado eram sete linhas para apagar à mão",
                "A linha diz **quantos itens e quantas calorias**, em vez de dizer só «Almoço»",
                "**Acrescentar ingredientes sem sair da receita** — a folha não se fecha a cada " +
                    "escolha, e faz-se a lista de uma vez",
                "**A app avisa quando o peso final não bate com os ingredientes**, e nunca o " +
                    "corrige: quem pesou o tacho ganha à tabela",
                "**Passos de preparação**, por ordem, com subir e descer — e a lê-los no ecrã " +
                    "da receita, com o tacho ao lume",
            ),
            highlightsEn = listOf(
                "**You see a saved meal before it enters the diary** — what is in it, with the " +
                    "grams and calories of each item",
                "**Half a meal, or two.** A field says how many times, and the total follows",
                "**Undo when applying.** To the wrong day it was seven rows to delete by hand",
                "The row says **how many items and how many calories**, instead of just \"Lunch\"",
                "**Add ingredients without leaving the recipe** — the sheet stays open, so you " +
                    "build the list in one go",
                "**The app warns when the final weight does not match the ingredients**, and " +
                    "never corrects it: whoever weighed the pan beats the table",
                "**Method steps**, in order, with move up and down — and readable on the recipe " +
                    "screen, with the pan on the stove",
            ),
        ),
        AppVersion(
            name = "2.17.0",
            title = "A revisão da AI passa a corrigir-se",
            titleEn = "The AI review becomes editable",
            highlights = listOf(
                "**O microfone deixa de ir para a pesquisa e passa a ir para a AI.** Já dizia " +
                    "«diz o que comeste», e entregava a frase a uma procura de catálogo — onde " +
                    "«dois ovos e uma torrada» não encontra nada",
                "**As gramas escrevem-se.** De 30 g para 180 g eram quinze toques no +10",
                "**Trocar um item por um alimento do catálogo**, e o registo passa a ligar-se a " +
                    "ele: micronutrientes medidos, porção habitual, e conta para os mais registados",
                "**Acrescentar o que a AI não viu** — o arroz que ficou tapado pela carne",
                "**Guardar como refeição no fim**, com o que acabaste de rever e mais nada",
                "**A fotografia do prato fica no diário** durante dois meses, e apaga-se sozinha",
            ),
            highlightsEn = listOf(
                "**The mic no longer goes to search — it goes to the AI.** It already said " +
                    "\"say what you ate\", then handed that sentence to a catalogue search, " +
                    "where \"two eggs and a slice of toast\" finds nothing",
                "**Grams are typed now.** Going from 30 g to 180 g was fifteen taps on +10",
                "**Swap an item for a catalogue food**, and the entry links to it: measured " +
                    "micronutrients, its usual serving, and it counts towards your most logged",
                "**Add what the AI missed** — the rice hidden behind the meat",
                "**Save as a meal at the end**, with what you just reviewed and nothing else",
                "**The photo of the plate stays in the diary** for two months, then clears itself",
            ),
        ),
        AppVersion(
            name = "2.16.0",
            title = "A procura abre no que tu comes",
            titleEn = "Search opens on what you eat",
            highlights = listOf(
                "**Seis separadores passam a três.** Nada desapareceu: os recentes e os " +
                    "favoritos estão dentro do Procurar, por baixo do que registas mais",
                "**A linha diz a porção** — «uma fatia 30 g» — nos alimentos que têm uma",
                "**Criar um alimento leva o nome que escreveste**, em vez de o pedir outra vez",
                "Os atalhos do que já comeste deixam de desaparecer quando a procura responde",
                "Os produtos de embalagem mostram a fotografia na lista",
            ),
            highlightsEn = listOf(
                "**Six tabs become three.** Nothing is gone: recents and favourites now sit " +
                    "inside Search, below what you log most",
                "**The row shows the serving** — «a slice 30 g» — for foods that have one",
                "**Creating a food carries the name you typed**, instead of asking again",
                "The shortcuts to what you have eaten no longer vanish when search answers",
                "Packaged products show their photo in the list",
            ),
        ),
        AppVersion(
            name = "2.8.0",
            title = "O que faltava à comida cozinhada",
            titleEn = "What the cooked food was missing",
            highlights = listOf(
                "**Uma receita cozinhada deixa de ganhar vitaminas ao lume.** A água que " +
                    "evapora concentrava tudo, vitamina C incluída — e essa perde-se a " +
                    "cozer. Escolhes como cozinhaste o prato e cada ingrediente perde o " +
                    "que a tabela dele diz",
                "**Um mililitro deixa de contar como uma grama.** 200 ml de azeite pesam " +
                    "184 g, e a app contava-lhes 200",
                "**A procura junta o mesmo alimento nos seus estados**: uma linha em vez " +
                    "de sete quase iguais, e as outras a um toque",
                "**O abacaxi encontra o ananás**, e o cimbalino encontra o café. Quem " +
                    "escreve «frango» encontra também o que ainda está em inglês",
                "**A app aprende quanto pesa uma fatia tua**, quando ela não é a da tabela",
            ),
            highlightsEn = listOf(
                "**A cooked recipe no longer gains vitamins from the heat.** Evaporating " +
                    "water concentrated everything, vitamin C included — and that one is " +
                    "lost to boiling. Pick how you cooked it and each ingredient loses " +
                    "what its own table says",
                "**A millilitre no longer counts as a gram.** 200 ml of olive oil weigh " +
                    "184 g, and the app was counting 200",
                "**Search groups a food with its own states**: one line instead of seven " +
                    "near-identical ones, the rest a tap away",
                "**Pineapple finds ananás**, and cimbalino finds café. Typing «frango» " +
                    "also finds what is still in English",
                "**The app learns how much your own slice weighs**, when it is not the " +
                    "table's",
            ),
        ),
        AppVersion(
            name = "2.7.0",
            title = "A comida cozinha, mede-se e diz o que não sabe",
            titleEn = "Food cooks, measures itself, and says what it does not know",
            highlights = listOf(
                "**O cru e o cozinhado deixam de ser dois alimentos.** Escolhes o método e a " +
                    "app faz as contas: o peso que se perde e as vitaminas que sobrevivem. " +
                    "Se pesaste depois de cozinhar, esse peso vale mais do que a tabela",
                "**Sete vezes mais alimentos com porção** — «uma fatia», «uma chávena» — em " +
                    "vez de escreveres gramas",
                "**Dois mil alimentos deixam de ter nome de laboratório em inglês**, e 97 " +
                    "que estavam repetidos passam a estar uma vez só. O que ainda não tem " +
                    "nome inteiro em português fica em inglês, e não meio traduzido",
                "**O catálogo passa a poder descarregar-se**, nas Definições: corrigir um " +
                    "alimento deixa de esperar por uma versão na loja",
                "**Um nutriente procurado e não encontrado passa a dizê-lo**, em vez de " +
                    "desaparecer do ecrã como se ninguém o tivesse medido",
                "O dia diz-te de quanto é o «cerca de» das calorias, quando isso muda o que " +
                    "elas querem dizer",
            ),
            highlightsEn = listOf(
                "**Raw and cooked are no longer two foods.** Pick the method and the app does " +
                    "the sums: the weight lost and the vitamins that survive. If you weighed " +
                    "it after cooking, that weight beats the table",
                "**Seven times more foods with a serving** — «a slice», «a cup» — instead of " +
                    "typing grams",
                "**Two thousand foods lose their English lab names**, and 97 that were " +
                    "duplicated are now there once. What has no full Portuguese name yet " +
                    "stays in English, rather than half translated",
                "**The catalogue can now be downloaded**, from Settings: fixing a food no " +
                    "longer waits for a store release",
                "**A nutrient looked for and not found now says so**, instead of vanishing " +
                    "from the screen as if nobody had measured it",
                "Your day tells you how big its «about» is, when that changes what the " +
                    "numbers mean",
            ),
        ),
        AppVersion(
            name = "2.6.0",
            title = "O sódio e a fibra contam em todo o catálogo",
            titleEn = "Sodium and fibre count across the catalogue",
            highlights = listOf(
                "**Ver que alimentos são ricos em fibra ou em sódio passa a olhar para o " +
                    "catálogo todo**, e não só para os portugueses: de 1 376 alimentos " +
                    "para mais de 7 600",
                "O sódio deixa de poder aparecer com dois valores diferentes para o " +
                    "mesmo alimento, conforme o ecrã",
            ),
            highlightsEn = listOf(
                "**Finding which foods are rich in fibre or sodium now looks at the whole " +
                    "catalogue**, not just the Portuguese ones: from 1,376 foods to over " +
                    "7,600",
                "Sodium can no longer show two different values for the same food, " +
                    "depending on the screen",
            ),
        ),
        AppVersion(
            name = "2.5.0",
            title = "Os teus favoritos passam a ir na cópia",
            titleEn = "Your favourites now travel in the backup",
            highlights = listOf(
                "**Os favoritos, os recentes e as porções que guardaste passam a ir na " +
                    "cópia de segurança.** Não iam, e restaurar uma cópia apagava-os",
                "A cópia diz agora com que versão do catálogo foi feita, antes de a " +
                    "importares",
            ),
            highlightsEn = listOf(
                "**Your favourites, recents and saved portions now travel in the backup.** " +
                    "They did not, and restoring a backup wiped them",
                "The backup now says which catalogue version made it, before you import it",
            ),
        ),
        AppVersion(
            name = "2.4.0",
            title = "O catálogo passa a ser reconstruível",
            titleEn = "The catalogue can be rebuilt",
            highlights = listOf(
                "**Dezasseis alimentos que faltavam** — sumo de arando, puré de castanha, " +
                    "tomate comprido, cavala marinada e seis salsichas",
                "Uma receita já não pode perder um ingrediente quando o catálogo é " +
                    "atualizado",
                "Os teus favoritos, os recentes e as porções que guardaste sobrevivem à " +
                    "atualização do catálogo",
            ),
            highlightsEn = listOf(
                "**Sixteen foods that were missing** — cranberry juice, chestnut purée, " +
                    "long tomato, marinated mackerel and six sausages",
                "A recipe can no longer lose an ingredient when the catalogue updates",
                "Your favourites, recents and saved portions survive a catalogue update",
            ),
        ),
        AppVersion(
            name = "2.3.0",
            title = "A app passa a ter movimento",
            titleEn = "The app starts to move",
            highlights = listOf(
                "**Transições entre ecrãs**, e cada uma diz uma coisa: os separadores " +
                    "desvanecem, um detalhe entra da direita com o ecrã de trás a " +
                    "acompanhar, um treino a começar cresce de dentro, e um resumo assenta",
                "Se desligaste as animações no telemóvel, a app respeita isso",
                "Num tablet, tocar num alimento do diário **abre-o ao lado** em vez de " +
                    "tapar o diário",
                "O teclado deixou de tapar o que estás a escrever em trinta e dois ecrãs",
                "Cartões, linhas e margens iguais em toda a app — e num ecrã grande nada " +
                    "se estica para lá do que se lê bem",
            ),
            highlightsEn = listOf(
                "**Screen transitions**, and each one says something: tabs cross-fade, a " +
                    "detail slides in from the right with the screen behind following " +
                    "along, a workout grows from within, and a summary settles",
                "If you turned animations off on your phone, the app respects that",
                "On a tablet, tapping a food in the diary **opens it beside** the diary " +
                    "instead of covering it",
                "The keyboard no longer covers what you are typing, on thirty-two screens",
                "Cards, rows and margins the same across the app — and on a big screen " +
                    "nothing stretches past what reads well",
            ),
        ),
        // A 2.2.0 saiu daqui ao entrar a 2.20.0: a lista tem tecto de doze e o
        // `AppChangelogTest` cobra-o. O histórico completo é trabalho do `CHANGELOG.md`.
    )
}
