package com.ruralitos.app.ui.screens

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import com.ruralitos.app.data.local.database.RuralitosDatabase
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import com.ruralitos.app.domain.SaludoProfesional
import com.ruralitos.app.ui.components.LogoRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import com.ruralitos.app.ui.theme.CianRuralitos
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * RURALITOS - INICIO
 *
 * Versión visual renovada a partir del archivo original.
 * Se conservan:
 * - firma pública de InicioRuralitosScreen
 * - callbacks
 * - navegación
 * - saludo dinámico
 * - menú lateral
 * - lógica de sincronización
 * - recursos existentes
 *
 * Solo se modifica la presentación visual.
 */

private val FondoPantalla = Color(0xFFF6F9FB)
private val AzulTitulo = Color(0xFF0A2A5E)
private val AzulSecundario = Color(0xFF5B7083)
private val VerdePrincipal = Color(0xFF0889A0)
private val VerdeSuave = Color(0xFFE3F4F7)
private val NaranjaPrincipal = Color(0xFFF7941D)
private val NaranjaFondo = Color(0xFFFFF8EF)
private val NaranjaBorde = Color(0xFFFFD7A3)
private val BordeTarjeta = Color(0xFFE2ECF1)
private val Montana1 = Color(0xFFF6F9FB)
private val Montana2 = Color(0xFFE8EFFA)
private val Montana3 = Color(0xFFE2ECF1)

@Composable
fun InicioRuralitosScreen(
    usuarioNombre: String,
    usuarioCargo: String,
    usuarioSexo: String = "",
    usuarioApellidos: String = "",
    codigoSenescyt: String,
    fichasPendientesSync: Int,
    estadoSincronizacion: String,
    onNuevaFicha: () -> Unit,
    onBuscarFicha: () -> Unit,
    onSala: () -> Unit,
    onDispensarizacion: () -> Unit,
    onNotasDiarias: () -> Unit,
    onAgenda: () -> Unit,
    onEstadisticas: () -> Unit,
    onPerfil: () -> Unit,
    onCredenciales: () -> Unit,
    onSeguridad: () -> Unit,
    onCambiarClave: () -> Unit,
    onEliminarCuenta: () -> Unit,
    onCerrarSesion: () -> Unit,
    usuarioId: Long = 0L
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val actividadesAgenda by remember(context, usuarioId) {
        RuralitosDatabase.obtenerBaseDatos(context).agendaDao().observar(usuarioId)
    }.collectAsState(initial = emptyList())
    val actividadesHoy = remember(actividadesAgenda) {
        val inicio = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val fin = inicio + 24L * 60 * 60 * 1000
        actividadesAgenda.filter { it.estado != "CANCELADA" && it.fechaHora in inicio until fin }
    }

    var hora by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }

    // Dr./Dra. + primer nombre + primer apellido, según el cargo y el sexo del perfil.
    val saludo = SaludoProfesional.completo(
        hora = hora,
        cargo = usuarioCargo,
        sexo = usuarioSexo.ifBlank { SaludoProfesional.inferirSexo(usuarioCargo) },
        apellidos = usuarioApellidos,
        nombresCompletos = usuarioNombre
    )

    val inicial = usuarioNombre
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "R"

    fun navegar(accion: () -> Unit) {
        scope.launch {
            drawerState.close()
            accion()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 340.dp),
                drawerContainerColor = Color.White
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shape = RoundedCornerShape(0.dp),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 18.dp,
                                        end = 14.dp,
                                        top = 20.dp,
                                        bottom = 22.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LogoRuralitos(
                                    modifier = Modifier.size(66.dp)
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 13.dp)
                                ) {
                                    Text(
                                        text = "Ruralitos",
                                        color = AzulClinicoOscuro,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text = usuarioNombre.ifBlank {
                                            "Profesional de salud"
                                        },
                                        color = AzulSecundario,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Cerrar",
                                        color = AzulClinico,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BordeClinico)

                        Column(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 26.dp,
                                    bottom = 16.dp
                                )
                        ) {
                            Text(
                                text = "Cuenta y seguridad",
                                color = TextoSecundario,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    bottom = 8.dp
                                )
                            )

                            ItemMenu(
                                titulo = "Identidad profesional",
                                color = AzulClinico
                            ) {
                                navegar(onCredenciales)
                            }

                            ItemMenu(
                                titulo = "Cambiar contraseña",
                                color = NaranjaClinico
                            ) {
                                navegar(onCambiarClave)
                            }

                            ItemMenu(
                                titulo = "Seguridad y respaldos",
                                color = CianRuralitos
                            ) {
                                navegar(onSeguridad)
                            }

                            ItemMenu(
                                titulo = "Correo de contacto",
                                color = AzulClinico
                            ) {
                                navegar {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_SENDTO, Uri.parse(
                                                "mailto:cesar1313118273@gmail.com"
                                            ))
                                        )
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(
                                            context, "No hay una app de correo instalada.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            Text(
                                text = "cesar1313118273@gmail.com",
                                color = AzulSecundario,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
                            )

                            ItemMenu(
                                titulo = "Eliminar mi cuenta",
                                color = RojoClinico
                            ) {
                                navegar(onEliminarCuenta)
                            }

                            Text(
                                text = "SENESCYT ${
                                    codigoSenescyt.ifBlank {
                                        "pendiente"
                                    }
                                }",
                                color = AzulSecundario,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    top = 10.dp,
                                    bottom = 16.dp
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = BordeClinico
                        )

                        Surface(
                            onClick = {
                                navegar(onCerrarSesion)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 14.dp,
                                    bottom = 18.dp
                                )
                                .heightIn(min = 52.dp),
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.3.dp,
                                RojoClinico.copy(alpha = 0.65f)
                            ),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "↪",
                                    color = RojoClinico,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "Cerrar sesión",
                                    color = RojoClinico,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FondoPantalla)
                .formularioSeguro()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                CabeceraInicioAzul(
                    saludo = saludo,
                    estadoSincronizacion = estadoSincronizacion,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 700.dp)
                        .padding(horizontal = 16.dp)
                        .offset(y = (-14).dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TarjetaVisitasHoy(
                        actividades = actividadesHoy,
                        onClick = onAgenda
                    )

                    Text(
                        text = "Herramientas",
                        color = AzulTitulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccesoInicio("Registro general", R.drawable.ruralitos_icono_reportes,
                            onDispensarizacion, Modifier.weight(1f))
                        AccesoInicio("Territorios", R.drawable.ruralitos_icono_red,
                            onSala, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccesoInicio("Notas Diarias", R.drawable.ruralitos_icono_fichas,
                            onNotasDiarias, Modifier.weight(1f))
                        AccesoInicio("Agenda", R.drawable.ruralitos_icono_agenda,
                            onAgenda, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            BarraInferior(
                onInicio = {},
                onFichas = onBuscarFicha,
                onNueva = onNuevaFicha,
                onReportes = onEstadisticas,
                onPerfil = onPerfil
            )
        }
    }
}

@Composable
private fun CabeceraRuralitos(
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(238.dp)
    ) {
        Image(
            painter = painterResource(
                R.drawable.ruralitos_paisaje_cabecera
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 22.dp,
                    top = 20.dp
                ),
            verticalAlignment = Alignment.Top
        ) {
            BotonMenu(onMenuClick)

            Spacer(Modifier.weight(1f))

            LogoRuralitos(
                modifier = Modifier.size(70.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .align(Alignment.BottomCenter)
        ) {
            val ola = Path().apply {
                moveTo(0f, size.height * .12f)

                cubicTo(
                    size.width * .16f,
                    size.height * .10f,
                    size.width * .30f,
                    size.height * .86f,
                    size.width * .49f,
                    size.height * .78f
                )

                cubicTo(
                    size.width * .67f,
                    size.height * .70f,
                    size.width * .74f,
                    size.height * .24f,
                    size.width * .88f,
                    size.height * .26f
                )

                cubicTo(
                    size.width * .94f,
                    size.height * .27f,
                    size.width * .98f,
                    size.height * .42f,
                    size.width,
                    size.height * .52f
                )

                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            drawPath(
                path = ola,
                color = FondoPantalla
            )
        }
    }
}

@Composable
private fun CabeceraInicioAzul(
    saludo: String,
    estadoSincronizacion: String,
    onMenuClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                AzulTitulo,
                RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)
            )
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 34.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onMenuClick,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(13.dp),
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(22.dp)) {
                        val inicio = size.width * .14f
                        val fin = size.width * .86f
                        listOf(size.height * .28f, size.height * .50f, size.height * .72f).forEach { y ->
                            drawLine(
                                color = Color.White,
                                start = Offset(inicio, y),
                                end = Offset(fin, y),
                                strokeWidth = 2.4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Ruralitos",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LogoRuralitos(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
            )
        }

        Text(
            text = saludo,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = "Bienvenido a Ruralitos",
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp)
        )

        val sincronizado = estadoSincronizacion == "Sincronizado"
        Surface(
            modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(50),
            color = if (sincronizado) Color(0xFFE9F5DF) else Color.White.copy(alpha = 0.16f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(
                            if (sincronizado) Color(0xFF3A7F1F) else Color.White,
                            CircleShape
                        )
                )
                Text(
                    text = estadoSincronizacion,
                    color = if (sincronizado) Color(0xFF2F5D14) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaVisitasHoy(
    actividades: List<com.ruralitos.app.data.local.entity.ActividadAgendaEntity>,
    onClick: () -> Unit
) {
    val ahora = System.currentTimeMillis()
    val siguiente = actividades.filter { it.fechaHora >= ahora }.minByOrNull { it.fechaHora }
    val formatoHora = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeTarjeta)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ruralitos_icono_agenda),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = when (actividades.size) {
                        0 -> "Hoy: sin actividades"
                        1 -> "Hoy: 1 actividad"
                        else -> "Hoy: ${actividades.size} actividades"
                    },
                    color = AzulTitulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = siguiente?.let {
                        val quien = it.persona.ifBlank { it.tipo }
                        "Siguiente: ${formatoHora.format(java.util.Date(it.fechaHora))} · $quien"
                    } ?: "Toca para abrir tu agenda",
                    color = AzulSecundario,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "›",
                color = AzulSecundario,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun BotonMenu(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = .96f),
        shadowElevation = 1.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(27.dp)) {
                val inicio = size.width * .16f
                val fin = size.width * .84f

                listOf(
                    size.height * .28f,
                    size.height * .50f,
                    size.height * .72f
                ).forEach { y ->
                    drawLine(
                        color = AzulTitulo,
                        start = Offset(inicio, y),
                        end = Offset(fin, y),
                        strokeWidth = 2.7.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarInicial(
    inicial: String
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(
                color = VerdeSuave,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = inicial,
            color = VerdePrincipal,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AccesoInicio(
    titulo: String,
    ilustracion: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 112.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(width = 1.dp, color = BordeTarjeta)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(ilustracion),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = titulo,
                color = AzulTitulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun FondoDecorativo(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val inicioY = size.height * .72f

        val fondo = Path().apply {
            moveTo(0f, inicioY)

            cubicTo(
                size.width * .18f,
                size.height * .61f,
                size.width * .32f,
                size.height * .78f,
                size.width * .47f,
                size.height * .76f
            )

            cubicTo(
                size.width * .62f,
                size.height * .74f,
                size.width * .76f,
                size.height * .61f,
                size.width,
                size.height * .67f
            )

            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = fondo,
            color = Montana1
        )

        val medio = Path().apply {
            moveTo(0f, size.height * .80f)

            cubicTo(
                size.width * .16f,
                size.height * .70f,
                size.width * .31f,
                size.height * .86f,
                size.width * .48f,
                size.height * .83f
            )

            cubicTo(
                size.width * .66f,
                size.height * .80f,
                size.width * .82f,
                size.height * .70f,
                size.width,
                size.height * .74f
            )

            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = medio,
            color = Montana2
        )

        val frente = Path().apply {
            moveTo(0f, size.height * .88f)

            cubicTo(
                size.width * .18f,
                size.height * .80f,
                size.width * .34f,
                size.height * .94f,
                size.width * .51f,
                size.height * .90f
            )

            cubicTo(
                size.width * .69f,
                size.height * .86f,
                size.width * .84f,
                size.height * .80f,
                size.width,
                size.height * .83f
            )

            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = frente,
            color = Montana3
        )
    }
}

@Composable
private fun FondoMenuLateralAzul(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val capaDerecha = Path().apply {
            moveTo(size.width * 0.72f, 0f)

            cubicTo(
                size.width * 0.92f,
                size.height * 0.07f,
                size.width * 0.82f,
                size.height * 0.18f,
                size.width,
                size.height * 0.24f
            )

            lineTo(size.width, size.height * 0.54f)

            cubicTo(
                size.width * 0.83f,
                size.height * 0.49f,
                size.width * 0.79f,
                size.height * 0.40f,
                size.width * 0.72f,
                size.height * 0.34f
            )

            close()
        }

        drawPath(
            path = capaDerecha,
            color = Color(0xFF83C7FF).copy(alpha = 0.24f)
        )

        val capaInferior = Path().apply {
            moveTo(0f, size.height * 0.79f)

            cubicTo(
                size.width * 0.22f,
                size.height * 0.72f,
                size.width * 0.43f,
                size.height * 0.91f,
                size.width * 0.61f,
                size.height * 0.86f
            )

            cubicTo(
                size.width * 0.78f,
                size.height * 0.82f,
                size.width * 0.88f,
                size.height * 0.73f,
                size.width,
                size.height * 0.70f
            )

            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = capaInferior,
            color = Color(0xFF65B9FF).copy(alpha = 0.25f)
        )

        val capaInferiorSuave = Path().apply {
            moveTo(0f, size.height * 0.90f)

            cubicTo(
                size.width * 0.24f,
                size.height * 0.84f,
                size.width * 0.44f,
                size.height * 0.98f,
                size.width * 0.67f,
                size.height * 0.92f
            )

            cubicTo(
                size.width * 0.81f,
                size.height * 0.88f,
                size.width * 0.91f,
                size.height * 0.84f,
                size.width,
                size.height * 0.82f
            )

            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = capaInferiorSuave,
            color = Color(0xFFE2ECF1).copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun ItemMenu(
    titulo: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .heightIn(min = 56.dp),
        color = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = color,
                                shape = CircleShape
                            )
                    )
                }
            }

            Text(
                text = titulo,
                color = AzulClinicoOscuro,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )

            Text(
                text = "›",
                color = TextoSecundario,
                fontSize = 22.sp
            )
        }
    }
}

@Composable
private fun BarraInferior(
    onInicio: () -> Unit,
    onFichas: () -> Unit,
    onNueva: () -> Unit,
    onReportes: () -> Unit,
    onPerfil: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column {
            HorizontalDivider(color = BordeTarjeta)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .padding(top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                ItemBarra(
                    texto = "Inicio",
                    activo = true,
                    onClick = onInicio,
                    modifier = Modifier.weight(1f)
                )

                ItemBarra(
                    texto = "Fichas",
                    activo = false,
                    onClick = onFichas,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        onClick = onNueva,
                        modifier = Modifier
                            .offset(y = (-6).dp)
                            .size(54.dp),
                        shape = RoundedCornerShape(17.dp),
                        color = VerdePrincipal,
                        border = BorderStroke(3.dp, Color.White),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "+",
                                color = Color.White,
                                fontSize = 30.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }

                ItemBarra(
                    texto = "Reportes",
                    activo = false,
                    onClick = onReportes,
                    modifier = Modifier.weight(1f)
                )

                ItemBarra(
                    texto = "Perfil",
                    activo = false,
                    onClick = onPerfil,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ItemBarra(
    texto: String,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (activo) {
        VerdePrincipal
    } else {
        AzulSecundario
    }

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(
                horizontal = 4.dp,
                vertical = 3.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconoBarra(
            tipo = texto,
            color = color
        )

        Text(
            text = texto,
            modifier = Modifier.padding(top = 3.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = if (activo) {
                FontWeight.SemiBold
            } else {
                FontWeight.SemiBold
            }
        )
    }
}

@Composable
private fun IconoBarra(
    tipo: String,
    color: Color
) {
    Canvas(Modifier.size(21.dp)) {
        val grosor = 2.dp.toPx()

        when (tipo) {
            "Inicio" -> {
                val casa = Path().apply {
                    moveTo(
                        size.width * .12f,
                        size.height * .48f
                    )

                    lineTo(
                        size.width * .50f,
                        size.height * .13f
                    )

                    lineTo(
                        size.width * .88f,
                        size.height * .48f
                    )

                    lineTo(
                        size.width * .88f,
                        size.height * .87f
                    )

                    lineTo(
                        size.width * .12f,
                        size.height * .87f
                    )

                    close()
                }

                drawPath(
                    path = casa,
                    color = color,
                    style = Stroke(grosor)
                )

                drawLine(
                    color = color,
                    start = Offset(
                        size.width * .42f,
                        size.height * .87f
                    ),
                    end = Offset(
                        size.width * .42f,
                        size.height * .58f
                    ),
                    strokeWidth = grosor
                )
            }

            "Fichas" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        size.width * .20f,
                        size.height * .10f
                    ),
                    size = Size(
                        size.width * .60f,
                        size.height * .80f
                    ),
                    cornerRadius = CornerRadius(
                        2.dp.toPx()
                    ),
                    style = Stroke(grosor)
                )

                listOf(.40f, .58f, .76f).forEach { f ->
                    drawLine(
                        color = color,
                        start = Offset(
                            size.width * .30f,
                            size.height * f
                        ),
                        end = Offset(
                            size.width * .70f,
                            size.height * f
                        ),
                        strokeWidth = grosor
                    )
                }
            }

            "Reportes" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        size.width * .12f,
                        size.height * .47f
                    ),
                    size = Size(
                        size.width * .16f,
                        size.height * .41f
                    ),
                    cornerRadius = CornerRadius(
                        2.dp.toPx()
                    )
                )

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        size.width * .42f,
                        size.height * .30f
                    ),
                    size = Size(
                        size.width * .16f,
                        size.height * .58f
                    ),
                    cornerRadius = CornerRadius(
                        2.dp.toPx()
                    )
                )

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        size.width * .72f,
                        size.height * .12f
                    ),
                    size = Size(
                        size.width * .16f,
                        size.height * .76f
                    ),
                    cornerRadius = CornerRadius(
                        2.dp.toPx()
                    )
                )
            }

            else -> {
                drawCircle(
                    color = color,
                    radius = size.width * .17f,
                    center = Offset(
                        size.width * .5f,
                        size.height * .3f
                    ),
                    style = Stroke(grosor)
                )

                drawArc(
                    color = color,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(
                        size.width * .18f,
                        size.height * .55f
                    ),
                    size = Size(
                        size.width * .64f,
                        size.height * .48f
                    ),
                    style = Stroke(grosor)
                )
            }
        }
    }
}
