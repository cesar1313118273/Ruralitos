package com.ruralitos.app.data.export

import com.ruralitos.app.domain.CalculadorRegistroComunitario
import com.ruralitos.app.domain.ColumnaRegistroComunitario
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.XMLConstants

object RegistroComunitarioExcelExporter {
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"

    internal fun actualizarHoja(
        bytes: ByteArray,
        columnas: List<ColumnaRegistroComunitario>
    ): ByteArray {
        require(columnas.isNotEmpty()) { "Selecciona al menos una ficha, barrio o EAIS." }
        require(columnas.size <= CalculadorRegistroComunitario.MAXIMO_COLUMNAS) {
            "La plantilla admite hasta ${CalculadorRegistroComunitario.MAXIMO_COLUMNAS} columnas."
        }
        val documento = leerXml(bytes)
        val celdas = documento.getElementsByTagNameNS(NS, "c")
        val porReferencia = buildMap<String, Element> {
            for (indice in 0 until celdas.length) {
                val celda = celdas.item(indice) as? Element ?: continue
                put(celda.getAttribute("r"), celda)
            }
        }
        val filasNumericas = CalculadorRegistroComunitario.filasNumericas
        val columnasActivas = columnas.size
        for (indice in 0 until CalculadorRegistroComunitario.MAXIMO_COLUMNAS) {
            val letra = letraColumna(indice + 2)
            val encabezado = porReferencia["${letra}1"]
                ?: error("La plantilla no contiene la celda ${letra}1.")
            if (indice < columnasActivas) escribirTexto(documento, encabezado, columnas[indice].titulo)
            else limpiarContenido(encabezado)
            filasNumericas.forEach { fila ->
                val celda = porReferencia["$letra$fila"]
                    ?: error("La plantilla no contiene la celda $letra$fila.")
                if (indice < columnasActivas) escribirNumero(documento, celda, columnas[indice].valor(fila))
                else limpiarContenido(celda)
            }
        }
        return serializar(documento)
    }

    private fun escribirTexto(documento: Document, celda: Element, texto: String) {
        limpiarContenido(celda)
        celda.setAttribute("t", "inlineStr")
        val isElement = documento.createElementNS(NS, "is")
        val tElement = documento.createElementNS(NS, "t")
        tElement.setAttributeNS(XMLConstants.XML_NS_URI, "xml:space", "preserve")
        tElement.textContent = texto
        isElement.appendChild(tElement)
        celda.appendChild(isElement)
    }

    private fun escribirNumero(documento: Document, celda: Element, valor: Int) {
        limpiarContenido(celda)
        celda.removeAttribute("t")
        documento.createElementNS(NS, "v").also {
            it.textContent = valor.toString()
            celda.appendChild(it)
        }
    }

    private fun limpiarContenido(celda: Element) {
        while (celda.firstChild != null) celda.removeChild(celda.firstChild)
        celda.removeAttribute("t")
    }

    private fun leerXml(bytes: ByteArray): Document = ExcelXmlSeguro.leer(bytes)

    private fun serializar(documento: Document): ByteArray = ExcelXmlSeguro.serializar(documento)

    private fun letraColumna(numero: Int): String {
        var restante = numero
        val resultado = StringBuilder()
        while (restante > 0) {
            restante--
            resultado.append(('A'.code + restante % 26).toChar())
            restante /= 26
        }
        return resultado.reverse().toString()
    }
}
