package com.ruralitos.app.data.export

import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Compatibilidad XML para las distintas implementaciones incluidas por Android. */
internal object ExcelXmlSeguro {
    // Se usa el URI literal porque la dependencia StAX requerida por XLSX
    // expone una versión mínima de XMLConstants sin esta constante pública.
    private const val PROCESAMIENTO_SEGURO =
        "http://javax.xml.XMLConstants/feature/secure-processing"
    private const val ACCESO_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD"
    private const val ACCESO_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema"
    private const val ACCESO_ESTILOS = "http://javax.xml.XMLConstants/property/accessExternalStylesheet"

    fun leer(bytes: ByteArray): Document {
        val fabrica = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
            configurarFeature(PROCESAMIENTO_SEGURO, true)
            configurarFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            configurarFeature("http://xml.org/sax/features/external-general-entities", false)
            configurarFeature("http://xml.org/sax/features/external-parameter-entities", false)
            runCatching { setAttribute(ACCESO_DTD, "") }
            runCatching { setAttribute(ACCESO_SCHEMA, "") }
        }
        return fabrica.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    }

    fun serializar(documento: Document): ByteArray = ByteArrayOutputStream().use { salida ->
        val fabrica = TransformerFactory.newInstance().apply {
            runCatching { setFeature(PROCESAMIENTO_SEGURO, true) }
            runCatching { setAttribute(ACCESO_DTD, "") }
            runCatching { setAttribute(ACCESO_ESTILOS, "") }
        }
        fabrica.newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            transform(DOMSource(documento), StreamResult(salida))
        }
        salida.toByteArray()
    }

    private fun DocumentBuilderFactory.configurarFeature(nombre: String, valor: Boolean) {
        // Android usa diferentes proveedores XML según la versión. Una opción de
        // endurecimiento no soportada no debe impedir exportar una plantilla local confiable.
        runCatching { setFeature(nombre, valor) }
    }
}
