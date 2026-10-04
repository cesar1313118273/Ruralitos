package com.ruralitos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.remote.PersonaConAccesoFicha
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** «Barrio», «Todo el centro»… para decir por qué vía una persona ve la ficha. */
internal fun textoViaAcceso(via: String): String = when (via) {
    "FICHA" -> "Solo esta ficha"
    "BARRIO" -> "Por el barrio"
    "EAIS" -> "Por el EAIS"
    else -> "Por todo el centro"
}

/**
 * Con quién compartí una ficha mía: ver el permiso de cada persona, asignarle una visita (le aparece en su agenda)
 * y quitarle el acceso a esta ficha cuando se le dio ficha por ficha.
 */
@Composable
fun AccesosDeFichaRuralitos(
    ficha: FichaFamiliarEntity,
    onCerrar: () -> Unit,
    /** Se llama cuando la ficha ya pasó a otra persona (la pantalla de la ficha debe cerrarse). */
    onTraspasada: () -> Unit = {}
) {
    val context = LocalContext.current
    val api = remember(context) { SupabaseApi(context) }
    val scope = rememberCoroutineScope()
    var personas by remember { mutableStateOf<List<PersonaConAccesoFicha>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var trabajando by remember { mutableStateOf(false) }
    var porQuitar by remember { mutableStateOf<PersonaConAccesoFicha?>(null) }
    var porTraspasar by remember { mutableStateOf<PersonaConAccesoFicha?>(null) }

    LaunchedEffect(recarga) {
        if (!api.hayInternet()) {
            cargando = false
            error = "Necesitas internet para ver con quién compartiste esta ficha."
            return@LaunchedEffect
        }
        cargando = true
        error = null
        runCatching { api.personasConAccesoFicha(ficha.syncId) }
            .onSuccess {
                personas = it
                withContext(Dispatchers.IO) {
                    RuralitosDatabase.obtenerBaseDatos(context).sincronizacionDao()
                        .marcarFichaCompartidaPorMi(ficha.syncId, it.size)
                }
            }
            .onFailure { error = it.message ?: "No se pudo cargar la lista." }
        cargando = false
    }

    porQuitar?.let { persona ->
        VentanaConfirmarRuralitos(
            titulo = "Quitar acceso a esta ficha",
            mensaje = "${persona.nombre} dejará de ver la ficha de ${ficha.nombreApellidoJefeFamilia.ifBlank { "esta familia" }}. " +
                "Las demás fichas que le compartiste no cambian.",
            textoConfirmar = "Quitar acceso",
            confirmarHabilitado = !trabajando,
            onConfirmar = {
                porQuitar = null
                trabajando = true
                scope.launch {
                    runCatching { api.quitarAccesoFicha(ficha.syncId, persona.usuarioId) }
                        .onSuccess { aviso = "Se quitó el acceso a esta ficha."; recarga++ }
                        .onFailure { aviso = it.message ?: "No se pudo quitar el acceso." }
                    trabajando = false
                }
            },
            textoCancelar = "Cancelar",
            peligro = true,
            onCancelar = { porQuitar = null }
        )
    }

    porTraspasar?.let { persona ->
        VentanaConfirmarRuralitos(
            titulo = "Traspasar la ficha",
            mensaje = "La ficha de ${ficha.nombreApellidoJefeFamilia.ifBlank { "esta familia" }} pasará a ser de ${persona.nombre}: " +
                "ella decidirá con quién se comparte y será la única que pueda eliminarla. " +
                "Tú seguirás pudiendo editarla. Las demás personas que la veían por ti dejarán de verla.",
            textoConfirmar = "Sí, traspasar",
            confirmarHabilitado = !trabajando,
            onConfirmar = {
                porTraspasar = null
                trabajando = true
                scope.launch {
                    runCatching { api.traspasarFicha(ficha.syncId, persona.usuarioId) }
                        .onSuccess {
                            com.ruralitos.app.data.sync.ProgramadorSincronizacion.ejecutarAhora(context)
                            AvisosRuralitos.mostrar("Ficha traspasada a ${persona.nombre}.")
                            onTraspasada()
                        }
                        .onFailure { aviso = it.message ?: "No se pudo traspasar la ficha." }
                    trabajando = false
                }
            },
            textoCancelar = "Cancelar",
            peligro = true,
            onCancelar = { porTraspasar = null }
        )
    }

    VentanaRuralitos(
        titulo = "Con quién compartiste esta ficha",
        subtitulo = ficha.nombreApellidoJefeFamilia.ifBlank { "Ficha ${ficha.numeroFichaFamiliar}" },
        simbolo = "⇄",
        color = MoradoClinico,
        onCerrar = onCerrar,
        contenido = {
            when {
                cargando -> Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                error != null -> MensajeEstadoRuralitos(
                    titulo = "No se pudo cargar",
                    descripcion = error.orEmpty(),
                    color = NaranjaClinico,
                    simbolo = "!"
                )
                personas.isEmpty() -> Text(
                    "Todavía no la compartiste con nadie.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("sin_personas_ficha")
                )
                else -> personas.forEach { persona ->
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(persona.nombre, fontWeight = FontWeight.SemiBold)
                        if (persona.cargo.isNotBlank()) {
                            Text(persona.cargo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            (if (persona.permiso == "LECTOR") "Solo lectura" else "Puede editar") + " · " + textoViaAcceso(persona.via),
                            style = MaterialTheme.typography.labelMedium,
                            color = MoradoClinico,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (persona.via == "FICHA") {
                                TextButton(
                                    onClick = { porQuitar = persona },
                                    enabled = !trabajando,
                                    modifier = Modifier.testTag("quitar_ficha_${persona.usuarioId}")
                                ) { Text("Quitar", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
                            }
                            TextButton(
                                onClick = { porTraspasar = persona },
                                enabled = !trabajando,
                                modifier = Modifier.testTag("traspasar_${persona.usuarioId}")
                            ) { Text("Traspasar", color = MoradoClinico, fontWeight = FontWeight.SemiBold) }
                        }
                        if (persona.via != "FICHA") {
                            Text(
                                "Ve esta ficha porque le compartiste todo un grupo. Se le quita desde Mis Salas → Compartir acceso → Compartido con.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 6.dp))
                }
            }
            aviso?.let {
                Text(it, color = CianRuralitos, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
        },
        acciones = {
            BotonSecundarioRuralitos(texto = "Cerrar", onClick = onCerrar)
        }
    )
}
