package com.ruralitos.app.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CifradoRespaldoTest {
    @Test
    fun `cifra y recupera contenido de varios bloques`() {
        val original = ByteArray(180_000) { (it % 251).toByte() }
        val destino = ByteArrayOutputStream()
        CifradoRespaldo.abrirSalida(destino, "Clave-segura-2026").use { it.write(original) }

        val recuperado = CifradoRespaldo.abrirEntrada(
            ByteArrayInputStream(destino.toByteArray()),
            "Clave-segura-2026"
        ).use { it.readBytes() }

        assertArrayEquals(original, recuperado)
    }

    @Test
    fun `rechaza una contraseña incorrecta`() {
        val destino = ByteArrayOutputStream()
        CifradoRespaldo.abrirSalida(destino, "Clave-correcta-2026").use { it.write("datos".toByteArray()) }

        assertThrows(Exception::class.java) {
            CifradoRespaldo.abrirEntrada(
                ByteArrayInputStream(destino.toByteArray()),
                "Clave-incorrecta"
            ).use { it.readBytes() }
        }
    }

    @Test
    fun `rechaza un respaldo manipulado`() {
        val destino = ByteArrayOutputStream()
        CifradoRespaldo.abrirSalida(destino, "Clave-segura-2026").use { it.write("información clínica".toByteArray()) }
        val alterado = destino.toByteArray().also { it[it.lastIndex - 8] = (it[it.lastIndex - 8].toInt() xor 1).toByte() }

        assertThrows(Exception::class.java) {
            CifradoRespaldo.abrirEntrada(
                ByteArrayInputStream(alterado),
                "Clave-segura-2026"
            ).use { it.readBytes() }
        }
    }

    @Test
    fun `rechaza un respaldo truncado aunque termine en un bloque valido`() {
        val destino = ByteArrayOutputStream()
        CifradoRespaldo.abrirSalida(destino, "Clave-segura-2026").use { it.write(ByteArray(70_000) { 7 }) }
        val completo = destino.toByteArray()
        val truncado = completo.copyOf(completo.size - 24)

        assertThrows(Exception::class.java) {
            CifradoRespaldo.abrirEntrada(
                ByteArrayInputStream(truncado),
                "Clave-segura-2026"
            ).use { it.readBytes() }
        }
    }
}
