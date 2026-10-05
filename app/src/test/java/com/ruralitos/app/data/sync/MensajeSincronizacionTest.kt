package com.ruralitos.app.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class MensajeSincronizacionTest {
    private fun mensaje(
        conflictos: Int = 0, pendientes: Int = 0, enCurso: Boolean = false, internet: Boolean = true,
        fallos: Int = 0, correcto: Boolean? = true
    ) = EstadoSincronizacion.mensaje(conflictos, pendientes, enCurso, internet, fallos, correcto)

    @Test
    fun sinNadaPendienteYConInternetDiceSincronizado() {
        assertEquals("Sincronizado", mensaje())
    }

    @Test
    fun unFalloAisladoNoSeMuestraComoError() {
        assertEquals("Sincronizado", mensaje(fallos = 1, correcto = false))
    }

    @Test
    fun soloAvisaDeErrorCuandoSeRepite() {
        assertEquals("No se pudo sincronizar · datos guardados, se reintentará solo", mensaje(fallos = 2, correcto = false))
    }

    @Test
    fun mientrasSubeDiceSincronizandoYNoPendiente() {
        assertEquals("Sincronizando 3 cambios…", mensaje(pendientes = 3, enCurso = true))
        assertEquals("Sincronizando 1 cambio…", mensaje(pendientes = 1, enCurso = true))
        assertEquals("2 pendientes de sincronizar", mensaje(pendientes = 2))
    }

    @Test
    fun unaSincronizacionSinNadaQueSubirNoCambiaElMensaje() {
        assertEquals("Sincronizado", mensaje(enCurso = true))
    }

    @Test
    fun sinInternetLoDiceClaro() {
        assertEquals("Sin conexión · datos guardados en el teléfono", mensaje(internet = false))
        assertEquals("Sin conexión · 4 pendientes de sincronizar", mensaje(pendientes = 4, internet = false))
    }

    @Test
    fun losConflictosVanPrimero() {
        assertEquals("2 cambios por revisar", mensaje(conflictos = 2, pendientes = 5, enCurso = true))
    }
}
