package com.ruralitos.app

import android.app.Application
import com.ruralitos.app.data.diagnostico.RegistroErrores

class RuralitosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RegistroErrores.instalar(this)
    }
}
