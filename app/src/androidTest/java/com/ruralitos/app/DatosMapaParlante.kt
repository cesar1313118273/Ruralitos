package com.ruralitos.app

import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/** Fichas de ejemplo para probar el mapa parlante: dos barrios con personas de distintas edades y condiciones. */
object DatosMapaParlante {
    const val PREFIJO = "QA-PARLANTE-"

    private fun nacimiento(anios: Int, meses: Int = 0): String {
        val c = Calendar.getInstance().apply { add(Calendar.YEAR, -anios); add(Calendar.MONTH, -meses); add(Calendar.DAY_OF_MONTH, -3) }
        return SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(c.time)
    }

    private fun ficha(nombre: String, barrio: String, lat: Double?, lon: Double?) = FichaFamiliarEntity(
        cedulaJefeHogar = "09${UUID.randomUUID().toString().filter { it.isDigit() }.take(8).padEnd(8, '0')}",
        institucionSistema = "", unidadOperativa = "QA", codigoUo = "1", areaNumero = "1", codigoLocalizacion = "1",
        parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1", provinciaCodigoLocalizacion = "1",
        numeroFichaFamiliar = "$PREFIJO${UUID.randomUUID()}", provincia = "P", canton = "C", parroquia = "R",
        sector = "S", manzana = "1", numeroFamilia = "1", direccionHabitualFamilia = "D", barrio = barrio,
        numeroCasa = "1", comunidad = "C", grupoCultural = "MESTIZO", nombreApellidoJefeFamilia = nombre,
        numeroTelefono = "0", fechaLlenado = "01/10/2026", numeroCarpeta = "1", responsableNombre = "QA",
        responsableCodigo = "1", latitud = lat, longitud = lon, estado = "COMPLETA", syncEstado = "SINCRONIZADO"
    )

    private fun persona(
        ficha: Long, nombre: String, anios: Int, meses: Int = 0, sexo: String = "M",
        hta: Boolean = false, dm: Boolean = false, visual: Boolean = false
    ) = MiembroFamiliaEntity(
        fichaId = ficha, grupoEdad = "", apellidosNombres = nombre, parentesco = "", fechaNacimiento = nacimiento(anios, meses),
        ocupacion = "", sexo = sexo, escolaridad = "", vacunasCompletas = true, saludBucalAdecuada = true,
        estadoNutricional = "SIN_ALTERACION", hipertensionArterial = hta, diabetesMellitus = dm,
        tuberculosis = false, problemaSaludMental = false, consumoAlcoholDrogas = false, enfermedadCronica = false,
        discapacidadVisual = visual
    )

    /** Cerezal: 4 familias en el mismo sector; El Carmen: 1 familia. Devuelve el id de cada ficha por clave. */
    suspend fun sembrar(database: RuralitosDatabase): Map<String, Long> {
        val sync = database.sincronizacionDao()
        val contenido = database.fichaContenidoDao()
        val ids = mutableMapOf<String, Long>()
        ids["c1"] = sync.guardarFichaRemota(ficha("RAMÓN LUIS", "Cerezal", -0.2201, -78.5123))
        ids["c2"] = sync.guardarFichaRemota(ficha("TORRES ANA", "Cerezal", -0.2160, -78.5080))
        ids["c3"] = sync.guardarFichaRemota(ficha("PÉREZ JUAN", "Cerezal", -0.2185, -78.5150))
        ids["c4"] = sync.guardarFichaRemota(ficha("SIN PUNTO MARÍA", "Cerezal", null, null))
        ids["e1"] = sync.guardarFichaRemota(ficha("GÓMEZ PEDRO", "El Carmen", -0.2033, -78.4907))

        contenido.guardarMiembro(persona(ids.getValue("c1"), "RAMÓN LUIS", 66, hta = true))
        contenido.guardarMiembro(persona(ids.getValue("c1"), "RAMÓN SOFÍA", 63, sexo = "F", dm = true))
        contenido.guardarMiembro(persona(ids.getValue("c1"), "RAMÓN NIÑO", 0, meses = 14))
        contenido.guardarMiembro(persona(ids.getValue("c2"), "TORRES ANA", 70, sexo = "F", hta = true, dm = true))
        contenido.guardarMiembro(persona(ids.getValue("c2"), "TORRES LUCÍA", 24, sexo = "F"))
        contenido.guardarEmbarazada(
            EmbarazadaEntity(
                fichaId = ids.getValue("c2"), apellidosNombres = "TORRES LUCÍA",
                fechaUltimaMenstruacion = nacimiento(0, 4), fechaProbableParto = nacimiento(0, -5), semanasGestacion = 16
            )
        )
        contenido.guardarMiembro(persona(ids.getValue("c3"), "PÉREZ JUAN", 40, hta = true))
        contenido.guardarMiembro(persona(ids.getValue("c3"), "PÉREZ NIÑA", 7, sexo = "F", visual = true))
        contenido.guardarMiembro(persona(ids.getValue("c3"), "PÉREZ ABUELO", 82, hta = true))
        contenido.guardarMiembro(persona(ids.getValue("c4"), "SIN PUNTO MARÍA", 35, sexo = "F"))
        contenido.guardarMiembro(persona(ids.getValue("e1"), "GÓMEZ PEDRO", 50, dm = true))
        return ids
    }

    fun limpiar(database: RuralitosDatabase) {
        database.openHelper.writableDatabase.execSQL("DELETE FROM fichas_familiares WHERE numeroFichaFamiliar LIKE '$PREFIJO%'")
    }
}
