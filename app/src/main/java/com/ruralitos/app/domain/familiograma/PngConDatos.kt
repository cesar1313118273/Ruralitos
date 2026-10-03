package com.ruralitos.app.domain.familiograma

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Guarda un texto (el JSON del familiograma) dentro de un PNG, en un bloque estándar `iTXt`.
 * Los visores, Excel y el PDF ignoran ese bloque y muestran el dibujo normal; la app lo lee
 * para poder volver a editar el dibujo.
 */
object PngConDatos {
    const val CLAVE_FAMILIOGRAMA = "ruralitos.familiograma"

    private val FIRMA = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    private fun esPng(bytes: ByteArray): Boolean =
        bytes.size > 8 && FIRMA.indices.all { bytes[it] == FIRMA[it] }

    private fun entero(bytes: ByteArray, desde: Int): Int =
        ((bytes[desde].toInt() and 0xFF) shl 24) or ((bytes[desde + 1].toInt() and 0xFF) shl 16) or
            ((bytes[desde + 2].toInt() and 0xFF) shl 8) or (bytes[desde + 3].toInt() and 0xFF)

    private fun escribirEntero(salida: ByteArrayOutputStream, valor: Int) {
        salida.write(valor ushr 24); salida.write(valor ushr 16); salida.write(valor ushr 8); salida.write(valor)
    }

    private fun comprimir(datos: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(datos)
        deflater.finish()
        val salida = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) salida.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return salida.toByteArray()
    }

    private fun descomprimir(datos: ByteArray): ByteArray? {
        val inflater = Inflater()
        inflater.setInput(datos)
        val salida = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        return try {
            while (!inflater.finished()) {
                val n = inflater.inflate(buffer)
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) return null
                salida.write(buffer, 0, n)
            }
            salida.toByteArray()
        } catch (_: Exception) {
            null
        } finally {
            inflater.end()
        }
    }

    private fun bloque(tipo: String, datos: ByteArray): ByteArray {
        val salida = ByteArrayOutputStream()
        escribirEntero(salida, datos.size)
        val cuerpo = tipo.toByteArray(Charsets.ISO_8859_1) + datos
        salida.write(cuerpo)
        val crc = CRC32().apply { update(cuerpo) }
        escribirEntero(salida, crc.value.toInt())
        return salida.toByteArray()
    }

    /** Devuelve el PNG con el [texto] incrustado (reemplaza uno anterior con la misma [clave]). */
    fun insertar(png: ByteArray, texto: String, clave: String = CLAVE_FAMILIOGRAMA): ByteArray {
        require(esPng(png)) { "No es un PNG válido." }
        val datos = ByteArrayOutputStream().apply {
            write(clave.toByteArray(Charsets.ISO_8859_1)); write(0)
            write(1); write(0) // comprimido con zlib
            write(0) // sin idioma
            write(0) // sin clave traducida
            write(comprimir(texto.toByteArray(Charsets.UTF_8)))
        }.toByteArray()
        val nuevo = bloque("iTXt", datos)

        val salida = ByteArrayOutputStream(png.size + nuevo.size)
        salida.write(png, 0, 8)
        var posicion = 8
        var insertado = false
        while (posicion + 12 <= png.size) {
            val largo = entero(png, posicion)
            val tipo = String(png, posicion + 4, 4, Charsets.ISO_8859_1)
            val fin = posicion + 12 + largo
            if (fin > png.size) break
            val esNuestro = tipo == "iTXt" && claveDe(png, posicion + 8, largo) == clave
            if (!esNuestro) salida.write(png, posicion, fin - posicion)
            if (tipo == "IHDR" && !insertado) {
                salida.write(nuevo)
                insertado = true
            }
            posicion = fin
        }
        return salida.toByteArray()
    }

    private fun claveDe(png: ByteArray, inicio: Int, largo: Int): String? {
        var fin = inicio
        while (fin < inicio + largo && png[fin].toInt() != 0) fin++
        return if (fin >= inicio + largo) null else String(png, inicio, fin - inicio, Charsets.ISO_8859_1)
    }

    /** Lee el texto incrustado con [clave], o `null` si el PNG no lo trae. */
    fun leer(png: ByteArray, clave: String = CLAVE_FAMILIOGRAMA): String? {
        if (!esPng(png)) return null
        var posicion = 8
        while (posicion + 12 <= png.size) {
            val largo = entero(png, posicion)
            val tipo = String(png, posicion + 4, 4, Charsets.ISO_8859_1)
            val inicio = posicion + 8
            if (largo < 0 || inicio + largo > png.size) return null
            if (tipo == "iTXt" && claveDe(png, inicio, largo) == clave) {
                var i = inicio
                while (png[i].toInt() != 0) i++
                i++ // fin de la clave
                val comprimido = png[i].toInt() == 1
                i += 2 // bandera y método
                while (png[i].toInt() != 0) i++ // idioma
                i++
                while (png[i].toInt() != 0) i++ // clave traducida
                i++
                val texto = png.copyOfRange(i, inicio + largo)
                val plano = if (comprimido) descomprimir(texto) ?: return null else texto
                return String(plano, Charsets.UTF_8)
            }
            if (tipo == "IEND") return null
            posicion = inicio + largo + 4
        }
        return null
    }
}
