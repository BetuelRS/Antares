# 0006 — A app compila sem R8

**Estado:** Aceite · **Desde:** 1.0.0

## Contexto

Não existe `isMinifyEnabled` em lado nenhum do `composeApp/build.gradle.kts`, nem ficheiro de
regras ProGuard. As compilações de release saem com o bytecode inteiro.

O que isso custa, medido no `antares-0.9.18.1-arm64-release.apk`:

| | Comprimido | Por dentro |
|---|---|---|
| APK arm64 | **39,8 MB** | 95,1 MB em 630 ficheiros |
| APK universal | **71,1 MB** | quatro arquiteturas no mesmo ficheiro |

E onde está o peso, sem compressão:

| Parte | Tamanho | O R8 mexe? |
|---|---|---|
| Cinco ficheiros `classes*.dex` | **66,9 MB** | sim — é exatamente isto que ele encolhe |
| `libmaplibre.so` | 11,4 MB | não, é nativo |
| `libbarhopper_v3.so`, do leitor de códigos | 4,9 MB | não |
| `libsqliteJni.so` | 1,9 MB | não |
| Catálogos semeados (`seed_foods`, `tca`, `exercises`) | 6,0 MB | não, e são a razão de a app funcionar sem rede |

Os 67 MB de bytecode são a parte atacável, e é muita: uma app de Compose Multiplatform com
navegação, Room, Ktor, Coil, CameraX, ML Kit, MapLibre, Health Connect e WorkManager arrasta um
grafo de classes que o R8 costuma cortar para menos de metade.

## Decisão

**Continuar sem R8.**

A app apoia-se em reflexão e em serialização em sítios onde uma remoção errada não parte a
compilação — parte a app em execução, e só naquele ecrã:

- `kotlinx.serialization` sobre as **29 entidades** da base, que são as mesmas classes que entram e
  saem do backup. Uma classe apagada aqui não dá erro: dá uma cópia que não abre.
- O Room gera implementações que são procuradas por nome.
- O Koin resolve por tipo, e um construtor removido só falha quando o ecrã abre.
- As rotas são `@Serializable`, e é assim que os argumentos de navegação viajam.

Cada uma destas bibliotecas traz as suas regras `consumer-proguard`, e na maioria dos casos
chegam. O problema é o custo de descobrir quando **não** chegam: a falha aparece só na release,
só naquele caminho, e o rasto que deixa não aponta para a causa. Numa app cujo modo de falhar é
«a pessoa perde o diário», isso não é um risco que se corra por 20 MB.

*Rejeitado:* ligar o R8 agora; encolher só os recursos com `isShrinkResources`.

## Consequências

**O preço, e é real:**

- O APK arm64 tem 39,8 MB quando podia ter perto de metade.
- O universal tem 71,1 MB, e é o que se instala à mão. Quem instalar por fora do repositório
  descarrega quatro arquiteturas para usar uma.
- Os nomes das classes vão todos no artefacto. Não há aqui segredo nenhum a proteger — o código é
  GPL-3.0 e está publicado — mas convém dizê-lo em vez de dar a entender o contrário.

**Bom:**

- O que se testa é o que se instala. Não há uma classe de defeitos que só existe na release.
- Não há ficheiro de regras a envelhecer em silêncio a cada atualização de biblioteca.

## Quando isto se reavalia

**Se a app for para uma loja.** Aí a distribuição passa a ser por *App Bundle*, cada telemóvel
recebe só a sua arquitetura, e o tamanho passa a ser um número que outra pessoa vê antes de
decidir instalar.

O caminho, nessa altura, é ligar o R8 em `debug` primeiro, correr a app inteira ecrã a ecrã, e só
depois passá-lo à release — não o contrário.
