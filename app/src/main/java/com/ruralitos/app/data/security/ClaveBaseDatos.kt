package com.ruralitos.app.data.security

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object ClaveBaseDatos {
    private const val ALIAS = "ruralitos_clave_maestra"
    private const val PREFERENCIAS = "seguridad_base_datos"
    private const val CLAVE_CIFRADA = "clave_cifrada"
    private const val IV = "iv"

    @Synchronized
    fun obtenerOCrear(context: Context): ByteArray {
        val preferencias = context.applicationContext.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
        val cifrada = preferencias.getString(CLAVE_CIFRADA, null)
        val iv = preferencias.getString(IV, null)
        if (cifrada != null && iv != null) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                obtenerClaveMaestra(crearSiFalta = false),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            return cipher.doFinal(Base64.decode(cifrada, Base64.NO_WRAP))
        }
        return ByteArray(32).also {
            SecureRandom().nextBytes(it)
            importar(context, it)
        }
    }

    @Synchronized
    @SuppressLint("ApplySharedPref")
    fun importar(context: Context, clave: ByteArray) {
        require(clave.size == 32) { "La clave de base de datos no es válida." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, obtenerClaveMaestra(crearSiFalta = true))
        val cifrada = cipher.doFinal(clave)
        context.applicationContext.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
            .edit()
            .putString(CLAVE_CIFRADA, Base64.encodeToString(cifrada, Base64.NO_WRAP))
            .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            // Debe persistirse antes de abrir la base; una escritura asíncrona podría dejarla sin clave tras un cierre inesperado.
            .commit()
    }

    private fun obtenerClaveMaestra(crearSiFalta: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existente = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existente != null) return existente
        check(crearSiFalta) {
            "La clave protegida del dispositivo no está disponible. Restaura un respaldo de Ruralitos."
        }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }
}
