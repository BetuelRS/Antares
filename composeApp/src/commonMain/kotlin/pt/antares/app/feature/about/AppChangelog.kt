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

    const val CURRENT = "2.0.3"

    val versions: List<AppVersion> = listOf(
        AppVersion(
            name = "2.0.3",
            title = "Os números deixam de mudar sozinhos",
            titleEn = "Numbers stop changing on their own",
            highlights = listOf(
                "Uma série feita a 62,5 kg voltava pré-preenchida a 63: a app arredondava em " +
                    "silêncio um número que tinha sido registado",
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
                "A set logged at 62.5 kg came back pre-filled as 63: the app silently rounded " +
                    "a number you had recorded",
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
        AppVersion(
            name = "1.0.0",
            title = "A app vive no telemóvel",
            titleEn = "The app lives on your phone",
            highlights = listOf(
                "**Nada do que registas sai deste telemóvel.** Não há conta, não há servidor com uma cópia",
                "O backup passou a ser um ficheiro teu: **exportas, guardas onde quiseres, e restauras** — e leva as fotos de progresso",
                "Ao restaurar podes **juntar** ao que já tens (fica o mais recente) ou **substituir** tudo",
                "O **Backup** tem lugar próprio nas Definições, a um toque — estava escondido no fim de «Detalhes e metas»",
                "Quando a app fecha sozinha, o motivo fica guardado e podes **partilhá-lo**",
                "Dois exercícios com o mesmo nome deixaram de rebentar o detalhe do treino",
                "A app arrancava a ler um megabyte de exercícios **todas as vezes**, mesmo já os tendo",
            ),
            highlightsEn = listOf(
                "**Nothing you log leaves this phone.** There is no account, and no server holding a copy",
                "Backup is now a file you own: **export it, keep it where you like, restore it** — and it carries your progress photos",
                "When restoring you can **merge** with what you have (most recent wins) or **replace** everything",
                "**Backup** now has its own place in Settings, one tap away — it was buried at the bottom of «Details and goals»",
                "When the app closes on its own, the reason is saved and you can **share it**",
                "Two exercises with the same name no longer crash the workout detail",
                "The app was reading a megabyte of exercises on **every single start**, even with them already loaded",
            ),
        ),
        AppVersion(
            name = "0.14.0",
            title = "O ciclo, e o que ele faz à balança",
            titleEn = "The cycle, and what it does to the scale",
            highlights = listOf(
                "Registo do ciclo, **só neste telemóvel**, para a balança deixar de confundir retenção com gordura",
                "A retenção do ciclo dá 1 a 3 kg — a app diz isso em vez de te deixar pensar que é gordura",
                "Referências de nutrientes na gravidez, amamentação e pós-menopausa",
                "Em gravidez e amamentação a app deixa de propor défice",
            ),
            highlightsEn = listOf(
                "Cycle logging, **on this phone only**, so the scale stops mistaking retention for fat",
                "Cycle retention is 1 to 3 kg — the app says so instead of letting you think it is fat",
                "Nutrient references for pregnancy, breastfeeding and post-menopause",
                "During pregnancy and breastfeeding the app no longer proposes a deficit",
            ),
        ),
        AppVersion(
            name = "0.13.0",
            title = "A app lembra-se",
            titleEn = "The app remembers",
            highlights = listOf(
                "Separador **Progresso** — o único ecrã que olha para trás",
                "Gráfico do peso a sério, com escala, tendência e o teu peso-alvo marcado",
                "Mapa de consistência: doze semanas de dias registados, de relance",
                "A linha do tempo dos teus objetivos — quando os puseste e quando lá chegaste",
                "Marcos: 30 dias registados, 5 kg de mudança, factuais e sem elogios vazios",
                "Fotos de progresso, **só neste telemóvel**",
                "**1376 alimentos portugueses do INSA**, medidos em Portugal, todos com micros",
                "«Já estiveste aqui»: o mesmo peso, mas o corpo já não é o mesmo",
                "Braço, coxa e peito juntam-se à cintura nas medições",
            ),
            highlightsEn = listOf(
                "New **Progress** tab — the one screen that looks backwards",
                "A real weight chart, with scale, trend and your goal weight marked",
                "Consistency map: twelve weeks of logged days, at a glance",
                "The timeline of your goals — when you set them and when you got there",
                "Milestones: 30 days logged, 5 kg of change, factual and without empty praise",
                "Progress photos, **on this phone only**",
                "**1376 Portuguese foods from INSA**, measured in Portugal, all with micronutrients",
                "\"You have been here\": the same weight, but the body is no longer the same",
                "Arm, thigh and chest join the waist in measurements",
            ),
        ),
        AppVersion(
            name = "0.12.0",
            title = "Mostra-me a conta",
            titleEn = "Show me the maths",
            highlights = listOf(
                "**«Mostra-me a conta»** — a aritmética da tua meta, aberta, com os teus números",
                "Quando a balança não responde há semanas, a app pára de cortar e explica porquê",
                "Pausa de dieta: comer à manutenção umas semanas em vez de cortar mais",
                "Chegar ao peso-alvo deixa de te manter em défice — a app propõe manutenção",
                "Um peso-alvo abaixo do saudável para a tua altura passa a ser dito",
                "A partir dos 65 o mínimo de proteína sobe; abaixo dos 18 o ritmo é mais conservador",
            ),
            highlightsEn = listOf(
                "**\"Show me the maths\"** — the arithmetic of your target, open, with your numbers",
                "When the scale has not moved for weeks, the app stops cutting and explains why",
                "Diet break: eat at maintenance for a few weeks instead of cutting further",
                "Reaching your goal weight no longer keeps you in a deficit — the app proposes maintenance",
                "A goal weight below what is healthy for your height is now said out loud",
                "From 65 the protein minimum rises; under 18 the rate is more conservative",
            ),
        ),
        AppVersion(
            name = "0.11.0",
            title = "O teu corpo, e o teu rumo",
            titleEn = "Your body, and your direction",
            highlights = listOf(
                "Composição corporal: sabes o valor, estimas por medidas ou pelo IMC — e a app diz sempre qual foi",
                "Com a tua % de gordura, a meta é calculada pela massa magra em vez do peso todo",
                "O ritmo escolhe-se em kg por semana; as calorias são a consequência, com zona segura à vista",
                "Peso-alvo com data prevista pelo teu ritmo real — e a app cala-se quando os dados não chegam",
                "Régua do IMC a sério, FFMI, e a manutenção sempre à vista",
                "O nível de atividade passou a contar o dia sem treinos; antes o exercício contava duas vezes",
                "Altura em pés e polegadas para quem usa imperial",
            ),
            highlightsEn = listOf(
                "Body composition: you know the value, estimate from measurements or from BMI — and the app always says which",
                "With your body fat known, the target is computed from lean mass instead of total weight",
                "Rate is chosen in kg per week; the calories are the consequence, with the safe zone in view",
                "Goal weight with a date projected from your real rate — and the app stays quiet when the data is not enough",
                "A proper BMI scale, FFMI, and maintenance always in view",
                "Activity level now describes the day without workouts; exercise used to count twice",
                "Height in feet and inches for anyone using imperial",
            ),
        ),
        AppVersion(
            name = "0.10.0",
            title = "A app tira-se do caminho",
            titleEn = "The app gets out of the way",
            highlights = listOf(
                "**Outra vez** — repete num toque a última vez que comeste essa refeição",
                "A app aprende a tua dose habitual de cada alimento — e nunca inventa números",
                "Guarda uma refeição inteira e repete-a; copia-a de qualquer dia",
                "Marca vários alimentos e regista-os todos de uma vez",
                "Os teus 20 alimentos e as tuas 5 refeições à cabeça da pesquisa",
                "Os líquidos deixaram de ser contados como sólidos",
            ),
            highlightsEn = listOf(
                "**Again** — repeat the last time you ate that meal, in one tap",
                "The app learns your usual portion of each food — and never invents numbers",
                "Save a whole meal and repeat it; copy it from any day",
                "Tick several foods and log them all at once",
                "Your top 20 foods and top 5 meals up front in search",
                "Liquids stopped being counted as solids",
            ),
        ),
        AppVersion(
            name = "0.9.0",
            title = "A balança preenche-se sozinha",
            titleEn = "The scale fills itself in",
            highlights = listOf(
                "**Health Connect**: o peso e os treinos entram sozinhos",
                "A tua balança inteligente preenche a % de gordura sozinha",
                "A app escreve a composição corporal no Health Connect, além de a ler",
                "Cada pesagem passa a saber de onde veio, para não voltar a entrar duas vezes",
            ),
            highlightsEn = listOf(
                "**Health Connect**: weight and workouts come in by themselves",
                "Your smart scale fills in body fat by itself",
                "The app writes body composition to Health Connect as well as reading it",
                "Each weigh-in now knows where it came from, so it never lands twice",
            ),
        ),
        AppVersion(
            name = "0.8.0",
            title = "A semana passada, em factos",
            titleEn = "Last week, in facts",
            highlights = listOf(
                "**Relatório semanal**: a app diz-te como comeste — factos, e não conselhos",
                "Quando a balança e o registo discordam ao longo de semanas, propõe um ajuste ao ritmo",
                "A proposta só se aplica se a aceitares",
            ),
            highlightsEn = listOf(
                "**Weekly report**: the app tells you how you have been eating — facts, not advice",
                "When the scale and the log disagree over weeks, it proposes an adjustment to your rate",
                "The proposal only applies if you accept it",
            ),
        ),
    )
}
