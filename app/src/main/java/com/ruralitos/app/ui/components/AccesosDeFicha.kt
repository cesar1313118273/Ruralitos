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
    onCerrar: () -> Unit
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
    var paraVisita by remember { mutableStateOf<PersonaConAccesoFicha?>(null) }

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

    paraVisita?.let { persona ->
        AsignarVisitaRuralitos(
            ficha = ficha,
            persona = persona,
            onCerrar = { paraVisita = null },
            onAsignada = { paraVisita = null; aviso = "Visita asignada. A ${persona.nombre} le aparece en su agenda al sincronizar." }
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
                            TextButton(
                                onClick = { paraVisita = persona },
                                enabled = !trabajando,
                                modifier = Modifier.testTag("asignar_visita_${persona.usuarioId}")
                            ) { Text("Asignar visita", color = AzulClinico, fontWeight = FontWeight.SemiBold) }
                            if (persona.via == "FICHA") {
                                TextButton(
                                    onClick = { porQuitar = persona },
                                    enabled = !trabajando,
                                    modifier = Modifier.testTag("quitar_ficha_${persona.usuarioId}")
                                ) { Text("Quitar", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
                            }
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

/** Elegir fecha, hora y una nota para la visita que se le asigna a un compañero. */
@Composable
private fun AsignarVisitaRuralitos(
    ficha: FichaFamiliarEntity,
    persona: PersonaConAccesoFicha,
    onCerrar: () -> Unit,
    onAsignada: () -> Unit
) {
    val context = LocalContext.current
    val api = remember(context) { SupabaseApi(context) }
    val scope = rememberCoroutineScope()
    val manana = remember { inicioDiaLocal(System.currentTimeMillis() + 24L * 60 * 60 * 1000) }
    var dia by remember { mutableLongStateOf(manana) }
    var hora by remember { mutableIntStateOf(8) }
    var minuto by remember { mutableIntStateOf(0) }
    var nota by remember { mutableStateOf("") }
    var eligiendoDia by remember { mutableStateOf(false) }
    var eligiendoHora by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (eligiendoDia) {
        CalendarioRuralitos(
            titulo = "Día de la visita",
            fechaInicialMillis = dia,
            onElegida = { dia = it; eligiendoDia = false },
            onCerrar = { eligiendoDia = false }
        )
    }
    if (eligiendoHora) {
        HoraRuralitos(
            titulo = "Hora de la visita",
            horaInicial = hora,
            minutoInicial = minuto,
            onElegida = { h, m -> hora = h; minuto = m; eligiendoHora = false },
            onCerrar = { eligiendoHora = false }
        )
    }

    VentanaRuralitos(
        titulo = "Asignar visita",
        subtitulo = "${persona.nombre} · ${ficha.nombreApellidoJefeFamilia.ifBlank { "Ficha ${ficha.numeroFichaFamiliar}" }}",
        simbolo = "◔",
        color = AzulClinico,
        cerrarAlTocarFuera = false,
        onCerrar = onCerrar,
        contenido = {
            BotonSecundarioRuralitos(
                texto = "Día: " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dia),
                onClick = { eligiendoDia = true },
                modifier = Modifier.testTag("visita_dia")
            )
            BotonSecundarioRuralitos(
                texto = "Hora: " + String.format(Locale.US, "%02d:%02d", hora, minuto),
                onClick = { eligiendoHora = true },
                modifier = Modifier.testTag("visita_hora")
            )
            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it.take(200) },
                label = { Text("Nota para quien visita (opcional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().testTag("visita_nota")
            )
            Text(
                "Le aparece en su Agenda con un recordatorio la próxima vez que sincronice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            error?.let {
                Text(it, color = NaranjaClinico, fontWeight = FontWeight.SemiBold)
            }
        },
        acciones = {
            BotonPrincipalRuralitos(
                texto = if (enviando) "Asignando…" else "Asignar visita",
                enabled = !enviando,
                color = AzulClinico,
                onClick = {
                    val momento = Calendar.getInstance().apply {
                        timeInMillis = dia
                        set(Calendar.HOUR_OF_DAY, hora); set(Calendar.MINUTE, minuto)
                    }.timeInMillis
                    enviando = true
                    error = null
                    scope.launch {
                        runCatching { api.asignarVisita(ficha.syncId, persona.usuarioId, momento, nota) }
                            .onSuccess { onAsignada() }
                            .onFailure { error = it.message ?: "No se pudo asignar la visita." }
                        enviando = false
                    }
                },
                modifier = Modifier.testTag("confirmar_visita")
            )
            BotonSecundarioRuralitos(texto = "Cancelar", onClick = onCerrar, enabled = !enviando)
        }
    )
}
