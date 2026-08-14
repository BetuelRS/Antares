# Changelog

Todas as alterações com significado para quem usa a Antares.

O formato segue o [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) e a numeração
segue o [Semantic Versioning 2.0.0](https://semver.org/lang/pt-BR/). As regras de quando sobe
cada número estão em [docs/referencia/versionamento.md](docs/referencia/versionamento.md).

Cada versão que mexeu na base de dados diz qual é a versão do esquema, porque é isso que decide
se uma atualização é indolor. Os esquemas estão em `composeApp/schemas/`.

---

## [Unreleased]

### Added

- Documentação escrita de raiz: `README.md` e `docs/`.
- 69 testes novos, sobre as definições do perfil, a composição corporal, a montagem da sessão de
  treino, o registo de receitas, a pesquisa de alimentos e o aviso de mudança de cálculo.

### Changed

- A numeração passou a ser SemVer estrito, e o `versionCode` deriva do nome em vez de ser
  contado à mão.
- O código passou a estar comentado de ponta a ponta.

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
