package com.ruralitos.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.data.security.CambioDeCuentaBloqueadoException
import com.ruralitos.app.data.security.ControlPropietarioLocal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Los datos de pacientes del teléfono pertenecen a una sola cuenta. (Solo se ejecuta en el emulador.) */
@RunWith(AndroidJUnit4::class)
class CambioDeCuentaTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = RuralitosDatabase.obtenerBaseDatos(context)
    private val supabase = SupabaseApi(context)

    private fun ficha(estado: String) = FichaFamiliarEntity(
        cedulaJefeHogar = "0000000003", institucionSistema = "MSP", unidadOperativa = "QA", codigoUo = "1",
        areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1",
        provinciaCodigoLocalizacion = "1", numeroFichaFamiliar = "QA-CUENTA-${UUID.randomUUID()}",
        provincia = "P", canton = "C", parroquia = "R", sector = "S", manzana = "1", numeroFamilia = "1",
        direccionHabitualFamilia = "D", barrio = "B", numeroCasa = "1", comunidad = "C", grupoCultural = "MESTIZO",
        nombreApellidoJefeFamilia = "OTRA CUENTA", numeroTelefono = "0", fechaLlenado = "01/01/2026", numeroCarpeta = "1",
        responsableNombre = "QA", responsableCodigo = "1", syncEstado = estado, organizacionId = "org-qa"
    )

    private fun preferencias() = context.getSharedPreferences("ruralitos_propietario", android.content.Context.MODE_PRIVATE)

    @Test
    fun otraCuentaConCambiosSinSubirNoPuedeEntrarYNoSePierdeNada() = runBlocking {
        ControlPropietarioLocal.borrarDatosDePacientes(context, database)
        preferencias().edit().putString("supabase_id", "cuenta-a").commit()
        val id = database.sincronizacionDao().guardarFichaRemota(ficha("PENDIENTE"))
        try {
            try {
                ControlPropietarioLocal.verificar(context, database, supabase, "cuenta-b", "0000000099")
                fail("debía bloquear el cambio de cuenta")
            } catch (esperado: CambioDeCuentaBloqueadoException) {
                assertTrue(esperado.message!!.contains("1 cambio"))
            }
            assertEquals("OTRA CUENTA", database.fichaFamiliarDao().buscarPorId(id)!!.nombreApellidoJefeFamilia)
            assertEquals("cuenta-a", preferencias().getString("supabase_id", null))
        } finally {
            ControlPropietarioLocal.borrarDatosDePacientes(context, database)
            preferencias().edit().clear().commit()
        }
    }

    @Test
    fun otraCuentaSinPendientesBorraLosDatosDeLaAnteriorYLosArchivos() = runBlocking {
        ControlPropietarioLocal.borrarDatosDePacientes(context, database)
        preferencias().edit().putString("supabase_id", "cuenta-a").commit()
        val id = database.sincronizacionDao().guardarFichaRemota(ficha("SINCRONIZADO"))
        val archivo = File(context.filesDir, "adjuntos_sincronizados/org-qa/x/a.png").apply {
            parentFile!!.mkdirs(); writeText("dato")
        }
        try {
            ControlPropietarioLocal.verificar(context, database, supabase, "cuenta-b", "0000000099")

            assertEquals(null, database.fichaFamiliarDao().buscarPorId(id))
            assertTrue(!archivo.exists())
            assertEquals("cuenta-b", preferencias().getString("supabase_id", null))
        } finally {
            ControlPropietarioLocal.borrarDatosDePacientes(context, database)
            preferencias().edit().clear().commit()
        }
    }

    @Test
    fun laMismaCuentaConservaSusDatos() = runBlocking {
        ControlPropietarioLocal.borrarDatosDePacientes(context, database)
        preferencias().edit().putString("supabase_id", "cuenta-a").commit()
        val id = database.sincronizacionDao().guardarFichaRemota(ficha("SINCRONIZADO"))
        try {
            ControlPropietarioLocal.verificar(context, database, supabase, "cuenta-a", "0000000001")
            assertEquals("OTRA CUENTA", database.fichaFamiliarDao().buscarPorId(id)!!.nombreApellidoJefeFamilia)
        } finally {
            ControlPropietarioLocal.borrarDatosDePacientes(context, database)
            preferencias().edit().clear().commit()
        }
    }
}
