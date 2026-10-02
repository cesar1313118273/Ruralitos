package com.ruralitos.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.export.RegistroGeneralExcelExporter
import com.ruralitos.app.domain.ColumnaRegistroComunitario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class ExcelExportAndroidTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun registroGeneralSeProcesaConElMotorXmlDeAndroid() {
        val registro = ByteArrayOutputStream()
        val fila = MutableList<Any?>(47) { null }.apply { this[0] = "Centro Android"; this[13] = "ANA" }
        context.assets.open("REGISTROGENERAL.xlsx").use { entrada ->
            RegistroGeneralExcelExporter.exportarFilasPrueba(
                entrada,
                registro,
                listOf(ColumnaRegistroComunitario("Barrio Android", mapOf(3 to 2, 42 to 1))),
                listOf(fila)
            )
        }
        verificarXlsx(registro.toByteArray(), 2)
    }

    private fun verificarXlsx(bytes: ByteArray, hojasEsperadas: Int) {
        assertTrue(bytes.size > 1_000)
        var hojas = 0
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entrada = zip.nextEntry
            while (entrada != null) {
                if (entrada.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml"))) hojas++
                zip.closeEntry()
                entrada = zip.nextEntry
            }
        }
        assertEquals(hojasEsperadas, hojas)
    }
}
