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

    const val CURRENT = "2.29.0"

    val versions: List<AppVersion> = listOf(
        AppVersion(
            name = "2.29.0",
            title = "A corrida ganha o botão que toda a gente procura",
            titleEn = "Running gets the button everyone looks for",
            highlights = listOf(
                "**Pausar e retomar.** Não havia. Atar o sapato obrigava a esperar dez " +
                    "segundos pela pausa automática, ou a terminar a corrida",
                "**Terminar só aparece com a corrida em pausa** — que é quando a decisão " +
                    "faz sentido. A correr, o sítio mais premido do ecrã deixa de ser um " +
                    "botão vermelho que acaba tudo",
                "**O cadeado passa a bloquear o ecrã todo**, incluindo o gesto de voltar do " +
                    "sistema. Abre-se com o dedo em cima dele, sem pressa",
                "**«Ritmo deste km» em vez de «ritmo atual».** O antigo vinha da velocidade " +
                    "do último bocado e saltava de 4:10 para 6:50 e de volta. Não se lia",
                "**Os dois últimos quilómetros à vista**, para saberes se estás a acelerar " +
                    "ou a abrandar sem esperar pelo fim",
            ),
            highlightsEn = listOf(
                "**Pause and resume.** There was none. Tying a shoelace meant waiting ten " +
                    "seconds for auto-pause, or ending the run",
                "**Finish only shows up while paused** — which is when the decision makes " +
                    "sense. While running, the most-pressed spot on the screen is no longer " +
                    "a red button that ends everything",
                "**The padlock now locks the whole screen**, including the system back " +
                    "gesture. Press and hold it to unlock",
                "**\"This km's pace\" instead of \"current pace\".** The old one came from " +
                    "the speed of the last stretch and jumped from 4:10 to 6:50 and back",
                "**The last two kilometres in view**, so you know whether you are speeding " +
                    "up or slowing down without waiting for the end",
            ),
        ),
        AppVersion(
            name = "2.28.0",
            title = "Uma rotina passa a poder subir sozinha",
            titleEn = "A routine can now go up on its own",
            highlights = listOf(
                "**Cada rotina escolhe uma regra de progressão** — nenhuma, linear, ou " +
                    "dupla. Uma rotina era 3×8-12 hoje e 3×8-12 daqui a três meses",
                "**A linear sobe o peso** quando fizeste o topo do intervalo em todas as " +
                    "séries. A **dupla** sobe primeiro as repetições até ao máximo, e só " +
                    "depois o peso, voltando ao mínimo",
                "**A app propõe e não reescreve.** O alvo aparece no editor e no campo da " +
                    "sessão; o peso que escreveste na rotina fica exactamente onde estava",
                "**Cada linha diz o que fizeste da última vez** — «↑ da última vez: 3×10 a " +
                    "60 kg» —, que é de onde a proposta saiu",
                "**Sobe 2,5 kg, ou 5 lb** se contas em libras. Não são o mesmo número, e " +
                    "não podiam ser: são os discos que existem em cada ginásio",
            ),
            highlightsEn = listOf(
                "**Every routine picks a progression rule** — none, linear, or double. A " +
                    "routine was 3×8-12 today and 3×8-12 three months from now",
                "**Linear adds weight** when you hit the top of the range on every set. " +
                    "**Double** takes reps to the maximum first, and only then adds weight, " +
                    "going back to the minimum",
                "**The app suggests, it does not rewrite.** The target shows up in the " +
                    "editor and in the session field; the weight you wrote stays put",
                "**Every line says what you did last time** — \"↑ last time: 3×10 at " +
                    "60 kg\" — which is where the suggestion came from",
                "**It goes up by 2.5 kg, or 5 lb** if you count in pounds. They are not the " +
                    "same number, and they could not be: they are the plates that exist",
            ),
        ),
        AppVersion(
            name = "2.27.0",
            title = "A biblioteca deixa de tratar 873 exercícios por igual",
            titleEn = "The library stops treating 873 exercises the same",
            highlights = listOf(
                "**Os teus exercícios ficam no topo** — os que marcaste, e os que mais " +
                    "fazes. Toda a gente treina os mesmos quinze ou vinte, e a lista " +
                    "mostrava-os todos por ordem alfabética, todas as vezes",
                "**Uma estrela em cada linha** para marcares os teus. Vão na cópia de " +
                    "segurança, como tudo o resto que escolhes",
                "**O detalhe diz o que já fizeste** — a melhor série, o 1RM estimado, " +
                    "quantas vezes e a última. A app sabia os quatro e mostrava-os noutros ecrãs",
                "**O filtro de nível saiu e entrou «só os meus».** Ninguém procura exercícios " +
                    "de nível intermédio; quem criou um à mão procura-o",
                "**Apagar um exercício teu passa a perguntar**, e diz em quantas rotinas ele " +
                    "está. Era a única coisa que se apagava sem confirmação nem volta atrás",
                "**A escolher um exercício para uma rotina, a lista diz o que já lá está.** " +
                    "Mostrava os 873 sem distinguir, e acrescentar o mesmo duas vezes era fácil",
                "**Os nomes dos filtros deixam de ser cortados a meio.** Lia-se «Equipame», " +
                    "e com a letra grande «Mús» e «Equi»",
            ),
            highlightsEn = listOf(
                "**Your exercises come first** — the ones you starred, and the ones you do " +
                    "most. Everyone trains the same fifteen or twenty, and the list showed " +
                    "all of them alphabetically, every time",
                "**A star on every line** to mark your own. They travel in the backup, like " +
                    "everything else you choose",
                "**The detail says what you have already done** — best set, estimated 1RM, " +
                    "how many times and the last one. The app knew all four and showed them elsewhere",
                "**The level filter is gone and “only mine” took its place.** Nobody searches " +
                    "for intermediate-level exercises; someone who made one by hand searches for it",
                "**Deleting one of your exercises now asks**, and says how many routines it " +
                    "is in. It was the only thing deleted with no confirmation and no undo",
                "**Picking an exercise for a routine, the list says what is already there.** " +
                    "It showed all 873 without distinction, and adding the same one twice was easy",
                "**Filter labels stop being cut in half.** It read “Equipame”, and with large " +
                    "text “Mús” and “Equi”",
            ),
        ),
        AppVersion(
            name = "2.26.0",
            title = "O fim de um treino passa a dizer se ele foi melhor",
            titleEn = "Finishing a workout now tells you if it was better",
            highlights = listOf(
                "**O resumo compara com a última vez que fizeste esta rotina** — a duração, " +
                    "o volume e as séries, lado a lado. Mostrava três números e não os " +
                    "comparava com nada",
                "**E com a média das últimas três**, por baixo: a última vez aponta para um " +
                    "treino que podes ir ver, a média não se deixa enganar por um dia mau",
                "**«Sem recordes» desapareceu.** Dizê-lo a seguir a cada treino normal " +
                    "transformava a ausência de recorde num facto negativo repetido",
                "**Podes partilhar o resumo como imagem**, com o mesmo botão do progresso",
                "**O cartão do treino deixa de dizer «ainda não treinaste»** a quem só fez " +
                    "treinos livres — treinaste; o que não fizeste foi treinar uma rotina",
                "**Sem perfil, o «Hoje» leva-te às perguntas** em vez de ser um beco: era " +
                    "uma frase no meio do ecrã e mais nada",
            ),
            highlightsEn = listOf(
                "**The summary compares with the last time you did this routine** — " +
                    "duration, volume and sets, side by side. It showed three numbers and " +
                    "compared them with nothing",
                "**And with the average of the last three**, underneath: the last time " +
                    "points at a workout you can go and look at, the average is not fooled " +
                    "by one bad day",
                "**“No records” is gone.** Saying it after every ordinary workout turned the " +
                    "absence of a record into a repeated piece of bad news",
                "**You can share the summary as an image**, with the same button as progress",
                "**The workout card stops saying “you haven't trained yet”** to someone who " +
                    "only did free workouts — you did; what you did not do was train a routine",
                "**With no profile, Today takes you to the questions** instead of being a " +
                    "dead end: it was a sentence in the middle of the screen and nothing else",
            ),
        ),
        AppVersion(
            name = "2.25.0",
            title = "As estatísticas do treino passam a ter tempo",
            titleEn = "Your training stats get a sense of time",
            highlights = listOf(
                "**Escolhes o período** — dia, semana, mês ou ano —, e ele governa o ecrã " +
                    "inteiro. Dizia «esta semana» e não deixava mudar",
                "**Séries por músculo por semana, com a faixa de 10 a 20** que a literatura " +
                    "usa para crescer. É a conta que responde a «estou a treinar o " +
                    "suficiente?», e o volume, que não é comparável entre músculos, deixa de " +
                    "ser ele a desenhar a barra",
                "**Treinos por semana e volume por semana, em linha**, para se ver uma " +
                    "paragem em vez de a adivinhar",
                "**Os recordes dizem quando aconteceram**, e o mais recente vem assinalado. " +
                    "Um de 2024 aparecia igual a um de ontem",
                "**«Esta semana» quer dizer o mesmo em toda a app** — de segunda a domingo. " +
                    "Aqui contava sete dias para trás e no painel de treino contava a semana",
            ),
            highlightsEn = listOf(
                "**You pick the period** — day, week, month or year — and it governs the " +
                    "whole screen. It used to say “this week” and let you change nothing",
                "**Sets per muscle per week, with the 10-to-20 band** the literature uses " +
                    "for growth. It is the count that answers “am I training enough?”, and " +
                    "volume — which is not comparable between muscles — stops being the one " +
                    "that draws the bar",
                "**Workouts per week and volume per week, as a line**, so a break shows up " +
                    "instead of having to be guessed",
                "**Records say when they happened**, and the most recent one is marked. One " +
                    "from 2024 looked just like one from yesterday",
                "**“This week” means the same thing everywhere in the app** — Monday to " +
                    "Sunday. Here it counted seven days back while the workout panel counted " +
                    "the week",
            ),
        ),
        AppVersion(
            name = "2.24.0",
            title = "O histórico deixa de dizer duas coisas sobre cada treino",
            titleEn = "The history stops saying two things about each workout",
            highlights = listOf(
                "**Cada linha do histórico diz a rotina, a data, a duração e as séries** — " +
                    "eram a data e o volume, e dois treinos completamente diferentes ficavam " +
                    "iguais",
                "**Uma 🌟 nos treinos em que bateste um recorde**, e é o recorde do dia em " +
                    "que ele aconteceu, não o melhor de hoje",
                "**Abrir um treino diz qual foi e quando** — a rotina no título, a data e a " +
                    "hora por baixo. Dizia «Treino» e mais nada",
                "**O RPE que escreves aparece**. Era gravado desde sempre e não se via em " +
                    "lado nenhum depois de o escrever",
                "**Filtras o histórico por rotina**, e já não por exercício: quem procura o " +
                    "supino quer a progressão do supino, e essa está no exercício",
            ),
            highlightsEn = listOf(
                "**Every history row now shows the routine, the date, the duration and the " +
                    "sets** — it was the date and the volume, and two completely different " +
                    "workouts looked the same",
                "**A 🌟 on the workouts where you set a record**, and it is the record as it " +
                    "stood that day, not your best today",
                "**Opening a workout tells you which one and when** — the routine in the " +
                    "title, the date and time below. It used to say “Workout” and nothing else",
                "**The RPE you write shows up.** It was always saved and never shown " +
                    "anywhere after you wrote it",
                "**You filter the history by routine**, not by exercise: if you are looking " +
                    "for the bench press you want its progression, and that lives in the " +
                    "exercise",
            ),
        ),
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
        // Da 2.2.0 à 2.19.0 saíram daqui à medida que entraram as versões novas: a lista tem
        // tecto de doze e o `AppChangelogTest` cobra-o. O histórico completo é trabalho do
        // `CHANGELOG.md`, que não tem tecto nenhum.
    )
}
