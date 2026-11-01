package com.aring.movil.data

import android.graphics.Bitmap
import android.util.Base64
import com.aring.movil.BuildConfig
import com.aring.movil.ui.panel.common.decodeSampledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String, val codigo: Int? = null) : Exception(message)

object ApiService {

    /**
     * Dirección del backend, configurable al compilar mediante la propiedad Gradle API_BASE_URL.
     * El valor predeterminado usa el acceso HTTPS público de Tailscale Funnel.
     */
    const val BASE_URL = BuildConfig.API_BASE_URL

    @Volatile
    var token: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun login(usuario: String, password: String): JSONObject {
        val cuerpo = JSONObject()
            .put("username", usuario)
            .put("password", password)
            .toString()
        return parsearObjeto(ejecutar("POST", "/api/auth/login", cuerpo))
    }

    suspend fun loginTecnico(rut: String, password: String): String =
        login(rut, password).optString("token")

    suspend fun dashboardStats(): JSONObject =
        parsearObjeto(ejecutar("GET", "/api/dashboard/stats"))

    suspend fun tareas(): JSONArray =
        parsearArreglo(ejecutar("GET", "/api/tareas"))

    suspend fun tareasMias(tecnicoId: Int): JSONArray =
        parsearArreglo(ejecutar("GET", "/api/tareas/mias?tecnicoId=$tecnicoId"))

    suspend fun crearTarea(cuerpo: JSONObject): JSONObject =
        parsearObjeto(ejecutar("POST", "/api/tareas", cuerpo.toString()))

    suspend fun iniciarTarea(id: Int, ayudantesIds: List<Int>): JSONObject {
        val arr = JSONArray()
        ayudantesIds.forEach { arr.put(it) }
        return parsearObjeto(
            ejecutar(
                "POST",
                "/api/tareas/$id/iniciar",
                JSONObject().put("ayudantesIds", arr).toString()
            )
        )
    }

    suspend fun actualizarCabeceraTarea(id: Int, tecnicoId: Int): JSONObject =
        parsearObjeto(
            ejecutar(
                "PUT",
                "/api/tareas/$id",
                JSONObject().put("tecnicoCabeceraId", tecnicoId).toString()
            )
        )

    suspend fun cambiarEstadoTarea(id: Int, estado: String): JSONObject =
        parsearObjeto(
            ejecutar(
                "PUT",
                "/api/tareas/$id/estado",
                JSONObject().put("estado", estado).toString()
            )
        )

    suspend fun eliminarTarea(id: Int) {
        ejecutar("DELETE", "/api/tareas/$id")
    }

    suspend fun tecnicos(): JSONArray =
        parsearArreglo(ejecutar("GET", "/api/tecnicos"))

    suspend fun clientes(): JSONArray =
        parsearArreglo(ejecutar("GET", "/api/clientes"))

    suspend fun crearTecnico(cuerpo: JSONObject): JSONObject =
        parsearObjeto(ejecutar("POST", "/api/tecnicos", cuerpo.toString()))

    suspend fun actualizarTecnico(id: Int, cuerpo: JSONObject): JSONObject =
        parsearObjeto(ejecutar("PUT", "/api/tecnicos/$id", cuerpo.toString()))

    suspend fun eliminarTecnico(id: Int) {
        ejecutar("DELETE", "/api/tecnicos/$id")
    }

    suspend fun notificaciones(tecnicoId: Int?): JSONArray {
        val path = if (tecnicoId != null) {
            "/api/notificaciones?tecnicoId=$tecnicoId"
        } else {
            "/api/notificaciones"
        }
        return parsearArreglo(ejecutar("GET", path))
    }

    suspend fun marcarLeida(id: Int): JSONObject =
        parsearObjeto(ejecutar("PUT", "/api/notificaciones/$id/leida"))

    suspend fun marcarTodasLeidas(tecnicoId: Int?) {
        val path = if (tecnicoId != null) {
            "/api/notificaciones/leer-todas?tecnicoId=$tecnicoId"
        } else {
            "/api/notificaciones/leer-todas"
        }
        ejecutar("PUT", path)
    }

    suspend fun eliminarNotificaciones() {
        ejecutar("DELETE", "/api/notificaciones")
    }

    suspend fun eliminarNotificacionesTecnico(tecnicoId: Int) {
        ejecutar("DELETE", "/api/notificaciones?tecnicoId=$tecnicoId")
    }

    suspend fun enviarInforme(
        token: String,
        tareaId: Long,
        cuerpo: JSONObject
    ): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/api/tareas/$tareaId/tableros")
            .addHeader("Authorization", "Bearer $token")
            .post(cuerpo.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val mensaje = runCatching {
                    JSONObject(body).optString("mensaje")
                }.getOrNull()

                throw ApiException(
                    mensaje?.takeIf { it.isNotBlank() }
                        ?: "Error del servidor al guardar el informe (${resp.code}).",
                    resp.code
                )
            }
            runCatching { JSONObject(body) }.getOrDefault(JSONObject())
        }
    }

    fun fotoBase64(path: String?): String? {
        if (path == null) return null
        return try {
            val bitmap = decodeSampledBitmap(path, maxDim = 1280) ?: return null
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            if (!bitmap.isRecycled) bitmap.recycle()
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun ejecutar(
        method: String,
        path: String,
        cuerpo: String? = null,
        tokenOverride: String? = null
    ): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .apply {
                (tokenOverride ?: token)?.let { header("Authorization", "Bearer $it") }
            }
            .method(method, cuerpo?.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val mensaje = runCatching {
                        JSONObject(body).optString("mensaje")
                    }.getOrNull()

                    throw ApiException(
                        mensaje?.takeIf { it.isNotBlank() }
                            ?: "Error del servidor (${resp.code}).",
                        resp.code
                    )
                }
                body
            }
        } catch (e: IOException) {
            throw ApiException(
                "No se pudo conectar con el servidor. Verifique que el backend esté disponible.",
                null
            )
        }
    }

    private fun parsearObjeto(body: String): JSONObject =
        runCatching { JSONObject(body) }.getOrDefault(JSONObject())

    private fun parsearArreglo(body: String): JSONArray =
        runCatching { JSONArray(body) }.getOrDefault(JSONArray())
}
