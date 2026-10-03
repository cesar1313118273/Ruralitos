package com.ruralitos.app.domain.familiograma

import org.json.JSONArray
import org.json.JSONObject

/**
 * Convierte el familiograma a texto JSON y de vuelta. Ese texto se guarda dentro del mismo PNG
 * que se inserta en el Excel y el PDF, así el dibujo se puede volver a editar sin más archivos.
 */
object SerializadorFamiliograma {
    private const val VERSION = 1

    fun aJson(doc: Familiograma): String {
        val o = JSONObject()
        o.put("version", VERSION)
        o.put("mostrarNombres", doc.mostrarNombres)
        o.put("personas", JSONArray().also { a ->
            doc.personas.forEach { p ->
                a.put(
                    JSONObject()
                        .put("id", p.id).put("sexo", p.sexo.clave).put("x", p.x.toDouble()).put("y", p.y.toDouble())
                        .put("edad", p.edad).put("patologias", JSONArray(p.patologias))
                        .put("fallecido", p.fallecido).put("informante", p.informante).put("enHogar", p.enHogar)
                        .put("nombre", p.nombre).put("parentesco", p.parentesco)
                )
            }
        })
        o.put("uniones", JSONArray().also { a ->
            doc.uniones.forEach { u ->
                a.put(JSONObject().put("id", u.id).put("a", ancla(u.a)).put("b", ancla(u.b)).put("tipo", u.tipo.name))
            }
        })
        o.put("filiaciones", JSONArray().also { a ->
            doc.filiaciones.forEach { f ->
                val j = JSONObject().put("id", f.id).put("hijo", ancla(f.hijo)).put("adoptado", f.adoptado)
                f.unionId?.let { j.put("unionId", it) }
                f.progenitor?.let { j.put("progenitor", ancla(it)) }
                a.put(j)
            }
        })
        o.put("abortos", JSONArray().also { a ->
            doc.abortos.forEach {
                a.put(JSONObject().put("id", it.id).put("unionId", it.unionId).put("x", it.x.toDouble()).put("y", it.y.toDouble()))
            }
        })
        o.put("entornos", JSONArray().also { a ->
            doc.entornos.forEach {
                a.put(
                    JSONObject().put("id", it.id).put("tipo", it.tipo.name)
                        .put("x", it.x.toDouble()).put("y", it.y.toDouble()).put("etiqueta", it.etiqueta)
                )
            }
        })
        o.put("vinculos", JSONArray().also { a ->
            doc.vinculos.forEach { a.put(JSONObject().put("id", it.id).put("a", ancla(it.a)).put("b", ancla(it.b))) }
        })
        o.put("trazos", JSONArray().also { a ->
            doc.trazos.forEach { t ->
                a.put(
                    JSONObject().put("id", t.id).put("tipo", t.tipo.name).put("color", t.color)
                        .put("grosor", t.grosor.toDouble()).put("punteado", t.punteado)
                        .put("puntos", JSONArray().also { pa ->
                            t.puntos.forEach { pa.put(JSONArray().put(it.x.toDouble()).put(it.y.toDouble())) }
                        })
                )
            }
        })
        o.put("textos", JSONArray().also { a ->
            doc.textos.forEach {
                a.put(JSONObject().put("id", it.id).put("x", it.x.toDouble()).put("y", it.y.toDouble()).put("texto", it.texto))
            }
        })
        o.put("patologiasNuevas", JSONArray().also { a ->
            doc.patologiasNuevas.forEach { a.put(JSONObject().put("nombre", it.nombre).put("codigo", it.codigo)) }
        })
        return o.toString()
    }

    /** Devuelve `null` si el texto no es un familiograma válido. */
    fun desdeJson(texto: String): Familiograma? = runCatching {
        val o = JSONObject(texto)
        Familiograma(
            personas = lista(o, "personas") { j ->
                Persona(
                    id = j.getString("id"),
                    sexo = SexoPersona.desde(j.optString("sexo", "H")),
                    x = j.getDouble("x").toFloat(),
                    y = j.getDouble("y").toFloat(),
                    edad = j.optString("edad", ""),
                    patologias = j.optJSONArray("patologias")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty(),
                    fallecido = j.optBoolean("fallecido", false),
                    informante = j.optBoolean("informante", false),
                    enHogar = j.optBoolean("enHogar", true),
                    nombre = j.optString("nombre", ""),
                    parentesco = j.optString("parentesco", "")
                )
            },
            uniones = lista(o, "uniones") { j ->
                Union(
                    j.getString("id"), ancla(j.getJSONObject("a")), ancla(j.getJSONObject("b")),
                    enumOr(j.optString("tipo"), TipoUnion.MATRIMONIO)
                )
            },
            filiaciones = lista(o, "filiaciones") { j ->
                Filiacion(
                    id = j.getString("id"),
                    hijo = ancla(j.getJSONObject("hijo")),
                    unionId = if (j.has("unionId")) j.getString("unionId") else null,
                    progenitor = j.optJSONObject("progenitor")?.let(::ancla),
                    adoptado = j.optBoolean("adoptado", false)
                )
            },
            abortos = lista(o, "abortos") { j ->
                Aborto(j.getString("id"), j.getString("unionId"), j.getDouble("x").toFloat(), j.getDouble("y").toFloat())
            },
            entornos = lista(o, "entornos") { j ->
                val tipo = enumOr(j.optString("tipo"), TipoEntorno.IGLESIA)
                Entorno(
                    j.getString("id"), tipo, j.getDouble("x").toFloat(), j.getDouble("y").toFloat(),
                    j.optString("etiqueta", tipo.etiqueta)
                )
            },
            vinculos = lista(o, "vinculos") { j ->
                Vinculo(j.getString("id"), ancla(j.getJSONObject("a")), ancla(j.getJSONObject("b")))
            },
            trazos = lista(o, "trazos") { j ->
                Trazo(
                    id = j.getString("id"),
                    tipo = enumOr(j.optString("tipo"), TipoTrazo.LAPIZ),
                    puntos = j.getJSONArray("puntos").let { a ->
                        List(a.length()) { i -> a.getJSONArray(i).let { Punto(it.getDouble(0).toFloat(), it.getDouble(1).toFloat()) } }
                    },
                    color = j.optInt("color", COLOR_TINTA),
                    grosor = j.optDouble("grosor", 2.4).toFloat(),
                    punteado = j.optBoolean("punteado", false)
                )
            },
            textos = lista(o, "textos") { j ->
                Texto(j.getString("id"), j.getDouble("x").toFloat(), j.getDouble("y").toFloat(), j.getString("texto"))
            },
            patologiasNuevas = lista(o, "patologiasNuevas") { j ->
                PatologiaNueva(j.getString("nombre"), j.getString("codigo"))
            },
            mostrarNombres = o.optBoolean("mostrarNombres", false)
        )
    }.getOrNull()

    private fun ancla(a: Ancla) = JSONObject().put("id", a.elementoId).put("lado", a.lado.clave)
    private fun ancla(j: JSONObject) = Ancla(j.getString("id"), Lado.desde(j.optString("lado", "t")))

    private fun <T> lista(o: JSONObject, clave: String, leer: (JSONObject) -> T): List<T> {
        val a = o.optJSONArray(clave) ?: return emptyList()
        return List(a.length()) { leer(a.getJSONObject(it)) }
    }

    private inline fun <reified E : Enum<E>> enumOr(nombre: String, porDefecto: E): E =
        enumValues<E>().firstOrNull { it.name == nombre } ?: porDefecto
}
