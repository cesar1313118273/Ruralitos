package com.ruralitos.app.data.backup

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CifradoRespaldo {
    private const val FIRMA = "RURALITOS_BACKUP_1"
    private const val ITERACIONES = 210_000
    private const val TAMANO_BLOQUE = 64 * 1024
    private const val TAMANO_TAG = 16
    private const val ALGORITMO_SHA1 = 1
    private const val ALGORITMO_SHA256 = 2

    fun abrirSalida(destino: OutputStream, clave: String): OutputStream {
        require(clave.length >= 10) { "La contraseña del respaldo debe tener al menos 10 caracteres." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val prefijoIv = ByteArray(8).also { SecureRandom().nextBytes(it) }
        val algoritmo = if (runCatching {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            }.isSuccess
        ) ALGORITMO_SHA256 else ALGORITMO_SHA1
        val salida = DataOutputStream(destino)
        salida.writeUTF(FIRMA)
        salida.writeByte(algoritmo)
        salida.writeInt(ITERACIONES)
        salida.writeByte(salt.size)
        salida.write(salt)
        salida.writeByte(prefijoIv.size)
        salida.write(prefijoIv)
        return SalidaCifradaPorBloques(
            salida,
            derivarClave(clave, salt, ITERACIONES, algoritmo),
            prefijoIv
        )
    }

    fun abrirEntrada(origen: InputStream, clave: String): InputStream {
        val entrada = DataInputStream(origen)
        require(entrada.readUTF() == FIRMA) { "El archivo no es un respaldo de Ruralitos." }
        val algoritmo = entrada.readUnsignedByte()
        require(algoritmo == ALGORITMO_SHA1 || algoritmo == ALGORITMO_SHA256) { "Algoritmo no compatible." }
        val iteraciones = entrada.readInt()
        require(iteraciones in 100_000..500_000) { "Parámetros de respaldo no válidos." }
        val salt = ByteArray(entrada.readUnsignedByte().also { require(it in 16..32) })
        entrada.readFully(salt)
        val prefijoIv = ByteArray(entrada.readUnsignedByte().also { require(it == 8) })
        entrada.readFully(prefijoIv)
        return EntradaCifradaPorBloques(
            entrada,
            derivarClave(clave, salt, iteraciones, algoritmo),
            prefijoIv
        )
    }

    private fun derivarClave(clave: String, salt: ByteArray, iteraciones: Int, algoritmo: Int): SecretKeySpec {
        val nombre = if (algoritmo == ALGORITMO_SHA256) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"
        val especificacion = PBEKeySpec(clave.toCharArray(), salt, iteraciones, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance(nombre).generateSecret(especificacion).encoded
            SecretKeySpec(bytes, "AES")
        } finally {
            especificacion.clearPassword()
        }
    }

    private fun cifrar(datos: ByteArray, clave: SecretKeySpec, prefijoIv: ByteArray, contador: Int): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, clave, GCMParameterSpec(128, construirIv(prefijoIv, contador)))
        return cipher.doFinal(datos)
    }

    private fun descifrar(datos: ByteArray, clave: SecretKeySpec, prefijoIv: ByteArray, contador: Int): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, clave, GCMParameterSpec(128, construirIv(prefijoIv, contador)))
        return try {
            cipher.doFinal(datos)
        } catch (error: AEADBadTagException) {
            throw SecurityException("Contraseña incorrecta o respaldo alterado.", error)
        }
    }

    private fun construirIv(prefijo: ByteArray, contador: Int): ByteArray = ByteArray(12).also { iv ->
        prefijo.copyInto(iv)
        iv[8] = (contador ushr 24).toByte()
        iv[9] = (contador ushr 16).toByte()
        iv[10] = (contador ushr 8).toByte()
        iv[11] = contador.toByte()
    }

    private class SalidaCifradaPorBloques(
        private val salida: DataOutputStream,
        private val clave: SecretKeySpec,
        private val prefijoIv: ByteArray
    ) : OutputStream() {
        private val buffer = ByteArray(TAMANO_BLOQUE)
        private var usados = 0
        private var contador = 0
        private var cerrado = false

        override fun write(valor: Int) {
            buffer[usados++] = valor.toByte()
            if (usados == buffer.size) emitir(buffer)
        }

        override fun write(datos: ByteArray, offset: Int, longitud: Int) {
            require(offset >= 0 && longitud >= 0 && offset + longitud <= datos.size)
            var posicion = offset
            var restante = longitud
            while (restante > 0) {
                val cantidad = minOf(restante, buffer.size - usados)
                datos.copyInto(buffer, usados, posicion, posicion + cantidad)
                usados += cantidad
                posicion += cantidad
                restante -= cantidad
                if (usados == buffer.size) emitir(buffer)
            }
        }

        private fun emitir(datosCompletos: ByteArray) {
            val planos = if (datosCompletos === buffer) buffer.copyOf(usados) else datosCompletos
            val cifrados = cifrar(planos, clave, prefijoIv, contador++)
            salida.writeInt(planos.size)
            salida.writeInt(cifrados.size)
            salida.write(cifrados)
            usados = 0
        }

        override fun flush() = salida.flush()

        override fun close() {
            if (cerrado) return
            if (usados > 0) emitir(buffer.copyOf(usados))
            val marcaFinal = cifrar(ByteArray(0), clave, prefijoIv, contador)
            salida.writeInt(-1)
            salida.writeInt(marcaFinal.size)
            salida.write(marcaFinal)
            salida.flush()
            salida.close()
            cerrado = true
        }
    }

    private class EntradaCifradaPorBloques(
        private val entrada: DataInputStream,
        private val clave: SecretKeySpec,
        private val prefijoIv: ByteArray
    ) : InputStream() {
        private var bloque = ByteArray(0)
        private var posicion = 0
        private var contador = 0
        private var finalizado = false

        override fun read(): Int {
            if (!asegurarDatos()) return -1
            return bloque[posicion++].toInt() and 0xff
        }

        override fun read(destino: ByteArray, offset: Int, longitud: Int): Int {
            require(offset >= 0 && longitud >= 0 && offset + longitud <= destino.size)
            if (longitud == 0) return 0
            if (!asegurarDatos()) return -1
            val cantidad = minOf(longitud, bloque.size - posicion)
            bloque.copyInto(destino, offset, posicion, posicion + cantidad)
            posicion += cantidad
            return cantidad
        }

        private fun asegurarDatos(): Boolean {
            while (posicion >= bloque.size && !finalizado) cargarBloque()
            return posicion < bloque.size
        }

        private fun cargarBloque() {
            val longitudPlana = entrada.readInt()
            val longitudCifrada = entrada.readInt()
            require(longitudCifrada in TAMANO_TAG..(TAMANO_BLOQUE + TAMANO_TAG)) { "Bloque no válido." }
            val cifrados = ByteArray(longitudCifrada)
            entrada.readFully(cifrados)
            if (longitudPlana == -1) {
                require(descifrar(cifrados, clave, prefijoIv, contador).isEmpty()) { "Final no válido." }
                finalizado = true
                bloque = ByteArray(0)
                return
            }
            require(longitudPlana in 1..TAMANO_BLOQUE && longitudCifrada == longitudPlana + TAMANO_TAG) {
                "Longitud de bloque no válida."
            }
            bloque = descifrar(cifrados, clave, prefijoIv, contador++)
            require(bloque.size == longitudPlana) { "Contenido no válido." }
            posicion = 0
        }
    }
}
