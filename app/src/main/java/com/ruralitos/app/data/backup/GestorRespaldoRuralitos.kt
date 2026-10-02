package com.ruralitos.app.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ruralitos.app.BuildConfig
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.UsuarioEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ResultadoRespaldo(
    val fichasIncluidas: Int,
    val adjuntosIncluidos: Int,
    val adjuntosOmitidos: Int
)

data class ResultadoRestauracion(
    val fichasImportadas: Int,
    val fichasOmitidas: Int,
    val adjuntosRestaurados: Int
)

private data class ArchivoPortable(
    val tipo: String,
    val registroId: Long,
    val uri: String,
    val ruta: String
)

object GestorRespaldoRuralitos {
    private const val FORMATO = 2
    private const val LIMITE_EXTRAIDO = 1024L * 1024L * 1024L
    private const val MAXIMO_ENTRADAS = 20_000
    private val tablas = listOf(
        "fichas_familiares", "miembros_familia", "embarazadas",
        "mortalidad_familiar", "calificaciones_riesgo", "valores_riesgo",
        "gestion_riesgo", "contaminacion_ambiental", "lugares_tratamiento",
        "adjuntos_ficha", "historial_fichas"
    )
    private val camposNoPortables = mapOf(
        "fichas_familiares" to setOf(
            "firmaUri", "creadoPorUsuarioId", "actualizadoPorUsuarioId",
            "completadoPorUsuarioId", "syncId", "syncEstado", "syncVersion",
            "syncError", "organizacionId", "establecimientoRemotoId", "eaisId",
            "territorioId"
        ),
        "adjuntos_ficha" to setOf("uri", "syncId"),
        "historial_fichas" to setOf("usuarioId", "syncId")
    )

    suspend fun crear(
        context: Context,
        destino: Uri,
        clave: String,
        usuario: UsuarioEntity
    ): ResultadoRespaldo = withContext(Dispatchers.IO) {
        val database = RuralitosDatabase.obtenerBaseDatos(context)
        val sqlite = database.openHelper.writableDatabase
        sqlite.query("PRAGMA wal_checkpoint(FULL)").close()
        val fichas = contarFichasDelUsuario(sqlite, usuario.id)
        val archivos = recopilarArchivos(context, sqlite, usuario.id)
        var incluidos = 0
        var omitidos = 0
        val manifiesto = mutableListOf<JSONObject>()

        val salida = context.contentResolver.openOutputStream(destino, "w")
            ?: error("No se pudo crear el archivo de respaldo.")
        CifradoRespaldo.abrirSalida(salida, clave).use { cifrada ->
            ZipOutputStream(cifrada).use { zip ->
                zip.agregarTexto(
                    "metadata.json",
                    JSONObject()
                        .put("formato", FORMATO)
                        .put("creadoEn", System.currentTimeMillis())
                        .put("versionApp", BuildConfig.VERSION_NAME)
                        .put("fichas", fichas)
                        .put("portable", true)
                        .toString()
                )
                tablas.forEach { tabla ->
                    zip.putNextEntry(ZipEntry("data/$tabla.jsonl"))
                    sqlite.query(consultaExportacion(tabla), arrayOf(usuario.id)).use { cursor ->
                        while (cursor.moveToNext()) {
                            zip.write(
                                cursorAJson(cursor, camposNoPortables[tabla].orEmpty())
                                    .toString().toByteArray(Charsets.UTF_8)
                            )
                            zip.write('\n'.code)
                        }
                    }
                    zip.closeEntry()
                }
                archivos.forEach { archivo ->
                    val guardado = runCatching {
                        abrirUri(context, Uri.parse(archivo.uri))?.use { entrada ->
                            zip.putNextEntry(ZipEntry(archivo.ruta))
                            entrada.copyTo(zip)
                            zip.closeEntry()
                        } ?: error("Archivo no disponible")
                    }.isSuccess
                    if (guardado) {
                        manifiesto += JSONObject()
                            .put("tipo", archivo.tipo)
                            .put("registroId", archivo.registroId)
                            .put("ruta", archivo.ruta)
                        incluidos++
                    } else {
                        runCatching { zip.closeEntry() }
                        omitidos++
                    }
                }
                zip.agregarTexto(
                    "archivos.jsonl",
                    manifiesto.joinToString(
                        separator = "\n",
                        postfix = if (manifiesto.isEmpty()) "" else "\n"
                    )
                )
            }
        }
        ResultadoRespaldo(fichas, incluidos, omitidos)
    }

    suspend fun restaurar(
        context: Context,
        origen: Uri,
        clave: String,
        usuario: UsuarioEntity,
        sala: SalaEntity?
    ): ResultadoRestauracion = withContext(Dispatchers.IO) {
        require(usuario.supabaseId.isNotBlank()) { "Debes iniciar sesión con una cuenta en línea." }
        val organizacion = sala?.organizacionId?.ifBlank { null } ?: usuario.organizacionId
        require(organizacion.isNotBlank()) { "Selecciona una Sala antes de importar fichas." }
        val temporal = File(context.cacheDir, "importacion_${System.currentTimeMillis()}").apply {
            check(mkdirs()) { "No se pudo preparar la importación." }
        }
        val archivosCreados = mutableListOf<File>()
        try {
            val entrada = context.contentResolver.openInputStream(origen)
                ?: error("No se pudo abrir el respaldo.")
            entrada.use { abierta ->
                CifradoRespaldo.abrirEntrada(abierta, clave).use { descifrada ->
                    extraerSeguro(ZipInputStream(descifrada), temporal)
                    val buffer = ByteArray(1024)
                    while (descifrada.read(buffer) >= 0) {
                        // Fuerza la validación de la marca AES-GCM final.
                    }
                }
            }
            val metadata = File(temporal, "metadata.json")
                .takeIf(File::isFile)?.readText(Charsets.UTF_8)
                ?.let(::JSONObject)
                ?: error("El respaldo no contiene metadatos.")
            require(metadata.optInt("formato") == FORMATO && metadata.optBoolean("portable")) {
                "Este archivo corresponde a un respaldo antiguo de dispositivo. Crea un respaldo portable nuevo."
            }
            val archivos = leerManifiestoArchivos(temporal)
            val database = RuralitosDatabase.obtenerBaseDatos(context)
            var importadas = 0
            var omitidas = 0
            var adjuntos = 0

            try {
                database.withTransaction {
                    val sqlite = database.openHelper.writableDatabase
                    val mapaFichas = mutableMapOf<Long, Long>()
                    val mapaCalificaciones = mutableMapOf<Long, Long>()
                    val fichasInsertadas = mutableListOf<Pair<Long, String>>()

                    leerFilas(temporal, "fichas_familiares").forEach { fila ->
                        val idAnterior = fila.getLong("id")
                        val codigoDestino = sala?.codigoUo.orEmpty().ifBlank {
                            fila.optString("codigoUo")
                        }
                        if (existeFicha(
                                sqlite,
                                codigoDestino,
                                fila.optString("numeroFichaFamiliar"),
                                fila.optString("cedulaJefeHogar")
                            )
                        ) {
                            omitidas++
                            return@forEach
                        }
                        val valores = valoresPortables(fila, setOf("id")).apply {
                            put("codigoUo", codigoDestino)
                            sala?.let { aplicarSala(this, it) }
                            put("creadoPorUsuarioId", usuario.id)
                            put("actualizadoPorUsuarioId", usuario.id)
                            if (fila.optString("estado") == "COMPLETA") {
                                put("completadoPorUsuarioId", usuario.id)
                            } else putNull("completadoPorUsuarioId")
                            put("syncId", UUID.randomUUID().toString())
                            put("syncEstado", "PENDIENTE")
                            put("syncVersion", 0L)
                            put("syncError", "")
                            put("organizacionId", organizacion)
                            if (sala?.establecimientoId != null) {
                                put("establecimientoRemotoId", sala.establecimientoId)
                            } else putNull("establecimientoRemotoId")
                            put("eaisId", "")
                            put("territorioId", "")
                            putNull("firmaUri")
                        }
                        val nuevoId = insertar(sqlite, "fichas_familiares", valores)
                        mapaFichas[idAnterior] = nuevoId
                        fichasInsertadas += nuevoId to fila.optString("numeroFichaFamiliar")
                        restaurarFirma(
                            context, temporal, archivos, idAnterior,
                            nuevoId, sqlite, archivosCreados
                        )
                        importadas++
                    }

                    listOf(
                        "miembros_familia", "embarazadas", "mortalidad_familiar",
                        "gestion_riesgo", "contaminacion_ambiental", "lugares_tratamiento"
                    ).forEach { tabla ->
                        leerFilas(temporal, tabla).forEach { fila ->
                            val nuevaFicha = mapaFichas[fila.getLong("fichaId")]
                                ?: return@forEach
                            val valores = valoresPortables(
                                fila, setOf("id", "fichaId", "syncId")
                            )
                            valores.put("fichaId", nuevaFicha)
                            valores.put("syncId", UUID.randomUUID().toString())
                            insertar(sqlite, tabla, valores)
                        }
                    }
                    leerFilas(temporal, "calificaciones_riesgo").forEach { fila ->
                        val nuevaFicha = mapaFichas[fila.getLong("fichaId")] ?: return@forEach
                        val valores = valoresPortables(
                            fila, setOf("id", "fichaId", "syncId")
                        )
                        valores.put("fichaId", nuevaFicha)
                        valores.put("syncId", UUID.randomUUID().toString())
                        mapaCalificaciones[fila.getLong("id")] =
                            insertar(sqlite, "calificaciones_riesgo", valores)
                    }
                    leerFilas(temporal, "valores_riesgo").forEach { fila ->
                        val nuevaCalificacion =
                            mapaCalificaciones[fila.getLong("calificacionId")]
                                ?: return@forEach
                        val valores = valoresPortables(
                            fila, setOf("id", "calificacionId", "syncId")
                        )
                        valores.put("calificacionId", nuevaCalificacion)
                        valores.put("syncId", UUID.randomUUID().toString())
                        insertar(sqlite, "valores_riesgo", valores)
                    }
                    leerFilas(temporal, "historial_fichas").forEach { fila ->
                        val nuevaFicha = mapaFichas[fila.getLong("fichaId")] ?: return@forEach
                        val valores = valoresPortables(
                            fila, setOf("id", "fichaId", "usuarioId", "syncId")
                        )
                        valores.put("fichaId", nuevaFicha)
                        valores.put("usuarioId", usuario.id)
                        valores.put("syncId", UUID.randomUUID().toString())
                        insertar(sqlite, "historial_fichas", valores)
                    }
                    leerFilas(temporal, "adjuntos_ficha").forEach { fila ->
                        val nuevaFicha = mapaFichas[fila.getLong("fichaId")] ?: return@forEach
                        val origenAdjunto =
                            archivos["adjunto" to fila.getLong("id")] ?: return@forEach
                        val destino = copiarArchivoInterno(
                            context,
                            origenAdjunto,
                            "adjuntos_importados",
                            "adjunto_${UUID.randomUUID()}.${origenAdjunto.extension.ifBlank { "bin" }}"
                        )
                        archivosCreados += destino
                        val valores = valoresPortables(
                            fila, setOf("id", "fichaId", "uri", "syncId")
                        )
                        valores.put("fichaId", nuevaFicha)
                        valores.put("uri", Uri.fromFile(destino).toString())
                        valores.put("syncId", UUID.randomUUID().toString())
                        insertar(sqlite, "adjuntos_ficha", valores)
                        adjuntos++
                    }
                    fichasInsertadas.forEach { (fichaId, numero) ->
                        val historial = ContentValues().apply {
                            put("fichaId", fichaId)
                            put("numeroFicha", numero)
                            put("usuarioId", usuario.id)
                            put("usuarioNombre", usuario.nombres)
                            put("accion", "IMPORTACION_RESPALDO")
                            put("detalle", "Ficha importada desde un respaldo portable cifrado.")
                            put("creadoEn", System.currentTimeMillis())
                            put("syncId", UUID.randomUUID().toString())
                        }
                        insertar(sqlite, "historial_fichas", historial)
                    }
                }
            } catch (error: Exception) {
                archivosCreados.forEach { runCatching { it.delete() } }
                throw error
            }
            ResultadoRestauracion(importadas, omitidas, adjuntos)
        } finally {
            temporal.deleteRecursively()
        }
    }

    private fun recopilarArchivos(
        context: Context,
        sqlite: SupportSQLiteDatabase,
        usuarioId: Long
    ): List<ArchivoPortable> {
        val resultado = mutableListOf<ArchivoPortable>()
        sqlite.query(
            "SELECT id, firmaUri FROM fichas_familiares " +
                "WHERE firmaUri IS NOT NULL AND firmaUri <> '' " +
                "AND creadoPorUsuarioId = ?",
            arrayOf(usuarioId)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val uri = cursor.getString(1)
                val extension = extensionUri(context, Uri.parse(uri), "png")
                resultado += ArchivoPortable(
                    "firma", id, uri, "archivos/firmas/$id.$extension"
                )
            }
        }
        sqlite.query(
            "SELECT a.id, a.uri FROM adjuntos_ficha a " +
                "JOIN fichas_familiares f ON f.id = a.fichaId " +
                "WHERE a.uri <> '' AND f.creadoPorUsuarioId = ?",
            arrayOf(usuarioId)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val uri = cursor.getString(1)
                val extension = extensionUri(context, Uri.parse(uri), "bin")
                resultado += ArchivoPortable(
                    "adjunto", id, uri, "archivos/adjuntos/$id.$extension"
                )
            }
        }
        return resultado
    }

    private fun leerManifiestoArchivos(temporal: File): Map<Pair<String, Long>, File> {
        val archivo = File(temporal, "archivos.jsonl")
        if (!archivo.isFile) return emptyMap()
        return archivo.useLines(Charsets.UTF_8) { lineas ->
            lineas.filter(String::isNotBlank).mapNotNull { linea ->
                val item = JSONObject(linea)
                val destino = File(temporal, item.getString("ruta"))
                if (!destino.isFile) null
                else (item.getString("tipo") to item.getLong("registroId")) to destino
            }.toMap()
        }
    }

    private fun restaurarFirma(
        context: Context,
        temporal: File,
        archivos: Map<Pair<String, Long>, File>,
        idAnterior: Long,
        nuevoId: Long,
        sqlite: SupportSQLiteDatabase,
        creados: MutableList<File>
    ) {
        val origen = archivos["firma" to idAnterior] ?: return
        require(origen.canonicalPath.startsWith(temporal.canonicalPath + File.separator))
        val destino = copiarArchivoInterno(
            context,
            origen,
            "firmas_importadas",
            "firma_${UUID.randomUUID()}.${origen.extension.ifBlank { "png" }}"
        )
        creados += destino
        sqlite.execSQL(
            "UPDATE fichas_familiares SET firmaUri = ? WHERE id = ?",
            arrayOf(Uri.fromFile(destino).toString(), nuevoId)
        )
    }

    private fun aplicarSala(valores: ContentValues, sala: SalaEntity) {
        fun ponerSiExiste(nombre: String, valor: String) {
            if (valor.isNotBlank()) valores.put(nombre, valor)
        }
        ponerSiExiste("unidadOperativa", sala.nombreCentroSalud.ifBlank { sala.nombreSala })
        ponerSiExiste("institucionSistema", sala.institucionSistema)
        ponerSiExiste("provinciaCodigoLocalizacion", sala.provinciaCodigoLocalizacion)
        ponerSiExiste("provincia", sala.provincia)
        ponerSiExiste("cantonCodigoLocalizacion", sala.cantonCodigoLocalizacion)
        ponerSiExiste("canton", sala.canton)
        ponerSiExiste("parroquiaCodigoLocalizacion", sala.parroquiaCodigoLocalizacion)
        ponerSiExiste("parroquia", sala.parroquia)
        ponerSiExiste("sector", sala.sector)
        ponerSiExiste("areaNumero", sala.areaNumero)
    }

    private fun existeFicha(
        sqlite: SupportSQLiteDatabase,
        codigoUo: String,
        numero: String,
        cedula: String
    ): Boolean = sqlite.query(
        """
        SELECT 1 FROM fichas_familiares
         WHERE codigoUo = ? AND numeroFichaFamiliar = ? AND cedulaJefeHogar = ?
         LIMIT 1
        """.trimIndent(),
        arrayOf(codigoUo, numero, cedula)
    ).use(Cursor::moveToFirst)

    private fun leerFilas(raiz: File, tabla: String): List<JSONObject> {
        val archivo = File(raiz, "data/$tabla.jsonl")
        if (!archivo.isFile) return emptyList()
        return archivo.useLines(Charsets.UTF_8) { lineas ->
            lineas.filter(String::isNotBlank).map(::JSONObject).toList()
        }
    }

    private fun cursorAJson(cursor: Cursor, excluidos: Set<String>): JSONObject =
        JSONObject().also { json ->
            cursor.columnNames.forEachIndexed { indice, nombre ->
                if (nombre in excluidos) return@forEachIndexed
                when (cursor.getType(indice)) {
                    Cursor.FIELD_TYPE_NULL -> json.put(nombre, JSONObject.NULL)
                    Cursor.FIELD_TYPE_INTEGER -> json.put(nombre, cursor.getLong(indice))
                    Cursor.FIELD_TYPE_FLOAT -> json.put(nombre, cursor.getDouble(indice))
                    Cursor.FIELD_TYPE_STRING -> json.put(nombre, cursor.getString(indice))
                    Cursor.FIELD_TYPE_BLOB -> error("El respaldo no admite columnas binarias.")
                }
            }
        }

    private fun valoresPortables(
        json: JSONObject,
        excluidos: Set<String>
    ): ContentValues = ContentValues().also { valores ->
        val claves = json.keys()
        while (claves.hasNext()) {
            val clave = claves.next()
            if (clave in excluidos) continue
            when (val valor = json.opt(clave)) {
                null, JSONObject.NULL -> valores.putNull(clave)
                is Boolean -> valores.put(clave, valor)
                is Int -> valores.put(clave, valor)
                is Long -> valores.put(clave, valor)
                is Double -> valores.put(clave, valor)
                is Float -> valores.put(clave, valor)
                else -> valores.put(clave, valor.toString())
            }
        }
    }

    private fun insertar(
        sqlite: SupportSQLiteDatabase,
        tabla: String,
        valores: ContentValues
    ): Long {
        val id = sqlite.insert(tabla, SQLiteDatabase.CONFLICT_ABORT, valores)
        check(id > 0) { "No se pudo importar un registro de $tabla." }
        return id
    }

    private fun contarFichasDelUsuario(
        sqlite: SupportSQLiteDatabase,
        usuarioId: Long
    ): Int = sqlite.query(
        "SELECT COUNT(*) FROM fichas_familiares WHERE creadoPorUsuarioId = ?",
        arrayOf(usuarioId)
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) else 0
    }

    private fun consultaExportacion(tabla: String): String = when (tabla) {
        "fichas_familiares" ->
            "SELECT f.* FROM fichas_familiares f WHERE f.creadoPorUsuarioId = ? ORDER BY f.id"
        "valores_riesgo" ->
            "SELECT v.* FROM valores_riesgo v " +
                "JOIN calificaciones_riesgo c ON c.id = v.calificacionId " +
                "JOIN fichas_familiares f ON f.id = c.fichaId " +
                "WHERE f.creadoPorUsuarioId = ? ORDER BY v.id"
        else -> {
            require(tabla in tablas && tabla != "fichas_familiares")
            "SELECT t.* FROM $tabla t JOIN fichas_familiares f ON f.id = t.fichaId " +
                "WHERE f.creadoPorUsuarioId = ? ORDER BY t.id"
        }
    }

    private fun extensionUri(context: Context, uri: Uri, defecto: String): String =
        when (context.contentResolver.getType(uri)) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> uri.lastPathSegment?.substringAfterLast('.', "")
                ?.lowercase()?.takeIf { it.matches(Regex("[a-z0-9]{1,8}")) } ?: defecto
        }

    private fun abrirUri(context: Context, uri: Uri) =
        if (uri.scheme == "file") uri.path?.let(::FileInputStream)
        else context.contentResolver.openInputStream(uri)

    private fun copiarArchivoInterno(
        context: Context,
        origen: File,
        carpeta: String,
        nombre: String
    ): File {
        val directorio = File(context.filesDir, carpeta).apply { mkdirs() }
        return File(directorio, nombre).also { origen.copyTo(it, overwrite = false) }
    }

    private fun ZipOutputStream.agregarTexto(nombre: String, contenido: String) {
        putNextEntry(ZipEntry(nombre))
        write(contenido.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun extraerSeguro(zip: ZipInputStream, destino: File) {
        val raiz = destino.canonicalFile
        var total = 0L
        var entradas = 0
        while (true) {
            val entrada = zip.nextEntry ?: break
            entradas++
            require(entradas <= MAXIMO_ENTRADAS) {
                "El respaldo contiene demasiados archivos."
            }
            val archivo = File(raiz, entrada.name).canonicalFile
            require(archivo.path.startsWith(raiz.path + File.separator)) {
                "Ruta no permitida en el respaldo."
            }
            if (entrada.isDirectory) {
                archivo.mkdirs()
            } else {
                archivo.parentFile?.mkdirs()
                FileOutputStream(archivo).use { salida ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val leidos = zip.read(buffer)
                        if (leidos < 0) break
                        total += leidos
                        require(total <= LIMITE_EXTRAIDO) {
                            "El respaldo supera el tamaño permitido."
                        }
                        salida.write(buffer, 0, leidos)
                    }
                }
            }
            zip.closeEntry()
        }
    }
}
