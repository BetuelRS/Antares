# Changelog

Todas as alterações com significado para quem usa a Antares.

O formato segue o [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) e a numeração
segue o [Semantic Versioning 2.0.0](https://semver.org/lang/pt-BR/). As regras de quando sobe
cada número estão em [docs/referencia/versionamento.md](docs/referencia/versionamento.md).

Cada versão que mexeu na base de dados diz qual é a versão do esquema, porque é isso que decide
se uma atualização é indolor. Os esquemas estão em `composeApp/schemas/`.

---

## [Unreleased]

## [2.30.0] — 2026-09-06

**A corrida passa a dizer-te o quilómetro em voz alta.** Quem corre não olha para o
telemóvel — ouve —, e a app tinha o gatilho no motor de parciais desde a versão anterior e
nada que o dissesse. E ganha voltas marcadas à mão, para quem treina por séries e não tem um
quilómetro redondo onde marcar.

Esquema da base inalterado, **v41**; catálogo inalterado, **v6**. **Nenhuma coluna é nova** —
as voltas viajam no mesmo sítio dos quilómetros, e o campo que as distingue nasce com
omissão: uma cópia de segurança feita antes desta versão continua a abrir.

### Adicionado

- **Avisos por voz ao fim de cada quilómetro**: o número, o ritmo desse quilómetro e o tempo
  total. *«Quilómetro 3. Ritmo 5 e 42 por quilómetro. Tempo total 17 minutos.»* É o
  `TextToSpeech` do sistema — não sai nada do telemóvel, não custa nada e funciona sem rede.
- **A voz pede o áudio a baixar a música em vez de a cortar**, e devolve-lha ao acabar de
  falar. Uma frase de quatro segundos que cale a música e a reponha a seguir é pior do que o
  silêncio.
- **Voltas marcadas à mão**, num botão ao lado da pausa. Serve os 400 rápidos e os 200 lentos,
  onde não há quilómetro redondo onde marcar — e a volta é anunciada como o quilómetro é.
- **A tabela de parciais diz qual é qual**: «Km 2» e «Volta 2» são coisas diferentes, e
  passam a ler-se como tal em vez de serem os dois um «2».

### Corrigido

- **A notificação da corrida mostrava a distância em quilómetros a quem escolheu milhas**, e
  não seguia o idioma da app. Era o único aviso da app fora dessas duas regras — os cinco
  lembretes, o do treinador, o do jejum e o widget já as seguiam.
- **E os números dela não eram os do ecrã.** Truncava os decimais em vez de os arredondar: na
  mesma corrida e quase no mesmo instante, o ecrã dizia `0.06 km` e a notificação `0,05 km`.
  Passam os dois pelas mesmas contas.
- **A app dizia «menos de metade do que comeste hoje traz o teor de água medido» em dias em
  que não tinhas comido nada.** São duas ausências diferentes — não ter registado, e ter
  registado sem se saber — e o cartão da água dizia-as com a mesma frase, no «Hoje» e no
  diário. Passam a ser duas frases, e a de quem não registou fala do que falta registar.

## [2.29.0] — 2026-09-06

**A corrida ganha o botão que toda a gente procura.** Não havia pausa. Nem no ecrã, nem no
`RunViewModel`, nem no `RunController` — só começar, terminar e descartar. Atar o sapato
obrigava a esperar dez segundos pela pausa automática, ou a acabar a corrida.

Esquema da base inalterado, **v41**; catálogo inalterado, **v6**. **Nenhum dado é novo** — a
pausa é uma bandeira no motor, e os parciais que passam a ver-se a meio já eram calculados.

### Adicionado

- **Pausar e retomar.** Botão grande, no lugar onde estava o de terminar. Em pausa, o tempo em
  movimento e a distância ficam parados; o tempo decorrido, que é de relógio, continua.
- **O GPS continua a ler em pausa**, para o mapa continuar a mostrar onde estás — mas o
  percurso não cresce, e o caminho até ao bebedouro não entra na corrida. Ao retomar, a
  contagem parte de onde estás e não de onde pausaste.
- **Os dois últimos quilómetros à vista**, com o ritmo de cada um. É o que diz se estás a
  acelerar ou a abrandar sem esperar pelo fim.

### Alterado

- **Terminar só aparece com a corrida em pausa.** A correr, o sítio mais premido do ecrã
  deixa de ser um botão vermelho que acaba tudo. Continua a exigir toque longo: são duas
  fechaduras, e nenhuma delas se abre no bolso.
- **«Ritmo deste km» substitui o «ritmo atual».** O antigo era a velocidade do último troço
  entre âncoras: ia de 4:10 a 6:50 e voltava de poucos em poucos segundos. Um número que
  oscila assim não se lê a correr.
- **O cadeado bloqueia o ecrã todo**, incluindo o gesto de voltar do sistema — antes só
  desactivava o botão de terminar, e o recuo saía da corrida com o cadeado fechado. Abre-se
  mantendo o dedo no próprio cadeado; um toque simples abria-se dentro do bolso.

## [2.28.0] — 2026-09-06

**A rotina passa a poder subir sozinha.** Era estática: 3×8-12 hoje, 3×8-12 daqui a três
meses. A app tinha o histórico todo — o que se levantou, quantas repetições, em que dia — e
não usava nada disso para dizer o que fazer na semana seguinte.

**Esquema da base: v40 → v41**, duas colunas novas na rotina — a regra e o degrau. A
actualização é indolor: todas as rotinas que já existem nascem com a regra a «nenhuma», que é
o que era verdade antes. Catálogo inalterado, **v6**.

### Adicionado

- **Uma regra de progressão por rotina**, escolhida de três: **nenhuma**, **linear** e **dupla**.
  - **Linear**: fizeste o topo do intervalo em **todas** as séries? Sobe o peso. As repetições
    ficam onde estavam.
  - **Dupla**: sobe primeiro as repetições até ao máximo, e só depois o peso — voltando ao
    mínimo do intervalo. Uma repetição a mais do que a **pior** série, e não do que a melhor:
    o alvo é o número que todas têm de alcançar.
- **O alvo proposto aparece no editor e na sessão** — «alvo 62,5 kg» —, e é ele que entra no
  campo em vez do peso da última vez.
- **Cada linha do editor diz o que fizeste da última vez**, que é de onde a proposta saiu:
  «↑ da última vez: 3×10 a 60 kg». A seta é só para quem subiu.
- **O degrau é da tua unidade**: 2,5 kg em métrico, **5 lb** em imperial. Não são o mesmo
  número — 2,5 kg seriam 5,51 lb, um peso que não se monta com os discos que existem. Quem
  treina com halteres escreve o seu, uma vez, para a rotina toda.

### Alterado

- **A app propõe e não reescreve.** O peso que escreveste na rotina fica exactamente onde
  estava — nada é alterado no fim do treino, e por isso não há nada para desfazer.
- **Sem um peso que se tenha aguentado, não há proposta.** Se as séries da última vez foram a
  pesos diferentes — séries descendentes, ou o dia em que se baixou a meio —, a app cala-se em
  vez de inventar uma média. «Não sei» e «fica na mesma» são coisas diferentes.
- **Um treino abandonado a meio não conta como a última vez**, e o aquecimento também não.

## [2.27.0] — 2026-09-06

**A biblioteca de exercícios.** São 873, com imagens, músculos e instruções — o dobro do que a
Strong dá de graça —, e a lista tratava-os **todos por igual, todas as vezes**: por ordem
alfabética, sem saber quais são os teus. Toda a gente treina os mesmos quinze ou vinte.

**Esquema da base: v39 → v40**, uma tabela nova para os exercícios marcados. A actualização é
indolor: quem actualiza não tem favoritos nenhuns, que é o que era verdade antes de ela existir.
Catálogo inalterado, **v6**.

### Adicionado

- **Uma secção «os teus» no topo da lista**, com duas partes: os que marcaste, e **os que mais
  fazes** — contados nas últimas doze semanas, do mais feito para o menos. Contam-se **treinos e
  não séries**: cinco séries de supino num treino são uma vez que fizeste supino. Só aparece
  antes de se procurar, como os recentes do ecrã de exercício avulso.
- **Uma estrela em cada linha.** Os favoritos vivem em tabela própria e **vão na cópia de
  segurança** — na linha do catálogo ficariam de fora dela, que é a lição que os favoritos de
  alimentos já pagaram uma vez.
- **O detalhe diz o que já fizeste naquele exercício**: a melhor série, o 1RM estimado, quantas
  vezes e a última. Os quatro números já eram calculados noutros ecrãs. A melhor série é a de
  maior peso × repetições — 80 kg × 8 descreve mais trabalho do que 100 kg × 1 —, e o 1RM é o
  melhor de **todas** as séries e não o da melhor: são duas perguntas diferentes.
- **Sem nunca teres feito o exercício, o cartão não aparece.** «Nunca fizeste isto» e «fizeste e
  deu zero» são coisas diferentes, e a segunda não acontece.
- **A escolher um exercício para uma rotina, a linha diz «já nesta rotina».** Marca e **não
  esconde**: há quem repita o mesmo exercício de propósito, e esconder tirava-lhe isso sem o
  dizer.

### Alterado

- **O filtro de nível saiu, e no lugar dele entrou «só os meus».** O nível vem da base de origem
  — 523 iniciante, 293 intermédio, 57 avançado — e ninguém procura exercícios de nível
  intermédio; quem criou um à mão procura-o.
- **Apagar um exercício criado por ti passa a pedir confirmação, e diz em quantas rotinas ele
  está — e que o que já treinaste com ele fica no histórico.** Era a única coisa da app que se
  apagava sem confirmação e sem desfazer. Conta rotinas e não linhas: uma rotina que o repita
  continua a ser uma.

### Corrigido

- **Os nomes dos filtros eram cortados a meio das letras.** Três chips a dividir a largura, com
  o texto limitado a uma linha e sem reticências: lia-se **«Equipame»** já com a letra no
  tamanho normal, e **«Mús»**, **«Equi»** com a letra a 200 %. Passam a quebrar de linha, e o
  que não couber leva reticências — uma linha cortada diz que a app se avariou, reticências
  dizem que há mais palavra.

## [2.26.0] — 2026-09-05

**O resumo pós-treino.** Depois de uma hora de treino, o ecrã mostrava **três números
empilhados e uma lista** — e não comparava com nada. O número que faz voltar não é o
volume de hoje: é o de hoje ao lado do da última vez.

Esquema da base inalterado, **v39**; catálogo inalterado, **v6**. **Nenhum dado é novo** —
a duração, o volume e as séries de cada treino já eram somados noutro sítio; o que faltava
era uma consulta que dissesse qual foi o treino **anterior a este** da mesma rotina.

### Adicionado

- **A comparação com a última vez que fizeste esta rotina** — duração, volume e séries,
  cada um com o sinal à frente e um `=` quando não mudou. A diferença viaja com o treino de
  que partiu, e por isso «+436 kg» aponta para um treino que está no histórico.
- **E com a média das últimas três**, por baixo. As duas, e não uma: a última vez é a que se
  consegue ir ver, e um dia mau dela faz o de hoje parecer um salto que não houve; a média
  não se deixa enganar por um dia, e em troca não aponta para lado nenhum. **A média só
  aparece com três** — com duas, «média das últimas três» seria mentir sobre o que se somou.
- **O nome da rotina no título** — «Empurrar A». Dizia «Resumo do treino», e quem acabou de
  treinar já sabe que treinou. O esboço escreve «Empurrar A · terminado»; a 200 % de escala de
  letra o «· terminado» era o que se perdia, e o nome vale mais num ecrã a que só se chega
  tocando em «Terminar».
- **Partilhar o resumo como imagem**, com o partilhador que o progresso já usava.
- **Um treino livre diz porque é que não compara.** Não tem rotina, e por isso não tem termo
  de comparação — e a primeira vez de uma rotina diz outra coisa, porque é outra coisa.

### Alterado

- **A secção dos recordes só existe quando há um.** Dizer «sem recordes» a seguir a cada
  treino normal transformava a ausência num facto negativo repetido.
- **A contagem de séries do resumo ganha plural.** Dizia «1» ao lado de um rótulo fixo.

### Corrigido

- **«Ainda não treinaste», com os treinos da semana contados dois centímetros abaixo.** O
  cartão de destaque do treino só olha para as sessões que nasceram de uma rotina, e o cartão
  da semana conta-as todas: quem só faz treinos livres via as duas coisas ao mesmo tempo. Os
  números estavam certos — a frase é que não.
- **Sem perfil, o «Hoje» era um beco sem saída**: uma frase centrada, sem botão e sem caminho
  para o arranque. Passa a levar lá.
- **O cartão de partilha do progresso gravava a imagem a cada desenho do ecrã**, e não quando
  alguém carrega em partilhar — custo constante para uma acção rara. Agora grava a pedido, e
  os dois cartões partilháveis da app passam pelo mesmo código.
- **A 200 % de escala de letra, o título da barra do topo era cortado a meio das letras.** A
  barra tem altura fixa: um título de duas linhas não a faz crescer, corta-lhe a segunda. Passa
  a ser uma linha com reticências, em todos os ecrãs.
- **E, na sessão de treino, o «1RM est. · 114 kg» saía uma letra por linha**, na vertical.

## [2.25.0] — 2026-09-04

**As estatísticas do treino.** Duas listas sem tempo nenhum: o volume por músculo «desta
semana», sem dizer o que isso era nem deixar mudar, e os recordes sem data. O ecrã de
nutrição tem quatro períodos desde sempre; este não tinha nenhum.

Esquema da base inalterado, **v39**; catálogo inalterado, **v6**. **Nenhum dado é novo** — a
consulta que trazia o volume por músculo já trazia o peso, as repetições e os músculos de
cada série, e o que faltava era contá-las em vez de só as multiplicar.

### Adicionado

- **Seletor de período** — dia · semana · mês · ano —, o mesmo do ecrã de nutrição, e
  governa o ecrã inteiro e não um cartão só.
- **Séries por músculo por semana, com faixa de referência de 10 a 20.** É a métrica que
  responde a «estou a treinar o suficiente para crescer?», e nenhuma das outras respondia. Um
  músculo abaixo da faixa fica assinalado a âmbar — a única forma de descobrir um
  desequilíbrio é alguém o apontar. E a frase por baixo diz o que a faixa é: uma orientação da
  literatura, não um alvo calculado a partir de nada desta pessoa.
- **Treinos por semana e volume por semana, desenhados** com o mesmo gráfico do peso e das
  medidas — com eixo e datas, para uma paragem de um mês ocupar no traço o espaço que ocupou
  na vida. As semanas sem treino entram a zero, e as duas linhas só aparecem com quatro
  semanas ou mais: dois pontos não são uma tendência.
- **Os recordes dizem quando aconteceram**, e o mais recente vem assinalado. Um de 2024
  aparecia igual a um de ontem.

### Alterado

- **A barra por músculo mede séries, e já não volume.** O volume não é comparável entre
  grupos musculares — um dia de pernas tem sempre mais do que um de braços —, e como barra
  fazia a alternância do plano parecer desequilíbrio. Continua na linha, ao lado da contagem:
  é a estatística certa para o equilíbrio, e só deixou de ser ela a decidir o comprimento.
- **«Esta semana» quer dizer a mesma coisa em toda a app.** Este ecrã contava sete dias para
  trás a partir de agora; o painel de treino, o relatório do treinador e a grelha do
  progresso contam de segunda a domingo. Passam a ser a mesma semana.
- **Os nomes das sete rotinas de exemplo vêm dos recursos.** Eram literais no código e nem da
  mesma língua — seis em inglês e «Pernas» em português, e via-se assim com a app em inglês.

### Corrigido

- **«~4236 min da última vez», no cartão de destaque do treino.** A 2.24.0 diz ter corrigido
  este defeito em quatro sítios e eram cinco: este ficou, e lia-se duas linhas acima de
  «70h 36m» no histórico do mesmo treino. Passa pelo mesmo formatador que os outros quatro.
- **Um eixo de gráfico que dizia «−0,5 treinos».** A folga da escala é calculada dos dados e
  não sabe o que eles são; numa contagem, que começa em zero, punha o eixo abaixo dele. O
  gráfico do peso não muda — um peso nunca chega perto do zero.
- **Doze importações sem uso** no módulo do treino, que nem o detekt nem o lint apanham.

## [2.24.0] — 2026-09-03

**O histórico do treino.** A app grava séries, repetições, RPE, aquecimento, supersérie,
duração, rotina e data — e a linha do histórico mostrava **a data e o volume**. Dois treinos
completamente diferentes ficavam iguais: «sáb, 9 ago · 9 338 kg» e «ter, 12 ago · 9 340 kg»
podiam ser um dia de pernas e um de braços.

**Nenhum dado desta versão é novo, e o esquema não mexeu** — v39, catálogo v6. Os quatro
dados da linha já eram montados noutro sítio: o painel de treino usa-os desde a 2.20.0 para
os três últimos treinos.

### Adicionado

- **Quatro dados em cada linha do histórico** — a rotina, a data, a duração e as séries de
  trabalho —, com o volume a continuar à direita. Ele não é comparável entre grupos
  musculares, e como única métrica visível fazia a alternância do plano parecer progressão.
- **Uma 🌟 nos treinos em que houve recorde.** É o recorde **do dia em que aconteceu**, e não
  o melhor de hoje: um treino de há um ano que foi o melhor de sempre naquele exercício
  continua a ter a estrela depois de outro o bater. Não é guardada em lado nenhum — calcula-se
  das séries, que é o que faz corrigir uma série corrigir a estrela no mesmo instante.
- **O detalhe de um treino ganha cabeçalho**: a rotina no título, a data e a hora por baixo, e
  as três métricas. Dizia «Treino» e «45 min · 9 338 kg» — abrir um treino de há três meses
  não dizia qual era nem quando foi.
- **O RPE aparece nas séries do detalhe.** Era gravado desde sempre e nunca era lido em lado
  nenhum da app depois de escrito. Aparece só onde foi escrito: uma série sem RPE não ganha um
  traço nem um zero.

### Alterado

- **O histórico filtra-se por rotina, e já não por exercício.** Filtrar sessões por exercício
  devolvia os dias em que ele foi feito e mostrava a data e o volume da sessão — quem procura
  o supino quer a progressão do supino, e essa vive no detalhe do exercício, que já tem o
  gráfico. O filtro só oferece rotinas que chegaram a ser treinadas.
- **Um treino feito com uma rotina que entretanto se apagou continua a dizer o nome dela.** O
  histórico fala do passado, e chamar-lhe «treino livre» seria reescrever o que aconteceu.

### Corrigido

- **«Iniciar treino vazio» abria o treino que já estava a decorrer.** A app só permite um
  treino de cada vez, e este botão pedia um vazio e recebia o que estava a meio. A 2.20.0
  escondeu o cartão de destaque e o ▶ das rotinas por esta razão exacta, e deixou este botão
  — e o texto dela dizia «o ecrã oferece retomá-lo e mais nada», o que não era verdade.
  Desaparece enquanto houver um treino aberto, como os outros dois.
- **«Retomar o treino · 2618 min».** O botão contava minutos sem os converter, e um treino
  esquecido aberto de um dia para o outro ficava com um número que não se lê — enquanto a
  barra da sessão, no mesmo treino, dizia `43:39:17`. Passa a dizer `43h 39m`, que é o formato
  que a janela alimentar e o jejum já usam.

## [2.23.1] — 2026-09-02

**Duas decisões refeitas.** As perguntas de abertura da 2.23.0 tinham sido respondidas na
ausência do dono, e as respostas dele foram outras nas duas. Esta versão é isso: o horário
passa a avisar, e uma supersérie passa a ser uma supersérie.

Esquema da base inalterado, **v39**; catálogo inalterado, **v6**.

### Adicionado

- **O horário semanal passa a avisar**, com interruptor nas definições e hora à escolha.
  **Desligado por omissão** — uma app que começa a interromper sem ninguém lhe pedir é uma
  app que se desinstala. Avisa só nos dias com rotina marcada, só depois da hora escolhida,
  uma vez por dia, e **não avisa quem já treinou**. O nome da rotina vai no aviso: «Está
  marcado: Empurrar A» diz o que fazer; «tens treino hoje» obrigaria a abrir a app para saber.

### Alterado

- **Uma supersérie abre os dois exercícios ao mesmo tempo.** É o que ela é: alternar entre
  eles sem descanso pelo meio. Até aqui só o exercício aberto podia registar, e o ecrã trocava
  a seleção sozinho quando as séries acabavam — numa supersérie isso lutava com quem a faz,
  série sim série não.

## [2.23.0] — 2026-09-02

**O editor de rotinas.** Pôr o sexto exercício em primeiro custava cinco toques, com a lista a
saltar debaixo do dedo em cada um. Definir 4×6-8 com 180 s de descanso obrigava a abrir o
teclado quatro vezes. E «Empurrar A» e «Empurrar B», que partilham oitenta por cento dos
exercícios, faziam-se as duas de raiz.

Esquema da base inalterado, **v39**; catálogo inalterado, **v6**.

### Adicionado

- **Arrastar para reordenar.** Manter premido um exercício e levá-lo para onde ele deve estar.
  A lista segue a mão, e a ordem grava-se **uma vez**, quando o dedo levanta.
- **Duplicar uma rotina**, com os exercícios, os alvos e os grupos de supersérie. Chega com
  «(cópia)» já escrito no nome, e **não vai ocupar os dias da original no calendário** —
  duplicar é para partir de uma base, não para ficar com duas rotinas a disputar a terça-feira.
- **Mudar o nome da rotina** no menu do canto, junto do duplicar e do apagar.
- **Desfazer no mover.** Era a única acção do editor sem volta atrás, e é mais fácil de fazer
  por engano do que apagar — que a tem desde sempre.

### Alterado

- **Os alvos acertam-se com `−` e `+`**, com atalhos para os valores que se repetem: 3, 4 ou 5
  séries; 60, 90, 120 ou 180 segundos de descanso. Eram cinco campos de texto empilhados. O
  peso continua a escrever-se, porque não há passo que sirva a quem sobe de 2,5 em 2,5 e a
  quem sobe de dez em dez.
- **A supersérie passa a ser um cabeçalho do grupo**, e não um chip repetido em cada linha.
  O grupo existia, mostrava-se três vezes e não agrupava nada.
- **O nome da rotina deixa de se gravar a cada tecla.** Eram vinte escritas na base num nome
  de vinte letras, e era o único campo da app sem espera.

### Corrigido

- **O peso alvo da rotina passa a servir para alguma coisa.** Preenchia-se, convertia-se,
  mostrava-se — e a sessão ignorava-o: um exercício novo com peso alvo definido abria o campo
  vazio, apesar de a rotina saber o número. Agora entra quando não há histórico de onde tirar.
- **O máximo de repetições nunca fica abaixo do mínimo.** «8-6» não é um intervalo, e ninguém
  reparava nele até a sessão o mostrar ao contrário.

## [2.22.0] — 2026-09-02

**Os exercícios de peso do corpo.** Cento e onze exercícios do catálogo da app — flexões,
dominadas, fundos, prancha, agachamento livre — **não se conseguiam registar de todo**: a
série exige um peso maior do que zero, e uma flexão não tem peso para escrever. Quem
escrevesse `1` para contornar o botão cinzento ficava com um «recorde» de 1,4 kg naquele
exercício e com o mapa de volume envenenado.

**Esquema da base: v38 → v39.** Nenhuma série já gravada muda — a coluna nova nasce vazia, e
vazia quer dizer o que era verdade antes: nada daquela carga veio do corpo. Catálogo
inalterado, **v6**.

### Adicionado

- **Uma flexão regista-se.** Nos exercícios marcados como sendo de peso do corpo, o campo do
  peso passa a ser a **carga extra** — o que está no cinto, ou nada — e o teu peso entra por
  baixo. A série conta para o volume, para o 1RM e para os recordes como qualquer outra.
- **A conta fica à vista**, como no resto da app: *«O teu peso: 78 kg»*, e com carga extra,
  *«88 kg no total»*. Nada aparece sem se ver de onde veio.
- **Podes dizer quanto do teu peso conta em cada exercício.** Uma flexão levanta menos do que
  uma dominada, e o número é teu: começa nos 100 % e mudas onde quiseres. **A app não propõe
  uma percentagem** — os «cerca de 65 %» que se leem por aí são uma aproximação, e este não é
  um número que ela possa medir por ti.
- **Carga extra para quem usa cinto**, somada por cima do corpo.

### Alterado

- **A linha do histórico diz «peso do corpo»** quando a carga veio de lá. Sem isso, uma flexão
  aparecia como uma série de 78 kg e ninguém sabia que os 78 eram a própria pessoa.
- **O interruptor de aquecimento não aparece nestes exercícios**: não há campo de peso para
  aquecer com menos, e era mais uma coisa no ecrã mais apertado da app.

### Corrigido

- **Sem peso registado, a app diz que falta** em vez de assumir um. Um peso de recurso seria
  um número que nunca escreveste a entrar no teu volume e nos teus recordes.

## [2.21.0] — 2026-09-02

**A sessão de treino.** É o ecrã mais usado da área de treino e o mais crítico — usa-se com
uma mão, suado, entre séries —, e era o que pedia mais toques por série. Ganha relógio,
calculadora de discos, fluxo de teclado e notas.

**Esquema da base: v37 → v38**, uma tabela nova para as notas. Catálogo inalterado, **v6**.
A actualização é indolor: um treino sem notas e um treino com zero notas são a mesma coisa.

### Adicionado

- **O relógio do treino na barra do topo**, ao segundo. A duração só se sabia no resumo, no
  fim. Conta a partir do instante gravado no treino, e não de quando o ecrã abriu: quem sai
  da app para atender uma chamada volta e encontra o tempo certo.
- **O título passa a ser o nome da rotina** — «Empurrar A» em vez de «Treino». Quem está a
  meio de um treino já sabe que está num treino.
- **A calculadora de discos**, por baixo do campo do peso: *«20 + 1,25 por lado · barra de
  20 kg»*. Em libras são os discos de libras — 45, 35, 25, 10, 5 e 2,5, com a barra de 45 —,
  e não os métricos convertidos: um disco de 45 lb não é um disco de 20 kg, e ninguém tem um
  disco de «44,1». Quando o peso não se consegue montar, **diz quanto falta** em vez de
  arredondar em silêncio.
- **O 1RM estimado à vista**, ao lado do alvo do exercício, e já com as séries de hoje
  dentro. É o número com que se decide o peso da série seguinte, e essa decisão toma-se aqui.
- **O recorde é dito no momento em que acontece**, e não só no resumo do fim.
- **Notas por exercício**, escritas no treino a que pertencem: «ombro direito sensível»,
  «máquina 2 ocupada». São do dia e não da rotina — a instrução permanente de um exercício é
  outra coisa, e um ombro de terça-feira não muda o plano de todas as semanas seguintes.

### Alterado

- **O teclado salta sozinho de peso para repetições e grava.** Eram dois toques por série só
  para mudar de campo; numa sessão de vinte séries, quarenta toques.
- **O botão de gravar passa a ter o nome da acção** — «Gravar série» —, em vez de um ✓ de
  40 dp. Era o alvo mais tocado do ecrã e o mais pequeno.
- **O RPE sai da linha de registo** e passa para o menu «⋯» de cada série. Era um terceiro
  campo permanente para um número que a maioria não escreve, e roubava largura ao peso e às
  repetições. Continua a gravar-se, e as séries que já o têm mostram-no.
- **Os campos do peso e das repetições deixam de ter largura fixa.** Eram 96, 80 e 72 dp lado
  a lado, e com a letra a 200 % os rótulos não cabiam. Agora dividem a linha e quebram-na
  quando é preciso.

### Corrigido

- **O chip de supersérie deixa de fingir que é tocável.** Tinha uma acção vazia: parecia um
  botão, respondia ao toque, e não fazia nada. Passa a ser o que sempre foi — uma etiqueta.

## [2.20.1] — 2026-09-02

**A barra de baixo.** Cinco separadores permanentes, e um deles era a corrida — uma coisa que
se faz umas vezes por mês a ocupar um quinto da única barra que está sempre no ecrã. Passa a
ser `Hoje · Diário · Treino · Progresso · Mais`, e a corrida vai viver dentro do treino, que
é onde a atividade já morava.

**Nada desapareceu: mudou de sítio.** Todos os ecrãs continuam alcançáveis, e os testes-guarda
que provam isso foram reescritos para o caminho novo em vez de apagados. Esquema da base
inalterado, **v37**; catálogo inalterado, **v6**.

### Adicionado

- **O progresso ganha separador próprio.** O peso, as fotografias e os gráficos eram a
  primeira coisa dentro do «Perfil», atrás de um ícone de pessoa. É o ecrã que responde a
  *está a resultar?*, e estava guardado no único separador cujo nome não dizia nada sobre o
  que lá havia dentro.
- **A corrida no painel de treino**, com a distância da semana e a última corrida — nome, data
  e distância. Sem corridas esta semana diz que não há; sem nenhuma corrida alguma vez, convida
  a começar. É uma porta, e não um relatório vazio.

### Alterado

- **O «Perfil» passa a «Mais»**, e junta num sítio só o que estava em dois: os atalhos do corpo
  — perfil e metas, refeições guardadas, estatísticas, ricos em, treinador — e o menu da app,
  que vivia atrás de uma engrenagem no canto superior do «Perfil». Um menu de definições
  escondido dentro de um separador chamado «Perfil» é o sítio onde ninguém procura a cópia de
  segurança.
- **A corrida sai da barra de baixo.** Continua inteira — começar, o mapa, o histórico, os
  detalhes de cada corrida. O que muda é o caminho: agora é o separador do treino.

### Corrigido

- **A corrida ganha uma seta para voltar.** Enquanto foi separador, a barra de baixo estava
  sempre no ecrã e era por ali que se saía. Empurrada a partir do treino, ficava sem saída
  visível nenhuma — só o gesto do sistema, que não se vê. **Foi o defeito que a versão
  criou**, e ficou um teste-guarda que o apanha em qualquer ecrã, não só neste.
- **Os períodos do progresso cortavam-se.** Os quatro botões dividiam a largura em quatro
  partes iguais numa linha só, e «3 meses» lia-se **«3»** num telemóvel estreito — um período
  que não existe. Passam a quebrar de linha, como já faziam nas estatísticas da nutrição.
  Nenhum teste vê largura de texto.

## [2.20.0] — 2026-09-02

**O centro de treino.** O separador do treino era o único dos cinco que não mostrava um
número: cinco botões cinzentos iguais e uma lista de nomes. Passa a responder à pergunta que
se faz ao abri-lo — *treino o quê hoje, e como é que começo?*

**Nenhum dado desta versão é novo.** Todos já eram calculados noutro sítio da app e nunca
chegavam aqui. Esquema da base inalterado, **v37**; catálogo inalterado, **v6**.

### Adicionado

- **O treino de hoje, em destaque, com um botão que o começa.** Vem do plano da semana, que
  já existia e que nenhum ecrã de treino lia — o cartão do «Hoje» sabia dizer o nome da
  rotina do dia e este não. Começar a rotina de hoje custava três toques e um percorrer até
  ao fundo do editor.
- **Sem plano para hoje, o cartão mostra a última rotina treinada**, com a data. Sem plano e
  **sem histórico** — que é o estado de quem acabou de instalar a app — leva a marcar a
  semana, em vez de escolher uma rotina por conta própria: a app semeia sete, e nenhuma
  delas é mais de quem a usa do que as outras.
- **A semana em sete pontos**, de segunda a domingo, com o volume e as séries. É o mesmo
  desenho que o relatório do treinador já usava, e agora é o mesmo componente.
- **Cada rotina diz quantos exercícios tem e quando foi feita pela última vez**, e ganha um
  ▶ que a começa. Tocar no nome continua a abrir o editor: são duas acções, e por isso dois
  alvos.
- **Os últimos treinos, com quatro dados** — rotina, data, duração e séries. A linha do
  histórico tinha data e volume, e dois treinos completamente diferentes ficavam iguais.

### Alterado

- **A biblioteca, o histórico, as estatísticas e o plano da semana passam para o menu do
  canto.** Eram quatro botões de largura toda, visitam-se uma vez por mês, e ocupavam metade
  do ecrã.
- **Com um treino a decorrer, o ecrã oferece retomá-lo e mais nada** — e diz há quanto tempo
  ele dura: «Retomar o treino · 12 min». Os botões de começar desaparecem enquanto ele durar:
  quem os tocasse ia parar ao treino aberto e não à rotina que escolheu, porque a app só
  permite um treino de cada vez — o botão dizia o contrário do que fazia.
- **Criar uma rotina passa a ser um «＋» ao lado de «As minhas rotinas»**, e não um botão no
  fim da lista, que com sete rotinas ficava fora do ecrã. O treino vazio fica sozinho no fim,
  que é onde a acção mais rara pertence — era a terceira coisa do ecrã.

### Corrigido

- **Os sete quadrados da semana eram invisíveis.** No relatório do treinador também: eram
  pintados com uma cor que, desde a paleta da 2.18.2, é a mesma do cartão onde vivem. Uma
  semana sem dias marcados ficava a ser sete letras sozinhas. Nenhum teste vê cor.

## [2.19.0] — 2026-08-30

**O exercício avulso.** O ecrã de registar uma atividade era correcto e mal servido pelos seus
próprios controlos: a duração só andava de cinco em cinco — uma aula de cinquenta minutos custava
dez toques, e um treino de vinte e dois não se registava de todo —, quem faz padel três vezes por
semana escrevia «padel» três vezes por semana, e um registo errado só se corrigia apagando e
refazendo a procura toda.

Esquema da base: **v37**. Catálogo inalterado, **v6**.

### Adicionado

- **A duração escreve-se.** Um campo de texto no lugar do contador, com o `−`/`+` de cinco a
  ficar como acessório e quatro atalhos — 15 · 30 · 45 · 60. É o mesmo controlo no ecrã de
  registar e no de corrigir, porque dois controlos de duração com regras diferentes é a forma de
  eles divergirem.
- **As atividades recentes à cabeça da lista**, sem repetições e da mais recente para a mais
  antiga, enquanto a caixa de procura está vazia e não há categoria escolhida.
- **Tocar num exercício do diário abre-o para corrigir**, como já acontecia com a comida. Só os
  escritos à mão: uma linha de treino ou de corrida é o reflexo de uma sessão que tem dono, e uma
  da Health Connect traz calorias medidas por um relógio.
- **Um exercício passa a guardar a hora a que começou**, e a linha do diário mostra-a. As quatro
  origens já a sabiam e deitavam-na fora: a corrida tem o instante do arranque, o treino tem o
  início da sessão, a Health Connect tem o da sessão importada, e o registo à mão tem o relógio.
  Num dia que não é hoje o registo nasce sem hora — que é o que ele honestamente é.

### Corrigido

- **As duas descrições de acessibilidade que estavam escritas no código** — `"-5"` e `"+5"` —
  passam a recursos, e passam a dizer de quê: «Menos 5 minutos». Eram as duas únicas de toda a
  app, e agora há um teste-guarda que as impede de voltar: o TalkBack anunciava «menos cinco»
  em português e em inglês, sem dizer de quê.
- **Os controlos da duração deixam de ter largura fixa**, e cabem com a letra no tamanho
  máximo do sistema. A 200 % o botão de somar minutos saía do ecrã, o atalho «60» partia-se em
  dois algarismos e o botão de pôr a hora lia-se na vertical, uma letra por linha.
- **O mesmo no diálogo de corrigir uma refeição**, que tinha a mesma forma e, em inglês, os
  mesmos dois rótulos. A hora passa a ter linha própria e os botões vão por baixo.

### Nota

- **A intensidade não entrou, e não é esquecimento:** a tabela de METs já a traz, como linha
  própria. A corrida tem cinco ritmos e a bicicleta cinco velocidades. Um cursor de intensidade
  ao lado seria uma segunda maneira de dizer a mesma coisa, e sem árbitro entre as duas.

## [2.18.2] — 2026-08-30

**A releitura com o estudo.** As treze versões que já saíram tinham sido construídas a partir
das linhas-resumo do plano, sem um único documento do estudo aberto. Foram abertos todos os
que a rota manda, e o que se segue é o que divergiu.

*Em três passagens: a leitura de cada documento contra o código, os esboços abertos ao lado do
emulador ecrã a ecrã, e — a terceira — os dezassete esboços comparados um a um com o que a app
faz hoje, incluindo os das áreas que ainda não têm versão.*

Esquema da base: **v36**, catálogo **v6**.

### Alterado

- **O microfone, a câmara e o leitor de códigos passam a viver dentro do campo de procura**, e
  o campo diz o que faz: «Procurar ou descrever». Desaparecem os dois botões «Descrever» e
  «Foto» que estavam por baixo dele — com o emoji dentro da etiqueta, que um leitor de ecrã lê
  como palavra e não como ícone, e a duplicar a câmara que a barra de registo rápido já tinha.
- **Os botões dizem onde é que o registo vai cair.** «Registar no almoço» em vez de «Adicionar
  ao diário», e «Registar 2 doses no jantar» numa receita que se divide em doses. Quem chega à
  revisão da AI por um atalho escolheu a refeição dois ecrãs atrás, e a app nunca mais lha
  confirmava.
- **O cartão da meta do dia, o resumo do diário e as calorias de um alimento ou de uma receita
  ganham a forma de destaque dos esboços** — um véu da cor da app a esvair-se para o fundo do
  cartão, com o contorno da mesma cor. É a única superfície com gradiente, e é de propósito:
  um destaque que aparece três vezes no mesmo ecrã deixa de destacar.
- **A app passa a ter a cor que sempre disse ter.** Os cartões eram cinzento-lavanda porque o
  tema nomeava nove cores e deixava as outras trinta ao sistema — e as que ficavam por dizer
  são as que mais se vêem. Passam a ser o quase-preto azulado da paleta, o texto passa a
  branco quente, o contorno deixa de ser cinzento neutro, e cada cor de destaque ganha o seu
  próprio fundo. Por trás de tudo entra o ar dos esboços: dois brilhos muito fracos, um
  quente e um âmbar, sobre o preto mais fundo.
- **O selo que diz que um alimento foi medido em Portugal deixa de ser vermelho.** Nesta app o
  vermelho quer dizer «isto faz alguma coisa», e um selo de origem não é uma acção.
- **Os ±10 voltam à revisão da AI**, ao lado do campo de gramas. A versão que tornou o campo
  escrivível tirou-os, e eles são o que serve o ajuste de uma mão — que é como aquela folha se
  usa. O campo continua a ser o que resolve ir de 30 g para 180 g.
- **A colher de sopa passa a aparecer só nos líquidos.** Antes escondia-se onde o alimento já
  tinha uma porção nomeada, e por isso um frango cru — que não tem nenhuma — continuava a
  oferecer uma colher de sopa de frango cru.
- **Os separadores da pesquisa passam a Tudo · Favoritos · Meus**, como o esboço 03 os
  desenha. Os favoritos ganham separador próprio; as refeições guardadas deixam de ter um e
  passam a ser a **primeira secção do «Tudo»** — a app abre no que se come, sem um toque pelo
  meio.
- **A linha de um alimento mostra a porção habitual** — «300 g habituais» — a partir de três
  registos. É o número com que esta pessoa regista este alimento, e ganha à porção da tabela.
- **O cabeçalho diz onde é que o registo vai cair** — «Almoço · hoje» em vez de «Adicionar
  alimento».
- **Criar um alimento sai do botão flutuante** e passa a linha no fim dos resultados, já com
  o nome escrito: «Não encontraste? Criar «queijo»». O botão mais visível do ecrã estava a
  oferecer a acção mais rara.

- **As refeições guardadas passam a ser uma lista só**, ordenada por nome, com a origem
  escrita na linha — «4 itens · 640 kcal · guardada do diário», ou «7 ingredientes · 512
  kcal/dose · 4 doses». Eram duas secções com dois títulos, e duas secções obrigam a saber
  como a refeição foi montada antes de a procurar, que não é a pergunta de quem a quer
  registar.
- **O multiplicador de uma refeição guardada passa a chips** — ×0,5 ×1 ×1,5 ×2. Era um campo
  de texto, e o campo trazia atrás um filtro de algarismos, um tecto de caracteres e três
  estados que não queriam dizer nada: vazio, zero e negativo.
- **As refeições guardadas aparecem sempre**, e não só a quem chegou à pesquisa por uma
  refeição do dia. Deixam também de aparecer em dois sítios ao mesmo tempo — havia cinco
  numa secção e todas no separador, e era o separador a única forma de ver a sexta.
- **O botão de criar diz o que vai criar**, e deixa de haver dois botões de criar no mesmo
  ecrã.
- **A basal calculada sem massa gorda passa a declarar a margem dela.** A app dizia «cerca de
  1797 kcal, com 62 para cada lado» a quem tinha medido a cintura, e mostrava um número nu a
  quem nunca mediu a massa gorda — que é a maioria, e é o caminho com mais margem. A fórmula
  usada aí erra cerca de 10 % do basal.
- **A percentagem de massa gorda medida à fita passa a ser corrigida do desvio por sexo.**
  A fórmula das circunferências dá pouco aos homens e demais às mulheres, sempre na mesma
  direção. **Isto muda números que já estavam no telemóvel**: sobe 2,6 pontos nos homens,
  desce 2,3 nas mulheres, e o metabolismo basal acompanha.
- **O interruptor das metas adaptativas diz o que se usa quando está desligado** — uma regra
  fixa que ignora a adaptação do corpo e sobrestima a perda ao fim de alguns meses.
- **O ritmo semanal no ecrã do progresso passa a usar a janela escolhida nas definições.** O
  perfil de saúde já a usava e este não, e o mesmo número saía diferente nos dois sítios.
- **O ecrã da cópia de segurança diz o que a cópia não leva** — a fotografia de um prato, que
  dura sessenta dias no telemóvel e depois se apaga. A decisão era defensável e não estava
  escrita em lado nenhum.
- **A colher de sopa deixa de aparecer em alimentos que já têm porções nomeadas.** É uma
  medida de volume aplicada a tudo: faz sentido no azeite e não no bife.

### Adicionado

- **A ficha de um alimento diz de quanto é o «cerca de» dos números dele** — «130 kcal / 100 g
  ± 10 %» — e explica de onde vem a margem: uma tabela nacional publica a média de umas quantas
  amostras, um rótulo tem tolerâncias legais, e uma estimativa é um palpite. A app já levava
  essa margem até ao total do dia; faltava dizê-la onde ela nasce, que é onde se escolhe o
  alimento.
- **Cada nutriente diz de onde veio, quando não veio de onde veio o alimento.** Um alimento
  medido em Portugal pode levar o iodo da tabela francesa e metade das vitaminas da americana
  — é o que a junção das três fontes faz —, e a app dizia uma origem só, a de quem lhe deu o
  nome e as calorias. Agora a linha do iodo diz «Este valor vem da Tabela da CIQUAL», e as que
  não dizem nada são as que vieram de onde o alimento veio. São 671 alimentos com pelo menos
  um nutriente de outra fonte.
- **O diário reparte a margem do dia pelas origens**, da que mais traz para a que menos traz.
  «±150 kcal» não sugere gesto nenhum; saber que 120 dessas vêm dos 400 kcal que a AI adivinhou
  sugere um — pesar aquele prato. Aparece nos mesmos dois casos em que a margem já aparecia, e
  não todos os dias.
- **«As minhas refeições» é um ecrã**, alcançável do «Eu» e do diário, com um «+» para montar
  uma nova e uma caixa para procurar entre elas.

- **Mudar o nome de uma refeição guardada, e tirar-lhe um item**, com desfazer. Até aqui só
  se podia apagar, e um nome mal escolhido no dia em que se guardou durava para sempre.
- **Editar uma receita a partir do ecrã dela**, onde se vê o que se vai mudar.
- **O aviso «este valor é estimado» passa a levar a corrigir o alimento.** Dizia que o número
  era estimado e deixava a pessoa com ele na mão.
- **Aviso de peso final impossível em receitas sem família de confeção**, que são a maioria:
  1 200 g de ingredientes aceitavam 50 g de peso final em silêncio, e os valores por 100 g
  saíam vinte e quatro vezes errados.

### Corrigido

- **Fechar a folha da AI deixa de apagar o que se escreveu.** Cancelar guardava e fechar
  apagava: um arrastão para baixo por engano custava a frase toda.
- **A razão de o botão de confirmar da AI estar cinzento aparece ao lado dele**, e não no
  topo do ecrã com a lista pelo meio.
- **O estado vazio da pesquisa deixa de dizer «escreve pelo menos 2 letras»** a quem ainda
  não registou nada. Descrevia o campo, e não o que faltava na lista.
- **Uma receita sem nome deixa de ser uma linha em branco.** Ela nasce no instante em que se
  abre a folha de ingredientes ou a de passos, e quem recua sem escrever nada deixava-a na
  lista sem nada escrito — e apagar uma receita passa por conseguir abri-la.
- **A colher de sopa também desaparece nos alimentos que trazem a porção da própria fonte**,
  e não só nos que têm porções nomeadas à parte.
- **Tirar um item de uma refeição guardada passa a ter volta atrás visível.** O desfazer da
  app é um aviso ao fundo do ecrã, e a folha da refeição é uma janela por cima dele: o aviso
  não chegava a aparecer. Passa a ser uma linha dentro da folha, sem os quatro segundos de
  corrida.
- **Uma frase em inglês mostrava a barra invertida no ecrã.**

---

## [2.18.1] — 2026-08-28

**A preparação, escrita e por ordem.** A segunda metade da 2.18.0, e o terceiro número é o
que a regra manda: partir uma versão a meio não desloca as cem referências cruzadas do plano
— ver [versionamento.md](docs/referencia/versionamento.md). Esquema da base: **v35**.

### Adicionado

- **Passos de preparação nas receitas.** Escrever, corrigir, apagar com desfazer, e subir ou
  descer um de cada vez. A ordem é o dado — «leva ao lume» antes de «tempera» é outra
  receita —, e é por isso que os passos têm posição própria, ao contrário dos ingredientes,
  onde a ordem não significa nada.
- **Os passos aparecem onde se cozinha**, no ecrã da receita, por baixo dos números. Quem
  abre esse ecrã vem registar o que comeu; quem vem cozinhar rola até ao fim, e é uma vez só.
- Os passos entram na exportação e na cópia de segurança, como tudo o resto que se escreve.

**O que os passos não fazem:** não entram em conta nenhuma. Uma receita continua a ser a
soma dos ingredientes, e um passo é texto que quem cozinhou escreveu para si próprio — é a
única coisa numa receita que a app não sabe conferir, e é de propósito.

---

## [2.18.0] — 2026-08-28

**As refeições guardadas deixam de se aplicar às cegas.** Uma refeição guardada é uma cópia
congelada de um dia que pode ter meses; o que lá está deixou de ser óbvio muito antes de
alguém lhe voltar a tocar. Esquema da base: **v34**.

*Esta versão **não teve APK próprio**: foi partida a meio (regra B2) e as duas metades saíram
no mesmo lançamento, o da [2.18.1](#2181--2026-08-28). Não há etiqueta `v2.18.0`, e o
changelog dentro da app apresenta as duas como uma entrada só. Fica em secção própria porque é
onde o trabalho dela está descrito, e escrito aqui porque um número com data e sem artefacto é
exactamente o género de coisa que este ficheiro existe para não deixar acontecer em silêncio.*

### Adicionado

- **Ver antes de aplicar.** Um toque na linha abre a lista do que lá está, com as gramas e as
  calorias de cada item. Antes, o mesmo toque escrevia sete registos no diário e fechava o
  ecrã — e saber o que tinha entrado obrigava a ir ao diário ver.
- **Multiplicador.** Meia refeição, ou duas. As gramas e os macros escalam; os
  micronutrientes ficam por 100 g, que é como a base os guarda em todo o lado.
- **Desfazer ao aplicar.** Aplicar ao dia errado escrevia sete linhas que depois se apagavam
  uma a uma, à procura de quais tinham acabado de entrar no meio das que já lá estavam.
- **Atalho no diário:** «aplicar refeição guardada» no menu de cada refeição, ao lado do
  «guardar» que já lá estava. É a olhar para o dia que nasce a vontade de repetir.
- **Aviso quando o peso final não bate com os ingredientes.** A 2.8.0 previa o peso e só o
  dizia com o campo vazio; escrever 2000 g em 400 g de ingredientes não dizia nada, e todos
  os valores por 100 g saíam cinco vezes errados em silêncio. O limiar não é uma percentagem
  escolhida à mão: a app compara com o intervalo que **os próprios métodos de confeção**
  publicados dão — medido, dentro da mesma família o rendimento muda até 0,43 só por se
  escolher outro método.

### Alterado

- **A linha de uma refeição guardada diz quantos itens e quantas calorias.** Dizia o nome e a
  refeição do dia — «Almoço» —, que é a coisa menos útil que se pode dizer sobre uma lista
  chamada «Almoço de segunda».
- **Acrescentar ingredientes deixa de sair do ecrã da receita.** Era um ecrã de pesquisa
  inteiro por ingrediente, com ida e volta; passa a ser uma folha que **não se fecha ao
  escolher** — quem faz uma receita acrescenta ingredientes em série.

---

## [2.17.0] — 2026-08-28

**A revisão da AI deixa de ser só «ver e apagar».** O que o modelo devolve passa a ser
corrigível: escreve-se as gramas, troca-se um item por um alimento do catálogo, acrescenta-se
o que ele não viu. Esquema da base: **v34**.

### Alterado

- **O microfone deixa de ir para a pesquisa e passa a ir para a AI.** O que ele pedia já era
  «diz o que comeste» — uma frase de refeição, com quantidades e mais do que um alimento — e
  entregava-a a uma pesquisa de catálogo, onde «dois ovos e uma torrada» não encontra nada.
  O ditado abre a folha da AI **escrito e não analisado**: lê-se antes de gastar uma
  utilização numa frase que o telemóvel ouviu mal.
- **As gramas de cada item escrevem-se.** Havia `−10` e `+10`, e ir de 30 g para 180 g eram
  quinze toques — que ninguém dá, e o número errado ficava.

### Adicionado

- **Trocar um item por um alimento do catálogo.** O modelo acerta no nome e erra no alimento
  com frequência; agora escolhe-se o certo. É mais do que um nome corrigido: **o registo passa
  a ligar ao catálogo**, e até aqui tudo o que a AI gravava era um retrato solto — sem
  micronutrientes medidos, fora dos «mais registados», sem porção habitual.
- **Acrescentar um alimento em falta.** O modelo omite com a mesma facilidade com que erra: o
  arroz tapado pela carne na fotografia não aparecia em lista nenhuma, e a única saída era
  desistir da revisão.
- **Guardar como refeição, no fim da revisão.** Guarda **os itens que se acabou de rever**, e
  não a refeição do dia — o método que já existia lia o diário inteiro, e quem tivesse
  registado o pão às oito ficava com ele dentro de um modelo chamado «Almoço».
- **A fotografia do prato fica no registo**, e aparece no diário ao lado da linha. Vive **dois
  meses** e é apagada sozinha; não entra na cópia de segurança. Três refeições fotografadas por
  dia dariam perto de mil imagens por ano nas cinco cópias que rodam em Documentos, e a foto é
  uma ajuda a rever — o registo são os números, e esses ficam para sempre.

### Corrigido

- **O aviso legal aparecia nas quatro fases da folha**, incluindo por cima de um campo vazio e
  de uma mensagem de erro. Um aviso de que os números são estimados, onde não há números
  nenhuns, deixa de se ler onde é preciso. Agora está só na revisão, em frente à lista.

---

## [2.16.0] — 2026-08-27

**Abrir no que se come.** A pesquisa deixa de pedir uma escolha entre seis antes de escrever a
primeira letra. Esquema da base: **v33**.

### Alterado

- **Seis separadores passam a três** — Procurar, Meus, Refeições. Nada desapareceu: os
  recentes e os favoritos passam a estar dentro do Procurar, por baixo do que registas mais,
  e as receitas e os modelos partilham o Refeições. Quatro dos seis respondiam à mesma
  pergunta, cada um no seu canto.
- **A linha de um alimento diz a porção**, quando ele tem uma: «uma fatia 30 g» a seguir às
  kcal. Vale para um em cada quatro alimentos do catálogo, e nos outros a linha fica como
  estava.
- **Criar um alimento leva o nome que escreveste.** Quem procurou «pão da avó» e não encontrou
  já escreveu o nome uma vez.

### Corrigido

- **Os atalhos do que já comeste deixam de desaparecer** no instante em que a pesquisa
  responde — que era exactamente quando passavam a poder ser comparados com o resto.

### Adicionado

- **Os produtos de embalagem passam a mostrar a fotografia** na lista de resultados de fora.
  A Open Food Facts publica-a, e agora aparece — o ecrã «O que sai daqui» diz que isso é mais
  um pedido a eles, um por cada fotografia que vês.

---

## [2.8.0] — 2026-08-25

**O que faltava do bloco D.** Seis promessas do plano que a 2.7.0 deixou por cumprir, e que
não se viam porque nenhuma delas rebenta nada — dão números errados em silêncio. Esquema da
base: **v32**, catálogo **v5**.

### Corrigido

- **Uma receita cozinhada deixa de ganhar vitaminas ao lume.** A conta dividia tudo pelo peso
  final, e a água que evapora concentrava também a vitamina C — uma sopa de 500 g de
  espinafres reduzida a 400 g saía com 35 mg por 100 g quando os espinafres crus tinham 28.
  Cozer destrói um quarto dela: são 26,25. Agora escolhe-se como se cozinhou o prato e cada
  ingrediente perde o que a tabela da família dele diz, porque cozer espinafres e cozer arroz
  no mesmo tacho não destrói a mesma coisa.
- **Um mililitro deixa de contar como uma grama.** 200 ml de azeite pesam 184 g e a app
  contava-lhes 200 — 9 % de gordura a mais em cada colher. 696 líquidos passam a ter a
  densidade medida ao lado; os outros ficam a 1,00, que é o que a app já assumia.
- **74 sólidos deixam de ser medidos em mililitros.** O «Lavagante, cozido em água» e o
  «Leite em pó» estavam marcados como líquidos numa lista herdada de uma versão antiga.

### Adicionado

- **A pesquisa junta o mesmo alimento nos seus estados.** Procurar «frango» dava sete linhas
  quase iguais — cru, assado, com pele, sem pele. Agora é uma linha por alimento, com «+ 2
  estados» a abrir as outras. São 360 alimentos em 175 grupos, e o que **não** se junta é
  tão importante: «carne» e «carne e pele» têm gorduras diferentes, e duas fontes com o mesmo
  nome são duas medições.
- **O abacaxi encontra o ananás, e o cimbalino encontra o café.** 47 grupos de palavras que
  se procuram umas às outras, incluindo o inglês das tabelas — quem escreve «frango» encontra
  «Chicken, breast, raw». O catálogo continua com um nome por alimento: isto vive no índice
  de pesquisa e em mais lado nenhum.
- **A app aprende quanto pesa uma fatia tua.** A tabela diz 30 g e a tua faca corta 45 — a da
  tabela é uma mediana medida noutro sítio, e a tua é uma medição do que tu comes. Só aparece
  quando há mesmo um hábito, e só quando difere da tabela em mais de 10 %.
- **A receita sugere o peso final que as tabelas prevêem**, com um botão que o escreve. Nunca
  se grava sozinho, e não aparece se menos de 60 % do peso tiver rendimento publicado.

---

## [2.7.0] — 2026-08-24

**O bloco do catálogo inteiro numa versão.** O catálogo deixa de precisar da loja, aprende a
dizer o que não sabe, sabe o que acontece à comida quando se cozinha, e passa a dar porções em
vez de gramas. Esquema da base: **v30**.

*O conteúdo que o plano tinha em 2.8.0 a 2.15.0 sai aqui, ou não se lança de todo: o motor de
qualidade e a oficina de curadoria não mudam nada na app. Os **números** ficam livres, e a
2.8.0 é usada a seguir para o que faltou desta.*

### Adicionado

- **A comida crua e a mesma comida cozinhada deixam de ser dois alimentos.** Num alimento que
  se cozinhe, uma pergunta nova — «e se for cozinhado?» — dá os números de grelhado, cozido,
  assado, frito, estufado ou salteado, e é isso que fica registado no diário. As contas saem
  de duas tabelas do departamento de agricultura americano: quanto peso se perde e **quanto de
  cada vitamina sobrevive**. Cozer espinafres perde vitamina C para a água *e* perde água, e
  contar só a primeira coisa dá um número mais errado do que não fazer conta nenhuma — que é
  o que toda a concorrência faz. Onde ninguém mediu o peso perdido, a app pergunta-o: se
  pesaste depois de cozinhar, esse peso vale mais do que qualquer tabela.
- **Sete vezes mais alimentos com porção**, de 297 para 2 090 — «uma fatia», «uma chávena»,
  «uma unidade média» — e 543 deles com mais do que uma maneira de medir. Para o resto do
  catálogo, registar continua a ser escrever gramas.
- **Dois mil cento e trinta e um alimentos deixam de ter nome de laboratório em inglês.** Numa
  app portuguesa eram 4 958, e traduz-se o vocabulário e não os nomes: «raw» traduz-se uma vez,
  e não seiscentas e vinte e nove. O que ainda não tem nome inteiro em português fica em
  inglês, em vez de aparecer meio traduzido — que era o que estava a acontecer a 5 765 deles,
  com nomes como «Wild arroz, cozinhado» e «Pie, Dutch Maçã, Comercial».
- **Noventa e sete alimentos repetidos deixam de estar no catálogo duas vezes.** O agrião, o
  espadarte, o alecrim e o tomilho estavam lá em francês e em português, e a app dava duas
  respostas à mesma pergunta conforme a linha que se tocasse. Quem os tiver nos favoritos ou
  numa receita não perde nada: cada um deixa uma marca a apontar para o que fica.
- **Vinte e três mil valores de nutrientes novos**, em 2 954 alimentos americanos: a água, o
  fósforo, o colesterol, as gorduras mono e poli-insaturadas, o álcool. Estavam na fonte e a
  leitura antiga não os tinha trazido.
- **O dia passa a dizer de quanto é o «cerca de» das suas calorias**, quando isso muda a
  leitura dele: quando o que falta é menor do que a própria incerteza da contagem — e aí um dia
  não chega para concluir nada, mas a tendência da semana chega — e quando mais de um terço das
  calorias veio de estimativas em vez de pesagens.

- **O catálogo de alimentos passa a poder descarregar-se**, nas Definições. Até aqui, corrigir
  as kcal de um alimento custava uma versão publicada — compilar, assinar, esperar pela
  revisão —, e era por isso que as correções não se faziam. Só desce quando carregas no botão:
  a app nunca vai à rede por causa do catálogo sem alguém lhe pedir.
- **O ficheiro é verificado antes de substituir o que já lá está.** O resumo tem de bater com
  o que a origem declara, o ficheiro tem de abrir, e a versão tem de ser mais recente do que a
  instalada. Uma actualização que corra mal deixa o catálogo antigo exactamente como estava, e
  o ecrã diz qual das três coisas falhou em vez de dizer «erro».
- **O GitHub entra no ecrã «O que sai daqui»**, com o que se envia: o endereço, inevitável em
  qualquer pedido, e a versão da app. É o sexto destino da lista, e o único que nunca acontece
  sem alguém carregar num botão.
- **Um nutriente que foi procurado e não se encontrou passa a dizê-lo**, em vez de desaparecer
  do ecrã como se ninguém o tivesse medido. São duas coisas diferentes e agora distinguem-se:
  «está abaixo do limite que o método deteta» e «há, mas é pouco de mais para lhe pôr um
  número». São 10 612 e 792 no catálogo.

### Corrigido

- **Um vestígio deixa de poder entrar nas somas do dia.** Somar obrigava a escolher um número
  — zero, metade do limite, o limite — e qualquer escolha é aritmética sobre o que ninguém
  mediu. Aparece no alimento, e não na conta.
- **O ecrã de boas-vindas prometia 1376 alimentos medidos em Portugal**, e o das atribuições
  dizia o mesmo. São 1372: quatro dos do INSA estavam repetidos e foram fundidos na medição
  que ganhou a arbitragem. A tabela do INSA continua a ter 1376 — o que mudou é quantos deles
  o catálogo carrega com identificador próprio.

---

## [2.6.0] — 2026-08-23

Cada nutriente passa a ter um nome só. Esquema da base: **v28**.

### Corrigido

- **O sódio podia aparecer com dois valores diferentes para o mesmo alimento**, conforme o
  ecrã que o lia. Vivia em dois sítios ao mesmo tempo — numa coluna da linha do alimento e no
  mapa de micronutrientes — e a coluna guardava um inteiro arredondado enquanto o mapa
  guardava as casas decimais que a fonte publicou. Acontecia em 29 alimentos.

### Alterado

- **O sódio e a fibra passam a viver só no vocabulário**, como os outros nutrientes com meta
  diária. Deixam de ter coluna própria na linha do alimento.
- **Passam também a estar em quase todos os alimentos**, e não só nos do INSA: de 1 376 para
  7 686 e 7 754. Vinham da fonte para uma coluna que os ecrãs de micronutrientes não liam.
- A mediana de nutrientes por alimento sobe de **18 para 20**.

### Adicionado

- **Um vocabulário congelado**, com as 42 chaves de nutriente que a app conhece: o nome
  internacional do INFOODS ao lado de cada uma, a unidade, o grupo, e a referência da EFSA
  quando existe — com a indicação de ser meta a atingir ou tecto a não passar.
- **A construção do catálogo chumba se um importador emitir uma chave não declarada.** Dois
  nomes para o mesmo nutriente são dois nutrientes: a app mostrava os dois, cada um com
  metade dos alimentos, e as barras de ambos ficavam a meio sem razão nenhuma.

### Notas

- **Cinco dos oito nutrientes que o plano queria acrescentar não existem em fonte nenhuma.**
  A biotina, o crómio, o molibdénio, o flúor e a cafeína não estão na CIQUAL, nem na tabela do
  INSA, nem no USDA. Declará-los dava cinco chaves eternamente vazias.
- Dos outros três, o **sal** e a **energia em kJ** não são nutrientes novos: são o sódio ×2,5 e
  as kcal ×4,184 — o mesmo número noutra escala. A **frutose** só existe numa das fontes e não
  tem referência. Nenhum entra no catálogo.
- **Uma ferramenta nova**: `node tools/verificar.mjs` corre os testes, o detekt, o lint, as
  funções do servidor e as buscas de segredos, e responde em cinco linhas. Escreveu-se por
  causa da regra D1, e apanhou-se a si própria duas vezes enquanto era escrita — a somar
  relatórios de uma execução anterior, e a contar avisos do compilador como achados de estilo.

---


## [2.5.0] — 2026-08-22

O que é teu sai de dentro da linha do alimento. Esquema da base: **v27**.

### Corrigido

- **Os favoritos e os recentes passam a ir na cópia de segurança.** Não iam, e ninguém tinha
  como saber: da tabela de alimentos só se exportam os que tu crias — o catálogo não se
  exporta, por ser grande e reconstruível — e os favoritos viviam lá dentro. **Quem
  restaurasse uma cópia perdia tudo o que tinha marcado**, sem aviso e sem erro. Agora são
  uma tabela como as outras, e viajam como as outras.

### Alterado

- **O favorito, a última utilização e a porção guardada mudaram de casa.** Viviam dentro da
  linha do alimento, e o catálogo é substituído por inteiro a cada versão: a escrita gravava
  a linha toda por cima. A 2.4.0 corrigiu isso transportando-as à mão; agora o problema
  **deixa de poder existir** — o que não vive na linha do alimento não pode ser apagado ao
  escrevê-la.
- **A cópia de segurança passa a dizer com que versão do catálogo foi feita**, e aparece no
  resumo antes de importares. Não recusa nada: o diário copia a nutrição toda no momento do
  registo, por isso um histórico restaurado não depende do catálogo — recusar uma cópia mais
  recente bloqueava quem trocou de telemóvel sem proteger de nada.
- A limpeza do catálogo passa a olhar para a tabela nova ao decidir o que nunca se apaga.

### Notas

- **Esta versão é a primeira metade da 2.5.0 do plano**, que se partiu em duas ao ser medida.
  A tabela `foods` não é só catálogo: também lá vivem os alimentos que crias, os da Open Food
  Facts e os estimados por AI. A separação das bases é a 2.5.1, e depois desta é mecânica.
  A excepção de numeração está escrita em [docs/referencia/versionamento.md](docs/referencia/versionamento.md).
- **A cópia de segurança nunca levou o catálogo às costas**, ao contrário do que o plano
  dizia: já só se exportavam os alimentos criados pela pessoa. O que encolhe com a separação
  é a base do telemóvel, e não a cópia.
- **A migração é escrita à mão**, e é a primeira. O Room sabe criar e apagar tabelas, mas não
  sabe mudar dados de sítio: uma migração automática criava a tabela nova vazia e deitava as
  colunas fora com o que estava lá dentro.

---

## [2.4.0] — 2026-08-21

O catálogo de alimentos passa a ser construído fora da app. Esquema da base: **v26**, sem
mudanças.

### Adicionado

- **Dezasseis alimentos que se perdiam há meses.** A CIQUAL publica `-` quando um valor não foi
  determinado, e o importador antigo, ao encontrar um macronutriente por determinar, deitava fora
  o alimento inteiro — noventa e nove ao todo. Catorze recuperam-se pela própria equação da
  energia ao contrário: se a energia está publicada e falta um macro, o macro sai de lá. Dois
  vêm de um par exacto no USDA. Entre eles, sumo de arando, puré de castanha, agar seco, tomate
  comprido cru, cavala marinada e seis salsichas.
- **Uma lista do que não entra, com nome e razão.** Oitenta e três alimentos da CIQUAL ficam de
  fora porque lhes falta a energia **e** um macronutriente, e derivar um sem o outro seria
  inventar o número. Estão nomeados um a um em `tools/catalogo/desvios.json`. Antes
  desapareciam sem rasto.

### Corrigido

- **Uma receita podia perder um ingrediente em silêncio.** As limpezas do catálogo nunca
  apagavam um alimento que a pessoa tivesse posto como favorito, usado, ou registado no diário —
  mas não olhavam para as receitas nem para as refeições-tipo. Um ingrediente que só existisse
  numa receita podia ser apagado numa importação, e nada no ecrã dizia que faltava.
- **Favoritos, recentes e porções guardadas sobrevivem à troca do catálogo.** A escrita do
  catálogo grava a linha inteira por cima, e essas quatro colunas são as únicas que não vêm do
  ficheiro. Passam a viajar dentro da linha nova, e não a ser repostas a seguir: reposição tem
  uma janela em que uma interrupção as apagava.

### Alterado

- **Dezoito passos passam a uma pergunta.** O catálogo era cinco ficheiros semeados por ordem e
  treze correções que corriam no telemóvel a cada arranque, e o comentário do próprio ficheiro
  dizia que não podiam ser fundidos nem reordenados — quem tinha a app desde março passara por
  uns e não por outros. **O estado final dependia do caminho.** Agora o catálogo chega cozido num
  ficheiro só, e a app pergunta apenas se o que está gravado é mais antigo do que o que veio.
  Todas as instalações convergem para o mesmo estado, venham de onde vierem.
- **Corrigir um alimento deixa de custar código.** Custava uma versão na Play Store, porque cada
  correção era um passo novo no semeador. Passa a ser uma execução de `tools/catalogo/construir.mjs`.
- **A construção chumba quando perde um alimento.** A fonte declara quantos traz; se algum não
  chegar ao ficheiro e não estiver declarado, a construção falha. Foi assim que os noventa e nove
  se perderam sem ninguém dar por isso durante meses.
- A construção é **determinística** — duas execuções dão bytes idênticos — e a ordem é por código
  de caracteres e não pela localização da máquina, para que o `git diff` diga a verdade.

### Notas

- **Os nomes corrigidos à mão não regridem.** Cinco dos dezoito passos arrumaram nomes americanos
  ao longo de meses; reconstruir das fontes devolvia-lhes o nome de laboratório. Foram extraídos
  de uma instalação com os dezoito passos corridos até ao fim, e a reconstrução foi comparada
  alimento a alimento: **zero diferenças em 7 995 alimentos**, mais os dezasseis recuperados.
- **O plano dizia dezassete passos, e eram dezoito.** Contados no ficheiro: cinco importações e
  treze correções.
- **O plano prometia recuperar os noventa e nove, e só dezasseis eram recuperáveis.** Os outros
  oitenta e três não têm energia nem macronutriente publicados, e entrar com eles obrigava a
  escrever números que ninguém mediu. Ficam declarados, à espera da ausência tipada da 2.7.0.
- O catálogo passa de 7 998 para **8 011 alimentos**: 3 401 da CIQUAL, 2 937 do USDA, 1 376 do
  INSA, 284 portugueses curados e 13 extras escritos à mão.

---

## [2.3.0] — 2026-08-21

A app ganha movimento e uma linguagem de ecrã só. Esquema da base: **v26**, sem mudanças.

### Adicionado

- **Transições entre ecrãs**, e cinco delas, porque cada uma diz uma coisa diferente sobre a
  relação entre dois ecrãs. Os separadores desvanecem — não há hierarquia entre eles. Um
  detalhe entra da direita com o ecrã de trás a acompanhar a **um terço** da velocidade e a
  escurecer, que é a paralaxe que os faz lerem-se como folhas empilhadas. A câmara e o leitor
  sobem de baixo. Uma sessão de treino ou uma corrida **crescem de dentro**, com o ecrã
  anterior a recuar para trás: não se está a navegar, está-se a entrar noutro estado da app.
  Um resumo sobe e **assenta**, com um travar no fim — é o único movimento com peso, e
  acontece uma vez por sessão. Voltar é sempre o movimento ao contrário, e não outro.
- **Quem desligou as animações no telemóvel não as recebe.** A app lê a definição do sistema.
  Não é uma preferência da app: quem as desliga fá-lo por enjoo com movimento, por bateria,
  ou porque num aparelho lento cada animação é meio segundo de espera.
- **Num tablet, o alimento abre ao lado do diário** em vez de o tapar. Abrir um registo,
  voltar atrás e abrir o seguinte era o percurso mais cansativo da app num ecrã grande — a
  biblioteca de exercícios já o tinha resolvido, e o diário passa a usar o mesmo painel.
- **Numa janela larga a lista do painel pára de crescer** e fica com 340 dp, e o espaço que
  sobra vai para o detalhe, que é o lado com imagens e instruções. Antes, os dois modos de
  ecrã largo desenhavam exactamente o mesmo.

### Corrigido

- **Trinta e dois ecrãs perdiam o conteúdo por baixo do teclado.** Usavam o andaime do
  Material em vez do da app, e é no da app que vive o empurrão que faz o conteúdo subir
  quando o teclado abre. Escrever num campo em baixo do ecrã significava deixar de o ver.
  Não dava erro nem aviso.

### Alterado

- Nasce o **`AntaresScreen`**: andaime, rolagem, largura de leitura e margem por omissão.
  Trinta e sete ficheiros escreviam a largura à mão, o que quer dizer que um ecrã novo ficava
  sem ela por esquecimento e se esticava por 1200 dp num tablet.
- O **`AntaresCard` passa a aceitar clique**, que era a razão de vinte ficheiros usarem o
  cartão do Material — e levarem com ele cantos, elevação e espaçamento a diferir de ecrã
  para ecrã sem ninguém ter decidido isso. Ficam duas excepções, escritas com a razão.
- Nasce a **`LinhaDaLista`**. O `MenuItem` e o `MeItem` eram cópias exactas um do outro,
  comentário incluído; o `ToggleRow` e o `SettingSwitchRow` também.

### Notas

- **A migração não foi de sete implementações para uma**, como o plano dizia. Foram contados
  vinte e quatro composables de linha e só quatro eram mesmo repetição: as outras são linhas
  de domínio com afordâncias próprias — apagar, deslizar, editar, mover — e colapsá-las seria
  tirar-lhes coisas.
- **O Progresso não ganhou painel lista-e-detalhe**, e não por falta de tempo: não tem lista
  nem detalhe. É uma coluna de cartões, e o que se abre a partir deles são ecrãs inteiros, o
  que é um menu e não uma lista.
- O teste da largura de leitura foi escrito duas vezes. À primeira exigia o mecanismo e
  acusou dez ecrãs que estão certos: são listas, e a lista adaptável transforma largura em
  colunas. **A regra é sobre o resultado** — ou se limita a linha, ou se entrega a largura a
  um contentor que a converte em colunas.

---

## [2.2.0] — 2026-08-21

A app passa a dizer o que sai daqui, e a deixar cortar o que sai todos os dias. Leva também
o que uma auditoria às três versões anteriores encontrou por fazer e por corrigir. Esquema
da base: **v26**, sem mudanças.

### Adicionado

- **Ecrã «O que sai daqui»**, no menu. Seis linhas, cada uma com o que vai e quando: a Open
  Food Facts, a análise por foto e por texto, o mapa das corridas, as imagens dos exercícios,
  o pedido de apagamento — e, numa secção à parte porque não é rede, a cópia de segurança.
  Estes cinco destinos estavam descritos no repositório desde sempre e em lado nenhum dentro
  da app. Quem usa a app não lê o repositório.
- **Aviso antes da primeira procura em linha.** A app diz que o texto vai à Open Food Facts
  **antes** de o enviar, e não depois. Recusar ali desliga a pesquisa; é a mesma escolha que
  o interruptor guarda.
- **Interruptor da pesquisa em linha**, nas definições. Corta a procura por texto e a leitura
  de código de barras — os dois vão ao mesmo sítio pela mesma razão. Fica ligado por omissão,
  que é como a app sempre funcionou. Desligada, a app diz que está desligada em vez de fingir
  que não há resultados.
- **Nome sugerido para a corrida**: «Corrida da manhã», já escrito no campo e apagável. O
  campo abria vazio, quase ninguém escrevia nada, e o detalhe de todas as corridas chamava-se
  «Resumo» — o ficheiro GPX exportado saía sem nome nenhum.

### Corrigido

- **O tipo de atividade e a auto-pausa não eram guardados.** Quem anda sempre a pé escolhia
  «caminhada» a cada arranque; quem desligava a auto-pausa encontrava-a ligada na vez
  seguinte. A meta da corrida, na mesma linha de código, já era guardada.
- **A primeira cópia de segurança automática saía vazia.** Numa instalação limpa a cópia
  disparava antes de haver o que copiar: vinte e seis tabelas, zero linhas, 526 bytes — e o
  cartão a dizer «última cópia: hoje». Uma cópia que não protege nada é pior do que nenhuma,
  porque **cala o aviso**. A cópia passa a esperar pelo fim do arranque, e sai no instante em
  que ele acaba.
- **Substituir os dados aceitava uma cópia sem uma linha.** Esse ficheiro vazio tinha os dois
  campos que a assinatura exige, portanto passava por cópia legítima, e substituir com ele
  esvaziava todas as tabelas — catálogo incluído. Era um ficheiro que a própria app tinha
  escrito e posto na pasta. Passa a ser recusado antes de a transação abrir.
- **O texto das fotos de progresso deixou de ser verdade na 2.1.0**, e ninguém o disse. Dizia
  que «nenhuma outra aplicação lhes chega»; desde que a cópia automática as leva para
  «Documentos/Antares», uma pasta partilhada, isso é falso. O texto passa a dizer o que
  acontece, e porquê: é o preço de as fotos sobreviverem à perda do telemóvel.
- Duas frases diziam «1 backups» e «1 registos».

### Alterado

- `docs/referencia/regras.md` dizia **«vinte e oito»** regras — são vinte e nove — e **«38
  testes-guarda»**, quando são cinquenta. Três testes-guarda que existiam desde antes do
  plano — `PlateauHonestyTest`, `GoalGuardrailsTest` e `TargetBreakdownSweepTest` — estavam
  sem documentação.

### Notas

- **A frase do primeiro ecrã continua a dizer «Nada sai do telemóvel», e é falsa.** Ler um
  código de barras vai à Open Food Facts e uma análise por foto vai à Anthropic, nenhuma das
  duas por exportação de ninguém. Corrigi-la ficou para a **2.40.0**, onde o arranque é
  reescrito, por decisão do dono. Fica dito aqui para não se perder.
- O interruptor **não é um modo avião**: o mapa das corridas, as imagens dos exercícios e a
  análise por AI continuam a poder sair, cada uma quando é pedida. O ecrã novo diz quando.

---

## [2.1.0] — 2026-08-20

A cópia de segurança deixa de depender de alguém se lembrar dela. Não melhora nada do que a
app faz; evita perder três anos do que ela já guardou. Esquema da base: **v26**, sem
mudanças.

### Adicionado

- **Cópia automática para «Documentos/Antares».** De três em três dias, no arranque, com as
  cinco últimas em rotação. Vai lá o mesmo ZIP da exportação manual — dados, CSV e fotos de
  progresso. A pasta é a de Documentos do telemóvel e não a da app: **continua lá depois de
  desinstalar**, que é precisamente o caso de que uma cópia serve para proteger.
- **Cartão de estado da cópia**, no menu e no Hoje. Diz há quantos dias foi a última, onde
  ficou e quantas estão guardadas, e tem um botão para copiar já. No Hoje só aparece quando
  passa uma semana sem cópia: um cartão permanente a dizer que está tudo bem é um cartão que
  se deixa de ler.
- **O que vai na cópia, com contagens.** O ecrã da cópia diz quantas refeições, treinos,
  corridas, pesagens e fotos vão no ficheiro. Uma cópia vazia escrita por um erro passa a
  ver-se de relance.
- **Resumo do ficheiro antes de importar.** A data em que foi exportado, a versão que o
  escreveu e quantos registos traz de cada tipo. Substituir os dados é irreversível e era
  decidido às cegas.
- **Confirmação antes de limpar as pesquisas falhadas.** A lista é o registo do que falta no
  catálogo; apagá-la por engano perde meses de sinal que ninguém repete de propósito.

### Removido

- **A cópia automática da Google.** `allowBackup` passa a `false` e as duas regras de
  extração foram apagadas. Era uma cópia que ninguém via, que dependia de haver sessão
  Google iniciada e de a Google decidir corrê-la, e que **nunca levou as fotos de
  progresso** — quem restaurasse por ela recuperava o histórico com as imagens em falta.

  À letra da regra de versionamento isto é uma MAIOR: quem atualiza perde uma coisa com que
  contava. Fica MENOR porque a capacidade é **substituída no mesmo lançamento** e não
  retirada — a pasta nova sobrevive à desinstalação tal como a cópia da Google sobrevivia, e
  a primeira cópia automática corre no primeiro arranque depois de atualizar, e não ao fim
  dos três dias. Não há janela nenhuma em que alguém fique sem rede.

### Notas

- No Android 9 e anteriores a app pede a permissão de escrita para poder gravar em
  Documentos; a partir do Android 10 escreve pelo MediaStore e não pede nada. A permissão
  está declarada com `maxSdkVersion="28"`, ou seja não existe nos telemóveis modernos — e o
  `READ_EXTERNAL_STORAGE` que o Android acrescenta sozinho ao lado dela herda o mesmo teto.
- **Uma cópia feita antes de reinstalar deixa de ser gerida pela app.** O Android marca cada
  ficheiro com a app que o criou e apaga essa marca quando a app é desinstalada — verificado
  no emulador. O ficheiro fica na pasta e continua a poder ser aberto pelo botão de importar,
  que é o que interessa; o que a app deixa de poder fazer é listá-lo e apagá-lo. Quem
  reinstalar começa uma série nova de cinco, e as antigas ficam lá até serem apagadas à mão.
  Não se resolve sem pedir acesso ao armazenamento inteiro, que é desproporcionado para gerir
  cinco ficheiros.
- O `BackupRulesTest`, que verificava as regras da cópia na nuvem, foi substituído pelo
  `SemCopiaNaNuvemTest`, que verifica o contrário — e que a cópia local que a substitui não
  desapareceu.

---

## [2.0.4] — 2026-08-18

Nove coisas que a app dizia e não eram verdade. Nenhuma parte nada — todas fazem quem lê
acreditar numa coisa que não acontece. Esquema da base: **v26**, sem mudanças.

Os quatro achados que mais decidiam a versão foram confirmados no código antes de se lhes
tocar, e um estava mal lido: **a administração não estava escondida** — tinha secção própria
e uma entrada à vista no fim das definições. O item era para a esconder, não para lhe abrir
uma porta.

### Corrigido

- **Dois controlos do perfil gravavam e não faziam nada.** A janela da tendência (7 ou 28
  dias) escolhia-se e a conta ignorava-a; a meta de massa gorda escrevia-se e não voltava a
  aparecer. Ligados em vez de apagados: a app já prometia as duas coisas, e tornar
  verdadeira uma promessa feita é corrigir. A meta mostra-se ao lado da medida a que se
  compara, com os pontos que faltam.
- **A instrução de medir a cintura era igual para os dois sexos.** A fórmula da Marinha pede
  o umbigo no homem e o ponto mais estreito na mulher, e medir no sítio errado invalida o
  erro-padrão de 3,6 pontos que a app declara. São agora duas frases, escolhidas pelo
  perfil — e **o pescoço**, o outro valor da fórmula, ganha a instrução que nunca teve.
- **Os micronutrientes não levavam a lado nenhum.** Uma linha dizia que um nutriente está a
  34 % e não era tocável, quando o ecrã dos alimentos ricos nele já existe e já se alcança
  do Hoje e do perfil.
- **O desnível da corrida faltava no resumo**, que é o ecrã que se vê logo a seguir a
  correr, e no detalhe vinha em metros a quem usa pés. Nasce um formatador de altitude, e
  os dois ecrãs passam a usá-lo.
- **O interruptor das metas adaptativas estava em dois ecrãs** com o mesmo título e a mesma
  descrição. Fica o das definições, na secção do comportamento.
- **O relógio do jejum acordava de segundo a segundo** para mostrar horas e minutos:
  cinquenta e nove em cada sessenta voltas redesenhavam o ecrã sem mudar um algarismo, e o
  desvio acumulado atrasava a mudança até um segundo. Dorme até à mudança do minuto.
- **O cartão do treinador desenhava uma linha em branco** quando o foco vinha vazio.
- **A administração estava à vista** no fim das definições. Aparece agora só depois de sete
  toques na versão, como o Android faz com o modo de programador. Continua a pedir o
  código.

---

## [2.0.3] — 2026-08-18

Cinco defeitos que mexem em números, e o teste que impede cada um de voltar. Nenhum parte a
app: os cinco fazem-na dizer uma coisa e guardar outra, que é a única espécie de defeito que
não se nota a usar. Esquema da base: **v26**, sem mudanças.

Primeira versão saída do plano de produção. Os cinco achados foram confirmados no código
antes de se lhes tocar — e dois números estavam errados: eram **oito** estilos de tipografia
em falta e **176** usos, não sete e 175. Um achado estava meio errado: o peso já era
validado, só a altura é que não.

### Fixed

- **O peso de uma série era arredondado a inteiro, em dois sítios.** Uma série feita a
  62,5 kg reaparecia pré-preenchida como 63, e bastava não reparar para o registo ficar com
  meio quilo a mais. A linha da série gravada dizia 63 e a correção dizia 62,5 — a mesma
  série, dois números, no mesmo cartão. Entra um formatador próprio para a carga, ao lado do
  que já existia para o peso do corpo; os volumes somados continuam arredondados, que é o
  que se quer em 20 587 lb.
- **Uma série gravada não se corrigia.** A função existia e ninguém a chamava: com o peso
  errado, a única saída era apagar e refazer — e o cronómetro de descanso recomeçava. Um
  toque na linha abre agora a correção do peso e das repetições. O RPE e o aquecimento
  ficam de fora de propósito: são o que se sentiu na altura, e mudá-los seria reescrever a
  memória do treino em vez de corrigir um engano.
- **Apagar uma série a meio deixava a seguinte com um índice repetido**, porque o índice
  saía da contagem das séries e não do maior já usado. É por esse índice que o histórico
  ordena. Apagar passa a deixar buracos na numeração, que ninguém vê — o ecrã mostra a
  ordem, não o número.
- **Oito dos quinze estilos do Material 3 não declaravam fonte**, e caíam no Roboto do
  sistema sem erro nem aviso: **176 utilizações**, a maior delas o `bodySmall`, com 123. O
  `TipografiaCompletaTest` passa a chumbar a compilação se algum voltar a ficar por
  declarar.
- **Uma altura fora de 100–250 cm era descartada em silêncio.** O campo ficava com o número
  escrito, e o metabolismo continuava a ser calculado com a altura antiga. Passa a dizer o
  intervalo aceite, em centímetros ou em pés e polegadas conforme as unidades.

Três regras de trabalho ganharam testes que as cobram — os números do catálogo citados nos
documentos, as dúvidas deixadas no código, e as migrações de esquema sobre uma base cheia —
e os dois greps de segredos passaram a viver num `pre-push`. Não mudam a app; mudam o que
pode entrar nela.

### Fixed — antes desta versão

- **O CI estava vermelho em todos os pushes** desde que os testes de interface entraram, e
  não havia por onde ver. O `gradlew build` corre também o `testReleaseUnitTest`, e o
  `compose-ui-test-manifest` — que declara a `ComponentActivity` onde esses testes compõem —
  entra como `debugImplementation`. Vinte e um testes davam «Unable to resolve activity».
  Passa a correr os testes na variante debug e a compilar o artefacto na release.
- **O modo de demonstração inventava cargas**: «Ankle Circles — 155 kg», porque o peso saía
  de um embaralhado do identificador do exercício. Passa a sair da escala do equipamento, e
  o peso do corpo deixa de levar carga nenhuma.
- **O catálogo da demonstração era uma fatia alfabética** — daí 354 g de mexilhão cru e 109 g
  de licor ao pequeno-almoço. Espalha-se pelo catálogo e exige densidade energética
  plausível, para as porções deixarem de ser absurdas.
- As ações do CI passam a v5: o GitHub avisava que as v4 têm como alvo o Node 20, já
  descontinuado.

---

## [2.0.2] — 2026-08-16

Uma caçada geral, com a app a correr em libras e em inglês. Esquema da base: **v26**.

### Fixed

- **O sistema imperial ainda era meio sistema.** O cartão do corpo mostrava «153,9 lb» e,
  três linhas abaixo, «0,4 kg/semana», «Tendência: 70,6 kg» e «Faixa saudável: 58,6–79,2 kg».
  O guarda que existia para isto só varria ficheiros Kotlin, e estes «kg» viviam dentro dos
  `strings.xml`. Foram catorze textos, e o guarda passa a varrer os dois idiomas.
- A cintura passa a polegadas e a altura a pés e polegadas para quem usa imperial.
- Nos totais das corridas, «107:56:02» partia-se em «107:56:0» e «2» na linha seguinte, e
  «630.21 mi» em duas linhas. Um total que se parte lê-se como outro número.
- Um teste-guarda falhava conforme a ordem em que os testes corriam: provocava a falha
  contando com que os recursos do Compose não existissem, e eles passam a existir assim que
  outro teste no mesmo processo os arranca.

### Removed

- **A preferência entre kcal e kJ, que nunca chegou a existir.** Havia coluna na base, havia
  quem a soubesse escrever — e não havia opção em ecrã nenhum, nem um único sítio que a lesse
  para converter um número. Sai por automigração 25→26, como saiu a coluna `dirty`.
- Três textos que nenhum ecrã usava, um deles a prometer que o apagamento levava os dados
  «no servidor» — e não há servidor desde a 1.0.0.

### Added

- Dois testes para uma pergunta que a remoção de uma coluna levanta: uma cópia de segurança
  feita antes dela ainda abre? Abre, e agora está provado nos dois lados.

---

## [2.0.1] — 2026-08-16

Três defeitos que nenhum dos 1338 testes apanhou, porque os três só se vêem num ecrã. Foram
encontrados a correr a 2.0.0 num emulador — de telemóvel, deitado e de tablet — logo a seguir
a lançá-la. Esquema da base: **v25**, sem alterações.

### Fixed

- **O gráfico do peso parecia uma subida onde havia descida.** Os extremos do eixo vertical
  estavam um em cada ponta, mesmo por cima das datas: lia-se «69,2 a 18 de julho» e «75,6 a 12
  de agosto» por baixo de uma tendência que dizia menos 1,5 kg. Passam a ser uma linha só, com
  o eixo escrito por extenso.
- **Em libras, o volume do treino mostrava quilos.** O «kg» estava escrito dentro do texto, e
  o número ia em bruto.
- **A distância misturava vírgula com ponto.** «6,87 mi» ao lado de «153.9 lb» no mesmo cartão:
  a corrida tinha a vírgula fixa e o resto da app escolhe o separador pelo idioma.
- **Num tablet, as listas dentro do painel de detalhe apertavam-se a três colunas** onde só
  cabiam duas. Contavam pela janela; passam a contar pela caixa onde estão.
- O botão de criar exercício tapava a última linha da lista.

---

## [2.0.0] — 2026-08-16

Cinquenta tarefas decididas item a item a 2026-08-14 e feitas uma a uma, cada uma com o seu
commit e a suite verde. Esquema da base: **v25**.

**Porque é MAJOR e não MINOR:** duas coisas que existiam na 1.0.0 deixam de existir — o ecrã
de conquistas e o estilo «Antares Ring» do ecrã Hoje. A regra escrita em
[docs/referencia/versionamento.md](docs/referencia/versionamento.md) não pergunta se a versão é
importante; pergunta se quem atualiza perde alguma coisa com que contava. Perde.

### Removed

- **O ecrã de conquistas.** Desbloqueava 22 de 22 de uma vez, com emojis ao lado da iconografia
  do resto da app: eram contadores disfarçados. Os marcos do Progresso dizem o mesmo sem enfeite.
- **O estilo «Antares Ring» do ecrã Hoje.** Dois estilos duplicavam a superfície a manter e a
  testar por um ganho estético. Ficou o clássico, por escolha do dono.
- **As 23 tabelas de sincronização** saíram das migrações do servidor. A app não sincroniza desde
  a 1.0.0, e as tabelas ficaram lá a ocupar espaço e a confundir quem lesse o esquema.
- A coluna `dirty`, que marcava linhas por enviar para um servidor que já não existe, saiu de 23
  tabelas por migração automática.

### Added

- **A app roda.** A tranca do retrato saiu. Paisagem, tablet e ecrã dividido têm esquema próprio:
  uma coluna, lista e detalhe lado a lado, ou duas colunas, conforme a largura da janela.
- **Desfazer** em tudo o que apaga — registo, pesagem, série, rotina, receita, refeição guardada.
  Só as fotografias de progresso e o apagamento da conta não voltam atrás, e ambos avisam antes.
- Registo rápido a partir do Hoje e do diário, por texto, por foto ou por código de barras.
- **Lembrete de água** pelo que falta beber, com horas de silêncio à escolha, e lembrete de
  pesagem no dia e à hora que a pessoa escolher.
- **Filtros nos históricos** de treino e de corrida: por mês, por exercício e por tipo.
- **Receitas com doses.** Uma receita passa a ter um número de doses, e registá-la propõe uma dose
  em vez do tacho inteiro.
- Dez nutrientes novos, que a CIQUAL já trazia e o gerador do catálogo deitava fora.
- Proveniência real em cada alimento: quem o mediu, e quando.
- Aviso de alimento duplicado antes de criar mais uma cópia, e criação a partir de uma pesquisa
  que não deu nada.
- A leitura contínua de códigos de barras guarda **quais** os códigos que não foram encontrados,
  e oferece criá-los.
- Estatísticas de micronutrientes por dia, semana, mês e ano.
- O ciclo passa a poder marcar a data em que aconteceu, e não a de hoje.
- As imagens dos exercícios têm os três estados que têm mesmo: a carregar, sem ligação, e a
  imagem. E ficam em cache no telemóvel.

### Changed

- **As calorias do exercício passam a ser líquidas** — o que ele gasta a mais do que estar
  sentado. O repouso já está dentro da meta diária, e somá-lo outra vez contava-o duas vezes.
  Quem regista exercício vê o orçamento do dia descer.
- **A meta de água segue a referência da EFSA, por sexo**, e é de água total: a que vem da comida
  passa a contar para ela.
- **O chão de proteína escala** com o treino de força e com a profundidade do défice, entre 1,8 e
  2,8 g/kg, e o «Mostra-me a conta» explica porquê.
- **O basal calculado a partir da fita métrica diz a margem que tem** (±62 kcal): o método da
  marinha americana tem 3,6 pontos percentuais de erro-padrão, e fingir precisão seria mentir.
- O sistema imperial passou a ser um sistema inteiro: peso, distância, ritmo e porções.
- A refeição segue a hora a que se comeu, e não a hora a que se registou.
- Uma segunda pesagem no mesmo dia passa a perguntar o que fazer, em vez de decidir sozinha.
- O onboarding deixa saltar os passos a que a app sabe responder sozinha.
- O treino faz-se um exercício de cada vez, em vez de mostrar a rotina toda aberta.
- O ecrã de jejum foi refeito à volta do que o relógio diz.
- Sugestões e registos deixam de se confundir no diário, e a cor passa a dizer uma só coisa.
- O `CoreModule` e o grafo de navegação foram partidos por área.

### Fixed

- O apagamento da conta filtrava por uma coluna que não existe na `purchase_events`: devolvia 400
  e a conta **nunca chegava a ser apagada**. E não tocava em quatro tabelas com dados por pessoa.
- Restaurar uma cópia de segurança passou a acontecer dentro de uma transação. Uma falha a meio
  deixava a pessoa sem os dados antigos e com metade dos novos.
- Os ecrãs de lista que ficavam mudos quando vazios passaram a dizer o que fazer.
- O mapa de consistência contava desde sempre e não desde que a pessoa começou, e dava «1%».
- Imagens e gráficos passaram a ter descrição, e cada silêncio passou a ter razão escrita.

---

## [1.0.0] — 2026-08-04

A app passa a viver só no telemóvel. Esquema da base: **v21**.

### Removed

- **A conta e a sincronização.** Nada do que registas sai deste telemóvel. As tabelas que o
  servidor tinha continuam nas migrações antigas por causa de quem usou versões anteriores, mas
  não recebem escritas — o `NoSyncTest` falha se isso mudar.

### Added

- **O backup é um ficheiro teu**: exportas, guardas onde quiseres e restauras. Leva as fotos de
  progresso.
- Ao restaurar, escolhes **juntar** ao que já tens (fica o mais recente) ou **substituir** tudo.
- O backup tem lugar próprio nas Definições, a um toque. Estava no fim de «Detalhes e metas».
- Quando a app fecha sozinha, o motivo fica guardado e podes **partilhá-lo**. Antes não deixava
  rasto nenhum.

### Fixed

- Dois exercícios com o mesmo nome deixaram de rebentar o detalhe do treino.
- A app lia um megabyte de exercícios **em todos os arranques**, mesmo já os tendo.

---

## [0.14.0] — 2026-07-31

Ciclo, gravidez e amamentação. Esquema da base: **v20**.

### Added

- Registo do ciclo, **só neste telemóvel**. A retenção dá 1 a 3 kg, e a app diz isso em vez de
  te deixar pensar que é gordura.
- Referências de nutrientes na gravidez, na amamentação e na pós-menopausa.

### Changed

- Em gravidez e amamentação a app deixa de propor défice.

---

## [0.13.0] — 2026-07-29

O único ecrã que olha para trás. Esquema da base: **v19**.

### Added

- Separador **Progresso**: gráfico do peso com escala, tendência e o peso-alvo marcado.
- Mapa de consistência — doze semanas de dias registados, de relance.
- A linha do tempo dos teus objetivos: quando os puseste e quando lá chegaste.
- Marcos factuais — 30 dias registados, 5 kg de mudança — sem elogios vazios.
- **Fotos de progresso, só neste telemóvel.**
- **1376 alimentos portugueses do INSA**, medidos em Portugal, todos com micronutrientes.
- Braço, coxa e peito passam a ficar guardados ao longo do tempo.
- «Já estiveste aqui»: o mesmo peso, mas o corpo já não é o mesmo.

---

## [0.12.0] — 2026-07-28

A aritmética aberta. Esquema da base: **v18**.

### Added

- **«Mostra-me a conta»** — a aritmética da tua meta, aberta, com os teus números.
- Pausa de dieta: comer à manutenção umas semanas em vez de cortar mais.
- Quando a balança não responde há semanas, a app pára de cortar e explica porquê.

### Changed

- Chegar ao peso-alvo deixa de te manter em défice: a app propõe manutenção.
- Um peso-alvo abaixo do saudável para a tua altura passa a ser dito.
- A partir dos 65 o mínimo de proteína sobe; abaixo dos 18 o ritmo é mais conservador.

---

## [0.11.0] — 2026-07-27

O corpo e o rumo. Esquema da base: **v17**.

### Added

- **Composição corporal**: sabes o valor, estimas por medidas ou pelo IMC — e a app diz sempre
  qual foi.
- Com a percentagem de gordura conhecida, a meta é calculada pela massa magra em vez do peso
  todo.
- Peso-alvo com data prevista pelo teu ritmo real — e a app cala-se quando os dados não chegam.
- Régua do IMC, FFMI, e a manutenção sempre à vista.
- Altura em pés e polegadas para quem usa imperial.

### Changed

- O ritmo escolhe-se em kg por semana; as calorias são a consequência, com a zona segura à
  vista.
- O nível de atividade passou a descrever o dia **sem** treinos. Antes o exercício contava duas
  vezes.

---

## [0.10.0] — 2026-07-22

A app deixa de fazer perguntas que já sabe. Esquema da base: **v14–v16**.

### Added

- A app aprende a tua **dose habitual** de cada alimento — e nunca inventa números.
- Guarda uma refeição inteira e repete-a; copia-a de qualquer dia.
- **«Outra vez»** — repete num toque a última vez que comeste essa refeição.
- Marca vários alimentos e regista-os todos de uma vez.

### Changed

- Os líquidos deixaram de ser contados como sólidos.

---

## [0.9.0] — 2026-07-20

A balança preenche-se sozinha. Esquema da base: **v13**.

### Added

- **Health Connect**: o peso e os treinos entram sozinhos, e a composição corporal passa a ser
  escrita além de lida.
- Cada pesagem passa a saber de onde veio, para a app não reimportar o que já cá está.

---

## [0.8.0] — 2026-07-18

Esquema da base: **v12**.

### Added

- **Relatório semanal**: a app olha para a semana passada e diz-te como comeste. Factos, e não
  conselhos.
- Quando a balança e o registo discordam ao longo de semanas, propõe um ajuste ao ritmo — que só
  se aplica se aceitares.

---

## [0.7.0] — 2026-07-12

Esquema da base: **v9**.

### Added

- **Plano semanal**: as rotinas passam a ter dia marcado.

---

## [0.6.0] — 2026-07-10

Esquema da base: **v8**.

### Added

- **Corrida com GPS**: percurso, ritmo, distância e mapa, com pausa automática.

---

## [0.5.0] — 2026-07-09

Esquema da base: **v7**.

### Added

- **Jejum**: protocolos, a janela a contar e o histórico.

---

## [0.4.0] — 2026-07-08

Esquema da base: **v5–v6**.

### Added

- **Treino de força**: catálogo de exercícios, rotinas, sessões e séries.
- Recordes estimados a partir das séries feitas.
- Exercício avulso, para quem não quer montar uma rotina.

---

## [0.3.0] — 2026-07-07

Esquema da base: **v4**.

### Added

- **Receitas**: ingredientes, peso depois de cozinhar, e registo de uma porção no diário.

---

## [0.2.0] — 2026-07-06

Esquema da base: **v3**.

### Added

- **O diário alimentar**: alimentos, pesquisa, e a água do dia.

---

## [0.1.0] — 2026-07-05

Esquema da base: **v2**.

### Added

- **Perfil e pesagens**, e a meta diária calculada a partir deles.

---

## Sobre este histórico

Este ficheiro foi escrito a **2026-08-14**, e não ao longo do caminho.

A app foi desenvolvida em privado entre 2026-07-05 e 2026-08-04 com uma numeração de quatro
segmentos que chegou a `0.9.18.1`, com 63 versões e sem changelog nenhum. Este histórico
reconstrói-a em SemVer a partir de duas fontes que não dependem de memória:

- **`composeApp/schemas/`** — os 21 esquemas exportados da base de dados dizem em que ordem cada
  funcionalidade ganhou sítio onde viver.
- **As datas dos artefactos** que estão fora do repositório, arredondadas ao dia.

Foi reordenado e cortado de propósito, e por isso **as versões aqui não correspondem uma a uma
às antigas**. Duas regras guiaram o corte:

1. **Nunca se anuncia o nascimento de uma coisa que já não existe.** A app teve conta e
   sincronização durante o desenvolvimento; isso aparece uma vez, como remoção na 1.0.0, e nunca
   como novidade. Um changelog que apresenta funcionalidades removidas é a forma mais eficaz de
   fazer alguém descrever a app errada com toda a confiança.
2. **Onde há texto escrito na altura, manda o texto; onde não há, manda o esquema.** O esquema
   diz quando uma funcionalidade passou a *poder* existir, e não quando chegou ao ecrã.

As versões anteriores à 1.0.0 foram compilações de desenvolvimento, sem registo público.

[Unreleased]: https://github.com/BetuelRS/Antares/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/BetuelRS/Antares/releases/tag/v1.0.0
