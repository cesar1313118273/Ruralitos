package com.ruralitos.app.data.sync

import androidx.room.withTransaction
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import com.ruralitos.app.data.remote.SupabaseApi

object SincronizadorSalas {
    /**
     * Primero se trae TODO de la red y solo después se toca la base, en una sola transacción:
     * si la conexión falla a medias, las Salas, EAIS y barrios que ya había en el teléfono quedan intactos.
     */
    suspend fun actualizar(
        database: RuralitosDatabase,
        api: SupabaseApi
    ): List<SalaEntity> {
        val remotas = api.obtenerSalas()
        val eaisPorSala = remotas.associate { it.organizacionId to api.obtenerEais(it.organizacionId) }
        val territoriosPorSala = remotas.associate { it.organizacionId to api.obtenerTerritorios(it.organizacionId) }
        val ahora = System.currentTimeMillis()
        val dao = database.salaDao()
        val salas = remotas.map { sala ->
            SalaEntity(
                organizacionId = sala.organizacionId,
                establecimientoId = sala.establecimientoId,
                nombreSala = sala.nombreSala,
                codigoSala = sala.codigoSala,
                rol = sala.rol,
                permiso = sala.permiso,
                activa = true,
                codigoUo = sala.codigoUo,
                nombreCentroSalud = sala.nombreCentroSalud,
                institucionSistema = sala.institucionSistema,
                provinciaCodigoLocalizacion = sala.provinciaCodigoLocalizacion,
                provincia = sala.provincia,
                cantonCodigoLocalizacion = sala.cantonCodigoLocalizacion,
                canton = sala.canton,
                parroquiaCodigoLocalizacion = sala.parroquiaCodigoLocalizacion,
                parroquia = sala.parroquia,
                sector = sala.sector,
                areaNumero = sala.areaNumero,
                actualizadoEn = ahora
            )
        }
        database.withTransaction {
            dao.desactivarSalas()
            if (salas.isNotEmpty()) dao.guardarSalas(salas)
            salas.forEach { sala ->
                dao.desactivarEais(sala.organizacionId)
                dao.desactivarTerritorios(sala.organizacionId)
                val eais = eaisPorSala.getValue(sala.organizacionId).map {
                    EaisSalaEntity(
                        id = it.id,
                        salaId = it.organizacionId,
                        numero = it.numero,
                        activo = it.activo,
                        actualizadoEn = ahora
                    )
                }
                val territorios = territoriosPorSala.getValue(sala.organizacionId).map {
                    TerritorioSalaEntity(
                        id = it.id,
                        salaId = it.organizacionId,
                        eaisId = it.eaisId,
                        tipo = it.tipo,
                        nombre = it.nombre,
                        activo = it.activo,
                        actualizadoEn = ahora
                    )
                }
                if (eais.isNotEmpty()) dao.guardarEais(eais)
                if (territorios.isNotEmpty()) dao.guardarTerritorios(territorios)
                // Se conservan los EAIS y barrios inactivos: las fichas históricas
                // siguen apuntando a sus identificadores y deben poder editarse.
            }
        }
        val activa = api.organizacionGuardada()
        if (activa == null || salas.none { it.organizacionId == activa }) {
            api.guardarOrganizacionActiva(salas.firstOrNull()?.organizacionId)
        }
        return salas
    }
}
