package com.ruralitos.app.data.security

import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileInputStream

object CifradorBaseDatos {
    private const val CABECERA_SQLITE = "SQLite format 3\u0000"
    @Volatile private var libreriaCargada = false

    @Synchronized
    fun cargarLibreria() {
        if (!libreriaCargada) {
            System.loadLibrary("sqlcipher")
            libreriaCargada = true
        }
    }

    @Synchronized
    fun preparar(context: Context, nombreBase: String, clave: ByteArray) {
        cargarLibreria()
        val archivo = context.getDatabasePath(nombreBase)
        if (archivo.isFile && esBasePlana(archivo)) migrarBasePlana(archivo, clave)
    }

    fun esBasePlana(archivo: File): Boolean {
        if (!archivo.isFile || archivo.length() < 16) return false
        val cabecera = ByteArray(16)
        FileInputStream(archivo).use { if (it.read(cabecera) != cabecera.size) return false }
        return String(cabecera, Charsets.US_ASCII) == CABECERA_SQLITE
    }

    fun validarCifrada(archivo: File, clave: ByteArray): Int {
        cargarLibreria()
        val base = SQLiteDatabase.openDatabase(
            archivo.path,
            clave,
            null,
            SQLiteDatabase.OPEN_READONLY,
            null
        )
        return try {
            base.rawQuery("SELECT count(*) FROM sqlite_master", emptyArray()).use { cursor ->
                check(cursor.moveToFirst())
            }
            base.version
        } finally {
            base.close()
        }
    }

    private fun migrarBasePlana(original: File, clave: ByteArray) {
        val cifrada = File(original.parentFile, original.name + ".cifrada")
        val respaldoPlano = File(original.parentFile, original.name + ".plana_temporal")
        limpiarTemporales(cifrada)
        if (respaldoPlano.exists()) check(respaldoPlano.delete()) {
            "No se pudo limpiar el respaldo temporal anterior."
        }

        // Algunos fabricantes no permiten que ATTACH cree directamente un archivo cifrado.
        // Se crea primero con la API de SQLCipher y después se adjunta para exportar los datos.
        val baseCifradaVacia = SQLiteDatabase.openOrCreateDatabase(cifrada, clave, null, null)
        try {
            // SQLCipher abre de forma diferida; una escritura obliga a crear la cabecera física.
            baseCifradaVacia.execSQL("CREATE TABLE `__ruralitos_preparacion` (`id` INTEGER)")
            baseCifradaVacia.execSQL("DROP TABLE `__ruralitos_preparacion`")
        } finally {
            baseCifradaVacia.close()
        }
        check(cifrada.isFile && cifrada.length() > 0) { "No se pudo crear la base cifrada temporal." }

        val basePlana = SQLiteDatabase.openDatabase(
            original.path,
            "",
            null,
            SQLiteDatabase.OPEN_READWRITE,
            null
        )
        var adjunta = false
        val version = try {
            runCatching { basePlana.rawQuery("PRAGMA wal_checkpoint(FULL)", emptyArray()).use { it.moveToFirst() } }
            basePlana.execSQL(
                "ATTACH DATABASE ? AS encrypted KEY ?",
                arrayOf<Any>(cifrada.path, clave)
            )
            adjunta = true
            basePlana.rawQuery("SELECT sqlcipher_export('encrypted')", emptyArray()).use { it.moveToFirst() }
            val actual = basePlana.version
            basePlana.execSQL("PRAGMA encrypted.user_version = $actual")
            basePlana.execSQL("DETACH DATABASE encrypted")
            adjunta = false
            actual
        } finally {
            if (adjunta) runCatching { basePlana.execSQL("DETACH DATABASE encrypted") }
            basePlana.close()
        }
        File(original.path + "-wal").delete()
        File(original.path + "-shm").delete()
        check(validarCifrada(cifrada, clave) == version) { "No se pudo verificar la base cifrada." }

        check(original.renameTo(respaldoPlano)) { "No se pudo proteger la base existente." }
        try {
            check(cifrada.renameTo(original)) { "No se pudo activar la base cifrada." }
            check(validarCifrada(original, clave) == version)
            respaldoPlano.delete()
        } catch (error: Exception) {
            original.delete()
            respaldoPlano.renameTo(original)
            throw error
        } finally {
            limpiarTemporales(cifrada)
        }
    }

    private fun limpiarTemporales(archivo: File) {
        listOf(
            archivo,
            File(archivo.path + "-journal"),
            File(archivo.path + "-wal"),
            File(archivo.path + "-shm")
        ).forEach { temporal ->
            if (temporal.exists()) check(temporal.delete()) {
                "No se pudo limpiar ${temporal.name}."
            }
        }
    }
}
