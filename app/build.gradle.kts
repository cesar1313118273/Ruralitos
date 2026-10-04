import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val ruralitosLocalProperties = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use(::load)
}

fun valorBuildConfig(nombre: String): String =
    ruralitosLocalProperties.getProperty(nombre, "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    namespace = "com.ruralitos.app"
    compileSdk = 36
    assetPacks += listOf(":mapas")

    // Los APK que se instalan directo (depuración y pruebas) no llevan paquetes de recursos: se les añaden los mapas.
    // La versión que se sube a Google Play (bundleRelease) los recibe del paquete `:mapas`, no del módulo base.
    sourceSets {
        getByName("debug") { assets.srcDir(rootProject.file("mapas/src/main/assets")) }
        if (project.findProperty("pruebaRelease") == "true") {
            getByName("release") { assets.srcDir(rootProject.file("mapas/src/main/assets")) }
        }
    }

    // Firma de publicación: los datos viven en `keystore.properties` (fuera de git) o en variables de entorno.
    // Sin ellos, la versión release se firma con la clave de depuración SOLO para poder probarla en local.
    val firmaPropiedades = Properties().apply {
        val archivo = rootProject.file("keystore.properties")
        if (archivo.exists()) archivo.inputStream().use(::load)
    }
    fun datoDeFirma(nombre: String, entorno: String): String? =
        firmaPropiedades.getProperty(nombre) ?: System.getenv(entorno)
    val tieneFirmaPublicacion = datoDeFirma("storeFile", "RURALITOS_STORE_FILE") != null
    signingConfigs {
        if (tieneFirmaPublicacion) {
            create("publicacion") {
                storeFile = rootProject.file(datoDeFirma("storeFile", "RURALITOS_STORE_FILE")!!)
                storePassword = datoDeFirma("storePassword", "RURALITOS_STORE_PASSWORD")
                keyAlias = datoDeFirma("keyAlias", "RURALITOS_KEY_ALIAS")
                keyPassword = datoDeFirma("keyPassword", "RURALITOS_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "com.ruralitos.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 15
        versionName = "2.4.0-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testProguardFiles("proguard-test.pro")
        buildConfigField("String", "SUPABASE_URL", "\"${valorBuildConfig("supabase.url")}\"")
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${valorBuildConfig("supabase.publishableKey")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = if (tieneFirmaPublicacion) signingConfigs.getByName("publicacion")
                else signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (project.findProperty("pruebaRelease") == "true") proguardFiles("proguard-pruebas.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    // `-PpruebaRelease=true` ejecuta las pruebas del emulador sobre la versión ya optimizada (R8).
    if (project.findProperty("pruebaRelease") == "true") testBuildType = "release"
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // El mapa SQLite se copia una sola vez al almacenamiento privado para acceso aleatorio.
    androidResources {
        noCompress += "mbtiles"
    }
}

// Las herramientas de depuración (ui-tooling, ui-test-manifest) traen versiones más nuevas de Compose que la
// versión de la app. Se fijan al BOM para que lo que se prueba en depuración sea lo mismo que se publica.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "androidx.compose.foundation" || requested.group == "androidx.compose.animation" ||
            requested.group == "androidx.compose.ui"
        ) {
            useVersion("1.7.0")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    // org.json real para probar en la JVM el guardado del familiograma (en el teléfono ya viene incluido).
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Solo al probar la versión release (-PpruebaRelease=true): la actividad que usan las pruebas de interfaz.
    if (project.findProperty("pruebaRelease") == "true") releaseImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml) {
        exclude(group = "org.apache.xmlbeans", module = "xmlbeans")
    }
    // El JAR original de XMLBeans 2.6.0 contiene ocho clases duplicadas que D8 no admite.
    implementation(files("libs/xmlbeans-2.6.0-dedup.jar"))
    // Apache POI usa StAX (javax.xml.stream), que Android no incluye.
    // Sin esto el Excel y el PDF fallan con NoClassDefFoundError.
    implementation("stax:stax-api:1.0.1")
    implementation("com.fasterxml.woodstox:woodstox-core:5.4.0")
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.maplibre.android.opengl)
    implementation("io.github.rallista:valhalla-mobile:0.6.3")
    implementation("io.github.rallista:valhalla-models-config:0.5.2")
}
