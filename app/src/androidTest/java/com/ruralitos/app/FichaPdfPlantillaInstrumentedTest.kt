package com.ruralitos.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.export.CalificacionExport
import com.ruralitos.app.data.export.FichaExportData
import com.ruralitos.app.data.export.FichaPdfPlantillaExporter
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.GestionRiesgoEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.domain.GrupoEdadFamiliar
import com.ruralitos.app.ui.screens.guardarPngConTransparenciaVerificada
import com.ruralitos.app.ui.screens.procesarFondoClaro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class FichaPdfPlantillaInstrumentedTest {
    @Test
    fun guardaFirmaComoPngConCanalAlfaReal() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val foto = Bitmap.createBitmap(240, 120, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(238, 235, 229))
        }
        Canvas(foto).drawLine(
            20f,
            60f,
            220f,
            60f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(20, 40, 90)
                strokeWidth = 8f
            }
        )
        val procesada = procesarFondoClaro(foto, 75)
        val salida = File(context.getExternalFilesDir(null), "qa/firma_transparente_qa.png").apply {
            parentFile?.mkdirs()
        }

        assertTrue(procesada.porcentajeTransparente > 60)
        assertTrue(guardarPngConTransparenciaVerificada(procesada.bitmap, salida))
        val verificada = android.graphics.BitmapFactory.decodeFile(salida.absolutePath)
        assertEquals(0, Color.alpha(verificada.getPixel(0, 0)))
        assertEquals(255, Color.alpha(verificada.getPixel(verificada.width / 2, verificada.height / 2)))
        verificada.recycle()
        procesada.bitmap.recycle()
        foto.recycle()
    }
    @Test
    fun generaCuatroPaginasA4HorizontalesDesdeLaPlantillaOficial() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val salida = File(context.getExternalFilesDir(null), "qa/ficha_qa.pdf").apply {
            parentFile?.mkdirs()
        }
        val ficha = FichaFamiliarEntity(
            cedulaJefeHogar = "1300000000",
            institucionSistema = "MSP",
            unidadOperativa = "CENTRO DE SALUD RURALITOS",
            codigoUo = "001234",
            areaNumero = "4",
            codigoLocalizacion = "010203",
            parroquiaCodigoLocalizacion = "03",
            cantonCodigoLocalizacion = "02",
            provinciaCodigoLocalizacion = "01",
            numeroFichaFamiliar = "FF-260717-120000-PRUEBA",
            provincia = "MANABI",
            canton = "PEDERNALES",
            parroquia = "COJIMIES",
            sector = "RURAL",
            manzana = "1",
            numeroFamilia = "2",
            direccionHabitualFamilia = "COMUNIDAD DE PRUEBA",
            barrio = "BARRIO RURAL",
            numeroCasa = "3",
            comunidad = "COMUNIDAD",
            grupoCultural = "MESTIZO",
            nombreApellidoJefeFamilia = "FAMILIA DE PRUEBA",
            numeroTelefono = "0999999999",
            fechaLlenado = "17/07/2026",
            numeroCarpeta = "7",
            latitud = -0.1234567,
            longitud = -79.1234567,
            altitud = 120.0,
            responsableNombre = "MEDICO DE PRUEBA",
            responsableCodigo = "SENESCYT-PRUEBA"
        )
        val miembro = MiembroFamiliaEntity(
            fichaId = 1,
            grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO,
            apellidosNombres = "PERSONA INTEGRANTE",
            parentesco = "JEFE/A DE FAMILIA",
            fechaNacimiento = "01/01/1980",
            ocupacion = "AGRICULTOR",
            sexo = "H",
            escolaridad = "BAS",
            vacunasCompletas = true,
            saludBucalAdecuada = true,
            numeroHistoriaClinica = "1300000000",
            cedula = "1300000000"
        )
        fun crearImagen(nombre: String, color: Int): String {
            val archivo = File(salida.parentFile, nombre)
            val bitmap = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(color)
            FileOutputStream(archivo).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            return Uri.fromFile(archivo).toString()
        }

        val familiogramaUri = crearImagen("familiograma_qa.png", Color.rgb(57, 125, 214))
        val croquisUri = crearImagen("croquis_qa.png", Color.rgb(37, 166, 117))
        val data = FichaExportData(
            ficha = ficha,
            miembros = listOf(miembro),
            embarazadas = emptyList(),
            mortalidad = emptyList(),
            calificaciones = listOf(
                CalificacionExport(
                    CalificacionRiesgoEntity(
                        fichaId = 1,
                        fechaCalificacion = "17/07/2026",
                        responsable = "MEDICO DE PRUEBA",
                        total = 8,
                        nivel = "BAJO"
                    ),
                    List(18) { if (it < 2) 4 else 0 }
                )
            ),
            gestion = listOf(
                GestionRiesgoEntity(
                    fichaId = 1,
                    fechaAnalisis = "17/07/2026",
                    numero = 1,
                    compromisoFamilia = "Cumplir controles de salud.",
                    compromisoEquipoSalud = "Realizar seguimiento domiciliario."
                )
            ),
            contaminacion = listOf(
                ContaminacionAmbientalEntity(1, 1, "17/07/2026", "HUMO EN EL SECTOR", "INDUSTRIA")
            ),
            lugaresTratamiento = listOf(LugarTratamientoEntity(1, 1, "CENTRO DE SALUD RURALITOS")),
            adjuntos = listOf(
                AdjuntoFichaEntity(fichaId = 1, tipo = "FAMILIOGRAMA", uri = familiogramaUri),
                AdjuntoFichaEntity(fichaId = 1, tipo = "CROQUIS", uri = croquisUri)
            )
        )

        FileOutputStream(salida).use { FichaPdfPlantillaExporter.generar(context, data, it) }
        assertTrue(salida.length() > 100_000)
        ParcelFileDescriptor.open(salida, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                assertEquals(4, renderer.pageCount)
                repeat(4) { indice ->
                    renderer.openPage(indice).use { pagina ->
                        assertTrue(pagina.width > pagina.height)
                        val bitmap = Bitmap.createBitmap(pagina.width, pagina.height, Bitmap.Config.ARGB_8888)
                        pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        FileOutputStream(File(salida.parentFile, "pagina_${indice + 1}.png")).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        bitmap.recycle()
                        // Ampliación x3 en cuatro cuadrantes para revisar el detalle del texto.
                        val grande = Bitmap.createBitmap(pagina.width * 3, pagina.height * 3, Bitmap.Config.ARGB_8888)
                        grande.eraseColor(Color.WHITE)
                        pagina.render(grande, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val mitadX = grande.width / 2
                        val mitadY = grande.height / 2
                        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1).forEachIndexed { q, (cx, cy) ->
                            val recorte = Bitmap.createBitmap(grande, cx * mitadX, cy * mitadY, mitadX, mitadY)
                            FileOutputStream(File(salida.parentFile, "pagina_${indice + 1}_q${q + 1}.png")).use {
                                recorte.compress(Bitmap.CompressFormat.PNG, 90, it)
                            }
                            recorte.recycle()
                        }
                        grande.recycle()
                    }
                }
            }
        }
        println("PDF_QA_PATH=${salida.absolutePath}")
    }
}
