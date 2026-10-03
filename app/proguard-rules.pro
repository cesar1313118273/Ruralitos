# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# SQLCipher usa clases JNI que deben conservar sus nombres.
-keep,includedescriptorclasses class net.zetetic.database.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.zetetic.database.sqlcipher.** { *; }

# --- Apache POI (Excel/PDF): carga clases por nombre y usa esquemas XML generados ---
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.apache.commons.** { *; }
-keep class com.ctc.wstx.** { *; }
-keep class org.codehaus.stax2.** { *; }
-keep class javax.xml.stream.** { *; }
-keep class com.zaxxer.sparsebits.** { *; }
-dontwarn org.apache.**
-dontwarn org.openxmlformats.**
-dontwarn com.microsoft.schemas.**
-dontwarn javax.xml.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn org.w3c.dom.**
-dontwarn net.sf.saxon.**
-dontwarn aQute.bnd.**
-dontwarn org.osgi.**
-dontwarn org.bouncycastle.**
-dontwarn com.github.javaparser.**
-dontwarn org.slf4j.**
-dontwarn org.codehaus.stax2.**
-dontwarn com.ctc.wstx.**

# --- Rutas sin conexión (Valhalla) y mapas ---
-keep class io.github.rallista.** { *; }
-keep class com.valhalla.** { *; }
-keep class valhalla.** { *; }
-dontwarn io.github.rallista.**

# --- Datos que se (de)serializan con org.json por nombre de campo no necesitan reglas; Room trae las suyas. ---
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*
-renamesourcefileattribute SourceFile

# Clases que Apache POI referencia pero no incluye (firmas digitales XML que la app no usa).
-dontwarn org.etsi.uri.**
-dontwarn org.w3.x2000.**
-dontwarn org.w3.x2001.**
-dontwarn org.openxmlformats.schemas.**

# Solo afecta al APK de pruebas del emulador (androidx.test referencia clases de Guava/concurrent que no usa).
-dontwarn androidx.concurrent.futures.**
-dontwarn com.google.common.**
-dontwarn javax.lang.model.element.Modifier

# El APK de pruebas usa estas bibliotecas a través de la app; sin esto R8 las elimina (solo importa al probar release).
-keep class androidx.tracing.** { *; }

# XMLBeans (Apache POI) lee campos de QName y de las clases generadas por nombre; sin esto el Excel/PDF falla en release.
-keep class javax.xml.** { *; }
-keep class org.w3c.dom.** { *; }
-keep class javax.xml.namespace.QName { *; }

# En la versión publicada no quedan mensajes de depuración ni informativos en el registro del teléfono.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
