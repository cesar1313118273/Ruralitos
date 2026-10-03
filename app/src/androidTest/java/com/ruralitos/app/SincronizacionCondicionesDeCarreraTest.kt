package com.ruralitos.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.sync.AplicadorDescarga
import com.ruralitos.app.data.sync.DatosFichaRemota
import com.ruralitos.app.domain.GrupoEdadFamiliar
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * La descarga de una ficha es atómica: o se aplica completa (ficha + datos hijos + estado SINCRONIZADO) o se omite
 * porque el teléfono tiene cambios sin subir. Estas pruebas cubren el defecto que perdía ediciones hechas mientras
 * se sincronizaba, y el de las fichas que el servidor da de baja.
 */
@RunWith(AndroidJUnit4::class)
class SincronizacionCondicionesDeCarreraTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = RuralitosDatabase.obtenerBaseDatos(context)
    private val sync = database.sincronizacionDao()
    private val org = "org-qa"
    private val aplicador = AplicadorDescarga(database) { }

    private fun limpiar() {
        database.openHelper.writableDatabase.apply {
            execSQL("DELETE FROM miembros_familia WHERE fichaId IN (SELECT id FROM fichas_familiares WHERE organizacionId = '$org')")
            execSQL("DELETE FROM fichas_familiares WHERE organizacionId = '$org'")
            execSQL("DELETE FROM eliminaciones_sync WHERE organizacionId = '$org'")
        }
    }

    @Before fun antes() = limpiar()
    @After fun despues() = limpiar()

    private fun fichaLocal(syncId: String, estado: String = "SINCRONIZADO", version: Long = 3) = FichaFamiliarEntity(
        cedulaJefeHogar = "0000000002", institucionSistema = "CLINICA DE PRUEBA", unidadOperativa = "QA", codigoUo = "1",
        areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1",
        provinciaCodigoLocalizacion = "1", numeroFichaFamiliar = "QA-CARRERA-$syncId",
        provincia = "P", canton = "C", parroquia = "R", sector = "S", manzana = "1", numeroFamilia = "1",
        direccionHabitualFamilia = "D", barrio = "B", numeroCasa = "1", comunidad = "C", grupoCultural = "MESTIZO",
        nombreApellidoJefeFamilia = "LOCAL", numeroTelefono = "0", fechaLlenado = "01/01/2026", numeroCarpeta = "1",
        responsableNombre = "QA", responsableCodigo = "1",
        syncId = syncId, syncEstado = estado, syncVersion = version, organizacionId = org
    )

    private fun cabeceraRemota(syncId: String, version: Long, jefe: String) = JSONObject()
        .put("id", syncId).put("version", version).put("updated_at", "2026-10-02T10:00:00.000+00:00")
        .put("numero_ficha_familiar", "QA-CARRERA-$syncId").put("nombre_apellido_jefe_familia", jefe)
        .put("fecha_llenado", "2026-01-01").put("estado", "BORRADOR")

    private fun miembroRemoto(id: String, fichaSyncId: String, nombre: String, eliminado: Boolean = false) = JSONObject()
        .put("id", id).put("ficha_id", fichaSyncId).put("grupo_edad", GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO)
        .put("apellidos_nombres", nombre).put("parentesco", "HIJO/A").put("sexo", "H")
        .put("deleted_at", if (eliminado) "2026-10-02T10:00:00+00:00" else JSONObject.NULL)

    private fun miembroLocal(fichaId: Long, nombre: String) = MiembroFamiliaEntity(
        fichaId = fichaId, grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO,
        apellidosNombres = nombre, parentesco = "HIJO/A", fechaNacimiento = "01/01/2000",
        ocupacion = "", sexo = "H", escolaridad = "BAS"
    )

    @Test
    fun laEdicionHechaAntesDeAplicarLaDescargaNoSePisaNiSePierde() = runBlocking {
        val syncId = UUID.randomUUID().toString()
        val fichaId = sync.guardarFichaRemota(fichaLocal(syncId))
        // El profesional guarda un integrante mientras la sincronización traía datos de la red.
        database.fichaContenidoDao().guardarMiembro(miembroLocal(fichaId, "EDITADO DURANTE LA SINCRONIZACION"))
        assertEquals("PENDIENTE", sync.fichaPorSyncId(syncId)!!.syncEstado)

        val aplicada = aplicador.aplicar(org, DatosFichaRemota(syncId, cabeceraRemota(syncId, 4, "SERVIDOR")), completo = true)

        assertFalse("una ficha con cambios sin subir no se pisa", aplicada)
        val despues = sync.fichaPorSyncId(syncId)!!
        assertEquals("PENDIENTE", despues.syncEstado)
        assertEquals("LOCAL", despues.nombreApellidoJefeFamilia)
        assertEquals(1, sync.miembros(fichaId).size)
        assertTrue(sync.fichasPendientes(org).any { it.syncId == syncId })
    }

    @Test
    fun descargaCompletaAplicaTodoYDejaLaFichaSincronizada() = runBlocking {
        val syncId = UUID.randomUUID().toString()
        val fichaId = sync.guardarFichaRemota(fichaLocal(syncId))
        val sobrante = miembroLocal(fichaId, "YA NO EXISTE EN EL SERVIDOR")
        // Los datos locales que el servidor ya no tiene se limpian solo en una descarga completa
        sync.marcarDescargando(fichaId)
        sync.guardarMiembroRemoto(sobrante.copy(syncId = "sobrante-$syncId"))
        sync.marcarSincronizadaDescarga(fichaId, 3)

        val datos = DatosFichaRemota(
            syncId, cabeceraRemota(syncId, 4, "SERVIDOR"),
            miembros = listOf(miembroRemoto("m-$syncId", syncId, "NUEVO DEL SERVIDOR"))
        )
        assertTrue(aplicador.aplicar(org, datos, completo = true))

        val despues = sync.fichaPorSyncId(syncId)!!
        assertEquals("SINCRONIZADO", despues.syncEstado)
        assertEquals(4L, despues.syncVersion)
        assertEquals("SERVIDOR", despues.nombreApellidoJefeFamilia)
        assertEquals(listOf("NUEVO DEL SERVIDOR"), sync.miembros(fichaId).map { it.apellidosNombres })
        assertTrue("aplicar una descarga no debe generar cambios pendientes", sync.fichasPendientes(org).none { it.syncId == syncId })

        // y a partir de ahora, una edición del profesional sí vuelve a dejarla pendiente
        database.fichaContenidoDao().guardarMiembro(miembroLocal(fichaId, "OTRO"))
        assertEquals("PENDIENTE", sync.fichaPorSyncId(syncId)!!.syncEstado)
    }

    @Test
    fun descargaIncrementalRespetaLoQueNoVieneYBorraLoMarcadoComoEliminado() = runBlocking {
        val syncId = UUID.randomUUID().toString()
        val fichaId = sync.guardarFichaRemota(fichaLocal(syncId))
        sync.marcarDescargando(fichaId)
        sync.guardarMiembroRemoto(miembroLocal(fichaId, "SE QUEDA").copy(syncId = "queda-$syncId"))
        sync.guardarMiembroRemoto(miembroLocal(fichaId, "SE BORRA").copy(syncId = "borra-$syncId"))
        sync.marcarSincronizadaDescarga(fichaId, 3)

        // Solo cambió un dato hijo: no viene la cabecera ni el integrante que no cambió.
        val datos = DatosFichaRemota(syncId, cabecera = null, miembros = listOf(
            miembroRemoto("borra-$syncId", syncId, "SE BORRA", eliminado = true)
        ))
        assertTrue(aplicador.aplicar(org, datos, completo = false))

        assertEquals(listOf("SE QUEDA"), sync.miembros(fichaId).map { it.apellidosNombres })
        val despues = sync.fichaPorSyncId(syncId)!!
        assertEquals("SINCRONIZADO", despues.syncEstado)
        assertEquals(3L, despues.syncVersion)
        assertEquals("LOCAL", despues.nombreApellidoJefeFamilia)
    }

    @Test
    fun laFichaQueElServidorDioDeBajaSeRetiraDelTelefonoSinGenerarEliminacionPendiente() = runBlocking {
        val syncId = UUID.randomUUID().toString()
        val fichaId = sync.guardarFichaRemota(fichaLocal(syncId))
        sync.marcarDescargando(fichaId)
        sync.guardarMiembroRemoto(miembroLocal(fichaId, "HIJO").copy(syncId = "m-$syncId"))
        sync.marcarSincronizadaDescarga(fichaId, 3)

        aplicador.eliminarFichas(org, setOf(syncId))

        assertNull(sync.fichaPorSyncId(syncId))
        assertTrue(sync.eliminaciones(org).isEmpty())
    }

    @Test
    fun laFichaConCambiosPropiosNoSeRetiraAunqueElServidorLaDeDeBaja() = runBlocking {
        val syncId = UUID.randomUUID().toString()
        sync.guardarFichaRemota(fichaLocal(syncId, estado = "PENDIENTE"))

        aplicador.eliminarFichas(org, setOf(syncId))

        assertEquals("PENDIENTE", sync.fichaPorSyncId(syncId)!!.syncEstado)
    }

    @Test
    fun alElegirElServidorTrasUnConflictoSePisanLosCambiosLocales() = runBlocking {
        val syncId = UUID.randomUUID().toString()
        val fichaId = sync.guardarFichaRemota(fichaLocal(syncId, estado = "CONFLICTO"))
        database.fichaContenidoDao().guardarMiembro(miembroLocal(fichaId, "SOLO LOCAL"))

        val datos = DatosFichaRemota(
            syncId, cabeceraRemota(syncId, 9, "SERVIDOR"),
            miembros = listOf(miembroRemoto("m-$syncId", syncId, "DEL SERVIDOR"))
        )
        assertFalse("sin forzar, el conflicto no se pisa", aplicador.aplicar(org, datos, completo = true))
        assertTrue(aplicador.aplicar(org, datos, completo = true, forzar = true))

        assertEquals("SINCRONIZADO", sync.fichaPorSyncId(syncId)!!.syncEstado)
        assertEquals(listOf("DEL SERVIDOR"), sync.miembros(fichaId).map { it.apellidosNombres })
    }

    @Test
    fun alConservarLosCambiosLocalesTrasUnConflictoLaFichaVuelveAPendienteConLaVersionDelServidor() = runBlocking {
        val syncId = UUID.randomUUID().toString()
        val fichaId = sync.guardarFichaRemota(fichaLocal(syncId, estado = "CONFLICTO", version = 3))

        assertEquals(1, sync.conservarLocalTrasConflicto(fichaId, 9))

        val despues = sync.fichaPorSyncId(syncId)!!
        assertEquals("PENDIENTE", despues.syncEstado)
        assertEquals(9L, despues.syncVersion)
        assertTrue(sync.fichasPendientes(org).any { it.syncId == syncId })
    }

    @Test
    fun lasFichasEnConflictoNoSeSuben() = runBlocking {
        val syncId = UUID.randomUUID().toString()
        sync.guardarFichaRemota(fichaLocal(syncId, estado = "CONFLICTO"))
        assertTrue(sync.fichasPendientes(org).none { it.syncId == syncId })
    }
}
