package com.ruralitos.app.data.sync

import com.ruralitos.app.data.remote.InfoFichaCompartida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanCompartidasTest {
    private fun recibida(id: String, org: String = "o1") =
        InfoFichaCompartida(id, true, "a", "Ana", "LECTOR", 0, organizacionId = org)

    private fun local(id: String, permiso: String = "", estado: String = "SINCRONIZADO") =
        FichaLocalCompartida(id, permiso, estado)

    @Test
    fun bajaUnaFichaAntiguaQueAcabanDeCompartir() {
        val plan = PlaneadorCompartidas.planear("o1", listOf(local("propia")), listOf(recibida("nueva")), emptySet())
        assertEquals(listOf("nueva"), plan.faltantes)
    }

    @Test
    fun noPideLoQueYaTieneNiLoDeOtraSalaNiLoQueElUsuarioEliminoAqui() {
        val plan = PlaneadorCompartidas.planear(
            "o1",
            listOf(local("ya", "LECTOR")),
            listOf(recibida("ya"), recibida("otraSala", "o2"), recibida("eliminada")),
            setOf("eliminada")
        )
        assertTrue(plan.faltantes.isEmpty())
    }

    @Test
    fun marcaComoCandidataASalirLaCompartidaQueElServidorYaNoMenciona() {
        val plan = PlaneadorCompartidas.planear(
            "o1",
            listOf(local("sigue", "LECTOR"), local("quitada", "EDITOR"), local("propia", ""), local("pendiente", "EDITOR", "PENDIENTE")),
            listOf(recibida("sigue")),
            emptySet()
        )
        // La propia no es compartida y la que tiene cambios sin subir nunca se retira.
        assertEquals(listOf("quitada"), plan.candidatasARetirar)
    }
}

class PlanTraspasosTest {
    @Test
    fun laFichaQueEntregueSeRetiraSiYaNoLaVeoPeroUnaPropiaNo() {
        val locales = listOf(
            FichaLocalCompartida("entregada", "", "SINCRONIZADO"),
            FichaLocalCompartida("mia", "", "SINCRONIZADO"),
            FichaLocalCompartida("entregadaConCambios", "", "PENDIENTE")
        )
        val plan = PlaneadorCompartidas.planear(
            "o1", locales, emptyList(), emptySet(),
            traspasadas = setOf("entregada", "entregadaConCambios")
        )
        assertEquals(listOf("entregada"), plan.candidatasARetirar)
    }
}
