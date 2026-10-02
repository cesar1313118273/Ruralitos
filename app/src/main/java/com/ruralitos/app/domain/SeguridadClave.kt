package com.ruralitos.app.domain

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class ClaveProtegida(val hash: String, val salt: String)

object SeguridadClave {
    private const val ITERACIONES = 120_000
    private const val LONGITUD_BITS = 256
    private const val ALGORITMO = "PBKDF2WithHmacSHA1"

    fun proteger(clave: String): ClaveProtegida {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return ClaveProtegida(
            hash = derivar(clave, salt).aHex(),
            salt = salt.aHex()
        )
    }

    fun verificar(clave: String, hashEsperado: String, saltHex: String): Boolean =
        runCatching {
            MessageDigest.isEqual(
                derivar(clave, saltHex.desdeHex()),
                hashEsperado.desdeHex()
            )
        }.getOrDefault(false)

    private fun derivar(clave: String, salt: ByteArray): ByteArray {
        val especificacion = PBEKeySpec(clave.toCharArray(), salt, ITERACIONES, LONGITUD_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).encoded
        } finally {
            especificacion.clearPassword()
        }
    }

    private fun ByteArray.aHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun String.desdeHex(): ByteArray {
        require(length % 2 == 0)
        return ByteArray(length / 2) { indice ->
            substring(indice * 2, indice * 2 + 2).toInt(16).toByte()
        }
    }
}
