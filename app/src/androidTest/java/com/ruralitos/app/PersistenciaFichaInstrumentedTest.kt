package com.ruralitos.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenciaFichaInstrumentedTest {
    private lateinit var database: RuralitosDatabase

    @Before
    fun preparar() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RuralitosDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(RuralitosDatabase.SINCRONIZACION_CALLBACK)
            .build()
        database.openHelper.writableDatabase
    }

    @After
    fun cerrar() {
        database.close()
    }

    @Test
    fun guardarYEditarContenidoMarcaLaFichaPendienteSinPerderDatos() = runBlocking {
        val id = database.fichaFamiliarDao().guardarFicha(
            fichaBase(syncEstado = "SINCRONIZADO", actualizadoEn = 1_000L)
        )
        val miembroId = database.fichaContenidoDao().guardarMiembro(
            miembroBase(fichaId = id, nombre = "Persona inicial")
        )

        val despuesDeInsertar = requireNotNull(database.fichaFamiliarDao().buscarPorId(id))
        assertEquals("PENDIENTE", despuesDeInsertar.syncEstado)
        assertTrue(despuesDeInsertar.actualizadoEn > 1_000L)
        assertEquals(
            "Persona inicial",
            database.sincronizacionDao().miembros(id).single().apellidosNombres
        )

        val miembro = database.sincronizacionDao().miembros(id).single()
        database.fichaContenidoDao().actualizarMiembro(
            miembro.copy(id = miembroId, apellidosNombres = "Persona modificada")
        )

        assertEquals(
            "Persona modificada",
            database.sincronizacionDao().miembros(id).single().apellidosNombres
        )
        assertEquals(
            "PENDIENTE",
            requireNotNull(database.fichaFamiliarDao().buscarPorId(id)).syncEstado
        )
    }

    @Test
    fun editarDatosPrincipalesPersisteLosValoresYMarcaPendiente() = runBlocking {
        val id = database.fichaFamiliarDao().guardarFicha(
            fichaBase(syncEstado = "SINCRONIZADO", actualizadoEn = 1_500L)
        )

        database.fichaFamiliarDao().actualizarDatosPrincipales(
            fichaId = id,
            cedula = "0912345675",
            nombre = "Familia modificada",
            telefono = "0999999999",
            numeroFicha = "FF-EDITADA",
            fecha = "16/07/2026",
            carpeta = "CARPETA-2",
            responsableNombre = "Profesional",
            responsableCodigo = "1016.2025-3143718",
            usuarioId = null,
            actualizadoEn = 1_600L
        )

        val editada = requireNotNull(database.fichaFamiliarDao().buscarPorId(id))
        assertEquals("Familia modificada", editada.nombreApellidoJefeFamilia)
        assertEquals("FF-EDITADA", editada.numeroFichaFamiliar)
        assertEquals("16/07/2026", editada.fechaLlenado)
        assertEquals("PENDIENTE", editada.syncEstado)
    }

    @Test
    fun unaVersionAnteriorNoPuedeMarcarComoSincronizadosCambiosNuevos() = runBlocking {
        val id = database.fichaFamiliarDao().guardarFicha(
            fichaBase(syncEstado = "PENDIENTE", actualizadoEn = 2_000L)
        )
        database.fichaContenidoDao().guardarMiembro(
            miembroBase(fichaId = id, nombre = "Cambio durante sincronización")
        )

        val resultado = database.sincronizacionDao().marcarSincronizada(
            id = id,
            version = 1,
            actualizadoEnEsperado = 2_000L
        )

        assertEquals(0, resultado)
        assertEquals(
            "PENDIENTE",
            requireNotNull(database.fichaFamiliarDao().buscarPorId(id)).syncEstado
        )
    }

    @Test
    fun unaDescargaRemotaNoSeConvierteEnEdicionLocal() = runBlocking {
        val id = database.fichaFamiliarDao().guardarFicha(
            fichaBase(syncEstado = "DESCARGANDO", actualizadoEn = 3_000L)
        )
        database.fichaContenidoDao().guardarMiembro(
            miembroBase(fichaId = id, nombre = "Dato remoto")
        )

        val duranteDescarga = requireNotNull(database.fichaFamiliarDao().buscarPorId(id))
        assertEquals("DESCARGANDO", duranteDescarga.syncEstado)
        assertEquals(3_000L, duranteDescarga.actualizadoEn)

        assertEquals(
            1,
            database.sincronizacionDao().marcarSincronizada(id, 5, 3_000L)
        )
        assertEquals(
            "SINCRONIZADO",
            requireNotNull(database.fichaFamiliarDao().buscarPorId(id)).syncEstado
        )
    }

    @Test
    fun numeracionEsIndependientePorBarrio() = runBlocking {
        val dao = database.fichaFamiliarDao()
        assertEquals(1, dao.siguienteNumeroFichaBarrio("barrio-a"))
        dao.guardarFicha(fichaBase("PENDIENTE", 1L).copy(territorioId = "barrio-a", barrio = "Norte", numeroFichaFamiliar = "1"))
        dao.guardarFicha(fichaBase("PENDIENTE", 2L).copy(territorioId = "barrio-b", barrio = "Sur", numeroFichaFamiliar = "1"))
        dao.guardarFicha(fichaBase("PENDIENTE", 3L).copy(territorioId = "barrio-a", barrio = "Norte", numeroFichaFamiliar = "2"))
        assertEquals(3, dao.siguienteNumeroFichaBarrio("barrio-a"))
        assertEquals(2, dao.siguienteNumeroFichaBarrio("barrio-b"))
        assertTrue(dao.existeNumeroFichaEnBarrio("barrio-a", "1"))
        assertTrue(dao.existeNumeroFichaEnBarrio("barrio-b", "1"))
    }

    @Test
    fun desactivarEaisConservaFichaYReferenciaHistorica() = runBlocking {
        val salaDao = database.salaDao()
        salaDao.guardarEais(EaisSalaEntity(id = "eais-1", salaId = "sala-1", numero = 1))
        salaDao.guardarTerritorio(TerritorioSalaEntity(id = "barrio-1", salaId = "sala-1", eaisId = "eais-1", tipo = "BARRIO", nombre = "Norte"))
        val fichaId = database.fichaFamiliarDao().guardarFicha(
            fichaBase("SINCRONIZADO", 4L).copy(eaisId = "eais-1", territorioId = "barrio-1", barrio = "Norte", numeroFichaFamiliar = "1")
        )
        salaDao.desactivarEais("sala-1")
        salaDao.desactivarTerritoriosDeEais("eais-1")
        assertEquals("eais-1", database.fichaFamiliarDao().buscarPorId(fichaId)?.eaisId)
        assertEquals("Norte", database.fichaFamiliarDao().buscarPorId(fichaId)?.barrio)
        assertEquals(false, salaDao.buscarEais("eais-1")?.activo)
        assertEquals("Norte", salaDao.buscarTerritorio("barrio-1")?.nombre)
    }

    private fun fichaBase(syncEstado: String, actualizadoEn: Long) = FichaFamiliarEntity(
        cedulaJefeHogar = "1710034065",
        institucionSistema = "CLINICA DE PRUEBA",
        unidadOperativa = "Centro de salud",
        codigoUo = "UO-1",
        areaNumero = "1",
        codigoLocalizacion = "010101",
        parroquiaCodigoLocalizacion = "01",
        cantonCodigoLocalizacion = "01",
        provinciaCodigoLocalizacion = "01",
        numeroFichaFamiliar = "FF-PRUEBA-${actualizadoEn}",
        provincia = "Pichincha",
        canton = "Quito",
        parroquia = "Quito",
        sector = "Centro",
        manzana = "",
        numeroFamilia = "",
        direccionHabitualFamilia = "",
        barrio = "",
        numeroCasa = "",
        comunidad = "",
        grupoCultural = "",
        nombreApellidoJefeFamilia = "Familia de prueba",
        numeroTelefono = "",
        fechaLlenado = "17/07/2026",
        numeroCarpeta = "",
        creadoEn = actualizadoEn,
        actualizadoEn = actualizadoEn,
        syncEstado = syncEstado,
        organizacionId = "organizacion-prueba"
    )

    private fun miembroBase(fichaId: Long, nombre: String) = MiembroFamiliaEntity(
        fichaId = fichaId,
        grupoEdad = "20-64",
        apellidosNombres = nombre,
        parentesco = "JEFE/A DE FAMILIA",
        fechaNacimiento = "01/01/1990",
        ocupacion = "Médico",
        sexo = "M",
        escolaridad = "SUPERIOR",
        numeroHistoriaClinica = "1710034065",
        cedula = "1710034065"
    )
}
