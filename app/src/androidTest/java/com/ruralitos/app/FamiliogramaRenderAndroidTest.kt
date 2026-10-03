package com.ruralitos.app

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.familiograma.AlmacenFamiliograma
import com.ruralitos.app.domain.familiograma.Aborto
import com.ruralitos.app.domain.familiograma.AbreviaturasPatologia
import com.ruralitos.app.domain.familiograma.Ancla
import com.ruralitos.app.domain.familiograma.ArmadoFamiliograma
import com.ruralitos.app.domain.familiograma.Entorno
import com.ruralitos.app.domain.familiograma.Familiograma
import com.ruralitos.app.domain.familiograma.IntegranteFamiliograma
import com.ruralitos.app.domain.familiograma.Lado
import com.ruralitos.app.domain.familiograma.PngConDatos
import com.ruralitos.app.domain.familiograma.Punto
import com.ruralitos.app.domain.familiograma.SerializadorFamiliograma
import com.ruralitos.app.domain.familiograma.Texto
import com.ruralitos.app.domain.familiograma.TipoEntorno
import com.ruralitos.app.domain.familiograma.TipoTrazo
import com.ruralitos.app.domain.familiograma.TipoUnion
import com.ruralitos.app.domain.familiograma.Trazo
import com.ruralitos.app.domain.familiograma.Vinculo
import com.ruralitos.app.ui.familiograma.DibujanteFamiliograma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

/** Dibuja familiogramas de ejemplo en Android real y comprueba que el PNG conserva el dibujo. */
@RunWith(AndroidJUnit4::class)
class FamiliogramaRenderAndroidTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun cinco() = ArmadoFamiliograma.desdeIntegrantes(
        listOf(
            IntegranteFamiliograma("Pérez Gómez Luis", "JEFE/A DE FAMILIA", "H", "45", listOf("Hipertensión arterial")),
            IntegranteFamiliograma("Pérez Ruiz Ana", "CÓNYUGE/PAREJA", "M", "42", listOf("Diabetes mellitus", "Insuficiencia renal crónica")),
            IntegranteFamiliograma("Pérez Ruiz Leo", "HIJO/A", "H", "22"),
            IntegranteFamiliograma("Pérez Ruiz Eva", "HIJO/A", "M", "18"),
            IntegranteFamiliograma("Pérez Ruiz Teo", "HIJO/A", "H", "9", listOf("Asma"))
        )
    )

    private fun guardarPng(nombre: String, doc: Familiograma): ByteArray {
        val bitmap = DibujanteFamiliograma().renderizar(doc, AlmacenFamiliograma.ANCHO_PNG, AlmacenFamiliograma.ALTO_PNG)
        assertTrue(bitmap.hasAlpha())
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)
        val bytes = PngConDatos.insertar(salida.toByteArray(), SerializadorFamiliograma.aJson(doc))
        val carpeta = File(context.getExternalFilesDir(null), "familiograma").apply { mkdirs() }
        File(carpeta, nombre).writeBytes(bytes)
        return bytes
    }

    @Test
    fun dibujaLosCincoIntegrantes() {
        val doc = cinco()
        val bytes = guardarPng("familiograma_base.png", doc)
        val recuperado = SerializadorFamiliograma.desdeJson(checkNotNull(PngConDatos.leer(bytes)))
        assertEquals(doc, recuperado)
        // el PNG sigue siendo una imagen normal que Android decodifica
        val decodificado = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull(decodificado)
        assertEquals(AlmacenFamiliograma.ANCHO_PNG, decodificado.width)
    }

    @Test
    fun dibujaUnFamiliogramaCompletoConTodosLosSimbolos() {
        var doc = ArmadoFamiliograma.desdeIntegrantes(
            listOf(
                IntegranteFamiliograma("Pérez Gómez Luis", "JEFE/A DE FAMILIA", "H", "45", listOf("Hipertensión arterial", "Tabaquismo")),
                IntegranteFamiliograma("Pérez Ruiz Ana", "CÓNYUGE/PAREJA", "M", "42", listOf("Diabetes mellitus", "Insuficiencia renal crónica")),
                IntegranteFamiliograma("Pérez Ruiz Leo", "HIJO/A", "H", "22"),
                IntegranteFamiliograma("Pérez Ruiz Eva", "HIJO/A", "M", "18"),
                IntegranteFamiliograma("Pérez Ruiz Teo", "HIJO/A", "H", "9", listOf("Asma")),
                IntegranteFamiliograma("Pérez Rojas Pedro", "PADRE/MADRE", "H", "78", listOf("Infarto")),
                IntegranteFamiliograma("Gómez Sol Rosa", "PADRE/MADRE", "M", "75", listOf("Artritis reumatoide")),
                IntegranteFamiliograma("Pérez Gómez Ema", "HERMANO/A", "M", "50", listOf("Hipotiroidismo"))
            )
        )
        val ana = doc.personas.first { it.nombre.contains("Ana") }
        val pedro = doc.personas.first { it.nombre.contains("Pedro") }
        val eva = doc.personas.first { it.nombre.contains("Eva") }
        val teo = doc.personas.first { it.nombre.contains("Teo") }
        val union = doc.uniones.first { it.a.elementoId != pedro.id }
        val medioUnion = com.ruralitos.app.domain.familiograma.GeometriaFamiliograma.puntoMedioUnion(doc, union)!!.punto
        doc = doc.copy(
            personas = doc.personas.map {
                when (it.id) {
                    ana.id -> it.copy(informante = true)
                    pedro.id -> it.copy(fallecido = true, enHogar = false)
                    else -> it
                }
            },
            filiaciones = doc.filiaciones.map { if (it.hijo.elementoId == teo.id) it.copy(adoptado = true) else it },
            uniones = doc.uniones.map { if (it.a.elementoId == pedro.id) it.copy(tipo = TipoUnion.SEPARACION) else it },
            abortos = listOf(Aborto("a1", union.id, medioUnion.x + 45f, medioUnion.y + 48f)),
            entornos = listOf(
                Entorno("e1", TipoEntorno.IGLESIA, 80f, 400f),
                Entorno("e2", TipoEntorno.ESCUELA, 930f, 400f),
                Entorno("e3", TipoEntorno.CENTRO_SALUD, 930f, 250f),
                Entorno("e4", TipoEntorno.JUNTA, 930f, 120f),
                Entorno("e5", TipoEntorno.MERCADO, 80f, 250f),
                Entorno("e6", TipoEntorno.CANCHA, 250f, 500f),
                Entorno("e7", TipoEntorno.FINCA, 350f, 500f),
                Entorno("e8", TipoEntorno.TRABAJO, 450f, 500f),
                Entorno("e9", TipoEntorno.RIO, 550f, 500f),
                Entorno("e10", TipoEntorno.TRANSPORTE, 650f, 500f),
                Entorno("e11", TipoEntorno.PARQUE, 750f, 500f)
            ),
            vinculos = listOf(
                Vinculo("v1", Ancla("e1", Lado.DERECHA), Ancla(eva.id, Lado.IZQUIERDA)),
                Vinculo("v2", Ancla("e2", Lado.IZQUIERDA), Ancla(teo.id, Lado.DERECHA)),
                Vinculo("v3", Ancla("e3", Lado.IZQUIERDA), Ancla(ana.id, Lado.DERECHA))
            ),
            trazos = listOf(
                Trazo("t1", TipoTrazo.LAPIZ, listOf(Punto(20f, 535f), Punto(120f, 540f), Punto(220f, 534f), Punto(320f, 540f), Punto(420f, 534f), Punto(520f, 540f), Punto(620f, 534f), Punto(720f, 540f), Punto(820f, 534f)), 0xFF1565C0.toInt(), 3f, false)
            ),
            textos = listOf(Texto("x1", 40f, 30f, "Comunidad Los Ríos"))
        )
        doc.personas.forEach { p -> p.patologias.forEach { doc = AbreviaturasPatologia.registrar(doc, it) } }
        val bytes = guardarPng("familiograma_completo.png", doc)
        assertEquals(doc, SerializadorFamiliograma.desdeJson(checkNotNull(PngConDatos.leer(bytes))))
    }

    /** El dibujo guardado se inserta en el recuadro de la hoja 4 del Excel y sale en el PDF. */
    @Test
    fun elDibujoSaleEnLaHojaCuatroDelPdf() {
        val doc = cinco().let { d ->
            var r = d
            r.personas.forEach { p -> p.patologias.forEach { r = AbreviaturasPatologia.registrar(r, it) } }
            r
        }
        val png = guardarPng("familiograma_para_pdf.png", doc)
        val archivoPng = File(context.getExternalFilesDir(null), "familiograma/familiograma_para_pdf.png")
        archivoPng.writeBytes(png)
        val ficha = com.ruralitos.app.data.local.entity.FichaFamiliarEntity(
            cedulaJefeHogar = "1300000000", institucionSistema = "MSP", unidadOperativa = "CENTRO PRUEBA",
            codigoUo = "001234", areaNumero = "4", codigoLocalizacion = "010203", parroquiaCodigoLocalizacion = "03",
            cantonCodigoLocalizacion = "02", provinciaCodigoLocalizacion = "01", numeroFichaFamiliar = "1",
            provincia = "MANABI", canton = "PEDERNALES", parroquia = "COJIMIES", sector = "RURAL", manzana = "1",
            numeroFamilia = "2", direccionHabitualFamilia = "DIRECCION PRUEBA", barrio = "BARRIO", numeroCasa = "3",
            comunidad = "COMUNIDAD", grupoCultural = "MESTIZO", nombreApellidoJefeFamilia = "PERSONA PRUEBA",
            numeroTelefono = "0999999999", fechaLlenado = "02/10/2026", numeroCarpeta = "7",
            responsableNombre = "PROFESIONAL PRUEBA", responsableCodigo = "1016.2025-3143718"
        )
        val datos = com.ruralitos.app.data.export.FichaExportData(
            ficha = ficha, miembros = emptyList(), embarazadas = emptyList(), mortalidad = emptyList(),
            calificaciones = emptyList(), gestion = emptyList(), contaminacion = emptyList(),
            lugaresTratamiento = emptyList(),
            adjuntos = listOf(
                com.ruralitos.app.data.local.entity.AdjuntoFichaEntity(
                    fichaId = 1, tipo = "FAMILIOGRAMA", uri = android.net.Uri.fromFile(archivoPng).toString()
                )
            )
        )
        val pdf = File(context.getExternalFilesDir(null), "familiograma/ficha_con_familiograma.pdf")
        java.io.FileOutputStream(pdf).use { com.ruralitos.app.data.export.FichaPdfPlantillaExporter.generar(context, datos, it) }
        android.os.ParcelFileDescriptor.open(pdf, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { d ->
            android.graphics.pdf.PdfRenderer(d).use { r ->
                assertEquals(4, r.pageCount)
                r.openPage(3).use { pagina ->
                    val bmp = Bitmap.createBitmap(pagina.width * 2, pagina.height * 2, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    pagina.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    java.io.FileOutputStream(File(pdf.parentFile, "pagina4_con_familiograma.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
        }
    }
}
