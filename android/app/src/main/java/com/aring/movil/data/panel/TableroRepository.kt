package com.aring.movil.data.panel

import com.aring.movil.data.ApiService
import org.json.JSONArray
import org.json.JSONObject

object TableroRepository {

    fun buildTrifasico(tareaId: Int, tecnicoId: Int, st: TrifasicoState): JSONObject {
        val root = base(tareaId, tecnicoId, TYPE_TRIFASICO, st.registration.descripcion, st.registration.ubicacion, st.registration.nivel)
        root.put("tamanioTablero", st.registration.tamanioTablero)
        root.put("reapriete", st.reapriete?.name)
        root.put("aseo", st.aseo?.name)
        root.put("mediciones", medicionesTrifasicas(st.measurements))
        root.put("checklist", checklistTrifasico(st.checklist))
        root.put("imagenes", imagenes(st.previousPhotos, st.postPhotos))
        root.put("observaciones", strings(st.observations))
        return root
    }

    fun buildMonofasico(tareaId: Int, tecnicoId: Int, st: MonofasicoState): JSONObject {
        val root = base(tareaId, tecnicoId, TYPE_MONOFASICO, st.registration.descripcion, st.registration.ubicacion, st.registration.nivel)
        root.put("reapriete", st.reapriete?.name)
        root.put("aseo", st.aseo?.name)
        root.put("mediciones", medicionesMonofasicas(st.measurements))
        root.put("checklist", checklistMonofasico(st.checklist))
        root.put("imagenes", imagenes(st.previousPhotos, st.postPhotos))
        root.put("observaciones", strings(st.observations))
        return root
    }

    private fun base(
        tareaId: Int,
        tecnicoId: Int,
        tipo: String,
        descripcion: String,
        ubicacion: String,
        nivel: String
    ): JSONObject {
        val root = JSONObject()
        root.put("tareaId", tareaId)
        root.put("tipoTablero", tipo)
        root.put("descripcion", descripcion)
        root.put("ubicacion", ubicacion)
        root.put("nivel", nivel)
        root.put("tecnicoId", tecnicoId)
        return root
    }

    private fun medicionesTrifasicas(items: List<TrifasicoMeasurement>): JSONArray {
        val arr = JSONArray()
        items.forEach { m ->
            val o = JSONObject()
            o.put("variableId", m.variableId)
            o.put("label", m.label)
            o.put("unit", m.unit)
            o.put("valor", m.value)
            ApiService.fotoBase64(m.photoPath)?.let { o.put("fotoBase64", it) }
            arr.put(o)
        }
        return arr
    }

    private fun medicionesMonofasicas(items: List<MonofasicoMeasurement>): JSONArray {
        val arr = JSONArray()
        items.forEach { m ->
            val o = JSONObject()
            o.put("variableId", m.variableId)
            o.put("label", m.label)
            o.put("unit", m.unit)
            o.put("valor", m.value)
            ApiService.fotoBase64(m.photoPath)?.let { o.put("fotoBase64", it) }
            arr.put(o)
        }
        return arr
    }

    private fun checklistTrifasico(items: List<TrifasicoChecklistItem>): JSONArray {
        val arr = JSONArray()
        items.forEach { c ->
            val o = JSONObject()
            o.put("itemId", c.itemId)
            o.put("label", c.label)
            c.status?.let { o.put("status", it.name) }
            ApiService.fotoBase64(c.photoPath)?.let { o.put("fotoBase64", it) }
            arr.put(o)
        }
        return arr
    }

    private fun checklistMonofasico(items: List<MonofasicoChecklistItem>): JSONArray {
        val arr = JSONArray()
        items.forEach { c ->
            val o = JSONObject()
            o.put("itemId", c.itemId)
            o.put("label", c.label)
            c.status?.let { o.put("status", it.name) }
            ApiService.fotoBase64(c.photoPath)?.let { o.put("fotoBase64", it) }
            arr.put(o)
        }
        return arr
    }

    private fun imagenes(previas: List<PhotoSlot>, posteriores: List<PhotoSlot>): JSONArray {
        val arr = JSONArray()
        previas.forEach { slot ->
            ApiService.fotoBase64(slot.path)?.let { base64 ->
                arr.put(JSONObject()
                    .put("categoria", "PREVIA")
                    .put("indice", slot.index)
                    .put("base64", base64))
            }
        }
        posteriores.forEach { slot ->
            ApiService.fotoBase64(slot.path)?.let { base64 ->
                arr.put(JSONObject()
                    .put("categoria", "POSTERIOR")
                    .put("indice", slot.index)
                    .put("base64", base64))
            }
        }
        return arr
    }

    private fun strings(items: List<String>): JSONArray = JSONArray().apply {
        items.forEach { put(it) }
    }
}
