package com.ruralitos.app

import com.ruralitos.app.data.export.RegistroGeneralExcelExporter
import com.ruralitos.app.domain.ColumnaRegistroComunitario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

class RegistroComunitarioExcelExporterTest {
    @Test
    fun `rellena ambas hojas del registro general`() {
        val plantilla = listOf(
            File(System.getProperty("user.dir"), "app/src/main/assets/REGISTROGENERAL.xlsx"),
            File(System.getProperty("user.dir"), "src/main/assets/REGISTROGENERAL.xlsx")
        ).first { it.exists() }.readBytes()
        val salida = ByteArrayOutputStream()
        val fila = MutableList<Any?>(47) { null }.apply { this[0] = "Centro de prueba"; this[13] = "ANA" }
        RegistroGeneralExcelExporter.exportarFilasPrueba(
            ByteArrayInputStream(plantilla),
            salida,
            listOf(ColumnaRegistroComunitario("Barrio A", mapOf(3 to 7, 42 to 2))),
            listOf(fila)
        )

        val original = entradasZip(plantilla)
        val generado = entradasZip(salida.toByteArray())
        val hojaUno = generado.getValue("xl/worksheets/sheet1.xml").decodeToString()
        val libro = generado.getValue("xl/workbook.xml").decodeToString()

        assertTrue(hojaUno.contains("Barrio A"))
        assertTrue(hojaUno.contains(">7</"))
        assertTrue(hojaUno.contains(">2</"))
        assertTrue(generado.getValue("xl/worksheets/sheet2.xml").decodeToString().contains("Centro de prueba"))
        assertEquals(original.keys, generado.keys)
        assertTrue(libro.contains("fullCalcOnLoad=\"1\""))
        assertTrue(libro.contains("forceFullCalc=\"1\""))
    }

    private fun entradasZip(bytes: ByteArray): Map<String, ByteArray> = buildMap {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entrada = zip.nextEntry
            while (entrada != null) {
                put(entrada.name, zip.readBytes())
                zip.closeEntry()
                entrada = zip.nextEntry
            }
        }
    }
}
