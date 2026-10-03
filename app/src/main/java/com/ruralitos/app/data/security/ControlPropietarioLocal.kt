package com.ruralitos.app.data.security

import android.content.Context
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.seed.EstablecimientosSaludSeeder
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.data.sync.MarcasDescarga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Se intentó entrar con otra cuenta mientras este teléfono guarda cambios de la anterior sin subir. */
class CambioDeCuentaBloqueadoException(mensaje: String) : IllegalStateException(mensaje)

/**
 * Los datos de pacientes del teléfono pertenecen a UNA cuenta. Si otra cuenta inicia sesión en el mismo teléfono,
 * los datos de la anterior se borran antes de que la nueva pueda verlos, salvo que haya cambios sin subir:
 * en ese caso se impide el cambio para no perder trabajo, y se pide volver a entrar con la cuenta original.
 */
object ControlPropietarioLocal {
    private const val ARCHIVO = "ruralitos_propietario"
    private const val CLAVE = "supabase_id"

    private val CARPETAS_DE_PACIENTES = listOf(
        "adjuntos_sincronizados", "familiogramas", "croquis_mapas", "firmas_profesionales"
    )

    suspend fun verificar(
        context: Context,
        database: RuralitosDatabase,
        supabase: SupabaseApi,
        supabaseId: String,
        cedula: String
    ) {
        val preferencias = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        val usuarios = database.usuarioDao()
        val actual = withContext(Dispatchers.IO) {
            preferencias.getString(CLAVE, null)
                ?: when {
                    // Instalaciones anteriores a este control: se deduce por los usuarios que ya usaron el teléfono.
                    usuarios.buscarPorSupabaseId(supabaseId) != null ||
                        (cedula.isNotBlank() && usuarios.buscarPorCedula(cedula) != null) -> supabaseId
                    usuarios.contar() == 0 && database.sincronizacionDao().contarFichasDeTodasLasOrganizaciones() == 0 ->
                        supabaseId
                    else -> "otra-cuenta"
                }
        }
        if (actual == supabaseId) {
            preferencias.edit().putString(CLAVE, supabaseId).apply()
            return
        }
        val pendientes = withContext(Dispatchers.IO) { database.sincronizacionDao().contarCambiosSinSubir() }
        if (pendientes > 0) {
            supabase.cerrarSesion()
            throw CambioDeCuentaBloqueadoException(
                "Este teléfono guarda $pendientes cambio(s) sin sincronizar de otra cuenta. " +
                    "Inicia sesión con esa cuenta, espera a que sincronice y luego podrás entrar con esta."
            )
        }
        borrarDatosDePacientes(context, database)
        preferencias.edit().putString(CLAVE, supabaseId).apply()
    }

    /** Borra del teléfono todo lo que pertenece a pacientes (base de datos, archivos y marcas de descarga). */
    suspend fun borrarDatosDePacientes(context: Context, database: RuralitosDatabase) =
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            CARPETAS_DE_PACIENTES.forEach { File(context.filesDir, it).deleteRecursively() }
            MarcasDescarga.borrarTodo(context)
            runCatching { EstablecimientosSaludSeeder.cargarSiEstaVacio(context) }
            Unit
        }

    /** Se llama cuando la propia cuenta elimina sus datos o se cierra por completo. */
    fun olvidarPropietario(context: Context) {
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
