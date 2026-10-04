package com.ruralitos.app

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.domain.CatalogoAlcance
import com.ruralitos.app.ui.components.EstadoAlcance
import com.ruralitos.app.ui.components.SelectorAlcanceRuralitos
import com.ruralitos.app.ui.components.TarjetaCodigoRuralitos
import com.ruralitos.app.ui.screens.SeguridadRespaldoScreen
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.RuralitosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Filtros de «Compartir acceso» y «Exportar», código copiable y el menú de Seguridad y respaldo. */
@RunWith(AndroidJUnit4::class)
class AlcanceYRespaldoUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun existe(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun hay(texto: String) = rule.onAllNodes(hasText(texto, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private fun ficha(id: Long, jefe: String, eais: String, barrio: String) = FichaFamiliarEntity(
        id = id, cedulaJefeHogar = "0$id", institucionSistema = "", unidadOperativa = "", codigoUo = "", areaNumero = "",
        codigoLocalizacion = "", parroquiaCodigoLocalizacion = "", cantonCodigoLocalizacion = "", provinciaCodigoLocalizacion = "",
        numeroFichaFamiliar = id.toString(), provincia = "", canton = "", parroquia = "", sector = "", manzana = "", numeroFamilia = "",
        direccionHabitualFamilia = "", barrio = "", numeroCasa = "", comunidad = "", grupoCultural = "",
        nombreApellidoJefeFamilia = jefe, numeroTelefono = "", fechaLlenado = "", numeroCarpeta = "",
        organizacionId = "S1", eaisId = eais, territorioId = barrio, syncId = "sync-$id", syncEstado = "SINCRONIZADO"
    )

    private val catalogo = CatalogoAlcance(
        salas = listOf(
            SalaEntity("S1", null, "Centro Uno", "S1", "ADMINISTRADOR", "ADMINISTRADOR", nombreCentroSalud = "Centro Uno")
        ),
        eais = listOf(EaisSalaEntity("E1", "S1", 1), EaisSalaEntity("E2", "S1", 2)),
        territorios = listOf(
            TerritorioSalaEntity("B1", "S1", "E1", "BARRIO", "San Pedro"),
            TerritorioSalaEntity("B2", "S1", "E2", "BARRIO", "El Carmen")
        ),
        fichas = listOf(
            ficha(1, "Pérez Loja Juan", "E1", "B1"),
            ficha(2, "Pérez Armijos Rosa", "E1", "B1"),
            ficha(3, "Torres Vega Luis", "E2", "B2")
        ),
        salaActivaId = "S1"
    )

    @Test
    fun elSelectorOfreceLosCincoNivelesYResumeLoElegido() {
        val estado = EstadoAlcance()
        rule.setContent {
            RuralitosTheme {
                SelectorAlcanceRuralitos(estado, catalogo, "1. Qué quieres compartir", MoradoClinico, prefijoPrueba = "t")
            }
        }
        listOf("CENTRO_ACTIVO", "CENTRO", "EAIS", "BARRIO", "FICHA").forEach { assertTrue(it, existe("t_nivel_$it")) }
        assertTrue(hay("Todo el centro · 3 fichas"))

        rule.onNodeWithTag("t_nivel_BARRIO").performClick()
        rule.waitUntil(5_000) { existe("t_op_B1") }
        assertTrue(hay("Todavía no elegiste nada"))
        rule.onNodeWithTag("t_op_B1").performClick()
        rule.waitUntil(5_000) { hay("1 barrio · 2 fichas") }

        rule.onNodeWithTag("t_todos").performClick()
        rule.waitUntil(5_000) { hay("2 barrios · 3 fichas") }
        rule.onNodeWithTag("t_ninguno").performClick()
        rule.waitUntil(5_000) { hay("Todavía no elegiste nada") }
    }

    @Test
    fun alElegirFichaSePuedenMarcarVariasYBuscarPorNombre() {
        val estado = EstadoAlcance()
        rule.setContent {
            RuralitosTheme { SelectorAlcanceRuralitos(estado, catalogo, "Qué exportar", MoradoClinico, prefijoPrueba = "t") }
        }
        rule.onNodeWithTag("t_nivel_FICHA").performClick()
        rule.waitUntil(5_000) { existe("t_op_1") && existe("t_op_3") }
        rule.onNodeWithTag("t_op_1").performClick()
        rule.onNodeWithTag("t_op_3").performClick()
        rule.waitUntil(5_000) { hay("2 fichas") }
        assertEquals(setOf("1", "3"), estado.elegidos)
        estado.consulta = "armijos"
        rule.waitUntil(5_000) { !existe("t_op_3") && existe("t_op_2") }
        assertFalse(existe("t_op_1"))
    }

    @Test
    fun elCodigoSeMuestraAgrupadoYSeCopiaSinEspacios() {
        rule.setContent { RuralitosTheme { TarjetaCodigoRuralitos(codigo = "A1B2C3D4E5F60718", color = MoradoClinico) } }
        assertTrue(hay("A1B2 C3D4 E5F6 0718"))
        rule.onNodeWithTag("copiar_codigo").performClick()
        val portapapeles = rule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        rule.waitUntil(5_000) {
            portapapeles.primaryClip?.getItemAt(0)?.text?.toString() == "A1B2C3D4E5F60718"
        }
        assertTrue(existe("enviar_codigo"))
    }

    @Test
    fun seguridadYRespaldoSeparaExportarImportarYEliminar() {
        val usuario = UsuarioEntity(
            id = 7, cedula = "0000000007", nombres = "QA", cargo = "Médico", rol = "ADMIN",
            claveHash = "", claveSalt = "", correo = "", telefono = ""
        )
        rule.setContent { RuralitosTheme { SeguridadRespaldoScreen(usuario = usuario, salaActiva = null, onRestaurado = {}, onRegresar = {}) } }
        rule.waitUntil(15_000) { existe("abrir_exportar") }
        assertTrue(existe("abrir_importar") && existe("abrir_eliminar"))
        assertFalse("ya no hay «Protecciones activas»", hay("Protecciones activas"))

        rule.onNodeWithTag("abrir_exportar").performClick()
        rule.waitUntil(5_000) { hay("1. Qué exportar") }
        assertTrue(existe("exportar_nivel_BARRIO") && existe("crear_respaldo"))
        assertFalse("en Exportar no se ve lo de eliminar", existe("eliminar_todas_fichas"))

        rule.onNodeWithTag("abrir_exportar").let { /* el menú ya no está visible */ }
        assertFalse(existe("abrir_importar"))
    }
}
