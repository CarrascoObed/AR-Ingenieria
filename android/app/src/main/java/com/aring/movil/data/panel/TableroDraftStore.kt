package com.aring.movil.data.panel

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object TableroDraftStore {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("aring_tableros", Context.MODE_PRIVATE)
    }

    /**
     * Clave del borrador por técnico: draft_{tecnicoId}_{tareaId}_{tipo}.
     * Así el ayudante nunca retoma el borrador que dejó el técnico de cabecera
     * cuando ambas cuentas se usan en el mismo dispositivo; cada técnico abre
     * siempre su propio tablero dentro de la tarea.
     */
    private fun key(tareaId: Int, tipo: String, inspectionId: String = "legacy"): String {
        val tecnicoId = runCatching { com.aring.movil.data.Prefs.leerTecnico()?.id }.getOrNull() ?: 0
        if (inspectionId == "legacy") return "draft_${tecnicoId}_${tareaId}_$tipo"
        return "draft_${tecnicoId}_${tareaId}_${tipo}_$inspectionId"
    }

    data class Borrador(val id: String, val tipo: String, val descripcion: String, val ubicacion: String, val terminado: Boolean)

    fun listar(tareaId: Int): List<Borrador> {
        val tecnicoId = runCatching { com.aring.movil.data.Prefs.leerTecnico()?.id }.getOrNull() ?: 0
        val prefix = "draft_${tecnicoId}_${tareaId}_"
        return prefs.all.entries.mapNotNull { (k, raw) ->
            if (!k.startsWith(prefix) || raw !is String) return@mapNotNull null
            runCatching {
                val p = k.removePrefix(prefix).split('_', limit = 2)
                val json = JSONObject(raw)
                val tipo = p[0]
                Borrador(p.getOrElse(1) { "legacy" }, tipo, json.optString("descripcion"), json.optString("ubicacion"), json.optBoolean("finished"))
            }.getOrNull()
        }.sortedBy { it.id }
    }

    fun limpiar(tareaId: Int, tipo: String, inspectionId: String) { prefs.edit().remove(key(tareaId, tipo, inspectionId)).apply() }
    fun limpiarTarea(tareaId: Int, tecnicoId: Int = runCatching { com.aring.movil.data.Prefs.leerTecnico()?.id }.getOrNull() ?: 0) {
        val prefix = "draft_${tecnicoId}_${tareaId}_"
        prefs.edit().also { editor -> prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove) }.apply()
    }

    fun guardarTrifasico(tareaId: Int, st: TrifasicoState, inspectionId: String = st.inspectionId) {
        prefs.edit().putString(key(tareaId, TYPE_TRIFASICO, inspectionId), jsonTrifasico(st).put("finished", st.finished).toString()).apply()
    }

    fun cargarTrifasico(tareaId: Int, inspectionId: String = "legacy"): TrifasicoState? {
        val raw = prefs.getString(key(tareaId, TYPE_TRIFASICO, inspectionId), null) ?: return null
        return runCatching { trifasicoDesdeJson(JSONObject(raw)).copy(inspectionId = inspectionId, finished = JSONObject(raw).optBoolean("finished")) }.getOrNull()
    }

    fun guardarMonofasico(tareaId: Int, st: MonofasicoState, inspectionId: String = st.inspectionId) {
        prefs.edit().putString(key(tareaId, TYPE_MONOFASICO, inspectionId), jsonMonofasico(st).put("finished", st.finished).toString()).apply()
    }

    fun cargarMonofasico(tareaId: Int, inspectionId: String = "legacy"): MonofasicoState? {
        val raw = prefs.getString(key(tareaId, TYPE_MONOFASICO, inspectionId), null) ?: return null
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
