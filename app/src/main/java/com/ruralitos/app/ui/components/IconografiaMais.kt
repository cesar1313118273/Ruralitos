package com.ruralitos.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ruralitos.app.R
import com.ruralitos.app.domain.IconosMais
import com.ruralitos.app.domain.PictogramaDispensarizacion

/**
 * Lámina MAIS histórica y figuras de riesgo/edad aportadas para Ruralitos.
 * Se incluyen en la aplicación para que nunca dependan de internet ni de la fuente
 * de emojis instalada en el teléfono.
 */
object RecursosIconografiaMais {
    private val recursos = mapOf(
        IconosMais.DISCAPACIDAD_VISUAL to R.drawable.mais_discapacidad_visual,
        IconosMais.DISCAPACIDAD_LENGUAJE to R.drawable.mais_discapacidad_lenguaje,
        IconosMais.DISCAPACIDAD_AUDITIVA to R.drawable.riesgo_iv_auditiva,
        IconosMais.DISCAPACIDAD_FISICA to R.drawable.riesgo_iv_fisica,
        IconosMais.DISCAPACIDAD_FISICA_APOYO to R.drawable.riesgo_iv_apoyo,
        IconosMais.DISCAPACIDAD_INTELECTUAL to R.drawable.riesgo_iv_intelectual,
        IconosMais.SALUD_MENTAL to R.drawable.riesgo_salud_mental,
        IconosMais.EMBARAZO_BAJO_RIESGO to R.drawable.mais_embarazo_bajo_riesgo,
        IconosMais.EMBARAZO_ALTO_RIESGO to R.drawable.mais_embarazo_alto_riesgo,
        IconosMais.EMBARAZO_RIESGO to R.drawable.mais_embarazo_riesgo,
        IconosMais.DIABETES to R.drawable.riesgo_diabetes,
        IconosMais.HIPERTENSION_DIABETES to R.drawable.mais_hipertension_diabetes,
        IconosMais.MENOR_DOS to R.drawable.mais_menor_dos,
        IconosMais.OBESIDAD_MENOR_CINCO to R.drawable.mais_obesidad_menor_cinco,
        IconosMais.OBESIDAD_CINCO_ONCE to R.drawable.mais_obesidad_cinco_once,
        IconosMais.DESNUTRICION_AGUDA to R.drawable.mais_desnutricion_aguda,
        IconosMais.DESNUTRICION_CRONICA to R.drawable.riesgo_desnutricion_cronica,
        IconosMais.HIPERTENSION to R.drawable.riesgo_hipertension,
        IconosMais.TUBERCULOSIS to R.drawable.riesgo_tuberculosis,
        IconosMais.CUIDADOS_PALIATIVOS to R.drawable.riesgo_paliativos,
        IconosMais.VIH to R.drawable.riesgo_vih,
        IconosMais.OBESIDAD_ADOLESCENTE to R.drawable.mais_obesidad_adolescente,
        IconosMais.OBESIDAD_ADULTO to R.drawable.mais_obesidad_adulto,
        IconosMais.OBESIDAD_ADULTO_MAYOR to R.drawable.mais_obesidad_adulto_mayor,
        IconosMais.VACUNACION_INCOMPLETA to R.drawable.mais_vacunacion_incompleta,
        IconosMais.CONSUMO_ALCOHOL_DROGAS to R.drawable.mais_consumo_alcohol_drogas,
        IconosMais.SANEAMIENTO_AMBIENTAL to R.drawable.mais_saneamiento_ambiental,
        IconosMais.RIESGO_I_MENOR_DOS to R.drawable.riesgo_i_0_23,
        IconosMais.RIESGO_I_DOS_NUEVE to R.drawable.riesgo_i_2_9,
        IconosMais.RIESGO_I_EMBARAZO to R.drawable.riesgo_i_embarazo,
        IconosMais.RIESGO_II_MENOR_DOS to R.drawable.riesgo_ii_0_23,
        IconosMais.RIESGO_II_DOS_NUEVE to R.drawable.riesgo_ii_2_9,
        IconosMais.RIESGO_II_ADOLESCENTE to R.drawable.riesgo_ii_10_19,
        IconosMais.RIESGO_II_ADULTO to R.drawable.riesgo_ii_20_64,
        IconosMais.RIESGO_II_ADULTO_MAYOR to R.drawable.riesgo_ii_65_mas,
        IconosMais.RIESGO_II_EMBARAZO to R.drawable.riesgo_ii_embarazo,
        IconosMais.RIESGO_III_MENOR_DOS to R.drawable.riesgo_iii_0_23,
        IconosMais.RIESGO_III_DOS_NUEVE to R.drawable.riesgo_iii_2_9,
        IconosMais.RIESGO_III_ADOLESCENTE to R.drawable.riesgo_iii_10_19,
        IconosMais.RIESGO_III_ADULTO to R.drawable.riesgo_iii_20_64,
        IconosMais.RIESGO_III_ADULTO_MAYOR to R.drawable.riesgo_iii_65_mas,
        IconosMais.RIESGO_III_EMBARAZO to R.drawable.riesgo_iii_embarazo,
        IconosMais.RIESGO_III_EMBARAZO_ADOLESCENTE to R.drawable.riesgo_iii_embarazo_adolescente
    )

    @DrawableRes
    fun recurso(id: String): Int? = recursos[id]

    fun bitmap(context: Context, id: String, tamanoPx: Int): Bitmap? {
        val recurso = recurso(id) ?: return null
        val original = BitmapFactory.decodeResource(context.resources, recurso) ?: return null
        if (original.width == tamanoPx && original.height == tamanoPx) return original
        return Bitmap.createScaledBitmap(original, tamanoPx, tamanoPx, true).also {
            if (it !== original) original.recycle()
        }
    }
}

@Composable
fun IconoMais(
    pictograma: PictogramaDispensarizacion,
    modifier: Modifier = Modifier
) {
    val recurso = RecursosIconografiaMais.recurso(pictograma.id) ?: return
    Image(
        painter = painterResource(recurso),
        contentDescription = pictograma.etiqueta,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
