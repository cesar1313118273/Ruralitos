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
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddress
import java.io.File
import java.io.OutputStream
import java.util.Locale

object FichaPdfPlantillaExporter {
    // A4 horizontal a 72 puntos por pulgada.
    private const val ANCHO_PAGINA = 842
    private const val ALTO_PAGINA = 595
    private const val MARGEN = 12f

    private data class LimitesHoja(
        val primeraColumna: Int,
        val ultimaColumna: Int,
        val primeraFila: Int,
        val ultimaFila: Int
    )

    private val limites = listOf(
        LimitesHoja(0, 103, 0, 46),
        LimitesHoja(0, 99, 0, 48),
        LimitesHoja(1, 100, 0, 27),
        LimitesHoja(1, 96, 0, 49)
    )

    suspend fun exportar(context: Context, fichaId: Long, destino: Uri) = withContext(Dispatchers.IO) {
        val data = FichaExcelExporter.cargarDatos(context, fichaId)
        context.contentResolver.openOutputStream(destino, "w")?.use { salida ->
            generar(context, data, salida)
        } ?: error("No se pudo abrir el destino PDF.")
    }

    internal fun generar(context: Context, data: FichaExportData, salida: OutputStream) {
        // El PDF nace de la misma copia temporal y completa del Excel que se
        // descargaría. La plantilla incluida en assets permanece siempre limpia.
        FichaExcelExporter.abrirPlantilla(context).use { original ->
            FichaExcelExporter.crearLibroCompletado(context, data).use { completado ->
                val evaluador = completado.creationHelper.createFormulaEvaluator()
                runCatching { evaluador.evaluateAll() }
                val documento = PdfDocument()
                try {
                    for (indice in 0..3) {
                        val pagina = documento.startPage(
                            PdfDocument.PageInfo.Builder(ANCHO_PAGINA, ALTO_PAGINA, indice + 1).create()
                        )
                        dibujarHoja(
                            context = context,
                            canvas = pagina.canvas,
                            original = original,
                            completado = completado,
                            indice = indice,
                            evaluador = evaluador,
                            data = data
                        )
                        documento.finishPage(pagina)
                    }
                    documento.writeTo(salida)
                } finally {
                    documento.close()
                }
            }
        }
    }

    private fun dibujarHoja(
        context: Context,
        canvas: Canvas,
        original: Workbook,
        completado: Workbook,
        indice: Int,
        evaluador: FormulaEvaluator,
        data: FichaExportData
    ) {
        canvas.drawColor(Color.WHITE)
        val fondo = context.assets.open("ficha_plantilla_${indice + 1}.png").use(BitmapFactory::decodeStream)
        val area = ajustarDentro(
            fondo.width.toFloat(), fondo.height.toFloat(),
            RectF(MARGEN, MARGEN, ANCHO_PAGINA - MARGEN, ALTO_PAGINA - MARGEN)
        )
        canvas.drawBitmap(fondo, null, area, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG))
        fondo.recycle()

        val hojaOriginal = original.getSheetAt(indice)
        val hojaCompleta = completado.getSheetAt(indice)
        val geometria = GeometriaHoja(hojaCompleta, limites[indice], area)
        dibujarCeldasDinamicas(canvas, completado, hojaOriginal, hojaCompleta, geometria, evaluador)

        if (indice == 0) {
            data.ficha.firmaUri?.let { abrirBitmap(context, Uri.parse(it)) }?.let { bitmap ->
                dibujarImagen(canvas, bitmap, geometria.rectangulo(90, 42, 97, 45))
                bitmap.recycle()
            }
        }
        if (indice == 3) {
            data.adjuntos.lastOrNull { it.tipo.equals("FAMILIOGRAMA", ignoreCase = true) }
                ?.let { abrirBitmap(context, Uri.parse(it.uri)) }
                ?.let { bitmap ->
                    dibujarImagen(canvas, bitmap, geometria.rectangulo(27, 2, 96, 32))
                    bitmap.recycle()
                }
            data.adjuntos.lastOrNull { it.tipo.equals("CROQUIS", ignoreCase = true) }
                ?.let { abrirBitmap(context, Uri.parse(it.uri)) }
                ?.let { bitmap ->
                    dibujarImagen(canvas, bitmap, geometria.rectangulo(1, 34, 48, 49))
                    bitmap.recycle()
                }
        }
    }

    private fun dibujarCeldasDinamicas(
        canvas: Canvas,
        workbook: Workbook,
        original: Sheet,
        completado: Sheet,
        geometria: GeometriaHoja,
        evaluador: FormulaEvaluator
    ) {
        val formateador = DataFormatter(Locale.getDefault())
        val regiones = (0 until completado.numMergedRegions).map(completado::getMergedRegion)
        val limites = geometria.limites
        for (fila in limites.primeraFila..limites.ultimaFila) {
            for (columna in limites.primeraColumna..limites.ultimaColumna) {
                val celda = completado.getRow(fila)?.getCell(columna) ?: continue
                val region = regiones.firstOrNull { it.isInRange(fila, columna) }
                if (region != null && (region.firstRow != fila || region.firstColumn != columna)) continue
                val originalCelda = original.getRow(fila)?.getCell(columna)
                val esFormula = celda.cellTypeEnum == CellType.FORMULA
                if (!esFormula && clave(celda) == clave(originalCelda)) continue
                val texto = runCatching { formateador.formatCellValue(celda, evaluador) }
                    .getOrElse { celda.toString() }
                if (texto.isBlank()) continue
                val rect = if (region == null) geometria.rectangulo(columna, fila, columna + 1, fila + 1)
                else geometria.rectangulo(region.firstColumn, region.firstRow, region.lastColumn + 1, region.lastRow + 1)
                dibujarTexto(canvas, workbook, celda, texto, rect, geometria.escalaVertical)
            }
        }
    }

    private fun dibujarTexto(
        canvas: Canvas,
        workbook: Workbook,
        celda: Cell,
        texto: String,
        rect: RectF,
        escalaVertical: Float
    ) {
        val estilo = celda.cellStyle
        val fuente = workbook.getFontAt(estilo.fontIndex)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(
                Typeface.SANS_SERIF,
                if (fuente.bold) Typeface.BOLD else Typeface.NORMAL
            )
            textSize = (fuente.fontHeightInPoints * escalaVertical).coerceAtLeast(2.2f)
        }
        val maximo = (rect.width() - 2f).coerceAtLeast(2f)
        while (paint.measureText(texto) > maximo && paint.textSize > 2.1f) paint.textSize -= 0.25f
        val x = when (estilo.alignmentEnum) {
            HorizontalAlignment.CENTER, HorizontalAlignment.CENTER_SELECTION -> rect.centerX() - paint.measureText(texto) / 2f
            HorizontalAlignment.RIGHT -> rect.right - paint.measureText(texto) - 1f
            else -> rect.left + 1f
        }
        val metricas = paint.fontMetrics
        val y = rect.centerY() - (metricas.ascent + metricas.descent) / 2f
        canvas.save()
        canvas.clipRect(rect)
        canvas.drawText(texto.replace('\n', ' '), x, y, paint)
        canvas.restore()
    }

    private fun clave(celda: Cell?): String = when (celda?.cellTypeEnum) {
        null, CellType.BLANK -> ""
        CellType.FORMULA -> "F:${celda.cellFormula}"
        else -> "${celda.cellTypeEnum}:${celda}"
    }

    private class GeometriaHoja(
        hoja: Sheet,
        val limites: LimitesHoja,
        private val area: RectF
    ) {
        private val columnas = FloatArray(limites.ultimaColumna - limites.primeraColumna + 2)
        private val filas = FloatArray(limites.ultimaFila - limites.primeraFila + 2)
        private val anchoOrigen: Float
        private val altoOrigen: Float
        val escalaVertical: Float

        init {
            for (col in limites.primeraColumna..limites.ultimaColumna) {
                val indice = col - limites.primeraColumna
                columnas[indice + 1] = columnas[indice] + hoja.getColumnWidthInPixels(col)
            }
            for (fila in limites.primeraFila..limites.ultimaFila) {
                val indice = fila - limites.primeraFila
                val alto = hoja.getRow(fila)?.heightInPoints ?: hoja.defaultRowHeightInPoints
                filas[indice + 1] = filas[indice] + alto
            }
            anchoOrigen = columnas.last().coerceAtLeast(1f)
            altoOrigen = filas.last().coerceAtLeast(1f)
            escalaVertical = area.height() / altoOrigen
        }

        fun rectangulo(col1: Int, fila1: Int, col2: Int, fila2: Int): RectF {
            val izquierda = offsetColumna(col1)
            val derecha = offsetColumna(col2)
            val arriba = offsetFila(fila1)
            val abajo = offsetFila(fila2)
            return RectF(
                area.left + izquierda / anchoOrigen * area.width(),
                area.top + arriba / altoOrigen * area.height(),
                area.left + derecha / anchoOrigen * area.width(),
                area.top + abajo / altoOrigen * area.height()
            )
        }

        private fun offsetColumna(columna: Int): Float {
            val indice = (columna - limites.primeraColumna).coerceIn(0, columnas.lastIndex)
            return columnas[indice]
        }

        private fun offsetFila(fila: Int): Float {
            val indice = (fila - limites.primeraFila).coerceIn(0, filas.lastIndex)
            return filas[indice]
        }
    }

    private fun ajustarDentro(ancho: Float, alto: Float, contenedor: RectF): RectF {
        val escala = minOf(contenedor.width() / ancho, contenedor.height() / alto)
        val w = ancho * escala
        val h = alto * escala
        return RectF(
            contenedor.centerX() - w / 2f,
            contenedor.centerY() - h / 2f,
            contenedor.centerX() + w / 2f,
            contenedor.centerY() + h / 2f
        )
    }

    private fun dibujarImagen(canvas: Canvas, bitmap: Bitmap, destino: RectF) {
        val rect = ajustarDentro(bitmap.width.toFloat(), bitmap.height.toFloat(), destino)
        canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG))
    }

    private fun abrirBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
        if (uri.scheme == "file" || uri.scheme.isNullOrBlank()) {
            uri.path?.let { BitmapFactory.decodeFile(File(it).absolutePath) }
        } else {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }
    }.getOrNull()
}
