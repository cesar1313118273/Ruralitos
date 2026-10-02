package com.ruralitos.app

import com.ruralitos.app.data.remote.ErrorSupabase
import com.ruralitos.app.data.sync.errorSincronizacionReintentable
import java.io.FileNotFoundException
import java.net.UnknownHostException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizacionPolicyTest {
    @Test fun reintentaFallosDeRedYServidor() {
        assertTrue(errorSincronizacionReintentable(UnknownHostException("sin red")))
        assertTrue(errorSincronizacionReintentable(ErrorSupabase("temporal", 503)))
        assertTrue(errorSincronizacionReintentable(ErrorSupabase("límite", 429)))
    }

    @Test fun noReintentaErroresPermanentes() {
        assertFalse(errorSincronizacionReintentable(ErrorSupabase("sin permiso", 403)))
        assertFalse(errorSincronizacionReintentable(ErrorSupabase("datos inválidos", 400)))
        assertFalse(errorSincronizacionReintentable(FileNotFoundException("adjunto ausente")))
    }
}
