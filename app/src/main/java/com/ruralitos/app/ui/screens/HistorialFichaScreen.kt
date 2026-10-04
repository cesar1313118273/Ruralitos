package com.ruralitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaListaRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.TarjetaRegistroRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.VerdeSalud
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistorialFichaScreen(
    fichaId: Long,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val eventos by database.historialFichaDao()
        .listar(fichaId)
        .collectAsState(initial = emptyList())
    val formato = remember {
        SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault())
    }

    PantallaListaRuralitos(
        titulo = "Historial de cambios",
        descripcion = "Consulta qué se realizó, cuándo ocurrió y qué usuario fue responsable de cada acción.",
        subtitulo = "Trazabilidad de la ficha",
        onVolver = onRegresar
    ) {
        item {
            MensajeEstadoRuralitos(
                titulo = "${eventos.size} evento(s) registrado(s)",
                descripcion = "El historial es informativo y no modifica los datos actuales de la ficha.",
                color = AzulClinico,
                simbolo = eventos.size.toString()
            )
        }
        if (eventos.isEmpty()) {
            item {
                MensajeEstadoRuralitos(
                    titulo = "Sin cambios registrados",
                    descripcion = "Las acciones realizadas en esta ficha aparecerán aquí automáticamente.",
                    color = MoradoClinico,
                    simbolo = "H"
                )
            }
        }
        items(eventos, key = { it.id }) { evento ->
            val color = colorEvento(evento.accion)
            TarjetaRegistroRuralitos(
                titulo = nombreEvento(evento.accion),
                descripcion = formato.format(Date(evento.creadoEn)),
                simbolo = simboloEvento(evento.accion),
                color = color
            ) {
                Text(
                    "Responsable: ${evento.usuarioNombre.ifBlank { "Sin identificar" }}",
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                if (evento.detalle.isNotBlank()) {
                    Text(
                        evento.detalle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
}

private fun nombreEvento(accion: String): String = when (accion) {
    "FICHA_CREADA" -> "Ficha familiar creada"
    "FICHA_ACTUALIZADA" -> "Información de la ficha actualizada"
    "FICHA_COMPLETADA" -> "Ficha marcada como completa"
    "FICHA_ARCHIVADA" -> "Ficha archivada"
    "FICHA_REACTIVADA" -> "Ficha reactivada"
    "FICHA_ELIMINADA" -> "Ficha eliminada"
    "PDF_GENERADO" -> "Documento PDF generado"
    "EXCEL_GENERADO" -> "Archivo Excel generado"
    else -> accion.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun simboloEvento(accion: String): String = when {
    accion.contains("CREADA") -> "+"
    accion.contains("COMPLETA") -> "✓"
    accion.contains("ARCHIV") -> "A"
    accion.contains("ELIMIN") -> "!"
    accion.contains("PDF") -> "P"
    accion.contains("EXCEL") -> "X"
    else -> "H"
}

private fun colorEvento(accion: String): Color = when {
    accion.contains("ELIMIN") -> RojoClinico
    accion.contains("COMPLETA") || accion.contains("REACTIV") -> VerdeSalud
    accion.contains("ARCHIV") -> NaranjaClinico
    accion.contains("PDF") || accion.contains("EXCEL") -> MoradoClinico
    else -> AzulClinico
}
