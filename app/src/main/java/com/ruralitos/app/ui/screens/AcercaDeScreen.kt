package com.ruralitos.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico

private fun versionDe(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    info.versionName.orEmpty()
}.getOrDefault("")

/** Lee los avisos de licencia que se empaquetan con la app (carpeta `third_party_licenses`). */
private fun avisosDeLicencia(context: Context): List<Pair<String, String>> = runCatching {
    val carpeta = "third_party_licenses"
    context.assets.list(carpeta).orEmpty().sorted().map { nombre ->
        val titulo = nombre.substringBeforeLast('.').replace('_', ' ')
        titulo to context.assets.open("$carpeta/$nombre").bufferedReader().use { it.readText().trim() }
    }
}.getOrDefault(emptyList())

@Composable
fun AcercaDeScreen(onRegresar: () -> Unit) {
    val context = LocalContext.current
    val version = remember { versionDe(context) }
    val avisos = remember { avisosDeLicencia(context) }

    PantallaRuralitos(
        titulo = "Acerca de Ruralitos",
        subtitulo = if (version.isBlank()) null else "Versión $version",
        descripcion = "Información de la herramienta, avisos y licencias.",
        onVolver = onRegresar
    ) {
        Bloque(
            "Herramienta independiente",
            "Ruralitos es una herramienta de registro creada por su autor. No es un producto oficial ni está " +
                "afiliada, avalada ni patrocinada por el Ministerio de Salud Pública ni por ninguna otra institución " +
                "del sector salud. Cada centro o profesional es responsable de contar con la autorización de su " +
                "institución para usarla y de los datos que registra."
        )
        Bloque(
            "Datos de pacientes",
            "Los datos se guardan cifrados en este teléfono y, si inicias sesión, se sincronizan con tu Sala. " +
                "Solo las personas con acceso a esa Sala pueden verlos."
        )
        Bloque(
            "Mapas y cartografía",
            "© OpenStreetMap contributors · Protomaps. Los datos de OpenStreetMap se distribuyen bajo la licencia " +
                "abierta ODbL. Mapas con MapLibre; rutas sin conexión con Valhalla (licencia MIT). " +
                "Elevación del terreno: Mapzen/Terrarium (datos abiertos de varias fuentes)."
        )
        Bloque(
            "Otras bibliotecas",
            "Apache POI (Apache License 2.0) para Excel; SQLCipher de Zetetic (licencia BSD) para el cifrado de la base de datos; " +
                "Liberation Fonts (SIL Open Font License); Noto Sans (SIL Open Font License)."
        )
        PlantillaExcel()
        InformeErrores()
        avisos.forEach { (titulo, texto) -> Bloque("Licencia: $titulo", texto) }
    }
}

@Composable
private fun Bloque(titulo: String, texto: String) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = androidx.compose.ui.graphics.Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BordeClinico)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro)
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Si la app se cerró sola alguna vez, permite enviar el informe técnico (sin datos de pacientes). */
@Composable
private fun InformeErrores() {
    val context = LocalContext.current
    var hayInforme by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(com.ruralitos.app.data.diagnostico.RegistroErrores.hayRegistros(context))
    }
    Bloque(
        "Informe de errores",
        if (hayInforme) "La app se cerró de forma inesperada en este teléfono. El informe solo contiene el tipo de error " +
            "y la ruta del código; no incluye nombres, cédulas ni datos de fichas."
        else "No hay errores registrados en este teléfono."
    )
    if (hayInforme) {
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Button(onClick = {
                val texto = com.ruralitos.app.data.diagnostico.RegistroErrores.leer(context)
                val envio = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Informe de errores de Ruralitos")
                    putExtra(android.content.Intent.EXTRA_TEXT, texto)
                }
                context.startActivity(android.content.Intent.createChooser(envio, "Enviar informe").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Enviar informe") }
            androidx.compose.material3.OutlinedButton(onClick = {
                com.ruralitos.app.data.diagnostico.RegistroErrores.borrar(context)
                hayInforme = false
            }) { Text("Borrar informe") }
        }
    }
}

/** Cada centro puede usar su propia plantilla de Excel para la ficha (mismas hojas y casillas que la original). */
@Composable
private fun PlantillaExcel() {
    val context = LocalContext.current
    var propia by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(com.ruralitos.app.data.export.PlantillaPropia.existe(context))
    }
    var mensaje by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val selector = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            mensaje = runCatching { com.ruralitos.app.data.export.PlantillaPropia.importar(context, uri) }
                .fold(
                    onSuccess = { propia = true; "Plantilla guardada. Se usará al generar el Excel y el PDF de las fichas." },
                    onFailure = { it.message ?: "No se pudo usar ese archivo." }
                )
        }
    }
    Bloque(
        "Plantilla del Excel de la ficha",
        (if (propia) "Estás usando tu propia plantilla. " else "Estás usando la plantilla que trae Ruralitos. ") +
            "Si tu centro tiene su propio formato, puedes cargarlo: debe ser un .xlsx con las mismas cuatro hojas " +
            "(«1» a «4») y las mismas casillas, porque Ruralitos escribe los datos por posición."
    )
    mensaje?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Button(onClick = {
            selector.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        }) { Text("Cargar mi plantilla") }
        if (propia) {
            androidx.compose.material3.OutlinedButton(onClick = {
                com.ruralitos.app.data.export.PlantillaPropia.quitar(context)
                propia = false
                mensaje = "Se volvió a la plantilla de Ruralitos."
            }) { Text("Usar la de Ruralitos") }
        }
    }
}
