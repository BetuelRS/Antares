# Como continuar

> **Antes de trabalhar numa área, abrir os documentos dela em `estudo/`** — o de área, o
> esboço quando existe, e os do motor ou do sistema que a versão tocar. É a regra **C5**, e
> a rota está em [`a-divida-com-o-estudo.md`](a-divida-com-o-estudo.md), e o que já se sabe
> que falta em [`o-que-ficou-de-fora.md`](o-que-ficou-de-fora.md).
>
> **Nasceu de um falhanço:** treze versões saíram construídas a partir das linhas-resumo do
> plano, sem um único esboço aberto — e este ficheiro, que é o que se lê ao começar, não
> mencionava o estudo em lado nenhum. A 2.18.0 divergiu do esboço dela no ponto principal.
>
> **A auditoria está feita, a 2026-08-28.** Os dezassete documentos da rota foram lidos e o
> registo de cada versão no plano nomeia os que lhe tocam. A rota continua a valer para as
> versões seguintes.

Este ficheiro é o que se lê ao começar uma sessão nova. Substitui a mensagem que antes era
reconstruída de memória a cada vez — e que, sendo reconstruída, envelhecia sem ninguém notar.

**Actualiza-se ao fim de cada versão**, no mesmo commit que a publica.

## Onde estamos

- **2.25.0 é a última fechada**, a 2026-09-04. Esquema **v39** e catálogo **v6**, inalterados, e
  **nenhum dado é novo**. «As estatísticas do treino»: seletor de período com os quatro chips da
  nutrição, séries por músculo por semana com a faixa de 10 a 20, treinos e volume por semana
  desenhados com o `AntaresChart`, e os recordes com a data em que aconteceram. 1766 testes
  Kotlin, 58 das ferramentas, 68 Deno, detekt e lint limpos.
- **A versão abriu pelas correcções das anteriores**, por decisão do dono: o `%d min` do cartão
  de destaque — que era o **quinto** sítio e não o quarto —, doze importações mortas no módulo
  do treino, e os nomes das sete rotinas semeadas, que eram literais e em duas línguas.
- **«Esta semana» passa a querer dizer o mesmo em toda a app.** Este ecrã contava sete dias para
  trás a partir do relógio; o painel de treino, o treinador e a grelha do progresso contam a
  semana ISO. E os chips contavam 24 h × N enquanto os da nutrição — os mesmos quatro rótulos —
  contam dias inteiros.
- **A faixa de 10 a 20 séries foi decisão delegada, e a razão está escrita para poder ser
  desfeita:** não é o caso dos «65 % da flexão» que a 2.22.0 recusou — ali eram cento e onze
  frações inventadas, aqui é um intervalo publicado que o esboço nomeia, mostrado a dizer que
  não é um alvo calculado para esta pessoa.
- **A D7 apanhou seis defeitos na primeira passagem e dois na segunda**, e três deles são a
  mesma coisa: **duas janelas de tempo lidas juntas**. Os dois gráficos não cobriam a mesma
  primeira semana; os chips não contavam dias inteiros; e a linha do músculo dizia «15 séries ·
  39 000 kg» com a média da semana ao lado do total do mês.
- **A varredura de código morto só via oito dos quarenta e um ficheiros do `composeApp/src/commonMain/kotlin/pt/antares/app/core/calc/`** — os
  que acabam em `Calc.kt`, que é uma convenção de nomes que a pasta já tinha abandonado. Passa
  a ver a pasta inteira, e a contar chamadas com lambda à direita: sem isso acusava de morta uma
  função chamada três linhas abaixo.
- **A próxima do plano é a 2.26.0**, «O resumo pós-treino», com o esboço `10-treino-estatisticas`
  outra vez — é a secção 3 dele. As perguntas de abertura estão escritas no plano.
- *O que se segue é o estado até à 2.24.0.*
- **A 2.24.0 está publicada e verde**, conferido a 2026-09-04: `main` em `e6f03c6`, etiqueta
  `v2.24.0`, release com os quatro APKs mais o `catalogo.json` e o `manifesto.json`, e **CI
  verde nesse commit**. O `latest` responde HTTP 200 aos dois ficheiros e o `sha256` bate.
- **A varredura de 2026-09-04** — o `estudo/` lido inteiro e a 2.24.0 de lançamento corrida
  por cima da 2.20.1 com dados — encontrou **um defeito da própria 2.24.0**: o `%d min` sem
  conversão estava em **cinco** sítios e não em quatro. Fica em
  [`o-que-ficou-de-fora.md`](o-que-ficou-de-fora.md), com o resto do que ela mediu.
- **2.24.0 é a última fechada**, a 2026-09-03. Esquema **v39** e catálogo **v6**, inalterados,
  e **nenhum dado é novo** — é a mesma forma da 2.20.0. «O histórico do treino»: quatro dados
  em cada linha, a 🌟 dos recordes, cabeçalho no detalhe, o RPE que se via pela primeira vez, e
  o filtro por rotina no lugar do de exercício. 1742 testes Kotlin, 58 das ferramentas, 68
  Deno, detekt e lint limpos.
- **A troca (A5) foi o «Iniciar treino vazio», que abria o treino a decorrer** — o defeito que
  a 2.20.0 diz ter fechado no ▶ das rotinas e deixou neste botão, e que o CHANGELOG dela nega
  ao escrever «oferece retomá-lo e mais nada». Saiu «recordes com data», que é do ecrã da
  2.25.0.
- **A estrela é calculada e não guardada, contra o esboço 10 e por decisão do dono.** Um
  recorde guardado volta a poder discordar das séries — o defeito que a 2.21.0 desfez.
- **O `%d min` sem conversão existia em quatro sítios e não num.** O botão de retomar dizia
  «2618 min» e o resumo «2715 min»; a linha nova do histórico nasceu com o mesmo defeito. O
  `workout_hub_minutes` deixou de existir e os quatro passam pelo `formatDurationMin`.
  **E eram cinco:** o `workout_hub_last_duration` do cartão de destaque ficou, e o aparelho
  mostra «~4236 min last time» duas linhas acima de «70h 36m». A busca foi feita pelo nome da
  string e não pelo formato.
- **Um comentário meu prometeu o que o código não precisava de fazer**, e só apareceu ao
  tentar ver o teste vermelho: parti a conta dos recordes de propósito e **ela passou**,
  porque o `groupBy` já fazia o que a segunda passagem dizia fazer. Ver o teste a falhar não é
  cerimónia — é o que distingue um guarda de uma decoração.
- **A próxima do plano é a 2.25.0**, «As estatísticas do treino», com o esboço
  `10-treino-estatisticas` obrigatório — é o mesmo esboço desta versão, secção 2. Leva também
  «recordes com data», que saiu daqui por troca.
- *O que se segue é o estado até à 2.23.1.*
- **As seis versões que estavam por publicar saíram a 2026-09-03.** Estiveram fechadas e
  dentro de casa desde 09-02: a última etiqueta era a `v2.19.0` e a `main` do GitHub estava
  em `ad479ac`, seis commits atrás. **Nenhuma delas teve CI enquanto lá esteve**, que é a D4,
  e as seis foram feitas no mesmo dia sem nenhuma viver no telemóvel antes da seguinte, que é
  a A3 — isso não se desfaz, e fica escrito. Agora: `main` em `fc908b9` com o **CI verde**,
  seis etiquetas de `v2.20.0` a `v2.23.1`, e seis releases com os quatro APKs cada, mais o
  `catalogo.json` e o `manifesto.json`. Cada APK foi compilado **no seu próprio commit** e
  conferido com o `unzip`: um `catalogo.json` só. O `latest` é a `v2.23.1`, e o manifesto e o
  catálogo respondem lá com **HTTP 200** e o `sha256` a bater — que é a verificação que a app
  faz antes de trocar o catálogo.
- **2.23.1 é a última fechada**, a 2026-09-02, publicada a 09-03. Esquema **v39** e catálogo
  **v6**, inalterados. «As duas respostas que faltavam»: o horário passa a notificar, com
  interruptor e hora à escolha, desligado por omissão; e a supersérie mantém os dois
  exercícios abertos na sessão. 1728 testes Kotlin, 58 das ferramentas, 68 Deno, detekt e
  lint limpos, e o CI verde em `fc908b9`.
- **Nasceu de uma auditoria contra mim.** As perguntas de abertura da 2.22.0 e da 2.23.0 foram
  respondidas por mim na ausência do dono; ao voltar, foram-lhe refeitas as seis. **Quatro
  bateram e duas não** — o horário notificar e a supersérie —, e são estas duas.
- **E de outra falha, corrigida nos registos:** o esboço `07-treino-rotinas.html` não foi
  aberto durante a 2.23.0, e a revisão 2 dela foi feita contra o documento da área. A 2.21.0
  e a 2.22.0 tinham saído sem a lista «Abertos (C5)», e a 2.20.1 nomeava um documento que não
  foi lido. Está tudo escrito nos registos respectivos.
- *O que se segue é o estado até à 2.23.0.*
- **2.23.0**, a 2026-09-02. Esquema **v39** e catálogo **v6**, ambos
  inalterados. «As rotinas»: arrastar para reordenar, duplicar e mudar o nome, desfazer no
  mover, alvos com `−` e `+`, supersérie agrupada, e o peso alvo da rotina finalmente a
  servir para alguma coisa. 1724 testes Kotlin, 58 das ferramentas, 68 Deno, detekt e lint
  limpos.
- **O arrastar não se prova por `adb`.** Nenhuma das três formas de injectar o gesto o
  reproduz — o `swipe`, o `draganddrop` e uma sequência de `motionevent`. Ficou provado em
  **três testes de interface** que constroem o gesto passo a passo, um deles dentro de um
  `LazyColumn`, que é onde ele disputa o dedo com a lista que rola.
- **Uma premissa do estudo caiu:** a área 07 diz que «o centro de treino ignora o horário
  completamente». Deixou de ser verdade na 2.20.0.
- *O que se segue é o estado até à 2.22.0.*
- **2.22.0**, a 2026-09-02. Esquema da base **v38 → v39** — a tabela
  `exercise_load` e a coluna `workout_set.bodyweightKg`. Catálogo **v6**, inalterado.
  «Peso do corpo»: os **111 exercícios `body only`** do catálogo — flexões, dominadas,
  fundos, prancha — **deixam de ser impossíveis de registar**. O peso da pessoa entra como
  carga, com carga extra por cima e uma percentagem por exercício que é dela. 1714 testes
  Kotlin, 58 das ferramentas, 68 Deno, detekt e lint limpos. Verificada **actualizada por
  cima da 2.21.0 com dados lá dentro**.
- **É MENOR, e a razão está no desenho:** o `weightKg` de uma série continua a querer dizer
  a carga total. A coluna nova diz **quanto dela veio do corpo**, e nasce nula em toda a base
  — que é o que era verdade antes. Nenhuma série gravada muda. A `estudo/propostas/00-o-custo-de-mudar.md` avisava que
  isto daria *«dois significados de `weightKg` para sempre»*; daria, se ele passasse a guardar
  outra coisa.
- **A app não propõe a fração do peso.** Os «cerca de 65 % numa flexão» do `estudo/motor/05-exercicio-e-gasto.md` são uma
  aproximação sem fonte no repositório. A percentagem começa nos 100 %, mostra-se a conta, e
  quem a muda é quem faz o movimento.
- *O que se segue é o estado até à 2.21.0.*
- **2.21.0**, a 2026-09-02. Esquema da base **v37 → v38** — uma tabela
  nova, a `session_exercise_note`, por migração automática. Catálogo **v6**, inalterado.
  «A sessão: corrigir e cronometrar»: relógio do treino na barra, calculadora de discos,
  fluxo de teclado peso→reps→gravar, RPE fora da linha de registo, notas por exercício,
  1RM estimado à vista e recorde dito no momento. 1701 testes Kotlin, 58 das ferramentas,
  68 Deno, detekt e lint limpos. Verificada **actualizada por cima da 2.20.1 com dados lá
  dentro**, que é o que prova a migração.
- **Três das dez propostas da área 08 já estavam feitas** quando a medição C1 as foi ver —
  editar uma série, o `roundToInt` do pré-preenchimento e os índices repetidos. O plano
  descrevia-as como por fazer. Medir antes de abrir a versão foi o que evitou refazê-las.
- **As três perguntas de abertura foram respondidas por mim**, com o dono ausente e com essa
  delegação dada por ele: discos fixos que mudam com as unidades · RPE no «⋯» da série, sem
  interruptor · notas da sessão e não da rotina. As razões estão no registo da versão.
- *O que se segue é o estado até à 2.20.1.*
- **2.20.1**, a 2026-09-02. Esquema da base **v37**, catálogo **v6** —
  **nenhum dos dois mexeu**. «A barra»: passa a `Hoje · Diário · Treino · Progresso · Mais`. A
  corrida sai da navegação principal e vai viver dentro do treino, o progresso ganha
  separador próprio, e o «Eu» junta-se ao menu da app em «Mais». 1686 testes Kotlin, 58 das
  ferramentas, 68 Deno, detekt e lint limpos. Verificada a correr **com e sem corridas** — o
  segundo estado exigiu escrever na base por `run-as`, porque o emulador não tem GPS.
- **É a segunda metade da 2.20.0** (B2), e leva o terceiro número pelo precedente do
  `versionamento.md` — como a 2.5.1 e a 2.18.1. **MENOR pelo precedente da 2.1.0:** a corrida
  ganha entrada no treino no mesmo lançamento em que perde o separador. **Nenhuma das 51
  versões do plano se deslocou**, e a 2.21.0 continua a ser «a sessão».
- **A D7 apanhou o defeito que a própria versão criou:** a corrida, deixando de ser separador,
  ficou **sem saída visível** — a barra de baixo era a única maneira de sair dela e o
  `RunScreen` não tinha barra de topo. Ganhou título e seta, e ficou o `TodoEcraTemSaidaTest`,
  que exige um `popBackStack` ou um `popUpTo` no bloco de **todas** as rotas fora da barra.
- *O que se segue é o estado até à 2.20.0.*
- **2.20.0**, a 2026-09-02. Esquema da base **v37**, catálogo **v6** —
  **nenhum dos dois mexeu**. «O centro de treino»: o separador que não mostrava um único
  número passa a mostrar o treino de hoje, a semana, as rotinas com ▶ e os últimos treinos.
  **Nenhum dado é novo** — todos já eram calculados noutro sítio e não chegavam ali. 1680
  testes Kotlin, 58 das ferramentas, 68 Deno, detekt e lint limpos. Verificada a correr nos
  **três estados** do cartão de destaque, incluindo o do primeiro arranque, com `pm clear` e
  o arranque refeito.
- **A D7 nasceu nesta versão, e é a primeira a fechar com ela:** duas revisões separadas.
  A primeira apanhou **seis defeitos** que os 1677 testes verdes não viam — entre eles um
  comentário que prometia o que o ecrã não fazia, e um nome tocável com vinte dp de alvo. A
  segunda conferiu as seis propostas do esboço 06 uma a uma: quatro batem, uma corrigiu-se a
  favor do esboço, e **duas divergem de propósito** — o botão «Histórico» fica no menu, porque
  tirá-lo deixava o ecrã de histórico sem caminho nenhum.
- **A próxima do plano é a 2.24.0**, «O histórico do treino». As perguntas de abertura
  dela estão escritas no plano.
- *O que se segue é o estado até à 2.19.0, e fica por ser o que explica como se chegou aqui.*
- **2.19.0 foi publicada**, a 2026-08-30. Esquema da base **v37**, catálogo **v6**.
  «O exercício avulso»: duração escrevível com atalhos, recentes, corrigir um registo do
  diário, e a hora a que a atividade começou. Tag `v2.19.0`, commit `94f9505`, release com os
  quatro APKs mais o `catalogo.json` e o `manifesto.json`. 1668 testes Kotlin, 58 das
  ferramentas, 68 Deno, detekt e lint limpos. Verificada a correr **de instalação limpa da
  2.18.2 e actualizada por cima**. **A release seguinte tem de levar os dois ficheiros outra
  vez**: uma que não os traga apaga o caminho do botão «Procurar».
- **A medição C1 da 2.19.0 fez-se a 2026-08-30, e não estava feita antes.** Este ficheiro
  dizia que estava escrita no plano; **o cabeçalho `## 2.19.0` tinha desaparecido do plano** e
  o corpo da versão estava colado ao fim da 2.18.2, sem título. Reposto. **Três premissas
  caíram** — o id da atividade já era gravado, o «−5 sem mínimo» não existia, e o catálogo tem
  90 atividades e não ~150. E uma quarta: **a intensidade já está no catálogo**, dentro do
  nome, com cinco ritmos na corrida e cinco velocidades na bicicleta.
- **2.18.2 saiu antes dela**, no mesmo dia. Esquema da base **v36**, catálogo **v6**.
  Leva a auditoria com o estudo inteira — as três passagens — mais a origem por nutriente, a
  margem na ficha do alimento, a repartição da margem do dia, e a paleta. Tag `v2.18.2`,
  release com os quatro APKs mais o `catalogo.json` e o `manifesto.json`. Verificada a correr
  de **instalação limpa** com o APK de lançamento. **Muda números já gravados:** o viés por
  sexo da fita métrica.
- A **próxima do plano é a 2.20.0**, «O centro de treino», com o esboço `06-treino-centro`
  obrigatório. Entretanto entraram três acrescentos ao plano, com perguntas de abertura
  escritas: a **2.19.1** (o ruído do ecrã de exercício), a **2.49.0** (favoritos) e a
  **2.50.0** (importação de saúde em fundo).
- *O que se segue é o estado até à 2.18.1, e fica por ser o que explica como se chegou aqui.*
- **2.18.1 foi publicada** a 2026-08-28. Esquema da base **v35**, catálogo **v5**.
  **Treze das 51 versões do plano feitas** — a 2.18.1 é uma metade partida, e não uma vaga
  nova, como a 2.5.1. Contado a 2026-08-29, depois das duas passagens: **1641 testes Kotlin,
  54 das ferramentas, 68 Deno**, detekt e lint limpos, e o CI verde nos dois últimos commits
  de `main`.
- **A auditoria com o estudo está feita**, e foi em **duas passagens**. Nenhuma saiu ainda numa
  versão — o que mudou está por publicar: ver o CHANGELOG e os registos de releitura no plano.
  **Uma das correcções muda números já gravados** — o viés por sexo da fita métrica sobe 2,6
  pontos a massa gorda dos homens e desce 2,3 a das mulheres, e o basal acompanha.
  - **28 · a releitura**, documento a documento, contra o código.
  - **29 · os esboços ao lado do emulador**, ecrã a ecrã. Apanhou o que a leitura do código não
    apanhou, incluindo **um erro que era meu e do próprio dia**: fechei a secção «as tuas
    refeições» do ecrã de abertura julgando cumprir a área 03, quando o esboço a põe em
    primeiro lugar. As seis propostas da secção 1 do esboço 03 e o sítio próprio do esboço 05
    ficaram feitos, e verificados a correr.
- **O registo de publicação de sete versões estava em falta no plano**, da 2.7.0 à 2.18.1, e
  foi escrito a 2026-08-29 a partir do `gh release` e do `git` — não da memória. Duas coisas
  apareceram ao escrevê-lo: a **2.8.0 foi etiquetada com o CI vermelho** (um teste que comparava
  relógios, corrigido depois), e a **2.18.0 nunca foi um APK** — as duas metades saíram no
  mesmo lançamento.
- **A 2.18.0 partiu-se em duas** (regra B2): as refeições guardadas na 2.18.0, os passos de
  preparação na 2.18.1. A segunda metade fica no terceiro número, e a razão está escrita em
  [`versionamento.md`](versionamento.md) — inserir um MINOR a meio deslocava as cem
  referências cruzadas do plano.
- **Volta-se a fazer versão a versão**, por decisão do dono a 2026-08-27. O bloco D foi a
  excepção e não o modelo: as corridas existiram porque a cerimónia por versão era um terço
  do trabalho num bloco de doze, e o bloco E tem quatro.
- **O bloco D está fechado a sério.** A 2.7.0 levou-o quase todo e deixou seis promessas por
  cumprir; a 2.8.0 fecha-as. Nenhuma delas rebentava nada — davam números errados em
  silêncio, e só apareceram ao reler o plano promessa a promessa contra o código.
- A release levou os quatro APK **e** o `catalogo.json` e o `manifesto.json`, e o botão
  «Procurar» no aparelho passou a dizer **«está em dia»**. Antes dela dizia «não deu para
  chegar lá», porque o `latest` do GitHub apontava para a 2.6.0, que não os trazia — ver o
  passo 9 de [`lancar-uma-versao.md`](../guias/lancar-uma-versao.md). **A release seguinte tem
  de os levar outra vez**: uma que não os traga não mantém os anteriores, apaga o caminho.
- Os números do plano entre a 2.9.0 e a 2.15.0 ficam livres: o conteúdo deles saiu na 2.7.0 e
  na 2.8.0, ou não se lança de todo.
- A **2.5.1 foi fechada sem sair.** A razão está no plano e não se reabre sem a ler.

## O bloco D fez-se em três corridas

Por decisão do dono, a 2026-08-23. Uma corrida é um plano, uma execução e **um lançamento** —
em vez de uma versão de cada vez. A cerimónia por versão era perto de um terço do trabalho.

| Corrida | O quê | Estado |
|---|---|---|
| **1 · o encanamento** | 2.7.0, com a 2.8.0 absorvida | **feita e lançada** |
| **2 · as ferramentas** | 2.9.0 + 2.10.0 | **feita, e não se lança** |
| **3 · o conteúdo** | 2.11.0 a 2.15.0 | **feita, saiu na 2.7.0** |

**O bloco D saiu todo numa versão: a 2.7.0.** As três corridas foram feitas seguidas, e partir
o que já estava escrito em cinco lançamentos era cerimónia sem nada por baixo — a razão de as
corridas existirem.

O **conteúdo** que o plano tinha em 2.8.0 a 2.15.0 saiu aqui, ou não se lança de todo: o motor
de qualidade e a oficina são ferramentas do repositório, e o passo 1 do guia diz que essas não
se lançam. Os **números** ficam livres, e a 2.8.0 leva o que faltou. Saltar números não custa
nada: o `versionCode` deriva do nome e continua a crescer.

**A 2.7.0 não fechou o bloco.** Seis promessas do plano ficaram por cumprir e nenhuma delas
rebentava nada — davam números errados em silêncio, que é o modo de falhar deste bloco. Só
apareceram ao reler o plano promessa a promessa contra o código, e é isso que a 2.8.0 fecha.

**O que não se junta é o conteúdo com o código.** As decisões sobre nomes, fusões e porções
são do dono; tomá-las em lote sem ele as ver é o modo de falhar deste bloco — um alimento com
o nome trocado não rebenta, não dá erro, e custa uma versão a corrigir.

**O que não se batcha nunca:** a medição no início de cada peça. Cinco versões abertas, cinco
premissas do plano erradas, e as cinco apareceram a medir — não a ler.

## O que a corrida no aparelho mostrou

**Da 2.20.0, o centro de treino** (emulador Android 16, x86_64). **Quatro defeitos, e nenhum
dos 1677 testes via um único** — dois eram plurais, um era uma cor, e o quarto era um botão
que fazia outra coisa:

1. **«1 DAYS AGO».** O cartão contava dias e não tinha plural. O contador desapareceu em vez
   de ganhar um plural: passou a levar o dia, que o formatador escreve sem plural nenhum —
   «sáb, 30 ago». *(Esta linha dizia que o formatador «já diz ontem». Não diz: o
   `dayShortDated` dá sempre o dia da semana com a data. O registo da 2.20.0 no plano foi
   corrigido a 2026-09-02 e esta cópia ficou para trás.)*
2. **Os sete quadrados da semana eram invisíveis** — pintados com o `surfaceVariant`, que
   desde a paleta da 2.18.2 é a própria cor do cartão. **Também no relatório do treinador**,
   desde essa versão. **Nenhum teste vê cor.**
3. **«1 sets».** O mesmo defeito de plural, no meu código, com a app já a ter plurais.
4. **Com um treino a decorrer, o ecrã oferecia «Começar».** O `startOrResume` devolve a
   sessão aberta e ignora a rotina que se lhe pede — o botão dizia «Começar Full Body A» e
   levava ao treino que já corria. É a família de defeito da 2.17.0, e ficou como teste-guarda
   de interface.

**O que a corrida confirmou:** o ▶ a abrir a rotina certa num toque · os três estados do
cartão de destaque, o terceiro verificado de instalação limpa · a semana ISO a dizer «0» na
segunda-feira seguinte a um treino de domingo · «Retomar o treino · 1 min» · e os ▶ a
desaparecerem enquanto há treino aberto.

**Da 2.19.0, o exercício avulso** (emulador Android 16, x86_64), em **duas passagens: a 100 %
de escala de letra e a 200 %**, que é a definição que o `estudo/transversal/03` §3.1 nomeia e
que nada testava. **Quatro defeitos, e nenhum dos 1667 testes via um único** — porque nenhum
deles é um número errado: são larguras.

1. **A 100 %: «Set the time» ficava numa coluna de uma letra por linha.** Com a hora posta, o
   diálogo punha três textos na mesma linha — a hora, «Remove the time» e «Set the time» — e o
   terceiro era espremido até se ler na vertical. Só em inglês, e só depois de pôr a hora.
2. **A 200 %: o `+` da duração saía do ecrã.** Rótulo, `−`, campo de 132 dp e `+` numa linha
   só. O documento diz exactamente isto do `NewSetRow`, e eu tinha acabado de escrever a mesma
   forma.
3. **A 200 %: o atalho «60» partia-se em dois algarismos**, um por linha.
4. **A 200 %: «Set the time» voltava à vertical**, com a correcção 1 já feita.

**A correcção tira as larguras fixas em vez de as afinar**: rótulo em linha própria, campo com
`weight(1f)`, e os atalhos e os botões da hora em `FlowRow` — que já era vocabulário da app.
Verificado outra vez nas duas escalas. O `EditLogDialog` da comida, que tinha a mesma forma e
os mesmos dois rótulos ingleses, foi corrigido a seguir por decisão do dono.

**E uma proposta do estudo caiu ao ser medida.** O `estudo/transversal/03-acessibilidade.md` pede um guarda de
Robolectric com `fontScale`. Escreveu-se, e ele **passa também sobre o código partido** — o
Robolectric mede o texto «15» a três pixels, e sem fontes a sério nada transborda. No lugar
dele ficou a exigência da razão escrita ao lado de cada largura fixa, no molde do
`contentDescription = null`: visto a falhar sobre os oito sítios que havia, **cinco deles o
`NewSetRow` que o estudo nomeia**. Medir a sério pede um teste instrumentado, e a app não tem
`androidTest` nenhum.

**O que a corrida confirmou:** a migração **v36 → v37 com o dia intacto**, feita a instalar a
2.18.2 de lançamento, registar 30 min · 400 kcal, e actualizar por cima — o registo ficou lá e
**sem hora**, que é o que ele honestamente é · o campo escrevível, com **22 minutos a dar 293
kcal**, que é o treino que a área 13 diz não se conseguir registar · os atalhos, com o 45 a
levar 400 kcal a 600 · a secção «Recent» à cabeça da lista · o relógio a abrir na hora a que
se está · a linha a dizer **«45 min · 600 kcal · 07:25»**, com o orçamento a subir de 2963
para 3163 · o treino de força a chegar ao diário como **«Full Body A · 2 min · 11 kcal ·
07:31»**, com a hora do **início** da sessão · e a linha do treino a **não** abrir ao toque.

**O que a corrida não conseguiu provar:** a hora de uma corrida com GPS e a de uma sessão
importada da Health Connect — o emulador não tem percurso nem outra app a publicar sessões. As
duas escrevem-se na mesma linha que o treino de força escreve, e essa está vista a funcionar.

**E mostrou dois defeitos que não são desta versão**, escritos no plano por baixo da 2.19.0: a
caixa de notificações do sistema à entrada da sessão de treino, e o `EditLogDialog` da comida,
que tem a mesma forma que aqui se corrigiu — em inglês, com os mesmos dois rótulos.

### Da releitura com o estudo, na 2.18.2

Emulador Android 16, x86_64, instalação limpa. Foi a corrida que mais rendeu até hoje:
**quatro defeitos, e nenhum deles visto por 1 638 testes.**

1. **Uma string mostrava a barra invertida no ecrã.** O estado vazio da pesquisa dizia, em
   inglês, «This is where you\'ll see what you eat most». O Compose Resources **não desfaz o
   escape do XML do Android** — a barra vai para o ecrã como qualquer outro carácter. As
   outras trezentas strings inglesas escrevem o apóstrofo nu; as duas que traziam a barra
   eram as duas que eu tinha acabado de escrever. Corrigido, com teste-guarda gémeo do que já
   existia para o `%%`.
2. **A lista das refeições tinha uma linha sem nome.** Uma receita nasce no instante em que
   se abre a folha de ingredientes ou a de passos — eles precisam de um pai onde se agarrar —
   e quem recua sem escrever nada deixa-a na base. Com a lista só, ela passou a ser uma linha
   em branco com uma seta. Passa a dizer «Receita sem nome», que é o que a torna apagável:
   apagar uma receita passa por abri-la.
3. **A colher de sopa continuava a aparecer com uma porção nomeada.** A correcção da área 03
   escondia-a quando havia `porcoesExtra` — e as porções nomeadas vêm de **dois** sítios. O
   «Arroz carreteiro» mostrava «porção (300 g)» e a colher ao lado.
4. **O desfazer de tirar um item de uma refeição guardada não existia.** A folha é uma janela
   por cima do andaime, e o aviso de anular desenha-se no andaime: no aparelho ele não
   aparecia sequer na árvore de acessibilidade. **Construí um desfazer que ninguém alcançava.**
   Passa a ser uma linha dentro da própria folha — «X saiu da refeição · Anular» —, que de
   caminho não tem a corrida contra os quatro segundos.

**O que a corrida confirmou a funcionar:** a lista só, ordenada por nome e com a origem
escrita na linha, exactamente como o esboço a desenha · os chips ×0,5 ×1 ×1,5 ×2, com 525
kcal a passar a 263 e a 1050, e as gramas a acompanhar · mudar o nome, que chega à lista no
mesmo instante · tirar um item e voltar atrás · o botão flutuante a dizer «Nova receita» no
separador das refeições · o aviso «este valor é estimado» com o caminho para corrigir · a
linha do que a cópia **não** leva, com os sessenta dias tirados da constante · a margem da
Mifflin no «mostra-me as contas» — «1986 kcal, com 199 para cada lado» · e o interruptor das
metas adaptativas a mudar de texto quando se desliga.

**O que a corrida não conseguiu provar:** nada de novo. O aviso de anular do resto da app
continua a não se conseguir tocar por `adb` — ver as armadilhas.

### As corridas anteriores

**Da 2.18.0 e da 2.18.1** (emulador Android 16, x86_64):

1. **A migração v34 → v35 passa com o dia intacto.** Instalou-se a 2.17.0 de lançamento,
   fez-se o arranque, registaram-se 640 kcal, e actualizou-se por cima: o registo ficou lá
   com a hora.
2. **O atalho do diário abre direto no separador das refeições**, e a linha diz «1 item ·
   640 kcal · Breakfast» — antes dizia só o nome e «Breakfast».
3. **A pré-visualização abre com o multiplicador.** A meio, 640 kcal passam a 320 e o item
   passa de 100 g para 50 g à vista, antes de se gravar seja o que for.
4. **O menu de uma refeição vazia mostra só o que faz sentido:** «aplicar» e «copiar de
   outro dia». Guardar, mover e limpar continuam escondidos sem registos.
5. **«Saved meal» aparece na folha de adicionar**, com ícone próprio.

**O que a corrida não conseguiu provar, e porquê.** O toque no «Anular» do aviso — nem o
desta versão nem o que existe desde a 2.3.0. O aviso dura **quatro segundos**, e conduzir o
emulador por coordenada fixa não serve num ecrã que muda de altura a cada registo; conduzir
pela árvore de acessibilidade serve, mas cada leitura da árvore custa perto de dois segundos
e o aviso morre antes do toque chegar.

**A comparação é que fecha a questão:** apagou-se um registo pelo caminho antigo, com o mesmo
método, e o «Anular» falhou exactamente do mesmo modo. Um mecanismo que está publicado há
quinze versões não se partiu esta tarde — o que se partiu foi a forma de lhe tocar.

O que fica a guardar o desfazer: o `AplicarRefeicaoGuardadaTest`, que prova que ele apaga
**exactamente** os registos criados e deixa em paz os que já lá estavam, visto a falhar com o
código estragado de propósito.

**Da 2.17.0** (emulador Android 16, x86_64). Foi a corrida que mais rendeu até hoje: apanhou
**dois defeitos que nenhum dos 1575 testes via**, e os dois pela mesma razão — não mudam
estado nenhum, só não fazem nada.

1. **A migração v33 → v34 passa com o dia intacto.** Instalou-se primeiro um APK com o esquema
   revertido para v33, registou-se, e actualizou-se por cima: as 899 kcal do azeite ficaram lá,
   com a hora e tudo.
2. **A folha da AI já não mostra o aviso legal no ecrã de entrada** — mostra-o na revisão, em
   frente à lista. Vê-se nas duas capturas.
3. **A revisão inteira funciona**: campo de gramas escrevível, trocar, acrescentar, guardar
   como refeição, e o registo cai no diário com as gramas certas.
4. **A troca liga ao catálogo**: «fatia de pão, 76 kcal» passou a «Arroz branco cozido, 117
   kcal» com a porção do alimento, e o total foi de 225 para 266.
5. **A [`LinhaDaLista`](../../composeApp/src/commonMain/kotlin/pt/antares/app/core/designsystem/components/LinhaDaLista.kt)
   engolia o `onClick` quando `emCartao = false`.** A lista que troca um item não respondia a
   toque nenhum. O parâmetro era aceite e deitado fora ao desenhar — nada dava erro, nada
   mudava de aspecto. Corrigido, com um teste de interface.
6. **O campo de gramas aceitava letras.** O teclado do campo é o dos números, mas um teclado de
   hardware escreve o que quiser, e a linha ficou a dizer «Ovos e» onde devia dizer gramas.
   Agora só entram algarismos e um separador.

**O que a corrida não provou:** a fotografia do prato de ponta a ponta. O emulador só tem uma
cena sintética, e uma análise dela responde «não é comida» — nunca chega a haver o que gravar.
O que a guarda é: um teste do ViewModel (uma foto, um ficheiro, o mesmo caminho em todos os
registos), o `FotosDeRefeicaoTest` com ficheiros a sério, e o facto de a miniatura usar o mesmo
`AsyncImage(model = caminho)` que as fotos de progresso já usam desde a 2.2.0.

**Da 2.16.0** (emulador Android 16, x86_64): a migração v32 → v33 passa com o dia intacto, os
três separadores aparecem — Procurar · Meus · Refeições —, e a fatia de 30 g está na linha do
pão de forma. A porção só aparece nos alimentos que têm uma, que é um em cada quatro.

**Da 2.8.0:** o leite abre a dizer 97 ml para 100 g, que é 100 ÷ 1,03. E foi aqui que se
apanhou o que 1533 testes não apanharam — a densidade estava no catálogo, na base e na
conversão, e não fazia nada, porque a versão do catálogo tinha ficado em 4. Quem actualizasse
ficava com o catálogo antigo.

**Da 2.7.0**, que é a corrida que fixou o que se verifica:

1. **A actualização a partir da 2.6.0 passa.** A migração salta três versões de esquema, da
   v28 para a v30, e o dia registado na 2.6.0 — 1 050 kcal em dois registos — aparece igual
   depois de instalar por cima. Nada de re-onboarding, nada no `logcat`.
2. **A instalação limpa passa**, com o catálogo v4 semeado do APK.
3. **O cartão «e se for cozinhado?» aparece** onde tem de aparecer: em «Frango, carne, cru»,
   109 kcal passam a 139 no grelhado, com «100 g cru dão 79 g» e o campo para quem pesou
   depois de cozinhar. Metade do catálogo não o tem, e é de propósito.
4. **O botão «Procurar» dizia «não deu para chegar lá»** — e estava certo: a release mais
   recente era então a 2.6.0, que **não** trazia o `manifesto.json`, e o endereço respondia
   404, confirmado à mão. Depois de publicar a 2.7.0 com os dois ficheiros, o mesmo botão
   passou a dizer **«está em dia»**. É o sintoma do passo 9 do guia, visto dos dois lados.

E mostrou dois defeitos, corrigidos aqui: o ecrã de boas-vindas e o de atribuições prometiam
**1376** alimentos do INSA quando o catálogo tem **1372** — quatro foram fundidos noutra
medição —, e a linha das origens em [`dados-e-licencas.md`](dados-e-licencas.md) ainda contava
3401/2937/1376/284. O `NumerosDoCatalogoTest` não via nenhum dos dois: só olhava para linhas
que dizem CIQUAL, USDA ou INSA por extenso, e não para as que contam pelo identificador nem
para os textos da app. Agora vê, e o Gradle já não dá a tarefa por actualizada quando **só**
o documento muda.

## O estado do catálogo

**7 932 alimentos.** As decisões de conteúdo do bloco D foram tomadas e estão escritas com a
razão de cada uma em [`arbitragem.mjs`](../../tools/catalogo/arbitragem.mjs).

| | |
|---|---|
| colisões de nome | **zero** — 97 fusões e 24 nomes desambiguados |
| contradições | **zero** — as três regras da coerência corrigem-nas no oleoduto |
| suspeitas na fila | **90** — 28 de Atwater, 60 fora de escala, 2 discordâncias |
| nomes em português | **2 131**, de 1 362 segmentos de vocabulário |
| ainda em inglês | **2 909** |
| com porção | 2 090 |
| com família de confeção | 4 153 |
| com água declarada | 7 019 |
| líquidos | **773**, dos quais 696 com densidade medida |
| na fila dos líquidos | 55 — parecem líquidos e ninguém decidiu ainda |

**A regra de arbitragem, em três degraus.** Uma medição ganha a uma estimativa escrita à mão.
Duas medições que concordam resolvem-se a favor da portuguesa — a TCA mediu produto daqui.
Duas que discordam desempatam-se por uma terceira, e a USDA está cá e é independente das
outras duas.

**Nem toda a colisão é um duplicado.** Vinte e quatro eram dois alimentos diferentes com o
mesmo nome: a batata assada da CIQUAL tem 0,1 g de gordura e a da TCA tem 4,8. Aí fundir era
apagar comida, e o que se corrige é o nome.

## O que falta, e é trabalho de meses

- **2 909 alimentos ainda em inglês.** O separador «segmentos» da oficina mostra o que falta
  por quantos alimentos cada um desbloqueia — traduzir um segmento arruma todos os que
  esperam por ele. Medido: 1 362 segmentos dão 42 % dos nomes; para 65 % são precisos cerca
  de 2 500, e para 84 % cerca de 3 500.
- **90 suspeitas.** O que resta do Atwater são as fontes que usam factores específicos por
  alimento — o cacau, os frutos secos, as leguminosas — e isso é legítimo. O que resta de
  «fora de escala» são outliers verdadeiros: o álcool puro é mesmo o mais calórico do grupo
  das bebidas.
- **Cada lote de vocabulário traz outra leva de duplicados à superfície**, e isso não é a
  tradução a criar problemas: é o inventário a ficar legível. Foram seis voltas de arbitragem
  até aqui, e a seguinte virá com o lote seguinte.
- **55 candidatos a líquido**, em `tools/catalogo/liquidos-por-decidir.json`. Quase todos
  óleos, e não se marcam sozinhos: as três regras que tentei marcaram comida sólida — «Olive,
  black, in oil», «Milk chocolate, bar». O nome inglês é a armadilha.

## As ferramentas da corrida 2

Duas, e nenhuma delas aparece na app. Estão documentadas em
[`tools/README.md`](../../tools/README.md); em resumo:

- **O motor de qualidade** corre dentro do `construir.mjs`. Cinco verificações, todas a
  comparar o alimento consigo mesmo. Uma **contradição** — um número impossível — chumba a
  construção a menos que esteja declarada em `qualidade.json`. Uma **suspeita** não chumba
  nada e vai para a fila. Hoje: 12 contradições e 252 suspeitas em 8 011 alimentos.
- **A oficina** é uma página local (`node tools/oficina/servidor.mjs`). Escreve em
  `correcoes.json`, e a fila vem ordenada por quantas vezes cada alimento foi registado — o
  histórico sai do telemóvel e **não entra no git**.

- **O vocabulário dos nomes** (`tools/vocabulario/`) traduz segmento a segmento, com
  concordância: o género e o número vêm da base e os qualificadores seguem-na. Um nome só é
  aplicado quando fica **inteiro** — meio traduzido parece um defeito.

## As regras


[`regras.md`](regras.md) é a fonte única. São 31, em grupos A a F, e cada uma diz quem a
verifica. As que mais mandam no dia a dia:

| | |
|---|---|
| **A1** | nada começa sem o dono mandar, versão a versão. O plano não é autorização. |
| **A2** | cada versão abre com perguntas. Estão pré-escritas no plano — mas **depois** de abrir os documentos do estudo: metade delas já lá está respondida. |
| **C5** | abrir os documentos da área antes de lhe mexer, e **nomeá-los no registo da versão**. |
| **A5** | nada entra a meio sem sair outra coisa. |
| **B2** | versão que cresce a meio parte-se. Já aconteceu uma vez, e a excepção de numeração está em [`versionamento.md`](versionamento.md). |
| **C1** | confirmar o achado no código antes de lhe tocar. Já apanhou seis achados errados, quatro deles vindos do próprio estudo. |
| **C3** | nunca escrever um número que não se contou. Já se escreveu uma versão inteira à volta de um número que era um `sleep`. |
| **D1** | o que conta é o relatório de testes, não a última linha do Gradle. |
| **D3** | correr a app, não só os testes. |
| **D6** | varrer **antes** de publicar, não depois. |
| **D7** | **duas revisões por versão, e separadas**: uma a caçar defeitos, outra a conferir com o estudo e com o esboço. Lendo os ficheiros, não a memória deles. |

**Nomes de classes e ficheiros são substantivos, não frases.** Regra do dono, dada depois de
um ecrã se ter chamado `OQueSaiDaquiScreen`. Português nos nomes é aceite; frases não.

## A ordem de uma versão

1. **Abrir o estudo da área** — a rota está em [`a-divida-com-o-estudo.md`](a-divida-com-o-estudo.md).
   O esboço, quando existe, é quem decide a forma.
2. **Medir as premissas do plano contra o código** (C1). Em todas as versões deste ano houve
   pelo menos uma errada, e mede-se — não se lê.
3. **Perguntar** (A2), já com o estudo e a medição na mão.
4. Construir, com testes que se vêem a falhar antes de se aceitarem.
5. Correr no aparelho (D3).
6. Fechar os documentos, **nomeando no registo da versão os ficheiros do estudo que se
   abriram**. Um registo que não os nomeia é um registo de uma versão feita sem eles.

## Verificar

```bash
node tools/verificar.mjs
```

Uma chamada: testes Kotlin com os nomes dos que falham, detekt, lint, os testes das
ferramentas, as funções do servidor e as duas buscas de segredos. Seis linhas e um veredicto.
Apaga os relatórios antigos antes de correr — sem isso, uma execução que nem arranca deixa a
anterior a passar por verdade.

## O catálogo

Construído fora da app:

```bash
node tools/catalogo/construir.mjs
```

Determinístico, e **chumba** em duas situações: se perder um alimento que a fonte declare e
não esteja em `desvios.json`, e se aparecer uma **contradição** nova — um número impossível —
que não esteja em `qualidade.json`. Nos dois casos há uma opção para aceitar e ler o `git
diff` (`--aceitar-desvios`, `--aceitar-qualidade`).

O que **não** chumba são as suspeitas: energia que não bate com os macros, alimento fora de
escala no seu grupo, duas fontes a discordarem. Essas vão para a fila da oficina. A razão de
não chumbarem é que medem uma discordância entre métodos de medição, e chumbar por isso era
não poder publicar até a ANSES corrigir a tabela dela.

A versão sobe à mão em **dois** sítios — no `construir.mjs` e no `FoodSeeder` — e há um
teste-guarda a exigir que sejam a mesma. Ver [`tools/README.md`](../../tools/README.md).

## Armadilhas já pagas

- **Os heredocs desta consola comem um nível de barras invertidas**, mesmo com o delimitador
  entre plicas. Já partiram uma expressão regular, um `replace` e um ficheiro inteiro.
  Ficheiros com barras escrevem-se com a ferramenta de escrita.
- **Os ficheiros estão em `CRLF`.** Uma substituição de texto com `\n` não encontra nada, e um
  teste que procure `"…\n"` acusa quem mexeu no ficheiro em vez de quem partiu a regra.
- **O `/tmp` do node e o do Bash não são o mesmo sítio.** O `fs.writeFileSync("/tmp/x")` do
  node escreve em `C:	mp`; o `cp /tmp/x` do Git Bash lê o temporário do MSYS. Guardei uma
  cópia de segurança de um ficheiro com um script de node e restaurei-a com o `cp` — e o que
  voltou foi a versão do `git`, sem as duas correcções que estavam por comitar. **Para
  guardar e repor um ficheiro usa-se o `git`**, que sabe o que lá estava.
- **Não reescrever Kotlin com expressões regulares.** Três tentativas, duas produziram código
  partido.
- **`MSYS_NO_PATHCONV=1` em todo o comando `adb` que toque em `/sdcard`.** Sem isso o Git Bash
  reescreve o caminho e lêem-se ficheiros velhos sem dar por isso.
- **O aviso de anular não se toca pelo `adb`.** Dura quatro segundos; uma coordenada fixa
  falha num ecrã que muda de altura, e ler a árvore de acessibilidade para achar o botão
  custa perto de dois segundos. Provado na 2.18.1 contra o desfazer que existe desde a 2.3.0:
  falha igual. **O desfazer verifica-se em teste**, e no aparelho verifica-se só que o aviso
  aparece com o botão lá.
- **`adb uninstall` não garante que os dados desaparecem.** Estado limpo é `pm clear`, mais
  apagar `/sdcard/Documents/Antares` à mão.
- **Ficheiros apagados continuam dentro do APK.** A pasta intermédia de recursos do Gradle não
  se limpa com `--rerun-tasks`. Ver o passo 7 de [`lancar-uma-versao.md`](../guias/lancar-uma-versao.md).
- **Verificar sempre a partir de uma instalação limpa do APK de lançamento.** Foi aí, e só aí,
  que apareceram dois dos três defeitos da 2.1.0.
- **O CI não tem a pasta `estudo/`, e há um teste que a atravessa.** O
  `DocumentationHonestyTest` verifica que a documentação não cita ficheiros que não existem, e
  o estudo está fora do git: as citações dele estão na lista de excepções. **Uma ligação
  relativa — `](../../estudo/…)` — escapava a essa lista**, porque a comparação era feita
  contra o texto citado e não contra o caminho resolvido. Resultado: passava na máquina onde o
  estudo existe e falhava no CI, onde não existe. Para simular o CI: mover a pasta `estudo`
  para fora, correr o teste, e repô-la.
- **O motor SQLite empacotado não carrega na máquina virtual dos testes.** Uma pergunta sobre
  o comportamento do Room com o motor definido não se responde em teste unitário nesta
  máquina — responde-se no emulador.

## Quem faz o quê

O que decide **o que é verdade sobre um alimento**, ou **o que a app faz quando não sabe**,
fica no modelo mais capaz, com esforço alto: a abertura de cada versão, a medição C1, e as
versões de conteúdo. A execução mecânica — renomear em N sítios, converter testes, escrever
DAOs — vai para um modelo mais barato, com a instrução já escrita.

A razão é o modo de falhar: **um erro de encanamento rebenta num teste; um erro de conteúdo
fica calado e vai para a Play Store.**

## O plano e o relatório

O plano vive na pasta **estudo**, fora do git por decisão do dono — o registo
durável é o `git log`. As respostas às perguntas de abertura ficam escritas por baixo de cada
versão, e o que se aprendeu a construir fica por baixo dessas.

O relatório vivo republica-se ao fim de cada versão, com os números, o texto **e** as barras.
