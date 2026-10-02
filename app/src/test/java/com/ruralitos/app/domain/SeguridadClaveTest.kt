package com.ruralitos.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeguridadClaveTest {
    @Test
    fun `acepta la clave correcta y rechaza una incorrecta`() {
        val protegida = SeguridadClave.proteger("Ruralitos-2026")

        assertTrue(SeguridadClave.verificar("Ruralitos-2026", protegida.hash, protegida.salt))
        assertFalse(SeguridadClave.verificar("otra-clave", protegida.hash, protegida.salt))
    }

    @Test
    fun `dos usuarios con la misma clave reciben valores distintos`() {
        val primera = SeguridadClave.proteger("misma-clave")
        val segunda = SeguridadClave.proteger("misma-clave")

        assertNotEquals(primera.salt, segunda.salt)
        assertNotEquals(primera.hash, segunda.hash)
    }
}
