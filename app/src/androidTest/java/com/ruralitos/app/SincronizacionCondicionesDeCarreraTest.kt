package com.ruralitos.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.domain.GrupoEdadFamiliar
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * REPRODUCCIÓN DE UN DEFECTO CONOCIDO (aún sin corregir): qué le pasa a lo que el usuario edita mientras
 * la sincronización está descargando esa misma ficha. Hoy la edición queda guardada en el teléfono pero la
 * ficha se marca como SINCRONIZADA, así que nunca se sube y la siguiente descarga la pisa.
 * Al corregir el sincronizador hay que invertir las aserciones finales (debe quedar PENDIENTE).
 */
@RunWith(AndroidJUnit4::class)
class SincronizacionCondicionesDeCarreraTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun fichaDePrueba(syncId: String) = FichaFamiliarEntity(
        cedulaJefeHogar = "0000000002", institucionSistema = "MSP", unidadOperativa = "QA", codigoUo = "1",
        areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1",
        provinciaCodigoLocalizacion = "1", numeroFichaFamiliar = "QA-CARRERA-$syncId",
        provincia = "P", canton = "C", parroquia = "R", sector = "S", manzana = "1", numeroFamilia = "1",
        direccionHabitualFamilia = "D", barrio = "B", numeroCasa = "1", comunidad = "C", grupoCultural = "MESTIZO",
        nombreApellidoJefeFamilia = "QA", numeroTelefono = "0", fechaLlenado = "01/01/2026", numeroCarpeta = "1",
        responsableNombre = "QA", responsableCodigo = "1",
        syncId = syncId, syncEstado = "SINCRONIZADO", organizacionId = "org-qa"
    )

    @Test
    fun loQueSeEditaMientrasSeDescargaLaFichaNuncaQuedaPendienteDeSubir() {
        val database = RuralitosDatabase.obtenerBaseDatos(context)
        val sync = database.sincronizacionDao()
        val syncId = UUID.randomUUID().toString()
        runBlocking {
            database.openHelper.writableDatabase.apply {
                execSQL("DELETE FROM miembros_familia WHERE fichaId IN (SELECT id FROM fichas_familiares WHERE organizacionId = 'org-qa')")
                execSQL("DELETE FROM fichas_familiares WHERE organizacionId = 'org-qa'")
                execSQL("DELETE FROM eliminaciones_sync WHERE organizacionId = 'org-qa'")
            }
            val fichaId = sync.guardarFichaRemota(fichaDePrueba(syncId))
            try {
                // 1) la sincronización empieza a descargar esta ficha (así deja su estado en el código real)
                sync.marcarDescargando(fichaId)
                val antes = sync.fichaPorSyncId(syncId)!!

                // 2) el profesional guarda un integrante en ese mismo momento
                database.fichaContenidoDao().guardarMiembro(
                    MiembroFamiliaEntity(
                        fichaId = fichaId, grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO,
                        apellidosNombres = "EDITADO DURANTE LA SINCRONIZACION", parentesco = "HIJO/A",
                        fechaNacimiento = "01/01/2000", ocupacion = "", sexo = "H", escolaridad = "BAS"
                    )
                )
                val durante = sync.fichaPorSyncId(syncId)!!
                println("ESTADO_DURANTE=${durante.syncEstado} actualizadoEnCambio=${durante.actualizadoEn != antes.actualizadoEn}")

                // 3) la sincronización termina y marca la ficha como sincronizada (como hace descargarDesdeSupabase)
                val marcadas = sync.marcarSincronizada(fichaId, 7, durante.actualizadoEn)
                val despues = sync.fichaPorSyncId(syncId)!!
                println("MARCADAS=$marcadas ESTADO_FINAL=${despues.syncEstado}")

                // El integrante existe en el teléfono, pero la ficha ya no figura como pendiente: no se subirá.
                assertEquals(1, sync.miembros(fichaId).size)
                assertEquals("SINCRONIZADO", despues.syncEstado)
                assertEquals(false, sync.fichasPendientes("org-qa").any { it.syncId == syncId })
            } finally {
                // limpieza total, sea cual sea el estado en que haya quedado la ficha de prueba
                val sql = database.openHelper.writableDatabase
                sql.execSQL("DELETE FROM miembros_familia WHERE fichaId = $fichaId")
                sql.execSQL("DELETE FROM fichas_familiares WHERE syncId = '$syncId'")
                sql.execSQL("DELETE FROM eliminaciones_sync WHERE organizacionId = 'org-qa'")
            }
        }
    }
}
