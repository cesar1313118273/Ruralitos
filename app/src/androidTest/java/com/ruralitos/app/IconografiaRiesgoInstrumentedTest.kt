package com.ruralitos.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.domain.IconosMais
import com.ruralitos.app.ui.components.RecursosIconografiaMais
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconografiaRiesgoInstrumentedTest {
    @Test
    fun todosLosPictogramasSePuedenDibujarSinInternet() {
        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        IconosMais.todos.forEach { id ->
            assertNotNull("Falta recurso para $id", RecursosIconografiaMais.recurso(id))
            val imagen = RecursosIconografiaMais.bitmap(contexto, id, 42)
            assertNotNull("No se pudo decodificar $id", imagen)
            assertTrue("Dimensión incorrecta para $id", imagen!!.width == 42 && imagen.height == 42)
            imagen.recycle()
        }
    }
}
