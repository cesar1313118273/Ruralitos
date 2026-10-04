package com.ruralitos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Botón flotante pequeño y redondo; sin contenido propio lleva el signo + para agregar. */
@Composable
fun BotonFlotanteRedondo(
    descripcion: String,
    color: Color,
    etiquetaPrueba: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit = { Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = color,
        contentColor = Color.White,
        modifier = modifier
            .size(52.dp)
            .semantics { contentDescription = descripcion }
            .testTag(etiquetaPrueba)
    ) { contenido() }
}

/** Pila de botones flotantes abajo a la derecha, por encima de la barra de acción de la pantalla. */
@Composable
fun BoxScope.PilaBotonesFlotantes(contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 104.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = contenido
    )
}
