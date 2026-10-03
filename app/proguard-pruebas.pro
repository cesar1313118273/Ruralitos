# Solo al probar la versión release en el emulador (-PpruebaRelease=true): el APK de pruebas comparte bibliotecas con la
# app, así que no se recortan las de Kotlin/AndroidX/Google. El código de Ruralitos y las demás bibliotecas
# (Apache POI, SQLCipher, Valhalla, MapLibre) sí pasan por R8 con las reglas reales.
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class androidx.** { *; }
-keep class com.google.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.**
-dontwarn com.google.**
-keep class com.ruralitos.app.** { *; }
-keep class org.maplibre.** { *; }
