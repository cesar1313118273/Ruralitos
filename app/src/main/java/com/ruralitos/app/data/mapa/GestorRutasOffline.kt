package com.ruralitos.app.data.mapa

import android.content.Context
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.config.ValhallaConfigFactory
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng

data class RutaCalculada(
    val puntos: List<LatLng>,
    val kilometros: Double,
    val minutos: Int,
    val instrucciones: List<String>,
    val modo: String,
    /** Kilómetros y minutos de cada tramo cuando la ruta pasa por varias paradas. */
    val tramos: List<Pair<Double, Int>> = emptyList()
)

/** Red de OSM procesada en el dispositivo: nunca envía las ubicaciones del paciente. */
object GestorRutasOffline {
    private const val ARCHIVO = "ecuador_valhalla_tiles.tar"

    /** Tiempo máximo que se espera una respuesta del motor antes de usar la línea recta. */
    private const val LIMITE_MS = 30_000L

    private val preparacion = Mutex()
    private var motor: Valhalla? = null
    private val hiloMotor = Executors.newSingleThreadExecutor { Thread(it, "valhalla").apply { isDaemon = true } }
        .asCoroutineDispatcher()
    private val ocupado = AtomicBoolean(false)
    private val inicioCalculo = AtomicLong(0L)

    /** Un cálculo normal dura décimas de segundo; pasado esto se considera atascado. */
    private const val ATASCADO_MS = 5_000L

    private val modosValidos = setOf("auto", "pedestrian", "bicycle", "motor_scooter")

    private fun ubicaciones(puntos: List<LatLng>) = JSONArray().also { lista ->
        puntos.forEach { lista.put(JSONObject().put("lat", it.latitude).put("lon", it.longitude)) }
    }

    /** Copia la red vial a la memoria interna la primera vez (puede tardar; no cuenta para el límite de tiempo). */
    private suspend fun archivoDeRed(app: Context): File = preparacion.withLock {
        withContext(Dispatchers.IO) { prepararDatos(app) }
    }

    /**
     * El motor nativo no se puede interrumpir: entre dos puntos sin un camino real entre sí recorre toda la red y puede
     * tardar minutos. Se le deja trabajar en su propio hilo; si pasa del límite, quien llamó recibe un error y mientras
     * el cálculo atascado siga ocupando el motor, las demás peticiones fallan enseguida en vez de acumularse.
     */
    private suspend fun <T> enMotor(app: Context, archivo: File, trabajo: (Valhalla) -> T): T {
        // Un cálculo corto de otra pantalla se espera; uno atascado no.
        while (!ocupado.compareAndSet(false, true)) {
            val enCurso = (System.nanoTime() - inicioCalculo.get()) / 1_000_000
            check(enCurso < ATASCADO_MS) { "El motor de rutas sigue ocupado con un cálculo anterior" }
            delay(50)
        }
        inicioCalculo.set(System.nanoTime())
        val tarea = CoroutineScope(hiloMotor).async {
            try {
                val valhalla = motor ?: run {
                    val configuracion = ValhallaConfigFactory.usingTileExtract(archivo.absolutePath)
                    Valhalla(app, configuracion).also { motor = it }
                }
                trabajo(valhalla)
            } finally {
                ocupado.set(false)
            }
        }
        return withTimeoutOrNull(LIMITE_MS) { tarea.await() }
            ?: throw IllegalStateException("La ruta tardó demasiado en calcularse; se muestra la línea recta")
    }

    suspend fun calcular(
        context: Context,
        inicio: LatLng,
        destino: LatLng,
        modo: String = "auto"
    ): Result<RutaCalculada> = calcularVarios(context, listOf(inicio, destino), modo)

    /** Ruta que pasa por todas las paradas en el orden dado (mínimo dos). */
    suspend fun calcularVarios(
        context: Context,
        paradas: List<LatLng>,
        modo: String = "auto"
    ): Result<RutaCalculada> = runCatching {
        require(modo in modosValidos)
        require(paradas.size >= 2) { "Hacen falta al menos dos puntos" }
        val app = context.applicationContext
        val archivo = archivoDeRed(app)
        enMotor(app, archivo) { valhalla ->
            val solicitud = JSONObject()
                .put("locations", ubicaciones(paradas))
                .put("costing", modo)
                .put("language", "es-ES")
                .put("units", "kilometers")
            val viaje = JSONObject(valhalla.routeRaw(solicitud.toString())).getJSONObject("trip")
            val tramos = viaje.getJSONArray("legs")
            val puntos = buildList {
                repeat(tramos.length()) { indice ->
                    addAll(decodificarPolilinea6(tramos.getJSONObject(indice).getString("shape")))
                }
            }
            check(puntos.size >= 2) { "El motor no devolvió un recorrido" }
            val resumen = viaje.getJSONObject("summary")
            val instrucciones = buildList {
                repeat(tramos.length()) { tramo ->
                    val maniobras = tramos.getJSONObject(tramo).optJSONArray("maneuvers")
                    if (maniobras != null) repeat(maniobras.length()) { indice ->
                        maniobras.getJSONObject(indice).optString("instruction")
                            .takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            }
            val porTramo = buildList {
                repeat(tramos.length()) { i ->
                    val r = tramos.getJSONObject(i).optJSONObject("summary")
                    add(
                        (r?.optDouble("length", 0.0) ?: 0.0) to
                            kotlin.math.ceil((r?.optDouble("time", 0.0) ?: 0.0) / 60.0).toInt()
                    )
                }
            }
            RutaCalculada(
                puntos = puntos,
                kilometros = resumen.getDouble("length"),
                minutos = kotlin.math.ceil(resumen.getDouble("time") / 60.0).toInt(),
                instrucciones = instrucciones,
                modo = modo,
                tramos = porTramo
            )
        }
    }

    /**
     * Tiempo en segundos de ir de cada punto a cada otro por la red vial. Si no hay camino entre dos puntos queda en
     * infinito.
     */
    suspend fun matriz(context: Context, puntos: List<LatLng>, modo: String = "auto"): Result<Array<DoubleArray>> =
        runCatching {
            require(modo in modosValidos)
            require(puntos.size >= 2) { "Hacen falta al menos dos puntos" }
            val app = context.applicationContext
            val archivo = archivoDeRed(app)
            enMotor(app, archivo) { valhalla ->
                val lista = ubicaciones(puntos)
                val solicitud = JSONObject().put("sources", lista).put("targets", lista)
                    .put("costing", modo).put("units", "kilometers")
                val filas = JSONObject(valhalla.matrixRaw(solicitud.toString())).getJSONArray("sources_to_targets")
                Array(puntos.size) { i ->
                    val fila = filas.getJSONArray(i)
                    DoubleArray(puntos.size) { j ->
                        if (i == j) 0.0
                        else fila.optJSONObject(j)?.takeIf { !it.isNull("time") }?.getDouble("time")
                            ?: Double.POSITIVE_INFINITY
                    }
                }
            }
        }

    private fun prepararDatos(context: Context): File {
        val destino = File(context.filesDir, ARCHIVO)
        val esperada = ArchivosMapa.longitudAsset(context, ARCHIVO)
        if (destino.isFile && destino.length() == esperada) return destino
        ArchivosMapa.verificarEspacio(context, esperada)
        val parcial = File(context.filesDir, "$ARCHIVO.parcial")
        parcial.delete()
        try {
            context.assets.open(ARCHIVO).use { entrada ->
                parcial.outputStream().buffered().use { salida ->
                    entrada.copyTo(salida, bufferSize = 256 * 1024)
                }
            }
            check(parcial.length() == esperada) { "La red vial quedó incompleta" }
            if (destino.exists()) check(destino.delete())
            check(parcial.renameTo(destino)) { "No se pudo activar la red vial" }
        } finally {
            parcial.delete()
        }
        return destino
    }

    /** Valhalla codifica latitud/longitud a seis decimales. */
    internal fun decodificarPolilinea6(valor: String): List<LatLng> {
        var indice = 0
        var latitud = 0L
        var longitud = 0L
        fun siguiente(): Long {
            var resultado = 0L
            var desplazamiento = 0
            while (true) {
                require(indice < valor.length) { "Geometría de ruta incompleta" }
                val codigo = valor[indice++].code - 63
                resultado = resultado or ((codigo and 0x1F).toLong() shl desplazamiento)
                if (codigo < 0x20) break
                desplazamiento += 5
                require(desplazamiento <= 60) { "Geometría de ruta inválida" }
            }
            return if ((resultado and 1L) != 0L) (resultado shr 1).inv() else resultado shr 1
        }
        return buildList {
            while (indice < valor.length) {
                latitud += siguiente()
                longitud += siguiente()
                add(LatLng(latitud / 1_000_000.0, longitud / 1_000_000.0))
            }
        }
    }
}
