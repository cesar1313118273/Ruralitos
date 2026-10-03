package com.ruralitos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clase de ancho de la ventana (no del teléfono): cambia al girar, al abrir en pantalla dividida o en un plegable.
 * Sigue los cortes de Material: menos de 600 dp (teléfono), 600–839 dp (tableta vertical, plegable) y 840 dp o más.
 */
enum class ClaseAncho {
    COMPACTA, MEDIA, EXPANDIDA;

    companion object {
        fun de(ancho: Dp): ClaseAncho = when {
            ancho < 600.dp -> COMPACTA
            ancho < 840.dp -> MEDIA
            else -> EXPANDIDA
        }
    }
}

val LocalClaseAncho = compositionLocalOf { ClaseAncho.COMPACTA }

/** Número de columnas según el ancho disponible. */
@Composable
fun columnasAdaptables(compacta: Int = 1, media: Int = 2, expandida: Int = 2): Int =
    when (LocalClaseAncho.current) {
        ClaseAncho.COMPACTA -> compacta
        ClaseAncho.MEDIA -> media
        ClaseAncho.EXPANDIDA -> expandida
    }

/**
 * Reparte [items] en [columnas] columnas de igual ancho dentro de una columna que ya se desplaza
 * (no es perezosa: úsala para pocos elementos, como accesos o tarjetas de resumen).
 */
@Composable
fun <T> CuadriculaAdaptable(
    items: List<T>,
    columnas: Int,
    modifier: Modifier = Modifier,
    espacio: Dp = 12.dp,
    contenido: @Composable (T, Modifier) -> Unit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(espacio)) {
        items.chunked(columnas.coerceAtLeast(1)).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(espacio)) {
                fila.forEach { contenido(it, Modifier.weight(1f)) }
                repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Lista perezosa que pasa a 2 columnas en pantallas anchas. Los elementos que deben ocupar todo el ancho
 * (encabezados, botones, avisos) se declaran con [itemCompleto].
 */
@Composable
fun ListaAdaptable(
    modifier: Modifier = Modifier,
    columnasMedia: Int = 1,
    columnasExpandida: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    espacio: Dp = 12.dp,
    contenido: LazyGridScope.() -> Unit
) {
    val columnas = columnasAdaptables(1, columnasMedia, columnasExpandida)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnas),
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(espacio),
        horizontalArrangement = Arrangement.spacedBy(espacio),
        content = contenido
    )
}

fun LazyGridScope.itemCompleto(key: Any? = null, contenido: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { contenido() }
}

// ---- listas que se reparten en columnas sin cambiar el código de cada pantalla -------------------

private class Celda(val clave: Any?, val tarjeta: Boolean, val contenido: @Composable LazyItemScope.() -> Unit)

/** Anota lo que una pantalla declara con `item {}` / `items(...)` para poder repartirlo después. */
private class RegistroDeLista : LazyListScope {
    val celdas = mutableListOf<Celda>()

    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        celdas += Celda(key, tarjeta = false, content)
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    ) {
        repeat(count) { indice ->
            celdas += Celda(key?.invoke(indice), tarjeta = true) { itemContent(indice) }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @androidx.compose.foundation.ExperimentalFoundationApi
    override fun stickyHeader(key: Any?, contentType: Any?, content: @Composable LazyItemScope.(Int) -> Unit) {
        celdas += Celda(key, tarjeta = false) { content(0) }
    }
}

/**
 * Como un `LazyColumn`, pero en pantallas medianas y anchas las tarjetas (lo declarado con `items`) se colocan de a
 * [columnas] por fila. Lo declarado con `item` (encabezados, botones, avisos) ocupa todo el ancho.
 */
@Composable
fun ListaDeColumnasAdaptable(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    espacio: Dp = 12.dp,
    contenido: LazyListScope.() -> Unit
) {
    val columnas = columnasAdaptables(compacta = 1, media = 2, expandida = 2)
    if (columnas <= 1) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(espacio),
            content = contenido
        )
        return
    }
    val registro = RegistroDeLista().apply(contenido)
    // Las tarjetas contiguas se agrupan en filas; cualquier otra cosa va sola.
    val filas = buildList<List<Celda>> {
        var pendientes = mutableListOf<Celda>()
        fun cerrar() { if (pendientes.isNotEmpty()) { add(pendientes); pendientes = mutableListOf() } }
        registro.celdas.forEach { celda ->
            if (celda.tarjeta) {
                pendientes += celda
                if (pendientes.size == columnas) cerrar()
            } else {
                cerrar()
                add(listOf(celda))
            }
        }
        cerrar()
    }
    androidx.compose.foundation.lazy.LazyColumn(
        modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(espacio)
    ) {
        items(filas.size, key = { indice ->
            // Una clave repetida haría fallar la lista; se usa la de la primera tarjeta más la posición.
            "${filas[indice].first().clave ?: "f"}#$indice"
        }) { indice ->
            val fila = filas[indice]
            val alcance = this
            if (fila.size == 1 && !fila.first().tarjeta) {
                fila.first().contenido(alcance)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(espacio)) {
                    fila.forEach { celda ->
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { celda.contenido(alcance) }
                    }
                    repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
