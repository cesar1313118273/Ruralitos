package com.ruralitos.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ruralitos.app.data.local.dao.FichaFamiliarDao
import com.ruralitos.app.data.local.dao.FichaContenidoDao
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.GestionRiesgoEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.MortalidadFamiliarEntity
import com.ruralitos.app.data.local.entity.ValorRiesgoEntity
import com.ruralitos.app.data.local.dao.EstablecimientoSaludDao
import com.ruralitos.app.data.local.dao.UsuarioDao
import com.ruralitos.app.data.local.entity.EstablecimientoSaludEntity
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.data.local.entity.HistorialFichaEntity
import com.ruralitos.app.data.local.entity.EliminacionSyncEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import com.ruralitos.app.data.local.dao.HistorialFichaDao
import com.ruralitos.app.data.local.dao.SincronizacionDao
import com.ruralitos.app.data.local.dao.SalaDao
import com.ruralitos.app.data.local.dao.NotaDiariaDao
import com.ruralitos.app.data.local.entity.NotaDiariaEntity
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.data.local.dao.AgendaDao
import com.ruralitos.app.data.security.CifradorBaseDatos
import com.ruralitos.app.data.security.ClaveBaseDatos
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        FichaFamiliarEntity::class,
        EstablecimientoSaludEntity::class,
        MiembroFamiliaEntity::class,
        EmbarazadaEntity::class,
        MortalidadFamiliarEntity::class,
        CalificacionRiesgoEntity::class,
        ValorRiesgoEntity::class,
        GestionRiesgoEntity::class,
        ContaminacionAmbientalEntity::class,
        LugarTratamientoEntity::class,
        AdjuntoFichaEntity::class,
        UsuarioEntity::class,
        HistorialFichaEntity::class,
        EliminacionSyncEntity::class,
        SalaEntity::class,
        EaisSalaEntity::class,
        TerritorioSalaEntity::class,
        NotaDiariaEntity::class,
        ActividadAgendaEntity::class
    ],
    version = 22,
    exportSchema = false
)
abstract class RuralitosDatabase : RoomDatabase() {

    abstract fun fichaFamiliarDao(): FichaFamiliarDao
    abstract fun fichaContenidoDao(): FichaContenidoDao
    abstract fun establecimientoSaludDao(): EstablecimientoSaludDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun historialFichaDao(): HistorialFichaDao
    abstract fun sincronizacionDao(): SincronizacionDao
    abstract fun salaDao(): SalaDao
    abstract fun notaDiariaDao(): NotaDiariaDao
    abstract fun agendaDao(): AgendaDao

    companion object {
        @Volatile
        private var INSTANCE: RuralitosDatabase? = null

        fun obtenerBaseDatos(context: Context): RuralitosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val clave = ClaveBaseDatos.obtenerOCrear(context)
                    CifradorBaseDatos.preparar(context.applicationContext, "ruralitos_database", clave)
                    val factory = SupportOpenHelperFactory(clave.copyOf())
                    clave.fill(0)
                    val instancia = Room.databaseBuilder(
                        context.applicationContext,
                        RuralitosDatabase::class.java,
                        "ruralitos_database"
                    )
                        .openHelperFactory(factory)
                        .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8,
                            MIGRATION_8_9,
                            MIGRATION_9_10,
                            MIGRATION_10_11,
                            MIGRATION_11_12,
                            MIGRATION_12_13,
                            MIGRATION_13_14,
                            MIGRATION_14_15,
                            MIGRATION_15_16,
                            MIGRATION_16_17,
                            MIGRATION_17_18,
                            MIGRATION_18_19,
                            MIGRATION_19_20,
                            MIGRATION_20_21,
                            MIGRATION_21_22
                        )
                        .addCallback(SINCRONIZACION_CALLBACK)
                        .build()

                    INSTANCE = instancia
                    instancia
                }
            }
        }

        @Synchronized
        fun cerrarBaseDatos() {
            INSTANCE?.close()
            INSTANCE = null
        }

        /**
         * Los triggers no forman parte del esquema que Room crea desde las entidades.
         * Se reinstalan al crear y abrir la base para que una instalación nueva
         * conserve los cambios igual que una base migrada.
         */
        internal val SINCRONIZACION_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                crearTriggersSincronizacion(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                crearTriggersSincronizacion(db)
            }
        }

        internal fun crearTriggersSincronizacion(database: SupportSQLiteDatabase) {
            val hijas = listOf(
                "miembros_familia" to "miembros_familia",
                "embarazadas" to "embarazadas",
                "mortalidad_familiar" to "mortalidad_familiar",
                "calificaciones_riesgo" to "calificaciones_riesgo",
                "gestion_riesgo" to "gestion_riesgo",
                "contaminacion_ambiental" to "contaminacion_ambiental",
                "lugares_tratamiento" to "lugares_tratamiento",
                "adjuntos_ficha" to "adjuntos_ficha"
            )
            val nombres = buildList {
                add("sync_ficha_modificada")
                add("sync_ficha_eliminada")
                hijas.forEach { (tabla, _) ->
                    add("sync_${tabla}_insertado")
                    add("sync_${tabla}_actualizado")
                    add("sync_${tabla}_eliminado")
                }
                add("sync_valores_riesgo_insertado")
                add("sync_valores_riesgo_actualizado")
                add("sync_valores_riesgo_eliminado")
                add("sync_historial_insertado")
            }
            nombres.forEach { database.execSQL("DROP TRIGGER IF EXISTS `$it`") }

            val ahora = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"
            val siguienteActualizacion =
                "CASE WHEN actualizadoEn >= $ahora THEN actualizadoEn + 1 ELSE $ahora END"

            database.execSQL("""
                CREATE TRIGGER `sync_ficha_modificada`
                AFTER UPDATE ON `fichas_familiares`
                WHEN NEW.syncEstado = OLD.syncEstado
                     AND OLD.syncEstado IN ('SINCRONIZADO', 'ERROR')
                BEGIN
                    UPDATE fichas_familiares
                       SET syncEstado = 'PENDIENTE', syncError = ''
                     WHERE id = NEW.id;
                END
            """.trimIndent())
            database.execSQL("""
                CREATE TRIGGER `sync_ficha_eliminada`
                BEFORE DELETE ON `fichas_familiares`
                WHEN OLD.syncId <> '' AND OLD.syncEstado <> 'DESCARGANDO'
                BEGIN
                    INSERT OR IGNORE INTO eliminaciones_sync(tabla, registroSyncId, organizacionId, creadoEn)
                    VALUES('fichas_familiares', OLD.syncId, OLD.organizacionId, $ahora);
                END
            """.trimIndent())

            hijas.forEach { (tablaLocal, tablaRemota) ->
                database.execSQL("""
                    CREATE TRIGGER `sync_${tablaLocal}_insertado`
                    AFTER INSERT ON `$tablaLocal`
                    BEGIN
                        UPDATE fichas_familiares
                           SET syncEstado = 'PENDIENTE',
                               syncError = '',
                               actualizadoEn = $siguienteActualizacion
                         WHERE id = NEW.fichaId AND syncEstado <> 'DESCARGANDO';
                    END
                """.trimIndent())
                database.execSQL("""
                    CREATE TRIGGER `sync_${tablaLocal}_actualizado`
                    AFTER UPDATE ON `$tablaLocal`
                    BEGIN
                        UPDATE fichas_familiares
                           SET syncEstado = 'PENDIENTE',
                               syncError = '',
                               actualizadoEn = $siguienteActualizacion
                         WHERE id = NEW.fichaId AND syncEstado <> 'DESCARGANDO';
                    END
                """.trimIndent())
                database.execSQL("""
                    CREATE TRIGGER `sync_${tablaLocal}_eliminado`
                    BEFORE DELETE ON `$tablaLocal`
                    WHEN OLD.syncId <> '' AND COALESCE(
                        (SELECT syncEstado FROM fichas_familiares WHERE id = OLD.fichaId),
                        'DESCARGANDO'
                    ) <> 'DESCARGANDO'
                    BEGIN
                        INSERT OR IGNORE INTO eliminaciones_sync(tabla, registroSyncId, organizacionId, creadoEn)
                        SELECT '$tablaRemota', OLD.syncId, f.organizacionId, $ahora
                          FROM fichas_familiares f WHERE f.id = OLD.fichaId;
                        UPDATE fichas_familiares
                           SET syncEstado = 'PENDIENTE',
                               syncError = '',
                               actualizadoEn = $siguienteActualizacion
                         WHERE id = OLD.fichaId;
                    END
                """.trimIndent())
            }

            database.execSQL("""
                CREATE TRIGGER `sync_valores_riesgo_insertado`
                AFTER INSERT ON `valores_riesgo`
                BEGIN
                    UPDATE fichas_familiares
                       SET syncEstado = 'PENDIENTE', syncError = '',
                           actualizadoEn = $siguienteActualizacion
                     WHERE id = (SELECT fichaId FROM calificaciones_riesgo WHERE id = NEW.calificacionId)
                       AND syncEstado <> 'DESCARGANDO';
                END
            """.trimIndent())
            database.execSQL("""
                CREATE TRIGGER `sync_valores_riesgo_actualizado`
                AFTER UPDATE ON `valores_riesgo`
                BEGIN
                    UPDATE fichas_familiares
                       SET syncEstado = 'PENDIENTE', syncError = '',
                           actualizadoEn = $siguienteActualizacion
                     WHERE id = (SELECT fichaId FROM calificaciones_riesgo WHERE id = NEW.calificacionId)
                       AND syncEstado <> 'DESCARGANDO';
                END
            """.trimIndent())
            database.execSQL("""
                CREATE TRIGGER `sync_valores_riesgo_eliminado`
                BEFORE DELETE ON `valores_riesgo`
                WHEN OLD.syncId <> '' AND COALESCE((
                    SELECT f.syncEstado FROM calificaciones_riesgo c
                    JOIN fichas_familiares f ON f.id = c.fichaId
                    WHERE c.id = OLD.calificacionId
                ), 'DESCARGANDO') <> 'DESCARGANDO'
                BEGIN
                    INSERT OR IGNORE INTO eliminaciones_sync(tabla, registroSyncId, organizacionId, creadoEn)
                    SELECT 'valores_riesgo', OLD.syncId, f.organizacionId, $ahora
                      FROM calificaciones_riesgo c
                      JOIN fichas_familiares f ON f.id = c.fichaId
                     WHERE c.id = OLD.calificacionId;
                    UPDATE fichas_familiares
                       SET syncEstado = 'PENDIENTE', syncError = '',
                           actualizadoEn = $siguienteActualizacion
                     WHERE id = (SELECT fichaId FROM calificaciones_riesgo WHERE id = OLD.calificacionId);
                END
            """.trimIndent())
            database.execSQL("""
                CREATE TRIGGER `sync_historial_insertado`
                AFTER INSERT ON `historial_fichas`
                BEGIN
                    UPDATE fichas_familiares
                       SET syncEstado = 'PENDIENTE', syncError = '',
                           actualizadoEn = $siguienteActualizacion
                     WHERE id = NEW.fichaId AND syncEstado <> 'DESCARGANDO';
                END
            """.trimIndent())
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `establecimientos_salud` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `codigoUo` TEXT NOT NULL,
                        `nombreCentroSalud` TEXT NOT NULL,
                        `institucionSistema` TEXT NOT NULL,
                        `provinciaCodigoLocalizacion` TEXT NOT NULL,
                        `provincia` TEXT NOT NULL,
                        `cantonCodigoLocalizacion` TEXT NOT NULL,
                        `canton` TEXT NOT NULL,
                        `parroquiaCodigoLocalizacion` TEXT NOT NULL,
                        `parroquia` TEXT NOT NULL,
                        `sector` TEXT NOT NULL,
                        `areaNumero` TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_establecimientos_salud_codigoUo` ON `establecimientos_salud` (`codigoUo`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_establecimientos_salud_nombreCentroSalud` ON `establecimientos_salud` (`nombreCentroSalud`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP INDEX IF EXISTS `index_fichas_familiares_cedulaJefeHogar`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_fichas_familiares_cedulaJefeHogar` ON `fichas_familiares` (`cedulaJefeHogar`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_fichas_familiares_codigoUo` ON `fichas_familiares` (`codigoUo`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_fichas_familiares_fechaLlenado` ON `fichas_familiares` (`fechaLlenado`)")

                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `latitud` REAL")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `longitud` REAL")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `altitud` REAL")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `responsableNombre` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `responsableCodigo` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `firmaUri` TEXT")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `estado` TEXT NOT NULL DEFAULT 'BORRADOR'")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `creadoEn` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `actualizadoEn` INTEGER NOT NULL DEFAULT 0")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `miembros_familia` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fichaId` INTEGER NOT NULL,
                        `grupoEdad` TEXT NOT NULL,
                        `apellidosNombres` TEXT NOT NULL,
                        `parentesco` TEXT NOT NULL,
                        `fechaNacimiento` TEXT NOT NULL,
                        `ocupacion` TEXT NOT NULL,
                        `sexo` TEXT NOT NULL,
                        `escolaridad` TEXT NOT NULL,
                        `vacunasCompletas` INTEGER,
                        `saludBucalAdecuada` INTEGER,
                        `riesgoEnfermedadDiscapacidad` TEXT NOT NULL,
                        `numeroHistoriaClinica` TEXT NOT NULL,
                        `cedula` TEXT NOT NULL,
                        FOREIGN KEY(`fichaId`) REFERENCES `fichas_familiares`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_miembros_familia_fichaId` ON `miembros_familia` (`fichaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_miembros_familia_cedula` ON `miembros_familia` (`cedula`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `embarazadas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fichaId` INTEGER NOT NULL,
                        `apellidosNombres` TEXT NOT NULL,
                        `fechaUltimaMenstruacion` TEXT NOT NULL,
                        `fechaProbableParto` TEXT NOT NULL,
                        `semanasGestacion` INTEGER,
                        `dosisDtPrimera` INTEGER NOT NULL,
                        `dosisDtSegunda` INTEGER NOT NULL,
                        `dosisDtRefuerzo` INTEGER NOT NULL,
                        `gestas` INTEGER,
                        `partos` INTEGER,
                        `abortos` INTEGER,
                        `cesareas` INTEGER,
                        `antecedentesPatologicosObstetricos` TEXT NOT NULL,
                        FOREIGN KEY(`fichaId`) REFERENCES `fichas_familiares`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_embarazadas_fichaId` ON `embarazadas` (`fichaId`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `mortalidad_familiar` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fichaId` INTEGER NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `parentesco` TEXT NOT NULL,
                        `edadAlFallecer` INTEGER,
                        `causa` TEXT NOT NULL,
                        FOREIGN KEY(`fichaId`) REFERENCES `fichas_familiares`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_mortalidad_familiar_fichaId` ON `mortalidad_familiar` (`fichaId`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `calificaciones_riesgo` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fichaId` INTEGER NOT NULL,
                        `fechaCalificacion` TEXT NOT NULL,
                        `responsable` TEXT NOT NULL,
                        `total` INTEGER NOT NULL,
                        `nivel` TEXT NOT NULL,
                        FOREIGN KEY(`fichaId`) REFERENCES `fichas_familiares`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_calificaciones_riesgo_fichaId` ON `calificaciones_riesgo` (`fichaId`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `valores_riesgo` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `calificacionId` INTEGER NOT NULL,
                        `componente` INTEGER NOT NULL,
                        `valor` INTEGER NOT NULL,
                        FOREIGN KEY(`calificacionId`) REFERENCES `calificaciones_riesgo`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_valores_riesgo_calificacionId` ON `valores_riesgo` (`calificacionId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_valores_riesgo_calificacionId_componente` ON `valores_riesgo` (`calificacionId`, `componente`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gestion_riesgo` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fichaId` INTEGER NOT NULL,
                        `fechaAnalisis` TEXT NOT NULL,
                        `numero` INTEGER,
                        `compromisoFamilia` TEXT NOT NULL,
                        `compromisoEquipoSalud` TEXT NOT NULL,
                        `fechaEvaluacion` TEXT NOT NULL,
                        `cumplimiento` TEXT NOT NULL,
                        `causasIncumplimientoObservaciones` TEXT NOT NULL,
                        `responsable` TEXT NOT NULL,
                        FOREIGN KEY(`fichaId`) REFERENCES `fichas_familiares`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_gestion_riesgo_fichaId` ON `gestion_riesgo` (`fichaId`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `contaminacion_ambiental` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fichaId` INTEGER NOT NULL,
                        `fechaInforme` TEXT NOT NULL,
                        `tipoContaminanteDescripcion` TEXT NOT NULL,
                        `causanteContaminacion` TEXT NOT NULL,
                        FOREIGN KEY(`fichaId`) REFERENCES `fichas_familiares`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_contaminacion_ambiental_fichaId` ON `contaminacion_ambiental` (`fichaId`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `lugares_tratamiento` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fichaId` INTEGER NOT NULL,
                        `descripcion` TEXT NOT NULL,
                        FOREIGN KEY(`fichaId`) REFERENCES `fichas_familiares`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_lugares_tratamiento_fichaId` ON `lugares_tratamiento` (`fichaId`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `adjuntos_ficha` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fichaId` INTEGER NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `uri` TEXT NOT NULL,
                        `actualizadoEn` INTEGER NOT NULL,
                        FOREIGN KEY(`fichaId`) REFERENCES `fichas_familiares`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_adjuntos_ficha_fichaId` ON `adjuntos_ficha` (`fichaId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_adjuntos_ficha_fichaId_tipo` ON `adjuntos_ficha` (`fichaId`, `tipo`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `usuarios` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cedula` TEXT NOT NULL,
                        `nombres` TEXT NOT NULL,
                        `cargo` TEXT NOT NULL,
                        `rol` TEXT NOT NULL,
                        `claveHash` TEXT NOT NULL,
                        `claveSalt` TEXT NOT NULL,
                        `activo` INTEGER NOT NULL,
                        `creadoEn` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_usuarios_cedula` ON `usuarios` (`cedula`)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `creadoPorUsuarioId` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `actualizadoPorUsuarioId` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `completadoPorUsuarioId` INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `historial_fichas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fichaId` INTEGER NOT NULL,
                        `numeroFicha` TEXT NOT NULL,
                        `usuarioId` INTEGER,
                        `usuarioNombre` TEXT NOT NULL,
                        `accion` TEXT NOT NULL,
                        `detalle` TEXT NOT NULL,
                        `creadoEn` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_historial_fichas_fichaId` ON `historial_fichas` (`fichaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_historial_fichas_creadoEn` ON `historial_fichas` (`creadoEn`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `correo` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `telefono` TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_fichas_familiares_estado` ON `fichas_familiares` (`estado`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_fichas_familiares_actualizadoEn` ON `fichas_familiares` (`actualizadoEn`)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val uuidSql = """
                    lower(hex(randomblob(4))) || '-' ||
                    lower(hex(randomblob(2))) || '-' ||
                    '4' || substr(lower(hex(randomblob(2))), 2) || '-' ||
                    substr('89ab', abs(random()) % 4 + 1, 1) ||
                    substr(lower(hex(randomblob(2))), 2) || '-' ||
                    lower(hex(randomblob(6)))
                """.trimIndent().replace("\n", " ")

                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `supabaseId` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `organizacionId` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `establecimientoRemotoId` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `pinConfigurado` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `ultimoAccesoEn` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_usuarios_supabaseId` ON `usuarios` (`supabaseId`)")

                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `syncEstado` TEXT NOT NULL DEFAULT 'PENDIENTE'")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `syncVersion` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `syncError` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `organizacionId` TEXT NOT NULL DEFAULT ''")
                database.execSQL("UPDATE `fichas_familiares` SET `syncId` = $uuidSql WHERE `syncId` = ''")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_fichas_familiares_syncId` ON `fichas_familiares` (`syncId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_fichas_familiares_syncEstado` ON `fichas_familiares` (`syncEstado`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_fichas_familiares_organizacionId` ON `fichas_familiares` (`organizacionId`)")

                val tablasConSyncId = listOf(
                    "miembros_familia",
                    "embarazadas",
                    "mortalidad_familiar",
                    "calificaciones_riesgo",
                    "valores_riesgo",
                    "gestion_riesgo",
                    "contaminacion_ambiental",
                    "lugares_tratamiento",
                    "adjuntos_ficha",
                    "historial_fichas"
                )
                tablasConSyncId.forEach { tabla ->
                    database.execSQL("ALTER TABLE `$tabla` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                    database.execSQL("UPDATE `$tabla` SET `syncId` = $uuidSql WHERE `syncId` = ''")
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${tabla}_syncId` ON `$tabla` (`syncId`)")
                }

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `eliminaciones_sync` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tabla` TEXT NOT NULL,
                        `registroSyncId` TEXT NOT NULL,
                        `organizacionId` TEXT NOT NULL,
                        `creadoEn` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_eliminaciones_sync_tabla_registroSyncId` ON `eliminaciones_sync` (`tabla`, `registroSyncId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_eliminaciones_sync_organizacionId` ON `eliminaciones_sync` (`organizacionId`)")

                database.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS `sync_ficha_modificada`
                    AFTER UPDATE ON `fichas_familiares`
                    WHEN NEW.syncEstado = OLD.syncEstado
                         AND OLD.syncEstado IN ('SINCRONIZADO', 'ERROR')
                    BEGIN
                        UPDATE fichas_familiares
                           SET syncEstado = 'PENDIENTE', syncError = ''
                         WHERE id = NEW.id;
                    END
                """.trimIndent())
                database.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS `sync_ficha_eliminada`
                    BEFORE DELETE ON `fichas_familiares`
                    WHEN OLD.syncId <> ''
                    BEGIN
                        INSERT OR IGNORE INTO eliminaciones_sync(tabla, registroSyncId, organizacionId, creadoEn)
                        VALUES('fichas_familiares', OLD.syncId, OLD.organizacionId, CAST(strftime('%s','now') AS INTEGER) * 1000);
                    END
                """.trimIndent())

                val hijas = listOf(
                    "miembros_familia" to "miembros_familia",
                    "embarazadas" to "embarazadas",
                    "mortalidad_familiar" to "mortalidad_familiar",
                    "calificaciones_riesgo" to "calificaciones_riesgo",
                    "gestion_riesgo" to "gestion_riesgo",
                    "contaminacion_ambiental" to "contaminacion_ambiental",
                    "lugares_tratamiento" to "lugares_tratamiento",
                    "adjuntos_ficha" to "adjuntos_ficha"
                )
                hijas.forEach { (tablaLocal, tablaRemota) ->
                    database.execSQL("""
                        CREATE TRIGGER IF NOT EXISTS `sync_${tablaLocal}_insertado`
                        AFTER INSERT ON `$tablaLocal`
                        BEGIN
                            UPDATE fichas_familiares
                               SET syncEstado = 'PENDIENTE', syncError = ''
                             WHERE id = NEW.fichaId;
                        END
                    """.trimIndent())
                    database.execSQL("""
                        CREATE TRIGGER IF NOT EXISTS `sync_${tablaLocal}_actualizado`
                        AFTER UPDATE ON `$tablaLocal`
                        BEGIN
                            UPDATE fichas_familiares
                               SET syncEstado = 'PENDIENTE', syncError = ''
                             WHERE id = NEW.fichaId;
                        END
                    """.trimIndent())
                    database.execSQL("""
                        CREATE TRIGGER IF NOT EXISTS `sync_${tablaLocal}_eliminado`
                        BEFORE DELETE ON `$tablaLocal`
                        WHEN OLD.syncId <> ''
                        BEGIN
                            INSERT OR IGNORE INTO eliminaciones_sync(tabla, registroSyncId, organizacionId, creadoEn)
                            SELECT '$tablaRemota', OLD.syncId, f.organizacionId, CAST(strftime('%s','now') AS INTEGER) * 1000
                              FROM fichas_familiares f WHERE f.id = OLD.fichaId;
                            UPDATE fichas_familiares
                               SET syncEstado = 'PENDIENTE', syncError = ''
                             WHERE id = OLD.fichaId;
                        END
                    """.trimIndent())
                }

                database.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS `sync_valores_riesgo_insertado`
                    AFTER INSERT ON `valores_riesgo`
                    BEGIN
                        UPDATE fichas_familiares SET syncEstado = 'PENDIENTE', syncError = ''
                         WHERE id = (SELECT fichaId FROM calificaciones_riesgo WHERE id = NEW.calificacionId);
                    END
                """.trimIndent())
                database.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS `sync_valores_riesgo_actualizado`
                    AFTER UPDATE ON `valores_riesgo`
                    BEGIN
                        UPDATE fichas_familiares SET syncEstado = 'PENDIENTE', syncError = ''
                         WHERE id = (SELECT fichaId FROM calificaciones_riesgo WHERE id = NEW.calificacionId);
                    END
                """.trimIndent())
                database.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS `sync_historial_insertado`
                    AFTER INSERT ON `historial_fichas`
                    BEGIN
                        UPDATE fichas_familiares SET syncEstado = 'PENDIENTE', syncError = ''
                         WHERE id = NEW.fichaId;
                    END
                """.trimIndent())
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS `sync_valores_riesgo_eliminado`
                    BEFORE DELETE ON `valores_riesgo`
                    WHEN OLD.syncId <> ''
                    BEGIN
                        INSERT OR IGNORE INTO eliminaciones_sync(tabla, registroSyncId, organizacionId, creadoEn)
                        SELECT 'valores_riesgo', OLD.syncId, f.organizacionId,
                               CAST(strftime('%s','now') AS INTEGER) * 1000
                          FROM calificaciones_riesgo c
                          JOIN fichas_familiares f ON f.id = c.fichaId
                         WHERE c.id = OLD.calificacionId;
                        UPDATE fichas_familiares
                           SET syncEstado = 'PENDIENTE', syncError = ''
                         WHERE id = (
                             SELECT fichaId FROM calificaciones_riesgo
                              WHERE id = OLD.calificacionId
                         );
                    END
                """.trimIndent())
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `codigoSenescyt` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `firmaUri` TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS salas (
                        organizacionId TEXT NOT NULL PRIMARY KEY,
                        establecimientoId INTEGER,
                        nombreSala TEXT NOT NULL,
                        codigoSala TEXT NOT NULL,
                        rol TEXT NOT NULL,
                        permiso TEXT NOT NULL,
                        activa INTEGER NOT NULL,
                        codigoUo TEXT NOT NULL,
                        nombreCentroSalud TEXT NOT NULL,
                        institucionSistema TEXT NOT NULL,
                        provinciaCodigoLocalizacion TEXT NOT NULL,
                        provincia TEXT NOT NULL,
                        cantonCodigoLocalizacion TEXT NOT NULL,
                        canton TEXT NOT NULL,
                        parroquiaCodigoLocalizacion TEXT NOT NULL,
                        parroquia TEXT NOT NULL,
                        sector TEXT NOT NULL,
                        areaNumero TEXT NOT NULL,
                        actualizadoEn INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_salas_establecimientoId ON salas(establecimientoId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_salas_activa ON salas(activa)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_salas_nombreSala ON salas(nombreSala)")
                database.execSQL("""
                    INSERT OR IGNORE INTO salas (
                        organizacionId, establecimientoId, nombreSala, codigoSala, rol, permiso,
                        activa, codigoUo, nombreCentroSalud, institucionSistema,
                        provinciaCodigoLocalizacion, provincia, cantonCodigoLocalizacion, canton,
                        parroquiaCodigoLocalizacion, parroquia, sector, areaNumero, actualizadoEn
                    )
                    SELECT
                        u.organizacionId,
                        MAX(u.establecimientoRemotoId),
                        COALESCE(MAX(NULLIF(f.unidadOperativa, '')), 'Sala Ruralitos'),
                        '',
                        MAX(u.rol),
                        CASE WHEN MAX(u.rol) IN ('ADMIN', 'MEDICO') THEN
                            CASE WHEN MAX(u.rol) = 'ADMIN' THEN 'ADMINISTRADOR' ELSE 'EDITOR' END
                        ELSE 'LECTOR' END,
                        1,
                        COALESCE(MAX(f.codigoUo), ''),
                        COALESCE(MAX(f.unidadOperativa), ''),
                        COALESCE(MAX(f.institucionSistema), ''),
                        COALESCE(MAX(f.provinciaCodigoLocalizacion), ''),
                        COALESCE(MAX(f.provincia), ''),
                        COALESCE(MAX(f.cantonCodigoLocalizacion), ''),
                        COALESCE(MAX(f.canton), ''),
                        COALESCE(MAX(f.parroquiaCodigoLocalizacion), ''),
                        COALESCE(MAX(f.parroquia), ''),
                        COALESCE(MAX(f.sector), ''),
                        COALESCE(MAX(f.areaNumero), ''),
                        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)
                    FROM usuarios u
                    LEFT JOIN fichas_familiares f
                      ON f.organizacionId = u.organizacionId
                    WHERE u.organizacionId <> ''
                    GROUP BY u.organizacionId
                """.trimIndent())

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS eais_sala (
                        id TEXT NOT NULL PRIMARY KEY,
                        salaId TEXT NOT NULL,
                        numero INTEGER NOT NULL,
                        activo INTEGER NOT NULL,
                        actualizadoEn INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_eais_sala_salaId_numero ON eais_sala(salaId, numero)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_eais_sala_salaId_activo ON eais_sala(salaId, activo)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS territorios_sala (
                        id TEXT NOT NULL PRIMARY KEY,
                        salaId TEXT NOT NULL,
                        eaisId TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        nombre TEXT NOT NULL,
                        activo INTEGER NOT NULL,
                        actualizadoEn INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_territorios_sala_salaId ON territorios_sala(salaId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_territorios_sala_eaisId_activo ON territorios_sala(eaisId, activo)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_territorios_sala_eaisId_tipo_nombre ON territorios_sala(eaisId, tipo, nombre)")

                database.execSQL("ALTER TABLE fichas_familiares ADD COLUMN establecimientoRemotoId INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE fichas_familiares ADD COLUMN eaisId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE fichas_familiares ADD COLUMN territorioId TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_fichas_familiares_eaisId ON fichas_familiares(eaisId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_fichas_familiares_territorioId ON fichas_familiares(territorioId)")
            }
        }
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `estadoNutricional` TEXT NOT NULL DEFAULT ''")
                listOf(
                    "hipertensionArterial",
                    "diabetesMellitus",
                    "tuberculosis",
                    "problemaSaludMental",
                    "consumoAlcoholDrogas",
                    "enfermedadCronica",
                    "discapacidadVisual",
                    "discapacidadAuditiva",
                    "discapacidadLenguaje",
                    "discapacidadFisica",
                    "discapacidadIntelectual"
                ).forEach { columna ->
                    database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `$columna` INTEGER DEFAULT NULL")
                }
            }
        }
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                listOf(
                    "discapacidadPsicosocial",
                    "cuidadosPaliativos",
                    "vih",
                    "eventoSalud",
                    "casoConfirmado",
                    "casoSospechosoUno",
                    "casoSospechosoDos",
                    "prestadorComunitario",
                    "parteroAncestral",
                    "sabiduriaAncestral"
                ).forEach { columna ->
                    database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `$columna` INTEGER DEFAULT NULL")
                }
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `comorbilidadesCie10Json` TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `porcentajeDiscapacidad` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `necesitaAyudaTecnica` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `enfermedadCronicaDescompensada` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `riesgoGenetico` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `victimaViolencia` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `privadoLibertad` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `embarazadas` ADD COLUMN `riesgoObstetrico` TEXT NOT NULL DEFAULT ''")
                crearTriggersSincronizacion(database)
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notas_diarias` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `miembroId` INTEGER NOT NULL,
                        `fechaLocal` TEXT NOT NULL,
                        `contenido` TEXT NOT NULL,
                        `creadaEn` INTEGER NOT NULL,
                        FOREIGN KEY(`miembroId`) REFERENCES `miembros_familia`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_notas_diarias_miembroId` ON `notas_diarias` (`miembroId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_notas_diarias_fechaLocal` ON `notas_diarias` (`fechaLocal`)")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `actividades_agenda` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `usuarioId` INTEGER NOT NULL,
                        `fichaId` INTEGER,
                        `miembroId` INTEGER,
                        `persona` TEXT NOT NULL,
                        `cedula` TEXT NOT NULL,
                        `barrio` TEXT NOT NULL,
                        `fechaHora` INTEGER NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `nota` TEXT NOT NULL,
                        `estado` TEXT NOT NULL,
                        `recordar` INTEGER NOT NULL,
                        `creadoEn` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_actividades_agenda_usuarioId` ON `actividades_agenda` (`usuarioId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_actividades_agenda_fechaHora` ON `actividades_agenda` (`fechaHora`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_actividades_agenda_fichaId` ON `actividades_agenda` (`fichaId`)")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `origen` TEXT NOT NULL DEFAULT 'MANUAL'")
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `grupoRiesgo` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `fechaBase` INTEGER")
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `fechaEditada` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `notas_diarias` ADD COLUMN `realizada` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val uuid = "lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-' || " +
                    "lower(hex(randomblob(2))) || '-' || lower(hex(randomblob(2))) || '-' || lower(hex(randomblob(6)))"
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `organizacionId` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `syncEstado` TEXT NOT NULL DEFAULT 'PENDIENTE'")
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `syncVersion` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `actualizadoEn` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `actividades_agenda` ADD COLUMN `eliminadoEn` INTEGER")
                database.execSQL("UPDATE `actividades_agenda` SET `syncId` = $uuid, `actualizadoEn` = `creadoEn`")
                database.execSQL("UPDATE `actividades_agenda` SET `organizacionId` = COALESCE((SELECT f.organizacionId FROM fichas_familiares f WHERE f.id = actividades_agenda.fichaId), (SELECT u.organizacionId FROM usuarios u WHERE u.id = actividades_agenda.usuarioId), '')")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_actividades_agenda_syncId` ON `actividades_agenda` (`syncId`)")
                database.execSQL("ALTER TABLE `notas_diarias` ADD COLUMN `usuarioId` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `notas_diarias` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `notas_diarias` ADD COLUMN `syncEstado` TEXT NOT NULL DEFAULT 'PENDIENTE'")
                database.execSQL("ALTER TABLE `notas_diarias` ADD COLUMN `syncVersion` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `notas_diarias` ADD COLUMN `actualizadoEn` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `notas_diarias` ADD COLUMN `eliminadoEn` INTEGER")
                database.execSQL("UPDATE `notas_diarias` SET `syncId` = $uuid, `syncEstado` = 'LOCAL', `actualizadoEn` = `creadaEn`")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notas_diarias_syncId` ON `notas_diarias` (`syncId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_notas_diarias_usuarioId` ON `notas_diarias` (`usuarioId`)")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `fichas_familiares` ADD COLUMN `croquisElementosJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `miembros_familia` ADD COLUMN `factoresRiesgoEdadJson` TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE `embarazadas` ADD COLUMN `factoresObstetricosJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `sexo` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `apellidos` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
