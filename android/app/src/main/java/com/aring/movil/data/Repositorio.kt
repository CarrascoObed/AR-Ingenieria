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
    val notificaciones = mutableStateListOf<Notificacion>()

    suspend fun login(username: String, password: String): Sesion {
        val json = ApiService.login(username.trim(), password)
        ApiService.token = json.getString("token")
        val rol = json.optString("rol")
        return if (rol == "ROLE_ADMIN") {
            Sesion.AdminSesion(
                Admin(
                    token = json.getString("token"),
                    nombre = json.optString("nombre"),
                    apellido = json.optString("apellido"),
                    iniciales = json.optString("iniciales")
                        .ifBlank { inicialesDe(json.optString("nombre"), json.optString("apellido")) },
                    username = json.optString("username"),
                )
            )
        } else {
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
            Sesion.TecnicoSesion(base.copy(password = password))
        }
    }

    suspend fun cargarAdmin() {        val ts = ApiService.tareas().let(::tareasDesdeJson)
        val tecs = runCatching { ApiService.tecnicos().let(::tecnicosDesdeJson) }.getOrDefault(emptyList())
        val notifs = ApiService.notificaciones(null).let(::notificacionesDesdeJson)
        tareas.clear()
        tareas.addAll(ts)
        tecnicos.clear()
        tecnicos.addAll(tecs)
        notificaciones.clear()
        notificaciones.addAll(notifs)
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
        val notifs = ApiService.notificaciones(tecnicoId).let(::notificacionesDesdeJson)
        tareas.clear()
        tareas.addAll(ts)
        tecnicos.clear()
        tecnicos.addAll(tecs)
        notificaciones.clear()
        notificaciones.addAll(notifs)
    }

    suspend fun stats(): DashboardStats {
        val json = ApiService.dashboardStats()
        runCatching {
            val tecs = ApiService.tecnicos().let(::tecnicosDesdeJson)
            tecnicos.clear()
            tecnicos.addAll(tecs)
        }
        return DashboardStats(
            enProceso = json.optInt("enProceso"),
            finalizadas = json.optInt("finalizadas"),
            noIniciadas = json.optInt("noIniciadas"),
        )
    }

    suspend fun crearTecnico(
        nombre: String,
        apellido: String,
        rut: String,
        password: String,
        contactoNombre: String,
        contactoApellido: String,
        contactoTelefono: String,
    ) {
        val cuerpo = JSONObject()
            .put("nombre", nombre.trim())
            .put("apellido", apellido.trim())
            .put("rut", rut.trim())
            .put("password", password)
            .put("contactoNombre", contactoNombre.trim())
            .put("contactoApellido", contactoApellido.trim())
            .put("contactoTelefono", contactoTelefono.trim())
        val t = tecnicoDesdeJson(ApiService.crearTecnico(cuerpo))
        tecnicos.add(0, t)
    }

    suspend fun actualizarTecnico(
        id: Int,
        contactoNombre: String,
        contactoApellido: String,
        contactoTelefono: String,
        password: String,
    ) {
        val cuerpo = JSONObject()
            .put("password", password)
            .put("contactoNombre", contactoNombre.trim())
            .put("contactoApellido", contactoApellido.trim())
            .put("contactoTelefono", contactoTelefono.trim())
        val t = tecnicoDesdeJson(ApiService.actualizarTecnico(id, cuerpo))
        val i = tecnicos.indexOfFirst { it.id == id }
        if (i >= 0) tecnicos[i] = t else tecnicos.add(0, t)
    }

    suspend fun eliminarTecnico(id: Int) {
        ApiService.eliminarTecnico(id)
        tecnicos.removeAll { it.id == id }
    }

    suspend fun crearTarea(
        tipo: String,
        direccion: String,
        nombreRecinto: String,
        descripcion: String,
        tecnicoCabeceraId: Int,
    ) {
        val cuerpo = JSONObject()
            .put("tipo", tipo)
            .put("direccion", direccion.trim())
            .put("nombreRecinto", nombreRecinto.trim())
            .put("descripcion", descripcion.trim())
            .put("tecnicoCabeceraId", tecnicoCabeceraId)
        val tarea = tareaDesdeJson(ApiService.crearTarea(cuerpo))
        tareas.add(0, tarea)
    }

    suspend fun actualizarCabeceraTarea(id: Int, tecnicoId: Int) {
        reemplazarTarea(tareaDesdeJson(ApiService.actualizarCabeceraTarea(id, tecnicoId)))
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

    suspend fun eliminarTarea(id: Int) {
        ApiService.eliminarTarea(id)
        tareas.removeAll { it.id == id }
    }

    suspend fun marcarLeida(id: Int) {
        ApiService.marcarLeida(id)
        val i = notificaciones.indexOfFirst { it.id == id }
        if (i >= 0) notificaciones[i] = notificaciones[i].copy(leida = true)
    }

    suspend fun marcarTodasLeidas() {
        for (i in notificaciones.indices) {
            if (!notificaciones[i].leida) notificaciones[i] = notificaciones[i].copy(leida = true)
        }
        ApiService.marcarTodasLeidas(null)
    }

    suspend fun marcarTodasLeidasTecnico(tecnicoId: Int) {
        for (i in notificaciones.indices) {
            val n = notificaciones[i]
            if (!n.leida && n.tecnicoDestinoId == tecnicoId) {
                notificaciones[i] = n.copy(leida = true)
            }
        }
        ApiService.marcarTodasLeidas(tecnicoId)
    }

    suspend fun limpiarNotificaciones() {
        ApiService.eliminarNotificaciones()
        notificaciones.clear()
    }

    suspend fun limpiarNotificacionesTecnico(tecnicoId: Int) {
        ApiService.eliminarNotificacionesTecnico(tecnicoId)
        notificaciones.removeAll { it.tecnicoDestinoId == tecnicoId }
    }

    fun notificacionesDeTecnico(tecnicoId: Int): List<Notificacion> =
        notificaciones.filter { it.tecnicoDestinoId == tecnicoId }

    fun noLeidasTecnico(tecnicoId: Int): Int =
        notificaciones.count { !it.leida && it.tecnicoDestinoId == tecnicoId }

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

    private fun notificacionDesdeJson(o: JSONObject): Notificacion = Notificacion(
        id = o.getInt("id"),
        mensaje = o.optString("mensaje"),
        fecha = o.optString("fecha"),
        leida = o.optBoolean("leida"),
        tareaId = optIntNulo(o, "tareaId"),
        tecnicoDestinoId = optIntNulo(o, "tecnicoDestinoId"),
    )

    private fun optIntNulo(o: JSONObject, clave: String): Int? {
        if (!o.has(clave) || o.isNull(clave)) return null
        val v = o.optInt(clave, -1)
        return v.takeIf { it >= 0 }
    }

    private fun tareasDesdeJson(arr: JSONArray): List<Tarea> =
        (0 until arr.length()).map { tareaDesdeJson(arr.getJSONObject(it)) }

    private fun tecnicosDesdeJson(arr: JSONArray): List<Tecnico> =
        (0 until arr.length()).map { tecnicoDesdeJson(arr.getJSONObject(it)) }

    private fun notificacionesDesdeJson(arr: JSONArray): List<Notificacion> =
        (0 until arr.length()).map { notificacionDesdeJson(arr.getJSONObject(it)) }
}
