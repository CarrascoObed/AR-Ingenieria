package com.aring.movil.data.panel

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

object TableroDraftStore {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("aring_tableros", Context.MODE_PRIVATE)
    }

    /** Descarta borradores y registros sin nombre, que no pueden ser tableros realizados válidos. */
    fun limpiarIncompletos(tareaId: Int? = null) {
        val prefijoTarea = tareaId?.let { "_${it}_" }
        val editor = prefs.edit()
        prefs.all.entries.forEach { (key, raw) ->
            if (!key.startsWith("draft_")) return@forEach
            if (prefijoTarea != null && !Regex("draft_\\d+${Regex.escape(prefijoTarea)}").containsMatchIn(key)) return@forEach
            val realizadoConNombre = raw is String && runCatching {
                JSONObject(raw).let { it.optBoolean("finished") && it.optString("descripcion").isNotBlank() }
            }.getOrDefault(false)
            if (!realizadoConNombre) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    /**
     * Clave del borrador por técnico: draft_{tecnicoId}_{tareaId}_{tipo}.
     * Así el ayudante nunca retoma el borrador que dejó el técnico de cabecera
     * cuando ambas cuentas se usan en el mismo dispositivo; cada técnico abre
     * siempre su propio tablero dentro de la tarea.
     */
    private fun key(tareaId: Int, tipo: String, inspectionId: String = "legacy", tecnicoIdOverride: Int? = null): String {
        val tecnicoId = tecnicoIdOverride ?: runCatching { com.aring.movil.data.Prefs.leerTecnico()?.id }.getOrNull() ?: 0
        if (inspectionId == "legacy") return "draft_${tecnicoId}_${tareaId}_$tipo"
        return "draft_${tecnicoId}_${tareaId}_${tipo}_$inspectionId"
    }

    data class Borrador(
        val id: String,
        val tipo: String,
        val descripcion: String,
        val ubicacion: String,
        val terminado: Boolean,
        val fechaFinalizacion: Long = 0L
    )

    fun listar(tareaId: Int, tecnicoIdOverride: Int? = null): List<Borrador> {
        limpiarIncompletos(tareaId)
        val tecnicoId = tecnicoIdOverride ?: runCatching { com.aring.movil.data.Prefs.leerTecnico()?.id }.getOrNull() ?: 0
        val prefix = "draft_${tecnicoId}_${tareaId}_"
        data class Guardado(val key: String, val borrador: Borrador, val firma: String)
        val guardados = prefs.all.entries.mapNotNull { (k, raw) ->
            if (!k.startsWith(prefix) || raw !is String) return@mapNotNull null
            runCatching {
                val p = k.removePrefix(prefix).split('_', limit = 2)
                val json = JSONObject(raw)
                val tipo = p[0]
                val firma = "$tipo|${JSONObject(json.toString()).apply { remove("finished"); remove("finishedAt") }}"
                Guardado(
                    k,
                    Borrador(p.getOrElse(1) { "legacy" }, tipo, json.optString("descripcion"), json.optString("ubicacion"), json.optBoolean("finished"), json.optLong("finishedAt")),
                    firma
                )
            }.getOrNull()
        }
        val canonicos = guardados.groupBy { it.firma }.values.map { grupo ->
            grupo.maxWithOrNull(compareBy<Guardado>({ it.borrador.terminado }, { it.borrador.id != "legacy" }))!!
        }
        val clavesCanonicas = canonicos.mapTo(mutableSetOf()) { it.key }
        val clavesDuplicadas = guardados.asSequence().map { it.key }.filterNot { it in clavesCanonicas }.toList()
        if (clavesDuplicadas.isNotEmpty()) {
            prefs.edit().also { editor -> clavesDuplicadas.forEach(editor::remove) }.apply()
        }
        // Los registros previos a la fecha de finalización reciben un orden estable de migración.
        val antiguos = canonicos.map { it.borrador }.filter { it.terminado && it.fechaFinalizacion <= 0 }
            .sortedWith(compareBy<Borrador>({ it.tipo }, { it.id }))
            .mapIndexed { index, borrador -> "${borrador.tipo}|${borrador.id}" to index + 1L }.toMap()
        return canonicos.map { it.borrador.copy(
            fechaFinalizacion = it.borrador.fechaFinalizacion.takeIf { fecha -> fecha > 0 }
                ?: antiguos["${it.borrador.tipo}|${it.borrador.id}"] ?: Long.MAX_VALUE
        ) }.sortedWith(compareBy<Borrador>({ if (it.terminado) 0 else 1 }, { it.fechaFinalizacion }, { it.id }))
    }

    fun hayDescripcionDuplicada(
        tareaId: Int,
        inspectionId: String,
        descripcion: String,
        nombresServidor: List<Pair<String, String?>>
    ): Boolean {
        val nombre = normalizarDescripcion(descripcion)
        if (nombre.isBlank()) return false
        val tecnicoId = runCatching { com.aring.movil.data.Prefs.leerTecnico()?.id }.getOrNull() ?: 0
        val prefix = "draft_${tecnicoId}_${tareaId}_"
        val repetidoLocal = prefs.all.entries.any { (key, raw) ->
            if (!key.startsWith(prefix) || raw !is String) return@any false
            val id = key.removePrefix(prefix).split('_', limit = 2).getOrElse(1) { "legacy" }
            if (id == inspectionId) return@any false
            runCatching { normalizarDescripcion(JSONObject(raw).optString("descripcion")) == nombre }
                .getOrDefault(false)
        }
        val repetidoServidor = nombresServidor.any { (nombreServidor, operationId) ->
            operationId != inspectionId && normalizarDescripcion(nombreServidor) == nombre
        }
        return repetidoLocal || repetidoServidor
    }

    private fun normalizarDescripcion(descripcion: String): String =
        Normalizer.normalize(descripcion.trim(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("\\s+".toRegex(), " ")

    fun limpiar(tareaId: Int, tipo: String, inspectionId: String) { prefs.edit().remove(key(tareaId, tipo, inspectionId)).apply() }
    fun limpiarTarea(tareaId: Int, tecnicoId: Int = runCatching { com.aring.movil.data.Prefs.leerTecnico()?.id }.getOrNull() ?: 0) {
        val prefix = "draft_${tecnicoId}_${tareaId}_"
        prefs.edit().also { editor -> prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove) }.apply()
    }

    fun guardarTrifasico(tareaId: Int, st: TrifasicoState, inspectionId: String = st.inspectionId) {
        val storageKey = key(tareaId, TYPE_TRIFASICO, inspectionId)
        val anterior = prefs.getString(storageKey, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val yaTerminado = anterior?.optBoolean("finished") == true
        val fechaFinalizacion = anterior?.optLong("finishedAt")?.takeIf { yaTerminado && it > 0 }
            ?: if (st.finished && !yaTerminado) System.currentTimeMillis() else null
        val json = jsonTrifasico(st).put("finished", st.finished || yaTerminado)
        if (fechaFinalizacion != null) json.put("finishedAt", fechaFinalizacion)
        prefs.edit().putString(storageKey, json.toString()).apply()
    }

    fun cargarTrifasico(tareaId: Int, inspectionId: String = "legacy", tecnicoIdOverride: Int? = null): TrifasicoState? {
        val raw = prefs.getString(key(tareaId, TYPE_TRIFASICO, inspectionId, tecnicoIdOverride), null) ?: return null
        return runCatching { trifasicoDesdeJson(JSONObject(raw)).copy(inspectionId = inspectionId, finished = JSONObject(raw).optBoolean("finished")) }.getOrNull()
    }

    fun guardarMonofasico(tareaId: Int, st: MonofasicoState, inspectionId: String = st.inspectionId) {
        val storageKey = key(tareaId, TYPE_MONOFASICO, inspectionId)
        val anterior = prefs.getString(storageKey, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val yaTerminado = anterior?.optBoolean("finished") == true
        val fechaFinalizacion = anterior?.optLong("finishedAt")?.takeIf { yaTerminado && it > 0 }
            ?: if (st.finished && !yaTerminado) System.currentTimeMillis() else null
        val json = jsonMonofasico(st).put("finished", st.finished || yaTerminado)
        if (fechaFinalizacion != null) json.put("finishedAt", fechaFinalizacion)
        prefs.edit().putString(storageKey, json.toString()).apply()
    }

    fun cargarMonofasico(tareaId: Int, inspectionId: String = "legacy", tecnicoIdOverride: Int? = null): MonofasicoState? {
        val raw = prefs.getString(key(tareaId, TYPE_MONOFASICO, inspectionId, tecnicoIdOverride), null) ?: return null
        return runCatching { monofasicoDesdeJson(JSONObject(raw)).copy(inspectionId = inspectionId, finished = JSONObject(raw).optBoolean("finished")) }.getOrNull()
    }

    private fun fotosJson(fotos: List<PhotoSlot>): JSONArray = JSONArray().apply {
        fotos.forEach { put(JSONObject().put("i", it.index).put("p", it.path ?: "")) }
    }

    private fun fotosDesdeJson(arr: JSONArray?, cantidad: Int): List<PhotoSlot> {
        val mapa = mutableMapOf<Int, String?>()
        arr?.let { a ->
            for (j in 0 until a.length()) {
                val o = a.getJSONObject(j)
                mapa[o.getInt("i")] = o.optString("p").ifEmpty { null }
            }
        }
        return (1..cantidad).map { PhotoSlot(it, mapa[it]) }
    }

    private fun medicionesJson(items: List<Triple<String, String, String?>>): JSONArray = JSONArray().apply {
        items.forEach { (id, v, p) -> put(JSONObject().put("id", id).put("v", v).put("p", p ?: "")) }
    }

    private data class ChecklistDraft(val id: String, val status: ChecklistStatus?, val photoPath: String?, val observation: String)

    private fun checklistJson(items: List<ChecklistDraft>): JSONArray = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().put("id", item.id).put("s", item.status?.name).put("p", item.photoPath ?: "").put("o", item.observation))
        }
    }

    private fun observacionesJson(obs: List<String>): JSONArray = JSONArray().apply {
        obs.forEach { put(it) }
    }

    private fun observacionesDesdeJson(o: JSONObject): List<String> {
        val arr = o.optJSONArray("observations") ?: return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }
    }

    private fun jsonTrifasico(st: TrifasicoState): JSONObject {
        val o = JSONObject()
        o.put("descripcion", st.registration.descripcion)
        o.put("ubicacion", st.registration.ubicacion)
        o.put("nivel", st.registration.nivel)
        o.put("tamanioTablero", st.registration.tamanioTablero)
        o.put("observations", observacionesJson(st.observations))
        o.put("reaprieteFoto", st.reaprietePhoto.orEmpty())
        o.put("aseoFoto", st.aseoPhoto.orEmpty())
        o.put("previousPhotos", fotosJson(st.previousPhotos))
        o.put("mediciones", medicionesJson(st.measurements.map { Triple(it.variableId, it.value, it.photoPath) }))
        o.put("checklist", checklistJson(st.checklist.map { ChecklistDraft(it.itemId, it.status, it.photoPath, it.observation) }))
        o.put("postPhotos", fotosJson(st.postPhotos))
        return o
    }

    private fun jsonMonofasico(st: MonofasicoState): JSONObject {
        val o = JSONObject()
        o.put("descripcion", st.registration.descripcion)
        o.put("ubicacion", st.registration.ubicacion)
        o.put("nivel", st.registration.nivel)
        o.put("observations", observacionesJson(st.observations))
        o.put("reaprieteFoto", st.reaprietePhoto.orEmpty())
        o.put("aseoFoto", st.aseoPhoto.orEmpty())
        o.put("previousPhotos", fotosJson(st.previousPhotos))
        o.put("mediciones", medicionesJson(st.measurements.map { Triple(it.variableId, it.value, it.photoPath) }))
        o.put("checklist", checklistJson(st.checklist.map { ChecklistDraft(it.itemId, it.status, it.photoPath, it.observation) }))
        o.put("postPhotos", fotosJson(st.postPhotos))
        return o
    }

    private fun medicionesDesdeJson(arr: JSONArray?, base: Map<String, Pair<String, String?>>): Map<String, Pair<String, String?>> {
        val mapa = base.toMutableMap()
        arr?.let { a ->
            for (j in 0 until a.length()) {
                val e = a.getJSONObject(j)
                mapa[e.getString("id")] = e.optString("v") to e.optString("p").ifEmpty { null }
            }
        }
        return mapa
    }

    private fun checklistDesdeJson(arr: JSONArray?, base: Map<String, Triple<ChecklistStatus?, String?, String>>): Map<String, Triple<ChecklistStatus?, String?, String>> {
        val mapa = base.toMutableMap()
        arr?.let { a ->
            for (j in 0 until a.length()) {
                val e = a.getJSONObject(j)
                mapa[e.getString("id")] = Triple(
                    runCatching { ChecklistStatus.valueOf(e.optString("s")) }.getOrNull(),
                    e.optString("p").ifEmpty { null },
                    e.optString("o")
                )
            }
        }
        return mapa
    }

    private fun trifasicoDesdeJson(o: JSONObject): TrifasicoState {
        val base = TrifasicoState()
        val mediciones = medicionesDesdeJson(o.optJSONArray("mediciones"), emptyMap())
        val checklist = checklistDesdeJson(o.optJSONArray("checklist"), emptyMap())
        return base.copy(
            registration = base.registration.copy(
                descripcion = o.optString("descripcion"),
                ubicacion = o.optString("ubicacion"),
                nivel = o.optString("nivel"),
                tamanioTablero = o.optString("tamanioTablero"),
            ),
            previousPhotos = fotosDesdeJson(o.optJSONArray("previousPhotos"), 2),
            measurements = base.measurements.map { m ->
                val d = mediciones[m.variableId]
                m.copy(value = d?.first ?: "", photoPath = d?.second)
            },
            reaprietePhoto = o.optString("reaprieteFoto").ifEmpty { null },
            aseoPhoto = o.optString("aseoFoto").ifEmpty { null },
            checklist = base.checklist.map { c ->
                val d = checklist[c.itemId]
                c.copy(status = d?.first, photoPath = d?.second, observation = d?.third.orEmpty())
            },
            postPhotos = fotosDesdeJson(o.optJSONArray("postPhotos"), 2),
            observations = observacionesDesdeJson(o),
        )
    }

    private fun monofasicoDesdeJson(o: JSONObject): MonofasicoState {
        val base = MonofasicoState()
        val mediciones = medicionesDesdeJson(o.optJSONArray("mediciones"), emptyMap())
        val checklist = checklistDesdeJson(o.optJSONArray("checklist"), emptyMap())
        return base.copy(
            registration = base.registration.copy(
                descripcion = o.optString("descripcion"),
                ubicacion = o.optString("ubicacion"),
                nivel = o.optString("nivel"),
            ),
            previousPhotos = fotosDesdeJson(o.optJSONArray("previousPhotos"), 2),
            measurements = base.measurements.map { m ->
                val d = mediciones[m.variableId]
                m.copy(value = d?.first ?: "", photoPath = d?.second)
            },
            reaprietePhoto = o.optString("reaprieteFoto").ifEmpty { null },
            aseoPhoto = o.optString("aseoFoto").ifEmpty { null },
            checklist = base.checklist.map { c ->
                val d = checklist[c.itemId]
                c.copy(status = d?.first, photoPath = d?.second, observation = d?.third.orEmpty())
            },
            postPhotos = fotosDesdeJson(o.optJSONArray("postPhotos"), 2),
            observations = observacionesDesdeJson(o),
        )
    }
}
