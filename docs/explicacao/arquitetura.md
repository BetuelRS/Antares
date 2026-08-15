# Arquitetura

Este documento não descreve o que a app faz — isso lê-se no código, e um documento que o repete
começa a mentir no dia em que alguém muda uma linha.

Descreve o que o código **não consegue dizer sozinho**: porque é que está assim, e onde é que
morde.

## O mapa

Kotlin Multiplatform com um único alvo, Android. Compose para a interface, Room para os dados,
Koin para as ligações, DataStore para as preferências, Ktor para a rede.

```
composeApp/src/commonMain/kotlin/pt/antares/app/
  core/          contas, dados, rede, sistema de design
  feature/       um diretório por assunto: ecrã + ViewModel + repositório
  navigation/    rotas e barra de baixo
composeApp/src/androidMain/
                 o que não existe fora do Android: GPS, notificações,
                 widget, Health Connect, câmara
```

Não há *use cases* nem uma camada de domínio transversal: um ViewModel fala com um repositório, o
repositório fala com o DAO, e a aritmética vive à parte, em funções puras. Para uma app de uma
pessoa, uma camada a mais custa mais do que dá.

Duas funcionalidades fogem a isto e têm um pacote `domain` próprio — o jejum e a corrida. São as
duas que têm uma máquina de estados a sério (uma janela a contar, uma corrida a decorrer), e essa
lógica não cabe nem no ViewModel nem no repositório.

## As decisões que moldaram tudo o resto

Cada uma tem o seu registo, com o contexto em que foi tomada:

| | |
|---|---|
| [A app não sincroniza](decisoes/0001-a-app-nao-sincroniza.md) | não há conta com os teus dados, nem servidor com uma cópia |
| [Lápides e índices únicos](decisoes/0002-lapides-e-indices-unicos.md) | apagar é marcar, e isso colide com o índice do dia |
| [As contas em funções puras](decisoes/0003-contas-em-funcoes-puras.md) | a nutrição não sabe o que é o Android |
| [O versionamento deriva do nome](decisoes/0004-versionamento-derivado-do-nome.md) | um número, e o resto é consequência |
| [Documentação verificada por testes](decisoes/0005-documentacao-verificada-por-testes.md) | porque é que este repositório foi esvaziado uma vez |

## O congelado e o vivo

Duas regras que se contradizem de propósito.

Uma **receita é viva**: muda um ingrediente e todos os números dela mudam. Um **registo do diário
é congelado**: o que comeste ontem não muda porque hoje corrigiste a ficha do alimento.

A reconciliação está no `logRecipe`, em
`composeApp/src/commonMain/kotlin/pt/antares/app/feature/recipe/RecipeRepository.kt`: a receita é
convertida num alimento temporário que nunca entra no catálogo, e o registo copia dele os valores.
A partir daí o diário é dono do que lá ficou.

Se te vires a resolver um problema em que «o histórico mudou sozinho», é quase sempre aqui.

## A massa gorda tem uma fonte só

A tabela `body_measurement_log` guarda a série toda, e é ela a verdade. O `user_profile` guarda
uma **cópia** da percentagem mais recente, porque o cálculo do metabolismo basal a consulta a cada
conta e não pode andar a varrer o histórico.

A cópia nunca se escreve à mão. Toda a escrita passa pelo `BodyMeasurementRepository`, que a
repõe a partir da medição viva mais recente — e é isso que impede os dois de discordarem.

Três casos que a regra tem de aguentar, e que o `BodyCompositionSaveTest` fixa:

| O que se faz | O que acontece |
|---|---|
| Registar só a cintura | A percentagem mantém-se: nulo quer dizer «não medi isto agora» |
| Escolher «não sei» | Apaga nos dois sítios, e a linha do dia vai com ela se ficar vazia |
| Apagar a medição de hoje | O perfil volta à medição anterior, não fica com um valor órfão |

**Até 2026-08-15 não era assim.** Escolher «não sei» limpava o perfil e deixava o histórico do dia
intacto, porque o registo de medições fundia com o que lá estava em vez de substituir. As duas
fontes ficavam a mostrar valores diferentes para a mesma coisa, até à medição seguinte.

## As ligações

| Módulo | Onde | O que liga |
|---|---|---|
| `coreModule` | `composeApp/src/commonMain/kotlin/pt/antares/app/core/di/CoreModule.kt` | repositórios, rede, preferências |
| `viewModelModule` | `composeApp/src/commonMain/kotlin/pt/antares/app/core/di/ViewModelModule.kt` | um por ecrã |
| `databaseModule` | `composeApp/src/androidMain/kotlin/pt/antares/app/core/di/DatabaseModule.kt` | a base, que precisa do contexto do Android |

O `KoinGraphTest` constrói os repositórios todos e rebenta no teste, em vez de rebentar na
primeira vez que alguém abre o ecrã. Percorre uma **lista escrita à mão**: um serviço novo só é
coberto depois de ser acrescentado a essa lista.

## Sem rede

A app funciona sem rede, e isso é uma propriedade defendida por testes, não uma consequência
feliz. O relatório semanal é calculado no telemóvel — o `AdaptiveTargetsOfflineTest` falha se
alguma chamada de rede voltar a esse caminho.

O que precisa mesmo de rede é a pesquisa na Open Food Facts, a análise por foto e por texto, o
mapa das corridas e as imagens dos exercícios. Todas distinguem **falha de rede** de **não há
resultados**, porque o ecrã diz coisas diferentes e a pessoa faz coisas diferentes a seguir.

Ver [Privacidade](privacidade.md) para o que sai e para onde.

## O que não está resolvido

- O gerador de dados de demonstração calcula a carga a partir do código do exercício, sem olhar ao
  equipamento — daí aparecerem recordes de 155 kg em exercícios de mobilidade.
- O resumo de uma corrida sem percurso deixa dois terços do ecrã vazios.
