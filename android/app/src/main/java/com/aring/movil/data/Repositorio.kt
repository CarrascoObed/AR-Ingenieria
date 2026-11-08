package com.aring.movil.data

import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

object Repositorio {

    val tecnicos = mutableStateListOf<Tecnico>()
    val tareas = mutableStateListOf<Tarea>()

    suspend fun login(username: String, password: String): Tecnico {
        val json = ApiService.login(username.trim(), password)
        if (json.optString("rol") != "ROLE_TECNICO") {
            throw ApiException("Esta aplicación móvil es exclusiva para técnicos.", 403)
        }
        ApiService.token = json.getString("token")
        val tecnicoId = json.optInt("tecnicoId", -1)
        val lista = runCatching { ApiService.tecnicos().let(::tecnicosDesdeJson) }.getOrDefault(emptyList())
        tecnicos.clear()
        tecnicos.addAll(lista)
        val base = lista.firstOrNull { it.id == tecnicoId } ?: Tecnico(
            id = tecnicoId,
            nombre = json.optString("nombre"),
            apellido = json.optString("apellido"),
            rut = json.optString("username"),
            password = password,
            contactoNombre = "",
            contactoApellido = "",
            contactoTelefono = "",
        )
        return base.copy(password = password)
    }

    /**
     * Aplica a la vista local los efectos de envíos pendientes del outbox:
     * - Si hay marcador de finalización, la tarea se muestra Finalizada y pendiente de sincronizar.
     * - El contador suma los informes aún no confirmados por el servidor, para que no
     *   aparezca en cero después de guardar tableros en el dispositivo.
     */
    private fun conEstadoLocal(t: Tarea): Tarea {
        val pendientes = runCatching { OfflineSync.informes(t.id).size }.getOrDefault(0)
        return when {
            OfflineSync.pendiente(t.id) || t.sincronizacionPendiente ->
                t.copy(estado = "Finalizada", sincronizacionPendiente = true, tablerosRealizados = t.tablerosRealizados + pendientes)
            runCatching { OfflineSync.pendienteInicio(t.id) }.getOrDefault(false) ->
                t.copy(estado = "En proceso", sincronizacionPendiente = true, tablerosRealizados = t.tablerosRealizados + pendientes)
            pendientes > 0 -> t.copy(tablerosRealizados = t.tablerosRealizados + pendientes)
            else -> t
        }
    }

    suspend fun cargarTecnico(tecnicoId: Int) {
        val ts = try {
            ApiService.tareasMias(tecnicoId).also { OfflineTaskCache.save(tecnicoId, tareasDesdeJson(it)) }.let(::tareasDesdeJson)
        } catch (e: Exception) {
            OfflineTaskCache.load(tecnicoId)?.let(::tareasDesdeJson) ?: throw e
        }.map { conEstadoLocal(it) }
        val tecs = runCatching { ApiService.tecnicos().let(::tecnicosDesdeJson) }.getOrDefault(emptyList())
        tareas.clear()
        tareas.addAll(ts)
        tecnicos.clear()
        tecnicos.addAll(tecs)
    }

    suspend fun cambiarEstadoTarea(id: Int, nuevoEstado: String) {
        reemplazarTarea(tareaDesdeJson(ApiService.cambiarEstadoTarea(id, estadoApiDesdeEtiqueta(nuevoEstado))))
    }

    fun tareasDeTecnico(tecnicoId: Int): List<Tarea> =
        tareas
            .filter { it.esDeTecnico(tecnicoId) && (it.estado != "Finalizada" || it.sincronizacionPendiente) }
            .sortedBy { it.id }

    /** Tareas finalizadas confirmadas en el servidor; histórico de solo lectura del técnico. */
    fun finalizadasDeTecnico(tecnicoId: Int): List<Tarea> =
        tareas
            .filter { it.esDeTecnico(tecnicoId) && it.estado == "Finalizada" && !it.sincronizacionPendiente }
            .sortedByDescending { it.id }

    fun registrarTableroEnDispositivo(tareaId: Int) {
        val index = tareas.indexOfFirst { it.id == tareaId }
        if (index >= 0) reemplazarTarea(tareas[index].copy(tablerosRealizados = tareas[index].tablerosRealizados + 1))
    }

    suspend fun iniciarTarea(id: Int, ayudantesIds: List<Int>) {
        reemplazarTarea(tareaDesdeJson(ApiService.iniciarTarea(id, ayudantesIds)))
    }

    suspend fun actualizarAyudantes(id: Int, ayudantesIds: List<Int>): Tarea {
        val tarea = tareaDesdeJson(ApiService.actualizarAyudantes(id, ayudantesIds))
        reemplazarTarea(tarea)
        return tarea
    }

    fun formatearFecha(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val zoned = OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
            SimpleDateFormat("dd MMM, HH:mm", Locale("es", "CL"))
                .format(Date.from(zoned.toInstant()))
        } catch (_: Exception) {
            try {
                val d = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso)
                SimpleDateFormat("dd MMM, HH:mm", Locale("es", "CL")).format(d)
            } catch (_: Exception) {
                iso
            }
        }
    }

    private fun reemplazarTarea(tarea: Tarea) {
        val i = tareas.indexOfFirst { it.id == tarea.id }
        if (i >= 0) tareas[i] = tarea else tareas.add(0, tarea)
        val tecnicoId = Prefs.leerTecnico()?.id
        if (tecnicoId != null) OfflineTaskCache.save(tecnicoId, tareas.toList())
    }

    private fun estadoApiDesdeEtiqueta(etiqueta: String): String = when (etiqueta) {
        "En proceso" -> "EN_PROCESO"
        "Finalizada" -> "FINALIZADA"
        else -> "NO_INICIADA"
    }

    private fun tipoDesdeEtiqueta(etiqueta: String): String = when (etiqueta.lowercase()) {
        "mantenimiento" -> "MANTENIMIENTO"
        "reparación", "reparacion" -> "REPARACION"
        "cotización", "cotizacion" -> "COTIZACION"
        else -> etiqueta.uppercase(Locale.ROOT)
    }

    private fun tareaDesdeJson(o: JSONObject): Tarea = Tarea(
        id = o.getInt("id"),
        codigo = o.optString("codigo"),
        tipo = tipoDesdeEtiqueta(o.optString("tipo")),
        estado = o.optString("estado"),
        nombreRecinto = o.optString("nombreRecinto"),
        direccion = o.optString("direccion"),
        descripcion = o.optString("descripcion"),
        tecnicoCabeceraId = o.optJSONObject("tecnicoCabecera")?.optInt("id") ?: o.optInt("tecnicoCabeceraId"),
        tecnicoAyudantesIds = (o.optJSONArray("tecnicoAyudantesIds") ?: JSONArray()).let { arr ->
            (0 until arr.length()).map { arr.optInt(it) }
        },
        fechaInicio = o.optString("fechaInicio").ifEmpty { null },
        tablerosRealizados = o.optInt("tablerosRealizados"),
    )

    private fun tecnicoDesdeJson(o: JSONObject): Tecnico = Tecnico(
        id = o.getInt("id"),
        nombre = o.optString("nombre"),
        apellido = o.optString("apellido"),
        rut = o.optString("rut"),
        password = "",
        contactoNombre = o.optString("contactoNombre"),
        contactoApellido = o.optString("contactoApellido"),
        contactoTelefono = o.optString("contactoTelefono"),
    )

    private fun tareasDesdeJson(arr: JSONArray): List<Tarea> =
        (0 until arr.length()).map { tareaDesdeJson(arr.getJSONObject(it)) }

    private fun tecnicosDesdeJson(arr: JSONArray): List<Tecnico> =
        (0 until arr.length()).map { tecnicoDesdeJson(arr.getJSONObject(it)) }

}
