package com.ruralitos.app.data.sync

import android.content.Context
import android.net.Uri
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.GestionRiesgoEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.MortalidadFamiliarEntity
import com.ruralitos.app.data.local.entity.ValorRiesgoEntity
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.data.remote.ErrorSupabase
import com.ruralitos.app.data.remote.InfoFichaCompartida
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.room.withTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ResultadoSincronizacion(
    val subidas: Int,
    val eliminaciones: Int,
    val errores: Int,
    val descargadas: Int = 0,
    val erroresReintentables: Int = 0,
    /** Fichas que otra persona modificó mientras se editaban aquí: esperan una decisión del usuario. */
    val conflictos: Int = 0
)

internal fun errorSincronizacionReintentable(error: Throwable): Boolean = when (error) {
    is ErrorSupabase -> error.codigoHttp == null || error.codigoHttp == 408 ||
        error.codigoHttp == 429 || (error.codigoHttp?.let { it in 500..599 } == true)
    is FileNotFoundException -> false
    is IOException -> true
    else -> false
}

/** Su autor eliminó la ficha en la nube mientras este teléfono tenía cambios sin subir: la baja manda. */
internal class FichaEliminadaEnNubeException : Exception("La ficha fue eliminada por su autor.")

/** Marca interna: al subir, la nube tenía un cambio más reciente y es la que gana (ver [guardarCabeceraCon]). */
private const val GANA_NUBE = "_gana_nube"

private data class ResultadoDescarga(val descargadas: Int, val errores: Int, val reintentables: Int)

private class GrupoFicha {
    var cabecera: JSONObject? = null
    val miembros = mutableListOf<JSONObject>()
    val embarazadas = mutableListOf<JSONObject>()
    val mortalidad = mutableListOf<JSONObject>()
    val calificaciones = mutableListOf<JSONObject>()
    val valores = mutableListOf<JSONObject>()
    val gestiones = mutableListOf<JSONObject>()
    val contaminaciones = mutableListOf<JSONObject>()
    val lugares = mutableListOf<JSONObject>()
    val adjuntos = mutableListOf<JSONObject>()
    val historial = mutableListOf<JSONObject>()
}

class SincronizadorSupabase(context: Context) {
    private val appContext = context.applicationContext
    private val database = RuralitosDatabase.obtenerBaseDatos(appContext)
    private val dao = database.sincronizacionDao()
    private val api = SupabaseApi(appContext)
    private val privados = SincronizadorPrivado(appContext, api)
    private val marcas = MarcasDescarga(appContext)
    private val aplicador = AplicadorDescarga(database, ::eliminarArchivoInternoSiCorresponde)

    suspend fun ejecutar(soloSubidas: Boolean = false): ResultadoSincronizacion =
        bloqueoProceso.withLock { ejecutarSerializado(soloSubidas) }

    /**
     * Revisión barata (una consulta por Sala): ¿hay algo nuevo en la nube desde la última sincronización completa?
     * No descarga nada. Se usa con la aplicación abierta para enterarse de los cambios de los demás en pocos segundos.
     */
    suspend fun hayNovedades(): Boolean {
        val salas = database.salaDao().listarSalas()
        return salas.any { sala ->
            val huella = runCatching { api.huellaDeCambios(sala.organizacionId) }.getOrDefault("")
            huella.isNotBlank() && huella != marcas.huella(sala.organizacionId)
        }
    }

    /**
     * Al abrir una ficha compartida para trabajar en ella: se trae su última versión (y sus datos) de la nube, para
     * empezar siempre sobre lo más reciente. No toca una ficha con cambios sin subir. Devuelve verdadero si hubo algo que traer.
     */
    suspend fun refrescarFicha(fichaId: Long): Boolean = bloqueoProceso.withLock {
        val syncId = dao.fichaSyncId(fichaId) ?: return@withLock false
        val ficha = dao.fichaPorSyncId(syncId) ?: return@withLock false
        if (ficha.syncEstado == "PENDIENTE" || ficha.syncEstado == "ERROR" || ficha.syncEstado == "CONFLICTO") return@withLock false
        val cabecera = api.seleccionar("fichas_familiares?id=eq.$syncId&select=*").optJSONObject(0)
            ?: return@withLock false
        val org = ficha.organizacionId
        if (cabecera.estaEliminada()) {
            aplicador.eliminarFichas(org, setOf(syncId))
            return@withLock true
        }
        val grupo = GrupoFicha().also { it.cabecera = cabecera }
        traerHijosDeFicha(syncId, grupo)
        aplicador.aplicar(org, construirDatos(org, syncId, grupo), completo = true)
    }

    /**
     * Después de quitarme el acceso a lo que alguien me compartió: borra de este teléfono las fichas de esa persona que
     * el servidor ya no me deja ver (las que sigo viendo por otra vía se quedan). Nunca toca fichas con cambios sin subir.
     */
    suspend fun retirarFichasDeAutorSinAcceso(autorId: String): Int = bloqueoProceso.withLock {
        val aunVisibles = api.infoFichasCompartidas().filter { it.recibida }.mapTo(mutableSetOf()) { it.fichaId }
        val retirar = dao.fichasSincronizadasDeAutor(autorId).filter { it.syncId !in aunVisibles }
        retirar.groupBy { it.organizacionId }.forEach { (org, fichas) ->
            aplicador.eliminarFichas(org, fichas.mapTo(mutableSetOf()) { it.syncId })
        }
        retirar.size
    }

    private suspend fun ejecutarSerializado(soloSubidas: Boolean): ResultadoSincronizacion {
        api.tokenValido()
        val salasLocales = if (soloSubidas) database.salaDao().listarSalas() else emptyList()
        val salas = salasLocales.ifEmpty { SincronizadorSalas.actualizar(database, api) }
        if (!soloSubidas) retirarSalasSinAcceso(salas.map { it.organizacionId }.toSet())
        if (salas.isEmpty()) return ResultadoSincronizacion(0, 0, 0)

        val activa = api.organizacionGuardada()
            ?.takeIf { id -> salas.any { it.organizacionId == id } }
            ?: salas.first().organizacionId
        api.guardarOrganizacionActiva(activa)
        dao.asignarOrganizacionPendiente(activa)

        var totalSubidas = 0
        var totalEliminaciones = 0
        var totalErrores = 0
        var totalDescargas = 0
        var totalReintentables = 0
        var totalConflictos = 0
        salas.forEach { sala ->
            // La huella se toma ANTES de sincronizar: si alguien cambia algo durante la sincronización, la próxima revisión lo notará.
            val huellaAntes = if (soloSubidas) "" else runCatching { api.huellaDeCambios(sala.organizacionId) }.getOrDefault("")
            val parcial = sincronizarSala(sala.organizacionId, soloSubidas)
            if (!soloSubidas && parcial.errores == 0 && huellaAntes.isNotBlank()) {
                marcas.guardarHuella(sala.organizacionId, huellaAntes)
            }
            totalSubidas += parcial.subidas
            totalEliminaciones += parcial.eliminaciones
            totalErrores += parcial.errores
            totalDescargas += parcial.descargadas
            totalReintentables += parcial.erroresReintentables
            totalConflictos += parcial.conflictos
        }
        return ResultadoSincronizacion(
            subidas = totalSubidas,
            eliminaciones = totalEliminaciones,
            errores = totalErrores,
            descargadas = totalDescargas,
            erroresReintentables = totalReintentables,
            conflictos = totalConflictos
        )
    }

    /**
     * Si a esta cuenta le quitaron una Sala por completo, sus fichas ya sincronizadas se retiran del teléfono.
     * Las que tengan cambios sin subir se conservan (son trabajo de esta persona que aún no llegó al servidor).
     */
    private suspend fun retirarSalasSinAcceso(salasConAcceso: Set<String>) {
        dao.organizacionesLocales().filter { it !in salasConAcceso }.forEach { org ->
            runCatching {
                aplicador.eliminarFichas(org, dao.fichasLocalesSincronizadas(org).mapTo(mutableSetOf()) { it.syncId })
                marcas.olvidar(org)
            }
        }
    }

    private suspend fun sincronizarSala(organizacionId: String, soloSubidas: Boolean): ResultadoSincronizacion {
        var eliminadas = 0
        var errores = 0
        var reintentables = 0
        val conflictos = 0
        // Versiones anteriores dejaban fichas «por revisar»: ahora se resuelven solas al subirlas.
        dao.convertirConflictosEnPendientes()
        procesarEliminaciones(organizacionId).let {
            eliminadas += it.eliminadas
            errores += it.errores
            reintentables += it.reintentables
        }

        var subidas = 0
        dao.fichasPendientes(organizacionId).forEach { ficha ->
            // Una ficha que solo puedo ver no se sube nunca: si quedó marcada como pendiente, se devuelve a sincronizada.
            if (ficha.miPermiso == "LECTOR") {
                runCatching { dao.marcarSincronizadaDescarga(ficha.id, ficha.syncVersion) }
                return@forEach
            }
            runCatching { subirFicha(ficha, organizacionId) }
                .onSuccess { guardadaSinCambiosPosteriores ->
                    if (guardadaSinCambiosPosteriores) subidas++
                }.onFailure { error ->
                    if (error is FichaEliminadaEnNubeException) {
                        // Su autor la eliminó: se quita también de este teléfono.
                        runCatching {
                            dao.marcarSincronizadaDescarga(ficha.id, ficha.syncVersion)
                            aplicador.eliminarFichas(organizacionId, setOf(ficha.syncId))
                        }
                    } else {
                        errores++
                        if (errorSincronizacionReintentable(error)) reintentables++
                        dao.marcarError(
                            ficha.id,
                            error.message.orEmpty().ifBlank { "No se pudo sincronizar." }.take(300)
                        )
                    }
                }
        }

        var descargadas = 0
        if (!soloSubidas) {
            runCatching { descargarDesdeSupabase(organizacionId) }
                .onSuccess {
                    descargadas = it.descargadas
                    errores += it.errores
                    reintentables += it.reintentables
                }
                .onFailure { error -> errores++; if (errorSincronizacionReintentable(error)) reintentables++ }
        }
        if (!soloSubidas) runCatching { actualizarEtiquetasCompartidas(organizacionId) }
        val resultadoPrivado = privados.ejecutar(organizacionId, soloSubidas)
        return ResultadoSincronizacion(
            subidas + resultadoPrivado.subidas,
            eliminadas,
            errores + resultadoPrivado.errores,
            descargadas,
            reintentables + resultadoPrivado.reintentables,
            conflictos
        )
    }

    /**
     * Anota en cada ficha si me la compartieron (y quién) o a cuántas personas se la compartí. Si el servidor aún no
     * tiene la función (migración 20261006100000) o no hay internet, las etiquetas quedan como estaban.
     */
    private suspend fun actualizarEtiquetasCompartidas(org: String) {
        val filas = api.infoFichasCompartidas()
        // Primero se completa lo que falta (fichas recién compartidas) y se retira lo que ya no se puede ver.
        runCatching { ponerAlDiaFichasCompartidas(org, filas) }
        val previas = dao.fichasDeOrganizacion(org).associate { it.syncId to EtiquetaPrevia(it.miPermiso, it.editadaPorOtroEn) }
        // Las etiquetas no son cambios de la ficha: se anotan sin dejarla «pendiente de sincronizar».
        database.withTransaction {
            dao.suspenderSincronizadas()
            dao.limpiarEtiquetasCompartidas(org)
            filas.forEach { fila ->
                if (fila.recibida) dao.marcarFichaRecibida(fila.fichaId, fila.autorId, fila.autorNombre, fila.permiso)
                else {
                    if (fila.personas > 0) dao.marcarFichaCompartidaPorMi(fila.fichaId, fila.personas)
                    if (fila.editadaEn > 0L) dao.marcarFichaEditadaPorOtro(fila.fichaId, fila.editorNombre, fila.editadaEn)
                }
            }
            dao.reanudarSincronizadas()
        }
        // La primera vez solo se toma la foto de lo que ya había; los avisos son para lo que cambie después.
        val preferencias = appContext.getSharedPreferences("avisos_compartidas_ruralitos", Context.MODE_PRIVATE)
        val claveBase = "base_$org"
        if (preferencias.getBoolean(claveBase, false)) {
            runCatching { AvisosCompartidas.mostrar(appContext, AvisosCompartidas.detectar(previas, filas)) }
        } else {
            preferencias.edit().putBoolean(claveBase, true).apply()
        }
    }

    /**
     * La descarga incremental no ve dos cosas: una ficha antigua que acaban de compartirme (su fecha de cambio es vieja)
     * y una a la que me quitaron el acceso. Aquí se baja lo que falta, ficha por ficha, y se retira del teléfono lo que
     * el servidor ya no me deja ver (nunca una ficha con cambios sin subir ni una que sigo viendo).
     */
    private suspend fun ponerAlDiaFichasCompartidas(org: String, filas: List<InfoFichaCompartida>) {
        val locales = dao.fichasDeOrganizacion(org).map { FichaLocalCompartida(it.syncId, it.miPermiso, it.syncEstado) }
        val traspasadas = runCatching { api.fichasTraspasadas() }.getOrDefault(emptyList()).toSet()
        val plan = PlaneadorCompartidas.planear(org, locales, filas, dao.fichasEnBaja(org).toSet(), traspasadas)

        plan.faltantes.chunked(25).forEach { lote ->
            val cabeceras = api.seleccionar(
                "fichas_familiares?organizacion_id=eq.$org&deleted_at=is.null&id=in.(${lote.joinToString(",")})&select=*"
            )
            for (i in 0 until cabeceras.length()) {
                val cabecera = cabeceras.getJSONObject(i)
                val id = cabecera.getString("id")
                runCatching {
                    val grupo = GrupoFicha().also { it.cabecera = cabecera }
                    traerHijosDeFicha(id, grupo)
                    aplicador.aplicar(org, construirDatos(org, id, grupo), completo = true)
                }
            }
        }

        if (plan.candidatasARetirar.isNotEmpty()) {
            val siguenVisibles = mutableSetOf<String>()
            plan.candidatasARetirar.chunked(50).forEach { lote ->
                val visibles = api.seleccionar(
                    "fichas_familiares?deleted_at=is.null&id=in.(${lote.joinToString(",")})&select=id"
                )
                for (i in 0 until visibles.length()) siguenVisibles += visibles.getJSONObject(i).getString("id")
            }
            val retirar = plan.candidatasARetirar.filter { it !in siguenVisibles }
            if (retirar.isNotEmpty()) aplicador.eliminarFichas(org, retirar.toSet())
        }
    }

    // ---- subida ----------------------------------------------------------------------------

    /**
     * Sube la ficha. Si otra persona (o este mismo usuario desde otro teléfono) la cambió mientras tanto, no se
     * pregunta nada: gana el cambio más reciente en los datos de la ficha y los registros nuevos de las dos partes se
     * unen (ver [guardarCabeceraCon] y [subirSoloLoNuevoYTomarDeLaNube]). Devuelve falso si el usuario siguió
     * editando mientras tanto.
     */
    private suspend fun subirFicha(ficha: FichaFamiliarEntity, organizacionId: String): Boolean {
        var version = ficha.syncVersion
        val primera = fichaJson(ficha, organizacionId)
        // Sin firma local, la ficha del servidor también queda sin firma; con firma, esta no cambia hasta subirla.
        if (ficha.firmaUri == null) primera.put("firma_storage_path", JSONObject.NULL)
        var respuesta = guardarCabecera(ficha, primera, version)
        version = respuesta.optLong("version", version + 1)
        dao.fijarVersion(ficha.id, version)

        if (respuesta.optBoolean(GANA_NUBE)) {
            return subirSoloLoNuevoYTomarDeLaNube(ficha, organizacionId, respuesta)
        }

        val firmaRuta = subirFirmaSiExiste(ficha, organizacionId)
        if (firmaRuta != null && respuesta.texto("firma_storage_path") != firmaRuta) {
            respuesta = guardarCabecera(
                ficha, fichaJson(ficha, organizacionId).put("firma_storage_path", firmaRuta), version
            )
            version = respuesta.optLong("version", version + 1)
            dao.fijarVersion(ficha.id, version)
        }
        subirContenido(ficha, organizacionId)
        subirHistorial(ficha, organizacionId)
        return dao.marcarSincronizada(ficha.id, version, ficha.actualizadoEn) > 0
    }

    private suspend fun guardarCabecera(
        ficha: FichaFamiliarEntity,
        json: JSONObject,
        versionBase: Long
    ): JSONObject = try {
        guardarCabeceraCon(ficha, json, versionBase)
    } catch (error: ErrorSupabase) {
        // Si el servidor aún no tiene la columna de símbolos del croquis (falta aplicar su migración), la ficha
        // sigue sincronizándose sin ella; los símbolos viajarán cuando la columna exista.
        if (!error.message.orEmpty().contains("croquis_elementos_json")) throw error
        guardarCabeceraCon(ficha, JSONObject(json.toString()).also { it.remove("croquis_elementos_json") }, versionBase)
    }

    private suspend fun guardarCabeceraCon(
        ficha: FichaFamiliarEntity,
        json: JSONObject,
        versionBase: Long
    ): JSONObject {
        var version = versionBase
        if (version == 0L) {
            api.insertarPrivadoNuevo("fichas_familiares", json)?.let { return it }
            // Ya existía con este identificador: es un intento anterior de este mismo teléfono que no llegó a
            // registrarse. Se toma su versión como base y se actualiza.
            version = api.seleccionar("fichas_familiares?id=eq.${ficha.syncId}&select=version")
                .optJSONObject(0)?.optLong("version", 0L) ?: 0L
            if (version == 0L) throw ErrorSupabase("El servidor no aceptó crear la ficha.")
        }
        api.actualizarPrivadoSiVersion("fichas_familiares", ficha.syncId, version, json)?.let { return it }

        // La ficha cambió en la nube desde la última sincronización de este teléfono: se resuelve sola.
        val remota = api.seleccionar("fichas_familiares?id=eq.${ficha.syncId}&select=*").optJSONObject(0)
            ?: throw ErrorSupabase("No se pudo leer la ficha en la nube.")
        if (remota.estaEliminada()) throw FichaEliminadaEnNubeException()
        val remotaMs = ConversionesSync.timestampMillis(remota.texto("updated_at"), 0L)
        if (ficha.actualizadoEn >= remotaMs) {
            // El cambio de este teléfono es el más reciente: se sube encima de la versión actual de la nube.
            return api.actualizarPrivadoSiVersion("fichas_familiares", ficha.syncId, remota.optLong("version", 0L), json)
                ?: throw ErrorSupabase("La ficha cambió otra vez mientras se sincronizaba; se reintentará.")
        }
        // El cambio de la nube es más reciente: sus datos se quedan, y de este teléfono solo se suman los registros nuevos.
        return remota.put(GANA_NUBE, true)
    }

    /**
     * La nube tenía un cambio más reciente. De este teléfono se suben únicamente los registros que la nube no conoce
     * (integrantes, adjuntos y demás creados aquí), y luego se baja la ficha completa de la nube para que quede igual
     * en todos los teléfonos. Devuelve falso si el usuario siguió editando mientras tanto (se reintenta en la próxima).
     */
    private suspend fun subirSoloLoNuevoYTomarDeLaNube(
        ficha: FichaFamiliarEntity,
        organizacionId: String,
        cabeceraNube: JSONObject
    ): Boolean {
        val enNube = GrupoFicha().also { it.cabecera = cabeceraNube }
        traerHijosDeFicha(ficha.syncId, enNube)
        fun ids(filas: List<JSONObject>) = filas.mapTo(mutableSetOf()) { it.getString("id") }

        val miembros = ids(enNube.miembros)
        api.upsertVarios("miembros_familia", dao.miembros(ficha.id).filter { it.syncId !in miembros }
            .map { miembroJson(it, ficha.syncId, organizacionId) })
        val embarazadas = ids(enNube.embarazadas)
        api.upsertVarios("embarazadas", dao.embarazadas(ficha.id).filter { it.syncId !in embarazadas }
            .map { embarazadaJson(it, ficha.syncId, organizacionId) })
        val mortalidad = ids(enNube.mortalidad)
        api.upsertVarios("mortalidad_familiar", dao.mortalidad(ficha.id).filter { it.syncId !in mortalidad }
            .map { mortalidadJson(it, ficha.syncId, organizacionId) })
        val calificacionesNube = ids(enNube.calificaciones)
        val calificacionesNuevas = dao.calificaciones(ficha.id).filter { it.syncId !in calificacionesNube }
        api.upsertVarios("calificaciones_riesgo", calificacionesNuevas.map { calificacionJson(it, ficha.syncId, organizacionId) })
        api.upsertVarios(
            "valores_riesgo",
            calificacionesNuevas.flatMap { c -> dao.valores(c.id).map { valorJson(it, c.syncId, organizacionId) } },
            "calificacion_id,componente"
        )
        val gestiones = ids(enNube.gestiones)
        api.upsertVarios("gestion_riesgo", dao.gestiones(ficha.id).filter { it.syncId !in gestiones }
            .map { gestionJson(it, ficha.syncId, organizacionId) })
        val contaminaciones = ids(enNube.contaminaciones)
        api.upsertVarios("contaminacion_ambiental", dao.contaminaciones(ficha.id).filter { it.syncId !in contaminaciones }
            .map { contaminacionJson(it, ficha.syncId, organizacionId) })
        val lugares = ids(enNube.lugares)
        api.upsertVarios("lugares_tratamiento", dao.lugares(ficha.id).filter { it.syncId !in lugares }
            .map { lugarJson(it, ficha.syncId, organizacionId) })
        val tiposEnNube = enNube.adjuntos.map { it.texto("tipo") }.toSet()
        dao.adjuntos(ficha.id).filter { it.tipo !in tiposEnNube }.forEach { subirAdjunto(it, ficha.syncId, organizacionId) }
        subirHistorial(ficha, organizacionId)

        // Ahora sí: la ficha completa de la nube (con lo nuevo de este teléfono ya dentro) pasa a este teléfono.
        val actual = dao.fichaPorSyncId(ficha.syncId) ?: return false
        if (actual.actualizadoEn != ficha.actualizadoEn) return false
        val final = GrupoFicha().also { it.cabecera = cabeceraNube }
        traerHijosDeFicha(ficha.syncId, final)
        return aplicador.aplicar(organizacionId, construirDatos(organizacionId, ficha.syncId, final), completo = true, forzar = true)
    }

    private class ResultadoEliminaciones {
        var eliminadas = 0
        var errores = 0
        var reintentables = 0
    }

    /**
     * Manda a la nube lo que el usuario eliminó, en lotes. Primero las fichas (con eso las demás personas y teléfonos
     * ya dejan de verlas) y después el resto de sus registros, con un tiempo máximo por sincronización: lo que falte
     * sigue anotado y continúa en la siguiente. Un lote que falla se reintenta uno por uno para aislar el problema.
     */
    private suspend fun procesarEliminaciones(organizacionId: String): ResultadoEliminaciones {
        val resultado = ResultadoEliminaciones()
        val limite = System.currentTimeMillis() + PRESUPUESTO_BAJAS_MS
        val tablas = dao.tablasConEliminaciones(organizacionId).sortedBy { if (it == "fichas_familiares") 0 else 1 }
        for (tabla in tablas) {
            val esFicha = tabla == "fichas_familiares"
            var lotesSinExito = 0
            for (lote in dao.eliminacionesDeTabla(organizacionId, tabla, MAXIMO_BAJAS_POR_TABLA).chunked(TAMANO_LOTE_BAJAS)) {
                if (!esFicha && (System.currentTimeMillis() > limite || lotesSinExito >= 2)) break
                val enLote = runCatching { marcarEliminadosYLimpiar(tabla, lote.map { it.registroSyncId }) }
                if (enLote.isSuccess) {
                    dao.eliminarTumbas(lote.map { it.id })
                    resultado.eliminadas += lote.size
                    lotesSinExito = 0
                    continue
                }
                val falloDeRed = enLote.exceptionOrNull()?.let(::errorSincronizacionReintentable) == true
                if (falloDeRed) {
                    resultado.errores++
                    resultado.reintentables++
                    return resultado
                }
                var algunaListo = false
                lote.forEach { tumba ->
                    runCatching { marcarEliminadoYLimpiar(tumba.tabla, tumba.registroSyncId) }
                        .onSuccess {
                            dao.eliminarTumba(tumba.id)
                            resultado.eliminadas++
                            algunaListo = true
                        }
                        .onFailure { error ->
                            resultado.errores++
                            if (errorSincronizacionReintentable(error)) resultado.reintentables++
                        }
                }
                lotesSinExito = if (algunaListo) 0 else lotesSinExito + 1
            }
        }
        return resultado
    }

    private suspend fun marcarEliminadosYLimpiar(tabla: String, ids: List<String>) {
        val rutas = runCatching { rutasDeAlmacenamientoVarias(tabla, ids) }.getOrDefault(emptyList())
        val ignoradas = api.marcarEliminados(tabla, ids)
        // Si el servidor ignoró la baja de una ficha (no era de quien la pidió), sus archivos NO se tocan.
        rutas.filter { ruta -> ignoradas.none { id -> "/$id/" in ruta } }
            .forEach { ruta -> runCatching { api.eliminarArchivoAlmacenamiento(ruta) } }
    }

    private suspend fun rutasDeAlmacenamientoVarias(tabla: String, ids: List<String>): List<String> {
        val lista = ids.joinToString(",")
        return when (tabla) {
            "adjuntos_ficha" -> api.seleccionar("adjuntos_ficha?id=in.($lista)&select=storage_path").rutas()
            "fichas_familiares" ->
                api.seleccionar("adjuntos_ficha?ficha_id=in.($lista)&select=storage_path").rutas() +
                    api.seleccionar("fichas_familiares?id=in.($lista)&select=firma_storage_path")
                        .let { filas -> (0 until filas.length()).map { filas.getJSONObject(it).texto("firma_storage_path") } }
                        .filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    private suspend fun marcarEliminadoYLimpiar(tabla: String, id: String) {
        val rutas = runCatching { rutasDeAlmacenamiento(tabla, id) }.getOrDefault(emptyList())
        val ignorada = api.marcarEliminado(tabla, id)
        // Los archivos de una ficha o adjunto eliminado no deben quedarse en el servidor con datos de pacientes.
        // Si el servidor ignoró la baja de una ficha (no era de quien la pidió), sus archivos NO se tocan.
        if (!ignorada) rutas.forEach { ruta -> runCatching { api.eliminarArchivoAlmacenamiento(ruta) } }
    }

    private suspend fun rutasDeAlmacenamiento(tabla: String, id: String): List<String> = when (tabla) {
        "adjuntos_ficha" -> api.seleccionar("adjuntos_ficha?id=eq.$id&select=storage_path").rutas()
        "fichas_familiares" ->
            api.seleccionar("adjuntos_ficha?ficha_id=eq.$id&select=storage_path").rutas() +
                api.seleccionar("fichas_familiares?id=eq.$id&select=firma_storage_path")
                    .let { filas -> (0 until filas.length()).map { filas.getJSONObject(it).texto("firma_storage_path") } }
                    .filter { it.isNotBlank() }
        else -> emptyList()
    }

    private fun org.json.JSONArray.rutas(): List<String> =
        (0 until length()).map { getJSONObject(it).texto("storage_path") }.filter { it.isNotBlank() }

    private suspend fun subirContenido(ficha: FichaFamiliarEntity, organizacionId: String) {
        api.upsertVarios("miembros_familia", dao.miembros(ficha.id).map {
            miembroJson(it, ficha.syncId, organizacionId)
        })
        api.upsertVarios("embarazadas", dao.embarazadas(ficha.id).map {
            embarazadaJson(it, ficha.syncId, organizacionId)
        })
        api.upsertVarios("mortalidad_familiar", dao.mortalidad(ficha.id).map {
            mortalidadJson(it, ficha.syncId, organizacionId)
        })
        val calificaciones = dao.calificaciones(ficha.id)
        api.upsertVarios("calificaciones_riesgo", calificaciones.map {
            calificacionJson(it, ficha.syncId, organizacionId)
        })
        val valores = calificaciones.flatMap { calificacion ->
            dao.valores(calificacion.id).map { valor ->
                valorJson(valor, calificacion.syncId, organizacionId)
            }
        }
        api.upsertVarios("valores_riesgo", valores, "calificacion_id,componente")
        api.upsertVarios("gestion_riesgo", dao.gestiones(ficha.id).map {
            gestionJson(it, ficha.syncId, organizacionId)
        })
        api.upsertVarios("contaminacion_ambiental", dao.contaminaciones(ficha.id).map {
            contaminacionJson(it, ficha.syncId, organizacionId)
        })
        api.upsertVarios("lugares_tratamiento", dao.lugares(ficha.id).map {
            lugarJson(it, ficha.syncId, organizacionId)
        })
        dao.adjuntos(ficha.id).forEach {
            subirAdjunto(it, ficha.syncId, organizacionId)
        }
    }

    private suspend fun subirHistorial(ficha: FichaFamiliarEntity, organizacionId: String) {
        val eventos = dao.historial(ficha.id).map { evento ->
            JSONObject()
                .put("id", evento.syncId)
                .put("organizacion_id", organizacionId)
                .put("ficha_id", ficha.syncId)
                .put("numero_ficha", evento.numeroFicha)
                .put("usuario_nombre", evento.usuarioNombre)
                .put("accion", evento.accion)
                .put("detalle", evento.detalle)
                .put("ocurrido_en", fechaHoraIso(evento.creadoEn))
        }
        api.insertarIgnorandoVarios("historial_fichas", eventos)
    }

    /**
     * El servidor guarda un solo adjunto por ficha y tipo (aunque esté borrado). Si ya existe uno de ese tipo con
     * otro identificador —porque se reemplazó la foto o se quitó y se volvió a dibujar— se reutiliza SU identificador
     * y se reactiva, en vez de chocar con la restricción y dejar la ficha en error.
     */
    private suspend fun subirAdjunto(item: AdjuntoFichaEntity, fichaSyncId: String, organizacionId: String) {
        val uri = Uri.parse(item.uri)
        if (uri.scheme.isNullOrBlank()) return
        val mime = appContext.contentResolver.getType(uri) ?: when (
            uri.path?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
        ) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            else -> "image/jpeg"
        }
        val extension = when (mime) {
            "image/png" -> "png"
            "application/pdf" -> "pdf"
            else -> "jpg"
        }
        val tipoCodificado = java.net.URLEncoder.encode(item.tipo, "UTF-8")
        val existente = api.seleccionar(
            "adjuntos_ficha?ficha_id=eq.$fichaSyncId&tipo=eq.$tipoCodificado&select=id,storage_path&limit=1"
        ).optJSONObject(0)
        val syncId = existente?.texto("id")?.takeIf { it.isNotBlank() } ?: item.syncId
        if (syncId != item.syncId) {
            // El cambio de identificador no es una edición del usuario: se hace en una sola transacción y la ficha
            // conserva su estado y su fecha, para que no quede «pendiente» por un cambio que ya está en el servidor.
            database.withTransaction {
                val ficha = dao.fichaPorSyncId(fichaSyncId)
                if (ficha != null) dao.marcarDescargando(ficha.id)
                dao.cambiarSyncIdAdjunto(item.syncId, syncId)
                if (ficha != null) dao.restablecerEstado(ficha.id, ficha.syncEstado, ficha.actualizadoEn)
            }
        }
        val ruta = "$organizacionId/$fichaSyncId/$syncId-${item.tipo.lowercase()}.$extension"
        val tamano = api.subirAdjunto(uri, ruta, mime)
        api.upsert(
            "adjuntos_ficha",
            JSONObject()
                .put("id", syncId)
                .put("organizacion_id", organizacionId)
                .put("ficha_id", fichaSyncId)
                .put("tipo", item.tipo)
                .put("storage_path", ruta)
                .put("mime_type", mime)
                .put("tamano_bytes", tamano)
                .put("deleted_at", JSONObject.NULL)
        )
        // Si el archivo anterior tenía otra extensión, ya no hace falta.
        existente?.texto("storage_path")?.takeIf { it.isNotBlank() && it != ruta }?.let {
            runCatching { api.eliminarArchivoAlmacenamiento(it) }
        }
    }

    private suspend fun subirFirmaSiExiste(ficha: FichaFamiliarEntity, organizacionId: String): String? {
        val uri = ficha.firmaUri?.let(Uri::parse) ?: return null
        if (uri.scheme.isNullOrBlank()) return null
        val mime = appContext.contentResolver.getType(uri) ?: when (
            uri.path?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
        ) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/png"
        }
        val extension = extensionPara(mime, "png")
        val ruta = "$organizacionId/${ficha.syncId}/firma-${ficha.syncId}.$extension"
        api.subirAdjunto(uri, ruta, mime)
        return ruta
    }

    // ---- descarga --------------------------------------------------------------------------

    /**
     * Trae primero TODO lo que hay que saber del servidor (sin tocar la base) y luego aplica cada ficha en una
     * transacción propia (ver [AplicadorDescarga]). Después de la primera descarga completa solo pide lo que cambió
     * desde la última marca, con una repetición de 2 minutos por si una escritura tardó en confirmarse.
     */
    private suspend fun descargarDesdeSupabase(org: String): ResultadoDescarga {
        val ahora = System.currentTimeMillis()
        val completa = marcas.necesitaDescargaCompleta(org, ahora, dao.contarFichasDeOrganizacion(org) == 0)
        val nuevasMarcas = mutableMapOf<String, String>()

        suspend fun traer(tabla: String, campo: String = "updated_at", conBajas: Boolean = true): List<JSONObject> {
            val filtroBajas = if (completa && conBajas) "&deleted_at=is.null" else ""
            val desde = if (completa) null else marcas.marca(org, tabla)?.let { ConversionesSync.retroceder(it, 120) }
            val filas = api.seleccionarPorCursor(
                "$tabla?organizacion_id=eq.$org$filtroBajas&select=*", campo, desde
            )
            filas.lastOrNull()?.texto(campo)?.takeIf { it.isNotBlank() }?.let { nuevasMarcas[tabla] = it }
            return filas
        }

        val filasFicha = traer("fichas_familiares")
        val miembros = traer("miembros_familia")
        val embarazadas = traer("embarazadas")
        val mortalidad = traer("mortalidad_familiar")
        val calificaciones = traer("calificaciones_riesgo")
        val valores = traer("valores_riesgo")
        val gestiones = traer("gestion_riesgo")
        val contaminaciones = traer("contaminacion_ambiental")
        val lugares = traer("lugares_tratamiento")
        val adjuntos = traer("adjuntos_ficha")
        val historial = traer("historial_fichas", campo = "registrado_en", conBajas = false)

        val bajas = filasFicha.filter { it.estaEliminada() }.mapTo(mutableSetOf()) { it.getString("id") }
        if (completa) {
            bajas += api.seleccionarPorCursor(
                "fichas_familiares?organizacion_id=eq.$org&deleted_at=not.is.null&select=id,updated_at"
            ).map { it.getString("id") }
        }

        val grupos = linkedMapOf<String, GrupoFicha>()
        fun grupo(fichaId: String) = grupos.getOrPut(fichaId) { GrupoFicha() }
        filasFicha.filterNot { it.estaEliminada() }.forEach { grupo(it.getString("id")).cabecera = it }
        miembros.forEach { grupo(it.texto("ficha_id")).miembros += it }
        embarazadas.forEach { grupo(it.texto("ficha_id")).embarazadas += it }
        mortalidad.forEach { grupo(it.texto("ficha_id")).mortalidad += it }
        calificaciones.forEach { grupo(it.texto("ficha_id")).calificaciones += it }
        gestiones.forEach { grupo(it.texto("ficha_id")).gestiones += it }
        contaminaciones.forEach { grupo(it.texto("ficha_id")).contaminaciones += it }
        lugares.forEach { grupo(it.texto("ficha_id")).lugares += it }
        adjuntos.forEach { grupo(it.texto("ficha_id")).adjuntos += it }
        historial.forEach { grupo(it.texto("ficha_id")).historial += it }
        val fichaDeCalificacion = calificaciones.associate { it.getString("id") to it.texto("ficha_id") }
        valores.forEach { valor ->
            val calificacionId = valor.texto("calificacion_id")
            val fichaId = fichaDeCalificacion[calificacionId] ?: dao.fichaSyncIdDeCalificacion(calificacionId)
            if (fichaId != null) grupo(fichaId).valores += valor
        }
        grupos.keys.removeAll(bajas)
        // Lo que el usuario eliminó en este teléfono y la nube aún no confirmó no debe volver a bajar.
        grupos.keys.removeAll(dao.fichasEnBaja(org).toSet())

        // En el modo incremental puede cambiar un dato hijo sin que cambie la cabecera: si la ficha no está en el
        // teléfono hay que pedir también su cabecera.
        val sinCabecera = grupos.filter { (id, g) -> g.cabecera == null && dao.fichaPorSyncId(id) == null }.keys
        sinCabecera.chunked(50).forEach { lote ->
            api.seleccionar(
                "fichas_familiares?organizacion_id=eq.$org&deleted_at=is.null&id=in.(${lote.joinToString(",")})&select=*"
            ).let { filas -> (0 until filas.length()).forEach { grupos[filas.getJSONObject(it).getString("id")]?.cabecera = filas.getJSONObject(it) } }
        }

        var aplicadas = 0
        var errores = 0
        var reintentables = 0
        grupos.forEach { (syncId, grupo) ->
            val local = dao.fichaPorSyncId(syncId)
            if (local != null && local.syncEstado in setOf("PENDIENTE", "ERROR", "CONFLICTO")) return@forEach
            if (grupo.cabecera == null && local == null) return@forEach
            runCatching {
                val datos = construirDatos(org, syncId, grupo)
                if (aplicador.aplicar(org, datos, completa)) aplicadas++
            }.onFailure { error ->
                errores++
                if (errorSincronizacionReintentable(error)) reintentables++
            }
        }
        // En una descarga completa, lo que el teléfono tiene sincronizado y el servidor ya no muestra (borrado de
        // verdad o permiso retirado) también se retira. Si el servidor no devolvió ninguna ficha no se toca nada.
        val fichasVisibles = filasFicha.filterNot { it.estaEliminada() }.mapTo(mutableSetOf()) { it.getString("id") }
        val ausentes = if (completa && fichasVisibles.isNotEmpty()) {
            dao.fichasLocalesSincronizadas(org).map { it.syncId }.filter { it !in fichasVisibles && it !in bajas }
        } else emptyList()
        runCatching { aplicador.eliminarFichas(org, bajas + ausentes) }
            .onFailure { error -> errores++; if (errorSincronizacionReintentable(error)) reintentables++ }

        // Si algo falló, las marcas no avanzan: la próxima vez se repite lo mismo (aplicarlo dos veces es inocuo).
        if (errores == 0) {
            marcas.guardarMarcas(org, nuevasMarcas)
            if (completa) marcas.registrarCompleta(org, ahora)
        }
        return ResultadoDescarga(aplicadas, errores, reintentables)
    }

    /** Trae de golpe los datos hijos de una sola ficha (para resolver un conflicto). */
    private suspend fun traerHijosDeFicha(fichaSyncId: String, grupo: GrupoFicha) {
        fun consulta(tabla: String) = "$tabla?ficha_id=eq.$fichaSyncId&deleted_at=is.null&select=*"
        grupo.miembros += api.seleccionarPorCursor(consulta("miembros_familia"))
        grupo.embarazadas += api.seleccionarPorCursor(consulta("embarazadas"))
        grupo.mortalidad += api.seleccionarPorCursor(consulta("mortalidad_familiar"))
        grupo.calificaciones += api.seleccionarPorCursor(consulta("calificaciones_riesgo"))
        grupo.gestiones += api.seleccionarPorCursor(consulta("gestion_riesgo"))
        grupo.contaminaciones += api.seleccionarPorCursor(consulta("contaminacion_ambiental"))
        grupo.lugares += api.seleccionarPorCursor(consulta("lugares_tratamiento"))
        grupo.adjuntos += api.seleccionarPorCursor(consulta("adjuntos_ficha"))
        grupo.historial += api.seleccionarPorCursor(
            "historial_fichas?ficha_id=eq.$fichaSyncId&select=*", "registrado_en"
        )
        grupo.calificaciones.map { it.getString("id") }.chunked(50).forEach { lote ->
            val filas = api.seleccionar(
                "valores_riesgo?calificacion_id=in.(${lote.joinToString(",")})&deleted_at=is.null&select=*"
            )
            for (i in 0 until filas.length()) grupo.valores += filas.getJSONObject(i)
        }
    }

    /** Descarga los archivos de la ficha (firma y adjuntos) y los junta con sus filas. */
    private suspend fun construirDatos(org: String, syncId: String, grupo: GrupoFicha): DatosFichaRemota {
        val cabecera = grupo.cabecera
        val firmaUri = cabecera?.texto("firma_storage_path")?.takeIf { it.isNotBlank() }?.let { ruta ->
            val destino = File(
                appContext.filesDir,
                "adjuntos_sincronizados/$org/$syncId/firma.${ruta.substringAfterLast('.', "png")}"
            )
            descargarArchivoSiNecesario(
                ruta, destino,
                ConversionesSync.timestampMillis(cabecera.texto("updated_at"), System.currentTimeMillis())
            )
        }
        val archivos = mutableMapOf<String, String>()
        grupo.adjuntos.filterNot { it.estaEliminada() }.forEach { remoto ->
            val ruta = remoto.texto("storage_path")
            if (ruta.isBlank()) return@forEach
            val adjuntoId = remoto.getString("id")
            val mime = remoto.texto("mime_type").ifBlank { "application/octet-stream" }
            val extension = ruta.substringAfterLast('.', extensionPara(mime, "bin"))
            val destino = File(appContext.filesDir, "adjuntos_sincronizados/$org/$syncId/$adjuntoId.$extension")
            archivos[adjuntoId] = descargarArchivoSiNecesario(
                rutaStorage = ruta,
                destino = destino,
                actualizadoRemoto = ConversionesSync.timestampMillis(remoto.texto("updated_at"), System.currentTimeMillis()),
                tamanoEsperado = remoto.optLong("tamano_bytes", -1L)
            )
        }
        return DatosFichaRemota(
            syncId = syncId,
            cabecera = cabecera,
            miembros = grupo.miembros,
            embarazadas = grupo.embarazadas,
            mortalidad = grupo.mortalidad,
            calificaciones = grupo.calificaciones,
            valores = grupo.valores,
            gestiones = grupo.gestiones,
            contaminaciones = grupo.contaminaciones,
            lugares = grupo.lugares,
            adjuntos = grupo.adjuntos,
            historial = grupo.historial,
            archivosAdjuntos = archivos,
            firmaUri = firmaUri
        )
    }

    private suspend fun descargarArchivoSiNecesario(
        rutaStorage: String,
        destino: File,
        actualizadoRemoto: Long,
        tamanoEsperado: Long = -1L
    ): String {
        val vigente = destino.isFile &&
            (tamanoEsperado <= 0L || destino.length() == tamanoEsperado) &&
            destino.lastModified() >= actualizadoRemoto - 1_000L
        if (!vigente) {
            api.descargarAdjunto(rutaStorage, destino)
            destino.setLastModified(actualizadoRemoto)
        }
        return Uri.fromFile(destino).toString()
    }

    private fun eliminarArchivoInternoSiCorresponde(uriTexto: String) {
        val uri = Uri.parse(uriTexto)
        if (uri.scheme != "file") return
        runCatching {
            val archivo = File(checkNotNull(uri.path)).canonicalFile
            val raiz = appContext.filesDir.canonicalFile
            if (archivo.path.startsWith(raiz.path + File.separator)) archivo.delete()
        }
    }

    private fun extensionPara(mime: String, fallback: String): String = when (mime.lowercase()) {
        "image/png" -> "png"
        "image/jpeg", "image/jpg" -> "jpg"
        "application/pdf" -> "pdf"
        else -> fallback
    }

    private fun fichaJson(item: FichaFamiliarEntity, organizacionId: String) = JSONObject()
        .put("id", item.syncId)
        .put("organizacion_id", organizacionId)
        .put("establecimiento_id", item.establecimientoRemotoId ?: JSONObject.NULL)
        .put("eais_id", item.eaisId.ifBlank { null } ?: JSONObject.NULL)
        .put("territorio_id", item.territorioId.ifBlank { null } ?: JSONObject.NULL)
        .put("cedula_jefe_hogar", item.cedulaJefeHogar)
        .put("institucion_sistema", item.institucionSistema)
        .put("unidad_operativa", item.unidadOperativa)
        .put("codigo_uo", item.codigoUo)
        .put("area_numero", item.areaNumero)
        .put("codigo_localizacion", item.codigoLocalizacion)
        .put("parroquia_codigo_localizacion", item.parroquiaCodigoLocalizacion)
        .put("canton_codigo_localizacion", item.cantonCodigoLocalizacion)
        .put("provincia_codigo_localizacion", item.provinciaCodigoLocalizacion)
        .put("numero_ficha_familiar", item.numeroFichaFamiliar)
        .put("provincia", item.provincia)
        .put("canton", item.canton)
        .put("parroquia", item.parroquia)
        .put("sector", item.sector)
        .put("manzana", item.manzana)
        .put("numero_familia", item.numeroFamilia)
        .put("direccion_habitual_familia", item.direccionHabitualFamilia)
        .put("barrio", item.barrio)
        .put("numero_casa", item.numeroCasa)
        .put("comunidad", item.comunidad)
        .put("grupo_cultural", item.grupoCultural)
        .put("nombre_apellido_jefe_familia", item.nombreApellidoJefeFamilia)
        .put("numero_telefono", item.numeroTelefono)
        .put("fecha_llenado", fechaIsoObligatoria(item.fechaLlenado))
        .put("numero_carpeta", item.numeroCarpeta)
        .put("latitud", item.latitud ?: JSONObject.NULL)
        .put("longitud", item.longitud ?: JSONObject.NULL)
        .put("altitud", item.altitud ?: JSONObject.NULL)
        .put("croquis_elementos_json", item.croquisElementosJson)
        .put("responsable_nombre", item.responsableNombre)
        .put("responsable_codigo", item.responsableCodigo)
        .put("estado", item.estado)

    private fun miembroJson(item: MiembroFamiliaEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("grupo_edad", item.grupoEdad).put("apellidos_nombres", item.apellidosNombres)
        .put("parentesco", item.parentesco).put("fecha_nacimiento", fechaIso(item.fechaNacimiento))
        .put("ocupacion", item.ocupacion).put("sexo", item.sexo).put("escolaridad", item.escolaridad)
        .put("vacunas_completas", item.vacunasCompletas ?: JSONObject.NULL)
        .put("salud_bucal_adecuada", item.saludBucalAdecuada ?: JSONObject.NULL)
        .put("riesgo_enfermedad_discapacidad", item.riesgoEnfermedadDiscapacidad)
        .put("estado_nutricional", item.estadoNutricional)
        .put("hipertension_arterial", item.hipertensionArterial ?: JSONObject.NULL)
        .put("diabetes_mellitus", item.diabetesMellitus ?: JSONObject.NULL)
        .put("tuberculosis", item.tuberculosis ?: JSONObject.NULL)
        .put("problema_salud_mental", item.problemaSaludMental ?: JSONObject.NULL)
        .put("consumo_alcohol_drogas", item.consumoAlcoholDrogas ?: JSONObject.NULL)
        .put("enfermedad_cronica", item.enfermedadCronica ?: JSONObject.NULL)
        .put("discapacidad_visual", item.discapacidadVisual ?: JSONObject.NULL)
        .put("discapacidad_auditiva", item.discapacidadAuditiva ?: JSONObject.NULL)
        .put("discapacidad_lenguaje", item.discapacidadLenguaje ?: JSONObject.NULL)
        .put("discapacidad_fisica", item.discapacidadFisica ?: JSONObject.NULL)
        .put("discapacidad_intelectual", item.discapacidadIntelectual ?: JSONObject.NULL)
        .put("discapacidad_psicosocial", item.discapacidadPsicosocial ?: JSONObject.NULL)
        .put("cuidados_paliativos", item.cuidadosPaliativos ?: JSONObject.NULL)
        .put("vih", item.vih ?: JSONObject.NULL)
        .put("evento_salud", item.eventoSalud ?: JSONObject.NULL)
        .put("caso_confirmado", item.casoConfirmado ?: JSONObject.NULL)
        .put("caso_sospechoso_uno", item.casoSospechosoUno ?: JSONObject.NULL)
        .put("caso_sospechoso_dos", item.casoSospechosoDos ?: JSONObject.NULL)
        .put("prestador_comunitario", item.prestadorComunitario ?: JSONObject.NULL)
        .put("partero_ancestral", item.parteroAncestral ?: JSONObject.NULL)
        .put("sabiduria_ancestral", item.sabiduriaAncestral ?: JSONObject.NULL)
        .put("comorbilidades_cie10_json", item.comorbilidadesCie10Json)
        .put("porcentaje_discapacidad", item.porcentajeDiscapacidad ?: JSONObject.NULL)
        .put("necesita_ayuda_tecnica", item.necesitaAyudaTecnica ?: JSONObject.NULL)
        .put("enfermedad_cronica_descompensada", item.enfermedadCronicaDescompensada ?: JSONObject.NULL)
        .put("riesgo_genetico", item.riesgoGenetico ?: JSONObject.NULL)
        .put("victima_violencia", item.victimaViolencia ?: JSONObject.NULL)
        .put("privado_libertad", item.privadoLibertad ?: JSONObject.NULL)
        .put("factores_riesgo_edad_json", item.factoresRiesgoEdadJson)
        .put("numero_historia_clinica", item.numeroHistoriaClinica).put("cedula", item.cedula)

    private fun embarazadaJson(item: EmbarazadaEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("apellidos_nombres", item.apellidosNombres)
        .put("fecha_ultima_menstruacion", fechaIso(item.fechaUltimaMenstruacion))
        .put("fecha_probable_parto", fechaIso(item.fechaProbableParto))
        .put("semanas_gestacion", item.semanasGestacion ?: JSONObject.NULL)
        .put("dosis_dt_primera", item.dosisDtPrimera).put("dosis_dt_segunda", item.dosisDtSegunda)
        .put("dosis_dt_refuerzo", item.dosisDtRefuerzo).put("gestas", item.gestas ?: JSONObject.NULL)
        .put("partos", item.partos ?: JSONObject.NULL).put("abortos", item.abortos ?: JSONObject.NULL)
        .put("cesareas", item.cesareas ?: JSONObject.NULL)
        .put("antecedentes_patologicos_obstetricos", item.antecedentesPatologicosObstetricos)
        .put("riesgo_obstetrico", item.riesgoObstetrico)
        .put("factores_obstetricos_json", item.factoresObstetricosJson)

    private fun mortalidadJson(item: MortalidadFamiliarEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("nombre", item.nombre).put("parentesco", item.parentesco)
        .put("edad_al_fallecer", item.edadAlFallecer ?: JSONObject.NULL).put("causa", item.causa)

    private fun calificacionJson(item: CalificacionRiesgoEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("fecha_calificacion", fechaIsoObligatoria(item.fechaCalificacion))
        .put("responsable", item.responsable).put("total", item.total).put("nivel", item.nivel)

    private fun valorJson(item: ValorRiesgoEntity, calificacionId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("calificacion_id", calificacionId)
        .put("componente", item.componente).put("valor", item.valor)

    private fun gestionJson(item: GestionRiesgoEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("fecha_analisis", fechaIsoObligatoria(item.fechaAnalisis))
        .put("numero", item.numero ?: JSONObject.NULL).put("compromiso_familia", item.compromisoFamilia)
        .put("compromiso_equipo_salud", item.compromisoEquipoSalud)
        .put("fecha_evaluacion", fechaIso(item.fechaEvaluacion)).put("cumplimiento", item.cumplimiento)
        .put("causas_incumplimiento_observaciones", item.causasIncumplimientoObservaciones)
        .put("responsable", item.responsable)

    private fun contaminacionJson(item: ContaminacionAmbientalEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("fecha_informe", fechaIsoObligatoria(item.fechaInforme))
        .put("tipo_contaminante_descripcion", item.tipoContaminanteDescripcion)
        .put("causante_contaminacion", item.causanteContaminacion)

    private fun lugarJson(item: LugarTratamientoEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("descripcion", item.descripcion)

    private fun fechaIso(valor: String): Any = convertirFecha(valor) ?: JSONObject.NULL

    private fun fechaIsoObligatoria(valor: String): String =
        convertirFecha(valor) ?: FORMATO_ISO.format(Date())

    private fun convertirFecha(valor: String): String? {
        if (valor.isBlank()) return null
        FORMATOS_ENTRADA.forEach { formato ->
            try {
                val fecha = formato.parse(valor.trim()) ?: return@forEach
                return FORMATO_ISO.format(fecha)
            } catch (_: ParseException) {
                Unit
            }
        }
        return null
    }

    private fun fechaHoraIso(millis: Long): String = FORMATO_HORA_ISO.format(Date(millis))

    private companion object {
        val bloqueoProceso = Mutex()
        const val TAMANO_LOTE_BAJAS = 50
        const val MAXIMO_BAJAS_POR_TABLA = 20_000
        const val PRESUPUESTO_BAJAS_MS = 4 * 60_000L
        val FORMATOS_ENTRADA = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false },
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        )
        val FORMATO_ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val FORMATO_HORA_ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
