package com.ruralitos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import kotlinx.coroutines.flow.Flow

data class ResumenFichas(
    val total: Int,
    val borradores: Int,
    val completas: Int,
    val archivadas: Int
)

@Dao
interface FichaFamiliarDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun guardarFicha(ficha: FichaFamiliarEntity): Long

    @Update
    suspend fun actualizarFicha(ficha: FichaFamiliarEntity)

    @Delete
    suspend fun eliminarFicha(ficha: FichaFamiliarEntity)

    @Query("SELECT * FROM fichas_familiares ORDER BY actualizadoEn DESC")
    fun listarFichas(): Flow<List<FichaFamiliarEntity>>

    @Query("SELECT * FROM fichas_familiares WHERE cedulaJefeHogar = :cedula LIMIT 1")
    suspend fun buscarPorCedula(cedula: String): FichaFamiliarEntity?

    @Query("SELECT * FROM fichas_familiares WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): FichaFamiliarEntity?

    @Query("""
        SELECT COUNT(*) > 0 FROM fichas_familiares
        WHERE codigoUo = :codigoUo
          AND numeroFichaFamiliar = :numeroFicha
          AND id != :excluirId
    """)
    suspend fun existeNumeroFicha(
        codigoUo: String,
        numeroFicha: String,
        excluirId: Long = -1
    ): Boolean

    @Query("""
        SELECT COALESCE(MAX(CAST(numeroFichaFamiliar AS INTEGER)), 0) + 1
        FROM fichas_familiares
        WHERE territorioId = :barrioId
          AND numeroFichaFamiliar != ''
          AND numeroFichaFamiliar NOT GLOB '*[^0-9]*'
    """)
    suspend fun siguienteNumeroFichaBarrio(barrioId: String): Int

    @Query("""
        SELECT COUNT(*) > 0 FROM fichas_familiares
        WHERE territorioId = :barrioId AND numeroFichaFamiliar = :numeroFicha AND id != :excluirId
    """)
    suspend fun existeNumeroFichaEnBarrio(barrioId: String, numeroFicha: String, excluirId: Long = -1): Boolean

    @Query("SELECT * FROM fichas_familiares WHERE id = :id LIMIT 1")
    fun observarPorId(id: Long): Flow<FichaFamiliarEntity?>

    @Query("""
        UPDATE fichas_familiares SET
            sector = :sector,
            manzana = :manzana,
            numeroFamilia = :numeroFamilia,
            direccionHabitualFamilia = :direccion,
            barrio = :barrio,
            numeroCasa = :numeroCasa,
            comunidad = :comunidad,
            grupoCultural = :grupoCultural,
            latitud = :latitud,
            longitud = :longitud,
            altitud = :altitud,
            actualizadoPorUsuarioId = :usuarioId,
            actualizadoEn = :actualizadoEn
        WHERE id = :fichaId
    """)
    suspend fun actualizarUbicacion(
        fichaId: Long,
        sector: String,
        manzana: String,
        numeroFamilia: String,
        direccion: String,
        barrio: String,
        numeroCasa: String,
        comunidad: String,
        grupoCultural: String,
        latitud: Double?,
        longitud: Double?,
        altitud: Double?,
        usuarioId: Long? = null,
        actualizadoEn: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE fichas_familiares SET
            latitud = :latitud,
            longitud = :longitud,
            altitud = :altitud,
            actualizadoPorUsuarioId = :usuarioId,
            actualizadoEn = :actualizadoEn
        WHERE id = :fichaId
    """)
    suspend fun actualizarCoordenadas(
        fichaId: Long,
        latitud: Double,
        longitud: Double,
        altitud: Double?,
        usuarioId: Long? = null,
        actualizadoEn: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE fichas_familiares SET
            estado = :estado,
            actualizadoPorUsuarioId = :usuarioId,
            completadoPorUsuarioId = CASE WHEN :estado = 'COMPLETA' THEN :usuarioId ELSE completadoPorUsuarioId END,
            actualizadoEn = :actualizadoEn
        WHERE id = :fichaId
    """)
    suspend fun actualizarEstado(
        fichaId: Long,
        estado: String,
        usuarioId: Long? = null,
        actualizadoEn: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE fichas_familiares SET
            firmaUri = :firmaUri,
            actualizadoPorUsuarioId = :usuarioId,
            actualizadoEn = :actualizadoEn
        WHERE id = :fichaId
    """)
    suspend fun actualizarFirma(
        fichaId: Long,
        firmaUri: String?,
        usuarioId: Long?,
        actualizadoEn: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE fichas_familiares SET
            firmaUri = :firmaUri,
            actualizadoEn = :actualizadoEn,
            syncEstado = 'PENDIENTE',
            syncError = ''
        WHERE creadoPorUsuarioId = :usuarioId
           OR actualizadoPorUsuarioId = :usuarioId
           OR (:codigoAnterior != '' AND responsableCodigo = :codigoAnterior)
    """)
    suspend fun aplicarFirmaProfesional(
        usuarioId: Long,
        codigoAnterior: String,
        firmaUri: String?,
        actualizadoEn: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE fichas_familiares SET
            actualizadoPorUsuarioId = COALESCE(:usuarioId, actualizadoPorUsuarioId),
            actualizadoEn = :actualizadoEn,
            syncEstado = 'PENDIENTE',
            syncError = ''
        WHERE id = :fichaId
    """)
    suspend fun marcarPendiente(
        fichaId: Long,
        usuarioId: Long? = null,
        actualizadoEn: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE fichas_familiares SET
            actualizadoPorUsuarioId = :usuarioId,
            actualizadoEn = :actualizadoEn
        WHERE id = :fichaId
    """)
    suspend fun registrarModificacion(
        fichaId: Long,
        usuarioId: Long?,
        actualizadoEn: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE fichas_familiares SET
            cedulaJefeHogar = :cedula,
            nombreApellidoJefeFamilia = :nombre,
            numeroTelefono = :telefono,
            numeroFichaFamiliar = :numeroFicha,
            fechaLlenado = :fecha,
            numeroCarpeta = :carpeta,
            responsableNombre = :responsableNombre,
            responsableCodigo = :responsableCodigo,
            actualizadoPorUsuarioId = :usuarioId,
            actualizadoEn = :actualizadoEn
        WHERE id = :fichaId
    """)
    suspend fun actualizarDatosPrincipales(
        fichaId: Long,
        cedula: String,
        nombre: String,
        telefono: String,
        numeroFicha: String,
        fecha: String,
        carpeta: String,
        responsableNombre: String,
        responsableCodigo: String,
        usuarioId: Long? = null,
        actualizadoEn: Long = System.currentTimeMillis()
    )

    @Query("""
        SELECT * FROM fichas_familiares
        WHERE nombreApellidoJefeFamilia LIKE '%' || :texto || '%'
        OR cedulaJefeHogar LIKE '%' || :texto || '%'
        OR comunidad LIKE '%' || :texto || '%'
        OR EXISTS (SELECT 1 FROM miembros_familia AS m
                   WHERE m.fichaId = fichas_familiares.id
                     AND (m.apellidosNombres LIKE '%' || :texto || '%'
                          OR m.cedula LIKE '%' || :texto || '%'))
        ORDER BY actualizadoEn DESC
    """)
    fun buscarFichas(texto: String): Flow<List<FichaFamiliarEntity>>

    @Query("""
        SELECT * FROM fichas_familiares
        WHERE (:texto = '' OR nombreApellidoJefeFamilia LIKE '%' || :texto || '%'
            OR cedulaJefeHogar LIKE '%' || :texto || '%'
            OR comunidad LIKE '%' || :texto || '%'
            OR numeroFichaFamiliar LIKE '%' || :texto || '%'
            OR EXISTS (SELECT 1 FROM miembros_familia AS m
                       WHERE m.fichaId = fichas_familiares.id
                         AND (m.apellidosNombres LIKE '%' || :texto || '%'
                              OR m.cedula LIKE '%' || :texto || '%')))
          AND (:unidad = '' OR unidadOperativa LIKE '%' || :unidad || '%')
          AND (:sector = '' OR sector LIKE '%' || :sector || '%')
          AND (:fechaInicioClave = '' OR
            (substr(fechaLlenado, 7, 4) || substr(fechaLlenado, 4, 2) || substr(fechaLlenado, 1, 2)) >= :fechaInicioClave)
          AND (:fechaFinClave = '' OR
            (substr(fechaLlenado, 7, 4) || substr(fechaLlenado, 4, 2) || substr(fechaLlenado, 1, 2)) <= :fechaFinClave)
          AND (
            :estado = 'TODAS'
            OR (:estado = 'ACTIVAS' AND estado != 'ARCHIVADA')
            OR estado = :estado
          )
        ORDER BY actualizadoEn DESC
        LIMIT :limite OFFSET :desplazamiento
    """)
    fun buscarFichasFiltradasPaginadas(
        texto: String,
        unidad: String,
        sector: String,
        fechaInicioClave: String,
        fechaFinClave: String,
        estado: String,
        limite: Int,
        desplazamiento: Int
    ): Flow<List<FichaFamiliarEntity>>

    @Query("""
        SELECT COUNT(*) FROM fichas_familiares
        WHERE (:texto = '' OR nombreApellidoJefeFamilia LIKE '%' || :texto || '%'
            OR cedulaJefeHogar LIKE '%' || :texto || '%'
            OR comunidad LIKE '%' || :texto || '%'
            OR numeroFichaFamiliar LIKE '%' || :texto || '%'
            OR EXISTS (SELECT 1 FROM miembros_familia AS m
                       WHERE m.fichaId = fichas_familiares.id
                         AND (m.apellidosNombres LIKE '%' || :texto || '%'
                              OR m.cedula LIKE '%' || :texto || '%')))
          AND (:unidad = '' OR unidadOperativa LIKE '%' || :unidad || '%')
          AND (:sector = '' OR sector LIKE '%' || :sector || '%')
          AND (:fechaInicioClave = '' OR
            (substr(fechaLlenado, 7, 4) || substr(fechaLlenado, 4, 2) || substr(fechaLlenado, 1, 2)) >= :fechaInicioClave)
          AND (:fechaFinClave = '' OR
            (substr(fechaLlenado, 7, 4) || substr(fechaLlenado, 4, 2) || substr(fechaLlenado, 1, 2)) <= :fechaFinClave)
          AND (
            :estado = 'TODAS'
            OR (:estado = 'ACTIVAS' AND estado != 'ARCHIVADA')
            OR estado = :estado
          )
    """)
    fun contarFichasFiltradas(
        texto: String,
        unidad: String,
        sector: String,
        fechaInicioClave: String,
        fechaFinClave: String,
        estado: String
    ): Flow<Int>

    @Query("SELECT * FROM fichas_familiares WHERE fechaLlenado = :fecha ORDER BY actualizadoEn DESC")
    fun observarPorFecha(fecha: String): Flow<List<FichaFamiliarEntity>>

    @Query("""
        SELECT
            COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN estado = 'BORRADOR' THEN 1 ELSE 0 END), 0) AS borradores,
            COALESCE(SUM(CASE WHEN estado = 'COMPLETA' THEN 1 ELSE 0 END), 0) AS completas,
            COALESCE(SUM(CASE WHEN estado = 'ARCHIVADA' THEN 1 ELSE 0 END), 0) AS archivadas
        FROM fichas_familiares
    """)
    fun observarResumen(): Flow<ResumenFichas>
}
