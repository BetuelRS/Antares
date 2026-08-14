# 0005 — A documentação é verificada por testes

**Estado:** Aceite · **Desde:** 1.0.0

## Contexto

A 7 de agosto de 2026 este repositório foi **esvaziado de propósito**: todos os documentos, todos
os comentários e todo o histórico do git foram apagados de uma vez.

A razão não foi arrumação. Os documentos acumulados descreviam funcionalidades que já tinham sido
removidas — a conta, a sincronização, o relatório gerado por modelo — e continuavam a lê-los como
se fossem verdade. Quem chegava ao projeto e os lia ficava a **descrever a app errada com toda a
confiança**, e a agir em conformidade.

Um documento errado é pior do que documento nenhum. Sem documentação, uma pessoa lê o código. Com
documentação errada, não lê.

A reconstrução partiu de um princípio: **o código é a única fonte que não mente.** Tudo o resto
tem de provar que ainda é verdade.

## Decisão

**Uma afirmação sobre o projeto que possa apodrecer em silêncio tem de ter um teste a cobrá-la.**

Isso tem três formas concretas.

**1. Os testes-guarda.** Cada decisão estruturante tem um teste que falha quando ela é desfeita:
o `NoSyncTest` para a sincronização, o `TombstoneCollisionTest` para as lápides, o
`AdaptiveTargetsOfflineTest` para o funcionamento sem rede. A lista está em
[Testes-guarda](../../referencia/testes-guarda.md).

**2. O `DocumentationHonestyTest`.** Lê os ficheiros `.md` e falha se um documento citar um
caminho que não existe, ou se as versões do `CHANGELOG.md`, do `README.md` e do `AppChangelog`
deixarem de coincidir com o `versionName`.

**3. A convenção dos comentários.** Um comentário acrescenta um facto que não está na linha: uma
unidade, um limite, um efeito colateral, a razão de uma escolha. Não repete o que o nome já diz —
esse apodrece na primeira renomeação. Pode citar um teste pelo nome; não cita caminhos nem
números de linha.

## Consequências

**Bom:**

- Um caminho entre crases num documento é uma promessa, e há um teste a cobrá-la.
- Mudar o nome de um ficheiro parte a documentação **na compilação**, e não meses depois, na
  cabeça de quem lê.

**O limite, que é preciso dizer alto:**

**Isto verifica referências, não afirmações.** Um documento pode citar ficheiros que existem e
mesmo assim mentir sobre o que eles fazem.

Aconteceu, um dia depois de a documentação ser escrita: a página da privacidade dizia que «três
coisas» saíam do telemóvel. Eram cinco — faltavam os quadrados do mapa das corridas, que revelam
a zona onde a pessoa corre, e as imagens dos exercícios. Todos os caminhos citados existiam. O
teste passou. O documento estava errado no ponto que mais importava.

Contra isso não há automatismo. Só há uma regra: **quem escreve, verifica** — e verifica correndo
o comando, não pela memória.

## Uma nota sobre o custo

Esta decisão torna a documentação mais cara de escrever e mais cara de mudar. É intencional.

A alternativa foi testada durante um ano e acabou com o repositório a ser esvaziado.
