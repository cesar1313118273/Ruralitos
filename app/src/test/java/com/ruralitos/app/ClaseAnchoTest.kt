package com.ruralitos.app

import androidx.compose.ui.unit.dp
import com.ruralitos.app.ui.components.ClaseAncho
import org.junit.Assert.assertEquals
import org.junit.Test

class ClaseAnchoTest {
    @Test
    fun usaLosCortesDeMaterial() {
        assertEquals(ClaseAncho.COMPACTA, ClaseAncho.de(320.dp))
        assertEquals(ClaseAncho.COMPACTA, ClaseAncho.de(599.dp))
        assertEquals(ClaseAncho.MEDIA, ClaseAncho.de(600.dp))
        assertEquals(ClaseAncho.MEDIA, ClaseAncho.de(839.dp))
        assertEquals(ClaseAncho.EXPANDIDA, ClaseAncho.de(840.dp))
        assertEquals(ClaseAncho.EXPANDIDA, ClaseAncho.de(1180.dp))
    }
}
