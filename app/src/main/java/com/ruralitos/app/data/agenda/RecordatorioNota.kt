package com.ruralitos.app.data.agenda

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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ruralitos.app.MainActivity
import com.ruralitos.app.R
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.NotaDiariaEntity
import com.ruralitos.app.data.session.SesionLocal
import java.util.concurrent.TimeUnit

/** Avisos locales, sin internet y sin información clínica visible en la notificación. */
object RecordatorioNota {
    private const val DIA = 24 * 60 * 60 * 1000L
    private const val MEDIA_JORNADA = DIA / 2
    private const val CANAL = "notas_diarias_ruralitos"
    internal const val ETIQUETA_AVISO = "ruralitos_nota"
    private const val CLAVE_ID = "nota_id"
    private const val CLAVE_TURNO = "turno"

    fun actualizar(context: Context, nota: NotaDiariaEntity) {
        val manager = WorkManager.getInstance(context.applicationContext)
        for (turno in 1..6) {
            val nombre = "nota_${nota.id}_$turno"
            manager.cancelUniqueWork(nombre)
            val avisoEn = nota.creadaEn + turno * MEDIA_JORNADA
            if (nota.realizada || avisoEn <= System.currentTimeMillis()) continue
            val solicitud = OneTimeWorkRequestBuilder<RecordatorioNotaWorker>()
                .setInitialDelay(avisoEn - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putLong(CLAVE_ID, nota.id).putInt(CLAVE_TURNO, turno).build())
                .build()
            manager.enqueueUniqueWork(nombre, ExistingWorkPolicy.REPLACE, solicitud)
        }
    }

    fun cancelar(context: Context, id: Long) {
        val manager = WorkManager.getInstance(context.applicationContext)
        for (turno in 1..6) manager.cancelUniqueWork("nota_${id}_$turno")
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(ETIQUETA_AVISO, id.toInt())
    }

    suspend fun recuperarPendientes(context: Context) {
        val desde = System.currentTimeMillis() - 3 * DIA
        val usuarioId = SesionLocal(context).usuarioId() ?: return
        RuralitosDatabase.obtenerBaseDatos(context).notaDiariaDao().pendientesRecientes(desde)
            .filter { it.usuarioId == usuarioId }
            .forEach { actualizar(context, it) }
    }

    internal fun id(data: Data) = data.getLong(CLAVE_ID, 0L)
    internal fun turno(data: Data) = data.getInt(CLAVE_TURNO, 0)
    internal const val ID_CANAL = CANAL
    internal const val DURACION_MAXIMA = 3 * DIA
}

class RecordatorioNotaWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = RecordatorioNota.id(inputData)
        val turno = RecordatorioNota.turno(inputData)
        if (id <= 0L || turno !in 1..6) return Result.success()
        val nota = RuralitosDatabase.obtenerBaseDatos(applicationContext).notaDiariaDao().buscar(id)
            ?: return Result.success()
        if (nota.usuarioId == 0L || nota.usuarioId != SesionLocal(applicationContext).usuarioId() ||
            nota.eliminadoEn != null) return Result.success()
        if (nota.realizada || System.currentTimeMillis() > nota.creadaEn + RecordatorioNota.DURACION_MAXIMA) {
            return Result.success()
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(
                RecordatorioNota.ID_CANAL, "Notas diarias pendientes", NotificationManager.IMPORTANCE_DEFAULT
            ))
        }
        val abrir = PendingIntent.getActivity(
            applicationContext, id.toInt(), Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(RecordatorioNota.ETIQUETA_AVISO, id.toInt(),
            NotificationCompat.Builder(applicationContext, RecordatorioNota.ID_CANAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nota diaria pendiente")
            .setContentText("Revisa una nota pendiente en Ruralitos.")
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build())
        return Result.success()
    }
}
