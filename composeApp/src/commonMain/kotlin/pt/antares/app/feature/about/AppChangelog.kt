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

    const val CURRENT = "2.7.0"

    val versions: List<AppVersion> = listOf(
        AppVersion(
            name = "2.7.0",
            title = "O catálogo já não espera pela loja",
            titleEn = "The catalogue no longer waits for the store",
            highlights = listOf(
                "**O catálogo de alimentos passa a poder descarregar-se**, nas Definições. " +
                    "Corrigir um alimento deixa de esperar por uma versão na loja",
                "Só desce quando carregas no botão, e o ficheiro é verificado antes de " +
                    "substituir o que tens. Se correr mal, fica tudo como estava — e o ecrã " +
                    "diz o que falhou",
                "**Um nutriente que foi procurado e não se encontrou passa a dizê-lo**, em " +
                    "vez de desaparecer do ecrã como se ninguém o tivesse medido",
                "Um vestígio aparece no alimento mas não entra na conta do dia: somá-lo era " +
                    "inventar um número que ninguém mediu",
            ),
            highlightsEn = listOf(
                "**The food catalogue can now be downloaded**, from Settings. Fixing a food " +
                    "no longer waits for a store release",
                "It only downloads when you tap the button, and the file is checked before " +
                    "it replaces what you have. If it goes wrong, nothing changes — and the " +
                    "screen says what failed",
                "**A nutrient that was looked for and not found now says so**, instead of " +
                    "vanishing from the screen as if nobody had measured it",
                "A trace shows on the food but does not count towards your day: adding it " +
                    "meant inventing a number nobody measured",
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
        AppVersion(
            name = "2.2.0",
            title = "A app diz o que sai daqui, e deixa-te cortar",
            titleEn = "The app says what leaves, and lets you cut it",
            highlights = listOf(
                "Um ecrã novo, no menu, com **tudo o que sai do telemóvel** e quando: a " +
                    "pesquisa de alimentos, a análise por foto, o mapa das corridas, as " +
                    "imagens dos exercícios — e a cópia de segurança, à parte, porque essa " +
                    "não sai para a Internet",
                "A app avisa **antes** de a primeira procura sair, e não depois. Recusar ali " +
                    "desliga a pesquisa em linha",
                "Interruptor para desligar a pesquisa em linha e a leitura de códigos de " +
                    "barras. Desligada, a app di-lo em vez de fingir que não há resultados",
                "A corrida passa a sugerir um nome — «Corrida da manhã» — e a lembrar-se do " +
                    "tipo de atividade e da auto-pausa que escolheste",
                "A primeira cópia de segurança já não sai vazia, e substituir os dados deixa " +
                    "de aceitar uma cópia sem um único registo",
            ),
            highlightsEn = listOf(
                "A new screen, in the menu, with **everything that leaves the phone** and " +
                    "when: food search, photo analysis, the run map, exercise images — and " +
                    "the backup, kept apart, because that one goes nowhere on the internet",
                "The app warns **before** the first search leaves, not after. Declining " +
                    "there turns online search off",
                "A switch to turn off online search and barcode lookups. With it off, the " +
                    "app says so instead of pretending there are no results",
                "Runs now suggest a name — «Morning run» — and remember the activity type " +
                    "and auto-pause you picked",
                "The first backup is no longer empty, and replacing your data no longer " +
                    "accepts a backup without a single record",
            ),
        ),
        AppVersion(
            name = "2.1.0",
            title = "A cópia de segurança deixa de depender de ti",
            titleEn = "The backup stops depending on you",
            highlights = listOf(
                "A app guarda sozinha uma cópia dos teus dados em **Documentos/Antares**, " +
                    "de três em três dias, e mantém as cinco últimas. A pasta fica fora da " +
                    "app: continua lá se desinstalares",
                "Um cartão diz há quantos dias foi a última cópia, no menu e — quando passa " +
                    "de uma semana — no Hoje",
                "A cópia deixou de ir para a Google. Era uma cópia que ninguém via, que " +
                    "dependia de estares com sessão iniciada, e que não levava as fotos",
                "Antes de importar, a app diz o que está no ficheiro: a data, a versão que " +
                    "o escreveu e quantos registos traz de cada tipo",
                "Limpar as pesquisas falhadas passa a pedir confirmação",
            ),
            highlightsEn = listOf(
                "The app keeps a backup of your data in **Documents/Antares** on its own, " +
                    "every three days, keeping the last five. The folder sits outside the " +
                    "app: it stays there if you uninstall",
                "A card says how many days ago the last backup was, in the menu and — once " +
                    "it passes a week — on Today",
                "Backups no longer go to Google. That was a backup nobody could see, that " +
                    "depended on being signed in, and that left the photos out",
                "Before importing, the app says what is in the file: the date, the version " +
                    "that wrote it, and how many records of each kind it brings",
                "Clearing the failed searches now asks for confirmation",
            ),
        ),
        AppVersion(
            name = "2.0.4",
            title = "Nove coisas que a app dizia e não eram verdade",
            titleEn = "Nine things the app said that were not true",
            highlights = listOf(
                "A janela da tendência e a meta de massa gorda escolhiam-se, gravavam-se, e " +
                    "nada as lia: agora a janela governa mesmo a conta, e a meta aparece ao " +
                    "lado da medida a que se compara",
                "A instrução de medir a cintura era igual para os dois sexos, e a fórmula " +
                    "pede sítios diferentes — o umbigo no homem, o ponto mais estreito na " +
                    "mulher. O pescoço ganhou a instrução que nunca teve",
                "Tocar num micronutriente que está a 34 % não levava a lado nenhum, e o ecrã " +
                    "dos alimentos ricos nele já existia",
                "O desnível da corrida era medido e gravado, e faltava no resumo — e no " +
                    "detalhe vinha em metros a quem usa pés",
                "O interruptor das metas adaptativas estava em dois ecrãs, o relógio do jejum " +
                    "redesenhava-se sessenta vezes por minuto para mudar uma, e a " +
                    "administração estava à vista de quem só queria mudar o tema",
            ),
            highlightsEn = listOf(
                "The trend window and the body-fat goal were chosen, saved, and read by " +
                    "nothing: the window now drives the calculation, and the goal shows next " +
                    "to the measurement it compares against",
                "The waist measuring instruction was the same for both sexes, and the formula " +
                    "asks for different places — the navel for men, the narrowest point for " +
                    "women. The neck got the instruction it never had",
                "Tapping a micronutrient sitting at 34% led nowhere, and the screen listing " +
                    "foods rich in it already existed",
                "Run elevation was measured and saved, and missing from the summary — and the " +
                    "detail screen showed metres to people using feet",
                "The adaptive-targets switch lived on two screens, the fasting clock redrew " +
                    "sixty times a minute to change once, and admin sat in plain sight of " +
                    "anyone who just wanted to change the theme",
            ),
        ),
        AppVersion(
            name = "2.0.3",
            title = "Os números deixam de mudar sozinhos",
            titleEn = "Numbers stop changing on their own",
            highlights = listOf(
                "Uma série feita a 62,5 kg voltava pré-preenchida a 63, e a linha da série " +
                    "gravada dizia 63 enquanto a correção dizia 62,5 — a mesma série, dois números",
                "Uma série gravada com o peso errado só se resolvia apagando e refazendo — e o " +
                    "descanso recomeçava. Agora toca-se na linha e corrige-se",
                "Apagar uma série a meio fazia a seguinte repetir um número já usado, e é por " +
                    "esse número que o histórico ordena",
                "Oito estilos de letra caíam na fonte do sistema em vez da da app, em 176 " +
                    "sítios — o maior deles o texto pequeno, que está em todo o lado",
                "Uma altura fora de 100–250 cm era ignorada sem dizer nada, e o metabolismo " +
                    "continuava a ser calculado com a altura antiga",
            ),
            highlightsEn = listOf(
                "A set logged at 62.5 kg came back pre-filled as 63, and the saved row said 63 " +
                    "while the edit dialog said 62.5 — the same set, two numbers",
                "A set saved with the wrong weight could only be deleted and redone — and the " +
                    "rest timer restarted. Now you tap the row and fix it",
                "Deleting a set mid-workout made the next one repeat an index already in use, " +
                    "and that index is what the history sorts by",
                "Eight text styles fell back to the system font instead of the app's, in 176 " +
                    "places — the largest being small text, which is everywhere",
                "A height outside 100–250 cm was ignored without a word, and your metabolism " +
                    "kept being calculated from the old one",
            ),
        ),
        AppVersion(
            name = "2.0.2",
            title = "O imperial passa a ser imperial em todo o lado",
            titleEn = "Imperial becomes imperial everywhere",
            highlights = listOf(
                "Quem escolhe libras via «153,9 lb» e, três linhas abaixo, «0,4 kg/semana» " +
                    "e «Tendência: 70,6 kg» — a mesma pessoa em duas escalas",
                "Foram catorze textos: o check-in semanal, o ritmo, a faixa saudável, a massa " +
                    "magra e a gorda, a meta, os avisos da pesagem, a cintura e a altura",
                "A cintura passa a polegadas e a altura a pés e polegadas, para quem usa imperial",
                "Nos totais das corridas, «107:56:02» partia-se em duas linhas",
            ),
            highlightsEn = listOf(
                "In pounds you saw \"153.9 lb\" and, three lines below, \"0.4 kg/week\" and " +
                    "\"Trend: 70.6 kg\" — the same person on two scales",
                "Fourteen texts in all: the weekly check-in, the rate, the healthy range, lean " +
                    "and fat mass, the goal, the weigh-in prompts, the waist and the height",
                "Waist now reads in inches and height in feet and inches for imperial",
                "In the run totals, \"107:56:02\" broke across two lines",
            ),
        ),
        AppVersion(
            name = "2.0.1",
            title = "Três coisas que só se viam num ecrã",
            titleEn = "Three things you could only see on a screen",
            highlights = listOf(
                "Em libras, o volume do treino mostrava quilos, e a distância misturava vírgula com ponto",
                "O gráfico do peso tinha os extremos da escala onde deviam estar o primeiro e o " +
                    "último dia — parecia uma subida onde havia descida",
                "Num tablet, as listas dentro do painel de detalhe apertavam-se em três colunas onde só cabiam duas",
            ),
            highlightsEn = listOf(
                "In pounds, workout volume still showed kilograms, and distance mixed comma with dot",
                "The weight chart put the scale's ends where the first and last day should be " +
                    "— it read as a rise where there was a fall",
                "On a tablet, lists inside the detail pane squeezed into three columns where only two fit",
            ),
        ),
        AppVersion(
            name = "2.0.0",
            title = "A app roda, e as contas ficam honestas",
            titleEn = "The app rotates, and the maths gets honest",
            highlights = listOf(
                "**A app roda.** Deitada, num tablet ou em ecrã dividido, deixa de ser um telemóvel esticado",
                "Num tablet, o exercício **abre ao lado da lista** em vez de a tapar",
                "**Desfazer em tudo o que apaga** — um registo, uma pesagem, uma série, uma rotina",
                "O sistema imperial passou a ser um sistema inteiro: peso, distância, ritmo e porções",
                "As calorias do exercício passam a ser **o que ele gasta a mais** do que estar sentado",
                "A meta de água segue a referência da EFSA, por sexo, e **a água da comida conta**",
                "O basal calculado a partir da fita métrica passa a dizer a margem que tem",
                "O chão de proteína sobe com o treino e com a profundidade do défice, e diz porquê",
                "Receitas registam-se **por dose** — registar lasanha deixou de propor comê-la toda",
                "Dez nutrientes novos, que as tabelas já traziam e a app deitava fora",
                "Os históricos filtram-se por mês, por exercício e por tipo",
                "O ecrã de conquistas saiu: os marcos do Progresso dizem a mesma coisa sem enfeite",
            ),
            highlightsEn = listOf(
                "**The app rotates.** Landscape, tablet or split screen — no longer a stretched phone",
                "On a tablet, an exercise **opens beside the list** instead of covering it",
                "**Undo on everything that deletes** — a log, a weigh-in, a set, a routine",
                "Imperial is now a whole system: weight, distance, pace and portions",
                "Exercise calories are now **what it burns beyond sitting still**",
                "The water target follows the EFSA reference, by sex, and **water from food counts**",
                "A BMR computed from tape measurements now states the margin it carries",
                "The protein floor rises with training and with how deep the deficit is, and says why",
                "Recipes are logged **per serving** — logging lasagna no longer proposes eating all of it",
                "Ten new nutrients, which the tables already carried and the app was throwing away",
                "Histories filter by month, by exercise and by type",
                "The achievements screen is gone: the Progress milestones say the same without the trim",
            ),
        ),
    )
}
