# 0004 — O `versionCode` deriva do `versionName`

**Estado:** Aceite · **Desde:** 1.0.0

## Contexto

Até à 1.0.0 a numeração tinha quatro segmentos e chegou a `0.9.18.1`. Não era
[SemVer](https://semver.org/) nem outra coisa reconhecível: os quatro números tinham significados
que mudavam conforme a ocasião.

O `versionCode` — o inteiro que o Android exige e que tem de crescer sempre — era escrito à mão, e
ia em 66. Não tinha relação nenhuma com o nome. Olhar para 66 não dizia que versão era, e esquecer
de o subir era possível.

Em 63 versões compiladas ao longo de um mês, não havia changelog nenhum.

## Decisão

**SemVer estrito**, três números, e o `versionCode` deixa de ser escrito:

```kotlin
val appVersion = "1.0.0"
versionName = appVersion
versionCode = major * 10_000 + minor * 100 + patch   // 1.0.0 -> 10000
```

A fórmula recusa versões com quatro segmentos, ou com `minor`/`patch` acima de 99.

`10000 > 66`, portanto a subida manteve-se monótona apesar da mudança de esquema.

O histórico foi **reconstruído** em SemVer, a partir de duas fontes que não dependem de memória:
os 21 esquemas exportados da base de dados, que dizem em que ordem cada funcionalidade ganhou
sítio onde viver, e as datas dos artefactos compilados.

## Consequências

**Bom:**

- Um número só para mudar ao lançar. O `versionCode` não pode ficar para trás.
- 10203 lê-se como 1.2.3 sem consultar nada.
- Há um changelog, e a regra de quando cada número sobe está escrita.

**O preço, e é real:**

- **As versões do histórico não correspondem uma a uma às antigas.** Os APKs em `apks/` têm a
  numeração antiga e não foram renomeados — renomear um artefacto já compilado é perder a única
  prova do que ele é.
- Por isso o `CHANGELOG.md` acaba com uma nota a dizer que foi reconstruído. Sem ela, quem
  comparar o histórico com os ficheiros conclui, com razão, que foi inventado.

## A regra que governa o conteúdo

**Nunca se anuncia o nascimento de uma coisa que já não existe.**

A app teve conta e sincronização. No histórico reconstruído isso aparece uma vez, como remoção na
1.0.0, e nunca como novidade. Um changelog que apresenta como novidade aquilo que foi removido é a
maneira mais eficaz de fazer alguém descrever a app errada com toda a confiança — que é
exatamente o problema que [a decisão 0005](0005-documentacao-verificada-por-testes.md) tenta
resolver.

## Como isto é defendido

O `DocumentationHonestyTest` verifica a fórmula, o número de segmentos, e que o `CHANGELOG.md`, o
cartaz do `README.md` e o `AppChangelog.CURRENT` abrem todos na mesma versão.
