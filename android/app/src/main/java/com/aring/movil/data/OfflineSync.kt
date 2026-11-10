package com.aring.movil.data

import android.content.Context
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
                .put("operacionesPreparadas", JSONArray())
            atomicWrite(File(root, "prepare-$tareaId.json"), manifest.toString())
            atomicWrite(File(root, "finish-$tareaId.json"), JSONObject()
                .put("tareaId", tareaId).put("tecnicoId", tecnico.id)
                .put("prepararTableros", true).put("cantidadTableros", cantidadTableros).toString())
        }
        programar(root.context())
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
        if (hayPendientes()) programar(context.applicationContext)
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

    private fun programar(context: Context) {
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
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
    override fun doWork(): Result {
        Prefs.init(applicationContext)
        OfflineSync.initWithContext(applicationContext)
        com.aring.movil.data.panel.TableroDraftStore.init(applicationContext)
        val directory = File(applicationContext.filesDir, "offline-outbox")
        val technicianIds = mutableSetOf<Int>()
        var falloAlgo = false

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
            for (file in starts) {
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
                        setProgressAsync(androidx.work.workDataOf("tareaId" to taskId, "fase" to "PREPARACION", "actual" to index + 1, "total" to total))
                    }
                    finish.put("prepararTableros", false)
                    val tmp = File(finishFile.parentFile, finishFile.name + ".tmp")
                    tmp.writeText(finish.toString())
                    if (!tmp.renameTo(finishFile)) { finishFile.delete(); check(tmp.renameTo(finishFile)) }
                    setProgressAsync(androidx.work.workDataOf("tareaId" to taskId, "fase" to "ENVIO", "total" to total))
                }.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    falloAlgo = true
                    OfflineSync.registrarError((e as? ApiException)?.message ?: e.message ?: "No se pudieron preparar los informes de la tarea.")
                }
            }

            val reports = directory.listFiles { f -> f.name.startsWith("report-") }?.sortedBy { it.name }.orEmpty()
            for (file in reports) {
                // Un fallo en un informe no debe abortar el resto: cada archivo se procesa por separado.
                runCatching {
                    val item = JSONObject(file.readText())
                    val taskId = item.getInt("tareaId")
                    val finishRequest = File(directory, "finish-$taskId.json")
                    if (finishRequest.exists() && runCatching { JSONObject(finishRequest.readText()).optBoolean("prepararTableros") }.getOrDefault(false)) return@runCatching
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
                }.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    val api = e as? ApiException
                    if (api?.codigo != null && api.codigo !in 401..403) {
                        // Rechazo definitivo del servidor (validación 4xx): reintentarlo no
                        // cambiará el resultado. El informe se archiva como "rejected-" para
                        // que no bloquee la sincronización ni vuelva a intentarse; el archivo
                        // se conserva en el dispositivo por si hay que revisarlo.
                        file.renameTo(File(directory, "rejected-" + file.name.removePrefix("report-")))
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
                        falloAlgo = true
                        OfflineSync.registrarError(if (tareaId in rejectedTaskIds)
                            "El servidor rechazó un informe de esta tarea. La tarea se mantiene abierta para corregirlo."
                        else if (tareaId in pendingStartTaskIds) "El inicio de la tarea sigue pendiente de sincronización."
                        else "Hay informes pendientes de sincronización. La tarea se cerrará cuando el servidor los reciba.")
                        return@runCatching
                    }
                    val tecnicoId = item.getInt("tecnicoId")
                    technicianIds.add(tecnicoId)
                    val token = tokenPara(tecnicoId)
                    ApiService.token = token
                    kotlinx.coroutines.runBlocking { ApiService.cambiarEstadoTarea(tareaId, "FINALIZADA") }
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
