package com.ruralitos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruralitos.app.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun guardar(usuario: UsuarioEntity): Long

    @Query("SELECT * FROM usuarios ORDER BY nombres COLLATE NOCASE ASC")
    fun observarTodos(): Flow<List<UsuarioEntity>>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE cedula = :cedula LIMIT 1")
    suspend fun buscarPorCedula(cedula: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE supabaseId = :supabaseId LIMIT 1")
    suspend fun buscarPorSupabaseId(supabaseId: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE codigoSenescyt = :codigo LIMIT 1")
    suspend fun buscarPorCodigoSenescyt(codigo: String): UsuarioEntity?

    @Query("UPDATE usuarios SET activo = :activo WHERE id = :id")
    suspend fun cambiarEstado(id: Long, activo: Boolean)

    @Query("UPDATE usuarios SET claveHash = :hash, claveSalt = :salt WHERE id = :id")
    suspend fun actualizarClave(id: Long, hash: String, salt: String)

    @Query("""
        UPDATE usuarios SET
            cedula = :cedula,
            nombres = :nombres,
            cargo = :cargo,
            correo = :correo,
            telefono = :telefono,
            codigoSenescyt = :codigoSenescyt,
            sexo = :sexo,
            apellidos = :apellidos
        WHERE id = :id
    """)
    suspend fun actualizarPerfil(
        id: Long,
        cedula: String,
        nombres: String,
        cargo: String,
        correo: String,
        telefono: String,
        codigoSenescyt: String,
        sexo: String,
        apellidos: String
    )

    @Query("UPDATE usuarios SET codigoSenescyt = :codigo, firmaUri = :firmaUri WHERE id = :id")
    suspend fun actualizarCredencialesProfesionales(id: Long, codigo: String, firmaUri: String?)

    @Query("""
        UPDATE usuarios SET
            cedula = :cedula,
            nombres = :nombres,
            cargo = :cargo,
            rol = :rol,
            correo = :correo,
            telefono = :telefono,
            activo = :activo,
            supabaseId = :supabaseId,
            organizacionId = :organizacionId,
            establecimientoRemotoId = :establecimientoRemotoId,
            codigoSenescyt = :codigoSenescyt,
            sexo = CASE WHEN :sexo <> '' THEN :sexo ELSE sexo END,
            apellidos = CASE WHEN :apellidos <> '' THEN :apellidos ELSE apellidos END
        WHERE id = :id
    """)
    suspend fun actualizarDesdeSupabase(
        id: Long,
        cedula: String,
        nombres: String,
        cargo: String,
        rol: String,
        correo: String,
        telefono: String,
        activo: Boolean,
        supabaseId: String,
        organizacionId: String,
        establecimientoRemotoId: Long?,
        codigoSenescyt: String,
        sexo: String,
        apellidos: String
    )

    @Query("""
        UPDATE usuarios SET
            claveHash = :hash,
            claveSalt = :salt,
            pinConfigurado = 1
        WHERE id = :id
    """)
    suspend fun guardarPin(id: Long, hash: String, salt: String)

    @Query("UPDATE usuarios SET ultimoAccesoEn = :ahora WHERE id = :id")
    suspend fun registrarAcceso(id: Long, ahora: Long = System.currentTimeMillis())
}
