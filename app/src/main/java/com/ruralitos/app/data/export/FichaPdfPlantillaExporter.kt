package com.ruralitos.app.data.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * PDF de la ficha familiar: primero se llena el Excel (la plantilla ya viene configurada para
 * imprimir cada hoja en una página A4 horizontal) y luego ese mismo libro se convierte a PDF.
 */
object FichaPdfPlantillaExporter {
    suspend fun exportar(context: Context, fichaId: Long, destino: Uri) = withContext(Dispatchers.IO) {
        val data = FichaExcelExporter.cargarDatos(context, fichaId)
        context.contentResolver.openOutputStream(destino, "w")?.use { salida ->
            generar(context, data, salida)
        } ?: error("No se pudo abrir el destino PDF.")
    }

    internal fun generar(context: Context, data: FichaExportData, salida: OutputStream) {
        // El libro es una copia temporal y completa del Excel que se descargaría.
        // La plantilla incluida en assets permanece siempre limpia.
        FichaExcelExporter.crearLibroCompletado(context, data).use { libro ->
            val evaluador = libro.creationHelper.createFormulaEvaluator()
            runCatching { evaluador.evaluateAll() }
            ExcelPdfRenderer.render(context, libro, evaluador, salida)
        }
    }
}
