package com.ruralitos.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.ruralitos.app.BuildConfig
import com.ruralitos.app.domain.ConcesionAcceso
import com.ruralitos.app.data.security.SesionSupabase
import com.ruralitos.app.data.security.SesionSupabaseCifrada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class RegistroRemotoResultado(
    val requiereConfirmarCorreo: Boolean,
    val usuarioId: String?
)

data class PerfilRemoto(
    val id: String,
    val cedula: String,
    val nombres: String,
    val cargo: String,
    val correo: String,
    val telefono: String,
    val codigoSenescyt: String,
    val sexo: String = "",
    val apellidos: String = ""
)

data class MiembroEquipoRemoto(
    val usuarioId: String,
    val nombre: String,
    val cargo: String,
    val correo: String,
    val rol: String,
    val activo: Boolean,
    val permiso: String,
    /** Solo ve un EAIS o un barrio, no toda la Sala. */
    val alcanceLimitado: Boolean
)

/** Una persona con la que compartí mis fichas y lo que le compartí. */
data class AccesoOtorgado(
    val organizacionId: String,
    val usuarioId: String,
    val nombre: String,
    val cargo: String,
    val correo: String,
    val permiso: String,
    val centros: Int,
    val eais: Int,
    val barrios: Int,
    val fichas: Int
) {
    /** «Todo lo mío», «2 barrios y 3 fichas»… */
    val resumen: String
        get() {
            if (centros > 0) return "Todas tus fichas del centro"
            val partes = buildList {
                if (eais > 0) add(if (eais == 1) "1 EAIS" else "$eais EAIS")
                if (barrios > 0) add(if (barrios == 1) "1 barrio" else "$barrios barrios")
                if (fichas > 0) add(if (fichas == 1) "1 ficha" else "$fichas fichas")
            }
            return when (partes.size) {
                0 -> "Sin fichas"
                1 -> partes[0]
                else -> partes.dropLast(1).joinToString(", ") + " y " + partes.last()
            }
        }
}

/** Una persona que me compartió fichas y lo que me compartió. */
data class AccesoRecibido(
    val organizacionId: String,
    val autorId: String,
    val nombre: String,
    val cargo: String,
    val correo: String,
    val permiso: String,
    val centros: Int,
    val eais: Int,
    val barrios: Int,
    val fichas: Int
) {
    val resumen: String
        get() = AccesoOtorgado(organizacionId, autorId, nombre, cargo, correo, permiso, centros, eais, barrios, fichas)
            .resumen.replace("Todas tus fichas del centro", "Todas sus fichas del centro")
}

/** Lo que el servidor sabe de una ficha compartida: si me la dieron (autor, permiso) o a cuántas personas se la di. */
data class InfoFichaCompartida(
    val fichaId: String,
    val recibida: Boolean,
    val autorId: String,
    val autorNombre: String,
    val permiso: String,
    val personas: Int
)

/** Una persona que ve una de mis fichas; `via` = FICHA, BARRIO, EAIS o CENTRO. */
data class PersonaConAccesoFicha(
    val usuarioId: String,
    val nombre: String,
    val cargo: String,
    val permiso: String,
    val via: String
)

/** Una ficha suelta que le compartí a una persona. */
data class FichaCompartidaCon(
    val fichaId: String,
    val numero: String,
    val jefe: String,
    val permiso: String
)

data class MembresiaRemota(
    val organizacionId: String,
    val rol: String,
    val activo: Boolean,
    val establecimientoId: Long?
)

data class InvitacionRemota(
    val codigo: String,
    val expiraEn: String
)

data class SalaRemota(
    val organizacionId: String,
    val establecimientoId: Long?,
    val nombreSala: String,
    val codigoSala: String,
    val rol: String,
    val permiso: String,
    val codigoUo: String,
    val nombreCentroSalud: String,
    val institucionSistema: String,
    val provinciaCodigoLocalizacion: String,
    val provincia: String,
    val cantonCodigoLocalizacion: String,
    val canton: String,
    val parroquiaCodigoLocalizacion: String,
    val parroquia: String,
    val sector: String,
    val areaNumero: String
)

data class EaisRemoto(
    val id: String,
    val organizacionId: String,
    val numero: Int,
    val activo: Boolean
)

data class TerritorioRemoto(
    val id: String,
    val organizacionId: String,
    val eaisId: String,
    val tipo: String,
    val nombre: String,
    val activo: Boolean
)

data class CodigoAccesoRemoto(
    val codigo: String,
    val expiraEn: String
)

class ErrorSupabase(message: String, val codigoHttp: Int? = null) : IOException(message)

class SupabaseApi(context: Context) {
    private val appContext = context.applicationContext
    private val sesionSegura = SesionSupabaseCifrada(appContext)
    private val mutexRefresh = Mutex()
    private val baseUrl = (urlParaPruebas ?: BuildConfig.SUPABASE_URL).trimEnd('/')
    private val clavePublicable = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    init {
        require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://localhost")) {
            "Falta configurar supabase.url"
        }
        require(clavePublicable.isNotBlank()) { "Falta configurar supabase.publishableKey" }
    }

    fun hayInternet(): Boolean {
        val connectivity = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val red = connectivity.activeNetwork ?: return false
        val capacidades = connectivity.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun sesionGuardada(): SesionSupabase? = sesionSegura.obtener()
    fun organizacionGuardada(): String? = sesionSegura.organizacionId()
    fun guardarOrganizacionActiva(id: String?) = sesionSegura.guardarOrganizacion(id)
    fun recuperacionPendiente(): Boolean = sesionSegura.recuperacionPendiente()
    fun finalizarRecuperacion() = sesionSegura.marcarRecuperacion(false)

    suspend fun registrar(
        correo: String,
        clave: String,
        cedula: String,
        nombres: String,
        cargo: String,
        telefono: String,
        codigoSenescyt: String,
        sexo: String = "",
        apellidos: String = ""
    ): RegistroRemotoResultado {
        val data = JSONObject()
            .put("cedula", cedula)
            .put("nombres", nombres)
            .put("cargo", cargo)
            .put("telefono", telefono)
            .put("codigo_senescyt", codigoSenescyt.uppercase())
            .put("sexo", sexo)
            .put("apellidos", apellidos)
        val cuerpo = JSONObject()
            .put("email", correo.lowercase())
            .put("password", clave)
            .put("data", data)
        val respuesta = solicitar(
            metodo = "POST",
            ruta = "/auth/v1/signup?redirect_to=${codificar(REDIRECT_AUTH)}",
            cuerpo = cuerpo
        ).jsonObjeto()
        guardarSesionSiExiste(respuesta)
        val usuario = respuesta.optJSONObject("user")
        return RegistroRemotoResultado(
            requiereConfirmarCorreo = respuesta.optString("access_token").isBlank(),
            usuarioId = usuario?.optString("id")
        )
    }

    suspend fun iniciarSesion(correo: String, clave: String): SesionSupabase {
        val respuesta = solicitar(
            metodo = "POST",
            ruta = "/auth/v1/token?grant_type=password",
            cuerpo = JSONObject().put("email", correo.lowercase()).put("password", clave)
        ).jsonObjeto()
        return guardarSesion(respuesta)
    }

    suspend fun enviarRecuperacion(correo: String) {
        solicitar(
            metodo = "POST",
            ruta = "/auth/v1/recover?redirect_to=${codificar(REDIRECT_AUTH)}",
            cuerpo = JSONObject().put("email", correo.lowercase())
        )
    }

    suspend fun enviarCodigoEliminacion() {
        solicitar(
            metodo = "GET",
            ruta = "/auth/v1/reauthenticate",
            accessToken = tokenValido()
        )
    }

    suspend fun eliminarCuenta(codigo: String) {
        require(codigo.matches(Regex("[0-9]{6,8}"))) {
            "Ingresa el código completo recibido por correo."
        }
        solicitar(
            metodo = "POST",
            ruta = "/functions/v1/eliminar-cuenta",
            cuerpo = JSONObject().put("codigo", codigo),
            accessToken = tokenValido()
        )
        sesionSegura.limpiar()
    }

    fun limpiarSesionLocal() {
        sesionSegura.limpiar()
    }
    suspend fun actualizarClave(nuevaClave: String) {
        val token = tokenValido()
        solicitar(
            metodo = "PUT",
            ruta = "/auth/v1/user",
            cuerpo = JSONObject().put("password", nuevaClave),
            accessToken = token
        )
        sesionSegura.marcarRecuperacion(false)
    }

    suspend fun cambiarClave(correo: String, claveActual: String, nuevaClave: String) {
        iniciarSesion(correo, claveActual)
        actualizarClave(nuevaClave)
    }

    suspend fun cerrarSesion() {
        val token = sesionSegura.obtener()?.accessToken
        runCatching {
            if (!token.isNullOrBlank()) solicitar("POST", "/auth/v1/logout", accessToken = token)
        }
        sesionSegura.limpiar()
    }

    fun procesarCallback(uri: Uri?): Boolean {
        if (uri?.scheme != "ruralitos" || uri.host != "auth-callback") return false
        val parametros = mutableMapOf<String, String>()
        uri.fragment?.split('&')?.forEach { parte ->
            val (clave, valor) = parte.split('=', limit = 2).let {
                it.first() to it.getOrElse(1) { "" }
            }
            parametros[clave] = Uri.decode(valor)
        }
        uri.queryParameterNames.forEach { parametros[it] = uri.getQueryParameter(it).orEmpty() }
        val access = parametros["access_token"].orEmpty()
        if (access.isBlank()) return false
        val refresh = parametros["refresh_token"].orEmpty()
        val expira = parametros["expires_at"]?.toLongOrNull()
            ?: (ahoraSegundos() + (parametros["expires_in"]?.toLongOrNull() ?: 3600L))
        val anterior = sesionSegura.obtener()
        sesionSegura.guardar(
            SesionSupabase(
                accessToken = access,
                refreshToken = refresh.ifBlank { anterior?.refreshToken.orEmpty() },
                expiraEnSegundos = expira,
                usuarioId = extraerSubjectJwt(access).ifBlank { anterior?.usuarioId.orEmpty() },
                correo = anterior?.correo.orEmpty()
            )
        )
        if (parametros["type"] == "recovery") sesionSegura.marcarRecuperacion(true)
        return true
    }

    suspend fun obtenerPerfil(): PerfilRemoto? {
        val token = tokenValido()
        val usuarioId = checkNotNull(sesionSegura.obtener()?.usuarioId.takeUnless { it.isNullOrBlank() })
        val base = "id,cedula,nombres,cargo,correo,telefono,codigo_senescyt"
        // Las columnas sexo y apellidos existen solo si se aplicó la migración nueva de Supabase.
        val arreglo = runCatching {
            solicitar(
                "GET",
                "/rest/v1/perfiles?id=eq.${codificar(usuarioId)}&select=$base,sexo,apellidos",
                accessToken = token
            ).jsonArreglo()
        }.getOrElse {
            solicitar(
                "GET",
                "/rest/v1/perfiles?id=eq.${codificar(usuarioId)}&select=$base",
                accessToken = token
            ).jsonArreglo()
        }
        if (arreglo.length() == 0) return null
        val perfil = arreglo.getJSONObject(0).let {
            PerfilRemoto(
                id = it.getString("id"),
                cedula = it.optString("cedula"),
                nombres = it.optString("nombres"),
                cargo = it.optString("cargo"),
                correo = it.optString("correo"),
                telefono = it.optString("telefono"),
                codigoSenescyt = it.optString("codigo_senescyt"),
                sexo = it.optString("sexo"),
                apellidos = it.optString("apellidos")
            )
        }
        if (perfil.sexo.isNotBlank() && perfil.apellidos.isNotBlank()) return perfil
        // Sin las columnas nuevas, el sexo y los apellidos se leen de los datos guardados con la cuenta.
        val metadatos = runCatching {
            solicitar("GET", "/auth/v1/user", accessToken = token).jsonObjeto().optJSONObject("user_metadata")
        }.getOrNull()
        return perfil.copy(
            sexo = perfil.sexo.ifBlank { metadatos?.optString("sexo").orEmpty() },
            apellidos = perfil.apellidos.ifBlank { metadatos?.optString("apellidos").orEmpty() }
        )
    }

    suspend fun obtenerMembresia(): MembresiaRemota? {
        val token = tokenValido()
        val usuarioId = checkNotNull(sesionSegura.obtener()?.usuarioId.takeUnless { it.isNullOrBlank() })
        val arreglo = solicitar(
            "GET",
            "/rest/v1/miembros_organizacion?usuario_id=eq.${codificar(usuarioId)}&activo=eq.true" +
                "&select=organizacion_id,rol,activo,establecimiento_id&limit=1",
            accessToken = token
        ).jsonArreglo()
        if (arreglo.length() == 0) return null
        return arreglo.getJSONObject(0).let {
            MembresiaRemota(
                organizacionId = it.getString("organizacion_id"),
                rol = it.optString("rol", "MEDICO"),
                activo = it.optBoolean("activo", true),
                establecimientoId = if (it.isNull("establecimiento_id")) null else it.optLong("establecimiento_id")
            ).also { membresia -> sesionSegura.guardarOrganizacion(membresia.organizacionId) }
        }
    }

    suspend fun obtenerSalas(): List<SalaRemota> {
        val token = tokenValido()
        val usuarioId = checkNotNull(sesionSegura.obtener()?.usuarioId.takeUnless { it.isNullOrBlank() })
        val membresias = solicitar(
            "GET",
            "/rest/v1/miembros_organizacion?usuario_id=eq.${codificar(usuarioId)}&activo=eq.true" +
                "&select=organizacion_id,rol,activo,establecimiento_id&order=creado_en.asc",
            accessToken = token
        ).jsonArreglo()
        val resultado = mutableListOf<SalaRemota>()
        for (indice in 0 until membresias.length()) {
            val miembro = membresias.getJSONObject(indice)
            val organizacionId = miembro.getString("organizacion_id")
            val organizaciones = solicitar(
                "GET",
                "/rest/v1/organizaciones?id=eq.${codificar(organizacionId)}" +
                    "&select=id,nombre,codigo,establecimiento_id&limit=1",
                accessToken = token
            ).jsonArreglo()
            if (organizaciones.length() == 0) continue
            val organizacion = organizaciones.getJSONObject(0)
            val establecimientoId = when {
                !organizacion.isNull("establecimiento_id") -> organizacion.optLong("establecimiento_id")
                !miembro.isNull("establecimiento_id") -> miembro.optLong("establecimiento_id")
                else -> null
            }
            val establecimiento = if (establecimientoId != null) {
                solicitar(
                    "GET",
                    "/rest/v1/establecimientos_salud?id=eq.$establecimientoId" +
                        "&select=id,codigo_uo,nombre_centro_salud,institucion_sistema," +
                        "provincia_codigo_localizacion,provincia,canton_codigo_localizacion,canton," +
                        "parroquia_codigo_localizacion,parroquia,sector,area_numero&limit=1",
                    accessToken = token
                ).jsonArreglo().let { if (it.length() == 0) null else it.getJSONObject(0) }
            } else null
            val accesos = solicitar(
                "GET",
                "/rest/v1/accesos_sala?organizacion_id=eq.${codificar(organizacionId)}" +
                    "&usuario_id=eq.${codificar(usuarioId)}&activo=eq.true&select=permiso",
                accessToken = token
            ).jsonArreglo()
            var permiso = if (miembro.optString("rol").equals("ADMINISTRADOR", true)) {
                "ADMINISTRADOR"
            } else {
                "LECTOR"
            }
            for (accesoIndice in 0 until accesos.length()) {
                val candidato = accesos.getJSONObject(accesoIndice).optString("permiso", "LECTOR")
                if (rangoPermiso(candidato) > rangoPermiso(permiso)) permiso = candidato
            }
            resultado += SalaRemota(
                organizacionId = organizacionId,
                establecimientoId = establecimientoId,
                nombreSala = organizacion.optString("nombre").ifBlank {
                    establecimiento?.optString("nombre_centro_salud").orEmpty().ifBlank { "Sala Ruralitos" }
                },
                codigoSala = organizacion.optString("codigo"),
                rol = miembro.optString("rol", "MEDICO"),
                permiso = permiso,
                codigoUo = establecimiento?.optString("codigo_uo").orEmpty(),
                nombreCentroSalud = establecimiento?.optString("nombre_centro_salud").orEmpty(),
                institucionSistema = establecimiento?.optString("institucion_sistema").orEmpty(),
                provinciaCodigoLocalizacion = establecimiento?.optString("provincia_codigo_localizacion").orEmpty(),
                provincia = establecimiento?.optString("provincia").orEmpty(),
                cantonCodigoLocalizacion = establecimiento?.optString("canton_codigo_localizacion").orEmpty(),
                canton = establecimiento?.optString("canton").orEmpty(),
                parroquiaCodigoLocalizacion = establecimiento?.optString("parroquia_codigo_localizacion").orEmpty(),
                parroquia = establecimiento?.optString("parroquia").orEmpty(),
                sector = establecimiento?.optString("sector").orEmpty(),
                areaNumero = establecimiento?.optString("area_numero").orEmpty()
            )
        }
        val activa = organizacionGuardada()
        if (activa == null || resultado.none { it.organizacionId == activa }) {
            guardarOrganizacionActiva(resultado.firstOrNull()?.organizacionId)
        }
        return resultado
    }
    suspend fun obtenerEais(organizacionId: String): List<EaisRemoto> =
        seleccionarPaginado(
            "eais?organizacion_id=eq.${codificar(organizacionId)}&activo=eq.true" +
                "&select=id,organizacion_id,numero,activo&order=numero.asc"
        ).map {
            EaisRemoto(
                id = it.getString("id"),
                organizacionId = it.getString("organizacion_id"),
                numero = it.optInt("numero"),
                activo = it.optBoolean("activo", true)
            )
        }

    suspend fun obtenerTerritorios(organizacionId: String): List<TerritorioRemoto> =
        seleccionarPaginado(
            "territorios?organizacion_id=eq.${codificar(organizacionId)}&activo=eq.true" +
                "&select=id,organizacion_id,eais_id,tipo,nombre,activo&order=tipo.asc,nombre.asc"
        ).map {
            TerritorioRemoto(
                id = it.getString("id"),
                organizacionId = it.getString("organizacion_id"),
                eaisId = it.getString("eais_id"),
                tipo = it.optString("tipo", "BARRIO"),
                nombre = it.optString("nombre"),
                activo = it.optBoolean("activo", true)
            )
        }

    suspend fun crearSala(codigoUo: String): String {
        require(codigoUo.isNotBlank()) { "El centro de salud no tiene código UO." }
        val respuesta = rpc(
            "crear_sala",
            JSONObject().put("p_codigo_uo", codigoUo)
        )
        val id = respuesta.texto.trim().trim('"')
        require(id.isNotBlank()) { "Supabase no devolvio la Sala creada." }
        guardarOrganizacionActiva(id)
        return id
    }

    suspend fun crearEais(organizacionId: String, numero: Int): EaisRemoto {
        require(numero > 0) { "El numero de EAIS debe ser mayor que cero." }
        val remoto = upsert(
            tabla = "eais",
            objeto = JSONObject()
                .put("id", java.util.UUID.randomUUID().toString())
                .put("organizacion_id", organizacionId)
                .put("numero", numero)
                .put("activo", true),
            conflicto = "organizacion_id,numero"
        ) ?: throw ErrorSupabase("No se pudo crear el EAIS.")
        return EaisRemoto(
            id = remoto.getString("id"),
            organizacionId = remoto.getString("organizacion_id"),
            numero = remoto.optInt("numero"),
            activo = remoto.optBoolean("activo", true)
        )
    }
    suspend fun actualizarEais(organizacionId: String, eaisId: String, numero: Int): EaisRemoto {
        require(numero in 1..999) { "El número de EAIS debe estar entre 1 y 999." }
        val modificados = solicitar(
            "PATCH",
            "/rest/v1/eais?id=eq.${codificar(eaisId)}&organizacion_id=eq.${codificar(organizacionId)}",
            cuerpo = JSONObject().put("numero", numero),
            accessToken = tokenValido(),
            headers = mapOf("Prefer" to "return=representation")
        ).jsonArreglo()
        if (modificados.length() == 0) throw ErrorSupabase("No se pudo modificar el EAIS. Comprueba tu permiso de administrador.")
        val item = modificados.getJSONObject(0)
        return EaisRemoto(item.getString("id"), item.getString("organizacion_id"), item.getInt("numero"), item.optBoolean("activo", true))
    }

    /** Desactivación reversible: las fichas y los barrios conservan sus claves históricas. */
    suspend fun desactivarEais(organizacionId: String, eaisId: String): EaisRemoto {
        val modificados = solicitar(
            "PATCH",
            "/rest/v1/eais?id=eq.${codificar(eaisId)}&organizacion_id=eq.${codificar(organizacionId)}",
            cuerpo = JSONObject().put("activo", false),
            accessToken = tokenValido(),
            headers = mapOf("Prefer" to "return=representation")
        ).jsonArreglo()
        if (modificados.length() == 0) throw ErrorSupabase("No se pudo eliminar el EAIS. Comprueba tu permiso de administrador.")
        val item = modificados.getJSONObject(0)
        return EaisRemoto(item.getString("id"), item.getString("organizacion_id"), item.getInt("numero"), item.optBoolean("activo", false))
    }
    suspend fun crearTerritorio(
        organizacionId: String,
        eaisId: String,
        tipo: String,
        nombre: String
    ): TerritorioRemoto {
        val tipoNormalizado = tipo.uppercase()
        require(tipoNormalizado == "BARRIO") {
            "Solo se pueden crear barrios."
        }
        require(nombre.trim().length >= 2) { "Escribe el nombre del territorio." }
        val remoto = upsert(
            tabla = "territorios",
            objeto = JSONObject()
                .put("id", java.util.UUID.randomUUID().toString())
                .put("organizacion_id", organizacionId)
                .put("eais_id", eaisId)
                .put("tipo", tipoNormalizado)
                .put("nombre", nombre.trim())
                .put("activo", true),
            conflicto = "eais_id,tipo,nombre"
        ) ?: throw ErrorSupabase("No se pudo crear el barrio.")
        return TerritorioRemoto(
            id = remoto.getString("id"),
            organizacionId = remoto.getString("organizacion_id"),
            eaisId = remoto.getString("eais_id"),
            tipo = remoto.optString("tipo"),
            nombre = remoto.optString("nombre"),
            activo = remoto.optBoolean("activo", true)
        )
    }

    suspend fun actualizarTerritorio(
        organizacionId: String,
        territorioId: String,
        tipo: String,
        nombre: String
    ): TerritorioRemoto {
        val tipoNormalizado = tipo.uppercase()
        require(tipoNormalizado == "BARRIO") {
            "Solo se pueden guardar barrios."
        }
        val nombreNormalizado = nombre.trim()
        require(nombreNormalizado.length >= 2) { "Escribe el nombre del territorio." }
        val modificados = solicitar(
            "PATCH",
            "/rest/v1/territorios?id=eq.${codificar(territorioId)}" +
                "&organizacion_id=eq.${codificar(organizacionId)}",
            cuerpo = JSONObject()
                .put("tipo", tipoNormalizado)
                .put("nombre", nombreNormalizado),
            accessToken = tokenValido(),
            headers = mapOf("Prefer" to "return=representation")
        ).jsonArreglo()
        if (modificados.length() == 0) {
            throw ErrorSupabase(
                "No se pudo editar el barrio. Comprueba tu permiso de administrador."
            )
        }
        return modificados.getJSONObject(0).aTerritorioRemoto()
    }

    /**
     * Oculta el territorio para nuevas fichas sin borrar las relaciones históricas.
     * Si se vuelve a crear con el mismo nombre, el upsert existente puede reactivarlo.
     */
    suspend fun desactivarTerritorio(
        organizacionId: String,
        territorioId: String
    ): TerritorioRemoto {
        val modificados = solicitar(
            "PATCH",
            "/rest/v1/territorios?id=eq.${codificar(territorioId)}" +
                "&organizacion_id=eq.${codificar(organizacionId)}",
            cuerpo = JSONObject().put("activo", false),
            accessToken = tokenValido(),
            headers = mapOf("Prefer" to "return=representation")
        ).jsonArreglo()
        if (modificados.length() == 0) {
            throw ErrorSupabase(
                "No se pudo eliminar el barrio. Comprueba tu permiso de administrador."
            )
        }
        return modificados.getJSONObject(0).aTerritorioRemoto()
    }
    suspend fun crearCodigoAcceso(
        organizacionId: String,
        alcance: String,
        eaisId: String?,
        territorioId: String?,
        permiso: String,
        correo: String = ""
    ): CodigoAccesoRemoto {
        val arreglo = rpc(
            "crear_codigo_acceso",
            JSONObject()
                .put("p_organizacion_id", organizacionId)
                .put("p_alcance", alcance.uppercase())
                .put("p_eais_id", eaisId?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("p_territorio_id", territorioId?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("p_permiso", permiso.uppercase())
                .put("p_correo", correo.trim().lowercase().ifBlank { JSONObject.NULL })
                .put("p_horas_vigencia", 12)
        ).jsonArreglo()
        if (arreglo.length() == 0) throw ErrorSupabase("Supabase no devolvio el codigo de acceso.")
        return arreglo.getJSONObject(0).let {
            CodigoAccesoRemoto(
                codigo = it.getString("codigo"),
                expiraEn = it.optString("expira_en")
            )
        }
    }
    /**
     * Un solo código que abre varias partes a la vez: centros, EAIS, barrios o fichas sueltas. Cualquier usuario puede crearlo,
     * pero solo entrega las fichas que él mismo creó. Necesita las migraciones `20261005120000_acceso_por_alcances.sql` y
     * `20261005200000_compartir_lo_propio.sql` en Supabase.
     */
    suspend fun crearCodigoAccesoVarios(
        concesiones: List<ConcesionAcceso>,
        permiso: String,
        correo: String = ""
    ): CodigoAccesoRemoto {
        require(concesiones.isNotEmpty()) { "Elige al menos una parte para compartir." }
        val items = JSONArray()
        concesiones.forEach {
            items.put(
                JSONObject()
                    .put("organizacion_id", it.organizacionId)
                    .put("alcance", it.alcance)
                    .put("eais_id", it.eaisId ?: JSONObject.NULL)
                    .put("territorio_id", it.territorioId ?: JSONObject.NULL)
                    .put("ficha_id", it.fichaId ?: JSONObject.NULL)
            )
        }
        val arreglo = try {
            rpc(
                "crear_codigo_acceso_varios",
                JSONObject()
                    .put("p_items", items)
                    .put("p_permiso", permiso.uppercase())
                    .put("p_correo", correo.trim().lowercase().ifBlank { JSONObject.NULL })
                    .put("p_horas_vigencia", 12)
            ).jsonArreglo()
        } catch (error: ErrorSupabase) {
            if (error.codigoHttp == 404 || error.message.orEmpty().contains("crear_codigo_acceso_varios")) {
                throw ErrorSupabase(
                    "Compartir acceso necesita la actualización de Supabase " +
                        "(archivos 20261005120000_acceso_por_alcances.sql y 20261005200000_compartir_lo_propio.sql). " +
                        "Pídele a quien administra la nube que la aplique.",
                    error.codigoHttp
                )
            }
            throw error
        }
        if (arreglo.length() == 0) throw ErrorSupabase("Supabase no devolvió el código de acceso.")
        return arreglo.getJSONObject(0).let {
            CodigoAccesoRemoto(codigo = it.getString("codigo"), expiraEn = it.optString("expira_en"))
        }
    }

    /** Personas con las que yo compartí mis fichas, con un resumen de lo compartido. */
    suspend fun listarAccesosOtorgados(): List<AccesoOtorgado> {
        val filas = rpc("listar_accesos_otorgados", JSONObject()).jsonArreglo()
        return (0 until filas.length()).map { i ->
            val f = filas.getJSONObject(i)
            AccesoOtorgado(
                organizacionId = f.getString("organizacion_id"),
                usuarioId = f.getString("usuario_id"),
                nombre = listOf(f.optString("nombres"), f.optString("apellidos"))
                    .filter { it.isNotBlank() && it != "null" }.joinToString(" ").ifBlank { "Persona sin nombre" },
                cargo = f.optString("cargo").takeIf { it != "null" }.orEmpty(),
                correo = f.optString("correo").takeIf { it != "null" }.orEmpty(),
                permiso = f.optString("permiso"),
                centros = f.optInt("n_sala"),
                eais = f.optInt("n_eais"),
                barrios = f.optInt("n_barrios"),
                fichas = f.optInt("n_fichas")
            )
        }
    }

    /** Quita a una persona el acceso que yo le di (lo que le dieron otros no se toca). */
    suspend fun quitarAccesoCompartido(organizacionId: String, usuarioId: String) {
        rpc(
            "quitar_acceso_compartido",
            JSONObject().put("p_organizacion_id", organizacionId).put("p_usuario_id", usuarioId)
        )
    }

    /** Personas que me compartieron fichas, con un resumen de lo que me dieron. */
    suspend fun listarAccesosRecibidos(): List<AccesoRecibido> {
        val filas = rpc("listar_accesos_recibidos", JSONObject()).jsonArreglo()
        return (0 until filas.length()).map { i ->
            val f = filas.getJSONObject(i)
            AccesoRecibido(
                organizacionId = f.getString("organizacion_id"),
                autorId = f.getString("autor_id"),
                nombre = f.optString("nombres").takeIf { it.isNotBlank() && it != "null" }
                    ?: f.optString("correo").takeIf { it.isNotBlank() && it != "null" } ?: "Persona sin nombre",
                cargo = f.optString("cargo").takeIf { it != "null" }.orEmpty(),
                correo = f.optString("correo").takeIf { it != "null" }.orEmpty(),
                permiso = f.optString("permiso"),
                centros = f.optInt("n_sala"),
                eais = f.optInt("n_eais"),
                barrios = f.optInt("n_barrios"),
                fichas = f.optInt("n_fichas")
            )
        }
    }

    /** Me quito a mí mismo el acceso a lo que esa persona me compartió. */
    suspend fun quitarMiAcceso(organizacionId: String, autorId: String) {
        rpc(
            "quitar_mi_acceso",
            JSONObject().put("p_organizacion_id", organizacionId).put("p_autor_id", autorId)
        )
    }

    /** Etiquetas de las fichas compartidas (las que me dieron y las que yo di). */
    suspend fun infoFichasCompartidas(): List<InfoFichaCompartida> {
        val filas = rpc("info_fichas_compartidas", JSONObject()).jsonArreglo()
        return (0 until filas.length()).map { i ->
            val f = filas.getJSONObject(i)
            InfoFichaCompartida(
                fichaId = f.getString("ficha_id"),
                recibida = f.optString("tipo") == "RECIBIDA",
                autorId = f.optString("autor_id").takeIf { it != "null" }.orEmpty(),
                autorNombre = f.optString("autor_nombre").takeIf { it != "null" }.orEmpty(),
                permiso = f.optString("permiso").takeIf { it != "null" }.orEmpty(),
                personas = f.optInt("personas")
            )
        }
    }

    /** Personas que ven una ficha que yo creé y por qué vía (ficha suelta, barrio, EAIS o centro). */
    suspend fun personasConAccesoFicha(fichaId: String): List<PersonaConAccesoFicha> {
        val filas = rpc("personas_con_acceso_ficha", JSONObject().put("p_ficha_id", fichaId)).jsonArreglo()
        return (0 until filas.length()).map { i ->
            val f = filas.getJSONObject(i)
            PersonaConAccesoFicha(
                usuarioId = f.getString("usuario_id"),
                nombre = f.optString("nombres").takeIf { it.isNotBlank() && it != "null" }
                    ?: f.optString("correo").takeIf { it.isNotBlank() && it != "null" } ?: "Persona sin nombre",
                cargo = f.optString("cargo").takeIf { it != "null" }.orEmpty(),
                permiso = f.optString("permiso"),
                via = f.optString("via")
            )
        }
    }

    /** Quita a una persona el acceso a una sola ficha mía (solo el acceso dado ficha por ficha). */
    suspend fun quitarAccesoFicha(fichaId: String, usuarioId: String) {
        rpc("quitar_acceso_ficha", JSONObject().put("p_ficha_id", fichaId).put("p_usuario_id", usuarioId))
    }

    /** Fichas sueltas que le compartí a una persona. */
    suspend fun fichasCompartidasCon(usuarioId: String): List<FichaCompartidaCon> {
        val filas = rpc("fichas_compartidas_con", JSONObject().put("p_usuario_id", usuarioId)).jsonArreglo()
        return (0 until filas.length()).map { i ->
            val f = filas.getJSONObject(i)
            FichaCompartidaCon(
                fichaId = f.getString("ficha_id"),
                numero = f.optString("numero").takeIf { it != "null" }.orEmpty(),
                jefe = f.optString("jefe").takeIf { it != "null" }.orEmpty(),
                permiso = f.optString("permiso")
            )
        }
    }

    /** Le asigna una visita a alguien con quien ya compartí la ficha; le aparece en su agenda al sincronizar. */
    suspend fun asignarVisita(fichaId: String, usuarioId: String, fechaHoraMillis: Long, nota: String) {
        rpc(
            "asignar_visita",
            JSONObject()
                .put("p_ficha_id", fichaId)
                .put("p_usuario_id", usuarioId)
                .put("p_fecha_hora", fechaHoraMillis)
                .put("p_tipo", "Visita domiciliaria")
                .put("p_nota", nota.trim())
        )
    }

    suspend fun actualizarPerfil(perfil: PerfilRemoto) {
        val basico = JSONObject()
            .put("cedula", perfil.cedula)
            .put("nombres", perfil.nombres)
            .put("cargo", perfil.cargo)
            .put("telefono", perfil.telefono)
            .put("codigo_senescyt", perfil.codigoSenescyt.uppercase())
        val token = tokenValido()
        val ruta = "/rest/v1/perfiles?id=eq.${codificar(perfil.id)}"
        val cabeceras = mapOf("Prefer" to "return=minimal")
        // Si la migración de sexo/apellidos aún no está en Supabase, se guardan solo los datos básicos.
        runCatching {
            solicitar(
                metodo = "PATCH",
                ruta = ruta,
                cuerpo = JSONObject(basico.toString())
                    .put("sexo", perfil.sexo)
                    .put("apellidos", perfil.apellidos),
                accessToken = token,
                headers = cabeceras
            )
        }.getOrElse {
            solicitar(
                metodo = "PATCH",
                ruta = ruta,
                cuerpo = basico,
                accessToken = token,
                headers = cabeceras
            )
        }
        // Respaldo en los datos de la cuenta, que no dependen de columnas nuevas.
        runCatching {
            solicitar(
                metodo = "PUT",
                ruta = "/auth/v1/user",
                cuerpo = JSONObject().put(
                    "data",
                    JSONObject().put("sexo", perfil.sexo).put("apellidos", perfil.apellidos)
                ),
                accessToken = token
            )
        }
    }

    suspend fun crearOrganizacion(nombre: String, codigo: String): String {
        val respuesta = rpc(
            "crear_organizacion_inicial",
            JSONObject()
                .put("p_nombre", nombre)
                .put("p_codigo", codigo.uppercase())
                .put("p_establecimiento_id", JSONObject.NULL)
        )
        val id = respuesta.texto.trim().trim('"')
        require(id.isNotBlank()) { "Supabase no devolvio la organizacion." }
        sesionSegura.guardarOrganizacion(id)
        return id
    }

    suspend fun aceptarInvitacion(codigo: String): String {
        val respuesta = rpc("aceptar_invitacion", JSONObject().put("p_codigo", codigo.uppercase()))
        val id = respuesta.texto.trim().trim('"')
        require(id.isNotBlank()) { "Supabase no devolvio la organizacion." }
        sesionSegura.guardarOrganizacion(id)
        return id
    }

    suspend fun crearInvitacion(
        organizacionId: String,
        correo: String,
        rol: String
    ): InvitacionRemota {
        val arreglo = rpc(
            "crear_invitacion",
            JSONObject()
                .put("p_organizacion_id", organizacionId)
                .put("p_correo", correo.lowercase().ifBlank { JSONObject.NULL })
                .put("p_rol", rol.uppercase())
                .put("p_establecimiento_id", JSONObject.NULL)
                .put("p_horas_vigencia", 168)
        ).jsonArreglo()
        if (arreglo.length() == 0) throw ErrorSupabase("Supabase no devolvió el código de invitación.")
        return arreglo.getJSONObject(0).let {
            InvitacionRemota(
                codigo = it.getString("codigo"),
                expiraEn = it.optString("expira_en")
            )
        }
    }

    /** Personas con acceso a la Sala (solo la ve un administrador). */
    suspend fun listarEquipo(organizacionId: String): List<MiembroEquipoRemoto> {
        val org = codificar(organizacionId)
        val miembros = seleccionarPaginado(
            "miembros_organizacion?organizacion_id=eq.$org&select=usuario_id,rol,activo&order=creado_en.asc"
        )
        if (miembros.isEmpty()) return emptyList()
        val accesos = seleccionarPaginado(
            "accesos_sala?organizacion_id=eq.$org&activo=eq.true&select=usuario_id,permiso,alcance"
        )
        // Las fichas sueltas compartidas viven en otra tabla; si la actualización de Supabase aún no está, se omite.
        val accesosFicha = runCatching {
            seleccionarPaginado(
                "accesos_ficha?organizacion_id=eq.$org&activo=eq.true&select=usuario_id,permiso&order=usuario_id.asc,ficha_id.asc"
            )
        }.getOrDefault(emptyList())
        val ids = miembros.map { it.getString("usuario_id") }.distinct()
        val perfiles = ids.chunked(50).flatMap { lote ->
            seleccionar("perfiles?id=in.(${lote.joinToString(",")})&select=id,nombres,apellidos,cargo,correo")
                .let { filas -> (0 until filas.length()).map { filas.getJSONObject(it) } }
        }.associateBy { it.getString("id") }
        return miembros.map { fila ->
            val usuarioId = fila.getString("usuario_id")
            val perfil = perfiles[usuarioId]
            val permisosSala = accesos.filter { it.getString("usuario_id") == usuarioId }
            val permisos = permisosSala + accesosFicha.filter { it.getString("usuario_id") == usuarioId }
            MiembroEquipoRemoto(
                usuarioId = usuarioId,
                nombre = listOfNotNull(perfil?.optString("nombres"), perfil?.optString("apellidos"))
                    .filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Sin nombre" },
                cargo = perfil?.optString("cargo").orEmpty(),
                correo = perfil?.optString("correo").orEmpty(),
                rol = fila.optString("rol"),
                activo = fila.optBoolean("activo", true),
                permiso = permisos.maxByOrNull { rangoPermiso(it.optString("permiso")) }
                    ?.optString("permiso").orEmpty(),
                alcanceLimitado = permisos.isNotEmpty() && permisos.none { it.optString("alcance") == "SALA" }
            )
        }
    }

    /**
     * Quita el acceso de una persona a la Sala. Sus fichas y las de sus compañeros dejan de ser visibles para ella
     * en la próxima sincronización de su teléfono, que además las retira de allí.
     */
    suspend fun revocarAcceso(organizacionId: String, usuarioId: String) {
        val org = codificar(organizacionId)
        val usuario = codificar(usuarioId)
        val token = tokenValido()
        solicitar(
            "PATCH", "/rest/v1/accesos_sala?organizacion_id=eq.$org&usuario_id=eq.$usuario",
            cuerpo = JSONObject().put("activo", false), accessToken = token,
            headers = mapOf("Prefer" to "return=minimal")
        )
        solicitar(
            "PATCH", "/rest/v1/miembros_organizacion?organizacion_id=eq.$org&usuario_id=eq.$usuario",
            cuerpo = JSONObject().put("activo", false), accessToken = token,
            headers = mapOf("Prefer" to "return=minimal")
        )
        // Las fichas sueltas que se le compartieron también se retiran (si la tabla aún no existe, no hay nada que quitar).
        runCatching {
            solicitar(
                "PATCH", "/rest/v1/accesos_ficha?organizacion_id=eq.$org&usuario_id=eq.$usuario",
                cuerpo = JSONObject().put("activo", false), accessToken = token,
                headers = mapOf("Prefer" to "return=minimal")
            )
        }
    }

    suspend fun rpc(nombre: String, cuerpo: JSONObject): RespuestaHttp =
        solicitar("POST", "/rest/v1/rpc/$nombre", cuerpo, tokenValido())

    suspend fun seleccionar(rutaConConsulta: String): JSONArray =
        solicitar("GET", "/rest/v1/$rutaConConsulta", accessToken = tokenValido()).jsonArreglo()

    suspend fun seleccionarPaginado(rutaConConsulta: String, tamanoPagina: Int = 500): List<JSONObject> {
        // Sin un orden explícito, limit/offset puede repetir o saltar filas si algo cambia mientras se descarga.
        if (!rutaConConsulta.contains("order=")) return seleccionarPorCursor(rutaConConsulta, "id", null, tamanoPagina)
        val resultado = mutableListOf<JSONObject>()
        var offset = 0
        while (true) {
            val separador = if (rutaConConsulta.contains('?')) "&" else "?"
            val pagina = seleccionar("$rutaConConsulta${separador}limit=$tamanoPagina&offset=$offset")
            for (indice in 0 until pagina.length()) resultado += pagina.getJSONObject(indice)
            if (pagina.length() < tamanoPagina) break
            offset += pagina.length()
        }
        return resultado
    }

    /**
     * Lectura por cursor: orden fijo por [campo] y luego por id, y cada página empieza justo después de la
     * última fila vista. A diferencia de limit/offset, un cambio hecho mientras se descarga no hace saltar filas.
     * Con [desde] solo trae filas cuyo [campo] sea igual o posterior.
     */
    suspend fun seleccionarPorCursor(
        rutaConConsulta: String,
        campo: String = "updated_at",
        desde: String? = null,
        tamanoPagina: Int = 500
    ): List<JSONObject> {
        val resultado = mutableListOf<JSONObject>()
        val separador = if (rutaConConsulta.contains('?')) "&" else "?"
        var ultimoValor: String? = null
        var ultimoId: String? = null
        while (true) {
            val filtro = when {
                ultimoValor != null ->
                    "&or=($campo.gt.${codificar(ultimoValor)},and($campo.eq.${codificar(ultimoValor)}," +
                        "id.gt.${codificar(checkNotNull(ultimoId))}))"
                desde != null -> "&$campo=gte.${codificar(desde)}"
                else -> ""
            }
            val pagina = seleccionar(
                "$rutaConConsulta${separador}order=$campo.asc,id.asc&limit=$tamanoPagina$filtro"
            )
            for (indice in 0 until pagina.length()) resultado += pagina.getJSONObject(indice)
            if (pagina.length() < tamanoPagina) break
            val ultima = pagina.getJSONObject(pagina.length() - 1)
            ultimoValor = ultima.getString(campo)
            ultimoId = ultima.getString("id")
        }
        return resultado
    }

    /** Borra un archivo del almacenamiento; si ya no existe no es un error. */
    suspend fun eliminarArchivoAlmacenamiento(rutaStorage: String) {
        val ruta = rutaStorage.split('/').joinToString("/") { codificar(it) }
        try {
            solicitar(
                "DELETE", "/storage/v1/object/fichas-adjuntos/$ruta",
                accessToken = tokenValido()
            )
        } catch (error: ErrorSupabase) {
            if (error.codigoHttp != 404) throw error
        }
    }

    suspend fun upsert(tabla: String, objeto: JSONObject, conflicto: String = "id"): JSONObject? {
        val respuesta = solicitar(
            "POST",
            "/rest/v1/$tabla?on_conflict=${codificar(conflicto)}",
            cuerpo = objeto,
            accessToken = tokenValido(),
            headers = mapOf("Prefer" to "resolution=merge-duplicates,return=representation")
        ).jsonArreglo()
        return if (respuesta.length() > 0) respuesta.getJSONObject(0) else null
    }

    /** Inserta solo si el identificador no existe; una fila vacía señala conflicto. */
    suspend fun insertarPrivadoNuevo(tabla: String, objeto: JSONObject): JSONObject? {
        val filas = solicitar(
            "POST", "/rest/v1/$tabla?on_conflict=id", cuerpo = objeto,
            accessToken = tokenValido(),
            headers = mapOf("Prefer" to "resolution=ignore-duplicates,return=representation")
        ).jsonArreglo()
        return if (filas.length() > 0) filas.getJSONObject(0) else null
    }

    /** Actualización condicional: evita que otro dispositivo pierda cambios ya enviados. */
    suspend fun actualizarPrivadoSiVersion(
        tabla: String, id: String, version: Long, cambios: JSONObject
    ): JSONObject? {
        val filas = solicitar(
            "PATCH", "/rest/v1/$tabla?id=eq.${codificar(id)}&version=eq.$version",
            cuerpo = cambios, accessToken = tokenValido(),
            headers = mapOf("Prefer" to "return=representation")
        ).jsonArreglo()
        return if (filas.length() > 0) filas.getJSONObject(0) else null
    }

    suspend fun upsertVarios(tabla: String, objetos: List<JSONObject>, conflicto: String = "id") {
        objetos.chunked(100).forEach { lote ->
            suspend fun enviar(filas: List<JSONObject>) = solicitar(
                "POST",
                "/rest/v1/$tabla?on_conflict=${codificar(conflicto)}",
                cuerpo = JSONArray().apply { filas.forEach(::put) },
                accessToken = tokenValido(),
                headers = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
            )
            try {
                enviar(lote)
            } catch (error: ErrorSupabase) {
                // Si el servidor aún no tiene una columna nueva (falta aplicar su migración), el resto de los datos
                // sigue sincronizándose; lo nuevo viajará cuando la columna exista.
                val faltantes = COLUMNAS_RECIENTES.filter { error.message.orEmpty().contains(it) }
                if (faltantes.isEmpty()) throw error
                enviar(lote.map { fila -> JSONObject(fila.toString()).also { copia -> faltantes.forEach(copia::remove) } })
            }
        }
    }

    suspend fun insertarIgnorando(tabla: String, objeto: JSONObject, conflicto: String = "id") {
        solicitar(
            "POST",
            "/rest/v1/$tabla?on_conflict=${codificar(conflicto)}",
            cuerpo = objeto,
            accessToken = tokenValido(),
            headers = mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal")
        )
    }

    suspend fun insertarIgnorandoVarios(tabla: String, objetos: List<JSONObject>) {
        objetos.chunked(100).forEach { lote ->
            solicitar(
                "POST",
                "/rest/v1/$tabla?on_conflict=id",
                cuerpo = JSONArray().apply { lote.forEach(::put) },
                accessToken = tokenValido(),
                headers = mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal")
            )
        }
    }

    suspend fun marcarEliminado(tabla: String, id: String) {
        val modificados = solicitar(
            "PATCH",
            "/rest/v1/$tabla?id=eq.${codificar(id)}",
            cuerpo = JSONObject().put("deleted_at", fechaHoraIso()),
            accessToken = tokenValido(),
            headers = mapOf("Prefer" to "return=representation")
        ).jsonArreglo()
        if (modificados.length() == 0) {
            val existente = seleccionar("$tabla?id=eq.${codificar(id)}&select=id&limit=1")
            if (existente.length() > 0) {
                throw ErrorSupabase("Supabase no permitió eliminar el registro. Revisa los permisos del grupo.")
            }
        }
    }

    /**
     * Marca como eliminados varios registros de una tabla en una sola petición. Lo que no existe en el servidor
     * (nunca llegó a subirse) se da por eliminado; si existe pero el servidor no lo modificó, es un problema de permisos.
     */
    suspend fun marcarEliminados(tabla: String, ids: List<String>) {
        if (ids.isEmpty()) return
        val lista = ids.joinToString(",") { codificar(it) }
        val modificados = solicitar(
            "PATCH",
            "/rest/v1/$tabla?id=in.($lista)",
            cuerpo = JSONObject().put("deleted_at", fechaHoraIso()),
            accessToken = tokenValido(),
            headers = mapOf("Prefer" to "return=representation")
        ).jsonArreglo()
        if (modificados.length() >= ids.size) return
        val tocados = (0 until modificados.length()).map { modificados.getJSONObject(it).optString("id") }.toSet()
        val faltantes = ids.filter { it !in tocados }
        val visibles = seleccionar("$tabla?id=in.(${faltantes.joinToString(",") { codificar(it) }})&select=id")
        if (visibles.length() > 0) {
            throw ErrorSupabase("Supabase no permitió eliminar el registro. Revisa los permisos del grupo.")
        }
    }

    suspend fun subirAdjunto(uri: Uri, rutaStorage: String, mimeType: String): Long =
        withContext(Dispatchers.IO) {
            val token = tokenValido()
            val ruta = rutaStorage.split('/').joinToString("/") { codificar(it) }
            val conexion = URI.create("$baseUrl/storage/v1/object/fichas-adjuntos/$ruta")
                .toURL().openConnection() as HttpURLConnection
            try {
                conexion.requestMethod = "POST"
                conexion.connectTimeout = 20_000
                conexion.readTimeout = 60_000
                conexion.doOutput = true
                conexion.setChunkedStreamingMode(64 * 1024)
                conexion.setRequestProperty("apikey", clavePublicable)
                conexion.setRequestProperty("Authorization", "Bearer $token")
                conexion.setRequestProperty("Content-Type", mimeType)
                conexion.setRequestProperty("x-upsert", "true")
                var total = 0L
                appContext.contentResolver.openInputStream(uri)?.use { entrada ->
                    conexion.outputStream.use { salida ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val leidos = entrada.read(buffer)
                            if (leidos < 0) break
                            salida.write(buffer, 0, leidos)
                            total += leidos
                        }
                    }
                } ?: throw ErrorSupabase("No se pudo leer el archivo adjunto.")
                val codigo = conexion.responseCode
                if (codigo !in 200..299) {
                    val texto = conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw ErrorSupabase(mensajeError(texto, codigo), codigo)
                }
                total
            } finally {
                conexion.disconnect()
            }
        }

    suspend fun descargarAdjunto(rutaStorage: String, destino: File): Uri = withContext(Dispatchers.IO) {
        val token = tokenValido()
        val ruta = rutaStorage.split('/').joinToString("/") { codificar(it) }
        val conexion = URI.create("$baseUrl/storage/v1/object/fichas-adjuntos/$ruta")
            .toURL().openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 20_000
            conexion.readTimeout = 60_000
            conexion.setRequestProperty("apikey", clavePublicable)
            conexion.setRequestProperty("Authorization", "Bearer $token")
            val codigo = conexion.responseCode
            if (codigo !in 200..299) {
                val texto = conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw ErrorSupabase(mensajeError(texto, codigo), codigo)
            }
            val directorio = checkNotNull(destino.parentFile) {
                "El adjunto debe guardarse dentro de un directorio."
            }.apply { mkdirs() }
            val temporal = File(directorio, "${destino.name}.tmp")
            conexion.inputStream.use { entrada ->
                temporal.outputStream().use { salida -> entrada.copyTo(salida, 64 * 1024) }
            }
            if (destino.exists() && !destino.delete()) throw IOException("No se pudo reemplazar el adjunto local.")
            if (!temporal.renameTo(destino)) {
                temporal.copyTo(destino, overwrite = true)
                temporal.delete()
            }
            Uri.fromFile(destino)
        } finally {
            conexion.disconnect()
        }
    }

    suspend fun tokenValido(): String = mutexRefresh.withLock {
        val actual = sesionSegura.obtener() ?: throw ErrorSupabase("No existe una sesion iniciada.", 401)
        if (actual.expiraEnSegundos > ahoraSegundos() + 90) return@withLock actual.accessToken
        if (actual.refreshToken.isBlank()) throw ErrorSupabase("La sesion vencio. Inicia sesion nuevamente.", 401)
        val respuesta = solicitar(
            "POST",
            "/auth/v1/token?grant_type=refresh_token",
            JSONObject().put("refresh_token", actual.refreshToken)
        ).jsonObjeto()
        guardarSesion(respuesta).accessToken
    }

    private fun guardarSesionSiExiste(respuesta: JSONObject): SesionSupabase? =
        if (respuesta.optString("access_token").isBlank()) null else guardarSesion(respuesta)

    private fun guardarSesion(respuesta: JSONObject): SesionSupabase {
        val access = respuesta.getString("access_token")
        val usuario = respuesta.optJSONObject("user")
        val sesion = SesionSupabase(
            accessToken = access,
            refreshToken = respuesta.optString("refresh_token"),
            expiraEnSegundos = respuesta.optLong("expires_at").takeIf { it > 0 }
                ?: (ahoraSegundos() + respuesta.optLong("expires_in", 3600L)),
            usuarioId = usuario?.optString("id").orEmpty().ifBlank { extraerSubjectJwt(access) },
            correo = usuario?.optString("email").orEmpty()
        )
        sesionSegura.guardar(sesion)
        return sesion
    }

    private suspend fun solicitar(
        metodo: String,
        ruta: String,
        cuerpo: Any? = null,
        accessToken: String? = null,
        headers: Map<String, String> = emptyMap()
    ): RespuestaHttp = withContext(Dispatchers.IO) {
        val conexion = URI.create(baseUrl + ruta).toURL().openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = metodo
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 30_000
            conexion.setRequestProperty("apikey", clavePublicable)
            conexion.setRequestProperty("Accept", "application/json")
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (!accessToken.isNullOrBlank()) conexion.setRequestProperty("Authorization", "Bearer $accessToken")
            headers.forEach(conexion::setRequestProperty)
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            }
            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (codigo !in 200..299) throw ErrorSupabase(mensajeError(texto, codigo), codigo)
            RespuestaHttp(codigo, texto)
        } finally {
            conexion.disconnect()
        }
    }

    private fun JSONObject.aTerritorioRemoto(): TerritorioRemoto = TerritorioRemoto(
        id = getString("id"),
        organizacionId = getString("organizacion_id"),
        eaisId = getString("eais_id"),
        tipo = optString("tipo", "BARRIO"),
        nombre = optString("nombre"),
        activo = optBoolean("activo", true)
    )
    private fun mensajeError(texto: String, codigo: Int): String {
        val remoto = runCatching {
            JSONObject(texto).let {
                it.optString("msg").ifBlank {
                    it.optString("message").ifBlank { it.optString("error_description") }
                }
            }
        }.getOrNull().orEmpty()
        val traducido = when {
            codigo == 400 && remoto.contains("Invalid login", true) -> "Correo o contraseña incorrectos."
            codigo == 400 && remoto.contains("Email not confirmed", true) -> "Confirma primero el correo electrónico."
            codigo == 422 && remoto.contains("already", true) -> "Este correo ya esta registrado."
            codigo == 403 || remoto.contains("row-level security", true) ->
                "No tienes permiso para guardar estos datos en la Sala. Pide a un administrador que te dé acceso de edición."
            codigo == 429 -> "Demasiados intentos. Espera un momento y vuelve a intentar."
            codigo >= 500 -> "Supabase no está disponible temporalmente."
            else -> remoto.ifBlank { "Error de comunicación con Supabase ($codigo)." }
        }
        return traducido
    }

    private fun extraerSubjectJwt(jwt: String): String = runCatching {
        val parte = jwt.split('.')[1]
        val json = String(android.util.Base64.decode(parte, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
        JSONObject(json).optString("sub")
    }.getOrDefault("")

    private fun rangoPermiso(permiso: String): Int = when (permiso.uppercase()) {
        "ADMINISTRADOR" -> 3
        "EDITOR" -> 2
        else -> 1
    }
    private fun codificar(valor: String): String =
        URLEncoder.encode(valor, StandardCharsets.UTF_8.name())

    private fun ahoraSegundos(): Long = System.currentTimeMillis() / 1000L

    private fun fechaHoraIso(): String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        Locale.US
    ).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    companion object {
        /** Columnas agregadas en versiones recientes: si el servidor no las tiene, se omiten al subir. */
        private val COLUMNAS_RECIENTES = listOf("factores_riesgo_edad_json", "factores_obstetricos_json")

        /** Solo para pruebas del emulador: apunta la app a un servidor de mentira en este mismo teléfono. */
        @Volatile
        var urlParaPruebas: String? = null

        const val REDIRECT_AUTH = "ruralitos://auth-callback"
    }
}

data class RespuestaHttp(val codigo: Int, val texto: String) {
    fun jsonObjeto(): JSONObject = if (texto.isBlank()) JSONObject() else JSONObject(texto)
    fun jsonArreglo(): JSONArray = if (texto.isBlank()) JSONArray() else JSONArray(texto)
}
