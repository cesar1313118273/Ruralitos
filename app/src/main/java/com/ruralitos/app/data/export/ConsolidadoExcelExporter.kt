package com.ruralitos.app.data.export

import android.content.Context
import android.net.Uri
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.*
import com.ruralitos.app.domain.CatalogoCie10
import com.ruralitos.app.domain.DispensarizacionAutomatica
import com.ruralitos.app.domain.GrupoDispensarizacion
import com.ruralitos.app.domain.AgrupacionRegistroComunitario
import com.ruralitos.app.domain.CalculadorRegistroComunitario
import com.ruralitos.app.domain.ColumnaRegistroComunitario
import kotlinx.coroutines.flow.first
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

enum class AlcanceConsolidado { FICHA, TERRITORIO, EAIS, GENERAL }

data class FiltroConsolidado(
    val alcance: AlcanceConsolidado,
    val id: String = "",
    val etiqueta: String = "",
    val ids: Set<String> = if (id.isBlank()) emptySet() else setOf(id)
)

data class ResumenConsolidadoAnalitico(
    val fichas: Int, val personas: Int, val mujeres: Int, val hombres: Int,
    val embarazadas: Int, val discapacidades: Int,
    val gruposDispensarizacion: List<Pair<String, Int>>,
    val morbilidad: List<Pair<String, Int>>,
    val gruposEdad: List<Pair<String, Int>>
)

private data class DatosConsolidado(
    val fichas: List<FichaFamiliarEntity>, val miembros: List<MiembroFamiliaEntity>,
    val embarazadas: List<EmbarazadaEntity>, val mortalidad: List<MortalidadFamiliarEntity>,
    val calificaciones: List<CalificacionRiesgoEntity>, val valores: List<ValorRiesgoEntity>,
    val eais: Map<String, EaisSalaEntity>, val territorios: Map<String, TerritorioSalaEntity>
)

private data class FilaBase(val valores: List<Any?>)

/** Calcula indicadores y filas BASE para el registro general de dos hojas. */
object ConsolidadoExcelExporter {
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"

    internal suspend fun contenidoRegistroGeneral(
        context: Context,
        filtro: FiltroConsolidado,
        agrupaciones: List<AgrupacionRegistroComunitario>
    ): Pair<List<ColumnaRegistroComunitario>, List<List<Any?>>> {
        val datos = cargar(context, filtro)
        require(datos.fichas.isNotEmpty()) { "No hay fichas en la selección." }
        val ids = datos.fichas.mapTo(mutableSetOf()) { it.id }
        require(agrupaciones.flatMapTo(mutableSetOf()) { it.fichaIds } == ids) {
            "La selección cambió. Vuelve a revisar las familias antes de exportar."
        }
        val columnas = CalculadorRegistroComunitario.calcular(agrupaciones, datos.miembros, datos.embarazadas)
        val filas = construirFilas(datos)
        require(filas.size <= 4999) { "La plantilla admite hasta 4.999 personas por archivo." }
        return columnas to filas.map { it.valores }
    }

    suspend fun resumir(context: Context, filtro: FiltroConsolidado): ResumenConsolidadoAnalitico {
        val d = cargar(context, filtro); val embarazos = d.embarazadas.groupBy { it.fichaId }
        val resultados = d.miembros.map { m -> DispensarizacionAutomatica.clasificar(m, DispensarizacionAutomatica.buscarEmbarazo(m, embarazos[m.fichaId].orEmpty())) }
        val diagnosticos = d.miembros.flatMap { CatalogoCie10.decodificar(it.comorbilidadesCie10Json) }
        val morbilidad = (diagnosticos.groupingBy { it.etiqueta }.eachCount() + mapOf(
            "I10 · Hipertensión arterial" to d.miembros.count { it.hipertensionArterial == true },
            "E11 · Diabetes mellitus" to d.miembros.count { it.diabetesMellitus == true },
            "A15 · Tuberculosis" to d.miembros.count { it.tuberculosis == true }
        )).entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(8).map { it.key to it.value }
        return ResumenConsolidadoAnalitico(
            d.fichas.size, d.miembros.size, d.miembros.count { sexo(it) == 2 }, d.miembros.count { sexo(it) == 1 },
            d.embarazadas.size, d.miembros.count { discapacidad(it) != 0 },
            GrupoDispensarizacion.entries.filter { it != GrupoDispensarizacion.PENDIENTE }.map { g -> g.titulo to resultados.count { it.grupo == g } },
            morbilidad, d.miembros.groupingBy { it.grupoEdad.ifBlank { "Sin grupo" } }.eachCount().entries.sortedByDescending { it.value }.map { it.key to it.value }
        )
    }

    internal fun actualizarBaseParaRegistroGeneral(bytes: ByteArray, filas: List<List<Any?>>): ByteArray =
        actualizarBase(bytes, filas.map(::FilaBase))

    internal fun categoriasCronicasPrueba(
        diagnosticos: List<com.ruralitos.app.domain.DiagnosticoCie10>
    ): List<Int> = diagnosticos.take(2).map(::categoriaCronica)

    private suspend fun cargar(context: Context, filtro: FiltroConsolidado): DatosConsolidado {
        val db = RuralitosDatabase.obtenerBaseDatos(context); val dao = db.fichaContenidoDao()
        val seleccion = filtro.ids.ifEmpty { filtro.id.takeIf(String::isNotBlank)?.let(::setOf).orEmpty() }
        val fichas = db.fichaFamiliarDao().listarFichas().first().filter { f -> when (filtro.alcance) {
            AlcanceConsolidado.FICHA -> f.id.toString() in seleccion
            AlcanceConsolidado.TERRITORIO -> f.territorioId in seleccion
            AlcanceConsolidado.EAIS -> f.eaisId in seleccion
            AlcanceConsolidado.GENERAL -> true
        } }
        val ids = fichas.map { it.id }.toSet(); val eais = db.salaDao().listarEaisHistoricos()
        return DatosConsolidado(
            fichas, dao.listarTodosMiembros().first().filter { it.fichaId in ids }, dao.listarTodasEmbarazadas().first().filter { it.fichaId in ids },
            fichas.flatMap { dao.listarMortalidad(it.id).first() }, dao.listarTodasCalificaciones().first().filter { it.fichaId in ids }, dao.listarTodosValoresRiesgo().first(),
            eais.associateBy { it.id }, db.salaDao().listarTerritoriosHistoricos().associateBy { it.id }
        )
    }

    private fun construirFilas(d: DatosConsolidado): List<FilaBase> {
        val fichas = d.fichas.associateBy { it.id }; val embarazos = d.embarazadas.groupBy { it.fichaId }; val muertes = d.mortalidad.groupBy { it.fichaId }; val califs = d.calificaciones.groupBy { it.fichaId }
        val miembrosPorFicha = d.miembros.groupBy { it.fichaId }
        val valoresPorCalificacion = d.valores.groupBy { it.calificacionId }
        return d.miembros.map { m ->
            val f = fichas.getValue(m.fichaId); val miembrosFicha = miembrosPorFicha[f.id].orEmpty(); val primero = miembrosFicha.firstOrNull()?.id == m.id
            val emb = DispensarizacionAutomatica.buscarEmbarazo(m, embarazos[f.id].orEmpty()); val cal = califs[f.id].orEmpty().maxByOrNull { it.id }
            val vals = cal?.let { c -> valoresPorCalificacion[c.id] }.orEmpty(); val dis = DispensarizacionAutomatica.clasificar(m, emb)
            val muerte = muertes[f.id].orEmpty().firstOrNull().takeIf { primero }; val diagnosticos = CatalogoCie10.decodificar(m.comorbilidadesCie10Json); val cronicas = categoriasCronicas(m, diagnosticos)
            val (apellidos, nombres) = separarNombre(m.apellidosNombres); val hoy = Date()
            FilaBase(listOf(
                f.unidadOperativa, d.eais[f.eaisId]?.nombre.orEmpty(), f.numeroFichaFamiliar, f.provincia, f.canton, f.parroquia, f.barrio.ifBlank { f.comunidad }, f.direccionHabitualFamilia,
                f.responsableNombre, f.responsableNombre, "", "", fechaExcel(f.fechaLlenado), nombres, apellidos, sexo(m), fechaExcel(m.fechaNacimiento), f.nombreApellidoJefeFamilia,
                parentesco(m.parentesco), ocupacion(m.ocupacion), f.numeroTelefono, escolaridad(m.escolaridad), booleanCodigo(m.vacunasCompletas), booleanCodigo(m.saludBucalAdecuada), m.cedula,
                if (emb != null) 1 else if (sexo(m) == 2) 2 else 0, emb?.fechaUltimaMenstruacion?.let(::fechaExcel), riesgoObstetrico(emb?.riesgoObstetrico), emb?.semanasGestacion,
                emb?.fechaProbableParto?.let(::fechaExcel), if (muerte != null) 1 else if (primero) 2 else null, muerte?.edadAlFallecer, muerte?.causa,
                if (primero) tipoRiesgo(vals) else null, if (primero) calificacion(cal?.nivel) else null,
                when { m.cuidadosPaliativos == true -> 5; dis.grupo == GrupoDispensarizacion.IV -> 4; dis.grupo == GrupoDispensarizacion.III -> 3; dis.grupo == GrupoDispensarizacion.II -> 2; dis.grupo == GrupoDispensarizacion.I -> 1; else -> null },
                vulnerable(m, emb), prioritario(m, emb), discapacidad(m).takeIf { it > 0 }, m.porcentajeDiscapacidad?.div(100.0), if (primero) estructura(miembrosFicha) else null,
                cronicas.getOrNull(0), cronicas.getOrNull(1), otrasEnfermedades(m, diagnosticos), edad(m.fechaNacimiento), emb?.fechaUltimaMenstruacion?.let { diasDesde(it, hoy) }, fechaExcel(SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(hoy))
            ))
        }
    }

    private fun actualizarBase(bytes: ByteArray, filas: List<FilaBase>): ByteArray {
        val doc = leerXml(bytes); val sheetData = doc.getElementsByTagNameNS(NS, "sheetData").item(0) as Element
        val rows = (0 until sheetData.childNodes.length).mapNotNull { sheetData.childNodes.item(it) as? Element }.associateBy { it.getAttribute("r").toIntOrNull() }
        filas.forEachIndexed { i, fila ->
            val numero = i + 2
            val row = rows[numero] ?: error("La plantilla no contiene la fila $numero")
            val celdasFila = celdasDeFila(row)
            fila.valores.forEachIndexed { columna, valor ->
                celdasFila[letra(columna + 1) + numero]?.let { escribir(doc, it, valor) }
            }
        }
        // La plantilla reserva 4.999 filas. Se recorren una sola vez y solo se
        // limpian celdas que realmente contienen datos; antes cada búsqueda
        // volvía a escanear toda la fila y hacía la descarga muy lenta.
        rows.asSequence()
            .filter { (numero, _) -> numero != null && numero >= filas.size + 2 }
            .forEach { (_, row) ->
                celdasDeFila(row).values.forEach { celda ->
                    if (numeroColumna(celda.getAttribute("r")) <= 47 &&
                        (celda.hasChildNodes() || celda.hasAttribute("t"))
                    ) escribir(doc, celda, null)
                }
            }
        return serializar(doc)
    }

    private fun celdasDeFila(row:Element)=buildMap { val n=row.getElementsByTagNameNS(NS,"c");for(i in 0 until n.length)(n.item(i) as? Element)?.let{put(it.getAttribute("r"),it)} }
    private fun numeroColumna(referencia:String):Int{var total=0;referencia.takeWhile{it.isLetter()}.forEach{total=total*26+(it.uppercaseChar()-'A'+1)};return total}
    private fun celda(row:Element,ref:String):Element?{val n=row.getElementsByTagNameNS(NS,"c");for(i in 0 until n.length)(n.item(i) as? Element)?.takeIf{it.getAttribute("r")==ref}?.let{return it};return null}
    private fun escribir(d:Document,c:Element,v:Any?){while(c.firstChild!=null)c.removeChild(c.firstChild);when(v){null,""->c.removeAttribute("t");is Number->{c.removeAttribute("t");c.appendChild(d.createElementNS(NS,"v").apply{textContent=v.toString()})};else->{c.setAttribute("t","inlineStr");c.appendChild(d.createElementNS(NS,"is").apply{appendChild(d.createElementNS(NS,"t").apply{setAttributeNS(XMLConstants.XML_NS_URI,"xml:space","preserve");textContent=v.toString()})})}}}
    private fun leerXml(b:ByteArray):Document=ExcelXmlSeguro.leer(b)
    private fun serializar(d:Document):ByteArray=ExcelXmlSeguro.serializar(d)
    private fun letra(n:Int):String{var x=n;val s=StringBuilder();while(x>0){x--;s.append(('A'.code+x%26).toChar());x/=26};return s.reverse().toString()}

    private fun sexo(m:MiembroFamiliaEntity)=if(normal(m.sexo) in setOf("M","F","MUJER","FEMENINO"))2 else 1
    private fun booleanCodigo(v:Boolean?)=when(v){true->1;false->2;null->null}
    private fun escolaridad(v:String)=when(normal(v)){"SIN","SIN ESCOLARIDAD"->1;"BAS","BASICA"->2;"BACH","BACHILLER"->3;"SUP","SUPERIOR"->4;"ESP","ESPECIALIDAD"->5;else->null}
    private fun ocupacion(v:String):Int?{val n=normal(v);return when{"ESTUD" in n->5;"AMA" in n||"HOGAR" in n->3;"DESEMP" in n->4;"INDEPEND" in n->2;n.isBlank()||"NING" in n->6;else->1}}
    private fun parentesco(v:String):Int{val n=normal(v);return when{"JEFE" in n->8;"CONY" in n||"ESPOS" in n->1;"HIJ" in n->2;"NIET" in n->3;"PRIM" in n->4;"ABUEL" in n->5;"MADRE" in n->9;"PADRE" in n->10;"EQUIV" in n->7;else->6}}
    private fun discapacidad(m:MiembroFamiliaEntity)=when{m.discapacidadAuditiva==true->1;m.discapacidadFisica==true->2;m.discapacidadVisual==true->3;m.discapacidadLenguaje==true->4;m.discapacidadIntelectual==true->5;m.discapacidadPsicosocial==true->6;else->0}
    private fun riesgoObstetrico(v:String?)=when(normal(v.orEmpty())){"SIN_RIESGO"->1;"BAJO"->2;"ALTO"->3;"MUY_ALTO"->4;else->null}
    private fun calificacion(v:String?)=when(normal(v.orEmpty())){"SIN_RIESGO"->1;"BAJO"->2;"MEDIO"->3;"ALTO"->4;else->null}
    private fun tipoRiesgo(v:List<ValorRiesgoEntity>):Int?{val p=v.filter{it.valor>0}.map{it.componente};return when{p.any{it in 1..6}->1;p.any{it in 7..11}->2;p.any{it>=12}->3;else->null}}
    private fun vulnerable(m:MiembroFamiliaEntity,e:EmbarazadaEntity?)=when{e!=null&&riesgoObstetrico(e.riesgoObstetrico) in 3..4->1;m.estadoNutricional==DispensarizacionAutomatica.NUTRICION_DESNUTRICION_AGUDA&&edad(m.fechaNacimiento)<2->2;m.necesitaAyudaTecnica==true->3;m.enfermedadCronicaDescompensada==true->4;m.cuidadosPaliativos==true->5;m.riesgoGenetico==true->6;m.victimaViolencia==true->7;else->0}
    private fun prioritario(m:MiembroFamiliaEntity,e:EmbarazadaEntity?)=when{edad(m.fechaNacimiento)>=65->1;e!=null->2;edad(m.fechaNacimiento)<5&&m.estadoNutricional!=DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION->4;edad(m.fechaNacimiento)<5->3;m.vacunasCompletas==false->5;discapacidad(m)>0->6;m.problemaSaludMental==true->7;m.privadoLibertad==true->8;m.enfermedadCronica==true||m.hipertensionArterial==true||m.diabetesMellitus==true->9;m.tuberculosis==true->10;m.vih==true->11;m.victimaViolencia==true->12;else->0}
    private fun categoriasCronicas(m:MiembroFamiliaEntity,d:List<com.ruralitos.app.domain.DiagnosticoCie10>)=
        d.take(2).map(::categoriaCronica).ifEmpty { listOfNotNull(1.takeIf { m.diabetesMellitus == true }, 2.takeIf { m.hipertensionArterial == true }).take(2) }
    private fun categoriaCronica(d:com.ruralitos.app.domain.DiagnosticoCie10):Int { val c=normal(d.codigo);val n=normal(d.descripcion);return when{c.matches(Regex("E1[0-4].*"))||"DIABET" in n->1;c.matches(Regex("I1[0-5].*"))||"HIPERTENS" in n->2;c.startsWith("I2")||c.startsWith("I3")||c.startsWith("I4")||c.startsWith("I5")||"CARDIOPAT" in n->3;c.startsWith("J44")||"EPOC" in n||"OBSTRUCTIVA CRONICA" in n->4;c.startsWith("C")||"CANCER" in n||"NEOPLASIA MALIGNA" in n->5;else->6}}
    private fun otrasEnfermedades(m:MiembroFamiliaEntity,d:List<com.ruralitos.app.domain.DiagnosticoCie10>)=
        (d.drop(2).joinToString("; "){"${it.codigo} ${it.descripcion}"}+listOfNotNull("TUBERCULOSIS".takeIf{m.tuberculosis==true&&d.none{dx->dx.codigo.startsWith("A1")}},"VIH".takeIf{m.vih==true&&d.none{dx->dx.codigo.startsWith("B2")}},"SALUD MENTAL".takeIf{m.problemaSaludMental==true&&d.none{dx->dx.codigo.startsWith("F")}}).joinToString("; ").let{if(it.isBlank())"" else "; $it"}).trim(';',' ')
    private fun estructura(ms:List<MiembroFamiliaEntity>):Int{if(ms.size==1)return 4;val p=ms.map{normal(it.parentesco)};return when{p.any{"EQUIV" in it}->5;p.any{listOf("ABUEL","NIET","PRIM","TIO","SOBR").any{q->q in it}}->2;else->1}}
    private fun separarNombre(v:String):Pair<String,String>{val p=v.trim().split(Regex("\\s+")).filter(String::isNotBlank);return when{p.size>=3->p.take(2).joinToString(" ") to p.drop(2).joinToString(" ");p.size==2->p.first() to p.last();else->"" to v.trim()}}
    private fun edad(v:String)=runCatching{val f=SimpleDateFormat("dd/MM/yyyy",Locale.ROOT).apply{isLenient=false}.parse(v)!!;((Date().time-f.time)/31557600000.0).toInt().coerceAtLeast(0)}.getOrDefault(0)
    private fun diasDesde(v:String,hoy:Date)=runCatching{val f=SimpleDateFormat("dd/MM/yyyy",Locale.ROOT).apply{isLenient=false}.parse(v)!!;((hoy.time-f.time)/86400000L).toInt()}.getOrNull()
    private fun fechaExcel(v:String):Double?=runCatching{val f=SimpleDateFormat("dd/MM/yyyy",Locale.ROOT).apply{isLenient=false}.parse(v)!!;f.time/86400000.0+25569.0}.getOrNull()
    private fun normal(v:String)=Normalizer.normalize(v,Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"),"").uppercase(Locale.ROOT).trim()
}
