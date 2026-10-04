package com.ruralitos.app.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ruralitos.app.MainActivity
import com.ruralitos.app.R
import com.ruralitos.app.data.remote.InfoFichaCompartida

/** Lo que cambió desde la última sincronización en las fichas compartidas. */
data class NovedadesCompartidas(
    /** Fichas nuevas que me compartieron, por nombre de quien las creó. */
    val recibidasPorAutor: Map<String, Int>,
    /** Fichas mías que otra persona modificó. */
    val editadas: Int
) {
    val hayNovedades: Boolean get() = recibidasPorAutor.isNotEmpty() || editadas > 0
}

/** Estado de una ficha de este teléfono antes de poner las etiquetas nuevas. */
data class EtiquetaPrevia(val miPermiso: String, val editadaPorOtroEn: Long)

object AvisosCompartidas {
    private const val CANAL = "compartidas_ruralitos"
    private const val ETIQUETA = "ruralitos_compartidas"
    private const val ID_AVISO = 77

    /**
     * Compara lo que dice el servidor con lo que había en el teléfono. Solo cuentan las fichas que ya están aquí:
     * así una ficha que todavía no bajó no se avisa dos veces.
     */
    fun detectar(previas: Map<String, EtiquetaPrevia>, filas: List<InfoFichaCompartida>): NovedadesCompartidas {
        val recibidas = linkedMapOf<String, Int>()
        var editadas = 0
        filas.forEach { fila ->
            val previa = previas[fila.fichaId] ?: return@forEach
            if (fila.recibida) {
                if (previa.miPermiso.isBlank()) {
                    val autor = fila.autorNombre.ifBlank { "Otra persona" }
                    recibidas[autor] = (recibidas[autor] ?: 0) + 1
                }
            } else if (fila.editadaEn > 0L && fila.editadaEn > previa.editadaPorOtroEn) {
                editadas++
            }
        }
        return NovedadesCompartidas(recibidas, editadas)
    }

    /** «Ana te compartió 3 fichas. 2 fichas tuyas fueron modificadas por otras personas.» */
    fun texto(novedades: NovedadesCompartidas): String? {
        if (!novedades.hayNovedades) return null
        val partes = mutableListOf<String>()
        val total = novedades.recibidasPorAutor.values.sum()
        if (total > 0) {
            val fichas = if (total == 1) "1 ficha" else "$total fichas"
            val autores = novedades.recibidasPorAutor.keys.toList()
            partes += when {
                autores.size == 1 -> "${autores[0]} te compartió $fichas."
                autores.size == 2 -> "Te compartieron $fichas (${autores[0]} y ${autores[1]})."
                else -> "Te compartieron $fichas (${autores[0]}, ${autores[1]} y otras personas)."
            }
        }
        if (novedades.editadas > 0) {
            partes += if (novedades.editadas == 1) "1 ficha tuya fue modificada por otra persona."
            else "${novedades.editadas} fichas tuyas fueron modificadas por otras personas."
        }
        return partes.joinToString(" ")
    }

    /** Aviso local; en la pantalla bloqueada solo dice que hay novedades, sin nombres. */
    fun mostrar(context: Context, novedades: NovedadesCompartidas) {
        val detalle = texto(novedades) ?: return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CANAL, "Fichas compartidas", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val abrir = PendingIntent.getActivity(
            context, ID_AVISO, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val publico = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Fichas compartidas")
            .setContentText("Hay novedades en Ruralitos.")
            .build()
        manager.notify(
            ETIQUETA, ID_AVISO,
            NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Fichas compartidas")
                .setContentText(detalle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(detalle))
                .setContentIntent(abrir)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publico)
                .build()
        )
    }
}
