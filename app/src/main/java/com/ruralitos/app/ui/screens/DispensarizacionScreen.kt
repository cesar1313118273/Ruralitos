package com.ruralitos.app.ui.screens

import androidx.compose.ui.unit.isSpecified
import com.ruralitos.app.ui.components.TextoAjustado
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import com.ruralitos.app.ui.components.EncabezadoPantallaRuralitos
import com.ruralitos.app.ui.components.ItemMenuRuralitos
import com.ruralitos.app.ui.components.MenuDesplegableRuralitos
import com.ruralitos.app.ui.components.BotonSelectorRuralitos
import androidx.compose.foundation.layout.statusBarsPadding
import com.ruralitos.app.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.export.ArchivoFichaDescargado
import com.ruralitos.app.data.export.GestorDescargasFicha
import com.ruralitos.app.data.export.AlcanceConsolidado
import com.ruralitos.app.data.export.FiltroConsolidado
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import com.ruralitos.app.domain.DispensarizacionAutomatica
import com.ruralitos.app.domain.GrupoEdadRiesgo
import com.ruralitos.app.domain.IconosMais
import com.ruralitos.app.domain.AgrupacionRegistroComunitario
import com.ruralitos.app.domain.CalculadorRegistroComunitario
import com.ruralitos.app.domain.GrupoDispensarizacion
import com.ruralitos.app.domain.PictogramaDispensarizacion
import com.ruralitos.app.domain.ResultadoDispensarizacion
import com.ruralitos.app.ui.components.BotonVolverRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.IconoMais
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.launch

private enum class VistaDispensarizacion(val etiqueta: String) {
    FICHA("Ficha familiar"),
    TERRITORIO("Barrio"),
    EAIS("EAIS")
}

private enum class SeccionDispensarizacion(val etiqueta: String) {
    INDICADORES("Indicadores"),
    PERSONAS("Personas"),
    EXCEL("Excel")
}

private enum class PanelIndicador {
    RESUMEN_POBLACION,
    GRUPOS_DISPENSARIZACION,
    ESTRATEGIAS_NACIONALES,
    ALERTAS_EPIDEMIOLOGICAS,
    PRESTADORES_COMUNITARIOS
}

private fun alternarPanel(
    actual: PanelIndicador?,
    solicitado: PanelIndicador
): PanelIndicador? = if (actual == solicitado) null else solicitado

private data class PersonaDispensarizada(
    val ficha: FichaFamiliarEntity,
    val miembro: MiembroFamiliaEntity,
    val nombre: String,
    val parentesco: String,
    val categoriaEdad: GrupoEdadRiesgo?,
    val resultado: ResultadoDispensarizacion
)

internal fun categoriasResumenGrupo(grupo: GrupoDispensarizacion): List<GrupoEdadRiesgo> = when (grupo) {
    GrupoDispensarizacion.I -> listOf(
        GrupoEdadRiesgo.MENOR_DOS,
        GrupoEdadRiesgo.DOS_NUEVE,
        GrupoEdadRiesgo.EMBARAZADA
    )
    GrupoDispensarizacion.II, GrupoDispensarizacion.III -> GrupoEdadRiesgo.entries
    GrupoDispensarizacion.IV, GrupoDispensarizacion.PENDIENTE -> emptyList()
}

internal fun datosGruposDona(datos: List<Pair<String, Int>>): List<Triple<String, Int, GrupoDispensarizacion>> =
    datos.mapNotNull { (etiqueta, valor) ->
        val grupo = GrupoDispensarizacion.entries.firstOrNull { candidato ->
            candidato != GrupoDispensarizacion.PENDIENTE &&
                (etiqueta.equals("Grupo ${candidato.codigo}", ignoreCase = true) ||
                    etiqueta.equals(candidato.titulo, ignoreCase = true))
        }
        grupo?.takeIf { valor > 0 }?.let { Triple(etiqueta, valor, it) }
    }

@Composable
fun DispensarizacionScreen(
    fichaInicialId: Long? = null,
    onAbrirFicha: (FichaFamiliarEntity) -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val fichas by database.fichaFamiliarDao().listarFichas().collectAsState(initial = emptyList())
    val miembros by database.fichaContenidoDao().listarTodosMiembros().collectAsState(initial = emptyList())
    val embarazadas by database.fichaContenidoDao().listarTodasEmbarazadas().collectAsState(initial = emptyList())
    val territorios by database.salaDao().observarTodosTerritorios().collectAsState(initial = emptyList())
    val eais by database.salaDao().observarTodosEais().collectAsState(initial = emptyList())

    var vista by remember(fichaInicialId) {
        mutableStateOf(VistaDispensarizacion.FICHA)
    }
    var fichaSeleccionadaId by remember(fichaInicialId) { mutableStateOf(fichaInicialId) }
    var territorioSeleccionadoId by remember { mutableStateOf<String?>(null) }
    var eaisSeleccionadoId by remember { mutableStateOf<String?>(null) }
    var busqueda by remember { mutableStateOf("") }
    var exportando by remember { mutableStateOf(false) }
    var archivoExportado by remember { mutableStateOf<ArchivoFichaDescargado?>(null) }
    var errorExportacion by remember { mutableStateOf<String?>(null) }
    var seccion by remember { mutableStateOf(SeccionDispensarizacion.INDICADORES) }
    var panelIndicadorAbierto by remember {
        mutableStateOf<PanelIndicador?>(PanelIndicador.RESUMEN_POBLACION)
    }

    LaunchedEffect(fichas, fichaInicialId) {
        if (fichaSeleccionadaId == null || fichas.none { it.id == fichaSeleccionadaId }) {
            fichaSeleccionadaId = fichaInicialId?.takeIf { id -> fichas.any { it.id == id } }
                ?: fichas.firstOrNull()?.id
        }
    }

    LaunchedEffect(territorios) {
        if (territorioSeleccionadoId != null && territorios.none { it.id == territorioSeleccionadoId }) {
            territorioSeleccionadoId = null
        }
    }

    LaunchedEffect(eais) {
        if (eaisSeleccionadoId != null && eais.none { it.id == eaisSeleccionadoId }) {
            eaisSeleccionadoId = null
        }
    }

    val territorioSeleccionado = territorios.firstOrNull { it.id == territorioSeleccionadoId }
    val eaisSeleccionado = eais.firstOrNull { it.id == eaisSeleccionadoId }

    /*
     * REGLA DE CÁLCULO DE POBLACIÓN
     * 1. Ficha familiar: únicamente integrantes de la ficha elegida.
     * 2. Barrio: integrantes de TODAS las fichas asociadas al barrio elegido.
     * 3. EAIS: integrantes de TODAS las fichas asociadas al EAIS elegido.
     *
     * No se mezclan barrios ni EAIS y no se seleccionan todos automáticamente.
     */
    val fichasFiltradas = when (vista) {
        VistaDispensarizacion.FICHA -> {
            fichaSeleccionadaId?.let { id -> fichas.filter { it.id == id } }.orEmpty()
        }

        VistaDispensarizacion.TERRITORIO -> {
            territorioSeleccionado?.let { territorio ->
                fichas.filter { ficha ->
                    ficha.territorioId == territorio.id ||
                        (ficha.territorioId.isBlank() && coincideTerritorio(ficha, territorio))
                }
            }.orEmpty()
        }

        VistaDispensarizacion.EAIS -> {
            eaisSeleccionadoId?.let { id ->
                fichas.filter { it.eaisId == id }
            }.orEmpty()
        }
    }

    val idsFicha = fichasFiltradas.mapTo(mutableSetOf()) { it.id }
    val fichasPorId = fichas.associateBy { it.id }
    val embarazosPorFicha = embarazadas.groupBy { it.fichaId }

    val personas = miembros.asSequence()
        .filter { it.fichaId in idsFicha }
        .mapNotNull { miembro ->
            val ficha = fichasPorId[miembro.fichaId] ?: return@mapNotNull null
            val embarazo = DispensarizacionAutomatica.buscarEmbarazo(
                miembro,
                embarazosPorFicha[miembro.fichaId].orEmpty()
            )
            PersonaDispensarizada(
                ficha = ficha,
                miembro = miembro,
                nombre = miembro.apellidosNombres,
                parentesco = miembro.parentesco,
                categoriaEdad = DispensarizacionAutomatica.categoriaGrupoEdad(miembro, embarazo),
                resultado = DispensarizacionAutomatica.clasificar(miembro, embarazo)
            )
        }
        .toList()

    val idsCoincidentes = buscarFichasPorIntegrante(fichasFiltradas, miembros, busqueda)
    val personasFiltradas = personas.filter { it.ficha.id in idsCoincidentes }

    val gruposVisibles = listOf(
        GrupoDispensarizacion.I,
        GrupoDispensarizacion.II,
        GrupoDispensarizacion.III,
        GrupoDispensarizacion.IV
    )

    val conteos = gruposVisibles.associateWith { grupo ->
        personas.count { it.resultado.grupo == grupo }
    }

    val pendientesClasificacion = personas.count {
        it.resultado.grupo == GrupoDispensarizacion.PENDIENTE
    }

    val alcance = when (vista) {
        VistaDispensarizacion.FICHA -> fichasFiltradas.firstOrNull()?.let {
            "Ficha ${it.numeroFichaFamiliar} · ${it.nombreApellidoJefeFamilia}"
        } ?: "Selecciona una ficha familiar"

        VistaDispensarizacion.TERRITORIO -> territorioSeleccionado?.let {
            "${it.etiqueta}: ${it.nombre}"
        } ?: "Selecciona un barrio"

        VistaDispensarizacion.EAIS -> eaisSeleccionado?.let {
            "EAIS: ${it.nombre}"
        } ?: "Selecciona un EAIS"
    }

    val seleccionPoblacionCompleta = when (vista) {
        VistaDispensarizacion.FICHA -> fichaSeleccionadaId != null && fichasFiltradas.isNotEmpty()
        VistaDispensarizacion.TERRITORIO -> territorioSeleccionadoId != null
        VistaDispensarizacion.EAIS -> eaisSeleccionadoId != null
    }

    val agrupacionesRegistro = when (vista) {
        VistaDispensarizacion.FICHA -> fichas.firstOrNull { it.id == fichaSeleccionadaId }
            ?.let { ficha ->
                listOf(
                    AgrupacionRegistroComunitario(
                        titulo = ficha.cedulaJefeHogar.ifBlank { ficha.numeroFichaFamiliar },
                        fichaIds = setOf(ficha.id)
                    )
                )
            }
            .orEmpty()

        VistaDispensarizacion.TERRITORIO -> territorioSeleccionado?.let { territorio ->
            listOf(
                AgrupacionRegistroComunitario(
                    titulo = territorio.nombre,
                    fichaIds = fichasFiltradas.mapTo(mutableSetOf()) { it.id }
                )
            )
        }.orEmpty()

        VistaDispensarizacion.EAIS -> eaisSeleccionado?.let { item ->
            listOf(
                AgrupacionRegistroComunitario(
                    titulo = item.nombre,
                    fichaIds = fichasFiltradas.mapTo(mutableSetOf()) { it.id }
                )
            )
        }.orEmpty()
    }

    val columnasRegistro = CalculadorRegistroComunitario.calcular(
        agrupacionesRegistro,
        miembros,
        embarazadas
    )
    val resumenRegistro = CalculadorRegistroComunitario.resumir(columnasRegistro)

    val filtroActual = FiltroConsolidado(
        alcance = AlcanceConsolidado.FICHA,
        ids = idsFicha.mapTo(mutableSetOf()) { it.toString() },
        etiqueta = vista.name.lowercase()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
    ) {
        EncabezadoRegistroCurvo(
                titulo = "Registro general",
                descripcion = "Dispensarización y consolidado en una sola vista.",
                paso = seccion.etiqueta,
                onVolver = onRegresar
            )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

        item(key = "submenu") {
            BarraSeccionesDispensarizacion(
                seleccionada = seccion,
                onSeleccionar = {
                    seccion = it
                    busqueda = ""
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        /*
         * La selección de población es GLOBAL.
         * Todas las secciones utilizan exactamente esta misma población.
         */
        item(key = "selector_poblacion_global") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TituloSeccionRegistro("Población")

                SelectorVistaPoblacion(
                    seleccionada = vista,
                    onSeleccionar = { opcion ->
                        vista = opcion
                        busqueda = ""
                    }
                )

                when (vista) {
                    VistaDispensarizacion.FICHA -> SelectorFichas(
                        fichas = fichas,
                        miembros = miembros,
                        seleccion = fichaSeleccionadaId,
                        onSeleccion = { fichaSeleccionadaId = it }
                    )

                    VistaDispensarizacion.TERRITORIO -> SelectorTerritorios(
                        territorios = territorios,
                        seleccion = territorioSeleccionadoId,
                        onSeleccion = { territorioSeleccionadoId = it }
                    )

                    VistaDispensarizacion.EAIS -> SelectorEais(
                        eais = eais,
                        seleccion = eaisSeleccionadoId,
                        onSeleccion = { eaisSeleccionadoId = it }
                    )
                }
            }
        }

        when (seccion) {
            SeccionDispensarizacion.INDICADORES -> {
                if (!seleccionPoblacionCompleta) {
                    item(key = "indicadores_sin_poblacion") {
                        TarjetaEstadoRegistro(
                            titulo = "Selecciona la población",
                            descripcion = when (vista) {
                                VistaDispensarizacion.FICHA -> "Elige una ficha familiar para calcular únicamente sus integrantes."
                                VistaDispensarizacion.TERRITORIO -> "Elige un barrio para calcular a todas las personas de las fichas pertenecientes a ese barrio."
                                VistaDispensarizacion.EAIS -> "Elige un EAIS para calcular a todas las personas de las fichas pertenecientes a ese EAIS."
                            },
                            color = AzulClinico,
                            simbolo = "i",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    item(key = "panel_resumen_poblacion") {
                        AcordeonIndicador(
                            titulo = "Resumen de población",
                            subtitulo = "${fichasFiltradas.size} ficha(s) · ${personas.size} persona(s)",
                            color = AzulClinico,
                            simbolo = personas.size.toString(),
                            expandido = panelIndicadorAbierto == PanelIndicador.RESUMEN_POBLACION,
                            onToggle = {
                                panelIndicadorAbierto = alternarPanel(
                                    panelIndicadorAbierto,
                                    PanelIndicador.RESUMEN_POBLACION
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            TarjetaResumenPoblacion(
                                titulo = alcance,
                                fichas = fichasFiltradas.size,
                                personas = personas.size
                            )
                        }
                    }

                    item(key = "panel_grupos_dispensarizacion") {
                        AcordeonIndicador(
                            titulo = "Grupos de dispensarización",
                            subtitulo = "${personas.size} persona(s) clasificadas en la selección",
                            color = MoradoClinico,
                            simbolo = "4",
                            expandido = panelIndicadorAbierto == PanelIndicador.GRUPOS_DISPENSARIZACION,
                            onToggle = {
                                panelIndicadorAbierto = alternarPanel(
                                    panelIndicadorAbierto,
                                    PanelIndicador.GRUPOS_DISPENSARIZACION
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            GraficoDonaRegistro(
                                titulo = "Distribución por grupos",
                                datos = gruposVisibles.map {
                                    "Grupo ${it.codigo}" to (conteos[it] ?: 0)
                                }
                            )

                            if (pendientesClasificacion > 0) {
                                Text(
                                    "$pendientesClasificacion persona(s) pendientes de evaluación",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gruposVisibles.forEach { grupo ->
                                    TarjetaGrupo(
                                        grupo = grupo,
                                        personasGrupo = personas.filter {
                                            it.resultado.grupo == grupo
                                        },
                                        total = personas.size
                                    )
                                }
                            }
                        }
                    }

                    item(key = "panel_estrategias_nacionales") {
                        AcordeonIndicador(
                            titulo = "Estrategias nacionales",
                            subtitulo = "${resumenRegistro.estrategias.sumOf { it.second }} registro(s)",
                            color = CianRuralitos,
                            simbolo = "EN",
                            expandido = panelIndicadorAbierto == PanelIndicador.ESTRATEGIAS_NACIONALES,
                            onToggle = {
                                panelIndicadorAbierto = alternarPanel(
                                    panelIndicadorAbierto,
                                    PanelIndicador.ESTRATEGIAS_NACIONALES
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            GraficoBarrasRegistro(
                                titulo = "Estrategias nacionales",
                                datos = resumenRegistro.estrategias,
                                color = CianRuralitos,
                                mostrarTitulo = false
                            )
                        }
                    }

                    item(key = "panel_alertas_epidemiologicas") {
                        AcordeonIndicador(
                            titulo = "Alertas epidemiológicas",
                            subtitulo = "${resumenRegistro.alertas.sumOf { it.second }} registro(s)",
                            color = NaranjaClinico,
                            simbolo = "AE",
                            expandido = panelIndicadorAbierto == PanelIndicador.ALERTAS_EPIDEMIOLOGICAS,
                            onToggle = {
                                panelIndicadorAbierto = alternarPanel(
                                    panelIndicadorAbierto,
                                    PanelIndicador.ALERTAS_EPIDEMIOLOGICAS
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            GraficoBarrasRegistro(
                                titulo = "Alertas epidemiológicas",
                                datos = resumenRegistro.alertas,
                                color = NaranjaClinico,
                                mostrarTitulo = false
                            )
                        }
                    }

                    item(key = "panel_prestadores_comunitarios") {
                        AcordeonIndicador(
                            titulo = "Prestadores comunitarios",
                            subtitulo = "${resumenRegistro.prestadores.sumOf { it.second }} registro(s)",
                            color = AzulClinico,
                            simbolo = "",
                            icono = R.drawable.prestador_comunitario,
                            expandido = panelIndicadorAbierto == PanelIndicador.PRESTADORES_COMUNITARIOS,
                            onToggle = {
                                panelIndicadorAbierto = alternarPanel(
                                    panelIndicadorAbierto,
                                    PanelIndicador.PRESTADORES_COMUNITARIOS
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            GraficoBarrasRegistro(
                                titulo = "Prestadores comunitarios",
                                datos = resumenRegistro.prestadores,
                                color = AzulClinico,
                                mostrarTitulo = false,
                                iconos = listOf(
                                    R.drawable.prestador_comunitario,
                                    R.drawable.partero_ancestral,
                                    R.drawable.sabiduria_ancestral
                                )
                            )
                        }
                    }
                }
            }

            SeccionDispensarizacion.PERSONAS -> {
                if (!seleccionPoblacionCompleta) {
                    item(key = "personas_sin_poblacion") {
                        TarjetaEstadoRegistro(
                            titulo = "Selecciona la población",
                            descripcion = "Primero define Ficha familiar, Barrio o EAIS y después consulta las personas de esa selección.",
                            color = AzulClinico,
                            simbolo = "i",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    item(key = "seleccion_personas") {
                        TarjetaSeleccionActual(
                            alcance = alcance,
                            personas = personas.size,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item(key = "busqueda") {
                        OutlinedTextField(
                            value = busqueda,
                            onValueChange = { busqueda = it },
                            label = { Text("Nombre o cédula de cualquier integrante") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    if (personasFiltradas.isEmpty()) {
                        item(key = "personas_vacias") {
                            TarjetaEstadoRegistro(
                                titulo = "No hay personas para mostrar",
                                descripcion = if (personas.isEmpty()) {
                                    "No existen integrantes en la población seleccionada."
                                } else {
                                    "No hay coincidencias con la búsqueda."
                                },
                                color = NaranjaClinico,
                                simbolo = "!",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    items(
                        personasFiltradas,
                        key = { "persona_${it.ficha.id}-${it.nombre}-${it.parentesco}" }
                    ) { persona ->
                        TarjetaPersonaDispensarizada(
                            persona = persona,
                            onAbrirFicha = onAbrirFicha,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            SeccionDispensarizacion.EXCEL -> {
                item(key = "excel_estado") {
                    TarjetaEstadoRegistro(
                        titulo = "Registro general · 2 hojas",
                        descripcion = "$alcance · ${columnasRegistro.size} columna(s) · ${personas.size} personas",
                        color = CianRuralitos,
                        simbolo = "XLS",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item(key = "excel_acciones") {
                    TarjetaContenedoraRegistro(
                        titulo = "Exportación",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BotonSecundarioRuralitos(
                                texto = "Cambiar selección",
                                onClick = {
                                    seccion = SeccionDispensarizacion.INDICADORES
                                    panelIndicadorAbierto = PanelIndicador.RESUMEN_POBLACION
                                }
                            )

                            BotonPrincipalRuralitos(
                                texto = if (exportando) "Creando archivo…" else "Descargar Excel",
                                color = CianRuralitos,
                                enabled = !exportando && seleccionPoblacionCompleta,
                                onClick = {
                                    if (agrupacionesRegistro.isEmpty() || idsFicha.isEmpty()) {
                                        errorExportacion = "Selecciona una población con fichas para crear el Excel."
                                    } else if (agrupacionesRegistro.size > CalculadorRegistroComunitario.MAXIMO_COLUMNAS) {
                                        errorExportacion = "El archivo admite hasta ${CalculadorRegistroComunitario.MAXIMO_COLUMNAS} barrios o EAIS por descarga."
                                    } else {
                                        exportando = true
                                        errorExportacion = null
                                        scope.launch {
                                            val resultado = GestorDescargasFicha.generarRegistroGeneral(
                                                context = context,
                                                filtro = filtroActual,
                                                agrupaciones = agrupacionesRegistro
                                            )
                                            exportando = false
                                            archivoExportado = resultado.archivos.firstOrNull()
                                            errorExportacion = resultado.errores.firstOrNull()
                                        }
                                    }
                                }
                            )

                            errorExportacion?.let { mensaje ->
                                Text(
                                    mensaje,
                                    color = RojoClinico,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

    }
    }

    archivoExportado?.let { archivo ->
        AlertDialog(
            onDismissRequest = { archivoExportado = null },
            title = { Text("Registro general guardado") },
            text = {
                Text("El archivo se guardó en Descargas/Ruralitos. ¿Deseas compartirlo ahora?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        GestorDescargasFicha.compartir(context, listOf(archivo))
                        archivoExportado = null
                    }
                ) {
                    Text("Compartir ahora", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { archivoExportado = null }) {
                    Text("Conservar en el celular")
                }
            }
        )
    }
}

@Composable
private fun SelectorFichas(
    fichas: List<FichaFamiliarEntity>,
    miembros: List<MiembroFamiliaEntity>,
    seleccion: Long?,
    onSeleccion: (Long) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    var consulta by remember { mutableStateOf("") }
    val actual = fichas.firstOrNull { it.id == seleccion }
    val encontradas = buscarFichasPorIntegrante(fichas, miembros, consulta)
    Box(Modifier.fillMaxWidth().padding(top = 2.dp)) {
        BotonSelectorRuralitos(
            onClick = { abierto = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                actual?.let { "Ficha ${it.numeroFichaFamiliar} · ${it.nombreApellidoJefeFamilia}" }
                    ?: "Elegir ficha familiar",
                fontWeight = FontWeight.SemiBold
            )
        }
        MenuDesplegableRuralitos(expanded = abierto, onDismissRequest = { abierto = false }) {
            OutlinedTextField(
                value = consulta,
                onValueChange = { consulta = it },
                label = { Text("Buscar por nombre o cédula") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            )
            fichas.filter { it.id in encontradas }.take(50).forEach { ficha ->
                val coincidencia = miembros.firstOrNull { it.fichaId == ficha.id && consulta.isNotBlank() && normalizarBusqueda("${it.apellidosNombres} ${it.cedula}").contains(normalizarBusqueda(consulta)) }
                ItemMenuRuralitos(
                    text = {
                        Text(
                            "${if (seleccion == ficha.id) "✓ " else ""}Ficha ${ficha.numeroFichaFamiliar} · ${ficha.nombreApellidoJefeFamilia}${coincidencia?.let { " · ${it.apellidosNombres}" }.orEmpty()}",
                        )
                    },
                    onClick = { onSeleccion(ficha.id); abierto = false; consulta = "" }
                )
            }
        }
    }
}

@Composable
private fun SelectorTerritorios(
    territorios: List<TerritorioSalaEntity>,
    seleccion: String?,
    onSeleccion: (String) -> Unit
) = SelectorIdentificadoUnico(
    opciones = territorios.map { it.id to "${it.etiqueta}: ${it.nombre}" },
    seleccion = seleccion,
    textoVacio = "Elegir barrio",
    onSeleccion = onSeleccion
)

@Composable
private fun SelectorEais(
    eais: List<EaisSalaEntity>,
    seleccion: String?,
    onSeleccion: (String) -> Unit
) = SelectorIdentificadoUnico(
    opciones = eais.map { it.id to it.nombre },
    seleccion = seleccion,
    textoVacio = "Elegir EAIS",
    onSeleccion = onSeleccion
)

@Composable
private fun SelectorIdentificadoUnico(
    opciones: List<Pair<String, String>>,
    seleccion: String?,
    textoVacio: String,
    onSeleccion: (String) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    val etiquetaActual = opciones.firstOrNull { it.first == seleccion }?.second

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    ) {
        BotonSelectorRuralitos(
            onClick = { abierto = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                text = etiquetaActual ?: textoVacio,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
            )
        }

        MenuDesplegableRuralitos(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            opciones.forEach { (id, etiqueta) ->
                ItemMenuRuralitos(
                    text = {
                        Text(
                            text = "${if (id == seleccion) "✓ " else ""}$etiqueta",
                        )
                    },
                    onClick = {
                        onSeleccion(id)
                        abierto = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EncabezadoRegistroCurvo(
    titulo: String,
    descripcion: String,
    paso: String,
    onVolver: () -> Unit
) {
    EncabezadoPantallaRuralitos(
        titulo = titulo,
        subtitulo = paso,
        paso = null,
        totalPasos = null,
        etiquetaPaso = "",
        onVolver = onVolver,
        descripcion = descripcion
    )
}

@Composable
private fun BarraSeccionesDispensarizacion(
    seleccionada: SeccionDispensarizacion,
    onSeleccionar: (SeccionDispensarizacion) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeClinico.copy(alpha = 0.70f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SeccionDispensarizacion.entries.forEach { opcion ->
                val activa = opcion == seleccionada
                Surface(
                    onClick = { onSeleccionar(opcion) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (activa) AzulClinico else Color.Transparent,
                    border = if (activa) {
                        null
                    } else {
                        BorderStroke(1.dp, BordeClinico.copy(alpha = 0.36f))
                    },
                    shadowElevation = if (activa) 4.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconoSeccion(
                            seccion = opcion,
                            color = if (activa) Color.White else MoradoClinico
                        )
                        TextoAjustado(
                            texto = opcion.etiqueta,
                            color = if (activa) Color.White else MoradoClinico,
                            fontWeight = FontWeight.SemiBold,
                            tamano = 13.sp, tamanoMinimo = 7.sp,
                            modifier = Modifier.padding(start = 7.dp).weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconoSeccion(
    seccion: SeccionDispensarizacion,
    color: Color
) {
    Canvas(Modifier.size(20.dp)) {
        val trazo = 2.dp.toPx()
        when (seccion) {
            SeccionDispensarizacion.INDICADORES -> {
                drawLine(
                    color,
                    Offset(size.width * .10f, size.height * .78f),
                    Offset(size.width * .36f, size.height * .50f),
                    trazo,
                    StrokeCap.Round
                )
                drawLine(
                    color,
                    Offset(size.width * .36f, size.height * .50f),
                    Offset(size.width * .58f, size.height * .65f),
                    trazo,
                    StrokeCap.Round
                )
                drawLine(
                    color,
                    Offset(size.width * .58f, size.height * .65f),
                    Offset(size.width * .88f, size.height * .25f),
                    trazo,
                    StrokeCap.Round
                )
                listOf(
                    .10f to .78f,
                    .36f to .50f,
                    .58f to .65f,
                    .88f to .25f
                ).forEach { (x, y) ->
                    drawCircle(
                        color,
                        radius = 2.2.dp.toPx(),
                        center = Offset(size.width * x, size.height * y)
                    )
                }
            }

            SeccionDispensarizacion.PERSONAS -> {
                drawCircle(
                    color,
                    radius = size.width * .14f,
                    center = Offset(size.width * .38f, size.height * .31f),
                    style = Stroke(trazo)
                )
                drawCircle(
                    color,
                    radius = size.width * .12f,
                    center = Offset(size.width * .68f, size.height * .34f),
                    style = Stroke(trazo)
                )
                drawArc(
                    color,
                    200f,
                    140f,
                    false,
                    Offset(size.width * .10f, size.height * .52f),
                    androidx.compose.ui.geometry.Size(size.width * .55f, size.height * .42f),
                    style = Stroke(trazo)
                )
                drawArc(
                    color,
                    200f,
                    140f,
                    false,
                    Offset(size.width * .46f, size.height * .56f),
                    androidx.compose.ui.geometry.Size(size.width * .42f, size.height * .35f),
                    style = Stroke(trazo)
                )
            }

            SeccionDispensarizacion.EXCEL -> {
                drawRoundRect(
                    color,
                    Offset(size.width * .20f, size.height * .10f),
                    androidx.compose.ui.geometry.Size(size.width * .60f, size.height * .80f),
                    androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = Stroke(trazo)
                )
                drawLine(
                    color,
                    Offset(size.width * .30f, size.height * .42f),
                    Offset(size.width * .70f, size.height * .42f),
                    trazo
                )
                drawLine(
                    color,
                    Offset(size.width * .30f, size.height * .60f),
                    Offset(size.width * .70f, size.height * .60f),
                    trazo
                )
            }
        }
    }
}

@Composable
private fun TituloSeccionRegistro(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SelectorVistaPoblacion(
    seleccionada: VistaDispensarizacion,
    onSeleccionar: (VistaDispensarizacion) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VistaDispensarizacion.entries.forEach { opcion ->
            val activa = opcion == seleccionada
            Surface(
                onClick = { onSeleccionar(opcion) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (activa) AzulClinico else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (activa) AzulClinico else AzulClinico.copy(alpha = 0.18f)
                ),
                shadowElevation = if (activa) 4.dp else 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextoAjustado(
                        texto = opcion.etiqueta,
                        color = if (activa) Color.White else MoradoClinico,
                        fontWeight = FontWeight.SemiBold,
                        tamano = 13.sp, tamanoMinimo = 7.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaSeleccionActual(
    alcance: String,
    personas: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeClinico.copy(alpha = 0.8f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(AzulClinico, CircleShape)
            )
            Text(
                text = alcance,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$personas personas",
                color = AzulClinico,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TarjetaResumenPoblacion(
    titulo: String,
    fichas: Int,
    personas: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeClinico.copy(alpha = 0.70f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = AzulClinico.copy(alpha = 0.09f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = personas.toString(),
                        color = AzulClinico,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 13.dp)
            ) {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$fichas ficha(s) · $personas persona(s)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = personas.toString(),
                    color = AzulClinico,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Personas",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TarjetaContenedoraRegistro(
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeClinico.copy(alpha = 0.65f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            contenido()
        }
    }
}

@Composable
private fun TarjetaEstadoRegistro(
    titulo: String,
    descripcion: String,
    color: Color,
    simbolo: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(simbolo, color = color, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(titulo, color = color, fontWeight = FontWeight.SemiBold)
                Text(
                    descripcion,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AcordeonIndicador(
    titulo: String,
    subtitulo: String,
    color: Color,
    simbolo: String,
    expandido: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    icono: Int? = null,
    contenido: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f)),
        shadowElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(42.dp)
                        .background(color, RoundedCornerShape(50))
                )

                Surface(
                    modifier = Modifier
                        .padding(start = 11.dp)
                        .size(44.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.12f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (icono != null) {
                            Image(
                                painter = painterResource(icono),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = simbolo,
                                color = color,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (simbolo.length > 2) 11.sp else 14.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    TextoAjustado(
                        texto = titulo,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        tamano = MaterialTheme.typography.titleMedium.fontSize.takeIf { it.isSpecified } ?: 16.sp,
                        tamanoMinimo = 11.sp, maxLineas = 3
                    )
                    Text(
                        text = subtitulo,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.07f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Chevron(expandido = expandido, color = color)
                    }
                }
            }

            if (expandido) {
                androidx.compose.material3.HorizontalDivider(
                    color = BordeClinico.copy(alpha = 0.55f)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    contenido()
                }
            }
        }
    }
}
@Composable
private fun GraficoDonaRegistro(titulo: String, datos: List<Pair<String, Int>>) {
    val datosVisibles = datosGruposDona(datos)
    val total = datos.sumOf { it.second }
    val leyenda = datos.mapNotNull { (etiqueta, valor) ->
        val grupo = GrupoDispensarizacion.entries.firstOrNull { candidato ->
            candidato != GrupoDispensarizacion.PENDIENTE &&
                (etiqueta.equals("Grupo ${candidato.codigo}", ignoreCase = true) ||
                    etiqueta.equals(candidato.titulo, ignoreCase = true))
        }
        grupo?.let { Triple(etiqueta, valor, it) }
    }

    Text(
        titulo,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeClinico.copy(alpha = 0.65f)),
        shadowElevation = 1.dp
    ) {
        // La dona ocupa todo el ancho disponible y los grupos van debajo: así ninguna etiqueta se aprieta ni se corta.
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().widthIn(max = 260.dp).aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                    val ancho = 22.dp.toPx()
                    drawCircle(
                        color = BordeClinico.copy(alpha = 0.55f),
                        style = Stroke(width = ancho)
                    )
                    if (total > 0) {
                        var inicio = -90f
                        datosVisibles.forEach { (_, valor, grupo) ->
                            val angulo = 360f * valor / total
                            val barrido = (angulo - 2.2f).coerceAtLeast(0f)
                            drawArc(
                                color = colorGrupo(grupo),
                                startAngle = inicio + 1.1f,
                                sweepAngle = barrido,
                                useCenter = false,
                                style = Stroke(
                                    width = ancho,
                                    cap = StrokeCap.Round
                                )
                            )
                            inicio += angulo
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(0.58f),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, BordeClinico.copy(alpha = 0.55f)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TextoAjustado(
                            total.toString(), tamano = 34.sp, tamanoMinimo = 18.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                        )
                        TextoAjustado(
                            "personas", tamano = 13.sp, tamanoMinimo = 7.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                leyenda.forEach { (etiqueta, valor, grupo) ->
                    val porcentaje = if (total == 0) 0 else valor * 100 / total
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(11.dp),
                            shape = CircleShape,
                            color = colorGrupo(grupo)
                        ) {}
                        Text(
                            "$etiqueta · $valor",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colorGrupo(grupo).copy(alpha = 0.10f)
                        ) {
                            Text(
                                "$porcentaje%",
                                color = colorGrupo(grupo),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraficoBarrasRegistro(
    titulo: String,
    datos: List<Pair<String, Int>>,
    color: Color,
    mostrarTitulo: Boolean = true,
    iconos: List<Int> = emptyList()
) {
    val maximo = datos.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    if (mostrarTitulo) {
        Text(
            titulo,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
        )
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        datos.take(10).forEachIndexed { indice, (etiqueta, valor) ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BordeClinico.copy(alpha = 0.45f))
            ) {
                Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        iconos.getOrNull(indice)?.let {
                            Image(
                                painter = painterResource(it),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp).size(30.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Text(
                            etiqueta,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            valor.toString(),
                            color = color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(top = 3.dp)
                    ) {
                        Surface(
                            Modifier.fillMaxSize(),
                            color = color.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(50)
                        ) {}
                        Surface(
                            Modifier
                                .fillMaxWidth(valor.toFloat() / maximo)
                                .fillMaxSize(),
                            color = color,
                            shape = RoundedCornerShape(50)
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaGrupo(
    grupo: GrupoDispensarizacion,
    personasGrupo: List<PersonaDispensarizada>,
    total: Int
) {
    val cantidad = personasGrupo.size
    val color = colorGrupo(grupo)
    val porcentaje = if (total == 0) 0 else cantidad * 100 / total
    var expandido by remember(grupo) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f)),
        shadowElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(44.dp)
                        .background(color, RoundedCornerShape(50))
                )

                Surface(
                    modifier = Modifier
                        .padding(start = 11.dp)
                        .size(44.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.12f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = grupo.codigo,
                            color = color,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    // El título usa todo el ancho que queda y baja de línea por palabras; si una palabra no cabe, la letra se achica.
                    TextoAjustado(
                        texto = "Grupo ${grupo.codigo} · ${grupo.titulo}",
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        tamano = 16.sp, tamanoMinimo = 11.sp, maxLineas = 3
                    )
                    // La cantidad y el porcentaje van debajo, así no le quitan espacio al título.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$cantidad persona(s)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = color.copy(alpha = 0.09f),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.10f))
                        ) {
                            Text(
                                text = "$porcentaje%",
                                color = color,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.06f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Chevron(expandido = expandido, color = color)
                    }
                }
            }

            if (expandido) {
                androidx.compose.material3.HorizontalDivider(
                    color = color.copy(alpha = 0.12f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color.copy(alpha = 0.025f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (grupo == GrupoDispensarizacion.IV) {
                        Text(
                            text = "Por tipo de discapacidad",
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )

                        val tipos = listOf(
                            Triple(
                                "Visual",
                                IconosMais.DISCAPACIDAD_VISUAL,
                                personasGrupo.count { it.miembro.discapacidadVisual == true }
                            ),
                            Triple(
                                "Auditiva",
                                IconosMais.DISCAPACIDAD_AUDITIVA,
                                personasGrupo.count { it.miembro.discapacidadAuditiva == true }
                            ),
                            Triple(
                                "Del lenguaje",
                                IconosMais.DISCAPACIDAD_LENGUAJE,
                                personasGrupo.count { it.miembro.discapacidadLenguaje == true }
                            ),
                            Triple(
                                "Física",
                                IconosMais.DISCAPACIDAD_FISICA,
                                personasGrupo.count {
                                    it.miembro.discapacidadFisica == true &&
                                        it.miembro.necesitaAyudaTecnica != true
                                }
                            ),
                            Triple(
                                "Física con apoyo",
                                IconosMais.DISCAPACIDAD_FISICA_APOYO,
                                personasGrupo.count {
                                    it.miembro.discapacidadFisica == true &&
                                        it.miembro.necesitaAyudaTecnica == true
                                }
                            ),
                            Triple(
                                "Intelectual",
                                IconosMais.DISCAPACIDAD_INTELECTUAL,
                                personasGrupo.count { it.miembro.discapacidadIntelectual == true }
                            ),
                            Triple(
                                "Psicosocial",
                                IconosMais.SALUD_MENTAL,
                                personasGrupo.count { it.miembro.discapacidadPsicosocial == true }
                            )
                        )

                        tipos.forEach { (etiqueta, iconoId, personasTipo) ->
                            FilaDetalleGrupo(
                                etiqueta = etiqueta,
                                valor = personasTipo,
                                color = color,
                                icono = PictogramaDispensarizacion(
                                    iconoId,
                                    "Discapacidad $etiqueta"
                                )
                            )
                        }
                    } else if (grupo != GrupoDispensarizacion.PENDIENTE) {
                        Text(
                            text = "Por edad y condición",
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )

                        categoriasResumenGrupo(grupo).forEach { categoria ->
                            val icono = DispensarizacionAutomatica.iconoGrupoEdad(grupo, categoria)
                            val personasEdad = personasGrupo.count {
                                it.categoriaEdad == categoria ||
                                    (
                                        grupo == GrupoDispensarizacion.I &&
                                            categoria == GrupoEdadRiesgo.EMBARAZADA &&
                                            it.categoriaEdad == GrupoEdadRiesgo.EMBARAZADA_ADOLESCENTE
                                        )
                            }

                            FilaDetalleGrupo(
                                etiqueta = categoria.etiqueta,
                                valor = personasEdad,
                                color = color,
                                icono = icono
                            )
                        }

                        val sinEdad = personasGrupo.count { it.categoriaEdad == null }
                        if (sinEdad > 0) {
                            Text(
                                text = "Edad sin registrar · $sinEdad",
                                color = NaranjaClinico
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDetalleGrupo(
    etiqueta: String,
    valor: Int,
    color: Color,
    icono: PictogramaDispensarizacion?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (icono != null) {
                IconoMais(icono, Modifier.size(32.dp))
            } else {
                Box(Modifier.size(32.dp))
            }
            Text(etiqueta, modifier = Modifier.weight(1f))
            Text(valor.toString(), color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Chevron(
    expandido: Boolean,
    color: Color
) {
    Canvas(Modifier.size(18.dp)) {
        val y1 = if (expandido) size.height * .62f else size.height * .38f
        val y2 = if (expandido) size.height * .38f else size.height * .62f
        drawLine(
            color,
            Offset(size.width * .18f, y1),
            Offset(size.width * .50f, y2),
            2.dp.toPx(),
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(size.width * .50f, y2),
            Offset(size.width * .82f, y1),
            2.dp.toPx(),
            StrokeCap.Round
        )
    }
}

@Composable
private fun TarjetaPersonaDispensarizada(
    persona: PersonaDispensarizada,
    onAbrirFicha: (FichaFamiliarEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val grupo = persona.resultado.grupo
    val color = colorGrupo(grupo)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, BordeClinico.copy(alpha = 0.70f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.09f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            grupo.codigo,
                            color = color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        persona.nombre.ifBlank { "Integrante sin nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${persona.parentesco} · Ficha ${persona.ficha.numeroFichaFamiliar}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = color.copy(alpha = 0.09f)
                ) {
                    Text(
                        "Grupo ${grupo.codigo}",
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }

            persona.categoriaEdad?.let { categoria ->
                Text(
                    "Edad/condición: ${categoria.etiqueta}",
                    color = color,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (persona.resultado.pictogramas.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    persona.resultado.pictogramas.forEach { pictograma ->
                        IconoMais(pictograma, Modifier.size(44.dp))
                    }
                }
                Text(
                    persona.resultado.pictogramas.joinToString(" · ") { it.etiqueta },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 7.dp)
                )
            }

            persona.resultado.razones.forEach { razon ->
                Text(
                    "• $razon",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            if (persona.resultado.camposPendientes.isNotEmpty()) {
                Text(
                    "Pendiente: ${persona.resultado.camposPendientes.joinToString()}",
                    color = NaranjaClinico,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 9.dp)
                )
            }

            TextButton(
                onClick = { onAbrirFicha(persona.ficha) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp)
            ) {
                Text(
                    "Abrir ficha familiar",
                    color = AzulClinico,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun colorGrupo(grupo: GrupoDispensarizacion): Color = when (grupo) {
    GrupoDispensarizacion.PENDIENTE -> Color(0xFF607D8B)
    GrupoDispensarizacion.I -> CianRuralitos
    GrupoDispensarizacion.II -> NaranjaClinico
    GrupoDispensarizacion.III -> RojoClinico
    GrupoDispensarizacion.IV -> AzulClinico
}

private fun coincideTerritorio(ficha: FichaFamiliarEntity, territorio: TerritorioSalaEntity): Boolean =
    ficha.barrio.ifBlank { ficha.comunidad }.equals(territorio.nombre, ignoreCase = true)

private fun normalizarBusqueda(texto: String): String = Normalizer.normalize(
    texto.trim().lowercase(Locale.getDefault()),
    Normalizer.Form.NFD
).replace("\\p{Mn}+".toRegex(), "")

internal fun buscarFichasPorIntegrante(
    fichas: List<FichaFamiliarEntity>,
    miembros: List<MiembroFamiliaEntity>,
    consulta: String
): Set<Long> {
    val buscado = normalizarBusqueda(consulta)
    if (buscado.isBlank()) return fichas.mapTo(mutableSetOf()) { it.id }
    val porIntegrante = miembros.asSequence()
        .filter { normalizarBusqueda("${it.apellidosNombres} ${it.cedula}").contains(buscado) }
        .mapTo(mutableSetOf()) { it.fichaId }
    fichas.forEach { ficha ->
        if (normalizarBusqueda("${ficha.nombreApellidoJefeFamilia} ${ficha.cedulaJefeHogar} ${ficha.numeroFichaFamiliar}").contains(buscado)) {
            porIntegrante += ficha.id
        }
    }
    return porIntegrante
}
