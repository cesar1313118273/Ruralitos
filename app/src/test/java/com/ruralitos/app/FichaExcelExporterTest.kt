package com.ruralitos.app

import com.ruralitos.app.data.export.CalificacionExport
import com.ruralitos.app.data.export.FichaExcelExporter
import com.ruralitos.app.data.export.FichaExportData
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.GestionRiesgoEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.MortalidadFamiliarEntity
import com.ruralitos.app.domain.GrupoEdadFamiliar
import org.apache.poi.ss.util.CellReference
import org.apache.poi.ss.usermodel.PrintSetup
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64

class FichaExcelExporterTest {
    @Test
    fun rellenaUnaCopiaYPreservaLosSimbolosDeLaPlantilla() {
        val plantilla = listOf(
            File(System.getProperty("user.dir"), "app/src/main/assets/ficha_familiar.xlsx"),
            File(System.getProperty("user.dir"), "src/main/assets/ficha_familiar.xlsx")
        ).first { it.exists() }

        val ficha = FichaFamiliarEntity(
            cedulaJefeHogar = "1300000000",
            institucionSistema = "MSP",
            unidadOperativa = "CENTRO PRUEBA",
            codigoUo = "001234",
            areaNumero = "4",
            codigoLocalizacion = "010203",
            parroquiaCodigoLocalizacion = "03",
            cantonCodigoLocalizacion = "02",
            provinciaCodigoLocalizacion = "01",
            numeroFichaFamiliar = "99",
            provincia = "MANABI",
            canton = "PEDERNALES",
            parroquia = "COJIMIES",
            sector = "RURAL",
            manzana = "1",
            numeroFamilia = "2",
            direccionHabitualFamilia = "DIRECCION PRUEBA",
            barrio = "BARRIO",
            numeroCasa = "3",
            comunidad = "COMUNIDAD",
            grupoCultural = "MESTIZO",
            nombreApellidoJefeFamilia = "PERSONA PRUEBA",
            numeroTelefono = "0999999999",
            fechaLlenado = "16/07/2026",
            numeroCarpeta = "7",
            responsableNombre = "PROFESIONAL PRUEBA",
            responsableCodigo = "1016.2025-3143718"
        )
        val miembros = listOf(
            MiembroFamiliaEntity(
                fichaId = 1,
                grupoEdad = GrupoEdadFamiliar.MENOR_UN_ANIO,
                apellidosNombres = "BEBE PRUEBA",
                parentesco = "HIJO",
                fechaNacimiento = "01/01/2026",
                ocupacion = "NO DEBE EXPORTARSE",
                sexo = "H",
                escolaridad = "SUP"
            ),
            MiembroFamiliaEntity(
                fichaId = 1,
                grupoEdad = GrupoEdadFamiliar.CINCO_A_NUEVE,
                apellidosNombres = "NINA PRUEBA",
                parentesco = "HIJA",
                fechaNacimiento = "01/01/2019",
                ocupacion = "ESTUDIANTE",
                sexo = "M",
                escolaridad = "BAS"
            )
        )
        val contaminacion = listOf(
            ContaminacionAmbientalEntity(
                fichaId = 1,
                fechaInforme = "17/07/2026",
                tipoContaminanteDescripcion = "HUMO",
                causanteContaminacion = "INDUSTRIA"
            )
        )
        val data = FichaExportData(
            ficha = ficha,
            miembros = miembros,
            embarazadas = emptyList(),
            mortalidad = listOf(
                MortalidadFamiliarEntity(
                    fichaId = 1,
                    nombre = "PEREZ LOPEZ JUAN",
                    parentesco = "PADRE",
                    edadAlFallecer = 72,
                    causa = "CARDIOPATIA"
                )
            ),
            calificaciones = listOf(
                CalificacionExport(
                    CalificacionRiesgoEntity(
                        fichaId = 1,
                        fechaCalificacion = "17/07/2026",
                        responsable = "",
                        total = 72,
                        nivel = "ALTO"
                    ),
                    List(18) { 4 }
                )
            ),
            gestion = listOf(
                GestionRiesgoEntity(
                    fichaId = 1,
                    fechaAnalisis = "17/07/2026",
                    numero = 1,
                    compromisoFamilia = "COMPROMISO FAMILIAR",
                    compromisoEquipoSalud = "COMPROMISO EQUIPO",
                    responsable = ""
                )
            ),
            contaminacion = contaminacion,
            lugaresTratamiento = listOf(
                LugarTratamientoEntity(fichaId = 1, descripcion = "CENTRO DE SALUD"),
                LugarTratamientoEntity(fichaId = 1, descripcion = "MEDICO DE CONFIANZA"),
                LugarTratamientoEntity(fichaId = 1, descripcion = "HOSPITAL CANTONAL"),
                LugarTratamientoEntity(fichaId = 1, descripcion = "PROMOTOR COMUNITARIO"),
                LugarTratamientoEntity(fichaId = 1, descripcion = "NO DEBE EXPORTARSE")
            ),
            adjuntos = emptyList()
        )

        val bytes = XSSFWorkbook(
            ByteArrayInputStream(FichaExcelExporter.sanitizarPlantillaXlsx(plantilla.readBytes()))
        ).use { workbook ->
            FichaExcelExporter.rellenar(workbook, data)
            ByteArrayOutputStream().also { workbook.write(it) }.toByteArray()
        }
        val archivoQa = File(System.getProperty("user.dir"), "build/qa/ficha_roundtrip.xlsx")
        archivoQa.parentFile?.mkdirs()
        archivoQa.writeBytes(bytes)

        XSSFWorkbook(ByteArrayInputStream(bytes)).use { resultado ->
            fun valor(hoja: String, direccion: String): String {
                val referencia = CellReference(direccion)
                return resultado.getSheet(hoja)
                    .getRow(referencia.row)
                    .getCell(referencia.col.toInt())
                    ?.toString()
                    .orEmpty()
            }

            assertEquals("la institución del Ministerio ya no se escribe en el documento", "", valor("1", "B2"))
            assertEquals("PERSONA PRUEBA", valor("1", "AF7"))
            assertEquals("", valor("1", "AK12"))
            assertEquals("", valor("1", "BD12"))
            assertEquals("ESTUDIANTE", valor("1", "AK17"))
            assertEquals("X", valor("1", "AZ17"))
            assertEquals("PEREZ LOPEZ JUAN", valor("1", "B43"))
            assertEquals("PADRE", valor("1", "Y43"))
            assertEquals("72.0", valor("1", "AE43"))
            assertEquals("CARDIOPATIA", valor("1", "AJ43"))
            assertEquals(
                "COUNTIF(AT12:AT31,\"X\")",
                resultado.getSheet("1").getRow(31).getCell(45).cellFormula
            )
            assertEquals(
                "IF(AND(AN44>=15,AN44<=34),\"X\",\"\")",
                resultado.getSheet("2").getRow(44).getCell(31).cellFormula
            )
            assertEquals("17/07/2026", valor("4", "AY35"))
            assertEquals("HUMO", valor("4", "BE35"))
            assertEquals("INDUSTRIA", valor("4", "CK35"))
            assertEquals("4.0", valor("2", "AN7"))
            assertEquals("4.0", valor("2", "AN41"))
            assertEquals("SUM(AN7:AN42)", resultado.getSheet("2").getRow(43).getCell(39).cellFormula)
            assertEquals("PROFESIONAL PRUEBA", valor("2", "X47"))
            assertEquals("P.P.", valor("3", "CS6"))
            assertEquals("CENTRO DE SALUD", valor("4", "AY46"))
            assertEquals("MEDICO DE CONFIANZA", valor("4", "AY47"))
            assertEquals("HOSPITAL CANTONAL", valor("4", "AY48"))
            assertEquals("PROMOTOR COMUNITARIO", valor("4", "AY49"))
            assertTrue(resultado.getSheet("4").drawingPatriarch.shapes.size >= 37)
            repeat(resultado.numberOfSheets) { indice ->
                val configuracion = resultado.getSheetAt(indice).printSetup
                assertTrue(configuracion.landscape)
                assertEquals(PrintSetup.A4_PAPERSIZE, configuracion.paperSize)
                assertEquals(1.toShort(), configuracion.fitWidth)
                assertEquals(1.toShort(), configuracion.fitHeight)
            }
        }
    }

    @Test
    fun generaDosInicialesDeNombreYApellido() {
        assertEquals("C.Q.", FichaExcelExporter.inicialesResponsable("César Alejandro Canchingre Quevedo"))
        assertEquals("M.P.", FichaExcelExporter.inicialesResponsable("María de los Ángeles Pérez"))
    }
    @Test
    fun insertaLaFirmaEnElEspacioDelResponsable() {
        val plantilla = listOf(
            File(System.getProperty("user.dir"), "app/src/main/assets/ficha_familiar.xlsx"),
            File(System.getProperty("user.dir"), "src/main/assets/ficha_familiar.xlsx")
        ).first { it.exists() }
        val png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        )

        XSSFWorkbook(
            ByteArrayInputStream(FichaExcelExporter.sanitizarPlantillaXlsx(plantilla.readBytes()))
        ).use { workbook ->
            val sheet = workbook.getSheet("1")
            val cantidadAnterior = sheet.drawingPatriarch?.shapes?.size ?: 0

            FichaExcelExporter.insertarFirma(workbook, png)

            val dibujos = sheet.drawingPatriarch.shapes
            assertEquals(cantidadAnterior + 1, dibujos.size)
            val anchor = dibujos.last().anchor as XSSFClientAnchor
            assertEquals(90, anchor.col1.toInt())
            assertEquals(42, anchor.row1)
            assertEquals(97, anchor.col2.toInt())
            assertEquals(45, anchor.row2)
        }
    }

    @Test
    fun insertaFamiliogramaYCroquisYPreservaLasFigurasTrasGuardar() {
        val plantilla = listOf(
            File(System.getProperty("user.dir"), "app/src/main/assets/ficha_familiar.xlsx"),
            File(System.getProperty("user.dir"), "src/main/assets/ficha_familiar.xlsx")
        ).first { it.exists() }
        val png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        )

        val bytes = XSSFWorkbook(
            ByteArrayInputStream(FichaExcelExporter.sanitizarPlantillaXlsx(plantilla.readBytes()))
        ).use { workbook ->
            val cantidadAnterior = workbook.getSheet("4").drawingPatriarch.shapes.size
            FichaExcelExporter.insertarImagenAdjunta(workbook, png, "FAMILIOGRAMA")
            FichaExcelExporter.insertarImagenAdjunta(workbook, png, "CROQUIS")
            assertTrue(cantidadAnterior >= 37)
            ByteArrayOutputStream().also { workbook.write(it) }.toByteArray()
        }
        val archivoQa = File(System.getProperty("user.dir"), "build/qa/ficha_imagenes_roundtrip.xlsx")
        archivoQa.parentFile?.mkdirs()
        archivoQa.writeBytes(bytes)

        XSSFWorkbook(ByteArrayInputStream(bytes)).use { workbook ->
            val dibujos = workbook.getSheet("4").drawingPatriarch.shapes
            assertTrue(dibujos.size >= 39)
            val familiograma = dibujos[dibujos.size - 2].anchor as XSSFClientAnchor
            assertEquals(27, familiograma.col1.toInt())
            assertEquals(2, familiograma.row1)
            assertEquals(96, familiograma.col2.toInt())
            assertEquals(32, familiograma.row2)
            val croquis = dibujos.last().anchor as XSSFClientAnchor
            assertEquals(1, croquis.col1.toInt())
            assertEquals(34, croquis.row1)
            assertEquals(48, croquis.col2.toInt())
            assertEquals(49, croquis.row2)
        }
    }
}
