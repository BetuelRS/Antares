# 0003 — A aritmética vive em funções puras, longe do Android

**Estado:** Aceite

## Contexto

A app é, no fundo, uma calculadora com histórico. O metabolismo basal por três fórmulas
diferentes, o gasto por nível de atividade, os macronutrientes por estratégia, as referências da
EFSA por sexo e fase da vida, a massa gorda por dois métodos, o 1RM estimado, a tendência do peso
com média móvel.

Estas contas decidem números que a pessoa vê e em que confia. Se uma delas ficar errada, a app
continua a funcionar — só passa a mentir.

## Decisão

**Toda a aritmética vive em `composeApp/src/commonMain/kotlin/pt/antares/app/core/calc/`, em
funções puras: sem Android, sem base de dados, sem relógio.**

O que depende do tempo recebe o dia como argumento. O que depende do perfil recebe o perfil. Nada
lá dentro lê nada.

Os ecrãs chamam estas funções. **Não repetem a conta.**

## Consequências

**Bom:**

- A maior parte da suite de testes corre sem emulador e sem Robolectric, em segundos. Isso torna
  barato afirmar coisas sobre nutrição — e o que é barato afirmar, afirma-se muito.
- Uma fórmula pode ser verificada à mão contra a literatura, porque está isolada.
- É o que permite o ecrã «Mostra-me a conta»: a app consegue apresentar os passos porque os passos
  existem como passos.

**O preço:**

- Há indireção. Para perceber um número no ecrã é preciso saltar do ecrã para o `composeApp/src/commonMain/kotlin/pt/antares/app/core/calc/`.
- É preciso disciplina. Copiar três linhas de fórmula para dentro de um ViewModel é sempre mais
  rápido no momento.

## O que acontece quando se quebra

Aconteceu. O `DiaryViewModel` tinha uma cópia da fórmula da meta de água, em vez de chamar a
partilhada. Enquanto a constante não mudou, ninguém deu por nada — os dois ecrãs mostravam o mesmo
número.

No dia em que a constante mudasse, passavam a discordar, e o defeito apareceria num ecrã só. Foi
corrigido substituindo a cópia pela chamada:

```kotlin
// A mesma regra do ecrã de hoje, e pela mesma função: repetir a conta aqui
// fazia os dois ecrãs discordarem assim que a constante mudasse.
val goal = override
    ?: DailyGoals.waterMl(weight?.weightKg ?: ProfileRepository.DEFAULT_WEIGHT_KG)
```

**Regra prática:** se te vires a copiar uma fórmula para dentro de um ViewModel, ela devia estar
no `composeApp/src/commonMain/kotlin/pt/antares/app/core/calc/`. Se já lá está, chama-a.
