package com.ruralitos.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.data.security.SesionSupabase
import com.ruralitos.app.data.security.SesionSupabaseCifrada
import com.ruralitos.app.data.sync.MarcasDescarga
import com.ruralitos.app.data.sync.SincronizadorSupabase
import com.ruralitos.app.domain.GrupoEdadFamiliar
import com.ruralitos.app.pruebas.ServidorSupabaseFalso
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * La sincronización de punta a punta contra un servidor de mentira que imita a Supabase (ver [ServidorSupabaseFalso]).
 * «Otra persona» = la prueba cambia directamente las filas del servidor. Cubre lo que antes solo se había razonado:
 * subida, bajada incremental, conflictos, adjuntos reemplazados, bajas, retiro de acceso y paginación.
 */
@RunWith(AndroidJUnit4::class)
class SincronizacionConServidorFalsoTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = RuralitosDatabase.obtenerBaseDatos(context)
    private val dao = database.sincronizacionDao()
    private val org = "org-sync-qa"
    private val usuario = "usuario-sync-qa"
    private lateinit var servidor: ServidorSupabaseFalso
    private val archivos = mutableListOf<File>()

    @Before
    fun preparar() {
        limpiarLocal()
        servidor = ServidorSupabaseFalso()
        servidor.sembrarSala(org, usuario)
        SupabaseApi.urlParaPruebas = servidor.url
        SesionSupabaseCifrada(context).apply {
            guardar(SesionSupabase("token-qa", "refresh-qa", System.currentTimeMillis() / 1000 + 3600, usuario, "qa@example.com"))
            guardarOrganizacion(org)
        }
        MarcasDescarga.borrarTodo(context)
    }

    @After
    fun terminar() {
        SupabaseApi.urlParaPruebas = null
        servidor.close()
        archivos.forEach { it.delete() }
        limpiarLocal()
        MarcasDescarga.borrarTodo(context)
        SesionSupabaseCifrada(context).limpiar()
    }

    private fun limpiarLocal() {
        database.openHelper.writableDatabase.apply {
            execSQL("DELETE FROM miembros_familia WHERE fichaId IN (SELECT id FROM fichas_familiares WHERE organizacionId = '$org')")
            execSQL("DELETE FROM fichas_familiares WHERE organizacionId = '$org'")
            execSQL("DELETE FROM eliminaciones_sync WHERE organizacionId = '$org'")
            execSQL("DELETE FROM salas WHERE organizacionId = '$org'")
        }
        File(context.filesDir, "adjuntos_sincronizados/$org").deleteRecursively()
    }

    private fun fichaLocal(syncId: String, estado: String = "PENDIENTE", jefe: String = "LOCAL") = FichaFamiliarEntity(
        cedulaJefeHogar = "0000000010", institucionSistema = "", unidadOperativa = "QA", codigoUo = "1",
        areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1",
        provinciaCodigoLocalizacion = "1", numeroFichaFamiliar = "QA-SYNC-$syncId",
        provincia = "P", canton = "C", parroquia = "R", sector = "S", manzana = "1", numeroFamilia = "1",
        direccionHabitualFamilia = "D", barrio = "B", numeroCasa = "1", comunidad = "C", grupoCultural = "MESTIZO",
        nombreApellidoJefeFamilia = jefe, numeroTelefono = "0", fechaLlenado = "01/01/2026", numeroCarpeta = "1",
        responsableNombre = "QA", responsableCodigo = "1", syncId = syncId, syncEstado = estado, organizacionId = org
    )

    private fun miembro(fichaId: Long, nombre: String) = MiembroFamiliaEntity(
        fichaId = fichaId, grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO, apellidosNombres = nombre,
        parentesco = "HIJO/A", fechaNacimiento = "01/01/2000", ocupacion = "", sexo = "H", escolaridad = "BAS"
    )

    private fun sincronizar() = runBlocking { SincronizadorSupabase(context).ejecutar() }

    private suspend fun crearFichaConIntegrante(nombre: String = "INTEGRANTE UNO"): Pair<String, Long> {
        val syncId = UUID.randomUUID().toString()
        val id = dao.guardarFichaRemota(fichaLocal(syncId))
        database.fichaContenidoDao().guardarMiembro(miembro(id, nombre))
        return syncId to id
    }

    private fun archivo(nombre: String, contenido: ByteArray) =
        File(context.filesDir, nombre).apply { writeBytes(contenido) }.also { archivos += it }

    @Test
    fun laFichaNuevaSeSubeConSusDatosYQuedaSincronizada() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()

        val resultado = sincronizar()

        assertEquals(0, resultado.errores)
        assertEquals(1, resultado.subidas)
        val remota = servidor.fila("fichas_familiares", syncId)
        assertNotNull("la ficha debe existir en el servidor", remota)
        assertEquals(1L, remota!!.getLong("version"))
        assertEquals(org, remota.getString("organizacion_id"))
        assertEquals(1, servidor.filas("miembros_familia").size)
        val local = dao.fichaPorSyncId(syncId)!!
        assertEquals("SINCRONIZADO", local.syncEstado)
        assertEquals(1L, local.syncVersion)
        assertEquals(1, dao.miembros(id).size)
    }

    @Test
    fun loQueOtraPersonaCambiaLlegaEnUnaDescargaIncremental() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()
        sincronizar()
        val miembroId = servidor.filas("miembros_familia").single().getString("id")
        // otra persona edita un integrante y, como hace la app, vuelve a guardar la cabecera
        servidor.modificar("miembros_familia", miembroId) { put("apellidos_nombres", "CAMBIADO POR OTRA PERSONA") }
        servidor.modificar("fichas_familiares", syncId) { put("numero_telefono", "0987654321") }
        servidor.peticiones.clear()

        val resultado = sincronizar()

        assertEquals(0, resultado.errores)
        assertEquals("CAMBIADO POR OTRA PERSONA", dao.miembros(id).single().apellidosNombres)
        assertEquals("0987654321", dao.fichaPorSyncId(syncId)!!.numeroTelefono)
        assertEquals("SINCRONIZADO", dao.fichaPorSyncId(syncId)!!.syncEstado)
        assertTrue("la segunda descarga debe pedir solo lo nuevo",
            servidor.peticiones.any { it.startsWith("GET /rest/v1/miembros_familia") && it.contains("updated_at=gte.") })
    }

    @Test
    fun losFactoresDeRiesgoPorEdadYLosCriteriosObstetricosViajanEnAmbosSentidos() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()
        val miembro = dao.miembros(id).single()
        database.fichaContenidoDao().actualizarMiembro(miembro.copy(factoresRiesgoEdadJson = "[\"SEDENTARISMO\",\"VIOLENCIA\"]"))
        database.fichaContenidoDao().guardarEmbarazada(
            com.ruralitos.app.data.local.entity.EmbarazadaEntity(
                fichaId = id, apellidosNombres = "INTEGRANTE UNO", fechaUltimaMenstruacion = "01/03/2026",
                fechaProbableParto = "08/12/2026", riesgoObstetrico = "ALTO", factoresObstetricosJson = "[\"ANEMIA\"]"
            )
        )
        assertEquals(0, sincronizar().errores)
        val remotoMiembro = servidor.filas("miembros_familia").single()
        assertEquals("[\"SEDENTARISMO\",\"VIOLENCIA\"]", remotoMiembro.getString("factores_riesgo_edad_json"))
        assertEquals("[\"ANEMIA\"]", servidor.filas("embarazadas").single().getString("factores_obstetricos_json"))

        // otra persona cambia lo marcado y el teléfono lo recibe
        servidor.modificar("miembros_familia", remotoMiembro.getString("id")) { put("factores_riesgo_edad_json", "[\"FRAGILIDAD\"]") }
        servidor.modificar("embarazadas", servidor.filas("embarazadas").single().getString("id")) { put("factores_obstetricos_json", "[\"EPILEPSIA\"]") }
        servidor.modificar("fichas_familiares", syncId) { put("numero_telefono", "0911111111") }
        assertEquals(0, sincronizar().errores)
        assertEquals("[\"FRAGILIDAD\"]", dao.miembros(id).single().factoresRiesgoEdadJson)
        assertEquals("[\"EPILEPSIA\"]", dao.embarazadas(id).single().factoresObstetricosJson)
    }

    @Test
    fun sinLaColumnaNuevaEnElServidorTodoLoDemasSigueSincronizandoYLoMarcadoSeConserva() = runBlocking {
        servidor.columnasInexistentes = setOf("factores_riesgo_edad_json", "factores_obstetricos_json")
        val (syncId, id) = crearFichaConIntegrante()
        database.fichaContenidoDao().actualizarMiembro(dao.miembros(id).single().copy(factoresRiesgoEdadJson = "[\"SEDENTARISMO\"]"))

        val resultado = sincronizar()

        assertEquals("la falta de la columna no es un error", 0, resultado.errores)
        assertEquals("SINCRONIZADO", dao.fichaPorSyncId(syncId)!!.syncEstado)
        assertEquals(1, servidor.filas("miembros_familia").size)
        assertFalse(servidor.filas("miembros_familia").single().has("factores_riesgo_edad_json"))
        // una descarga completa no borra lo que solo existe en este teléfono
        MarcasDescarga.borrarTodo(context)
        servidor.modificar("fichas_familiares", syncId) { put("numero_telefono", "0922222222") }
        assertEquals(0, sincronizar().errores)
        assertEquals("[\"SEDENTARISMO\"]", dao.miembros(id).single().factoresRiesgoEdadJson)
    }

    @Test
    fun losSimbolosYTextosDelCroquisViajanEnAmbosSentidos() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()
        val json = "[{\"id\":\"a1\",\"tipo\":\"SIMBOLO\",\"codigo\":\"IGLESIA\",\"texto\":\"Iglesia\",\"lat\":-0.22,\"lon\":-78.51}]"
        database.fichaFamiliarDao().guardarElementosCroquis(id, json)
        assertEquals(0, sincronizar().errores)
        assertEquals(json, servidor.fila("fichas_familiares", syncId)!!.getString("croquis_elementos_json"))

        val otro = "[{\"id\":\"b2\",\"tipo\":\"TEXTO\",\"codigo\":\"\",\"texto\":\"Camino al rio\",\"lat\":-0.2,\"lon\":-78.5}]"
        servidor.modificar("fichas_familiares", syncId) { put("croquis_elementos_json", otro) }
        assertEquals(0, sincronizar().errores)
        assertEquals(otro, dao.fichaPorSyncId(syncId)!!.croquisElementosJson)
    }

    @Test
    fun sinLaColumnaDeSimbolosEnElServidorLaFichaSigueSincronizandoYLosSimbolosSeConservan() = runBlocking {
        servidor.columnasInexistentes = setOf("croquis_elementos_json")
        val (syncId, id) = crearFichaConIntegrante()
        val json = "[{\"id\":\"a1\",\"tipo\":\"SIMBOLO\",\"codigo\":\"ESCUELA\",\"texto\":\"Escuela\",\"lat\":-0.22,\"lon\":-78.51}]"
        database.fichaFamiliarDao().guardarElementosCroquis(id, json)

        assertEquals("la falta de la columna no es un error", 0, sincronizar().errores)
        assertEquals("SINCRONIZADO", dao.fichaPorSyncId(syncId)!!.syncEstado)
        assertFalse(servidor.fila("fichas_familiares", syncId)!!.has("croquis_elementos_json"))
        // una descarga completa no borra lo que solo existe en este teléfono
        MarcasDescarga.borrarTodo(context)
        servidor.modificar("fichas_familiares", syncId) { put("numero_telefono", "0933333333") }
        assertEquals(0, sincronizar().errores)
        assertEquals(json, dao.fichaPorSyncId(syncId)!!.croquisElementosJson)
    }

    @Test
    fun siOtraPersonaCambioLaFichaMientrasSeEditabaGanaLoMasRecienteYSeUnenLosRegistrosNuevos() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()
        sincronizar()
        // yo agrego un integrante (queda pendiente) ...
        database.fichaContenidoDao().guardarMiembro(miembro(id, "MI EDICION"))
        Thread.sleep(20)
        // ... y después otra persona cambia la cabecera en el servidor (su cambio es más reciente)
        servidor.modificar("fichas_familiares", syncId) { put("nombre_apellido_jefe_familia", "CAMBIO DEL EQUIPO") }

        val resultado = sincronizar()

        assertEquals("no se pregunta nada: se resuelve solo", 0, resultado.errores)
        assertEquals("SINCRONIZADO", dao.fichaPorSyncId(syncId)!!.syncEstado)
        assertEquals("gana el cambio más reciente", "CAMBIO DEL EQUIPO", dao.fichaPorSyncId(syncId)!!.nombreApellidoJefeFamilia)
        assertEquals("CAMBIO DEL EQUIPO", servidor.fila("fichas_familiares", syncId)!!.getString("nombre_apellido_jefe_familia"))
        assertTrue("mi integrante nuevo se suma", servidor.filas("miembros_familia").any { it.getString("apellidos_nombres") == "MI EDICION" })
        assertEquals(2, dao.miembros(id).size)
    }

    @Test
    fun siMiCambioEsElMasRecienteSeSubeEncimaDelServidor() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()
        sincronizar()
        servidor.modificar("fichas_familiares", syncId) { put("nombre_apellido_jefe_familia", "CAMBIO DEL EQUIPO") }
        Thread.sleep(20)
        database.fichaContenidoDao().guardarMiembro(miembro(id, "MI EDICION"))

        val resultado = sincronizar()

        assertEquals(0, resultado.errores)
        assertEquals("SINCRONIZADO", dao.fichaPorSyncId(syncId)!!.syncEstado)
        assertTrue(servidor.filas("miembros_familia").any { it.getString("apellidos_nombres") == "MI EDICION" })
        assertEquals("LOCAL", servidor.fila("fichas_familiares", syncId)!!.getString("nombre_apellido_jefe_familia"))
    }

    @Test
    fun unaFichaEliminadaPorSuAutorSeQuitaTambienDelTelefonoAunqueTengaCambios() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()
        sincronizar()
        database.fichaContenidoDao().guardarMiembro(miembro(id, "MI EDICION"))
        servidor.modificar("fichas_familiares", syncId) { put("deleted_at", "2026-01-01T00:00:00Z") }

        sincronizar()

        assertEquals(null, dao.fichaPorSyncId(syncId))
    }

    @Test
    fun reemplazarUnAdjuntoNoChocaConLaRestriccionUnicaDelServidor() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()
        val primero = archivo("qa_sync_croquis_1.png", ByteArray(300) { 1 })
        database.fichaContenidoDao().guardarAdjunto(AdjuntoFichaEntity(fichaId = id, tipo = "CROQUIS", uri = android.net.Uri.fromFile(primero).toString()))
        assertEquals(0, sincronizar().errores)
        val idRemoto = servidor.filas("adjuntos_ficha").single().getString("id")

        // el usuario reemplaza la imagen: la fila local se sustituye y nace con otro identificador
        val segundo = archivo("qa_sync_croquis_2.png", ByteArray(500) { 2 })
        database.fichaContenidoDao().guardarAdjunto(AdjuntoFichaEntity(fichaId = id, tipo = "CROQUIS", uri = android.net.Uri.fromFile(segundo).toString()))
        assertEquals("PENDIENTE", dao.fichaPorSyncId(syncId)!!.syncEstado)

        val resultado = sincronizar()

        assertEquals("no debe quedar la ficha en error", 0, resultado.errores)
        val filas = servidor.filas("adjuntos_ficha")
        assertEquals(1, filas.size)
        assertEquals("se reutiliza el identificador del servidor", idRemoto, filas.single().getString("id"))
        assertTrue(filas.single().isNull("deleted_at"))
        assertEquals(500, servidor.archivo(filas.single().getString("storage_path"))!!.size)
        assertEquals("SINCRONIZADO", dao.fichaPorSyncId(syncId)!!.syncEstado)
    }

    @Test
    fun laFichaQueElEquipoDioDeBajaDesapareceDelTelefono() = runBlocking {
        val (syncId, _) = crearFichaConIntegrante()
        sincronizar()
        servidor.modificar("fichas_familiares", syncId) { put("deleted_at", "2026-10-03T10:00:00+00:00") }

        sincronizar()

        assertNull(dao.fichaPorSyncId(syncId))
    }

    @Test
    fun alEliminarUnaFichaSincronizadaSeDaDeBajaYSeBorranSusArchivosDelServidor() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()
        val imagen = archivo("qa_sync_baja.png", ByteArray(200) { 3 })
        database.fichaContenidoDao().guardarAdjunto(AdjuntoFichaEntity(fichaId = id, tipo = "CROQUIS", uri = android.net.Uri.fromFile(imagen).toString()))
        sincronizar()
        assertEquals(1, servidor.rutasDeArchivos().size)

        database.fichaFamiliarDao().eliminarFicha(dao.fichaPorSyncId(syncId)!!)
        val resultado = sincronizar()

        assertEquals(0, resultado.errores)
        assertFalse("la baja debe llegar al servidor", servidor.fila("fichas_familiares", syncId)!!.isNull("deleted_at"))
        assertTrue("los archivos no deben quedar huérfanos", servidor.rutasDeArchivos().isEmpty())
    }

    @Test
    fun siLeQuitanElAccesoLasFichasSincronizadasSeRetiranPeroLasPendientesSeConservan() = runBlocking {
        val (sincronizada, _) = crearFichaConIntegrante("YA SINCRONIZADA")
        sincronizar()
        val (pendiente, _) = crearFichaConIntegrante("TRABAJO SIN SUBIR")
        servidor.modificar("miembros_organizacion", "m-$org-$usuario") { put("activo", false) }

        sincronizar()

        assertNull("lo ya sincronizado se retira del teléfono", dao.fichaPorSyncId(sincronizada))
        assertNotNull("lo que no se alcanzó a subir se conserva", dao.fichaPorSyncId(pendiente))
        assertEquals("PENDIENTE", dao.fichaPorSyncId(pendiente)!!.syncEstado)
    }

    @Test
    fun unaFichaConMuchosIntegrantesSeDescargaCompletaSinSaltarFilas() = runBlocking {
        val (syncId, id) = crearFichaConIntegrante()
        sincronizar()
        repeat(1_100) { n ->
            servidor.insertar("miembros_familia", JSONObject()
                .put("id", UUID.randomUUID().toString()).put("organizacion_id", org).put("ficha_id", syncId)
                .put("grupo_edad", GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO).put("apellidos_nombres", "MASIVO $n")
                .put("parentesco", "HIJO/A").put("sexo", "H"))
        }
        servidor.modificar("fichas_familiares", syncId) { put("numero_telefono", "1") }

        assertEquals(0, sincronizar().errores)

        assertEquals(1_101, dao.miembros(id).size)
        assertEquals(1_101, dao.miembros(id).map { it.syncId }.toSet().size)
    }
}
