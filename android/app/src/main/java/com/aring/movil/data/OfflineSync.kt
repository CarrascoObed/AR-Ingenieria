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

    @Synchronized fun guardarInforme(tareaId: Int, tecnico: Tecnico, payload: JSONObject) {
        guardarCredenciales(tecnico)
        val id = payload.optString("clientOperationId").ifBlank { UUID.randomUUID().toString() }
        payload.put("clientOperationId", id)
        atomicWrite(File(root, "report-$id.json"), JSONObject()
            .put("tareaId", tareaId).put("tecnicoId", tecnico.id).put("payload", payload).toString())
        programar(root.context())
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

            val reports = directory.listFiles { f -> f.name.startsWith("report-") }?.sortedBy { it.name }.orEmpty()
            for (file in reports) {
                // Un fallo en un informe no debe abortar el resto: cada archivo se procesa por separado.
                runCatching {
                    val item = JSONObject(file.readText())
                    val taskId = item.getInt("tareaId")
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

            val finishes = directory.listFiles { f -> f.name.startsWith("finish-") }?.sortedBy { it.name }.orEmpty()
            for (file in finishes) {
                runCatching {
                    val item = JSONObject(file.readText())
                    val tareaId = item.getInt("tareaId")
                    val tecnicoId = item.getInt("tecnicoId")
                    technicianIds.add(tecnicoId)
                    val token = tokenPara(tecnicoId)
                    ApiService.token = token
                    kotlinx.coroutines.runBlocking { ApiService.cambiarEstadoTarea(tareaId, "FINALIZADA") }
                    file.delete()
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
 * Refresco periódico en segundo plano (Fase 3): mientras haya sesión de técnico, cada 15 minutos
 * con conexión relee tareas y notificaciones del servidor, para que la invitación y los cambios
 * aparecen sin necesidad de tocar el botón de refresco.
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
