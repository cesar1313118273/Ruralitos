package com.ruralitos.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Xml
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFDrawing
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFPictureData
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.xmlpull.v1.XmlPullParser
import java.io.OutputStream
import java.io.StringReader
import java.util.Locale

/**
 * Convierte el libro de Excel ya llenado en un PDF: una hoja de Excel = una página A4 horizontal.
 *
 * El Excel es la única fuente de verdad: se dibujan sus celdas (relleno, bordes y texto), las
 * formas de la plantilla y las imágenes insertadas, todo en vectores. Las hojas ya vienen
 * configuradas con área de impresión, ajuste a una página y márgenes, y aquí se respeta eso.
 */
internal object ExcelPdfRenderer {
    private const val ANCHO_PAGINA = 842
    private const val ALTO_PAGINA = 595
    private const val EMU_POR_PUNTO = 12700f

    fun render(
        context: Context,
        libro: XSSFWorkbook,
        evaluador: FormulaEvaluator,
        salida: OutputStream
    ) {
        val fuentes = Fuentes(context)
        val documento = PdfDocument()
        try {
            for (indice in 0 until libro.numberOfSheets) {
                val hoja = libro.getSheetAt(indice) as XSSFSheet
                val pagina = documento.startPage(
                    PdfDocument.PageInfo.Builder(ANCHO_PAGINA, ALTO_PAGINA, indice + 1).create()
                )
                try {
                    HojaPdf(libro, hoja, indice, evaluador, fuentes).dibujar(pagina.canvas)
                } finally {
                    documento.finishPage(pagina)
                }
            }
            documento.writeTo(salida)
        } finally {
            documento.close()
        }
    }

    /** Liberation Sans: compatible en medidas con Arial, que usa la plantilla. */
    private class Fuentes(context: Context) {
        private val normal = cargar(context, "Regular", Typeface.NORMAL)
        private val negrita = cargar(context, "Bold", Typeface.BOLD)
        private val cursiva = cargar(context, "Italic", Typeface.ITALIC)
        private val negritaCursiva = cargar(context, "BoldItalic", Typeface.BOLD_ITALIC)

        fun tipografia(negrita: Boolean, cursiva: Boolean): Typeface = when {
            negrita && cursiva -> negritaCursiva
            negrita -> this.negrita
            cursiva -> this.cursiva
            else -> normal
        }

        private fun cargar(context: Context, estilo: String, respaldo: Int): Typeface =
            runCatching {
                Typeface.createFromAsset(context.assets, "fonts/LiberationSans/LiberationSans-$estilo.ttf")
            }.getOrElse { Typeface.create(Typeface.SANS_SERIF, respaldo) }
    }

    private class HojaPdf(
        private val libro: XSSFWorkbook,
        private val hoja: XSSFSheet,
        private val indice: Int,
        private val evaluador: FormulaEvaluator,
        private val fuentes: Fuentes
    ) {
        private val area: CellRangeAddress = areaImpresion()
        private val ultimaColumnaCalculada = area.lastColumn + 80
        private val columnaX = FloatArray(ultimaColumnaCalculada + 2)
        private val filaY: FloatArray
        private val formateador = DataFormatter(Locale.getDefault())
        private val combinadas = HashMap<Long, CellRangeAddress>()
        private val cubiertas = HashSet<Long>()
        private val regionDe = HashMap<Long, CellRangeAddress>()

        init {
            // POI devuelve baseColWidth en lugar de defaultColWidth para las columnas sin ancho propio;
            // aquí se leen directamente los anchos del archivo.
            val formato = hoja.ctWorksheet.sheetFormatPr
            val anchoPorDefecto = if (formato != null && formato.isSetDefaultColWidth) formato.defaultColWidth
            else (if (formato != null) formato.baseColWidth.toDouble() else 8.0) + 0.7109375
            val anchos = HashMap<Int, Double>()
            val ocultas = HashSet<Int>()
            hoja.ctWorksheet.colsArray.forEach { grupo ->
                grupo.colArray.forEach { col ->
                    for (indiceColumna in (col.min.toInt() - 1)..(col.max.toInt() - 1)) {
                        if (col.isSetWidth) anchos[indiceColumna] = col.width
                        if (col.hidden) ocultas += indiceColumna
                    }
                }
            }
            for (c in 0..ultimaColumnaCalculada) {
                val caracteres = anchos[c] ?: anchoPorDefecto
                val pixeles = ((256.0 * caracteres + 128.0 / 7.0) / 256.0 * 7.0).toInt()
                val ancho = if (c in ocultas) 0f else pixeles * 0.75f
                columnaX[c + 1] = columnaX[c] + ancho
            }
            val ultimaFila = maxOf(area.lastRow, hoja.lastRowNum) + 80
            filaY = FloatArray(ultimaFila + 2)
            for (f in 0..ultimaFila) {
                val fila = hoja.getRow(f)
                val alto = when {
                    fila != null && fila.zeroHeight -> 0f
                    fila != null -> fila.heightInPoints
                    else -> hoja.defaultRowHeightInPoints
                }
                filaY[f + 1] = filaY[f] + alto
            }
            for (i in 0 until hoja.numMergedRegions) {
                val region = hoja.getMergedRegion(i)
                combinadas[clave(region.firstRow, region.firstColumn)] = region
                for (f in region.firstRow..region.lastRow) {
                    for (c in region.firstColumn..region.lastColumn) {
                        regionDe[clave(f, c)] = region
                        if (f != region.firstRow || c != region.firstColumn) cubiertas += clave(f, c)
                    }
                }
            }
        }

        private fun clave(fila: Int, columna: Int): Long = (fila.toLong() shl 20) or columna.toLong()

        private fun areaImpresion(): CellRangeAddress {
            runCatching {
                val texto = libro.getPrintArea(indice)
                if (!texto.isNullOrBlank()) {
                    return CellRangeAddress.valueOf(texto.substringAfter('!').substringBefore(','))
                }
            }
            return CellRangeAddress(0, hoja.lastRowNum, 0, 60)
        }

        // Coordenadas relativas al inicio del área de impresión (en puntos).
        private fun x(columna: Int): Float {
            val c = columna.coerceIn(0, columnaX.size - 1)
            return columnaX[c] - columnaX[area.firstColumn]
        }

        private fun y(fila: Int): Float {
            val f = fila.coerceIn(0, filaY.size - 1)
            return filaY[f] - filaY[area.firstRow]
        }

        private fun rectangulo(f1: Int, c1: Int, f2: Int, c2: Int) =
            RectF(x(c1), y(f1), x(c2 + 1), y(f2 + 1))

        fun dibujar(canvas: Canvas) {
            canvas.drawColor(Color.WHITE)
            val ancho = x(area.lastColumn + 1).coerceAtLeast(1f)
            val alto = y(area.lastRow + 1).coerceAtLeast(1f)
            val margen = 7.2f // 0,1 pulgada, como la plantilla
            val disponibleX = ANCHO_PAGINA - 2 * margen
            val disponibleY = ALTO_PAGINA - 2 * margen
            val escala = minOf(disponibleX / ancho, disponibleY / alto)
            val desplazamientoX = (ANCHO_PAGINA - ancho * escala) / 2f
            val desplazamientoY = (ALTO_PAGINA - alto * escala) / 2f

            canvas.save()
            canvas.translate(desplazamientoX, desplazamientoY)
            canvas.scale(escala, escala)
            canvas.clipRect(0f, 0f, ancho, alto)
            try {
                dibujarRellenos(canvas)
                dibujarTextos(canvas)
                dibujarBordes(canvas)
                dibujarObjetos(canvas)
            } finally {
                canvas.restore()
            }
        }

        // ---------------------------------------------------------------- relleno

        private fun dibujarRellenos(canvas: Canvas) {
            val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            for (f in area.firstRow..area.lastRow) {
                val fila = hoja.getRow(f) ?: continue
                for (c in area.firstColumn..area.lastColumn) {
                    if (clave(f, c) in cubiertas) continue
                    val celda = fila.getCell(c) ?: continue
                    val estilo = celda.cellStyle as? XSSFCellStyle ?: continue
                    if (estilo.fillPatternEnum != FillPatternType.SOLID_FOREGROUND) continue
                    val color = colorDe(estilo.fillForegroundXSSFColor) ?: continue
                    val region = combinadas[clave(f, c)]
                    val rect = if (region == null) rectangulo(f, c, f, c)
                    else rectangulo(region.firstRow, region.firstColumn, region.lastRow, region.lastColumn)
                    if (rect.width() <= 0f || rect.height() <= 0f) continue
                    pincel.color = color
                    // Se extiende un poco para que no queden líneas blancas entre celdas.
                    canvas.drawRect(rect.left - 0.2f, rect.top - 0.2f, rect.right + 0.2f, rect.bottom + 0.2f, pincel)
                }
            }
        }

        // ---------------------------------------------------------------- bordes

        private fun dibujarBordes(canvas: Canvas) {
            val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.BUTT
            }
            for (f in area.firstRow..area.lastRow) {
                val fila = hoja.getRow(f) ?: continue
                for (c in area.firstColumn..area.lastColumn) {
                    val celda = fila.getCell(c) ?: continue
                    val estilo = celda.cellStyle as? XSSFCellStyle ?: continue
                    val rect = rectangulo(f, c, f, c)
                    if (rect.width() <= 0f || rect.height() <= 0f) continue
                    val region = regionDe[clave(f, c)]
                    val arriba = region == null || f == region.firstRow
                    val abajo = region == null || f == region.lastRow
                    val izquierda = region == null || c == region.firstColumn
                    val derecha = region == null || c == region.lastColumn
                    if (arriba) linea(canvas, pincel, estilo.borderTopEnum, estilo.topBorderXSSFColor, rect.left, rect.top, rect.right, rect.top)
                    if (abajo) linea(canvas, pincel, estilo.borderBottomEnum, estilo.bottomBorderXSSFColor, rect.left, rect.bottom, rect.right, rect.bottom)
                    if (izquierda) linea(canvas, pincel, estilo.borderLeftEnum, estilo.leftBorderXSSFColor, rect.left, rect.top, rect.left, rect.bottom)
                    if (derecha) linea(canvas, pincel, estilo.borderRightEnum, estilo.rightBorderXSSFColor, rect.right, rect.top, rect.right, rect.bottom)
                }
            }
        }

        private fun linea(
            canvas: Canvas,
            pincel: Paint,
            borde: BorderStyle?,
            color: XSSFColor?,
            x1: Float, y1: Float, x2: Float, y2: Float
        ) {
            if (borde == null || borde == BorderStyle.NONE) return
            val grosor = when (borde) {
                BorderStyle.HAIR -> 0.25f
                BorderStyle.THIN, BorderStyle.DASHED, BorderStyle.DOTTED,
                BorderStyle.DASH_DOT, BorderStyle.DASH_DOT_DOT -> 0.6f
                BorderStyle.MEDIUM, BorderStyle.MEDIUM_DASHED, BorderStyle.MEDIUM_DASH_DOT,
                BorderStyle.MEDIUM_DASH_DOT_DOT, BorderStyle.SLANTED_DASH_DOT -> 1.3f
                BorderStyle.THICK -> 2f
                BorderStyle.DOUBLE -> 1.4f
                else -> 0.6f
            }
            pincel.strokeWidth = grosor
            pincel.color = colorDe(color) ?: Color.BLACK
            pincel.pathEffect = when (borde) {
                BorderStyle.DASHED, BorderStyle.MEDIUM_DASHED -> DashPathEffect(floatArrayOf(3f, 2f), 0f)
                BorderStyle.DOTTED, BorderStyle.HAIR -> DashPathEffect(floatArrayOf(1f, 1f), 0f)
                BorderStyle.DASH_DOT, BorderStyle.MEDIUM_DASH_DOT, BorderStyle.SLANTED_DASH_DOT ->
                    DashPathEffect(floatArrayOf(3f, 1.5f, 1f, 1.5f), 0f)
                BorderStyle.DASH_DOT_DOT, BorderStyle.MEDIUM_DASH_DOT_DOT ->
                    DashPathEffect(floatArrayOf(3f, 1.5f, 1f, 1.5f, 1f, 1.5f), 0f)
                else -> null
            }
            canvas.drawLine(x1, y1, x2, y2, pincel)
        }

        // ---------------------------------------------------------------- texto

        private fun dibujarTextos(canvas: Canvas) {
            for (f in area.firstRow..area.lastRow) {
                val fila = hoja.getRow(f) ?: continue
                for (c in area.firstColumn..area.lastColumn) {
                    if (clave(f, c) in cubiertas) continue
                    val celda = fila.getCell(c) ?: continue
                    val texto = textoDe(celda)
                    if (texto.isBlank()) continue
                    val region = combinadas[clave(f, c)]
                    val rect = if (region == null) rectangulo(f, c, f, c)
                    else rectangulo(region.firstRow, region.firstColumn, region.lastRow, region.lastColumn)
                    if (rect.width() <= 0f || rect.height() <= 0f) continue
                    dibujarTextoCelda(canvas, celda, texto, rect, f, region?.firstColumn ?: c, region?.lastColumn ?: c)
                }
            }
        }

        private fun textoDe(celda: Cell): String = runCatching {
            formateador.formatCellValue(celda, evaluador)
        }.getOrElse { celda.toString() }

        private fun esNumerica(celda: Cell): Boolean {
            val tipo = if (celda.cellTypeEnum == CellType.FORMULA) celda.cachedFormulaResultTypeEnum else celda.cellTypeEnum
            return tipo == CellType.NUMERIC
        }

        private fun dibujarTextoCelda(
            canvas: Canvas,
            celda: Cell,
            texto: String,
            rect: RectF,
            fila: Int,
            primeraColumna: Int,
            ultimaColumna: Int
        ) {
            val estilo = celda.cellStyle as XSSFCellStyle
            val fuente = estilo.font
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = fuentes.tipografia(fuente.bold, fuente.italic)
                textSize = fuente.fontHeightInPoints.toFloat().coerceAtLeast(1f)
                color = colorDe(fuente.xssfColor) ?: Color.BLACK
                isUnderlineText = fuente.underline.toInt() != 0
            }
            val horizontal = when (val h = estilo.alignmentEnum) {
                HorizontalAlignment.GENERAL ->
                    if (esNumerica(celda)) HorizontalAlignment.RIGHT else HorizontalAlignment.LEFT
                else -> h
            }
            val vertical = estilo.verticalAlignmentEnum ?: VerticalAlignment.BOTTOM
            val relleno = 1.6f + estilo.indention.toFloat() * 3f
            val rotacion = estilo.rotation.toInt()
            val giro = when (rotacion) {
                in 1..90 -> -rotacion.toFloat()
                in 91..180 -> (rotacion - 90).toFloat()
                else -> 0f
            }
            val envolver = estilo.wrapText || rotacion == 255
            val contenido =
                if (rotacion == 255) texto.map { it.toString() }.joinToString("\n") else texto

            // Zona donde se coloca el texto (girada si la celda tiene rotación).
            var zona = RectF(rect)
            if (giro != 0f) {
                val vertical90 = kotlin.math.abs(giro) > 45f
                val w = if (vertical90) rect.height() else rect.width()
                val h = if (vertical90) rect.width() else rect.height()
                zona = RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
            }
            val ancho = (zona.width() - 2 * relleno).coerceAtLeast(2f)

            if (estilo.shrinkToFit && !envolver) {
                while (paint.measureText(contenido) > ancho && paint.textSize > 2f) paint.textSize -= 0.25f
            }

            // Como en Excel, el texto largo continúa sobre las celdas vecinas vacías.
            var recorte = RectF(rect)
            val medida = paint.measureText(contenido.replace('\n', ' '))
            if (!envolver && giro == 0f && medida > ancho) {
                val necesario = medida + 2 * relleno
                recorte = when (horizontal) {
                    HorizontalAlignment.LEFT -> extender(rect, fila, primeraColumna, ultimaColumna, necesario, true, false)
                    HorizontalAlignment.RIGHT -> extender(rect, fila, primeraColumna, ultimaColumna, necesario, false, true)
                    else -> extender(rect, fila, primeraColumna, ultimaColumna, necesario, true, true)
                }
                zona = RectF(recorte)
            }

            canvas.save()
            canvas.clipRect(recorte)
            if (giro != 0f) {
                canvas.translate(rect.centerX(), rect.centerY())
                canvas.rotate(giro)
            }

            if (!envolver) {
                val x = when (horizontal) {
                    HorizontalAlignment.CENTER, HorizontalAlignment.CENTER_SELECTION ->
                        zona.centerX() - medida / 2f
                    HorizontalAlignment.RIGHT -> zona.right - relleno - medida
                    else -> zona.left + relleno
                }
                val metricas = paint.fontMetrics
                val alturaLinea = metricas.descent - metricas.ascent
                val yBase = when (vertical) {
                    VerticalAlignment.TOP -> zona.top + 0.8f - metricas.ascent
                    VerticalAlignment.CENTER, VerticalAlignment.JUSTIFY, VerticalAlignment.DISTRIBUTED ->
                        zona.centerY() - alturaLinea / 2f - metricas.ascent
                    else -> zona.bottom - 0.8f - metricas.descent
                }
                canvas.drawText(contenido.replace('\n', ' '), x, yBase, paint)
            } else {
                val alineacion = when (horizontal) {
                    HorizontalAlignment.CENTER, HorizontalAlignment.CENTER_SELECTION -> Layout.Alignment.ALIGN_CENTER
                    HorizontalAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                    else -> Layout.Alignment.ALIGN_NORMAL
                }
                val disposicion = StaticLayout.Builder
                    .obtain(contenido, 0, contenido.length, paint, ancho.toInt().coerceAtLeast(2))
                    .setAlignment(alineacion)
                    .setIncludePad(false)
                    .setLineSpacing(0f, 1f)
                    .build()
                val alto = disposicion.height.toFloat()
                val arriba = when (vertical) {
                    VerticalAlignment.TOP -> zona.top + 0.8f
                    VerticalAlignment.CENTER, VerticalAlignment.JUSTIFY, VerticalAlignment.DISTRIBUTED ->
                        zona.centerY() - alto / 2f
                    else -> zona.bottom - 0.8f - alto
                }
                canvas.translate(zona.left + relleno, maxOf(arriba, zona.top))
                disposicion.draw(canvas)
            }
            canvas.restore()
        }

        /** Amplía el recuadro sobre celdas vecinas vacías hasta que quepa el texto. */
        private fun extender(
            base: RectF,
            fila: Int,
            primeraColumna: Int,
            ultimaColumna: Int,
            necesario: Float,
            haciaDerecha: Boolean,
            haciaIzquierda: Boolean
        ): RectF {
            var izquierda = base.left
            var derecha = base.right
            var colDerecha = ultimaColumna + 1
            var colIzquierda = primeraColumna - 1
            val filaPoi = hoja.getRow(fila)
            fun libre(columna: Int): Boolean {
                if (columna < area.firstColumn || columna > area.lastColumn) return false
                if (clave(fila, columna) in cubiertas) return false
                val vecina = filaPoi?.getCell(columna)
                return vecina == null || textoDe(vecina).isBlank()
            }
            while (derecha - izquierda < necesario) {
                var avanzo = false
                if (haciaDerecha && libre(colDerecha)) {
                    derecha = x(colDerecha + 1); colDerecha++; avanzo = true
                }
                if (derecha - izquierda >= necesario) break
                if (haciaIzquierda && libre(colIzquierda)) {
                    izquierda = x(colIzquierda); colIzquierda--; avanzo = true
                }
                if (!avanzo) break
            }
            return RectF(izquierda, base.top, derecha, base.bottom)
        }

        // ---------------------------------------------------------------- formas e imágenes

        private fun dibujarObjetos(canvas: Canvas) {
            val dibujo: XSSFDrawing = hoja.drawingPatriarch ?: return
            val xml = runCatching { dibujo.ctDrawing.xmlText() }.getOrNull() ?: return
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var evento = parser.eventType
            while (evento != XmlPullParser.END_DOCUMENT) {
                if (evento == XmlPullParser.START_TAG && parser.local() in ANCLAS) {
                    val ancla = leerAncla(parser)
                    runCatching { dibujarAncla(canvas, dibujo, ancla) }
                }
                evento = parser.next()
            }
        }

        private class Ancla(val rect: RectF, val elementos: List<Nodo>)

        private class Nodo(val tipo: String) {
            var desdeX = 0f; var desdeY = 0f; var anchoX = 0f; var altoY = 0f
            var giroGrados = 0f
            var voltearH = false; var voltearV = false
            var geometria = "rect"
            var relleno: Int? = null
            var linea: Int? = null
            var grosorLinea = 0.75f
            var incrustar: String? = null
            var parrafos = mutableListOf<Parrafo>()
            var finTipo: String? = null
            var inicioTipo: String? = null
            var anclaTexto = "t"
            var margenIzq = 0f; var margenArr = 0f; var margenDer = 0f; var margenAba = 0f
            // Grupos
            var hijoDesdeX = 0f; var hijoDesdeY = 0f; var hijoAnchoX = 1f; var hijoAltoY = 1f
            val hijos = mutableListOf<Nodo>()
        }

        private class Parrafo(var alinear: String = "l") {
            val tramos = mutableListOf<Tramo>()
        }

        private class Tramo(
            var texto: String = "",
            var tamano: Float = 10f,
            var negrita: Boolean = false,
            var cursiva: Boolean = false,
            var color: Int = Color.BLACK
        )

        private fun XmlPullParser.local(): String = name.substringAfter(':')

        private fun XmlPullParser.atributo(nombre: String): String? {
            for (i in 0 until attributeCount) {
                if (getAttributeName(i).substringAfter(':') == nombre) return getAttributeValue(i)
            }
            return null
        }

        private fun leerAncla(p: XmlPullParser): Ancla {
            val fin = p.local()
            var col1 = 0; var off1x = 0f; var row1 = 0; var off1y = 0f
            var col2 = 0; var off2x = 0f; var row2 = 0; var off2y = 0f
            val elementos = mutableListOf<Nodo>()
            var contexto = ""
            var depth = p.depth
            var evento = p.next()
            while (!(evento == XmlPullParser.END_TAG && p.depth == depth && p.local() == fin)) {
                if (evento == XmlPullParser.END_DOCUMENT) break
                if (evento == XmlPullParser.START_TAG) {
                    when (p.local()) {
                        "from" -> contexto = "from"
                        "to" -> contexto = "to"
                        "col" -> {
                            val v = p.nextText().trim().toIntOrNull() ?: 0
                            if (contexto == "from") col1 = v else col2 = v
                        }
                        "colOff" -> {
                            val v = (p.nextText().trim().toFloatOrNull() ?: 0f) / EMU_POR_PUNTO
                            if (contexto == "from") off1x = v else off2x = v
                        }
                        "row" -> {
                            val v = p.nextText().trim().toIntOrNull() ?: 0
                            if (contexto == "from") row1 = v else row2 = v
                        }
                        "rowOff" -> {
                            val v = (p.nextText().trim().toFloatOrNull() ?: 0f) / EMU_POR_PUNTO
                            if (contexto == "from") off1y = v else off2y = v
                        }
                        "sp", "cxnSp", "grpSp", "pic" -> elementos += leerNodo(p)
                    }
                }
                evento = p.next()
            }
            val rect = RectF(
                x(col1) + off1x, y(row1) + off1y,
                x(col2) + off2x, y(row2) + off2y
            )
            return Ancla(rect, elementos)
        }

        private fun leerNodo(p: XmlPullParser): Nodo {
            val etiqueta = p.local()
            val nodo = Nodo(etiqueta)
            val profundidad = p.depth
            var evento = p.next()
            var enLinea = false
            var enTexto = false
            var parrafo: Parrafo? = null
            var tramo: Tramo? = null
            var enPropTramo = false
            var enSpPr = false
            while (!(evento == XmlPullParser.END_TAG && p.depth == profundidad && p.local() == etiqueta)) {
                if (evento == XmlPullParser.END_DOCUMENT) break
                if (evento == XmlPullParser.START_TAG) {
                    when (p.local()) {
                        "sp", "cxnSp", "grpSp" -> if (p.depth > profundidad) {
                            nodo.hijos += leerNodo(p)
                        }
                        "spPr" -> enSpPr = true
                        "xfrm" -> {
                            nodo.giroGrados = (p.atributo("rot")?.toFloatOrNull() ?: 0f) / 60000f
                            nodo.voltearH = p.atributo("flipH") == "1" || p.atributo("flipH") == "true"
                            nodo.voltearV = p.atributo("flipV") == "1" || p.atributo("flipV") == "true"
                        }
                        "off" -> if (!enTexto) {
                            nodo.desdeX = (p.atributo("x")?.toFloatOrNull() ?: 0f)
                            nodo.desdeY = (p.atributo("y")?.toFloatOrNull() ?: 0f)
                        }
                        "ext" -> if (p.atributo("cx") != null) {
                            nodo.anchoX = (p.atributo("cx")?.toFloatOrNull() ?: 0f)
                            nodo.altoY = (p.atributo("cy")?.toFloatOrNull() ?: 0f)
                        }
                        "chOff" -> {
                            nodo.hijoDesdeX = p.atributo("x")?.toFloatOrNull() ?: 0f
                            nodo.hijoDesdeY = p.atributo("y")?.toFloatOrNull() ?: 0f
                        }
                        "chExt" -> {
                            nodo.hijoAnchoX = (p.atributo("cx")?.toFloatOrNull() ?: 1f).coerceAtLeast(1f)
                            nodo.hijoAltoY = (p.atributo("cy")?.toFloatOrNull() ?: 1f).coerceAtLeast(1f)
                        }
                        "prstGeom" -> nodo.geometria = p.atributo("prst") ?: "rect"
                        "tailEnd" -> nodo.finTipo = p.atributo("type")
                        "headEnd" -> nodo.inicioTipo = p.atributo("type")
                        "noFill" -> {
                            if (enLinea) nodo.linea = null else if (enSpPr && !enTexto) nodo.relleno = null
                        }
                        "ln" -> {
                            enLinea = true
                            nodo.grosorLinea = ((p.atributo("w")?.toFloatOrNull() ?: 9525f) / EMU_POR_PUNTO)
                                .coerceAtLeast(0.25f)
                        }
                        "srgbClr" -> {
                            val valor = p.atributo("val")
                            val color = valor?.let { runCatching { Color.parseColor("#$it") }.getOrNull() }
                            if (tramo != null && enPropTramo) tramo.color = color ?: Color.BLACK
                            else if (enLinea) nodo.linea = color
                            else if (enSpPr) nodo.relleno = color
                        }
                        "blip" -> nodo.incrustar = p.atributo("embed")
                        "bodyPr" -> {
                            enTexto = true
                            nodo.anclaTexto = p.atributo("anchor") ?: "t"
                            nodo.margenIzq = (p.atributo("lIns")?.toFloatOrNull() ?: 91440f) / EMU_POR_PUNTO
                            nodo.margenArr = (p.atributo("tIns")?.toFloatOrNull() ?: 45720f) / EMU_POR_PUNTO
                            nodo.margenDer = (p.atributo("rIns")?.toFloatOrNull() ?: 91440f) / EMU_POR_PUNTO
                            nodo.margenAba = (p.atributo("bIns")?.toFloatOrNull() ?: 45720f) / EMU_POR_PUNTO
                        }
                        "p" -> if (enTexto) {
                            parrafo = Parrafo().also { nodo.parrafos += it }
                        }
                        "pPr" -> parrafo?.alinear = p.atributo("algn") ?: "l"
                        "r" -> if (enTexto) {
                            tramo = Tramo().also { parrafo?.tramos?.add(it) }
                        }
                        "rPr" -> if (tramo != null) {
                            enPropTramo = true
                            tramo.tamano = (p.atributo("sz")?.toFloatOrNull() ?: 1000f) / 100f
                            tramo.negrita = p.atributo("b") == "1"
                            tramo.cursiva = p.atributo("i") == "1"
                        }
                        "t" -> if (tramo != null) tramo.texto = p.nextText()
                    }
                } else if (evento == XmlPullParser.END_TAG) {
                    when (p.local()) {
                        "ln" -> enLinea = false
                        "spPr" -> enSpPr = false
                        "rPr" -> enPropTramo = false
                        "r" -> tramo = null
                    }
                }
                evento = p.next()
            }
            return nodo
        }

        private fun dibujarAncla(canvas: Canvas, dibujo: XSSFDrawing, ancla: Ancla) {
            for (nodo in ancla.elementos) dibujarNodo(canvas, dibujo, nodo, ancla.rect, null)
        }

        /** [padre] y [escalaHijo] llevan las coordenadas del grupo al rectángulo de la hoja. */
        private fun dibujarNodo(
            canvas: Canvas,
            dibujo: XSSFDrawing,
            nodo: Nodo,
            rectAncla: RectF,
            grupo: Nodo?
        ) {
            when (nodo.tipo) {
                "grpSp" -> {
                    // El grupo ocupa el rectángulo del ancla; sus hijos se reubican dentro de él.
                    for (hijo in nodo.hijos) dibujarHijo(canvas, dibujo, hijo, rectAncla, nodo)
                }
                else -> dibujarHoja(canvas, dibujo, nodo, rectAncla)
            }
        }

        private fun dibujarHijo(
            canvas: Canvas,
            dibujo: XSSFDrawing,
            hijo: Nodo,
            rectGrupo: RectF,
            grupo: Nodo
        ) {
            val sx = rectGrupo.width() / grupo.hijoAnchoX
            val sy = rectGrupo.height() / grupo.hijoAltoY
            val rect = RectF(
                rectGrupo.left + (hijo.desdeX - grupo.hijoDesdeX) * sx,
                rectGrupo.top + (hijo.desdeY - grupo.hijoDesdeY) * sy,
                rectGrupo.left + (hijo.desdeX + hijo.anchoX - grupo.hijoDesdeX) * sx,
                rectGrupo.top + (hijo.desdeY + hijo.anchoYAlto() - grupo.hijoDesdeY) * sy
            )
            if (hijo.tipo == "grpSp") {
                for (nieto in hijo.hijos) dibujarHijo(canvas, dibujo, nieto, rect, hijo)
            } else {
                dibujarHoja(canvas, dibujo, hijo, rect)
            }
        }

        private fun Nodo.anchoYAlto(): Float = altoY

        private fun dibujarHoja(canvas: Canvas, dibujo: XSSFDrawing, nodo: Nodo, rect: RectF) {
            if (nodo.tipo == "pic") {
                val id = nodo.incrustar ?: return
                val datos = (dibujo.getRelationById(id) as? XSSFPictureData)?.data ?: return
                val bitmap = BitmapFactory.decodeByteArray(datos, 0, datos.size) ?: return
                try {
                    val destino = ajustar(bitmap.width.toFloat(), bitmap.height.toFloat(), rect)
                    canvas.drawBitmap(
                        bitmap, null, destino,
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    )
                } finally {
                    bitmap.recycle()
                }
                return
            }

            val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = nodo.grosorLinea
                strokeJoin = Paint.Join.MITER
            }
            canvas.save()
            if (nodo.giroGrados != 0f) canvas.rotate(nodo.giroGrados, rect.centerX(), rect.centerY())
            val forma = Path()
            var inicioX = 0f; var inicioY = 0f; var finX = 0f; var finY = 0f
            val abierta = when (nodo.geometria) {
                "line", "straightConnector1" -> {
                    val a = if (nodo.voltearV) rect.bottom else rect.top
                    val b = if (nodo.voltearV) rect.top else rect.bottom
                    val izquierda = if (nodo.voltearH) rect.right else rect.left
                    val derecha = if (nodo.voltearH) rect.left else rect.right
                    forma.moveTo(izquierda, a)
                    forma.lineTo(derecha, b)
                    inicioX = izquierda; inicioY = a; finX = derecha; finY = b
                    true
                }
                "flowChartConnector", "ellipse" -> {
                    forma.addOval(rect, Path.Direction.CW)
                    false
                }
                "flowChartTerminator", "roundRect" -> {
                    val radio = rect.height() / 2f
                    forma.addRoundRect(rect, radio, radio, Path.Direction.CW)
                    false
                }
                "upArrow" -> {
                    val cx = rect.centerX()
                    val cuerpo = rect.width() * 0.25f
                    val cabeza = rect.height() * 0.45f
                    forma.moveTo(cx, rect.top)
                    forma.lineTo(rect.right, rect.top + cabeza)
                    forma.lineTo(cx + cuerpo, rect.top + cabeza)
                    forma.lineTo(cx + cuerpo, rect.bottom)
                    forma.lineTo(cx - cuerpo, rect.bottom)
                    forma.lineTo(cx - cuerpo, rect.top + cabeza)
                    forma.lineTo(rect.left, rect.top + cabeza)
                    forma.close()
                    false
                }
                else -> {
                    forma.addRect(rect, Path.Direction.CW)
                    false
                }
            }
            if (!abierta && nodo.relleno != null) {
                relleno.color = nodo.relleno!!
                canvas.drawPath(forma, relleno)
            }
            nodo.linea?.let {
                trazo.color = it
                canvas.drawPath(forma, trazo)
                if (abierta) {
                    extremoDeLinea(canvas, nodo.finTipo, inicioX, inicioY, finX, finY, nodo.grosorLinea, it)
                    extremoDeLinea(canvas, nodo.inicioTipo, finX, finY, inicioX, inicioY, nodo.grosorLinea, it)
                }
            }
            if (nodo.parrafos.isNotEmpty()) dibujarTextoForma(canvas, nodo, rect)
            canvas.restore()
        }

        /** Punta de la línea: círculo ("oval") o flecha ("arrow") en el punto (x2, y2). */
        private fun extremoDeLinea(
            canvas: Canvas,
            tipo: String?,
            x1: Float, y1: Float, x2: Float, y2: Float,
            grosor: Float,
            color: Int
        ) {
            if (tipo == null || tipo == "none") return
            val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = color }
            val largo = maxOf(grosor * 4f, 5f)
            when (tipo) {
                "oval" -> canvas.drawCircle(x2, y2, maxOf(grosor * 1.6f, 2.2f), relleno)
                "arrow", "triangle", "stealth" -> {
                    val dx = x2 - x1
                    val dy = y2 - y1
                    val longitud = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                    val ux = dx / longitud
                    val uy = dy / longitud
                    val baseX = x2 - ux * largo
                    val baseY = y2 - uy * largo
                    val ancho = largo * 0.45f
                    val punta = Path().apply {
                        moveTo(x2, y2)
                        lineTo(baseX - uy * ancho, baseY + ux * ancho)
                        lineTo(baseX + uy * ancho, baseY - ux * ancho)
                        close()
                    }
                    canvas.drawPath(punta, relleno)
                }
            }
        }

        private fun dibujarTextoForma(canvas: Canvas, nodo: Nodo, rect: RectF) {
            val area = RectF(
                rect.left + nodo.margenIzq, rect.top + nodo.margenArr,
                rect.right - nodo.margenDer, rect.bottom - nodo.margenAba
            )
            val lineas = mutableListOf<Pair<StaticLayout, Float>>()
            var altoTotal = 0f
            for (parrafo in nodo.parrafos) {
                val tramos = parrafo.tramos.filter { it.texto.isNotEmpty() }
                if (tramos.isEmpty()) continue
                val base = tramos.first()
                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = fuentes.tipografia(base.negrita, base.cursiva)
                    textSize = base.tamano
                    color = base.color
                }
                val texto = tramos.joinToString("") { it.texto }
                val alineacion = when (parrafo.alinear) {
                    "ctr" -> Layout.Alignment.ALIGN_CENTER
                    "r" -> Layout.Alignment.ALIGN_OPPOSITE
                    else -> Layout.Alignment.ALIGN_NORMAL
                }
                val disposicion = StaticLayout.Builder
                    .obtain(texto, 0, texto.length, paint, area.width().toInt().coerceAtLeast(4))
                    .setAlignment(alineacion)
                    .setIncludePad(false)
                    .build()
                lineas += disposicion to altoTotal
                altoTotal += disposicion.height
            }
            if (lineas.isEmpty()) return
            val arriba = when (nodo.anclaTexto) {
                "ctr" -> area.centerY() - altoTotal / 2f
                "b" -> area.bottom - altoTotal
                else -> area.top
            }
            canvas.save()
            canvas.clipRect(rect)
            for ((disposicion, desplazamiento) in lineas) {
                canvas.save()
                canvas.translate(area.left, arriba + desplazamiento)
                disposicion.draw(canvas)
                canvas.restore()
            }
            canvas.restore()
        }

        private fun ajustar(ancho: Float, alto: Float, contenedor: RectF): RectF {
            val escala = minOf(contenedor.width() / ancho, contenedor.height() / alto)
            val w = ancho * escala
            val h = alto * escala
            return RectF(
                contenedor.centerX() - w / 2f, contenedor.centerY() - h / 2f,
                contenedor.centerX() + w / 2f, contenedor.centerY() + h / 2f
            )
        }

        // ---------------------------------------------------------------- color

        private fun colorDe(color: XSSFColor?): Int? {
            color ?: return null
            if (color.isIndexed) {
                val indice = color.indexed.toInt()
                PALETA_INDEXADA[indice]?.let { return it or 0xFF000000.toInt() }
                if (indice == 64) return Color.BLACK
                if (indice == 65) return Color.WHITE
            }
            val rgb = runCatching { color.rgbWithTint ?: color.rgb }.getOrNull()
            if (rgb != null && rgb.size >= 3) {
                return Color.rgb(
                    rgb[rgb.size - 3].toInt() and 255,
                    rgb[rgb.size - 2].toInt() and 255,
                    rgb[rgb.size - 1].toInt() and 255
                )
            }
            return null
        }
    }

    // Paleta clásica de Excel para colores "indexados" (java.awt no existe en Android).
    private val PALETA_INDEXADA: Map<Int, Int> = run {
        val valores = intArrayOf(
            0x000000, 0xFFFFFF, 0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF, 0x00FFFF,
            0x000000, 0xFFFFFF, 0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF, 0x00FFFF,
            0x800000, 0x008000, 0x000080, 0x808000, 0x800080, 0x008080, 0xC0C0C0, 0x808080,
            0x9999FF, 0x993366, 0xFFFFCC, 0xCCFFFF, 0x660066, 0xFF8080, 0x0066CC, 0xCCCCFF,
            0x000080, 0xFF00FF, 0xFFFF00, 0x00FFFF, 0x800080, 0x800000, 0x008080, 0x0000FF,
            0x00CCFF, 0xCCFFFF, 0xCCFFCC, 0xFFFF99, 0x99CCFF, 0xFF99CC, 0xCC99FF, 0xFFCC99,
            0x3366FF, 0x33CCCC, 0x99CC00, 0xFFCC00, 0xFF9900, 0xFF6600, 0x666699, 0x969696,
            0x003366, 0x339966, 0x003300, 0x333300, 0x993300, 0x993366, 0x333399, 0x333333
        )
        valores.indices.associateWith { valores[it] }
    }

    private val ANCLAS = setOf("twoCellAnchor", "oneCellAnchor")
}
