package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.domain.GrupoEdadFamiliar
import com.ruralitos.app.ui.components.ContenidoAdaptable
import com.ruralitos.app.ui.components.FondoRuralitos
import com.ruralitos.app.ui.screens.*
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Herramienta de revisión visual (no verifica nada): dibuja las pantallas principales con datos de ejemplo y guarda
 * una captura de cada una. Se ejecuta con el emulador en distintos tamaños (`wm size` / `wm density`) para comprobar
 * que la app se adapta a teléfonos pequeños, teléfonos grandes y tabletas.
 */
@RunWith(AndroidJUnit4::class)
class GaleriaPantallasTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun miembro(fichaId: Long, parentesco: String, sexo: String, nombre: String, nacimiento: String) =
        MiembroFamiliaEntity(
            fichaId = fichaId, grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO,
            apellidosNombres = nombre, parentesco = parentesco, fechaNacimiento = nacimiento,
            ocupacion = "Agricultor", sexo = sexo, escolaridad = "BAS"
        )

    @androidx.compose.runtime.Composable
    private fun Dibujar(nombre: String, ficha: FichaFamiliarEntity, usuario: UsuarioEntity, database: RuralitosDatabase) {
        val fichaId = ficha.id
        when (nombre) {
            "01_acceso" -> AccesoSupabaseScreen(false, null, { _, _ -> }, {}, {}, {})
            "02_inicio" -> InicioRuralitosScreen(
                usuarioNombre = "Ana María Torres", usuarioCargo = "Médico", usuarioSexo = "M",
                usuarioApellidos = "Torres", codigoSenescyt = "1234", fichasPendientesSync = 2,
                estadoSincronizacion = "Sincronizado", onNuevaFicha = {}, onBuscarFicha = {}, onSala = {},
                onDispensarizacion = {}, onNotasDiarias = {}, onAgenda = {}, onEstadisticas = {}, onPerfil = {},
                onCredenciales = {}, onSeguridad = {}, onCambiarClave = {}, onEliminarCuenta = {}, onCerrarSesion = {},
                usuarioId = 1
            )
            "03_buscar" -> BuscarFichasScreen({}, {})
            "04_ficha" -> FichaSeccionesScreen(ficha, {}, {}, {}, {})
            "05_miembros" -> MiembrosFamiliaScreen(fichaId, 1, {}, {})
            "06_salud" -> SaludFamiliarScreen(fichaId, {}, {})
            "07_riesgo" -> RiesgoFamiliarScreen(fichaId, "QA", {}, {})
            "08_agenda" -> AgendaScreen(1, "", {}, {}, {})
            "09_estadisticas" -> EstadisticasScreen({}, {})
            "10_dispensarizacion" -> DispensarizacionScreen(null, {}, {})
            "11_perfil" -> PerfilScreen(usuario, database.usuarioDao(), {}, {}, {}, {})
            "12_notas" -> NotasDiariasScreen("", 1, {})
            "13_ubicacion" -> UbicacionFamiliaScreen("Centro", null, {}, {})
        }
    }

    @Test
    fun capturaLasPantallas() {
        val etiqueta = InstrumentationRegistry.getArguments().getString("etiqueta") ?: "pantalla"
        val soloUna = InstrumentationRegistry.getArguments().getString("solo")
        val context = rule.activity
        val database = RuralitosDatabase.obtenerBaseDatos(context)
        val ficha = FichaFamiliarEntity(
            cedulaJefeHogar = "0000000009", institucionSistema = "MSP", unidadOperativa = "CENTRO DE SALUD EJEMPLO",
            codigoUo = "1", areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1",
            cantonCodigoLocalizacion = "1", provinciaCodigoLocalizacion = "1",
            numeroFichaFamiliar = "QA-GALERIA-${System.currentTimeMillis()}",
            provincia = "Loja", canton = "Loja", parroquia = "Vilcabamba", sector = "Centro", manzana = "1", numeroFamilia = "1",
            direccionHabitualFamilia = "Calle principal", barrio = "San José", numeroCasa = "12", comunidad = "C",
            grupoCultural = "MESTIZO", nombreApellidoJefeFamilia = "PÉREZ LUIS", numeroTelefono = "0999999999",
            fechaLlenado = "01/10/2026", numeroCarpeta = "1", responsableNombre = "QA", responsableCodigo = "1"
        )
        val fichaId = runBlocking { database.fichaFamiliarDao().guardarFicha(ficha) }
        runBlocking {
            val dao = database.fichaContenidoDao()
            dao.guardarMiembro(miembro(fichaId, "JEFE/A DE FAMILIA", "H", "PÉREZ LUIS", "01/01/1980"))
            dao.guardarMiembro(miembro(fichaId, "CÓNYUGE/PAREJA", "M", "GÓMEZ ANA", "01/01/1982"))
            dao.guardarMiembro(miembro(fichaId, "HIJO/A", "H", "PÉREZ LEO", "01/01/2010"))
        }
        val fichaGuardada = ficha.copy(id = fichaId)
        val usuario = UsuarioEntity(
            id = 1, cedula = "0000000001", nombres = "Ana María Torres", cargo = "Médico", rol = "ADMIN",
            claveHash = "", claveSalt = "", correo = "ana@example.com", telefono = "0999999999", codigoSenescyt = "1234"
        )
        val nombres = listOf(
            "01_acceso", "02_inicio", "03_buscar", "04_ficha", "05_miembros", "06_salud", "07_riesgo",
            "08_agenda", "09_estadisticas", "10_dispensarizacion", "11_perfil", "12_notas", "13_ubicacion"
        ).filter { soloUna == null || it.contains(soloUna) }
        var indice by mutableIntStateOf(0)
        rule.setContent {
            RuralitosTheme {
                FondoRuralitos {
                    ContenidoAdaptable { Dibujar(nombres[indice], fichaGuardada, usuario, database) }
                }
            }
        }
        val salida = File(context.getExternalFilesDir(null), "galeria/$etiqueta").apply { deleteRecursively(); mkdirs() }
        try {
            nombres.forEachIndexed { i, nombre ->
                indice = i
                rule.waitForIdle()
                Thread.sleep(1_500)
                rule.waitForIdle()
                runCatching {
                    val imagen = rule.onRoot().captureToImage().asAndroidBitmap()
                    File(salida, "$nombre.png").outputStream().use {
                        imagen.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it)
                    }
                }.onFailure { android.util.Log.e("Galeria", "No se pudo capturar $nombre", it) }
            }
        } finally {
            runBlocking { database.fichaFamiliarDao().eliminarFicha(fichaGuardada) }
        }
    }
}
