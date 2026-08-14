# Compilar a app

De um clone vazio a um APK no telemóvel.

## Antes de começar

| | |
|---|---|
| **JDK 17** | não 21 — o Gradle está fixado em 17, e é o que o CI usa |
| **Android SDK 36** | `compileSdk` e `targetSdk` |
| **Android Studio** | opcional; tudo o que está aqui corre na linha de comandos |

Não é preciso conta em serviço nenhum, nem chave nenhuma, para compilar e correr.

## Compilar

```bash
git clone https://github.com/BetuelRS/Antares.git
cd Antares
./gradlew :composeApp:assembleDebug
```

O APK fica em `composeApp/build/outputs/apk/debug/`.

## Instalar num telemóvel

```bash
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

A app corre a partir do **Android 8.0** (`minSdk 26`).

## Ligar a análise por foto e por texto

Esta é a única funcionalidade que precisa de servidor. Sem ela a app faz tudo o resto.

Cria um `local.properties` na raiz:

```properties
SUPABASE_URL=https://o-teu-projeto.supabase.co
SUPABASE_ANON_KEY=a-tua-chave-anonima
```

O ficheiro está no `.gitignore` e nunca deve ser versionado. Para preparar o lado do servidor, vê
[Publicar o servidor](publicar-o-servidor.md).

Sem estas duas linhas, a app compila, arranca e funciona — só a análise é que não responde, e
di-lo em vez de falhar em silêncio.

## Compilar para distribuição

```bash
./gradlew :composeApp:assembleRelease
```

O `release` está assinado com a chave de depuração. Isso chega para instalar à mão e **não chega**
para publicar numa loja.

Saem quatro APKs, um por arquitetura (`arm64-v8a`, `armeabi-v7a`, `x86_64`) e um universal. Para
um telemóvel moderno, o `arm64-v8a`; para um emulador, o `x86_64`.

## Quando corre mal

**`Unsupported class file major version`** — estás em JDK 21 ou superior. Confirma com
`java -version` e aponta o `JAVA_HOME` para um JDK 17.

**`SDK location not found`** — falta o `sdk.dir` no `local.properties`, ou a variável
`ANDROID_HOME`.

**A app arranca e não tem alimentos nenhuns** — o catálogo é semeado no primeiro arranque a partir
de `composeApp/src/commonMain/composeResources/files/`. Se apagaste os dados da app a meio da
sementeira, desinstala e volta a instalar.
