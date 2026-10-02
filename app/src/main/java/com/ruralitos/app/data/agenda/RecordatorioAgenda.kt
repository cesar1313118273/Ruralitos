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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ruralitos.app.MainActivity
import com.ruralitos.app.R
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.session.SesionLocal
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Avisos locales, sin información clínica visible en la pantalla bloqueada. */
object RecordatorioAgenda {
    private const val CLAVE_ID = "actividad_id"
    private const val CLAVE_FASE = "fase"
    private const val CANAL = "agenda_ruralitos"
    internal const val ETIQUETA_AVISO = "ruralitos_agenda"
    private const val DIA = 24 * 60 * 60 * 1000L
    private const val MINUTO = 60 * 1000L
    internal const val CONFIRMAR = "confirmar"
    internal const val PREVIO = "previo"
    internal const val DIA_CITA = "dia"
    internal const val ATRASADA = "atrasada"
    internal const val EXACTO = "exacto"
    private val fases = listOf(CONFIRMAR, PREVIO, DIA_CITA, ATRASADA, EXACTO)

    fun actualizar(context: Context, actividad: ActividadAgendaEntity) {
        if (actividad.estado != "PENDIENTE") {
            cancelar(context, actividad.id)
            return
        }
        val manager = WorkManager.getInstance(context.applicationContext)
        val ahora = System.currentTimeMillis()
        if (actividad.origen == "SEGUIMIENTO" && !actividad.fechaEditada) {
            listOf(PREVIO, DIA_CITA, ATRASADA, EXACTO).forEach {
                manager.cancelUniqueWork(nombre(actividad.id, it))
            }
            periodico(context, actividad.id, CONFIRMAR, DIA)
            return
        }
        manager.cancelUniqueWork(nombre(actividad.id, CONFIRMAR))
        val previo = actividad.fechaHora - 3 * DIA
        if (previo > ahora) unico(context, actividad.id, PREVIO, previo)
        else manager.cancelUniqueWork(nombre(actividad.id, PREVIO))
        if (actividad.fechaHora > ahora) {
            val calendarioDia = Calendar.getInstance().apply {
                timeInMillis = actividad.fechaHora
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val inicioDia = calendarioDia.timeInMillis
            val ocho = calendarioDia.apply {
                set(Calendar.HOUR_OF_DAY, 8)
            }.timeInMillis
            val avisoDia = minOf(ocho, maxOf(inicioDia, actividad.fechaHora - 60 * MINUTO))
            unico(context, actividad.id, DIA_CITA, maxOf(ahora + MINUTO, avisoDia))
            if (actividad.recordar) unico(context, actividad.id, EXACTO, actividad.fechaHora)
            else manager.cancelUniqueWork(nombre(actividad.id, EXACTO))
            periodico(context, actividad.id, ATRASADA,
                maxOf(MINUTO, actividad.fechaHora + DIA - ahora))
        } else {
            manager.cancelUniqueWork(nombre(actividad.id, DIA_CITA))
            manager.cancelUniqueWork(nombre(actividad.id, EXACTO))
            periodico(context, actividad.id, ATRASADA,
                maxOf(MINUTO, actividad.fechaHora + DIA - ahora))
        }
    }

    private fun unico(context: Context, id: Long, fase: String, cuando: Long) {
        val solicitud = OneTimeWorkRequestBuilder<RecordatorioAgendaWorker>()
            .setInitialDelay(maxOf(0L, cuando - System.currentTimeMillis()), TimeUnit.MILLISECONDS)
            .setInputData(datos(id, fase)).build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(nombre(id, fase), ExistingWorkPolicy.REPLACE, solicitud)
    }

    private fun periodico(context: Context, id: Long, fase: String, demora: Long) {
        val solicitud = PeriodicWorkRequestBuilder<RecordatorioAgendaWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(demora, TimeUnit.MILLISECONDS)
            .setInputData(datos(id, fase)).build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(nombre(id, fase), ExistingPeriodicWorkPolicy.KEEP, solicitud)
    }

    fun cancelar(context: Context, id: Long) {
        val manager = WorkManager.getInstance(context.applicationContext)
        fases.forEach { manager.cancelUniqueWork(nombre(id, it)) }
        val avisos = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fases.indices.forEach { avisos.cancel(ETIQUETA_AVISO, idAviso(id, it)) }
        avisos.cancel(id.toInt())
    }

    private fun nombre(id: Long, fase: String) = "agenda_" + id + "_" + fase
    private fun datos(id: Long, fase: String) = Data.Builder().putLong(CLAVE_ID, id)
        .putString(CLAVE_FASE, fase).build()
    internal fun idDesde(data: Data): Long = data.getLong(CLAVE_ID, 0L)
    internal fun faseDesde(data: Data): String = data.getString(CLAVE_FASE).orEmpty()
    internal fun idAviso(id: Long, indice: Int) = (id * 10 + indice).toInt()
    internal fun indice(fase: String) = fases.indexOf(fase).coerceAtLeast(0)
    internal const val ID_CANAL = CANAL
}

class RecordatorioAgendaWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = RecordatorioAgenda.idDesde(inputData)
        val fase = RecordatorioAgenda.faseDesde(inputData)
        if (id <= 0L) return Result.success()
        val actividad = RuralitosDatabase.obtenerBaseDatos(applicationContext).agendaDao().buscar(id)
            ?: return Result.success()
        if (actividad.usuarioId != SesionLocal(applicationContext).usuarioId() ||
            actividad.eliminadoEn != null) return Result.success()
        if (actividad.estado != "PENDIENTE") return Result.success()
        val ahora = System.currentTimeMillis()
        val sinConfirmar = actividad.origen == "SEGUIMIENTO" && !actividad.fechaEditada
        val vigente = when (fase) {
            RecordatorioAgenda.CONFIRMAR -> sinConfirmar
            RecordatorioAgenda.PREVIO -> !sinConfirmar && actividad.fechaHora > ahora &&
                ahora >= actividad.fechaHora - 3 * 24 * 60 * 60 * 1000L
            RecordatorioAgenda.DIA_CITA -> !sinConfirmar &&
                diaLocal(actividad.fechaHora) == diaLocal(ahora)
            RecordatorioAgenda.ATRASADA -> !sinConfirmar && actividad.fechaHora < ahora
            RecordatorioAgenda.EXACTO -> !sinConfirmar && actividad.recordar
            else -> false
        }
        if (!vigente) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(
                RecordatorioAgenda.ID_CANAL, "Avisos de agenda", NotificationManager.IMPORTANCE_DEFAULT
            ))
        }
        val abrir = PendingIntent.getActivity(applicationContext, id.toInt(),
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val titulo = when (fase) {
            RecordatorioAgenda.CONFIRMAR -> "Agenda por confirmar"
            RecordatorioAgenda.ATRASADA -> "Actividad atrasada"
            RecordatorioAgenda.PREVIO -> "Actividad en tres días"
            else -> "Actividad programada para hoy"
        }
        manager.notify(RecordatorioAgenda.ETIQUETA_AVISO,
            RecordatorioAgenda.idAviso(id, RecordatorioAgenda.indice(fase)),
            NotificationCompat.Builder(applicationContext, RecordatorioAgenda.ID_CANAL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText("Revisa tu agenda en Ruralitos.")
                .setContentIntent(abrir)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build())
        return Result.success()
    }

    private fun diaLocal(momento: Long): Int = Calendar.getInstance().apply {
        timeInMillis = momento
    }.let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
}
