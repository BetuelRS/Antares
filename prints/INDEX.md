# Capturas de ecrã

Um percurso pela app inteira, do primeiro arranque ao ecrã mais escondido. **Todas as imagens
foram tiradas a 2026-08-12**, numa só sessão, num emulador `Medium_Phone`, sobre a versão que hoje
se chama **1.0.0** (compilada nessa altura como `0.9.18.1`).

Este índice existe por um motivo: uma pasta de imagens sem legenda é a mesma armadilha dos
documentos antigos, mas em imagem. Quem abre uma captura de há meses não tem como saber se está a
ver a app ou um defeito já corrigido.

## Como ler a numeração

O percurso tem duas passagens pela app:

- **01 a 79** — a app **vazia**, acabada de instalar. É o que uma pessoa vê no primeiro dia.
- **81 a 108** — a mesma app **cheia**, depois de o modo de demonstração gerar dois anos de
  registos de uma pessoa inventada. Os ecrãs que só fazem sentido com histórico — gráficos,
  recordes, tendências — só existem nesta segunda passagem.

O `80` e o `81` são a fronteira: o desbloqueio do administrador e a geração dos dados.

## O percurso

| Ficheiros | O que mostram |
|---|---|
| `01-onboarding-*` | as nove perguntas iniciais, incluindo o teclado numérico (`04b`) |
| `02` a `07` | a app vazia: Hoje, perfil, histórico de peso, menu, definições, admin |
| `08` a `18` | o diário: vazio, pesquisa, detalhe de alimento, ficha nutricional, água, exercício |
| `19` a `31` | treino: hub, biblioteca, rotinas, plano semanal, sessão a decorrer, séries, resumo |
| `32` a `39` | corrida: hub, aviso de bateria, ao vivo, com GPS, resumo |
| `40` a `45` | «rico em», jejum a decorrer e histórico |
| `46` a `57` | perfil e saúde: registar peso, «mostrar contas», composição corporal passo a passo |
| `58` a `62` | editar perfil, exportar dados, estatísticas de nutrição |
| `63` a `66` | relatório semanal: histórico e detalhe |
| `67` a `71` | backup, Health Connect, atribuições, sobre, último erro |
| `72` a `79` | fotos de progresso, criar alimento, receitas |
| `80`, `81` | o seletor de nascimento depois de corrigido; o admin desbloqueado |
| `82` a `93` | com dados: 11 432 registos, Hoje cheio, históricos, recordes, detalhe de corrida |
| `94` a `99` | relatório semanal com números a sério, diário cheio |
| `100` a `107` | jejum, estatísticas de nutrição com micronutrientes |

## O que foi apagado, e porquê

**Dois ficheiros, a 2026-08-14**, os únicos em que havia prova e não palpite:

- `15-adicionar-exercicio.png` era **byte a byte igual** a `13-diario-com-registo.png`. Estava mal
  etiquetado: mostrava o diário, e não o ecrã que o nome anuncia. O ecrã verdadeiro é o `17`.
- `26b-sessao-treino.png` era cópia exata do `26`.

**Mais um, a 2026-08-15**: o `108-conquistas.png` mostrava um ecrã que a app deixou de ter.

**Mais seis**, que estavam no repositório e já não estavam em disco: quatro imagens de WhatsApp e
um `arro.jpeg`, de 2026-07-10, tiradas num telemóvel a sério. Não eram do percurso — eram relatos
de defeito, e os defeitos foram corrigidos entretanto. Mostravam:

- o seletor de nascimento a abrir em 2080–2100 (hoje: `80-onboarding-calendario-corrigido.png`);
- a pesquisa a listar códigos de barras e produtos sem relação como se fossem alimentos — hoje
  filtrado, e o `OffSearchOnlineTest` impede a regressão;
- nomes de exercícios mal traduzidos, do género «na Maquina Supino» — hoje «Supino com Barra»,
  como se vê em `25-editar-rotina.png`.

## O que este índice não afirma

As descrições acima saem dos nomes dos ficheiros e da ordem do percurso. Foram **abertas e
confirmadas uma a uma** apenas estas: `25`, `102` e `106`, mais as seis imagens apagadas. As
restantes não foram verificadas imagem a imagem.

Há pares com o mesmo nome e conteúdos diferentes — `60`/`61`, `63`/`64`, `72`/`73`, `75`/`76`/`77`,
`91`/`92`, `100`/`101` — que são estados sucessivos do mesmo ecrã na mesma sessão. Não se apagou
nenhum: distinguir «passo intermédio» de «defeito depois corrigido» exigia ter estado lá, e
apagar por palpite é pior do que deixar ficar.

Um caso concreto de nome enganador: `102-estatisticas-nutricao-semana.png` mostra a semana sem
dados e o `106-nutricao-semana-corrigida.png` mostra-a cheia. A diferença entre as duas é haver
registos, e não uma correção.
