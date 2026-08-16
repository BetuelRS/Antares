# Changelog

Todas as alterações com significado para quem usa a Antares.

O formato segue o [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) e a numeração
segue o [Semantic Versioning 2.0.0](https://semver.org/lang/pt-BR/). As regras de quando sobe
cada número estão em [docs/referencia/versionamento.md](docs/referencia/versionamento.md).

Cada versão que mexeu na base de dados diz qual é a versão do esquema, porque é isso que decide
se uma atualização é indolor. Os esquemas estão em `composeApp/schemas/`.

---

## [Unreleased]

Nada que mude a app para quem a usa: uma correção no gerador de dados de demonstração, que
só o dono alcança, e duas no CI, que não vai dentro do APK. Por isso não sobem número.

### Fixed

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
