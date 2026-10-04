package com.ruralitos.app

import android.util.Log
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.domain.GrupoEdadFamiliar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Rendimiento con una Sala grande: 5.000 fichas de 4 integrantes. Los límites son holgados (el emulador es más lento
 * que un teléfono actual) y sirven para detectar regresiones graves, no para medir con precisión.
 */
@RunWith(AndroidJUnit4::class)
class CargaMasivaTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = RuralitosDatabase.obtenerBaseDatos(context)
    private val total = 5_000

    @After
    fun limpiar() {
        database.openHelper.writableDatabase.apply {
            execSQL("DELETE FROM miembros_familia WHERE fichaId IN (SELECT id FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-CARGA-%')")
            execSQL("DELETE FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-CARGA-%'")
            execSQL("DELETE FROM eliminaciones_sync WHERE organizacionId = 'org-carga'")
        }
    }

    private fun medir(nombre: String, limiteMs: Long, bloque: suspend () -> Unit) {
        val ms = measureTimeMillis { runBlocking { bloque() } }
        Log.i("CargaMasiva", "$nombre: $ms ms")
        assertTrue("$nombre tardó $ms ms (límite $limiteMs ms)", ms <= limiteMs)
    }

    @Test
    fun cincoMilFichasSeBuscanYSeListanConFluidez() {
        val fichas = database.fichaFamiliarDao()
        val contenido = database.fichaContenidoDao()
        medir("crear $total fichas con 4 integrantes", 180_000) {
            database.withTransaction {
                repeat(total) { n ->
                    val id = fichas.guardarFicha(
                        FichaFamiliarEntity(
                            cedulaJefeHogar = "9%09d".format(n), institucionSistema = "", unidadOperativa = "CARGA",
                            codigoUo = "C1", areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1",
                            cantonCodigoLocalizacion = "1", provinciaCodigoLocalizacion = "1",
                            numeroFichaFamiliar = "QA-CARGA-$n", provincia = "P", canton = "C", parroquia = "R",
                            sector = "SECTOR ${n % 40}", manzana = "1", numeroFamilia = "1", direccionHabitualFamilia = "D",
                            barrio = "BARRIO ${n % 25}", numeroCasa = "$n", comunidad = "COMUNIDAD ${n % 60}",
                            grupoCultural = "MESTIZO", nombreApellidoJefeFamilia = "APELLIDO$n NOMBRE$n",
                            numeroTelefono = "0", fechaLlenado = "%02d/%02d/2026".format(1 + n % 28, 1 + n % 12),
                            numeroCarpeta = "$n", responsableNombre = "QA", responsableCodigo = "1",
                            syncEstado = "SINCRONIZADO", organizacionId = "org-carga"
                        )
                    )
                    repeat(4) { k ->
                        contenido.guardarMiembro(
                            MiembroFamiliaEntity(
                                fichaId = id, grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO,
                                apellidosNombres = "PERSONA$n-$k", parentesco = if (k == 0) "JEFE/A DE FAMILIA" else "HIJO/A",
                                fechaNacimiento = "01/01/1990", ocupacion = "", sexo = "H", escolaridad = "BAS",
                                cedula = "8%05d%02d".format(n, k)
                            )
                        )
                    }
                }
            }
        }
        assertEquals(total, runBlocking { fichas.listarFichas().first().count { it.numeroFichaFamiliar.startsWith("QA-CARGA-") } })

        medir("búsqueda por nombre de un integrante", 10_000) {
            val r = fichas.buscarFichasFiltradasPaginadas("PERSONA4321-2", "", "", "", "", "ACTIVAS", "TODAS", 20, 0).first()
            assertEquals(1, r.size)
        }
        medir("búsqueda por texto frecuente, primera página", 10_000) {
            assertEquals(20, fichas.buscarFichasFiltradasPaginadas("COMUNIDAD 7", "", "", "", "", "ACTIVAS", "TODAS", 20, 0).first().size)
        }
        medir("conteo de resultados de la búsqueda", 10_000) {
            assertTrue(fichas.contarFichasFiltradas("COMUNIDAD 3", "", "", "", "", "ACTIVAS", "TODAS").first() > 0)
        }
        medir("resumen para estadísticas", 8_000) { assertTrue(fichas.observarResumen().first().total >= total) }
        medir("fichas de una fecha", 8_000) { fichas.observarPorFecha("05/03/2026").first() }
        medir("pendientes de subir de la Sala", 8_000) { database.sincronizacionDao().fichasPendientes("org-carga") }
        medir("todos los integrantes (registro general)", 8_000) { assertTrue(contenido.listarTodosMiembros().first().size >= total * 4) }
    }
}
