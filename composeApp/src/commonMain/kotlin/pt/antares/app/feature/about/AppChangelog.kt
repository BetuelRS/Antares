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

    const val CURRENT = "2.23.1"

    val versions: List<AppVersion> = listOf(
        AppVersion(
            name = "2.23.1",
            title = "O horário passa a avisar, e uma supersérie passa a ser uma supersérie",
            titleEn = "The schedule starts reminding you, and a superset becomes a superset",
            highlights = listOf(
                "**O treino marcado no horário avisa-te**, à hora que escolheres. Desligado " +
                    "por omissão, e nunca avisa se já treinaste nesse dia",
                "**Uma supersérie abre os dois exercícios ao mesmo tempo** — que é o que ela " +
                    "é. Antes só um podia registar, e a app trocava de exercício sozinha a " +
                    "meio da supersérie",
            ),
            highlightsEn = listOf(
                "**The workout on your schedule reminds you**, at the time you choose. Off by " +
                    "default, and never if you already trained that day",
                "**A superset opens both exercises at once** — which is what a superset is. " +
                    "Before, only one could be logged, and the app switched exercises on its " +
                    "own halfway through",
            ),
        ),
        AppVersion(
            name = "2.23.0",
            title = "Reordenar uma rotina passa a ser arrastar",
            titleEn = "Reordering a routine becomes dragging",
            highlights = listOf(
                "**Arrastar para reordenar.** Pôr o sexto exercício em primeiro custava cinco " +
                    "toques, com a lista a saltar debaixo do dedo em cada um",
                "**Duplicar uma rotina**, com os exercícios, os alvos e as superséries — e sem " +
                    "ir ocupar os dias da original no calendário",
                "**Mudar o nome** da rotina, no menu do canto",
                "**Os alvos acertam-se com − e +**, com atalhos para 3, 4 ou 5 séries e para " +
                    "60, 90, 120 ou 180 segundos. Eram cinco campos de texto empilhados",
                "**O peso alvo da rotina passa a servir para alguma coisa**: um exercício sem " +
                    "histórico já não abre o campo vazio",
                "Desfazer no mover, e a supersérie a agrupar mesmo o que diz que agrupa",
            ),
            highlightsEn = listOf(
                "**Drag to reorder.** Moving the sixth exercise to the top took five taps, " +
                    "with the list jumping under your finger each time",
                "**Duplicate a routine**, with its exercises, targets and supersets — and " +
                    "without taking over the original's days on the schedule",
                "**Rename** a routine, from the corner menu",
                "**Targets are set with − and +**, with shortcuts for 3, 4 or 5 sets and for " +
                    "60, 90, 120 or 180 seconds. They were five stacked text fields",
                "**The routine's target weight finally does something**: an exercise with no " +
                    "history no longer opens an empty field",
                "Undo on move, and the superset actually grouping what it says it groups",
            ),
        ),
        AppVersion(
            name = "2.22.0",
            title = "Uma flexão passa a poder registar-se",
            titleEn = "A push-up can finally be logged",
            highlights = listOf(
                "**Cento e onze exercícios do catálogo não se conseguiam registar** — flexões, " +
                    "dominadas, fundos, prancha. A série exigia um peso, e uma flexão não tem " +
                    "peso para escrever",
                "**O teu peso entra como carga**, e a conta fica à vista: «O teu peso: 78 kg»",
                "**Carga extra para quem usa cinto**, somada por cima",
                "**Dizes quanto do teu peso conta em cada exercício.** Uma flexão levanta menos " +
                    "do que uma dominada — e o número é teu, porque não é um número que a app " +
                    "possa medir por ti",
                "Sem peso registado, a app diz que falta em vez de assumir um",
            ),
            highlightsEn = listOf(
                "**A hundred and eleven exercises in the catalogue could not be logged** — " +
                    "push-ups, pull-ups, dips, planks. A set required a weight, and a push-up " +
                    "has no weight to type",
                "**Your weight becomes the load**, and the sum is in plain sight: " +
                    "«Your weight: 78 kg»",
                "**Added load for anyone using a belt**, on top of your weight",
                "**You say how much of your weight counts on each exercise.** A push-up lifts " +
                    "less than a pull-up — and the number is yours, because it is not one the " +
                    "app can measure for you",
                "With no weight logged, the app says so instead of assuming one",
            ),
        ),
        AppVersion(
            name = "2.21.0",
            title = "A sessão de treino deixa de pedir tantos toques",
            titleEn = "The workout session stops asking for so many taps",
            highlights = listOf(
                "**O relógio do treino na barra**, ao segundo. A duração só se sabia no fim",
                "**A calculadora de discos**: que discos pôr de cada lado, por baixo do peso. " +
                    "Em libras são discos de libras, e não os métricos convertidos",
                "**O teclado salta de peso para repetições e grava** — eram dois toques por " +
                    "série só para mudar de campo",
                "**O RPE sai da linha e passa para o menu da série.** Era um campo permanente " +
                    "para um número que a maioria não escreve",
                "**Notas por exercício**, do treino de hoje e não da rotina",
                "**O 1RM estimado à vista**, já com as séries de hoje dentro, e o **recorde " +
                    "dito no momento** em que acontece",
                "O título passa a ser o nome da rotina, e o chip de supersérie deixa de " +
                    "fingir que é tocável",
            ),
            highlightsEn = listOf(
                "**The workout clock in the top bar**, to the second. You only knew the " +
                    "duration at the end",
                "**The plate calculator**: what to load on each side, under the weight. In " +
                    "pounds it uses pound plates, not converted metric ones",
                "**The keyboard jumps from weight to reps and saves** — it was two taps per " +
                    "set just to change field",
                "**RPE leaves the row and moves into the set's menu.** It was a permanent " +
                    "field for a number most people never fill in",
                "**Notes per exercise**, belonging to today's workout and not to the routine",
                "**The estimated 1RM in sight**, today's sets included, and the **record " +
                    "announced the moment** it happens",
                "The title becomes the routine's name, and the superset chip stops pretending " +
                    "to be tappable",
            ),
        ),
        AppVersion(
            name = "2.20.1",
            title = "A barra de baixo muda, e a corrida passa para o treino",
            titleEn = "The bottom bar changes, and running moves into Train",
            highlights = listOf(
                "**O progresso ganha separador próprio.** Era a primeira coisa dentro do " +
                    "«Perfil», atrás de um ícone de pessoa — e é o melhor ecrã da app",
                "**A corrida sai da barra e vive dentro do Treino**, com os quilómetros da " +
                    "semana e a última corrida. São os dois atividade, e ela ocupava um " +
                    "quinto da barra para uma coisa que se faz umas vezes por mês",
                "**O «Perfil» passa a «Mais»**, e junta num sítio só o que estava em dois: " +
                    "os atalhos do corpo e o menu da app, que vivia atrás de uma engrenagem",
                "**Os períodos do progresso deixam de se cortar.** «3 meses» lia-se «3» num " +
                    "telemóvel estreito — um período que não existe",
            ),
            highlightsEn = listOf(
                "**Progress gets its own tab.** It was the first thing inside «Profile», " +
                    "behind a person icon — and it is the best screen in the app",
                "**Running leaves the bar and lives inside Train**, with this week's " +
                    "distance and your last run. Both are activity, and it took up a fifth " +
                    "of the bar for something you do a few times a month",
                "**«Profile» becomes «More»**, and gathers in one place what was in two: " +
                    "the body shortcuts and the app menu, which lived behind a gear icon",
                "**The progress periods stop being clipped.** «3 months» read as «3» on a " +
                    "narrow phone — a period that does not exist",
            ),
        ),
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
        // Da 2.2.0 à 2.7.0 saíram daqui à medida que entraram as versões novas: a lista tem
        // tecto de doze e o `AppChangelogTest` cobra-o. O histórico completo é trabalho do
        // `CHANGELOG.md`, que não tem tecto nenhum.
    )
}
