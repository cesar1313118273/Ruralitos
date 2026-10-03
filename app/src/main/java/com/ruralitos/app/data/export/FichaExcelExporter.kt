package com.ruralitos.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.GestionRiesgoEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.MortalidadFamiliarEntity
import com.ruralitos.app.domain.GrupoEdadFamiliar
import com.ruralitos.app.domain.ReglasGrupoEdadFamiliar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.ClientAnchor
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.PrintSetup
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class CalificacionExport(
    val calificacion: CalificacionRiesgoEntity,
    val valores: List<Int>
)

data class FichaExportData(
    val ficha: FichaFamiliarEntity,
    val miembros: List<MiembroFamiliaEntity>,
    val embarazadas: List<EmbarazadaEntity>,
    val mortalidad: List<MortalidadFamiliarEntity>,
    val calificaciones: List<CalificacionExport>,
    val gestion: List<GestionRiesgoEntity>,
    val contaminacion: List<ContaminacionAmbientalEntity>,
    val lugaresTratamiento: List<LugarTratamientoEntity>,
    val adjuntos: List<AdjuntoFichaEntity>
)

object FichaExcelExporter {
    suspend fun exportar(context: Context, fichaId: Long, destino: Uri) = withContext(Dispatchers.IO) {
        val data = cargarDatos(context, fichaId)
        crearLibroCompletado(context, data).use { workbook ->
            context.contentResolver.openOutputStream(destino, "w")?.use { salida ->
                workbook.write(salida)
            } ?: error("No se pudo abrir el archivo de destino.")
        }
    }

    internal suspend fun cargarDatos(context: Context, fichaId: Long): FichaExportData = withContext(Dispatchers.IO) {
        val database = RuralitosDatabase.obtenerBaseDatos(context)
        val ficha = database.fichaFamiliarDao().buscarPorId(fichaId)
            ?: error("No se encontró la ficha solicitada.")
        val usuarioDao = database.usuarioDao()
        val responsableActual = ficha.responsableCodigo.takeIf { it.isNotBlank() }
            ?.let { usuarioDao.buscarPorCodigoSenescyt(it) }
            ?: ficha.actualizadoPorUsuarioId?.let { usuarioDao.buscarPorId(it) }
            ?: ficha.creadoPorUsuarioId?.let { usuarioDao.buscarPorId(it) }
        val fichaParaExportar = responsableActual?.firmaUri
            ?.takeIf { it.isNotBlank() }
            ?.let { ficha.copy(firmaUri = it) }
            ?: ficha
        val contenido = database.fichaContenidoDao()
        val calificaciones = contenido.listarCalificaciones(fichaId).first()
            .sortedBy { it.id }
            .takeLast(4)
            .map { calificacion ->
                CalificacionExport(
                    calificacion,
                    contenido.obtenerValoresRiesgo(calificacion.id).map { it.valor }
                )
            }
        FichaExportData(
            ficha = fichaParaExportar,
            miembros = contenido.listarMiembros(fichaId).first(),
            embarazadas = contenido.listarEmbarazadas(fichaId).first(),
            mortalidad = contenido.listarMortalidad(fichaId).first(),
            calificaciones = calificaciones,
            gestion = contenido.listarGestionRiesgo(fichaId).first(),
            contaminacion = contenido.listarContaminacion(fichaId).first(),
            lugaresTratamiento = contenido.listarLugaresTratamiento(fichaId).first(),
            adjuntos = contenido.listarAdjuntos(fichaId).first()
        )
    }

    /**
     * Crea una copia descartable de la plantilla oficial y coloca allí todos los
     * datos e imágenes. El archivo de assets nunca se escribe ni se ensucia.
     */
    internal fun crearLibroCompletado(context: Context, data: FichaExportData): XSSFWorkbook {
        val workbook = abrirPlantilla(context)
        return try {
            rellenar(workbook, data)
            insertarAdjuntos(context, workbook, data.adjuntos)
            insertarFirma(context, workbook, data.ficha.firmaUri)
            runCatching { workbook.creationHelper.createFormulaEvaluator().evaluateAll() }
            workbook.setForceFormulaRecalculation(true)
            workbook
        } catch (error: Throwable) {
            workbook.close()
            throw error
        }
    }

    internal fun abrirPlantilla(context: Context): XSSFWorkbook {
        val original = PlantillaPropia.bytes(context)
            ?: context.assets.open("ficha_familiar.xlsx").use { it.readBytes() }
        return XSSFWorkbook(ByteArrayInputStream(sanitizarPlantillaXlsx(original)))
    }

    /**
     * Algunas versiones de Excel agregan una extensión de cálculo sin declarar
     * su prefijo XML. Excel la tolera, pero Apache POI (y otros lectores
     * estrictos) no. Se elimina únicamente esa extensión opcional al cargar la
     * copia de trabajo; las cuatro hojas, estilos, figuras y fórmulas permanecen.
     */
    internal fun sanitizarPlantillaXlsx(original: ByteArray): ByteArray {
        val salida = ByteArrayOutputStream(original.size)
        ZipInputStream(ByteArrayInputStream(original)).use { entrada ->
            ZipOutputStream(salida).use { destino ->
                while (true) {
                    val item = entrada.nextEntry ?: break
                    destino.putNextEntry(ZipEntry(item.name).apply { time = item.time })
                    val bytes = entrada.readBytes()
                    if (item.name == "xl/workbook.xml") {
                        val xml = bytes.toString(Charsets.UTF_8)
                            .replace(
                                Regex(
                                    """<ext uri=\"\{7626C862-2A13-11E5-B345-FEFF819CDC9F\}\">[\s\S]*?</ext>"""
                                ),
                                ""
                            )
                        destino.write(xml.toByteArray(Charsets.UTF_8))
                    } else {
                        destino.write(bytes)
                    }
                    destino.closeEntry()
                    entrada.closeEntry()
                }
            }
        }
        return salida.toByteArray()
    }

    internal fun rellenar(workbook: XSSFWorkbook, data: FichaExportData) {
        rellenarHojaUno(workbook.getSheet("1"), data)
        rellenarHojaDos(workbook.getSheet("2"), data.calificaciones, data.ficha.responsableNombre)
        rellenarHojaTres(workbook.getSheet("3"), data.gestion, data.ficha.responsableNombre)
        rellenarHojaCuatro(workbook.getSheet("4"), data.contaminacion, data.lugaresTratamiento)
        configurarImpresionA4Horizontal(workbook)
    }

    private fun configurarImpresionA4Horizontal(workbook: XSSFWorkbook) {
        for (indice in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(indice)
            sheet.setAutobreaks(true)
            sheet.setFitToPage(true)
            sheet.setHorizontallyCenter(true)
            sheet.printSetup.apply {
                setPaperSize(PrintSetup.A4_PAPERSIZE)
                setLandscape(true)
                setFitWidth(1.toShort())
                setFitHeight(1.toShort())
            }
        }
    }

    private fun rellenarHojaUno(sheet: Sheet, data: FichaExportData) {
        val ficha = data.ficha
        sheet.texto("B2", com.ruralitos.app.domain.InstitucionVisible.limpiar(ficha.institucionSistema))
        sheet.texto("R2", ficha.unidadOperativa)
        sheet.texto("AK2", ficha.codigoUo)
        sheet.texto("AP2", ficha.areaNumero)
        sheet.texto("AT3", ficha.parroquiaCodigoLocalizacion)
        sheet.texto("AX3", ficha.cantonCodigoLocalizacion)
        sheet.texto("BB3", ficha.provinciaCodigoLocalizacion)
        sheet.texto("BF3", ficha.numeroFichaFamiliar)

        sheet.texto("B5", ficha.provincia)
        sheet.texto("L5", ficha.canton)
        sheet.texto("V5", ficha.parroquia)
        sheet.texto("AF5", ficha.sector)
        sheet.texto("AJ5", ficha.manzana)
        sheet.texto("AN5", ficha.numeroFamilia)
        sheet.texto("AR5", ficha.direccionHabitualFamilia)
        sheet.numero("BS5", ficha.latitud)
        sheet.numero("CE5", ficha.longitud)
        sheet.numero("CQ5", ficha.altitud)

        sheet.texto("B7", ficha.barrio)
        sheet.texto("L7", ficha.numeroCasa)
        sheet.texto("Q7", ficha.comunidad)
        sheet.texto("Y7", ficha.grupoCultural)
        sheet.texto("AF7", ficha.nombreApellidoJefeFamilia)
        sheet.texto("AX7", ficha.numeroTelefono)
        sheet.texto("BE7", ficha.fechaLlenado)
        sheet.texto("BN7", ficha.numeroCarpeta)
        sheet.texto("BR43", ficha.responsableNombre)
        sheet.texto("CH43", ficha.responsableCodigo)

        val filasPorGrupo = mapOf(
            GrupoEdadFamiliar.MENOR_UN_ANIO to listOf(12, 13),
            GrupoEdadFamiliar.UNO_A_CUATRO to (14..16).toList(),
            GrupoEdadFamiliar.CINCO_A_NUEVE to (17..19).toList(),
            GrupoEdadFamiliar.DIEZ_A_DIECINUEVE to (20..23).toList(),
            GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO to (24..27).toList(),
            GrupoEdadFamiliar.SESENTA_Y_CINCO_MAS to (28..31).toList()
        )
        data.miembros.groupBy { it.grupoEdad }.forEach { (grupo, miembros) ->
            val filas = filasPorGrupo[grupo].orEmpty()
            miembros.take(filas.size).forEachIndexed { index, miembro ->
                rellenarMiembro(sheet, filas[index], miembro)
            }
        }
        val columnasConTotales = listOf("AT", "AV", "AX", "AZ", "BB", "BD", "BF", "BH", "BJ", "BL", "BN")
        columnasConTotales.forEach { columna ->
            sheet.formula("${columna}32", "COUNTIF(${columna}12:${columna}31,\"X\")")
        }

        data.embarazadas.take(4).forEachIndexed { index, item ->
            val fila = 36 + index
            sheet.texto("E$fila", item.apellidosNombres)
            sheet.texto("Y$fila", item.fechaUltimaMenstruacion)
            sheet.texto("AE$fila", item.fechaProbableParto)
            sheet.entero("AK$fila", item.semanasGestacion)
            if (item.dosisDtPrimera) sheet.texto("AQ$fila", "X")
            if (item.dosisDtSegunda) sheet.texto("AU$fila", "X")
            if (item.dosisDtRefuerzo) sheet.texto("AY$fila", "X")
            sheet.entero("BC$fila", item.gestas)
            sheet.entero("BG$fila", item.partos)
            sheet.entero("BK$fila", item.abortos)
            sheet.entero("BO$fila", item.cesareas)
            sheet.texto("BS$fila", item.antecedentesPatologicosObstetricos)
        }

        data.mortalidad.take(4).forEachIndexed { index, item ->
            val fila = 43 + index
            sheet.texto("B$fila", item.nombre)
            sheet.texto("Y$fila", item.parentesco)
            sheet.entero("AE$fila", item.edadAlFallecer)
            sheet.texto("AJ$fila", item.causa)
        }
    }

    private fun rellenarMiembro(sheet: Sheet, fila: Int, item: MiembroFamiliaEntity) {
        sheet.texto("E$fila", item.apellidosNombres)
        sheet.texto("Y$fila", item.parentesco)
        sheet.texto("AE$fila", item.fechaNacimiento)
        if (ReglasGrupoEdadFamiliar.camposPermitidos(item.grupoEdad).ocupacion) {
            sheet.texto("AK$fila", item.ocupacion)
        }
        if (item.sexo == "H") sheet.texto("AT$fila", "X") else if (item.sexo == "M") sheet.texto("AV$fila", "X")
        val escolaridad = mapOf("SIN" to "AX", "BAS" to "AZ", "BACH" to "BB", "SUP" to "BD", "ESP" to "BF")
        if (ReglasGrupoEdadFamiliar.permiteEscolaridad(item.grupoEdad, item.escolaridad)) {
            escolaridad[item.escolaridad]?.let { sheet.texto("$it$fila", "X") }
        }
        item.vacunasCompletas?.let { sheet.texto("${if (it) "BH" else "BJ"}$fila", "X") }
        item.saludBucalAdecuada?.let { sheet.texto("${if (it) "BL" else "BN"}$fila", "X") }
        sheet.texto("BP$fila", item.riesgoEnfermedadDiscapacidad)
        sheet.texto("CJ$fila", item.numeroHistoriaClinica)
        sheet.texto("CS$fila", item.cedula)
    }

    private fun rellenarHojaDos(
        sheet: Sheet,
        items: List<CalificacionExport>,
        responsableFicha: String
    ) {
        val fechaColumnas = listOf("X", "AQ", "BJ", "CC")
        val valorColumnas = listOf("AN", "BG", "BZ", "CS")
        val responsableColumnas = fechaColumnas
        val filasComponentes = (7..41 step 2).toList()
        items.take(4).forEachIndexed { index, item ->
            sheet.texto("${fechaColumnas[index]}5", item.calificacion.fechaCalificacion)
            item.valores.take(18).forEachIndexed { componente, valor ->
                sheet.entero("${valorColumnas[index]}${filasComponentes[componente]}", valor)
            }
            sheet.texto("${responsableColumnas[index]}47", item.calificacion.responsable.ifBlank { responsableFicha })
        }

        val grupos = listOf(
            listOf("AN", "X", "AB", "AF", "AJ"),
            listOf("BG", "AQ", "AU", "AY", "BC"),
            listOf("BZ", "BJ", "BN", "BR", "BV"),
            listOf("CS", "CC", "CG", "CK", "CO")
        )
        grupos.forEach { (total, sinRiesgo, bajo, medio, alto) ->
            sheet.formula("${total}44", "SUM(${total}7:${total}42)")
            sheet.formula("${sinRiesgo}45", "IF(${total}44=0,\"X\",\"\")")
            sheet.formula("${bajo}45", "IF(AND(${total}44>=1,${total}44<=14),\"X\",\"\")")
            sheet.formula("${medio}45", "IF(AND(${total}44>=15,${total}44<=34),\"X\",\"\")")
            sheet.formula("${alto}45", "IF(AND(${total}44>=35,${total}44<=72),\"X\",\"\")")
        }
    }

    private fun rellenarHojaTres(
        sheet: Sheet,
        items: List<GestionRiesgoEntity>,
        responsableFicha: String
    ) {
        items.take(19).forEachIndexed { index, item ->
            val fila = 6 + index
            sheet.texto("B$fila", item.fechaAnalisis)
            sheet.entero("I$fila", item.numero)
            sheet.texto("M$fila", item.compromisoFamilia)
            sheet.texto("AI$fila", item.compromisoEquipoSalud)
            sheet.texto("BE$fila", item.fechaEvaluacion)
            when (item.cumplimiento) {
                "SI_CUMPLE" -> sheet.texto("BL$fila", "X")
                "NO_CUMPLE" -> sheet.texto("BO$fila", "X")
                "PARCIAL" -> sheet.texto("BR$fila", "X")
            }
            sheet.texto("BU$fila", item.causasIncumplimientoObservaciones)
            sheet.texto(
                "CS$fila",
                inicialesResponsable(item.responsable.ifBlank { responsableFicha })
            )
        }
    }

    private fun rellenarHojaCuatro(
        sheet: Sheet,
        contaminacion: List<ContaminacionAmbientalEntity>,
        lugares: List<LugarTratamientoEntity>
    ) {
        contaminacion.take(10).forEachIndexed { index, item ->
            val fila = 35 + index
            sheet.texto("AY$fila", item.fechaInforme)
            sheet.texto("BE$fila", item.tipoContaminanteDescripcion)
            sheet.texto("CK$fila", item.causanteContaminacion)
        }
        // La fila 45 contiene el encabezado. Los cuatro renglones útiles son
        // AY46:CS46, AY47:CS47, AY48:CS48 y AY49:CS49.
        (46..49).forEach { fila -> sheet.texto("AY$fila", "") }
        lugares.asSequence()
            .map { it.descripcion.trim() }
            .filter { it.isNotBlank() }
            .take(4)
            .forEachIndexed { index, descripcion ->
                sheet.texto("AY${46 + index}", descripcion)
            }
    }

    private fun insertarAdjuntos(
        context: Context,
        workbook: XSSFWorkbook,
        adjuntos: List<AdjuntoFichaEntity>
    ) {
        adjuntos.forEach { adjunto ->
            val uri = Uri.parse(adjunto.uri)
            val bytes = normalizarImagenParaExcel(context, uri) ?: return@forEach
            insertarImagenAdjunta(workbook, bytes, adjunto.tipo.uppercase(Locale.ROOT))
        }
    }

    internal fun insertarImagenAdjunta(
        workbook: XSSFWorkbook,
        bytes: ByteArray,
        tipoAdjunto: String,
        formato: Int = Workbook.PICTURE_TYPE_PNG
    ) {
        val anchor = workbook.creationHelper.createClientAnchor().apply {
            setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE)
        }
        when (tipoAdjunto) {
            "FAMILIOGRAMA" -> {
                anchor.setCol1(27); anchor.row1 = 2; anchor.setCol2(96); anchor.row2 = 32
            }
            "CROQUIS" -> {
                anchor.setCol1(1); anchor.row1 = 34; anchor.setCol2(48); anchor.row2 = 49
            }
            else -> return
        }
        val indice = workbook.addPicture(bytes, formato)
        val sheet = workbook.getSheet("4")
        val dibujo = sheet.createDrawingPatriarch()
        dibujo.createPicture(anchor, indice)
    }

    private fun insertarFirma(
        context: Context,
        workbook: XSSFWorkbook,
        firmaUri: String?
    ) {
        if (firmaUri.isNullOrBlank()) return
        val uri = Uri.parse(firmaUri)
        val bytes = normalizarImagenParaExcel(context, uri) ?: return
        insertarFirma(workbook, bytes)
    }

    internal fun insertarFirma(workbook: XSSFWorkbook, bytes: ByteArray) {
        val indice = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG)
        val anchor = workbook.creationHelper.createClientAnchor().apply {
            setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE)
            setCol1(90)
            row1 = 42
            setCol2(97)
            row2 = 45
        }
        val sheet = workbook.getSheet("1")
        val dibujo = sheet.createDrawingPatriarch()
        dibujo.createPicture(anchor, indice)
    }

    private fun normalizarImagenParaExcel(context: Context, uri: Uri): ByteArray? {
        val originales = leerBytes(context, uri) ?: return null
        // Las fotos de cámara pueden ser enormes: se reducen al decodificarlas para no
        // agotar la memoria del teléfono. Los recuadros del Excel son pequeños.
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(originales, 0, originales.size, limites)
        var muestreo = 1
        val mayor = maxOf(limites.outWidth, limites.outHeight)
        while (mayor / muestreo > MAX_LADO_IMAGEN_EXCEL) muestreo *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        val bitmap = BitmapFactory.decodeByteArray(originales, 0, originales.size, opciones) ?: return null
        bitmap.setHasAlpha(true)
        return try {
            ByteArrayOutputStream().use { salida ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)) return null
                salida.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private const val MAX_LADO_IMAGEN_EXCEL = 1600

    private fun leerBytes(context: Context, uri: Uri): ByteArray? = runCatching {
        when {
            uri.scheme == "file" -> uri.path?.let { File(it).readBytes() }
            uri.scheme.isNullOrBlank() -> uri.path?.let { File(it).readBytes() }
            else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
    }.getOrNull()

    internal fun inicialesResponsable(nombreCompleto: String): String {
        val conectores = setOf("DE", "DEL", "LA", "LAS", "LOS", "Y")
        val partes = nombreCompleto.trim()
            .split(Regex("\\s+"))
            .map { it.trim('.', ',', ';', ':') }
            .filter { it.isNotBlank() && it.uppercase(Locale.ROOT) !in conectores }
        if (partes.isEmpty()) return ""
        val primera = partes.first().first().uppercaseChar()
        val ultima = partes.last().first().uppercaseChar()
        return if (partes.size == 1) "$primera." else "$primera.$ultima."
    }
}

private fun Sheet.texto(direccion: String, valor: String?) {
    val referencia = CellReference(direccion)
    val fila = getRow(referencia.row) ?: createRow(referencia.row)
    val celda = fila.getCell(referencia.col.toInt()) ?: fila.createCell(referencia.col.toInt())
    if (valor.isNullOrEmpty()) {
        // Conserva el estilo de la plantilla sin crear una referencia a la
        // cadena compartida vacía (algunos visores mostraban su índice, 196).
        celda.setCellType(CellType.BLANK)
    } else {
        celda.setCellValue(valor)
    }
}

private fun Sheet.numero(direccion: String, valor: Double?) {
    if (valor == null) return
    val referencia = CellReference(direccion)
    val fila = getRow(referencia.row) ?: createRow(referencia.row)
    val celda = fila.getCell(referencia.col.toInt()) ?: fila.createCell(referencia.col.toInt())
    celda.setCellValue(valor)
}

private fun Sheet.entero(direccion: String, valor: Int?) = numero(direccion, valor?.toDouble())

private fun Sheet.formula(direccion: String, formula: String) {
    val referencia = CellReference(direccion)
    val fila = getRow(referencia.row) ?: createRow(referencia.row)
    val celda = fila.getCell(referencia.col.toInt()) ?: fila.createCell(referencia.col.toInt())
    celda.cellFormula = formula
}
