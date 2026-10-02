package com.ruralitos.app.data.export

import android.content.Context
import android.net.Uri
import com.ruralitos.app.domain.CalculadorRegistroComunitario
import com.ruralitos.app.domain.ColumnaRegistroComunitario
import com.ruralitos.app.domain.AgrupacionRegistroComunitario
import org.w3c.dom.Element
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Una selección y un archivo: Registro comunitario (hoja 1) y BASE (hoja 2). */
object RegistroGeneralExcelExporter {
    private const val PLANTILLA = "REGISTROGENERAL.xlsx"
    private const val HOJA_REGISTRO = "xl/worksheets/sheet1.xml"
    private const val HOJA_BASE = "xl/worksheets/sheet2.xml"
    private const val LIBRO = "xl/workbook.xml"
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"

    suspend fun exportar(
        context: Context,
        filtro: FiltroConsolidado,
        agrupaciones: List<AgrupacionRegistroComunitario>,
        destino: Uri
    ) {
        val (columnas, filas) = ConsolidadoExcelExporter.contenidoRegistroGeneral(context, filtro, agrupaciones)
        context.assets.open(PLANTILLA).use { entrada ->
            context.contentResolver.openOutputStream(destino, "w")?.use { salida ->
                exportarFlujo(entrada, salida, columnas, filas)
            } ?: error("No se pudo abrir el destino del registro general.")
        }
    }

    internal fun exportarFilasPrueba(
        entrada: InputStream,
        salida: OutputStream,
        columnas: List<ColumnaRegistroComunitario>,
        filas: List<List<Any?>>
    ) = exportarFlujo(entrada, salida, columnas, filas)

    private fun exportarFlujo(
        entrada: InputStream,
        salida: OutputStream,
        columnas: List<ColumnaRegistroComunitario>,
        filas: List<List<Any?>>
    ) {
        require(columnas.isNotEmpty()) { "Selecciona una familia, barrio o EAIS." }
        require(columnas.size <= CalculadorRegistroComunitario.MAXIMO_COLUMNAS) {
            "El archivo admite hasta ${CalculadorRegistroComunitario.MAXIMO_COLUMNAS} selecciones."
        }
        require(filas.isNotEmpty()) { "Las fichas seleccionadas no tienen integrantes para exportar." }
        require(filas.size <= 4999) { "El archivo admite hasta 4.999 personas." }
        require(filas.all { it.size == 47 }) { "Cada persona debe tener los 47 campos de BASE." }

        val procesadas = mutableSetOf<String>()
        ZipInputStream(entrada.buffered()).use { zipEntrada ->
            ZipOutputStream(salida.buffered()).use { zipSalida ->
                var zipItem = zipEntrada.nextEntry
                while (zipItem != null) {
                    zipSalida.putNextEntry(ZipEntry(zipItem.name).apply {
                        time = zipItem.time
                        comment = zipItem.comment
                    })
                    when (zipItem.name) {
                        HOJA_REGISTRO -> {
                            zipSalida.write(RegistroComunitarioExcelExporter.actualizarHoja(zipEntrada.readBytes(), columnas))
                            procesadas += HOJA_REGISTRO
                        }
                        HOJA_BASE -> {
                            zipSalida.write(ConsolidadoExcelExporter.actualizarBaseParaRegistroGeneral(zipEntrada.readBytes(), filas))
                            procesadas += HOJA_BASE
                        }
                        LIBRO -> {
                            zipSalida.write(forzarRecalculo(zipEntrada.readBytes()))
                            procesadas += LIBRO
                        }
                        else -> zipEntrada.copyTo(zipSalida)
                    }
                    zipSalida.closeEntry()
                    zipEntrada.closeEntry()
                    zipItem = zipEntrada.nextEntry
                }
            }
        }
        check(procesadas.containsAll(setOf(HOJA_REGISTRO, HOJA_BASE, LIBRO))) {
            "La plantilla REGISTROGENERAL.xlsx no contiene sus dos hojas o el libro."
        }
    }

    private fun forzarRecalculo(bytes: ByteArray): ByteArray {
        val documento = ExcelXmlSeguro.leer(bytes)
        val existente = documento.getElementsByTagNameNS(NS, "calcPr")
        val calculo = if (existente.length > 0) existente.item(0) as Element
            else documento.documentElement.appendChild(documento.createElementNS(NS, "calcPr")) as Element
        calculo.setAttribute("calcMode", "auto")
        calculo.setAttribute("fullCalcOnLoad", "1")
        calculo.setAttribute("forceFullCalc", "1")
        return ExcelXmlSeguro.serializar(documento)
    }
}
