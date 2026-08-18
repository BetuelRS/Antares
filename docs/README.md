# Documentação da Antares

Esta documentação está dividida por **aquilo de que precisas neste momento**, e não por assunto.
É a divisão do [Diátaxis](https://diataxis.fr/): quem quer resolver um problema concreto não quer
ler um ensaio, e quem quer perceber uma decisão não quer uma receita.

| Se queres… | Vai a |
|---|---|
| **fazer** uma coisa concreta, do princípio ao fim | [Guias](#guias) |
| **consultar** um facto enquanto trabalhas | [Referência](#referência) |
| **perceber** porque é que isto está assim | [Explicação](#explicação) |

---

## Guias

Passo a passo, com um princípio e um fim. Não explicam o porquê — apontam para onde ele está.

| | |
|---|---|
| [Compilar a app](guias/compilar.md) | de um clone vazio a um APK instalado |
| [Correr os testes](guias/correr-os-testes.md) | as duas suites, e as armadilhas que fazem parecer que está tudo partido |
| [Lançar uma versão](guias/lancar-uma-versao.md) | a lista completa, por ordem |
| [Publicar o servidor](guias/publicar-o-servidor.md) | as Edge Functions e os segredos que precisam |

## Referência

Factos, para consultar. Sem opinião e sem narrativa.

| | |
|---|---|
| [Regras de trabalho](referencia/regras.md) | as 29 regras da produção, e quem verifica cada uma |
| [Base de dados](referencia/base-de-dados.md) | tabelas, versões do esquema, regras das migrações |
| [Testes-guarda](referencia/testes-guarda.md) | o que cada um defende, e o que fazer quando falha |
| [Versionamento](referencia/versionamento.md) | o esquema de números e a fórmula do `versionCode` |
| [Dados e licenças](referencia/dados-e-licencas.md) | de onde vem cada alimento e cada exercício, e o que isso obriga |

## Explicação

O porquê. Lê-se de uma ponta à outra, não se consulta.

| | |
|---|---|
| [Arquitetura](explicacao/arquitetura.md) | as decisões que moldaram o código e as armadilhas que deixaram |
| [Privacidade](explicacao/privacidade.md) | o que fica no telemóvel, o que sai, e como verificar sem acreditar |
| [Registo de decisões](explicacao/decisoes/) | cada decisão estruturante, com o contexto em que foi tomada |

---

## Como esta documentação se mantém honesta

O `DocumentationHonestyTest` corre com a suite e falha se:

- a versão no topo do [CHANGELOG.md](../CHANGELOG.md) não for a versão da app;
- o cartaz de versão do [README.md](../README.md) não bater com o `versionName`;
- o `versionCode` deixar de derivar do nome pela fórmula documentada;
- **um documento citar um ficheiro que não existe.**

A última é a que interessa. Este repositório já foi esvaziado uma vez porque os documentos
descreviam funcionalidades removidas, e quem os lia ficava a descrever a app errada com toda a
confiança. Um caminho entre crases é uma promessa, e há um teste a cobrá-la.

O que este teste **não** consegue verificar são as afirmações. Um documento pode citar ficheiros
que existem e mesmo assim mentir sobre o que eles fazem — aconteceu com a página da privacidade,
que dizia «três coisas saem do telemóvel» quando eram cinco. Contra isso só há uma defesa: quem
escreve, verifica.
