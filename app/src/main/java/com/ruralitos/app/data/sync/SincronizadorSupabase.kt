package com.ruralitos.app.data.sync

import android.content.Context
import android.net.Uri
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
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.data.remote.ErrorSupabase
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ResultadoSincronizacion(
    val subidas: Int,
    val eliminaciones: Int,
    val errores: Int,
    val descargadas: Int = 0,
    val erroresReintentables: Int = 0
)

internal fun errorSincronizacionReintentable(error: Throwable): Boolean = when (error) {
    is ErrorSupabase -> error.codigoHttp == null || error.codigoHttp == 408 ||
        error.codigoHttp == 429 || (error.codigoHttp?.let { it in 500..599 } == true)
    is FileNotFoundException -> false
    is IOException -> true
    else -> false
}

class SincronizadorSupabase(context: Context) {
    private val appContext = context.applicationContext
    private val database = RuralitosDatabase.obtenerBaseDatos(appContext)
    private val dao = database.sincronizacionDao()
    private val api = SupabaseApi(appContext)
    private val privados = SincronizadorPrivado(appContext, api)

    suspend fun ejecutar(soloSubidas: Boolean = false): ResultadoSincronizacion =
        bloqueoProceso.withLock { ejecutarSerializado(soloSubidas) }

    private suspend fun ejecutarSerializado(soloSubidas: Boolean): ResultadoSincronizacion {
        api.tokenValido()
        val salasLocales = if (soloSubidas) database.salaDao().listarSalas() else emptyList()
        val salas = salasLocales.ifEmpty { SincronizadorSalas.actualizar(database, api) }
        if (salas.isEmpty()) return ResultadoSincronizacion(0, 0, 0)

        val activa = api.organizacionGuardada()
            ?.takeIf { id -> salas.any { it.organizacionId == id } }
            ?: salas.first().organizacionId
        api.guardarOrganizacionActiva(activa)
        dao.asignarOrganizacionPendiente(activa)

        var totalSubidas = 0
        var totalEliminaciones = 0
        var totalErrores = 0
        var totalDescargas = 0
        var totalReintentables = 0
        salas.forEach { sala ->
            val parcial = sincronizarSala(sala.organizacionId, soloSubidas)
            totalSubidas += parcial.subidas
            totalEliminaciones += parcial.eliminaciones
            totalErrores += parcial.errores
            totalDescargas += parcial.descargadas
            totalReintentables += parcial.erroresReintentables
        }
        return ResultadoSincronizacion(
            subidas = totalSubidas,
            eliminaciones = totalEliminaciones,
            errores = totalErrores,
            descargadas = totalDescargas,
            erroresReintentables = totalReintentables
        )
    }

    private suspend fun sincronizarSala(organizacionId: String, soloSubidas: Boolean): ResultadoSincronizacion {
        var eliminadas = 0
        var errores = 0
        var reintentables = 0
        dao.eliminaciones(organizacionId).forEach { tumba ->
            runCatching { api.marcarEliminado(tumba.tabla, tumba.registroSyncId) }
                .onSuccess {
                    dao.eliminarTumba(tumba.id)
                    eliminadas++
                }
                .onFailure { error -> errores++; if (errorSincronizacionReintentable(error)) reintentables++ }
        }

        var subidas = 0
        dao.fichasPendientes(organizacionId).forEach { ficha ->
            runCatching {
                val fichaRemota = fichaJson(ficha, organizacionId)
                    .put("firma_storage_path", JSONObject.NULL)
                var respuesta = api.upsert("fichas_familiares", fichaRemota)
                val firmaRuta = subirFirmaSiExiste(ficha, organizacionId)
                if (firmaRuta != null) {
                    respuesta = api.upsert(
                        "fichas_familiares",
                        fichaJson(ficha, organizacionId).put("firma_storage_path", firmaRuta)
                    )
                }
                subirContenido(ficha, organizacionId)
                subirHistorial(ficha, organizacionId)
                val version = respuesta?.optLong("version", ficha.syncVersion + 1)
                    ?: (ficha.syncVersion + 1)
                dao.marcarSincronizada(ficha.id, version, ficha.actualizadoEn) > 0
            }.onSuccess { guardadaSinCambiosPosteriores ->
                if (guardadaSinCambiosPosteriores) subidas++
            }.onFailure { error ->
                errores++
                if (errorSincronizacionReintentable(error)) reintentables++
                dao.marcarError(
                    ficha.id,
                    error.message.orEmpty().ifBlank { "No se pudo sincronizar." }.take(300)
                )
            }
        }

        var descargadas = 0
        if (!soloSubidas) {
            runCatching { descargarDesdeSupabase(organizacionId) }
                .onSuccess { descargadas = it }
                .onFailure { error -> errores++; if (errorSincronizacionReintentable(error)) reintentables++ }
        }
        val resultadoPrivado = privados.ejecutar(organizacionId, soloSubidas)
        return ResultadoSincronizacion(
            subidas + resultadoPrivado.subidas,
            eliminadas,
            errores + resultadoPrivado.errores,
            descargadas,
            reintentables + resultadoPrivado.reintentables
        )
    }
    private suspend fun subirContenido(ficha: FichaFamiliarEntity, organizacionId: String) {
        api.upsertVarios("miembros_familia", dao.miembros(ficha.id).map {
            miembroJson(it, ficha.syncId, organizacionId)
        })
        api.upsertVarios("embarazadas", dao.embarazadas(ficha.id).map {
            embarazadaJson(it, ficha.syncId, organizacionId)
        })
        api.upsertVarios("mortalidad_familiar", dao.mortalidad(ficha.id).map {
            mortalidadJson(it, ficha.syncId, organizacionId)
        })
        val calificaciones = dao.calificaciones(ficha.id)
        api.upsertVarios("calificaciones_riesgo", calificaciones.map {
            calificacionJson(it, ficha.syncId, organizacionId)
        })
        val valores = calificaciones.flatMap { calificacion ->
            dao.valores(calificacion.id).map { valor ->
                valorJson(valor, calificacion.syncId, organizacionId)
            }
        }
        api.upsertVarios("valores_riesgo", valores, "calificacion_id,componente")
        api.upsertVarios("gestion_riesgo", dao.gestiones(ficha.id).map {
            gestionJson(it, ficha.syncId, organizacionId)
        })
        api.upsertVarios("contaminacion_ambiental", dao.contaminaciones(ficha.id).map {
            contaminacionJson(it, ficha.syncId, organizacionId)
        })
        api.upsertVarios("lugares_tratamiento", dao.lugares(ficha.id).map {
            lugarJson(it, ficha.syncId, organizacionId)
        })
        dao.adjuntos(ficha.id).forEach {
            subirAdjunto(it, ficha.syncId, organizacionId)
        }
    }

    private suspend fun subirHistorial(ficha: FichaFamiliarEntity, organizacionId: String) {
        val eventos = dao.historial(ficha.id).map { evento ->
            JSONObject()
                .put("id", evento.syncId)
                .put("organizacion_id", organizacionId)
                .put("ficha_id", ficha.syncId)
                .put("numero_ficha", evento.numeroFicha)
                .put("usuario_nombre", evento.usuarioNombre)
                .put("accion", evento.accion)
                .put("detalle", evento.detalle)
                .put("ocurrido_en", fechaHoraIso(evento.creadoEn))
        }
        api.insertarIgnorandoVarios("historial_fichas", eventos)
    }

    private suspend fun subirAdjunto(item: AdjuntoFichaEntity, fichaSyncId: String, organizacionId: String) {
        val uri = Uri.parse(item.uri)
        if (uri.scheme.isNullOrBlank()) return
        val mime = appContext.contentResolver.getType(uri) ?: when (
            uri.path?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
        ) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            else -> "image/jpeg"
        }
        val extension = when (mime) {
            "image/png" -> "png"
            "application/pdf" -> "pdf"
            else -> "jpg"
        }
        val ruta = "$organizacionId/$fichaSyncId/${item.syncId}-${item.tipo.lowercase()}.$extension"
        val tamano = api.subirAdjunto(uri, ruta, mime)
        api.upsert(
            "adjuntos_ficha",
            JSONObject()
                .put("id", item.syncId)
                .put("organizacion_id", organizacionId)
                .put("ficha_id", fichaSyncId)
                .put("tipo", item.tipo)
                .put("storage_path", ruta)
                .put("mime_type", mime)
                .put("tamano_bytes", tamano)
        )
    }

    private suspend fun subirFirmaSiExiste(ficha: FichaFamiliarEntity, organizacionId: String): String? {
        val uri = ficha.firmaUri?.let(Uri::parse) ?: return null
        if (uri.scheme.isNullOrBlank()) return null
        val mime = appContext.contentResolver.getType(uri) ?: when (
            uri.path?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
        ) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/png"
        }
        val extension = extensionPara(mime, "png")
        val ruta = "$organizacionId/${ficha.syncId}/firma-${ficha.syncId}.$extension"
        api.subirAdjunto(uri, ruta, mime)
        return ruta
    }

    private suspend fun descargarDesdeSupabase(organizacionId: String): Int {
        val filasFicha = api.seleccionarPaginado(
            "fichas_familiares?organizacion_id=eq.$organizacionId&deleted_at=is.null&select=*"
        )
        val fichasLocales = mutableMapOf<String, FichaFamiliarEntity>()
        filasFicha.forEach { remoto ->
            val syncId = remoto.getString("id")
            val existente = dao.fichaPorSyncId(syncId)
            if (existente?.syncEstado == "PENDIENTE" || existente?.syncEstado == "ERROR") return@forEach
            val actualizadoRemoto = timestampMillis(remoto.texto("updated_at"), System.currentTimeMillis())
            val firmaRuta = remoto.texto("firma_storage_path")
            val firmaUri = if (firmaRuta.isBlank()) {
                existente?.firmaUri?.let(::eliminarArchivoInternoSiCorresponde)
                null
            } else {
                val destino = File(
                    appContext.filesDir,
                    "adjuntos_sincronizados/$organizacionId/$syncId/firma.${firmaRuta.substringAfterLast('.', "png")}"
                )
                descargarArchivoSiNecesario(firmaRuta, destino, actualizadoRemoto)
            }
            val entidad = FichaFamiliarEntity(
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
                fechaLlenado = fechaLocal(remoto.texto("fecha_llenado")),
                numeroCarpeta = remoto.texto("numero_carpeta"),
                latitud = remoto.doubleNullable("latitud"),
                longitud = remoto.doubleNullable("longitud"),
                altitud = remoto.doubleNullable("altitud"),
                responsableNombre = remoto.texto("responsable_nombre"),
                responsableCodigo = remoto.texto("responsable_codigo"),
                firmaUri = firmaUri,
                estado = remoto.texto("estado").ifBlank { "BORRADOR" },
                creadoEn = timestampMillis(remoto.texto("creado_en"), existente?.creadoEn ?: 0L),
                actualizadoEn = actualizadoRemoto,
                creadoPorUsuarioId = existente?.creadoPorUsuarioId,
                actualizadoPorUsuarioId = existente?.actualizadoPorUsuarioId,
                completadoPorUsuarioId = existente?.completadoPorUsuarioId,
                syncId = syncId,
                syncEstado = "DESCARGANDO",
                syncVersion = remoto.optLong("version", 1L),
                syncError = "",
                organizacionId = organizacionId,
                establecimientoRemotoId = if (remoto.isNull("establecimiento_id")) {
                    null
                } else {
                    remoto.optLong("establecimiento_id")
                },
                eaisId = remoto.texto("eais_id"),
                territorioId = remoto.texto("territorio_id")
            )
            val idLocal = if (entidad.id == 0L) {
                dao.guardarFichaRemota(entidad)
            } else {
                dao.actualizarFichaRemota(entidad)
                entidad.id
            }
            fichasLocales[syncId] = entidad.copy(id = if (entidad.id == 0L) idLocal else entidad.id)
        }

        descargarMiembros(organizacionId, fichasLocales)
        descargarEmbarazadas(organizacionId, fichasLocales)
        descargarMortalidad(organizacionId, fichasLocales)
        descargarRiesgos(organizacionId, fichasLocales)
        descargarGestiones(organizacionId, fichasLocales)
        descargarContaminacion(organizacionId, fichasLocales)
        descargarLugares(organizacionId, fichasLocales)
        descargarAdjuntos(organizacionId, fichasLocales)
        descargarHistorial(organizacionId, fichasLocales)
        val bajasConfirmadas = api.seleccionarPaginado(
            "fichas_familiares?organizacion_id=eq.$organizacionId&deleted_at=not.is.null&select=id"
        ).mapTo(mutableSetOf()) { it.getString("id") }
        eliminarFichasConfirmadas(organizacionId, bajasConfirmadas)

        filasFicha.forEach { remoto ->
            val syncId = remoto.getString("id")
            fichasLocales[syncId]?.let {
                dao.marcarSincronizada(
                    it.id,
                    remoto.optLong("version", 1L),
                    it.actualizadoEn
                )
            }
        }
        return fichasLocales.size
    }

    private suspend fun eliminarFichasConfirmadas(org: String, idsEliminados: Set<String>) {
        dao.fichasLocalesSincronizadas(org)
            .filter { it.syncId in idsEliminados }
            .forEach { ficha ->
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
                        eliminarArchivoInternoSiCorresponde(adjunto.uri)
                        add(adjunto.syncId)
                    }
                }
                ficha.firmaUri?.let(::eliminarArchivoInternoSiCorresponde)
                dao.marcarDescargando(ficha.id)
                dao.eliminarFichaRemota(ficha.syncId)
                (idsHijos + ficha.syncId).forEach { dao.descartarTumbaRemota(it) }
            }
    }

    private suspend fun descargarMiembros(org: String, fichas: Map<String, FichaFamiliarEntity>) {
        val remotos = api.seleccionarPaginado(
            "miembros_familia?organizacion_id=eq.$org&deleted_at=is.null&select=*"
        )
        val idsActivos = remotos.mapTo(mutableSetOf()) { it.getString("id") }
        remotos.forEach { remoto ->
                val ficha = fichas[remoto.texto("ficha_id")] ?: return@forEach
                val syncId = remoto.getString("id")
                val idExistente = dao.idMiembro(syncId) ?: 0
                val entidad = MiembroFamiliaEntity(
                        id = idExistente,
                        fichaId = ficha.id,
                        grupoEdad = remoto.texto("grupo_edad"),
                        apellidosNombres = remoto.texto("apellidos_nombres"),
                        parentesco = remoto.texto("parentesco"),
                        fechaNacimiento = fechaLocal(remoto.texto("fecha_nacimiento")),
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
                        numeroHistoriaClinica = remoto.texto("numero_historia_clinica"),
                        cedula = remoto.texto("cedula"),
                        syncId = syncId
                    )
                if (idExistente == 0L) dao.guardarMiembroRemoto(entidad)
                else dao.actualizarMiembroRemoto(entidad)
            }
        fichas.values.forEach { ficha ->
            dao.miembros(ficha.id).filter { it.syncId !in idsActivos }.forEach {
                dao.eliminarMiembroRemoto(it.syncId)
                dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun descargarEmbarazadas(org: String, fichas: Map<String, FichaFamiliarEntity>) {
        val remotos = api.seleccionarPaginado(
            "embarazadas?organizacion_id=eq.$org&deleted_at=is.null&select=*"
        )
        val idsActivos = remotos.mapTo(mutableSetOf()) { it.getString("id") }
        remotos.forEach { remoto ->
                val ficha = fichas[remoto.texto("ficha_id")] ?: return@forEach
                val syncId = remoto.getString("id")
                val idExistente = dao.idEmbarazada(syncId) ?: 0
                val entidad = EmbarazadaEntity(
                        id = idExistente,
                        fichaId = ficha.id,
                        apellidosNombres = remoto.texto("apellidos_nombres"),
                        fechaUltimaMenstruacion = fechaLocal(remoto.texto("fecha_ultima_menstruacion")),
                        fechaProbableParto = fechaLocal(remoto.texto("fecha_probable_parto")),
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
                        syncId = syncId
                    )
                if (idExistente == 0L) dao.guardarEmbarazadaRemota(entidad)
                else dao.actualizarEmbarazadaRemota(entidad)
            }
        fichas.values.forEach { ficha ->
            dao.embarazadas(ficha.id).filter { it.syncId !in idsActivos }.forEach {
                dao.eliminarEmbarazadaRemota(it.syncId)
                dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun descargarMortalidad(org: String, fichas: Map<String, FichaFamiliarEntity>) {
        val remotos = api.seleccionarPaginado(
            "mortalidad_familiar?organizacion_id=eq.$org&deleted_at=is.null&select=*"
        )
        val idsActivos = remotos.mapTo(mutableSetOf()) { it.getString("id") }
        remotos.forEach { remoto ->
                val ficha = fichas[remoto.texto("ficha_id")] ?: return@forEach
                val syncId = remoto.getString("id")
                val idExistente = dao.idMortalidad(syncId) ?: 0
                val entidad = MortalidadFamiliarEntity(
                        id = idExistente,
                        fichaId = ficha.id,
                        nombre = remoto.texto("nombre"),
                        parentesco = remoto.texto("parentesco"),
                        edadAlFallecer = remoto.intNullable("edad_al_fallecer"),
                        causa = remoto.texto("causa"),
                        syncId = syncId
                    )
                if (idExistente == 0L) dao.guardarMortalidadRemota(entidad)
                else dao.actualizarMortalidadRemota(entidad)
            }
        fichas.values.forEach { ficha ->
            dao.mortalidad(ficha.id).filter { it.syncId !in idsActivos }.forEach {
                dao.eliminarMortalidadRemota(it.syncId)
                dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun descargarRiesgos(org: String, fichas: Map<String, FichaFamiliarEntity>) {
        val calificacionesLocales = mutableMapOf<String, Long>()
        val calificacionesRemotas = api.seleccionarPaginado(
            "calificaciones_riesgo?organizacion_id=eq.$org&deleted_at=is.null&select=*"
        )
        val idsCalificacionesActivas = calificacionesRemotas.mapTo(mutableSetOf()) { it.getString("id") }
        calificacionesRemotas.forEach { remoto ->
                val ficha = fichas[remoto.texto("ficha_id")] ?: return@forEach
                val syncId = remoto.getString("id")
                val idExistente = dao.idCalificacion(syncId) ?: 0
                val entidad = CalificacionRiesgoEntity(
                        id = idExistente,
                        fichaId = ficha.id,
                        fechaCalificacion = fechaLocal(remoto.texto("fecha_calificacion")),
                        responsable = remoto.texto("responsable"),
                        total = remoto.optInt("total"),
                        nivel = remoto.texto("nivel").ifBlank { "SIN_RIESGO" },
                        syncId = syncId
                    )
                val idGuardado = if (idExistente == 0L) {
                    dao.guardarCalificacionRemota(entidad)
                } else {
                    dao.actualizarCalificacionRemota(entidad)
                    idExistente
                }
                calificacionesLocales[syncId] = if (idExistente == 0L) idGuardado else idExistente
            }
        val valoresRemotos = api.seleccionarPaginado(
            "valores_riesgo?organizacion_id=eq.$org&deleted_at=is.null&select=*"
        )
        val idsValoresActivos = valoresRemotos.mapTo(mutableSetOf()) { it.getString("id") }
        calificacionesLocales.values.forEach { calificacionId ->
            dao.valores(calificacionId).filter { it.syncId !in idsValoresActivos }.forEach {
                dao.eliminarValorRiesgoRemoto(it.syncId)
                dao.descartarTumbaRemota(it.syncId)
            }
        }
        valoresRemotos.forEach { remoto ->
                val calificacionId = calificacionesLocales[remoto.texto("calificacion_id")] ?: return@forEach
                val syncId = remoto.getString("id")
                val idExistente = dao.idValorRiesgo(syncId) ?: 0
                val entidad = ValorRiesgoEntity(
                        id = idExistente,
                        calificacionId = calificacionId,
                        componente = remoto.optInt("componente"),
                        valor = remoto.optInt("valor"),
                        syncId = syncId
                    )
                if (idExistente == 0L) dao.guardarValorRiesgoRemoto(entidad)
                else dao.actualizarValorRiesgoRemoto(entidad)
            }
        fichas.values.forEach { ficha ->
            dao.calificaciones(ficha.id)
                .filter { it.syncId !in idsCalificacionesActivas }
                .forEach { calificacion ->
                    val idsValores = dao.valores(calificacion.id).map { it.syncId }
                    dao.eliminarCalificacionRemota(calificacion.syncId)
                    (idsValores + calificacion.syncId).forEach { dao.descartarTumbaRemota(it) }
                }
        }
    }

    private suspend fun descargarGestiones(org: String, fichas: Map<String, FichaFamiliarEntity>) {
        val remotos = api.seleccionarPaginado(
            "gestion_riesgo?organizacion_id=eq.$org&deleted_at=is.null&select=*"
        )
        val idsActivos = remotos.mapTo(mutableSetOf()) { it.getString("id") }
        remotos.forEach { remoto ->
                val ficha = fichas[remoto.texto("ficha_id")] ?: return@forEach
                val syncId = remoto.getString("id")
                val idExistente = dao.idGestion(syncId) ?: 0
                val entidad = GestionRiesgoEntity(
                        id = idExistente,
                        fichaId = ficha.id,
                        fechaAnalisis = fechaLocal(remoto.texto("fecha_analisis")),
                        numero = remoto.intNullable("numero"),
                        compromisoFamilia = remoto.texto("compromiso_familia"),
                        compromisoEquipoSalud = remoto.texto("compromiso_equipo_salud"),
                        fechaEvaluacion = fechaLocal(remoto.texto("fecha_evaluacion")),
                        cumplimiento = remoto.texto("cumplimiento").ifBlank { "PENDIENTE" },
                        causasIncumplimientoObservaciones = remoto.texto("causas_incumplimiento_observaciones"),
                        responsable = remoto.texto("responsable"),
                        syncId = syncId
                    )
                if (idExistente == 0L) dao.guardarGestionRemota(entidad)
                else dao.actualizarGestionRemota(entidad)
            }
        fichas.values.forEach { ficha ->
            dao.gestiones(ficha.id).filter { it.syncId !in idsActivos }.forEach {
                dao.eliminarGestionRemota(it.syncId)
                dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun descargarContaminacion(org: String, fichas: Map<String, FichaFamiliarEntity>) {
        val remotos = api.seleccionarPaginado(
            "contaminacion_ambiental?organizacion_id=eq.$org&deleted_at=is.null&select=*"
        )
        val idsActivos = remotos.mapTo(mutableSetOf()) { it.getString("id") }
        remotos.forEach { remoto ->
                val ficha = fichas[remoto.texto("ficha_id")] ?: return@forEach
                val syncId = remoto.getString("id")
                val idExistente = dao.idContaminacion(syncId) ?: 0
                val entidad = ContaminacionAmbientalEntity(
                        id = idExistente,
                        fichaId = ficha.id,
                        fechaInforme = fechaLocal(remoto.texto("fecha_informe")),
                        tipoContaminanteDescripcion = remoto.texto("tipo_contaminante_descripcion"),
                        causanteContaminacion = remoto.texto("causante_contaminacion"),
                        syncId = syncId
                    )
                if (idExistente == 0L) dao.guardarContaminacionRemota(entidad)
                else dao.actualizarContaminacionRemota(entidad)
            }
        fichas.values.forEach { ficha ->
            dao.contaminaciones(ficha.id).filter { it.syncId !in idsActivos }.forEach {
                dao.eliminarContaminacionRemota(it.syncId)
                dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun descargarLugares(org: String, fichas: Map<String, FichaFamiliarEntity>) {
        val remotos = api.seleccionarPaginado(
            "lugares_tratamiento?organizacion_id=eq.$org&deleted_at=is.null&select=*"
        )
        val idsActivos = remotos.mapTo(mutableSetOf()) { it.getString("id") }
        remotos.forEach { remoto ->
                val ficha = fichas[remoto.texto("ficha_id")] ?: return@forEach
                val syncId = remoto.getString("id")
                val idExistente = dao.idLugar(syncId) ?: 0
                val entidad = LugarTratamientoEntity(
                        id = idExistente,
                        fichaId = ficha.id,
                        descripcion = remoto.texto("descripcion"),
                        syncId = syncId
                    )
                if (idExistente == 0L) dao.guardarLugarRemoto(entidad)
                else dao.actualizarLugarRemoto(entidad)
            }
        fichas.values.forEach { ficha ->
            dao.lugares(ficha.id).filter { it.syncId !in idsActivos }.forEach {
                dao.eliminarLugarRemoto(it.syncId)
                dao.descartarTumbaRemota(it.syncId)
            }
        }
    }

    private suspend fun descargarAdjuntos(org: String, fichas: Map<String, FichaFamiliarEntity>) {
        val remotos = api.seleccionarPaginado(
            "adjuntos_ficha?organizacion_id=eq.$org&deleted_at=is.null&select=*"
        )
        val idsActivos = remotos.mapTo(mutableSetOf()) { it.getString("id") }
        fichas.values.forEach { ficha ->
            dao.adjuntos(ficha.id).filter { it.syncId !in idsActivos }.forEach {
                eliminarArchivoInternoSiCorresponde(it.uri)
                dao.eliminarAdjuntoRemoto(it.syncId)
                dao.descartarTumbaRemota(it.syncId)
            }
        }
        remotos.forEach { remoto ->
            val fichaSyncId = remoto.texto("ficha_id")
            val ficha = fichas[fichaSyncId] ?: return@forEach
            val syncId = remoto.getString("id")
            val ruta = remoto.texto("storage_path")
            if (ruta.isBlank()) return@forEach
            val mime = remoto.texto("mime_type").ifBlank { "application/octet-stream" }
            val extension = ruta.substringAfterLast('.', extensionPara(mime, "bin"))
            val actualizado = timestampMillis(remoto.texto("updated_at"), System.currentTimeMillis())
            val destino = File(
                appContext.filesDir,
                "adjuntos_sincronizados/$org/$fichaSyncId/$syncId.$extension"
            )
            val uri = descargarArchivoSiNecesario(
                rutaStorage = ruta,
                destino = destino,
                actualizadoRemoto = actualizado,
                tamanoEsperado = remoto.optLong("tamano_bytes", -1L)
            )
            val idExistente = dao.idAdjunto(syncId) ?: 0
            val entidad = AdjuntoFichaEntity(
                    id = idExistente,
                    fichaId = ficha.id,
                    tipo = remoto.texto("tipo"),
                    uri = uri,
                    actualizadoEn = actualizado,
                    syncId = syncId
                )
            if (idExistente == 0L) dao.guardarAdjuntoRemoto(entidad)
            else dao.actualizarAdjuntoRemoto(entidad)
        }
    }

    private suspend fun descargarHistorial(org: String, fichas: Map<String, FichaFamiliarEntity>) {
        api.seleccionarPaginado("historial_fichas?organizacion_id=eq.$org&select=*")
            .forEach { remoto ->
                val ficha = fichas[remoto.texto("ficha_id")] ?: return@forEach
                val syncId = remoto.getString("id")
                val idExistente = dao.idHistorial(syncId) ?: 0
                val entidad = HistorialFichaEntity(
                        id = idExistente,
                        fichaId = ficha.id,
                        numeroFicha = remoto.texto("numero_ficha"),
                        usuarioId = null,
                        usuarioNombre = remoto.texto("usuario_nombre"),
                        accion = remoto.texto("accion"),
                        detalle = remoto.texto("detalle"),
                        creadoEn = timestampMillis(remoto.texto("ocurrido_en"), System.currentTimeMillis()),
                        syncId = syncId
                    )
                if (idExistente == 0L) dao.guardarHistorialRemoto(entidad)
                else dao.actualizarHistorialRemoto(entidad)
            }
    }

    private suspend fun descargarArchivoSiNecesario(
        rutaStorage: String,
        destino: File,
        actualizadoRemoto: Long,
        tamanoEsperado: Long = -1L
    ): String {
        val vigente = destino.isFile &&
            (tamanoEsperado <= 0L || destino.length() == tamanoEsperado) &&
            destino.lastModified() >= actualizadoRemoto - 1_000L
        if (!vigente) {
            api.descargarAdjunto(rutaStorage, destino)
            destino.setLastModified(actualizadoRemoto)
        }
        return Uri.fromFile(destino).toString()
    }

    private fun eliminarArchivoInternoSiCorresponde(uriTexto: String) {
        val uri = Uri.parse(uriTexto)
        if (uri.scheme != "file") return
        runCatching {
            val archivo = File(checkNotNull(uri.path)).canonicalFile
            val raiz = appContext.filesDir.canonicalFile
            if (archivo.path.startsWith(raiz.path + File.separator)) archivo.delete()
        }
    }

    private fun extensionPara(mime: String, fallback: String): String = when (mime.lowercase()) {
        "image/png" -> "png"
        "image/jpeg", "image/jpg" -> "jpg"
        "application/pdf" -> "pdf"
        else -> fallback
    }

    private fun fichaJson(item: FichaFamiliarEntity, organizacionId: String) = JSONObject()
        .put("id", item.syncId)
        .put("organizacion_id", organizacionId)
        .put("establecimiento_id", item.establecimientoRemotoId ?: JSONObject.NULL)
        .put("eais_id", item.eaisId.ifBlank { null } ?: JSONObject.NULL)
        .put("territorio_id", item.territorioId.ifBlank { null } ?: JSONObject.NULL)
        .put("cedula_jefe_hogar", item.cedulaJefeHogar)
        .put("institucion_sistema", item.institucionSistema)
        .put("unidad_operativa", item.unidadOperativa)
        .put("codigo_uo", item.codigoUo)
        .put("area_numero", item.areaNumero)
        .put("codigo_localizacion", item.codigoLocalizacion)
        .put("parroquia_codigo_localizacion", item.parroquiaCodigoLocalizacion)
        .put("canton_codigo_localizacion", item.cantonCodigoLocalizacion)
        .put("provincia_codigo_localizacion", item.provinciaCodigoLocalizacion)
        .put("numero_ficha_familiar", item.numeroFichaFamiliar)
        .put("provincia", item.provincia)
        .put("canton", item.canton)
        .put("parroquia", item.parroquia)
        .put("sector", item.sector)
        .put("manzana", item.manzana)
        .put("numero_familia", item.numeroFamilia)
        .put("direccion_habitual_familia", item.direccionHabitualFamilia)
        .put("barrio", item.barrio)
        .put("numero_casa", item.numeroCasa)
        .put("comunidad", item.comunidad)
        .put("grupo_cultural", item.grupoCultural)
        .put("nombre_apellido_jefe_familia", item.nombreApellidoJefeFamilia)
        .put("numero_telefono", item.numeroTelefono)
        .put("fecha_llenado", fechaIsoObligatoria(item.fechaLlenado))
        .put("numero_carpeta", item.numeroCarpeta)
        .put("latitud", item.latitud ?: JSONObject.NULL)
        .put("longitud", item.longitud ?: JSONObject.NULL)
        .put("altitud", item.altitud ?: JSONObject.NULL)
        .put("responsable_nombre", item.responsableNombre)
        .put("responsable_codigo", item.responsableCodigo)
        .put("estado", item.estado)

    private fun miembroJson(item: MiembroFamiliaEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("grupo_edad", item.grupoEdad).put("apellidos_nombres", item.apellidosNombres)
        .put("parentesco", item.parentesco).put("fecha_nacimiento", fechaIso(item.fechaNacimiento))
        .put("ocupacion", item.ocupacion).put("sexo", item.sexo).put("escolaridad", item.escolaridad)
        .put("vacunas_completas", item.vacunasCompletas ?: JSONObject.NULL)
        .put("salud_bucal_adecuada", item.saludBucalAdecuada ?: JSONObject.NULL)
        .put("riesgo_enfermedad_discapacidad", item.riesgoEnfermedadDiscapacidad)
        .put("estado_nutricional", item.estadoNutricional)
        .put("hipertension_arterial", item.hipertensionArterial ?: JSONObject.NULL)
        .put("diabetes_mellitus", item.diabetesMellitus ?: JSONObject.NULL)
        .put("tuberculosis", item.tuberculosis ?: JSONObject.NULL)
        .put("problema_salud_mental", item.problemaSaludMental ?: JSONObject.NULL)
        .put("consumo_alcohol_drogas", item.consumoAlcoholDrogas ?: JSONObject.NULL)
        .put("enfermedad_cronica", item.enfermedadCronica ?: JSONObject.NULL)
        .put("discapacidad_visual", item.discapacidadVisual ?: JSONObject.NULL)
        .put("discapacidad_auditiva", item.discapacidadAuditiva ?: JSONObject.NULL)
        .put("discapacidad_lenguaje", item.discapacidadLenguaje ?: JSONObject.NULL)
        .put("discapacidad_fisica", item.discapacidadFisica ?: JSONObject.NULL)
        .put("discapacidad_intelectual", item.discapacidadIntelectual ?: JSONObject.NULL)
        .put("discapacidad_psicosocial", item.discapacidadPsicosocial ?: JSONObject.NULL)
        .put("cuidados_paliativos", item.cuidadosPaliativos ?: JSONObject.NULL)
        .put("vih", item.vih ?: JSONObject.NULL)
        .put("evento_salud", item.eventoSalud ?: JSONObject.NULL)
        .put("caso_confirmado", item.casoConfirmado ?: JSONObject.NULL)
        .put("caso_sospechoso_uno", item.casoSospechosoUno ?: JSONObject.NULL)
        .put("caso_sospechoso_dos", item.casoSospechosoDos ?: JSONObject.NULL)
        .put("prestador_comunitario", item.prestadorComunitario ?: JSONObject.NULL)
        .put("partero_ancestral", item.parteroAncestral ?: JSONObject.NULL)
        .put("sabiduria_ancestral", item.sabiduriaAncestral ?: JSONObject.NULL)
        .put("comorbilidades_cie10_json", item.comorbilidadesCie10Json)
        .put("porcentaje_discapacidad", item.porcentajeDiscapacidad ?: JSONObject.NULL)
        .put("necesita_ayuda_tecnica", item.necesitaAyudaTecnica ?: JSONObject.NULL)
        .put("enfermedad_cronica_descompensada", item.enfermedadCronicaDescompensada ?: JSONObject.NULL)
        .put("riesgo_genetico", item.riesgoGenetico ?: JSONObject.NULL)
        .put("victima_violencia", item.victimaViolencia ?: JSONObject.NULL)
        .put("privado_libertad", item.privadoLibertad ?: JSONObject.NULL)
        .put("numero_historia_clinica", item.numeroHistoriaClinica).put("cedula", item.cedula)

    private fun embarazadaJson(item: EmbarazadaEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("apellidos_nombres", item.apellidosNombres)
        .put("fecha_ultima_menstruacion", fechaIso(item.fechaUltimaMenstruacion))
        .put("fecha_probable_parto", fechaIso(item.fechaProbableParto))
        .put("semanas_gestacion", item.semanasGestacion ?: JSONObject.NULL)
        .put("dosis_dt_primera", item.dosisDtPrimera).put("dosis_dt_segunda", item.dosisDtSegunda)
        .put("dosis_dt_refuerzo", item.dosisDtRefuerzo).put("gestas", item.gestas ?: JSONObject.NULL)
        .put("partos", item.partos ?: JSONObject.NULL).put("abortos", item.abortos ?: JSONObject.NULL)
        .put("cesareas", item.cesareas ?: JSONObject.NULL)
        .put("antecedentes_patologicos_obstetricos", item.antecedentesPatologicosObstetricos)
        .put("riesgo_obstetrico", item.riesgoObstetrico)

    private fun mortalidadJson(item: MortalidadFamiliarEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("nombre", item.nombre).put("parentesco", item.parentesco)
        .put("edad_al_fallecer", item.edadAlFallecer ?: JSONObject.NULL).put("causa", item.causa)

    private fun calificacionJson(item: CalificacionRiesgoEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("fecha_calificacion", fechaIsoObligatoria(item.fechaCalificacion))
        .put("responsable", item.responsable).put("total", item.total).put("nivel", item.nivel)

    private fun valorJson(item: ValorRiesgoEntity, calificacionId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("calificacion_id", calificacionId)
        .put("componente", item.componente).put("valor", item.valor)

    private fun gestionJson(item: GestionRiesgoEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("fecha_analisis", fechaIsoObligatoria(item.fechaAnalisis))
        .put("numero", item.numero ?: JSONObject.NULL).put("compromiso_familia", item.compromisoFamilia)
        .put("compromiso_equipo_salud", item.compromisoEquipoSalud)
        .put("fecha_evaluacion", fechaIso(item.fechaEvaluacion)).put("cumplimiento", item.cumplimiento)
        .put("causas_incumplimiento_observaciones", item.causasIncumplimientoObservaciones)
        .put("responsable", item.responsable)

    private fun contaminacionJson(item: ContaminacionAmbientalEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("fecha_informe", fechaIsoObligatoria(item.fechaInforme))
        .put("tipo_contaminante_descripcion", item.tipoContaminanteDescripcion)
        .put("causante_contaminacion", item.causanteContaminacion)

    private fun lugarJson(item: LugarTratamientoEntity, fichaId: String, org: String) = JSONObject()
        .put("id", item.syncId).put("organizacion_id", org).put("ficha_id", fichaId)
        .put("descripcion", item.descripcion)

    private fun JSONObject.texto(clave: String): String =
        if (!has(clave) || isNull(clave)) "" else optString(clave, "")

    private fun JSONObject.intNullable(clave: String): Int? =
        if (!has(clave) || isNull(clave)) null else optInt(clave)

    private fun JSONObject.doubleNullable(clave: String): Double? =
        if (!has(clave) || isNull(clave)) null else optDouble(clave)

    private fun JSONObject.booleanNullable(clave: String): Boolean? =
        if (!has(clave) || isNull(clave)) null else optBoolean(clave)

    private fun fechaLocal(valor: String): String {
        if (valor.isBlank()) return ""
        if (valor.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) return valor
        return runCatching {
            val fecha = FORMATO_ISO.parse(valor) ?: return@runCatching ""
            FORMATO_LOCAL.format(fecha)
        }.getOrDefault("")
    }

    private fun timestampMillis(valor: String, fallback: Long): Long {
        if (valor.isBlank()) return fallback
        val normalizado = valor.replace(Regex("(\\.\\d{3})\\d+"), "\$1")
        FORMATOS_HORA_ENTRADA.forEach { formato ->
            runCatching { formato.parse(normalizado)?.time }.getOrNull()?.let { return it }
        }
        return fallback
    }

    private fun fechaIso(valor: String): Any = convertirFecha(valor) ?: JSONObject.NULL

    private fun fechaIsoObligatoria(valor: String): String =
        convertirFecha(valor) ?: FORMATO_ISO.format(Date())

    private fun convertirFecha(valor: String): String? {
        if (valor.isBlank()) return null
        FORMATOS_ENTRADA.forEach { formato ->
            try {
                val fecha = formato.parse(valor.trim()) ?: return@forEach
                return FORMATO_ISO.format(fecha)
            } catch (_: ParseException) {
                Unit
            }
        }
        return null
    }

    private fun fechaHoraIso(millis: Long): String = FORMATO_HORA_ISO.format(Date(millis))

    private companion object {
        val bloqueoProceso = Mutex()
        val FORMATOS_ENTRADA = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false },
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        )
        val FORMATO_ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val FORMATO_LOCAL = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
        val FORMATO_HORA_ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val FORMATOS_HORA_ENTRADA = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { isLenient = false },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { isLenient = false },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
        )
    }
}
