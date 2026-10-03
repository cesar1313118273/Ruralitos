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

    defaultConfig {
        applicationId = "com.ruralitos.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "2.3.1-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"${valorBuildConfig("supabase.url")}\"")
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${valorBuildConfig("supabase.publishableKey")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // El mapa SQLite se copia una sola vez al almacenamiento privado para acceso aleatorio.
    androidResources {
        noCompress += "mbtiles"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
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
