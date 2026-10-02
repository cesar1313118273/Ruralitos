package com.ruralitos.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.export.FichaExcelExporter
import com.ruralitos.app.data.export.FichaExportData
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.domain.GrupoEdadFamiliar
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * Comprueba en Android real que Apache POI puede abrir la plantilla y escribir el Excel de la ficha.
 * Antes fallaba con NoClassDefFoundError (javax.xml.stream) porque Android no incluye StAX.
 */
@RunWith(AndroidJUnit4::class)
class FichaExcelAndroidTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun laFichaSeExportaAExcelEnAndroid() {
        val ficha = FichaFamiliarEntity(
            cedulaJefeHogar = "1300000000",
            institucionSistema = "MSP",
            unidadOperativa = "CENTRO PRUEBA",
            codigoUo = "001234",
            areaNumero = "4",
            codigoLocalizacion = "010203",
            parroquiaCodigoLocalizacion = "03",
            cantonCodigoLocalizacion = "02",
            provinciaCodigoLocalizacion = "01",
            numeroFichaFamiliar = "1",
            provincia = "MANABI",
            canton = "PEDERNALES",
            parroquia = "COJIMIES",
            sector = "RURAL",
            manzana = "1",
            numeroFamilia = "2",
            direccionHabitualFamilia = "DIRECCION PRUEBA",
            barrio = "BARRIO",
            numeroCasa = "3",
            comunidad = "COMUNIDAD",
            grupoCultural = "MESTIZO",
            nombreApellidoJefeFamilia = "PERSONA PRUEBA",
            numeroTelefono = "0999999999",
            fechaLlenado = "02/10/2026",
            numeroCarpeta = "7",
            responsableNombre = "PROFESIONAL PRUEBA",
            responsableCodigo = "1016.2025-3143718"
        )
        val miembro = MiembroFamiliaEntity(
            fichaId = 1,
            grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO,
            apellidosNombres = "PERSONA PRUEBA",
            parentesco = "JEFE/A DE FAMILIA",
            fechaNacimiento = "01/01/1985",
            ocupacion = "AGRICULTOR",
            sexo = "H",
            escolaridad = "BAS",
            cedula = "1300000000",
            numeroHistoriaClinica = "1300000000"
        )
        val datos = FichaExportData(
            ficha = ficha,
            miembros = listOf(miembro),
            embarazadas = emptyList(),
            mortalidad = emptyList(),
            calificaciones = emptyList(),
            gestion = emptyList(),
            contaminacion = emptyList(),
            lugaresTratamiento = emptyList(),
            adjuntos = emptyList()
        )

        val salida = ByteArrayOutputStream()
        FichaExcelExporter.crearLibroCompletado(context, datos).use { it.write(salida) }
        assertTrue(salida.size() > 5_000)
    }
}
