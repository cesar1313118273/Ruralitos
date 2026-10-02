package com.ruralitos.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ruralitos.app.data.remote.SupabaseApi
import java.util.concurrent.TimeUnit

class SincronizacionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val usuarioId = SupabaseApi(applicationContext).sesionGuardada()?.usuarioId
        if (usuarioId == null) {
            return Result.success()
        }
        return runCatching {
            SincronizadorSupabase(applicationContext).ejecutar(
                soloSubidas = inputData.getBoolean(CLAVE_SOLO_SUBIDAS, false)
            )
        }.fold(
            onSuccess = { resultado ->
                EstadoSincronizacion.registrar(applicationContext, usuarioId, resultado.errores == 0)
                if (resultado.erroresReintentables > 0) Result.retry() else Result.success()
            },
            onFailure = { error ->
                EstadoSincronizacion.registrar(applicationContext, usuarioId, false)
                if (errorSincronizacionReintentable(error)) Result.retry() else Result.failure()
            }
        )
    }

    companion object {
        const val CLAVE_SOLO_SUBIDAS = "solo_subidas"
    }
}

object ProgramadorSincronizacion {
    private val restriccionesPeriodicas = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()
    private val restriccionesInmediatas = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun configurar(context: Context) {
        val periodica = PeriodicWorkRequestBuilder<SincronizacionWorker>(15, TimeUnit.MINUTES)
            .setConstraints(restriccionesPeriodicas)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TRABAJO_PERIODICO,
            ExistingPeriodicWorkPolicy.KEEP,
            periodica
        )
        ejecutarAhora(context)
    }

    fun ejecutarAhora(context: Context) {
        val inmediata = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            .setConstraints(restriccionesInmediatas)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TRABAJO_INMEDIATO,
            // No interrumpir una carga a medias: la descarga se ejecuta después.
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            inmediata
        )
    }

    fun ejecutarCambiosLocales(context: Context) {
        val inmediata = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            .setInputData(workDataOf(SincronizacionWorker.CLAVE_SOLO_SUBIDAS to true))
            .setConstraints(restriccionesInmediatas)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TRABAJO_INMEDIATO,
            ExistingWorkPolicy.KEEP,
            inmediata
        )
    }

    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_INMEDIATO)
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_PERIODICO)
    }

    private const val TRABAJO_PERIODICO = "ruralitos_sync_periodica"
    private const val TRABAJO_INMEDIATO = "ruralitos_sync_inmediata"
}
