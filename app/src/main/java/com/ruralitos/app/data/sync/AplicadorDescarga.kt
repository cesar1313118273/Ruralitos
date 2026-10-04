package com.ruralitos.app.data.sync

import androidx.room.withTransaction
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.GestionRiesgoEntity
import com.ruralitos.app.data.local.entity.HistorialFichaEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.MortalidadFamiliarEntity
import com.ruralitos.app.data.local.entity.ValorRiesgoEntity
import org.json.JSONObject

/**
 * Todo lo que el servidor dice de UNA ficha, ya traído a memoria. Las filas pueden venir marcadas como
 * eliminadas (descarga incremental); en una descarga completa solo traen filas vigentes.
 */
internal class DatosFichaRemota(
    val syncId: String,
    /** Fila de la ficha. Nula si en esta descarga solo cambiaron sus datos hijos. */
    val cabecera: JSONObject?,
    val miembros: List<JSONObject> = emptyList(),
    val embarazadas: List<JSONObject> = emptyList(),
    val mortalidad: List<JSONObject> = emptyList(),
    val calificaciones: List<JSONObject> = emptyList(),
    val valores: List<JSONObject> = emptyList(),
    val gestiones: List<JSONObject> = emptyList(),
    val contaminaciones: List<JSONObject> = emptyList(),
    val lugares: List<JSONObject> = emptyList(),
    val adjuntos: List<JSONObject> = emptyList(),
    val historial: List<JSONObject> = emptyList(),
    /** Ruta local de cada archivo ya descargado, por identificador de adjunto. */
    val archivosAdjuntos: Map<String, String> = emptyMap(),
    val firmaUri: String? = null
)

/**
 * Aplica la descarga de una ficha en UNA transacción de la base: o queda todo (ficha + datos hijos + estado
 * SINCRONIZADO) o no cambia nada. Mientras dura la transacción nadie más puede escribir, así que ninguna edición
 * del profesional puede quedar «a medias» marcada como sincronizada.
 */
internal class AplicadorDescarga(
    private val database: RuralitosDatabase,
    private val borrarArchivoLocal: (String) -> Unit
) {
    private val dao = database.sincronizacionDao()

    /** Estados que indican cambios del teléfono que aún no se han subido: nunca se pisan al descargar. */
    private fun conCambiosPropios(estado: String) = estado == "PENDIENTE" || estado == "ERROR" || estado == "CONFLICTO"

    /** Verdadero si la ficha se aplicó; falso si se omitió por tener cambios propios sin subir. */
    suspend fun aplicar(
        organizacionId: String,
        datos: DatosFichaRemota,
        completo: Boolean,
        forzar: Boolean = false
    ): Boolean = database.withTransaction {
        val existente = dao.fichaPorSyncId(datos.syncId)
        if (!forzar && existente != null && conCambiosPropios(existente.syncEstado)) return@withTransaction false
        val cabecera = datos.cabecera
        if (cabecera == null && existente == null) return@withTransaction false

        val fichaId: Long
        val version: Long
        if (cabecera != null) {
            val entidad = entidadFicha(cabecera, organizacionId, existente, datos.firmaUri)
            if (existente == null) {
                fichaId = dao.guardarFichaRemota(entidad)
            } else {
                dao.actualizarFichaRemota(entidad)
                fichaId = existente.id
            }
            version = cabecera.optLong("version", 1L)
            if (existente != null && datos.firmaUri == null) {
                existente.firmaUri?.let(borrarArchivoLocal)
            }
        } else {
            fichaId = existente!!.id
            version = existente.syncVersion
            dao.marcarDescargando(fichaId)
        }

        aplicarMiembros(fichaId, datos.miembros, completo)
        aplicarEmbarazadas(fichaId, datos.embarazadas, completo)
        aplicarMortalidad(fichaId, datos.mortalidad, completo)
        aplicarRiesgos(fichaId, datos.calificaciones, datos.valores, completo)
        aplicarGestiones(fichaId, datos.gestiones, completo)
        aplicarContaminaciones(fichaId, datos.contaminaciones, completo)
        aplicarLugares(fichaId, datos.lugares, completo)
        aplicarAdjuntos(fichaId, datos.adjuntos, datos.archivosAdjuntos, completo)
        aplicarHistorial(fichaId, datos.historial)
        dao.descartarTumbaRemota(datos.syncId)
        dao.marcarSincronizadaDescarga(fichaId, version)
        true
    }

    /** Elimina del teléfono las fichas que el servidor ya dio de baja (solo las que no tienen cambios propios). */
    suspend fun eliminarFichas(organizacionId: String, syncIds: Set<String>) {
        if (syncIds.isEmpty()) return
        dao.fichasLocalesSincronizadas(organizacionId)
            .filter { it.syncId in syncIds }
            .forEach { ficha ->
                database.withTransaction {
                    val actual = dao.fichaPorSyncId(ficha.syncId)
                    if (actual == null || actual.syncEstado != "SINCRONIZADO") return@withTransaction
                    val idsHijos = buildList {
                        addAll(dao.miembros(ficha.id).map { it.syncId })
                        addAll(dao.embarazadas(ficha.id).map { it.syncId })
                        addAll(dao.mortalidad(ficha.id).map { it.syncId })
                        dao.calificaciones(ficha.id).forEach { calificacion ->
                            add(calificacion.syncId)
                            addAll(dao.valores(calificacion.id).map { it.syncId })
                        }
                        addAll(dao.gestiones(ficha.id).map { it.syncId })
                        addAll(dao.contaminaciones(ficha.id).map { it.syncId })
                        addAll(dao.lugares(ficha.id).map { it.syncId })
                        dao.adjuntos(ficha.id).forEach { adjunto ->
                            borrarArchivoLocal(adjunto.uri)
                            add(adjunto.syncId)
                        }
                    }
                    ficha.firmaUri?.let(borrarArchivoLocal)
                    dao.marcarDescargando(ficha.id)
                    dao.eliminarFichaRemota(ficha.syncId)
                    (idsHijos + ficha.syncId).forEach { dao.descartarTumbaRemota(it) }
                }
            }
    }

    // ---- datos hijos ---------------------------------------------------------------------

    private suspend fun aplicarMiembros(fichaId: Long, filas: List<JSONObject>, completo: Boolean) {
        val vigentes = filas.filterNot { it.estaEliminada() }
        filas.filter { it.estaEliminada() }.forEach {
            dao.eliminarMiembroRemoto(it.getString("id")); dao.descartarTumbaRemota(it.getString("id"))
        }
        val locales = dao.miembros(fichaId).associateBy { it.syncId }
        vigentes.forEach { remoto ->
            val syncId = remoto.getString("id")
            val idExistente = dao.idMiembro(syncId) ?: 0
            val entidad = MiembroFamiliaEntity(
                id = idExistente,
                fichaId = fichaId,
                grupoEdad = remoto.texto("grupo_edad"),
                apellidosNombres = remoto.texto("apellidos_nombres"),
                parentesco = remoto.texto("parentesco"),
                fechaNacimiento = ConversionesSync.fechaLocal(remoto.texto("fecha_nacimiento")),
                ocupacion = remoto.texto("ocupacion"),
                sexo = remoto.texto("sexo"),
                escolaridad = remoto.texto("escolaridad"),
                vacunasCompletas = remoto.booleanNullable("vacunas_completas"),
                saludBucalAdecuada = remoto.booleanNullable("salud_bucal_adecuada"),
                riesgoEnfermedadDiscapacidad = remoto.texto("riesgo_enfermedad_discapacidad"),
                estadoNutricional = remoto.texto("estado_nutricional"),
                hipertensionArterial = remoto.booleanNullable("hipertension_arterial"),
                diabetesMellitus = remoto.booleanNullable("diabetes_mellitus"),
                tuberculosis = remoto.booleanNullable("tuberculosis"),
                problemaSaludMental = remoto.booleanNullable("problema_salud_mental"),
                consumoAlcoholDrogas = remoto.booleanNullable("consumo_alcohol_drogas"),
                enfermedadCronica = remoto.booleanNullable("enfermedad_cronica"),
                discapacidadVisual = remoto.booleanNullable("discapacidad_visual"),
                discapacidadAuditiva = remoto.booleanNullable("discapacidad_auditiva"),
                discapacidadLenguaje = remoto.booleanNullable("discapacidad_lenguaje"),
                discapacidadFisica = remoto.booleanNullable("discapacidad_fisica"),
                discapacidadIntelectual = remoto.booleanNullable("discapacidad_intelectual"),
                discapacidadPsicosocial = remoto.booleanNullable("discapacidad_psicosocial"),
                cuidadosPaliativos = remoto.booleanNullable("cuidados_paliativos"),
                vih = remoto.booleanNullable("vih"),
                eventoSalud = remoto.booleanNullable("evento_salud"),
                casoConfirmado = remoto.booleanNullable("caso_confirmado"),
                casoSospechosoUno = remoto.booleanNullable("caso_sospechoso_uno"),
                casoSospechosoDos = remoto.booleanNullable("caso_sospechoso_dos"),
                prestadorComunitario = remoto.booleanNullable("prestador_comunitario"),
                parteroAncestral = remoto.booleanNullable("partero_ancestral"),
                sabiduriaAncestral = remoto.booleanNullable("sabiduria_ancestral"),
                comorbilidadesCie10Json = remoto.texto("comorbilidades_cie10_json").ifBlank { "[]" },
                porcentajeDiscapacidad = remoto.intNullable("porcentaje_discapacidad"),
                necesitaAyudaTecnica = remoto.booleanNullable("necesita_ayuda_tecnica"),
                enfermedadCronicaDescompensada = remoto.booleanNullable("enfermedad_cronica_descompensada"),
                riesgoGenetico = remoto.booleanNullable("riesgo_genetico"),
                victimaViolencia = remoto.booleanNullable("victima_violencia"),
                privadoLibertad = remoto.booleanNullable("privado_libertad"),
                // Si el servidor aún no tiene la columna, se conserva lo que ya hay en este teléfono.
                factoresRiesgoEdadJson = if (remoto.has("factores_riesgo_edad_json")) {
                    remoto.texto("factores_riesgo_edad_json").ifBlank { "[]" }
                } else locales[syncId]?.factoresRiesgoEdadJson ?: "[]",
                numeroHistoriaClinica = remoto.texto("numero_historia_clinica"),
                cedula = remoto.texto("cedula"),
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarMiembroRemoto(entidad) else dao.actualizarMiembroRemoto(entidad)
            dao.descartarTumbaRemota(syncId)
        }
        if (completo) {
            val activos = vigentes.mapTo(mutableSetOf()) { it.getString("id") }
            dao.miembros(fichaId).filter { it.syncId !in activos }.forEach {
                dao.eliminarMiembroRemoto(it.syncId); dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun aplicarEmbarazadas(fichaId: Long, filas: List<JSONObject>, completo: Boolean) {
        val vigentes = filas.filterNot { it.estaEliminada() }
        filas.filter { it.estaEliminada() }.forEach {
            dao.eliminarEmbarazadaRemota(it.getString("id")); dao.descartarTumbaRemota(it.getString("id"))
        }
        val locales = dao.embarazadas(fichaId).associateBy { it.syncId }
        vigentes.forEach { remoto ->
            val syncId = remoto.getString("id")
            val idExistente = dao.idEmbarazada(syncId) ?: 0
            val entidad = EmbarazadaEntity(
                id = idExistente,
                fichaId = fichaId,
                apellidosNombres = remoto.texto("apellidos_nombres"),
                fechaUltimaMenstruacion = ConversionesSync.fechaLocal(remoto.texto("fecha_ultima_menstruacion")),
                fechaProbableParto = ConversionesSync.fechaLocal(remoto.texto("fecha_probable_parto")),
                semanasGestacion = remoto.intNullable("semanas_gestacion"),
                dosisDtPrimera = remoto.optBoolean("dosis_dt_primera"),
                dosisDtSegunda = remoto.optBoolean("dosis_dt_segunda"),
                dosisDtRefuerzo = remoto.optBoolean("dosis_dt_refuerzo"),
                gestas = remoto.intNullable("gestas"),
                partos = remoto.intNullable("partos"),
                abortos = remoto.intNullable("abortos"),
                cesareas = remoto.intNullable("cesareas"),
                antecedentesPatologicosObstetricos = remoto.texto("antecedentes_patologicos_obstetricos"),
                riesgoObstetrico = remoto.texto("riesgo_obstetrico"),
                factoresObstetricosJson = if (remoto.has("factores_obstetricos_json")) {
                    remoto.texto("factores_obstetricos_json").ifBlank { "[]" }
                } else locales[syncId]?.factoresObstetricosJson ?: "[]",
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarEmbarazadaRemota(entidad) else dao.actualizarEmbarazadaRemota(entidad)
            dao.descartarTumbaRemota(syncId)
        }
        if (completo) {
            val activos = vigentes.mapTo(mutableSetOf()) { it.getString("id") }
            dao.embarazadas(fichaId).filter { it.syncId !in activos }.forEach {
                dao.eliminarEmbarazadaRemota(it.syncId); dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun aplicarMortalidad(fichaId: Long, filas: List<JSONObject>, completo: Boolean) {
        val vigentes = filas.filterNot { it.estaEliminada() }
        filas.filter { it.estaEliminada() }.forEach {
            dao.eliminarMortalidadRemota(it.getString("id")); dao.descartarTumbaRemota(it.getString("id"))
        }
        vigentes.forEach { remoto ->
            val syncId = remoto.getString("id")
            val idExistente = dao.idMortalidad(syncId) ?: 0
            val entidad = MortalidadFamiliarEntity(
                id = idExistente,
                fichaId = fichaId,
                nombre = remoto.texto("nombre"),
                parentesco = remoto.texto("parentesco"),
                edadAlFallecer = remoto.intNullable("edad_al_fallecer"),
                causa = remoto.texto("causa"),
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarMortalidadRemota(entidad) else dao.actualizarMortalidadRemota(entidad)
            dao.descartarTumbaRemota(syncId)
        }
        if (completo) {
            val activos = vigentes.mapTo(mutableSetOf()) { it.getString("id") }
            dao.mortalidad(fichaId).filter { it.syncId !in activos }.forEach {
                dao.eliminarMortalidadRemota(it.syncId); dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun aplicarRiesgos(
        fichaId: Long,
        calificaciones: List<JSONObject>,
        valores: List<JSONObject>,
        completo: Boolean
    ) {
        val vigentes = calificaciones.filterNot { it.estaEliminada() }
        calificaciones.filter { it.estaEliminada() }.forEach {
            val id = it.getString("id")
            val idsValores = dao.idCalificacion(id)?.let { local -> dao.valores(local).map { v -> v.syncId } }.orEmpty()
            dao.eliminarCalificacionRemota(id)
            (idsValores + id).forEach { borrado -> dao.descartarTumbaRemota(borrado) }
        }
        vigentes.forEach { remoto ->
            val syncId = remoto.getString("id")
            val idExistente = dao.idCalificacion(syncId) ?: 0
            val entidad = CalificacionRiesgoEntity(
                id = idExistente,
                fichaId = fichaId,
                fechaCalificacion = ConversionesSync.fechaLocal(remoto.texto("fecha_calificacion")),
                responsable = remoto.texto("responsable"),
                total = remoto.optInt("total"),
                nivel = remoto.texto("nivel").ifBlank { "SIN_RIESGO" },
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarCalificacionRemota(entidad) else dao.actualizarCalificacionRemota(entidad)
            dao.descartarTumbaRemota(syncId)
        }

        valores.filter { it.estaEliminada() }.forEach {
            dao.eliminarValorRiesgoRemoto(it.getString("id")); dao.descartarTumbaRemota(it.getString("id"))
        }
        val valoresVigentes = valores.filterNot { it.estaEliminada() }
        valoresVigentes.forEach { remoto ->
            val calificacionId = dao.idCalificacion(remoto.texto("calificacion_id")) ?: return@forEach
            val syncId = remoto.getString("id")
            val idExistente = dao.idValorRiesgo(syncId) ?: 0
            val entidad = ValorRiesgoEntity(
                id = idExistente,
                calificacionId = calificacionId,
                componente = remoto.optInt("componente"),
                valor = remoto.optInt("valor"),
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarValorRiesgoRemoto(entidad) else dao.actualizarValorRiesgoRemoto(entidad)
            dao.descartarTumbaRemota(syncId)
        }
        if (completo) {
            val idsCalificaciones = vigentes.mapTo(mutableSetOf()) { it.getString("id") }
            val idsValores = valoresVigentes.mapTo(mutableSetOf()) { it.getString("id") }
            dao.calificaciones(fichaId).forEach { calificacion ->
                if (calificacion.syncId !in idsCalificaciones) {
                    val hijos = dao.valores(calificacion.id).map { it.syncId }
                    dao.eliminarCalificacionRemota(calificacion.syncId)
                    (hijos + calificacion.syncId).forEach { dao.descartarTumbaRemota(it) }
                } else {
                    dao.valores(calificacion.id).filter { it.syncId !in idsValores }.forEach {
                        dao.eliminarValorRiesgoRemoto(it.syncId); dao.descartarTumbaRemota(it.syncId)
                    }
                }
            }
        }
    }

    private suspend fun aplicarGestiones(fichaId: Long, filas: List<JSONObject>, completo: Boolean) {
        val vigentes = filas.filterNot { it.estaEliminada() }
        filas.filter { it.estaEliminada() }.forEach {
            dao.eliminarGestionRemota(it.getString("id")); dao.descartarTumbaRemota(it.getString("id"))
        }
        vigentes.forEach { remoto ->
            val syncId = remoto.getString("id")
            val idExistente = dao.idGestion(syncId) ?: 0
            val entidad = GestionRiesgoEntity(
                id = idExistente,
                fichaId = fichaId,
                fechaAnalisis = ConversionesSync.fechaLocal(remoto.texto("fecha_analisis")),
                numero = remoto.intNullable("numero"),
                compromisoFamilia = remoto.texto("compromiso_familia"),
                compromisoEquipoSalud = remoto.texto("compromiso_equipo_salud"),
                fechaEvaluacion = ConversionesSync.fechaLocal(remoto.texto("fecha_evaluacion")),
                cumplimiento = remoto.texto("cumplimiento").ifBlank { "PENDIENTE" },
                causasIncumplimientoObservaciones = remoto.texto("causas_incumplimiento_observaciones"),
                responsable = remoto.texto("responsable"),
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarGestionRemota(entidad) else dao.actualizarGestionRemota(entidad)
            dao.descartarTumbaRemota(syncId)
        }
        if (completo) {
            val activos = vigentes.mapTo(mutableSetOf()) { it.getString("id") }
            dao.gestiones(fichaId).filter { it.syncId !in activos }.forEach {
                dao.eliminarGestionRemota(it.syncId); dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun aplicarContaminaciones(fichaId: Long, filas: List<JSONObject>, completo: Boolean) {
        val vigentes = filas.filterNot { it.estaEliminada() }
        filas.filter { it.estaEliminada() }.forEach {
            dao.eliminarContaminacionRemota(it.getString("id")); dao.descartarTumbaRemota(it.getString("id"))
        }
        vigentes.forEach { remoto ->
            val syncId = remoto.getString("id")
            val idExistente = dao.idContaminacion(syncId) ?: 0
            val entidad = ContaminacionAmbientalEntity(
                id = idExistente,
                fichaId = fichaId,
                fechaInforme = ConversionesSync.fechaLocal(remoto.texto("fecha_informe")),
                tipoContaminanteDescripcion = remoto.texto("tipo_contaminante_descripcion"),
                causanteContaminacion = remoto.texto("causante_contaminacion"),
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarContaminacionRemota(entidad) else dao.actualizarContaminacionRemota(entidad)
            dao.descartarTumbaRemota(syncId)
        }
        if (completo) {
            val activos = vigentes.mapTo(mutableSetOf()) { it.getString("id") }
            dao.contaminaciones(fichaId).filter { it.syncId !in activos }.forEach {
                dao.eliminarContaminacionRemota(it.syncId); dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun aplicarLugares(fichaId: Long, filas: List<JSONObject>, completo: Boolean) {
        val vigentes = filas.filterNot { it.estaEliminada() }
        filas.filter { it.estaEliminada() }.forEach {
            dao.eliminarLugarRemoto(it.getString("id")); dao.descartarTumbaRemota(it.getString("id"))
        }
        vigentes.forEach { remoto ->
            val syncId = remoto.getString("id")
            val idExistente = dao.idLugar(syncId) ?: 0
            val entidad = LugarTratamientoEntity(
                id = idExistente,
                fichaId = fichaId,
                descripcion = remoto.texto("descripcion"),
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarLugarRemoto(entidad) else dao.actualizarLugarRemoto(entidad)
            dao.descartarTumbaRemota(syncId)
        }
        if (completo) {
            val activos = vigentes.mapTo(mutableSetOf()) { it.getString("id") }
            dao.lugares(fichaId).filter { it.syncId !in activos }.forEach {
                dao.eliminarLugarRemoto(it.syncId); dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun aplicarAdjuntos(
        fichaId: Long,
        filas: List<JSONObject>,
        archivos: Map<String, String>,
        completo: Boolean
    ) {
        val vigentes = filas.filterNot { it.estaEliminada() }
        val locales = dao.adjuntos(fichaId)
        filas.filter { it.estaEliminada() }.forEach { remoto ->
            val syncId = remoto.getString("id")
            locales.firstOrNull { it.syncId == syncId }?.let { borrarArchivoLocal(it.uri) }
            dao.eliminarAdjuntoRemoto(syncId)
            dao.descartarTumbaRemota(syncId)
        }
        if (completo) {
            val activos = vigentes.mapTo(mutableSetOf()) { it.getString("id") }
            locales.filter { it.syncId !in activos }.forEach {
                borrarArchivoLocal(it.uri)
                dao.eliminarAdjuntoRemoto(it.syncId)
                dao.descartarTumbaRemota(it.syncId)
            }
        }
        vigentes.forEach { remoto ->
            val syncId = remoto.getString("id")
            val uri = archivos[syncId] ?: return@forEach
            val actualizado = ConversionesSync.timestampMillis(remoto.texto("updated_at"), System.currentTimeMillis())
            val idExistente = dao.idAdjunto(syncId) ?: 0
            val entidad = AdjuntoFichaEntity(
                id = idExistente,
                fichaId = fichaId,
                tipo = remoto.texto("tipo"),
                uri = uri,
                actualizadoEn = actualizado,
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarAdjuntoRemoto(entidad) else dao.actualizarAdjuntoRemoto(entidad)
            dao.descartarTumbaRemota(syncId)
        }
    }

    private suspend fun aplicarHistorial(fichaId: Long, filas: List<JSONObject>) {
        filas.forEach { remoto ->
            val syncId = remoto.getString("id")
            val idExistente = dao.idHistorial(syncId) ?: 0
            val entidad = HistorialFichaEntity(
                id = idExistente,
                fichaId = fichaId,
                numeroFicha = remoto.texto("numero_ficha"),
                usuarioId = null,
                usuarioNombre = remoto.texto("usuario_nombre"),
                accion = remoto.texto("accion"),
                detalle = remoto.texto("detalle"),
                creadoEn = ConversionesSync.timestampMillis(remoto.texto("ocurrido_en"), System.currentTimeMillis()),
                syncId = syncId
            )
            if (idExistente == 0L) dao.guardarHistorialRemoto(entidad) else dao.actualizarHistorialRemoto(entidad)
        }
    }

    // ---- cabecera --------------------------------------------------------------------------

    private fun entidadFicha(
        remoto: JSONObject,
        organizacionId: String,
        existente: FichaFamiliarEntity?,
        firmaUri: String?
    ): FichaFamiliarEntity {
        val actualizadoRemoto = ConversionesSync.timestampMillis(remoto.texto("updated_at"), System.currentTimeMillis())
        return FichaFamiliarEntity(
            id = existente?.id ?: 0,
            cedulaJefeHogar = remoto.texto("cedula_jefe_hogar"),
            institucionSistema = remoto.texto("institucion_sistema"),
            unidadOperativa = remoto.texto("unidad_operativa"),
            codigoUo = remoto.texto("codigo_uo"),
            areaNumero = remoto.texto("area_numero"),
            codigoLocalizacion = remoto.texto("codigo_localizacion"),
            parroquiaCodigoLocalizacion = remoto.texto("parroquia_codigo_localizacion"),
            cantonCodigoLocalizacion = remoto.texto("canton_codigo_localizacion"),
            provinciaCodigoLocalizacion = remoto.texto("provincia_codigo_localizacion"),
            numeroFichaFamiliar = remoto.texto("numero_ficha_familiar"),
            provincia = remoto.texto("provincia"),
            canton = remoto.texto("canton"),
            parroquia = remoto.texto("parroquia"),
            sector = remoto.texto("sector"),
            manzana = remoto.texto("manzana"),
            numeroFamilia = remoto.texto("numero_familia"),
            direccionHabitualFamilia = remoto.texto("direccion_habitual_familia"),
            barrio = remoto.texto("barrio"),
            numeroCasa = remoto.texto("numero_casa"),
            comunidad = remoto.texto("comunidad"),
            grupoCultural = remoto.texto("grupo_cultural"),
            nombreApellidoJefeFamilia = remoto.texto("nombre_apellido_jefe_familia"),
            numeroTelefono = remoto.texto("numero_telefono"),
            fechaLlenado = ConversionesSync.fechaLocal(remoto.texto("fecha_llenado")),
            numeroCarpeta = remoto.texto("numero_carpeta"),
            latitud = remoto.doubleNullable("latitud"),
            longitud = remoto.doubleNullable("longitud"),
            altitud = remoto.doubleNullable("altitud"),
            // Si el servidor aún no tiene la columna, se conservan los símbolos que ya hay en este teléfono.
            croquisElementosJson = if (remoto.has("croquis_elementos_json")) {
                remoto.texto("croquis_elementos_json").ifBlank { "[]" }
            } else existente?.croquisElementosJson ?: "[]",
            responsableNombre = remoto.texto("responsable_nombre"),
            responsableCodigo = remoto.texto("responsable_codigo"),
            firmaUri = firmaUri,
            estado = remoto.texto("estado").ifBlank { "BORRADOR" },
            creadoEn = ConversionesSync.timestampMillis(remoto.texto("creado_en"), existente?.creadoEn ?: 0L),
            actualizadoEn = actualizadoRemoto,
            creadoPorUsuarioId = existente?.creadoPorUsuarioId,
            actualizadoPorUsuarioId = existente?.actualizadoPorUsuarioId,
            completadoPorUsuarioId = existente?.completadoPorUsuarioId,
            syncId = remoto.getString("id"),
            syncEstado = "DESCARGANDO",
            syncVersion = remoto.optLong("version", 1L),
            syncError = "",
            organizacionId = organizacionId,
            establecimientoRemotoId = if (remoto.isNull("establecimiento_id")) null else remoto.optLong("establecimiento_id"),
            eaisId = remoto.texto("eais_id"),
            territorioId = remoto.texto("territorio_id"),
            autorRemotoId = remoto.texto("creado_por").ifBlank { existente?.autorRemotoId.orEmpty() },
            autorNombre = existente?.autorNombre.orEmpty(),
            miPermiso = existente?.miPermiso.orEmpty(),
            compartidaConPersonas = existente?.compartidaConPersonas ?: 0,
            editorNombre = existente?.editorNombre.orEmpty(),
            editadaPorOtroEn = existente?.editadaPorOtroEn ?: 0L
        )
    }
}
