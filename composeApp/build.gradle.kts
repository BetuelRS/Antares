import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localProp(key: String): String = localProps.getProperty(key, "")

/**
 * A versão da app, escrita uma vez só. Segue SemVer estrito: três números, nunca quatro.
 * Sobe MAJOR quando o que a pessoa tinha deixa de funcionar como antes, MINOR quando ela
 * ganha alguma coisa, PATCH quando só se corrige — ver `docs/VERSIONING.md`.
 *
 * O `AppChangelogTest` falha se o `AppChangelog.CURRENT` deixar de acompanhar este valor.
 */
val appVersion = "1.0.0"

/**
 * O `versionCode` deriva do nome em vez de ser contado à mão: `1.2.3` dá `10203`, e lê-se ao
 * contrário sem consultar tabela nenhuma. Cabem 99 em cada casa, e cresce sempre — que é a
 * única coisa que o Android exige de um `versionCode`.
 *
 * O valor anterior a esta regra era 66, contado à mão; `1.0.0` dá 10000, e por isso a subida
 * mantém-se monótona apesar da mudança de esquema.
 */
fun androidVersionCode(version: String): Int {
    val parts = version.split(".")
    require(parts.size == 3) { "versão fora de SemVer (esperado MAJOR.MINOR.PATCH): $version" }
    val (major, minor, patch) = parts.map {
        it.toIntOrNull() ?: error("versão com um segmento não numérico: $version")
    }
    require(minor < 100 && patch < 100) { "minor e patch têm de caber em duas casas: $version" }
    return major * 10_000 + minor * 100 + patch
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.room)
    alias(libs.plugins.detekt)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.datastore.preferences)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.functions)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)

            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.android)

            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.barcode.scanning)

            implementation(libs.androidx.work.runtime)

            implementation(libs.maplibre.android)

            implementation(libs.play.services.location)

            implementation(libs.androidx.health.connect)

            implementation(libs.guava)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.robolectric)
                implementation(libs.androidx.test.junit)

                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}

android {

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    namespace = "com.antares.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.antares.app"
        minSdk = 26
        targetSdk = 36
        versionName = appVersion
        versionCode = androidVersionCode(appVersion)

        buildConfigField("String", "SUPABASE_URL", "\"${localProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("SUPABASE_ANON_KEY")}\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    lint {
        // Mesma ideia do detekt: a linha de base guarda o que já estava aqui, e só o
        // código novo faz o CI falhar. Para a refazer, apagar o ficheiro e correr
        // ./gradlew :composeApp:lintDebug depois de apagar o ficheiro.
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = false
    }

    splits {
        abi {
            isEnable = true
            reset()

            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("runSupabaseIt", System.getProperty("runSupabaseIt") ?: "false")
    testLogging { showStandardStreams = true }
}

compose.resources {
    packageOfResClass = "pt.antares.app.generated.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)

    debugImplementation(libs.compose.ui.test.manifest)
}

detekt {
    // A análise corre sobre `src` inteiro e não por conjunto de fontes: num projeto
    // multiplataforma cada conjunto teria a sua tarefa, e o que interessa é uma só.
    source.setFrom("src")
    parallel = true
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))

    // A linha de base guarda o que já estava mal no dia em que isto foi ligado. Só
    // código novo faz o CI falhar; o que está aqui dentro corrige-se por vontade, não
    // por bloqueio. Para a refazer: `./gradlew detektBaseline`.
    baseline = rootProject.file("config/detekt/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = JavaVersion.VERSION_17.toString()
    reports {
        html.required = true
        xml.required = false
        txt.required = false
        sarif.required = false
        md.required = false
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = JavaVersion.VERSION_17.toString()
}
