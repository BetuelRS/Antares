# 0002 — Apagar é marcar, e o índice do dia conta as lápides

**Estado:** Aceite

## Contexto

Duas regras que nasceram separadas e que, juntas, produziram o defeito mais difícil de encontrar
que esta app teve.

**Primeira:** apagar não apaga. Uma linha apagada fica com `deleted = 1` — uma *lápide*. Era
necessário quando a app sincronizava, para o outro lado saber que a linha tinha desaparecido, e
manteve-se depois porque o mesmo mecanismo serve o desfazer e a exportação.

**Segunda:** várias tabelas têm um índice único no dia (`epochDay`), porque não faz sentido haver
duas pesagens no mesmo dia, nem duas medições, nem dois relatórios da mesma semana.

O SQLite conta as lápides no índice único. Ele não sabe que `deleted = 1` significa «não existe».

## O defeito

1. Pesas-te hoje.
2. Apagas a pesagem, porque escreveste o número errado.
3. Pesas-te outra vez.
4. **Não fica gravado.** Sem erro, sem aviso, sem nada no ecrã.

A linha nova colide com a lápide da antiga. A escrita falha em silêncio.

O mesmo valia para as medições corporais e para o relatório semanal.

## Decisão

Manter as duas regras, e **acrescentar um caminho de escrita que vê as lápides**.

Cada DAO com índice por dia ganhou um método `byDayForWrite`, que procura a linha do dia **sem**
filtrar as apagadas. A escrita reaproveita a lápide em vez de colidir com ela:

```kotlin
// Vê as lápides, para reaproveitar a linha do dia — o índice único conta-as.
val row = dao.byDayForWrite(epochDay)
// Mas só uma linha viva contribui com valores: reabrir um dia apagado começa
// do zero em vez de ressuscitar medidas que a pessoa desfez.
val existing = row?.takeIf { !it.deleted }
```

A segunda linha é tão importante como a primeira: a lápide é reutilizada como espaço, não como
conteúdo.

## Consequências

- Quem escrever um caminho novo que grave por dia **tem de usar o `byDayForWrite`**. Usar o método
  normal reintroduz o defeito, e o defeito é silencioso.
- Há dois métodos parecidos em cada DAO, e o nome do segundo é a única coisa que avisa.
- O custo de mudar isto agora — deixar de usar lápides, ou tirar os índices únicos — é maior do
  que o de o documentar.

## Como isto é defendido

O `TombstoneCollisionTest` reproduz a sequência apagar/reescrever em cada tabela com índice por
dia. Se alguém acrescentar uma tabela dessas e esquecer o `byDayForWrite`, o teste apanha.
