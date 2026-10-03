package com.ruralitos.app

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.backup.GestorRespaldoRuralitos
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.domain.GrupoEdadFamiliar
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Respaldo cifrado de punta a punta: crear, borrar los datos, restaurar y comprobar que todo vuelve. */
@RunWith(AndroidJUnit4::class)
class RespaldoRestauracionTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = RuralitosDatabase.obtenerBaseDatos(context)
    private val archivoRespaldo = File(context.cacheDir, "respaldo_prueba.ruralitos")

    @After
    fun limpiar() {
        archivoRespaldo.delete()
        database.openHelper.writableDatabase.apply {
            execSQL("DELETE FROM miembros_familia WHERE fichaId IN (SELECT id FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-RESPALDO-%')")
            execSQL("DELETE FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-RESPALDO-%'")
            execSQL("DELETE FROM eliminaciones_sync WHERE organizacionId = 'org-qa'")
            execSQL("DELETE FROM usuarios WHERE cedula = '0000000077'")
        }
    }

    @Test
    fun respaldaBorraYRestauraLaFichaConSusIntegrantesYAdjuntos() = runBlocking {
        val usuarioId = database.usuarioDao().guardar(
            UsuarioEntity(
                cedula = "0000000077", nombres = "QA Respaldo", cargo = "Médico", rol = "ADMIN",
                claveHash = "", claveSalt = "", supabaseId = "usuario-qa-respaldo", organizacionId = "org-qa"
            )
        )
        val usuario = database.usuarioDao().buscarPorId(usuarioId)!!
        val ficha = FichaFamiliarEntity(
            cedulaJefeHogar = "0000000078", institucionSistema = "", unidadOperativa = "QA", codigoUo = "QA1",
            areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1",
            provinciaCodigoLocalizacion = "1", numeroFichaFamiliar = "QA-RESPALDO-1",
            provincia = "P", canton = "C", parroquia = "R", sector = "S", manzana = "1", numeroFamilia = "1",
            direccionHabitualFamilia = "D", barrio = "B", numeroCasa = "1", comunidad = "C", grupoCultural = "MESTIZO",
            nombreApellidoJefeFamilia = "RESPALDO PRUEBA", numeroTelefono = "0", fechaLlenado = "01/01/2026",
            numeroCarpeta = "1", responsableNombre = "QA", responsableCodigo = "1",
            creadoPorUsuarioId = usuarioId, organizacionId = "org-qa"
        )
        val fichaId = database.fichaFamiliarDao().guardarFicha(ficha)
        val contenido = database.fichaContenidoDao()
        contenido.guardarMiembro(
            MiembroFamiliaEntity(
                fichaId = fichaId, grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO,
                apellidosNombres = "RESPALDO LUIS", parentesco = "JEFE/A DE FAMILIA", fechaNacimiento = "01/01/1980",
                ocupacion = "", sexo = "H", escolaridad = "BAS", cedula = "0000000078"
            )
        )
        val imagen = File(context.filesDir, "respaldo_prueba.png").apply { writeBytes(ByteArray(2048) { it.toByte() }) }
        contenido.guardarAdjunto(AdjuntoFichaEntity(fichaId = fichaId, tipo = "CROQUIS", uri = Uri.fromFile(imagen).toString()))

        val resultado = GestorRespaldoRuralitos.crear(context, Uri.fromFile(archivoRespaldo), "clave-de-prueba-123", usuario)
        assertEquals(1, resultado.fichasIncluidas)
        assertEquals(1, resultado.adjuntosIncluidos)
        assertTrue(archivoRespaldo.length() > 0)

        // Se pierde todo en el teléfono.
        database.openHelper.writableDatabase.execSQL("DELETE FROM fichas_familiares WHERE numeroFichaFamiliar = 'QA-RESPALDO-1'")
        imagen.delete()
        assertEquals(null, database.fichaFamiliarDao().buscarPorCedula("0000000078"))

        val restaurado = GestorRespaldoRuralitos.restaurar(context, Uri.fromFile(archivoRespaldo), "clave-de-prueba-123", usuario, null)
        assertEquals(1, restaurado.fichasImportadas)
        assertEquals(1, restaurado.adjuntosRestaurados)

        val recuperada = database.fichaFamiliarDao().buscarPorCedula("0000000078")!!
        assertEquals("RESPALDO PRUEBA", recuperada.nombreApellidoJefeFamilia)
        assertEquals("PENDIENTE", recuperada.syncEstado)
        assertEquals(listOf("RESPALDO LUIS"), database.sincronizacionDao().miembros(recuperada.id).map { it.apellidosNombres })
        val adjunto = database.sincronizacionDao().adjuntos(recuperada.id).single()
        assertEquals(2048L, File(Uri.parse(adjunto.uri).path!!).length())
        File(Uri.parse(adjunto.uri).path!!).delete()

        // Con otra clave el respaldo cifrado no se abre.
        val error = runCatching {
            GestorRespaldoRuralitos.restaurar(context, Uri.fromFile(archivoRespaldo), "clave-equivocada", usuario, null)
        }.exceptionOrNull()
        assertTrue("una clave equivocada debe fallar", error != null)
    }
}
