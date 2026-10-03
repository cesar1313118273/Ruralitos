package com.ruralitos.app

import com.ruralitos.app.data.export.PlantillaPropia
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class PlantillaPropiaTest {
    private fun fallaConMensaje(bytes: ByteArray, fragmento: String) {
        try {
            PlantillaPropia.validar(bytes)
            fail("debía rechazar la plantilla")
        } catch (esperado: IllegalArgumentException) {
            assertTrue(esperado.message, esperado.message!!.contains(fragmento))
        }
    }

    @Test
    fun laPlantillaDeLaAppEsValida() {
        val plantilla = listOf(File("src/main/assets/ficha_familiar.xlsx"), File("app/src/main/assets/ficha_familiar.xlsx"))
            .first { it.isFile }
        PlantillaPropia.validar(plantilla.readBytes())
    }

    @Test
    fun rechazaUnArchivoQueNoEsExcel() = fallaConMensaje("hola".toByteArray(), "no es un Excel")

    @Test
    fun rechazaUnLibroSinLasCuatroHojas() {
        val salida = ByteArrayOutputStream()
        XSSFWorkbook().use { libro -> libro.createSheet("1"); libro.createSheet("2"); libro.write(salida) }
        fallaConMensaje(salida.toByteArray(), "Faltan: 3, 4")
    }
}
