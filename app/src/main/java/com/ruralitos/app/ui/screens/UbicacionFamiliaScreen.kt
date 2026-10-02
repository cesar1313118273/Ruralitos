package com.ruralitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.CianRuralitos

data class UbicacionFamiliaForm(
    val sector: String,
    val manzana: String,
    val numeroFamilia: String,
    val direccion: String,
    val barrio: String,
    val numeroCasa: String,
    val comunidad: String,
    val grupoCultural: String
)

@Composable
fun UbicacionFamiliaScreen(
    sectorInicial: String,
    datosIniciales: UbicacionFamiliaForm? = null,
    onGuardar: (UbicacionFamiliaForm) -> Unit,
    onRegresar: () -> Unit
) {
    var sector by remember { mutableStateOf(datosIniciales?.sector ?: sectorInicial) }
    var manzana by remember { mutableStateOf(datosIniciales?.manzana.orEmpty()) }
    var numeroFamilia by remember { mutableStateOf(datosIniciales?.numeroFamilia.orEmpty()) }
    var direccion by remember { mutableStateOf(datosIniciales?.direccion.orEmpty()) }
    val barrio = datosIniciales?.barrio.orEmpty().ifBlank { datosIniciales?.comunidad.orEmpty() }
    var numeroCasa by remember { mutableStateOf(datosIniciales?.numeroCasa.orEmpty()) }
    var grupoCultural by remember { mutableStateOf(datosIniciales?.grupoCultural.orEmpty()) }

    PantallaRuralitos(
        titulo = "Dirección y vivienda",
        descripcion = "Completa cómo llegar al hogar. La latitud, longitud y altitud se obtienen únicamente en la sección Croquis, donde puedes mover el punto del mapa.",
        paso = 2,
        totalPasos = 10,
        etiquetaPaso = "Información del hogar",
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = "Guardar información de esta sección",
                descripcion = "Después podrás registrar a los integrantes",
                onClick = {
                    onGuardar(
                        UbicacionFamiliaForm(
                            sector = sector.trim(),
                            manzana = manzana.trim(),
                            numeroFamilia = numeroFamilia.trim(),
                            direccion = direccion.trim(),
                            barrio = barrio.trim(),
                            numeroCasa = numeroCasa.trim(),
                            comunidad = "",
                            grupoCultural = grupoCultural.trim()
                        )
                    )
                },
                color = CianRuralitos
            )
            BotonSecundarioRuralitos(
                texto = "Regresar al panel de la ficha",
                descripcion = "Salir de esta sección sin guardar cambios",
                onClick = onRegresar
            )
        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "Ubicación territorial",
            descripcion = "Datos del sector y organización local."
        ) {
            CampoTexto(sector, { sector = it }, "Sector")
            CampoTexto(manzana, { manzana = it }, "Manzana")
            CampoTexto(numeroFamilia, { numeroFamilia = it }, "Número de familia")
            androidx.compose.material3.OutlinedTextField(
                value = barrio,
                onValueChange = {},
                readOnly = true,
                label = { androidx.compose.material3.Text("Barrio asignado") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        SeccionFormularioRuralitos(
            titulo = "Dirección del domicilio",
            descripcion = "Escribe una dirección o referencia fácil de reconocer."
        ) {
            CampoTexto(direccion, { direccion = it }, "Dirección habitual o punto de referencia")
            CampoTexto(numeroCasa, { numeroCasa = it }, "Número de casa")
        }
        SeccionFormularioRuralitos(
            titulo = "Información cultural",
            descripcion = "Registra el grupo cultural declarado por la familia."
        ) {
            CampoTexto(grupoCultural, { grupoCultural = it }, "Grupo cultural")
        }
    }
}
