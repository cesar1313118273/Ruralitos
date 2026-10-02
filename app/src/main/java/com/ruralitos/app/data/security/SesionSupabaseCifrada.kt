package com.ruralitos.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SesionSupabase(
    val accessToken: String,
    val refreshToken: String,
    val expiraEnSegundos: Long,
    val usuarioId: String,
    val correo: String
)

class SesionSupabaseCifrada(context: Context) {
    private val preferencias = context.applicationContext.getSharedPreferences(
        "sesion_supabase_ruralitos",
        Context.MODE_PRIVATE
    )

    fun guardar(sesion: SesionSupabase) {
        val json = JSONObject()
            .put("access_token", sesion.accessToken)
            .put("refresh_token", sesion.refreshToken)
            .put("expira_en", sesion.expiraEnSegundos)
            .put("usuario_id", sesion.usuarioId)
            .put("correo", sesion.correo)
            .toString()
        preferencias.edit().putString(CLAVE_SESION, cifrar(json)).apply()
    }

    fun obtener(): SesionSupabase? = runCatching {
        val protegido = preferencias.getString(CLAVE_SESION, null) ?: return null
        val json = JSONObject(descifrar(protegido))
        SesionSupabase(
            accessToken = json.getString("access_token"),
            refreshToken = json.optString("refresh_token"),
            expiraEnSegundos = json.optLong("expira_en"),
            usuarioId = json.optString("usuario_id"),
            correo = json.optString("correo")
        )
    }.getOrNull()

    fun guardarOrganizacion(id: String?) {
        preferencias.edit().apply {
            if (id.isNullOrBlank()) remove(CLAVE_ORGANIZACION)
            else putString(CLAVE_ORGANIZACION, id)
        }.apply()
    }

    fun organizacionId(): String? = preferencias.getString(CLAVE_ORGANIZACION, null)

    fun marcarRecuperacion(pendiente: Boolean) {
        preferencias.edit().putBoolean(CLAVE_RECUPERACION, pendiente).apply()
    }

    fun recuperacionPendiente(): Boolean = preferencias.getBoolean(CLAVE_RECUPERACION, false)

    fun limpiar() {
        preferencias.edit().clear().apply()
    }

    private fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, obtenerOCrearClave())
        val contenido = cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + contenido, Base64.NO_WRAP)
    }

    private fun descifrar(texto: String): String {
        val combinado = Base64.decode(texto, Base64.NO_WRAP)
        require(combinado.size > TAMANO_IV)
        val iv = combinado.copyOfRange(0, TAMANO_IV)
        val contenido = combinado.copyOfRange(TAMANO_IV, combinado.size)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, obtenerOCrearClave(), GCMParameterSpec(128, iv))
        return cipher.doFinal(contenido).toString(Charsets.UTF_8)
    }

    private fun obtenerOCrearClave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val ALIAS = "ruralitos_supabase_session_v1"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val TAMANO_IV = 12
        const val CLAVE_SESION = "sesion_cifrada"
        const val CLAVE_ORGANIZACION = "organizacion_id"
        const val CLAVE_RECUPERACION = "recuperacion_pendiente"
    }
}
