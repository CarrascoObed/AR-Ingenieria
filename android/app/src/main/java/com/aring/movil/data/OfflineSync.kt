package com.aring.movil.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Durable on-device outbox. Reports stay here until the server confirms receipt. */
object OfflineSync {
    private lateinit var root: File
    private lateinit var authPrefs: android.content.SharedPreferences
    private lateinit var statusPrefs: android.content.SharedPreferences
    fun init(context: Context) {
        root = File(context.filesDir, "offline-outbox").apply { mkdirs() }
        authPrefs = context.getSharedPreferences("aring_outbox_auth", Context.MODE_PRIVATE)
        statusPrefs = context.getSharedPreferences("aring_outbox_status", Context.MODE_PRIVATE)
    }

    @Synchronized fun guardarInforme(
        tareaId: Int,
        tecnico: Tecnico,
        payload: JSONObject,
        deferUntilFinalization: Boolean = false,
        programarSincronizacion: Boolean = true
    ) {
        guardarCredenciales(tecnico)
        val id = payload.optString("clientOperationId").ifBlank { UUID.randomUUID().toString() }
        payload.put("clientOperationId", id)
        File(root, "rejected-$id.json").delete()
        File(root, "rejected-$id.error.json").delete()
        atomicWrite(File(root, "report-$id.json"), JSONObject()
            .put("tareaId", tareaId).put("tecnicoId", tecnico.id)
            .put("deferUntilFinalization", deferUntilFinalization).put("payload", payload).toString())
        if (programarSincronizacion) programar(root.context())
    }

    @Synchronized fun guardarInicio(tareaId: Int, tecnico: Tecnico, ayudantesIds: List<Int>) {
        guardarCredenciales(tecnico)
        atomicWrite(File(root, "start-$tareaId.json"), JSONObject()
            .put("tareaId", tareaId).put("tecnicoId", tecnico.id)
            .put("ayudantesIds", JSONArray(ayudantesIds)).toString())
        programar(root.context())
    }

    @Synchronized fun finalizar(tareaId: Int, tecnico: Tecnico) {
        guardarCredenciales(tecnico)
        atomicWrite(File(root, "finish-$tareaId.json"), JSONObject()
            .put("tareaId", tareaId).put("tecnicoId", tecnico.id).toString())
        programar(root.context())
    }

    /** Encola el cierre y deja que el Worker prepare los informes de forma secuencial. */
    @Synchronized fun finalizarDesdeTableros(tareaId: Int, tecnico: Tecnico, cantidadTableros: Int) {
        guardarCredenciales(tecnico)
        if (!File(root, "finish-$tareaId.json").exists()) {
            val manifest = JSONObject().put("tareaId", tareaId).put("tecnicoId", tecnico.id)
                .put("paqueteId", UUID.randomUUID().toString())
                .put("operacionesPreparadas", JSONArray())
            atomicWrite(File(root, "prepare-$tareaId.json"), manifest.toString())
            atomicWrite(File(root, "finish-$tareaId.json"), JSONObject()
                .put("tareaId", tareaId).put("tecnicoId", tecnico.id)
                .put("paqueteId", manifest.getString("paqueteId"))
                .put("prepararTableros", true).put("cantidadTableros", cantidadTableros)
                .put("fase", "PREPARACION").put("actual", 0).put("total", cantidadTableros).toString())
        }
        programar(root.context(), requiereInternet = false)
    }

    /** Persiste el avance del paquete para recuperarlo aunque la app se cierre. */
    @Synchronized fun guardarProgresoCierre(tareaId: Int, fase: String, actual: Int, total: Int) {
        val file = File(root, "finish-$tareaId.json")
        if (!file.exists()) return
        val state = JSONObject(file.readText())
            .put("fase", fase)
            .put("actual", actual.coerceAtLeast(0))
            .put("total", total.coerceAtLeast(0))
        atomicWrite(file, state.toString())
    }

    fun progresoPersistido(tareaId: Int): Triple<String, Int, Int>? = runCatching {
        val file = File(root, "finish-$tareaId.json")
        if (!file.exists()) return null
        val state = JSONObject(file.readText())
        Triple(state.optString("fase", "PREPARACION"), state.optInt("actual"), state.optInt("total"))
    }.getOrNull()

    fun errorCierre(tareaId: Int): String? {
        val sidecars = root.listFiles { f -> f.name.startsWith("rejected-") && f.name.endsWith(".error.json") }.orEmpty()
        if (sidecars.isNotEmpty()) {
            val messages = sidecars.mapNotNull { file -> runCatching {
                val error = JSONObject(file.readText())
                if (error.optInt("tareaId") != tareaId) return@runCatching null
                "${error.optString("nombre", "Tablero")}: ${error.optString("mensaje", "El servidor rechazó este informe.")}"
            }.getOrNull() }
            return messages.distinct().take(3).joinToString("\n").ifBlank { null }
        }
        val rejected = root.listFiles { f -> f.name.startsWith("rejected-") }.orEmpty()
        return rejected.mapNotNull { file ->
            runCatching {
                val report = JSONObject(file.readText())
                if (report.optInt("tareaId") != tareaId) return@runCatching null
                val name = report.optJSONObject("payload")?.optString("descripcion").orEmpty()
                    .ifBlank { "Tablero" }
                val reason = report.optString("errorServidor").ifBlank { "El servidor rechazó este informe." }
                "$name: $reason"
            }.getOrNull()
        }.distinct().take(3).joinToString("\n").ifBlank { null }
    }

    @Synchronized internal fun archivarInformeRechazado(file: File, mensaje: String, codigo: Int?) {
        val report = JSONObject(file.readText())
        val taskId = report.optInt("tareaId")
        val name = report.optJSONObject("payload")?.optString("descripcion").orEmpty().ifBlank { "Tablero" }
        val rejectedName = "rejected-" + file.name.removePrefix("report-")
        val rejectedFile = File(root, rejectedName)
        val errorFile = File(root, rejectedName.removeSuffix(".json") + ".error.json")
        atomicWrite(errorFile, JSONObject()
            .put("tareaId", taskId).put("nombre", name)
            .put("mensaje", mensaje).put("codigo", codigo).toString())
        report.put("errorServidor", mensaje).put("codigoServidor", codigo)
        atomicWrite(rejectedFile, report.toString())
        file.delete()
    }

    @Synchronized fun liberarCierreParaCorreccion(tareaId: Int): Boolean {
        if (errorCierre(tareaId).isNullOrBlank()) return false
        File(root, "finish-$tareaId.json").delete()
        return true
    }

    @Synchronized internal fun registrarTableroPreparado(tareaId: Int, operationId: String) {
        val file = File(root, "prepare-$tareaId.json")
        val manifest = if (file.exists()) JSONObject(file.readText()) else JSONObject()
        val operations = manifest.optJSONArray("operacionesPreparadas") ?: JSONArray()
        if ((0 until operations.length()).none { operations.optString(it) == operationId }) operations.put(operationId)
        manifest.put("operacionesPreparadas", operations)
        atomicWrite(file, manifest.toString())
    }

    internal fun tablerosPreparados(tareaId: Int): Set<String> {
        val file = File(root, "prepare-$tareaId.json")
        if (!file.exists()) return emptySet()
        val operations = JSONObject(file.readText()).optJSONArray("operacionesPreparadas") ?: return emptySet()
        return (0 until operations.length()).mapTo(mutableSetOf()) { operations.optString(it) }
    }

    fun informes(tareaId: Int): List<File> = root.listFiles { f -> f.name.startsWith("report-") && runCatching { JSONObject(f.readText()).optInt("tareaId") == tareaId }.getOrDefault(false) }
        ?.sortedBy { it.name } ?: emptyList()
    fun finalizaciones(): List<File> = root.listFiles { f -> f.name.startsWith("finish-") }?.sortedBy { it.name } ?: emptyList()
    fun tareasConCierrePendiente(tecnicoId: Int): List<Int> = finalizaciones().mapNotNull { file ->
        runCatching {
            JSONObject(file.readText()).takeIf { it.optInt("tecnicoId") == tecnicoId }?.optInt("tareaId")
        }.getOrNull()
    }
    fun pendiente(tareaId: Int): Boolean = File(root, "finish-$tareaId.json").exists()
    fun pendienteInicio(tareaId: Int): Boolean = File(root, "start-$tareaId.json").exists()
    fun hayPendientes(): Boolean = root.listFiles()?.any { it.name.startsWith("report-") || it.name.startsWith("finish-") || it.name.startsWith("start-") } == true
    fun ultimoError(): String = statusPrefs.getString("last_error", "").orEmpty()
    internal fun credenciales(tecnicoId: Int): Tecnico? {
        val rut = authPrefs.getString("rut_$tecnicoId", null) ?: return null
        return Tecnico(tecnicoId, "", "", rut, authPrefs.getString("password_$tecnicoId", "").orEmpty(), "", "", "")
    }
    internal fun registrarError(mensaje: String) { statusPrefs.edit().putString("last_error", mensaje).apply() }
    internal fun limpiarError() { statusPrefs.edit().remove("last_error").apply() }
    internal fun limpiarCredencialesSinPendientes(tecnicoId: Int) {
        val pendientes = root.listFiles()?.any { file ->
            (file.name.startsWith("report-") || file.name.startsWith("finish-") || file.name.startsWith("start-")) && runCatching {
                JSONObject(file.readText()).optInt("tecnicoId") == tecnicoId
            }.getOrDefault(false)
        } == true
        if (!pendientes) authPrefs.edit().remove("rut_$tecnicoId").remove("password_$tecnicoId").apply()
    }

    private fun guardarCredenciales(tecnico: Tecnico) {
        authPrefs.edit().putString("rut_${tecnico.id}", tecnico.rut)
            .putString("password_${tecnico.id}", tecnico.password).apply()
    }

    fun reintentarPendientes(context: Context) {
        if (!hayPendientes()) return
        val requierePreparacionLocal = finalizaciones().any { file ->
            runCatching { JSONObject(file.readText()).optBoolean("prepararTableros") }.getOrDefault(false)
        }
        programar(context.applicationContext, requiereInternet = !requierePreparacionLocal)
    }

    private fun File.context(): Context = appContext ?: error("OfflineSync no inicializado")
    @Volatile private var appContext: Context? = null
    fun initWithContext(context: Context) {
        appContext = context.applicationContext
        init(context)
        val current = Prefs.leerTecnico()
        if (current != null && hayPendientes()) {
            val pendingIds = buildSet {
                root.listFiles()?.filter { it.name.startsWith("report-") || it.name.startsWith("finish-") || it.name.startsWith("start-") }
                    ?.forEach { file -> runCatching { add(JSONObject(file.readText()).optInt("tecnicoId")) } }
            }
            if (current.id in pendingIds) guardarCredenciales(current)
        }
    }

    private fun programar(context: Context, requiereInternet: Boolean = true) {
        val builder = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
        if (requiereInternet) {
            builder.setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        }
        val request = builder.build()
        WorkManager.getInstance(context).enqueueUniqueWork("aring-offline-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private fun atomicWrite(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(target)) { target.delete(); check(tmp.renameTo(target)) { "No se pudo guardar el envío pendiente" } }
    }
}

object OfflineTaskCache {
    private lateinit var prefs: android.content.SharedPreferences
    fun init(context: Context) { prefs = context.getSharedPreferences("aring_offline_tasks", Context.MODE_PRIVATE) }
    fun save(tecnicoId: Int, tasks: List<Tarea>) {
        val array = org.json.JSONArray()
        tasks.filter { it.esDeTecnico(tecnicoId) }.forEach { t ->
            array.put(JSONObject().put("id", t.id).put("codigo", t.codigo).put("tipo", t.tipo)
                .put("estado", t.estado).put("nombreRecinto", t.nombreRecinto).put("direccion", t.direccion)
                .put("descripcion", t.descripcion).put("tecnicoCabeceraId", t.tecnicoCabeceraId)
                .put("tecnicoAyudantesIds", org.json.JSONArray(t.tecnicoAyudantesIds))
                .put("fechaInicio", t.fechaInicio).put("tablerosRealizados", t.tablerosRealizados))
        }
        prefs.edit().putString("tasks_$tecnicoId", array.toString()).apply()
    }
    fun load(tecnicoId: Int): org.json.JSONArray? = prefs.getString("tasks_$tecnicoId", null)?.let { org.json.JSONArray(it) }
}

class OfflineSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private fun hayConexionInternet(): Boolean {
        val manager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        return manager.getNetworkCapabilities(network)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override fun doWork(): Result {
        Prefs.init(applicationContext)
        OfflineSync.initWithContext(applicationContext)
        OfflineTaskCache.init(applicationContext)
        com.aring.movil.data.panel.TableroDraftStore.init(applicationContext)
        val directory = File(applicationContext.filesDir, "offline-outbox")
        val technicianIds = mutableSetOf<Int>()
        var falloAlgo = false
        val redDisponible = hayConexionInternet()

        var tokenRestaurado = false
        val tokenOriginal = ApiService.token
        try {
            val tokens = mutableMapOf<Int, String>()
            fun tokenPara(tecnicoId: Int): String = tokens.getOrPut(tecnicoId) {
                val tecnico = OfflineSync.credenciales(tecnicoId)
                    ?: throw ApiException("No hay credenciales guardadas para el técnico $tecnicoId. Inicie sesión de nuevo en este dispositivo.", null)
                kotlinx.coroutines.runBlocking { ApiService.loginTecnico(tecnico.rut, tecnico.password) }
            }

            // Orden garantizado: inicios (start-) → informes (report-) → finalizaciones (finish-).
            val starts = directory.listFiles { f -> f.name.startsWith("start-") }?.sortedBy { it.name }.orEmpty()
            if (redDisponible) for (file in starts) {
                runCatching {
                    val item = JSONObject(file.readText())
                    val tareaId = item.getInt("tareaId")
                    val tecnicoId = item.getInt("tecnicoId")
                    technicianIds.add(tecnicoId)
                    val token = tokenPara(tecnicoId)
                    ApiService.token = token
                    val ayudantes = item.optJSONArray("ayudantesIds") ?: JSONArray()
                    val ids = (0 until ayudantes.length()).map { ayudantes.optInt(it) }
                    kotlinx.coroutines.runBlocking { ApiService.iniciarTarea(tareaId, ids) }
                    file.delete()
                }.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    falloAlgo = true
                    OfflineSync.registrarError(
                        (e as? ApiException)?.message ?: e.message ?: "Error inesperado al iniciar una tarea pendiente."
                    )
                }
            }

            // La conversión de fotos a Base64 se hace tablero por tablero fuera de la pantalla.
            val finishRequests = directory.listFiles { f -> f.name.startsWith("finish-") }?.sortedBy { it.name }.orEmpty()
            for (finishFile in finishRequests) {
                val finish = runCatching { JSONObject(finishFile.readText()) }.getOrNull() ?: continue
                if (!finish.optBoolean("prepararTableros")) continue
                val taskId = finish.optInt("tareaId")
                val technicianId = finish.optInt("tecnicoId")
                runCatching {
                    val drafts = com.aring.movil.data.panel.TableroDraftStore.listar(taskId, technicianId)
                    require(drafts.size <= 100) { "Una tarea no puede tener más de 100 tableros." }
                    require(finish.optInt("cantidadTableros", drafts.size) <= 100) { "Una tarea no puede tener más de 100 tableros." }
                    require(drafts.all { it.terminado }) { "Hay tableros sin completar; no se puede finalizar la tarea." }
                    val prepared = OfflineSync.tablerosPreparados(taskId)
                    val total = drafts.size
                    setProgressAsync(androidx.work.workDataOf("tareaId" to taskId, "fase" to "PREPARACION", "actual" to 0, "total" to total))
                    drafts.forEachIndexed { index, draft ->
                        val operationId = if (draft.id == "legacy") "$taskId-${draft.tipo}-legacy" else draft.id
                        if (operationId !in prepared) {
                            val report = when (draft.tipo) {
                                com.aring.movil.data.panel.TYPE_TRIFASICO -> com.aring.movil.data.panel.TableroDraftStore
                                    .cargarTrifasico(taskId, draft.id, technicianId)?.let {
                                        com.aring.movil.data.panel.TableroRepository.buildTrifasico(taskId, technicianId, it)
                                    }
                                com.aring.movil.data.panel.TYPE_MONOFASICO -> com.aring.movil.data.panel.TableroDraftStore
                                    .cargarMonofasico(taskId, draft.id, technicianId)?.let {
                                        com.aring.movil.data.panel.TableroRepository.buildMonofasico(taskId, technicianId, it)
                                    }
                                else -> null
                            } ?: error("No se pudo recuperar el tablero ${index + 1}.")
                            report.put("clientOperationId", operationId)
                            OfflineSync.guardarInforme(taskId, OfflineSync.credenciales(technicianId)
                                ?: error("No hay credenciales guardadas para el técnico."), report,
                                deferUntilFinalization = true, programarSincronizacion = false)
                            OfflineSync.registrarTableroPreparado(taskId, operationId)
                        }
                        OfflineSync.guardarProgresoCierre(taskId, "PREPARACION", index + 1, total)
                        setProgressAsync(androidx.work.workDataOf("tareaId" to taskId, "fase" to "PREPARACION", "actual" to index + 1, "total" to total))
                    }
                    finish.put("prepararTableros", false)
                    finish.put("fase", "ENVIO").put("actual", 0).put("total", total)
                    val tmp = File(finishFile.parentFile, finishFile.name + ".tmp")
                    tmp.writeText(finish.toString())
                    if (!tmp.renameTo(finishFile)) { finishFile.delete(); check(tmp.renameTo(finishFile)) }
                    setProgressAsync(androidx.work.workDataOf("tareaId" to taskId, "fase" to "ENVIO", "actual" to 0, "total" to total))
                }.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    falloAlgo = true
                    OfflineSync.registrarError((e as? ApiException)?.message ?: e.message ?: "No se pudieron preparar los informes de la tarea.")
                }
            }

            // La preparación local del paquete no depende de internet. Al quedar listo,
            // se agrega un trabajo con restricción de red para enviar los informes después.
            if (!redDisponible) {
                val faltaPreparar = OfflineSync.finalizaciones().any { file ->
                    runCatching { JSONObject(file.readText()).optBoolean("prepararTableros") }.getOrDefault(false)
                }
                if (faltaPreparar) {
                    OfflineSync.registrarError("No se pudo terminar de empaquetar la tarea. Se reintentará la preparación local.")
                    tokenRestaurado = true
                    ApiService.token = tokenOriginal
                    return Result.retry()
                }
                OfflineSync.finalizaciones().forEach { file ->
                    val finish = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return@forEach
                    val taskId = finish.optInt("tareaId")
                    val total = finish.optInt("cantidadTableros")
                    OfflineSync.guardarProgresoCierre(taskId, "ESPERANDO_RED", finish.optInt("actual"), total)
                    setProgressAsync(androidx.work.workDataOf("tareaId" to taskId, "fase" to "ESPERANDO_RED", "actual" to 0, "total" to total))
                }
                OfflineSync.registrarError("El paquete está guardado en el teléfono y se enviará cuando haya conexión a internet.")
                OfflineSync.reintentarPendientes(applicationContext)
                tokenRestaurado = true
                ApiService.token = tokenOriginal
                return Result.success()
            }

            val reports = directory.listFiles { f -> f.name.startsWith("report-") }?.sortedBy { it.name }.orEmpty()
            val totalByTask = reports.mapNotNull { file ->
                runCatching { JSONObject(file.readText()).optInt("tareaId").takeIf { it > 0 } }.getOrNull()
            }.groupingBy { it }.eachCount().toMutableMap()
            val enviadosPorTarea = mutableMapOf<Int, Int>()
            directory.listFiles { f -> f.name.startsWith("finish-") }?.forEach { file ->
                runCatching {
                    val finish = JSONObject(file.readText())
                    val taskId = finish.optInt("tareaId")
                    if (taskId > 0) {
                        val total = finish.optInt("cantidadTableros", totalByTask[taskId] ?: 0)
                        if (total > 0) totalByTask[taskId] = maxOf(total, totalByTask[taskId] ?: 0)
                        if (finish.optString("fase") == "ENVIO" || finish.optString("fase") == "ESPERANDO_RED") {
                            enviadosPorTarea[taskId] = finish.optInt("actual").coerceAtMost(total)
                        }
                    }
                }
            }
            for (file in reports) {
                // Un fallo en un informe no debe abortar el resto: cada archivo se procesa por separado.
                runCatching {
                    val item = JSONObject(file.readText())
                    val taskId = item.getInt("tareaId")
                    val finishRequest = File(directory, "finish-$taskId.json")
                    if (finishRequest.exists() && runCatching { JSONObject(finishRequest.readText()).optBoolean("prepararTableros") }.getOrDefault(false)) return@runCatching
                    if (finishRequest.exists()) {
                        val finish = JSONObject(finishRequest.readText())
                        if (finish.optString("fase") != "ENVIO") {
                            val total = totalByTask[taskId] ?: finish.optInt("cantidadTableros")
                            val enviados = enviadosPorTarea[taskId] ?: finish.optInt("actual").coerceAtMost(total)
                            enviadosPorTarea[taskId] = enviados
                            OfflineSync.guardarProgresoCierre(taskId, "ENVIO", enviados, total)
                            setProgressAsync(androidx.work.workDataOf("tareaId" to taskId, "fase" to "ENVIO", "actual" to enviados, "total" to total))
                        }
                    }
                    if (item.optBoolean("deferUntilFinalization") && !File(directory, "finish-$taskId.json").exists()) return@runCatching
                    if (File(directory, "start-$taskId.json").exists()) return@runCatching
                    val tecnicoId = item.getInt("tecnicoId")
                    technicianIds.add(tecnicoId)
                    val token = tokenPara(tecnicoId)
                    kotlinx.coroutines.runBlocking {
                        ApiService.enviarInforme(token, taskId.toLong(), item.getJSONObject("payload"))
                    }
                    // Solo se elimina del outbox cuando el servidor confirmó la recepción.
                    file.delete()
                    val enviados = (enviadosPorTarea[taskId] ?: 0) + 1
                    enviadosPorTarea[taskId] = enviados
                    val total = totalByTask[taskId] ?: enviados
                    OfflineSync.guardarProgresoCierre(taskId, "ENVIO", enviados, total)
                    setProgressAsync(androidx.work.workDataOf("tareaId" to taskId, "fase" to "ENVIO", "actual" to enviados, "total" to total))
                }.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    val api = e as? ApiException
                    if (api?.codigo != null && api.codigo !in 401..403) {
                        // Rechazo definitivo del servidor (validación 4xx): reintentarlo no
                        // cambiará el resultado. El informe se archiva como "rejected-" para
                        // que no bloquee la sincronización ni vuelva a intentarse; el archivo
                        // se conserva en el dispositivo por si hay que revisarlo.
                        runCatching {
                            OfflineSync.archivarInformeRechazado(file, api.message ?: "El servidor rechazó este informe.", api.codigo)
                        }
                        OfflineSync.registrarError(api.message ?: "El servidor rechazó un informe pendiente.")
                    } else {
                        falloAlgo = true
                        OfflineSync.registrarError(
                            api?.message ?: e.message ?: "Error inesperado al enviar un informe pendiente."
                        )
                    }
                }
            }

            val rejectedTaskIds = directory.listFiles { f -> f.name.startsWith("rejected-") }?.mapNotNull { f ->
                runCatching { JSONObject(f.readText()).optInt("tareaId").takeIf { it != 0 } }.getOrNull()
            }?.toSet().orEmpty()
            val pendingReportTaskIds = directory.listFiles { f -> f.name.startsWith("report-") }?.mapNotNull { f ->
                runCatching { JSONObject(f.readText()).optInt("tareaId").takeIf { it != 0 } }.getOrNull()
            }?.toSet().orEmpty()
            val pendingStartTaskIds = directory.listFiles { f -> f.name.startsWith("start-") }?.mapNotNull { f ->
                runCatching { JSONObject(f.readText()).optInt("tareaId").takeIf { it != 0 } }.getOrNull()
            }?.toSet().orEmpty()
            val finishes = directory.listFiles { f -> f.name.startsWith("finish-") }?.sortedBy { it.name }.orEmpty()
            for (file in finishes) {
                runCatching {
                    val item = JSONObject(file.readText())
                    val tareaId = item.getInt("tareaId")
                    if (item.optBoolean("prepararTableros")) {
                        falloAlgo = true
                        return@runCatching
                    }
                    if (tareaId in rejectedTaskIds || tareaId in pendingReportTaskIds || tareaId in pendingStartTaskIds) {
                        if (tareaId in rejectedTaskIds) {
                            OfflineSync.registrarError("El servidor rechazó un informe de esta tarea. La tarea sigue abierta para corregirlo.")
                        } else {
                            falloAlgo = true
                            OfflineSync.registrarError(if (tareaId in pendingStartTaskIds)
                                "El inicio de la tarea sigue pendiente de sincronización."
                            else "Hay informes pendientes de sincronización. La tarea se cerrará cuando el servidor los reciba.")
                        }
                        return@runCatching
                    }
                    val tecnicoId = item.getInt("tecnicoId")
                    technicianIds.add(tecnicoId)
                    val token = tokenPara(tecnicoId)
                    ApiService.token = token
                    OfflineSync.guardarProgresoCierre(tareaId, "CIERRE", 0, 1)
                    setProgressAsync(androidx.work.workDataOf("tareaId" to tareaId, "fase" to "CIERRE", "actual" to 0, "total" to 1))
                    val confirmacion = kotlinx.coroutines.runBlocking { ApiService.cambiarEstadoTarea(tareaId, "FINALIZADA") }
                    if (!confirmacion.optString("estado").equals("Finalizada", ignoreCase = true)) {
                        throw ApiException("El servidor no confirmó el cierre de la tarea.", null)
                    }
                    setProgressAsync(androidx.work.workDataOf("tareaId" to tareaId, "fase" to "CONFIRMADO", "actual" to 1, "total" to 1))
                    file.delete()
                    File(directory, "prepare-$tareaId.json").delete()
                    com.aring.movil.data.panel.TableroDraftStore.limpiarTarea(tareaId, tecnicoId)
                }.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    falloAlgo = true
                    OfflineSync.registrarError(
                        (e as? ApiException)?.message ?: e.message ?: "Error inesperado al finalizar una tarea pendiente."
                    )
                }
            }

            tokenRestaurado = true
            ApiService.token = tokenOriginal

            // Los archivos con éxito ya se borraron; lo que quede sigue en el outbox para el próximo intento.
            technicianIds.forEach(OfflineSync::limpiarCredencialesSinPendientes)

            return when {
                !falloAlgo -> { OfflineSync.limpiarError(); Result.success() }
                // Cualquier fallo (red o 4xx) queda en reintento: los envíos permanecen en el outbox y
                // OfflineSync.initWithContext re-asocia credenciales frescas de la sesión al abrir la app.
                else -> Result.retry()
            }
        } catch (e: Exception) {
            OfflineSync.registrarError((e as? ApiException)?.message ?: e.message ?: "Error inesperado de sincronización.")
            return Result.retry()
        } finally {
            if (!tokenRestaurado) ApiService.token = tokenOriginal
        }
    }
}

/**
 * Refresco periódico en segundo plano: mientras haya sesión de técnico, cada 15 minutos
 * con conexión relee las tareas y técnicos asignados para mantener la lista actualizada.
 */
class RefrescoTecnicoWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return try {
            Prefs.init(applicationContext)
            OfflineSync.initWithContext(applicationContext)
            OfflineTaskCache.init(applicationContext)
            val tecnico = Prefs.leerTecnico() ?: return Result.success()
            val cred = OfflineSync.credenciales(tecnico.id) ?: tecnico
            val token = kotlinx.coroutines.runBlocking {
                ApiService.loginTecnico(cred.rut.ifBlank { tecnico.rut }, cred.password)
            }
            ApiService.token = token
            kotlinx.coroutines.runBlocking { Repositorio.cargarTecnico(tecnico.id) }
            Result.success()
        } catch (e: Exception) {
            OfflineSync.registrarError((e as? ApiException)?.message ?: e.message ?: "Error inesperado al refrescar la sesión.")
            Result.retry()
        }
    }
}

object RefrescoSesion {
    fun programar(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefrescoTecnicoWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "aring-refresco-tecnico",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
