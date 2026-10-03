package com.ruralitos.app.pruebas

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Servidor de mentira que se comporta como Supabase (PostgREST + Storage) en lo que usa la sincronización:
 * filtros `eq/gt/gte/lt/lte/is/in/or`, orden, paginación, upsert con `on_conflict`, `Prefer`, versión y
 * `updated_at` que sube solos en cada cambio, la restricción única de adjuntos (ficha, tipo) y el almacenamiento
 * de archivos. No simula permisos (RLS): eso se prueba cambiando las filas de membresía.
 *
 * Sirve para probar la sincronización de punta a punta en el emulador, incluido «lo que hace otra persona»:
 * la prueba modifica las filas del servidor directamente con [modificar].
 */
class ServidorSupabaseFalso : AutoCloseable {
    private val socket = ServerSocket(0)
    private val hilos = Executors.newCachedThreadPool()
    private val candado = Any()
    private val tablas = LinkedHashMap<String, MutableList<JSONObject>>()
    private val almacen = LinkedHashMap<String, ByteArray>()
    private var ultimoMicros = 0L

    val url: String get() = "http://localhost:${socket.localPort}"

    /** «METODO ruta?consulta» de cada petición recibida, ya decodificada. */
    val peticiones = CopyOnWriteArrayList<String>()

    init {
        hilos.execute {
            while (!socket.isClosed) {
                val cliente = runCatching { socket.accept() }.getOrNull() ?: break
                hilos.execute { runCatching { atender(cliente) } }
            }
        }
    }

    override fun close() {
        runCatching { socket.close() }
        hilos.shutdownNow()
    }

    // ---- acceso para las pruebas ---------------------------------------------------------

    fun filas(tabla: String): List<JSONObject> = synchronized(candado) {
        tablas[tabla].orEmpty().map { JSONObject(it.toString()) }
    }

    fun fila(tabla: String, id: String): JSONObject? = filas(tabla).firstOrNull { it.optString("id") == id }

    fun archivo(ruta: String): ByteArray? = synchronized(candado) { almacen[ruta] }

    fun rutasDeArchivos(): Set<String> = synchronized(candado) { almacen.keys.toSet() }

    /** Inserta una fila como lo haría el servidor (versión 1 y `updated_at` nuevos). */
    fun insertar(tabla: String, fila: JSONObject) = synchronized(candado) { insertarFila(tabla, fila) }

    /** Cambia una fila como lo haría OTRA persona: sube la versión y `updated_at`. */
    fun modificar(tabla: String, id: String, cambio: JSONObject.() -> Unit) = synchronized(candado) {
        val fila = tablas[tabla].orEmpty().first { it.optString("id") == id }
        actualizarFila(tabla, fila, JSONObject().apply(cambio))
    }

    fun sembrarSala(organizacionId: String, usuarioId: String) {
        insertar("organizaciones", JSONObject().put("id", organizacionId).put("nombre", "Sala de prueba")
            .put("codigo", "QA").put("establecimiento_id", JSONObject.NULL))
        insertar("miembros_organizacion", JSONObject().put("id", "m-$organizacionId-$usuarioId")
            .put("organizacion_id", organizacionId).put("usuario_id", usuarioId).put("rol", "MEDICO")
            .put("activo", true).put("establecimiento_id", JSONObject.NULL).put("creado_en", ahora()))
        insertar("accesos_sala", JSONObject().put("id", "a-$organizacionId-$usuarioId")
            .put("organizacion_id", organizacionId).put("usuario_id", usuarioId).put("permiso", "EDITOR")
            .put("alcance", "SALA").put("activo", true))
    }

    // ---- reglas del «servidor» -----------------------------------------------------------

    private val conVersion = setOf(
        "fichas_familiares", "miembros_familia", "embarazadas", "mortalidad_familiar", "calificaciones_riesgo",
        "valores_riesgo", "gestion_riesgo", "contaminacion_ambiental", "lugares_tratamiento", "adjuntos_ficha"
    )

    private fun ahora(): String {
        val micros = maxOf(System.currentTimeMillis() * 1000, ultimoMicros + 1)
        ultimoMicros = micros
        val instante = Instant.ofEpochSecond(micros / 1_000_000, (micros % 1_000_000) * 1000)
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSxxx"))
    }

    private fun insertarFila(tabla: String, original: JSONObject): JSONObject {
        val fila = JSONObject(original.toString())
        if (!fila.has("id")) fila.put("id", java.util.UUID.randomUUID().toString())
        if (tabla in conVersion) {
            if (!fila.has("version")) fila.put("version", 1)
            fila.put("updated_at", ahora())
            if (!fila.has("deleted_at")) fila.put("deleted_at", JSONObject.NULL)
        }
        if (tabla == "historial_fichas") fila.put("registrado_en", ahora())
        tablas.getOrPut(tabla) { mutableListOf() }.add(fila)
        return fila
    }

    private fun actualizarFila(tabla: String, fila: JSONObject, cambios: JSONObject): JSONObject {
        val versionAnterior = fila.optLong("version", 0)
        cambios.keys().forEach { clave -> fila.put(clave, cambios.get(clave)) }
        if (tabla in conVersion) {
            if (!cambios.has("version")) fila.put("version", versionAnterior + 1)
            fila.put("updated_at", ahora())
        }
        return fila
    }

    // ---- HTTP ----------------------------------------------------------------------------

    private class Peticion(
        val metodo: String, val ruta: String, val consulta: List<Pair<String, String>>,
        val cabeceras: Map<String, String>, val cuerpo: ByteArray
    )

    private fun atender(cliente: Socket) = cliente.use { s ->
        val entrada = BufferedInputStream(s.getInputStream())
        val peticion = leerPeticion(entrada) ?: return@use
        peticiones += "${peticion.metodo} ${peticion.ruta}?" + peticion.consulta.joinToString("&") { "${it.first}=${it.second}" }
        val (codigo, tipo, cuerpo) = try {
            synchronized(candado) { responder(peticion) }
        } catch (error: Exception) {
            Triple(500, "application/json", JSONObject().put("message", error.toString()).toString().toByteArray())
        }
        val razon = when (codigo) { 200 -> "OK"; 201 -> "Created"; 204 -> "No Content"; 404 -> "Not Found"; 409 -> "Conflict"; else -> "Error" }
        val salida = s.getOutputStream()
        salida.write(("HTTP/1.1 $codigo $razon\r\nContent-Type: $tipo\r\nContent-Length: ${cuerpo.size}\r\nConnection: close\r\n\r\n").toByteArray())
        salida.write(cuerpo)
        salida.flush()
    }

    private fun leerLinea(entrada: InputStream): String? {
        val b = ByteArrayOutputStream()
        while (true) {
            val c = entrada.read()
            if (c < 0) return if (b.size() == 0) null else b.toString("ISO-8859-1")
            if (c == '\n'.code) return b.toString("ISO-8859-1").trimEnd('\r')
            b.write(c)
        }
    }

    private fun leerPeticion(entrada: InputStream): Peticion? {
        val primera = leerLinea(entrada) ?: return null
        val partes = primera.split(' ')
        val destino = partes[1]
        val cabeceras = LinkedHashMap<String, String>()
        while (true) {
            val linea = leerLinea(entrada) ?: break
            if (linea.isEmpty()) break
            val i = linea.indexOf(':')
            if (i > 0) cabeceras[linea.substring(0, i).trim().lowercase()] = linea.substring(i + 1).trim()
        }
        val cuerpo = ByteArrayOutputStream()
        if (cabeceras["transfer-encoding"]?.contains("chunked", true) == true) {
            while (true) {
                val tam = leerLinea(entrada)?.substringBefore(';')?.trim()?.toInt(16) ?: break
                if (tam == 0) { leerLinea(entrada); break }
                val buffer = ByteArray(tam)
                var leidos = 0
                while (leidos < tam) {
                    val n = entrada.read(buffer, leidos, tam - leidos)
                    if (n < 0) break
                    leidos += n
                }
                cuerpo.write(buffer, 0, leidos)
                leerLinea(entrada)
            }
        } else {
            val largo = cabeceras["content-length"]?.toIntOrNull() ?: 0
            val buffer = ByteArray(largo)
            var leidos = 0
            while (leidos < largo) {
                val n = entrada.read(buffer, leidos, largo - leidos)
                if (n < 0) break
                leidos += n
            }
            cuerpo.write(buffer, 0, leidos)
        }
        val ruta = destino.substringBefore('?')
        val consulta = destino.substringAfter('?', "").split('&').filter { it.isNotEmpty() }.map {
            val k = URLDecoder.decode(it.substringBefore('='), "UTF-8")
            val v = URLDecoder.decode(it.substringAfter('=', ""), "UTF-8")
            k to v
        }
        return Peticion(partes[0], URLDecoder.decode(ruta, "UTF-8"), consulta, cabeceras, cuerpo.toByteArray())
    }

    private fun json(codigo: Int, valor: Any) = Triple(codigo, "application/json", valor.toString().toByteArray())

    private fun responder(p: Peticion): Triple<Int, String, ByteArray> {
        val ruta = p.ruta
        if (ruta.startsWith("/storage/v1/object/fichas-adjuntos/")) return responderAlmacen(p, ruta.removePrefix("/storage/v1/object/fichas-adjuntos/"))
        if (!ruta.startsWith("/rest/v1/")) return json(404, JSONObject().put("message", "no existe $ruta"))
        val tabla = ruta.removePrefix("/rest/v1/")
        val preferencia = p.cabeceras["prefer"].orEmpty()
        val representacion = preferencia.contains("return=representation")
        val filas = tablas.getOrPut(tabla) { mutableListOf() }
        val filtros = p.consulta.filter { it.first !in setOf("select", "order", "limit", "offset", "on_conflict") }

        return when (p.metodo) {
            "GET" -> {
                var resultado = filas.filter { f -> filtros.all { cumple(f, it.first, it.second) } }
                p.consulta.firstOrNull { it.first == "order" }?.second?.let { orden ->
                    val criterios = orden.split(',').map { it.substringBefore('.') to it.endsWith(".desc") }
                    resultado = resultado.sortedWith { a, b ->
                        for ((col, desc) in criterios) {
                            val c = comparar(a.opt(col), b.opt(col))
                            if (c != 0) return@sortedWith if (desc) -c else c
                        }
                        0
                    }
                }
                val desplazamiento = p.consulta.firstOrNull { it.first == "offset" }?.second?.toInt() ?: 0
                val limite = p.consulta.firstOrNull { it.first == "limit" }?.second?.toInt() ?: Int.MAX_VALUE
                json(200, JSONArray(resultado.drop(desplazamiento).take(limite).map { JSONObject(it.toString()) }))
            }
            "POST" -> {
                val cuerpo = String(p.cuerpo)
                val items = if (cuerpo.trimStart().startsWith("[")) {
                    JSONArray(cuerpo).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
                } else listOf(JSONObject(cuerpo))
                val conflicto = p.consulta.firstOrNull { it.first == "on_conflict" }?.second?.split(',') ?: listOf("id")
                val ignorar = preferencia.contains("ignore-duplicates")
                val devueltas = mutableListOf<JSONObject>()
                for (item in items) {
                    val existente = filas.firstOrNull { f -> conflicto.all { c -> f.has(c) && item.has(c) && comparar(f.get(c), item.get(c)) == 0 } }
                    if (existente == null) {
                        if (tabla == "adjuntos_ficha") {
                            // restricción única (ficha_id, tipo), incluso con filas dadas de baja
                            val choque = filas.any { it.optString("ficha_id") == item.optString("ficha_id") && it.optString("tipo") == item.optString("tipo") }
                            if (choque) return json(409, JSONObject().put("code", "23505")
                                .put("message", "duplicate key value violates unique constraint \"adjuntos_tipo_unico\""))
                        }
                        devueltas += insertarFila(tabla, item)
                    } else if (!ignorar) {
                        devueltas += actualizarFila(tabla, existente, item)
                    }
                }
                if (representacion) json(201, JSONArray(devueltas.map { JSONObject(it.toString()) })) else Triple(201, "application/json", ByteArray(0))
            }
            "PATCH" -> {
                val cambios = JSONObject(String(p.cuerpo))
                val afectadas = filas.filter { f -> filtros.all { cumple(f, it.first, it.second) } }.map { actualizarFila(tabla, it, JSONObject(cambios.toString())) }
                if (representacion) json(200, JSONArray(afectadas.map { JSONObject(it.toString()) })) else Triple(204, "application/json", ByteArray(0))
            }
            else -> json(405, JSONObject().put("message", "método no soportado"))
        }
    }

    private fun responderAlmacen(p: Peticion, ruta: String): Triple<Int, String, ByteArray> = when (p.metodo) {
        "POST", "PUT" -> { almacen[ruta] = p.cuerpo; json(200, JSONObject().put("Key", "fichas-adjuntos/$ruta")) }
        "GET" -> almacen[ruta]?.let { Triple(200, "application/octet-stream", it) }
            ?: json(404, JSONObject().put("error", "not_found").put("message", "Object not found"))
        "DELETE" -> if (almacen.remove(ruta) != null) json(200, JSONObject().put("message", "Successfully deleted"))
            else json(404, JSONObject().put("error", "not_found").put("message", "Object not found"))
        else -> json(405, JSONObject().put("message", "método no soportado"))
    }

    // ---- filtros de PostgREST ------------------------------------------------------------

    private fun micros(texto: String): Long? = runCatching {
        val i = OffsetDateTime.parse(texto).toInstant()
        i.epochSecond * 1_000_000 + i.nano / 1000
    }.getOrNull()

    private fun comparar(a: Any?, b: Any?): Int {
        val x = if (a == null || a == JSONObject.NULL) null else a.toString()
        val y = if (b == null || b == JSONObject.NULL) null else b.toString()
        if (x == null || y == null) return if (x == y) 0 else if (x == null) -1 else 1
        val fx = micros(x); val fy = micros(y)
        if (fx != null && fy != null && x.contains('T') && y.contains('T')) return fx.compareTo(fy)
        val nx = x.toDoubleOrNull(); val ny = y.toDoubleOrNull()
        if (nx != null && ny != null) return nx.compareTo(ny)
        return x.compareTo(y)
    }

    private fun cumple(fila: JSONObject, clave: String, valor: String): Boolean =
        if (clave == "or") condiciones(valor.removePrefix("(").removeSuffix(")")).any { cumpleCondicion(fila, it) }
        else cumpleOperador(fila, clave, valor)

    /** Divide por comas de primer nivel, respetando paréntesis. */
    private fun condiciones(texto: String): List<String> {
        val partes = mutableListOf<String>(); var nivel = 0; val actual = StringBuilder()
        for (c in texto) {
            if (c == '(') nivel++
            if (c == ')') nivel--
            if (c == ',' && nivel == 0) { partes += actual.toString(); actual.clear() } else actual.append(c)
        }
        if (actual.isNotEmpty()) partes += actual.toString()
        return partes
    }

    private fun cumpleCondicion(fila: JSONObject, condicion: String): Boolean {
        if (condicion.startsWith("and(")) return condiciones(condicion.removePrefix("and(").removeSuffix(")")).all { cumpleCondicion(fila, it) }
        val columna = condicion.substringBefore('.')
        return cumpleOperador(fila, columna, condicion.substringAfter('.'))
    }

    private fun cumpleOperador(fila: JSONObject, columna: String, expresion: String): Boolean {
        val valorFila = if (fila.has(columna)) fila.get(columna) else JSONObject.NULL
        return when {
            expresion == "is.null" -> valorFila == JSONObject.NULL
            expresion == "not.is.null" -> valorFila != JSONObject.NULL
            expresion.startsWith("eq.") -> valorFila != JSONObject.NULL && comparar(valorFila, expresion.removePrefix("eq.")) == 0
            expresion.startsWith("neq.") -> comparar(valorFila, expresion.removePrefix("neq.")) != 0
            expresion.startsWith("gte.") -> valorFila != JSONObject.NULL && comparar(valorFila, expresion.removePrefix("gte.")) >= 0
            expresion.startsWith("gt.") -> valorFila != JSONObject.NULL && comparar(valorFila, expresion.removePrefix("gt.")) > 0
            expresion.startsWith("lte.") -> valorFila != JSONObject.NULL && comparar(valorFila, expresion.removePrefix("lte.")) <= 0
            expresion.startsWith("lt.") -> valorFila != JSONObject.NULL && comparar(valorFila, expresion.removePrefix("lt.")) < 0
            expresion.startsWith("in.(") -> expresion.removePrefix("in.(").removeSuffix(")").split(',').any { comparar(valorFila, it) == 0 }
            else -> error("operador no soportado: $columna $expresion")
        }
    }
}
