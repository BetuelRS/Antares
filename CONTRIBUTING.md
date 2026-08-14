# Contribuir

Obrigado pelo interesse. Este é um projeto de uma pessoa, e por isso convém dizer as coisas como
elas são antes de gastares tempo.

## O que ajuda mesmo

**Relatar um defeito que consigas reproduzir.** É o mais útil de tudo, e não precisa de saber
programar.

**Corrigir um defeito que já esteja numa *issue*.** Está aberto a quem quiser.

**Traduzir.** A app é bilingue, português e inglês. Os textos estão em
`composeApp/src/commonMain/composeResources/`.

## O que provavelmente não vai ser aceite

**Funcionalidades novas grandes, sem falarmos antes.** Abre uma *issue* primeiro. Uma funcionalidade
tem de ser mantida durante anos, e nem todas valem esse preço.

**Mudanças que ponham dados do utilizador num servidor.** É a decisão mais deliberada do projeto —
ver [a decisão 0001](docs/explicacao/decisoes/0001-a-app-nao-sincroniza.md). Há um teste que
falha se a sincronização voltar.

**Reescritas de arquitetura.** Não porque a atual seja perfeita, mas porque o custo de a rever cai
todo em cima de uma pessoa.

## Relatar um defeito

Antes de abrires, confirma que estás na versão mais recente e procura se já existe.

Diz sempre:

- **o que fizeste**, passo a passo, do arranque da app até ao problema;
- **o que esperavas** e **o que aconteceu**;
- a **versão da app** — está em Definições → Sobre;
- o **modelo do telemóvel** e a versão do Android.

Se a app fechou sozinha, o motivo fica guardado: Definições → Último erro, e há um botão para o
partilhar. Isso vale mais do que qualquer descrição.

## Submeter uma alteração

**1. Compila e corre os testes antes de escrever código.** Se já estão vermelhos, o problema não
é teu. Ver [Compilar](docs/guias/compilar.md) e
[Correr os testes](docs/guias/correr-os-testes.md).

**2. Escreve o teste primeiro, quando der.** Os testes são a única especificação que este projeto
tem.

**3. Segue o que está à volta.** Não há guia de estilo separado: o código existente é o guia.
Comentários e nomes de teste em **português**.

**4. Se um teste-guarda falhar, lê o que ele defende.** A pergunta certa não é como o fazer passar
— é que decisão foi desfeita. A lista está em
[Testes-guarda](docs/referencia/testes-guarda.md).

**5. Antes de submeter:**

```bash
./gradlew build
cd supabase && deno test --allow-env --allow-net functions/_shared/
```

**6. Uma alteração por *pull request*.** Se estiveres a corrigir dois defeitos, são dois.

## Comentários

A convenção é apertada de propósito, porque este repositório já foi esvaziado uma vez por causa de
documentação que apodreceu — ver
[a decisão 0005](docs/explicacao/decisoes/0005-documentacao-verificada-por-testes.md).

Um comentário **acrescenta um facto que não está na linha**: uma unidade, um limite, um efeito
colateral, a razão de uma escolha. Não repete o que o nome já diz.

```kotlin
// Sim
// Vê as lápides, para reaproveitar a linha do dia — o índice único conta-as.
val row = dao.byDayForWrite(epochDay)

// Não
// Vai buscar a linha do dia.
val row = dao.byDayForWrite(epochDay)
```

Escreve-se no presente, sobre a decisão, e não sobre o caminho até ela. Pode citar-se um teste
pelo nome; não se citam caminhos de ficheiro nem números de linha, que ficam errados sozinhos.

Nos ecrãs em Compose, muito menos comentários: só onde a decisão de interface não é evidente.

## Licença

A Antares é [GPL-3.0](LICENSE). Ao contribuíres, aceitas que a tua alteração seja distribuída sob
a mesma licença.

## Código de conduta

Vale o [Código de Conduta](CODE_OF_CONDUCT.md) em todos os espaços do projeto.
