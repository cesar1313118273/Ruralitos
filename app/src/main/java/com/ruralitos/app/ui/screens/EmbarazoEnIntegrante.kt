package com.ruralitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.domain.DispensarizacionAutomatica
import com.ruralitos.app.domain.EvaluacionObstetrica
import com.ruralitos.app.domain.GrupoDispensarizacion
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.VerdeSalud

/**
 * Cuadro bajo los criterios: nivel de riesgo obstétrico y grupo de dispensarización. El grupo se decide del IV al I y se
 * queda en el primero que cumple: una patología crónica (o el consumo problemático) es Grupo III y no sigue buscando.
 */
@Composable
internal fun ResultadoRiesgoObstetrico(
    evaluacion: EvaluacionObstetrica,
    miembro: MiembroFamiliaEntity,
    embarazo: EmbarazadaEntity
) {
    val grupoPorEscala = when {
        evaluacion.hayCronica -> GrupoDispensarizacion.III
        evaluacion.razones.isNotEmpty() -> GrupoDispensarizacion.II
        else -> GrupoDispensarizacion.I
    }
    // El resto de la ficha de la persona (discapacidad, enfermedades, factores) puede subir el grupo.
    val completo = remember(miembro, embarazo) { DispensarizacionAutomatica.clasificar(miembro, embarazo) }
    val gana = completo.grupo != GrupoDispensarizacion.PENDIENTE && completo.grupo.prioridad > grupoPorEscala.prioridad
    val grupo = if (gana) completo.grupo else grupoPorEscala
    val motivos: List<String> = when {
        gana -> completo.razones
        grupo == GrupoDispensarizacion.III -> evaluacion.razones.filter { it.cronica }.map { it.etiqueta }
        grupo == GrupoDispensarizacion.II -> evaluacion.razones.filter { !it.cronica }.map { it.etiqueta }
        else -> emptyList()
    }
    val nivel = when (evaluacion.nivel) {
        0 -> "Sin riesgo obstétrico"
        1 -> "Riesgo 1 · Bajo"
        2 -> "Riesgo 2 · Alto"
        else -> "Riesgo 3 · Inminente"
    }
    val color = when (grupo) {
        GrupoDispensarizacion.I -> VerdeSalud
        GrupoDispensarizacion.II -> NaranjaClinico
        else -> RojoClinico
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
        MensajeEstadoRuralitos(
            titulo = "Grupo ${grupo.codigo} · ${grupo.titulo}",
            descripcion = "$nivel. " + when (grupo) {
                GrupoDispensarizacion.I -> "No tiene criterios elegidos ni detectados."
                GrupoDispensarizacion.II -> "Tiene factores de riesgo y nada de un grupo superior."
                GrupoDispensarizacion.III -> "Tiene una patología crónica o un criterio del Grupo III; no se sigue buscando en otros grupos."
                else -> "Tiene una discapacidad registrada."
            },
            color = color,
            simbolo = grupo.codigo,
            modifier = Modifier.testTag("resultado_obstetrico")
        )
        if (evaluacion.inminente) {
            MensajeEstadoRuralitos(
                titulo = "Riesgo inminente",
                descripcion = "Hay criterios del Riesgo 3: requiere atención inmediata.",
                color = RojoClinico,
                simbolo = "!",
                modifier = Modifier.testTag("aviso_riesgo_inminente")
            )
        }
        if (motivos.isNotEmpty()) {
            Text(
                "Motivos del Grupo ${grupo.codigo}: " + motivos.joinToString("; "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun CampoEnteroSalud(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(it.filter(Char::isDigit)) },
        label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
