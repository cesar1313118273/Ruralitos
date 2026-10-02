package com.ruralitos.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FichaPdfExporter {
    suspend fun exportar(context: Context, fichaId: Long, destino: Uri) = withContext(Dispatchers.IO) {
        val data = FichaExcelExporter.cargarDatos(context, fichaId)
        val documento = PdfDocument()
        try {
            val escritor = EscritorPdf(documento, data.ficha.numeroFichaFamiliar)
            val ficha = data.ficha

            escritor.tituloPrincipal("FICHA FAMILIAR")
            escritor.texto("Institución", ficha.institucionSistema)
            escritor.texto("Unidad operativa", "${ficha.unidadOperativa} (${ficha.codigoUo})")
            escritor.texto("Número de ficha", ficha.numeroFichaFamiliar)
            escritor.texto("Estado", ficha.estado)
            escritor.seccion("Datos de la familia")
            escritor.texto("Jefe/a de familia", ficha.nombreApellidoJefeFamilia)
            escritor.texto("Identificación", ficha.cedulaJefeHogar)
            escritor.texto("Teléfono", ficha.numeroTelefono)
            escritor.texto("Fecha de llenado", ficha.fechaLlenado)
            escritor.texto("Carpeta", ficha.numeroCarpeta)
            escritor.seccion("Ubicación")
            escritor.texto("Provincia / cantón / parroquia", "${ficha.provincia} / ${ficha.canton} / ${ficha.parroquia}")
            escritor.texto("Sector / barrio", "${ficha.sector} / ${ficha.barrio.ifBlank { ficha.comunidad }}")
            escritor.texto("Dirección", ficha.direccionHabitualFamilia)
            escritor.texto("Casa", ficha.numeroCasa)
            escritor.texto("Georreferencia", listOfNotNull(ficha.latitud, ficha.longitud, ficha.altitud).joinToString(" / "))
            escritor.seccion("Responsable")
            escritor.texto("Nombre", ficha.responsableNombre)
            escritor.texto("Código", ficha.responsableCodigo)
            ficha.firmaUri?.let { abrirBitmap(context, Uri.parse(it)) }?.let { escritor.imagen(it, "Firma") }

            escritor.nuevaPagina()
            escritor.seccion("Miembros familiares")
            escritor.tabla(
                listOf("Nombres", "Parentesco", "Nacimiento", "Sexo", "Identificación"),
                data.miembros.map {
                    listOf(it.apellidosNombres, it.parentesco, it.fechaNacimiento, it.sexo, it.cedula)
                },
                floatArrayOf(0.31f, 0.17f, 0.18f, 0.10f, 0.24f)
            )

            escritor.seccion("Embarazadas")
            if (data.embarazadas.isEmpty()) escritor.parrafo("Sin registros.")
            else escritor.tabla(
                listOf("Nombres", "FUM", "FPP", "Semanas", "Antecedentes"),
                data.embarazadas.map {
                    listOf(it.apellidosNombres, it.fechaUltimaMenstruacion, it.fechaProbableParto, it.semanasGestacion?.toString().orEmpty(), it.antecedentesPatologicosObstetricos)
                },
                floatArrayOf(0.27f, 0.14f, 0.14f, 0.12f, 0.33f)
            )

            escritor.seccion("Mortalidad familiar")
            if (data.mortalidad.isEmpty()) escritor.parrafo("Sin registros.")
            else escritor.tabla(
                listOf("Nombre", "Parentesco", "Edad", "Causa"),
                data.mortalidad.map { listOf(it.nombre, it.parentesco, it.edadAlFallecer?.toString().orEmpty(), it.causa) },
                floatArrayOf(0.30f, 0.20f, 0.12f, 0.38f)
            )

            escritor.nuevaPagina()
            escritor.seccion("Calificación de riesgos")
            if (data.calificaciones.isEmpty()) escritor.parrafo("Sin evaluaciones.")
            else escritor.tabla(
                listOf("Fecha", "Total", "Nivel", "Responsable"),
                data.calificaciones.map {
                    listOf(it.calificacion.fechaCalificacion, it.calificacion.total.toString(), it.calificacion.nivel.replace('_', ' '), it.calificacion.responsable)
                },
                floatArrayOf(0.18f, 0.12f, 0.25f, 0.45f)
            )

            escritor.seccion("Evolución y compromisos")
            if (data.gestion.isEmpty()) escritor.parrafo("Sin registros.")
            else escritor.tabla(
                listOf("Fecha", "Compromiso familiar", "Equipo de salud", "Cumplimiento"),
                data.gestion.map { listOf(it.fechaAnalisis, it.compromisoFamilia, it.compromisoEquipoSalud, it.cumplimiento.replace('_', ' ')) },
                floatArrayOf(0.15f, 0.34f, 0.34f, 0.17f)
            )

            escritor.nuevaPagina()
            escritor.seccion("Entorno y contaminación")
            val familiograma = data.adjuntos.firstOrNull { it.tipo == "FAMILIOGRAMA" }
                ?.let { abrirBitmap(context, Uri.parse(it.uri)) }
            val croquis = data.adjuntos.firstOrNull { it.tipo == "CROQUIS" }
                ?.let { abrirBitmap(context, Uri.parse(it.uri)) }
            if (familiograma != null) escritor.imagen(familiograma, "Familiograma", altoMaximo = 245f)
            else escritor.parrafo("Familiograma: sin imagen.")
            if (croquis != null) escritor.imagen(croquis, "Croquis", altoMaximo = 245f)
            else escritor.parrafo("Croquis: sin imagen.")

            escritor.seccion("Informes de contaminación")
            if (data.contaminacion.isEmpty()) escritor.parrafo("Sin registros.")
            else data.contaminacion.forEach {
                escritor.parrafo("${it.fechaInforme}: ${it.tipoContaminanteDescripcion}. Causante: ${it.causanteContaminacion}")
            }
            escritor.seccion("Lugares o personas para tratamiento")
            if (data.lugaresTratamiento.isEmpty()) escritor.parrafo("Sin registros.")
            else data.lugaresTratamiento.forEach { escritor.parrafo("- ${it.descripcion}") }

            escritor.finalizar()
            context.contentResolver.openOutputStream(destino, "w")?.use { documento.writeTo(it) }
                ?: error("No se pudo abrir el destino PDF.")
        } finally {
            documento.close()
        }
    }

    private fun abrirBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
        val bytes = if (uri.scheme == "file") uri.path?.let { File(it).readBytes() }
        else context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }.getOrNull()

    private class EscritorPdf(private val documento: PdfDocument, private val numeroFicha: String) {
        private val ancho = 595
        private val alto = 842
        private val margen = 36f
        private val anchoUtil = ancho - margen * 2
        private var numeroPagina = 0
        private var pagina: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = margen
        private val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(31, 41, 51) }

        init { nuevaPagina() }

        fun nuevaPagina() {
            cerrarPagina()
            numeroPagina++
            pagina = documento.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numeroPagina).create())
            canvas = pagina!!.canvas
            canvas!!.drawColor(Color.WHITE)
            y = margen
            estilo(10f, negrita = true, color = Color.rgb(30, 132, 73))
            canvas!!.drawText("RURALITOS", margen, y, pintura)
            estilo(8f, color = Color.DKGRAY)
            canvas!!.drawText("Ficha $numeroFicha", ancho - margen - pintura.measureText("Ficha $numeroFicha"), y, pintura)
            y += 22f
            canvas!!.drawLine(margen, y, ancho - margen, y, Paint(pintura).apply { strokeWidth = 1f })
            y += 18f
        }

        fun tituloPrincipal(texto: String) {
            asegurar(42f)
            estilo(20f, negrita = true, color = Color.rgb(30, 132, 73))
            canvas!!.drawText(texto, margen, y, pintura)
            y += 31f
        }

        fun seccion(texto: String) {
            asegurar(36f)
            y += 8f
            estilo(13f, negrita = true, color = Color.rgb(30, 132, 73))
            canvas!!.drawText(texto, margen, y, pintura)
            y += 19f
        }

        fun texto(etiqueta: String, valor: String) {
            val contenido = "$etiqueta: ${valor.ifBlank { "Sin registrar" }}"
            parrafo(contenido)
        }

        fun parrafo(texto: String) {
            estilo(9.5f)
            val lineas = envolver(texto, anchoUtil, pintura)
            asegurar(lineas.size * 13f + 5f)
            lineas.forEach { linea ->
                canvas!!.drawText(linea, margen, y, pintura)
                y += 13f
            }
            y += 3f
        }

        fun tabla(encabezados: List<String>, filas: List<List<String>>, proporciones: FloatArray) {
            require(encabezados.size == proporciones.size)
            val anchos = proporciones.map { it * anchoUtil }
            dibujarFila(encabezados, anchos, encabezado = true)
            filas.forEachIndexed { indice, fila ->
                dibujarFila(fila, anchos, encabezado = false, fondoAlterno = indice % 2 == 1)
            }
            y += 5f
        }

        private fun dibujarFila(
            celdas: List<String>,
            anchos: List<Float>,
            encabezado: Boolean,
            fondoAlterno: Boolean = false
        ) {
            estilo(if (encabezado) 8.2f else 7.8f, negrita = encabezado, color = if (encabezado) Color.WHITE else Color.rgb(31, 41, 51))
            val lineas = celdas.mapIndexed { index, valor -> envolver(valor.ifBlank { "-" }, anchos[index] - 8f, pintura) }
            val altoFila = maxOf(20f, (lineas.maxOfOrNull { it.size } ?: 1) * 10.5f + 8f)
            asegurar(altoFila)
            var x = margen
            celdas.indices.forEach { index ->
                val fondo = when {
                    encabezado -> Color.rgb(30, 132, 73)
                    fondoAlterno -> Color.rgb(245, 249, 247)
                    else -> Color.WHITE
                }
                pintura.style = Paint.Style.FILL
                pintura.color = fondo
                canvas!!.drawRect(x, y, x + anchos[index], y + altoFila, pintura)
                pintura.style = Paint.Style.STROKE
                pintura.strokeWidth = 0.6f
                pintura.color = Color.LTGRAY
                canvas!!.drawRect(x, y, x + anchos[index], y + altoFila, pintura)
                pintura.style = Paint.Style.FILL
                pintura.color = if (encabezado) Color.WHITE else Color.rgb(31, 41, 51)
                lineas[index].forEachIndexed { linea, texto ->
                    canvas!!.drawText(texto, x + 4f, y + 11f + linea * 10.5f, pintura)
                }
                x += anchos[index]
            }
            y += altoFila
        }

        fun imagen(bitmap: Bitmap, titulo: String, altoMaximo: Float = 130f) {
            val escala = minOf(anchoUtil / bitmap.width, altoMaximo / bitmap.height)
            val w = bitmap.width * escala
            val h = bitmap.height * escala
            asegurar(h + 43f)
            seccion(titulo)
            val izquierda = margen + (anchoUtil - w) / 2f
            canvas!!.drawBitmap(bitmap, null, RectF(izquierda, y, izquierda + w, y + h), Paint(Paint.ANTI_ALIAS_FLAG))
            y += h + 8f
        }

        private fun asegurar(espacio: Float) {
            if (y + espacio > alto - 48f) nuevaPagina()
        }

        private fun envolver(texto: String, maximo: Float, paint: Paint): List<String> {
            if (texto.isBlank()) return listOf("-")
            val resultado = mutableListOf<String>()
            var actual = ""
            texto.replace("\n", " ").split(Regex("\\s+")).forEach { palabra ->
                val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
                if (paint.measureText(prueba) <= maximo || actual.isEmpty()) actual = prueba
                else {
                    resultado += actual
                    actual = palabra
                }
            }
            if (actual.isNotEmpty()) resultado += actual
            return resultado
        }

        private fun estilo(tamano: Float, negrita: Boolean = false, color: Int = Color.rgb(31, 41, 51)) {
            pintura.textSize = tamano
            pintura.color = color
            pintura.style = Paint.Style.FILL
            pintura.typeface = if (negrita) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        private fun cerrarPagina() {
            val actual = pagina ?: return
            estilo(7.5f, color = Color.GRAY)
            val pie = "Ruralitos - Página $numeroPagina"
            actual.canvas.drawText(pie, ancho - margen - pintura.measureText(pie), alto - 22f, pintura)
            documento.finishPage(actual)
            pagina = null
            canvas = null
        }

        fun finalizar() = cerrarPagina()
    }
}
