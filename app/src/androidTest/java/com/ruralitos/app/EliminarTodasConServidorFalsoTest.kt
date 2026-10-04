package com.ruralitos.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.fichas.EliminadorFichas
import com.ruralitos.app.data.local.database.RuralitosDatabase
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * «Empezar desde cero» de punta a punta: con muchas fichas (cientos de registros por borrar en la nube) todas deben
 * quedar eliminadas en el servidor y ninguna debe volver a bajar al teléfono.
 */
@RunWith(AndroidJUnit4::class)
class EliminarTodasConServidorFalsoTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = RuralitosDatabase.obtenerBaseDatos(context)
    private val dao = database.sincronizacionDao()
    private val org = "org-borrado-qa"
    private val usuario = "usuario-borrado-qa"
    private lateinit var servidor: ServidorSupabaseFalso

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
        limpiarLocal()
        MarcasDescarga.borrarTodo(context)
        SesionSupabaseCifrada(context).limpiar()
    }

    private fun limpiarLocal() {
        database.openHelper.writableDatabase.apply {
            execSQL("DELETE FROM fichas_familiares WHERE organizacionId = '$org'")
            execSQL("DELETE FROM eliminaciones_sync WHERE organizacionId = '$org'")
            execSQL("DELETE FROM salas WHERE organizacionId = '$org'")
        }
    }

    private fun contar(tabla: String, donde: String = "1=1"): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $tabla WHERE $donde").use { it.moveToFirst(); it.getInt(0) }

    private fun ficha(syncId: String) = FichaFamiliarEntity(
        cedulaJefeHogar = "0000000010", institucionSistema = "", unidadOperativa = "QA", codigoUo = "1",
        areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1",
        provinciaCodigoLocalizacion = "1", numeroFichaFamiliar = "QA-BORRADO-$syncId",
        provincia = "P", canton = "C", parroquia = "R", sector = "S", manzana = "1", numeroFamilia = "1",
        direccionHabitualFamilia = "D", barrio = "B", numeroCasa = "1", comunidad = "C", grupoCultural = "MESTIZO",
        nombreApellidoJefeFamilia = "JEFE", numeroTelefono = "0", fechaLlenado = "01/01/2026", numeroCarpeta = "1",
        responsableNombre = "QA", responsableCodigo = "1", syncId = syncId, syncEstado = "PENDIENTE", organizacionId = org
    )

    private suspend fun crearFichas(cantidad: Int) = repeat(cantidad) {
        val id = dao.guardarFichaRemota(ficha(UUID.randomUUID().toString()))
        repeat(2) { n ->
            database.fichaContenidoDao().guardarMiembro(
                MiembroFamiliaEntity(
                    fichaId = id, grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO, apellidosNombres = "PERSONA $n",
                    parentesco = "HIJO/A", fechaNacimiento = "01/01/2000", ocupacion = "", sexo = "H", escolaridad = "BAS"
                )
            )
        }
    }

    private fun sincronizar() = runBlocking { SincronizadorSupabase(context).ejecutar() }

    @Test
    fun conMuchasFichasTodasQuedanEliminadasEnLaNubeYNoVuelven() = runBlocking {
        val total = 70 // 70 fichas + 140 integrantes = 210 registros por borrar (antes solo se procesaban 100 por vez)
        crearFichas(total)
        sincronizar()
        assertEquals(total, servidor.filas("fichas_familiares").size)
        assertEquals(total * 2, servidor.filas("miembros_familia").size)

        val eliminadas = EliminadorFichas.eliminarTodas(context, database, 1L)
        assertEquals(total, eliminadas)
        assertEquals(0, contar("fichas_familiares", "organizacionId = '$org'"))
        assertTrue("las bajas quedan anotadas", contar("eliminaciones_sync", "organizacionId = '$org'") >= total)

        sincronizar()

        assertEquals("todas las fichas dadas de baja en el servidor", total,
            servidor.filas("fichas_familiares").count { !it.isNull("deleted_at") })
        assertEquals("y sus integrantes", total * 2,
            servidor.filas("miembros_familia").count { !it.isNull("deleted_at") })
        assertEquals("nada queda pendiente", 0, contar("eliminaciones_sync", "organizacionId = '$org'"))
        assertEquals("ninguna ficha volvió al teléfono", 0, contar("fichas_familiares", "organizacionId = '$org'"))

        // una sincronización más (descarga completa) tampoco las trae de vuelta
        MarcasDescarga.borrarTodo(context)
        sincronizar()
        assertEquals(0, contar("fichas_familiares", "organizacionId = '$org'"))
    }

    @Test
    fun mientrasLaNubeNoConfirmaLaBajaLaFichaNoVuelveAAparecer() = runBlocking {
        crearFichas(3)
        sincronizar()
        servidor.rechazarModificaciones = true

        EliminadorFichas.eliminarTodas(context, database, 1L)
        MarcasDescarga.borrarTodo(context)
        val resultado = sincronizar()

        assertTrue("la nube no aceptó las bajas: se informa como error", resultado.errores > 0)
        assertEquals("las fichas eliminadas no vuelven aunque la nube las siga mostrando", 0,
            contar("fichas_familiares", "organizacionId = '$org'"))
        assertTrue("las bajas siguen anotadas para reintentar", contar("eliminaciones_sync", "organizacionId = '$org' AND tabla = 'fichas_familiares'") == 3)

        // al volver los permisos, se completan y siguen sin volver
        servidor.rechazarModificaciones = false
        sincronizar()
        assertEquals(3, servidor.filas("fichas_familiares").count { !it.isNull("deleted_at") })
        assertEquals(0, contar("eliminaciones_sync", "organizacionId = '$org'"))
        assertEquals(0, contar("fichas_familiares", "organizacionId = '$org'"))
    }
}
