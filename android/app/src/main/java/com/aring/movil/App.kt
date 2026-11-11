package com.aring.movil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.aring.movil.data.ApiException
import com.aring.movil.data.ApiService
import com.aring.movil.data.OfflineSync
import com.aring.movil.data.OfflineTaskCache
import com.aring.movil.data.Prefs
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Tarea
import com.aring.movil.data.Tecnico
import com.aring.movil.data.inicialesDe
import com.aring.movil.data.panel.TableroDraftStore
import com.aring.movil.data.panel.newId
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.BarraNavegacion
import com.aring.movil.ui.components.ConfirmarDialog
import com.aring.movil.ui.components.TopBar
import com.aring.movil.ui.panel.TableroConfirmacion
import com.aring.movil.ui.panel.TableroFlujo
import com.aring.movil.ui.panel.TableroSeleccion
import com.aring.movil.ui.screens.LoginScreen
import com.aring.movil.ui.tecnico.DetalleTareaTecnico
import com.aring.movil.ui.tecnico.AdministrarAyudantesScreen
import com.aring.movil.ui.tecnico.IngresarTareaScreen
import com.aring.movil.ui.tecnico.TareasTecnicoScreen
import com.aring.movil.ui.theme.FondoGris
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ITEMS_TECNICO = listOf(
    "Tareas" to Icons.Filled.List,
)

private fun textoProgresoCierre(fase: String, actual: Int, total: Int): String = when (fase) {
    "PREPARACION" -> {
        val porcentaje = if (total > 0) (actual * 100 / total).coerceIn(0, 100) else 100
        "Empaquetando información: $actual de $total tableros · $porcentaje%"
    }
    "ESPERANDO_RED" -> "Paquete completo guardado · esperando conexión para enviarlo"
    "ENVIO" -> {
        val porcentaje = if (total > 0) (actual * 99 / total).coerceIn(0, 99) else 0
        "Enviando informes al servidor: $actual de $total · $porcentaje%"
    }
    "CIERRE" -> "Informes recibidos; esperando confirmación del servidor · 99%"
    "CONFIRMADO" -> "100% · Servidor confirmó la tarea finalizada"
    else -> "Preparando el paquete local…"
}

@Composable
fun App() {
    var tecnico by remember { mutableStateOf(Prefs.leerTecnico()) }

    LaunchedEffect(Unit) {
        // Borra sesiones de administrador guardadas por versiones anteriores de la app.
        Prefs.borrarAdmin()
        ApiService.token = Prefs.leerTokenTecnico()
    }

    if (tecnico != null) {
        MainScreenTecnico(
            tecnico = tecnico!!,
            onCerrarSesion = {
                Prefs.borrarTecnico()
                Prefs.borrarTokenTecnico()
                ApiService.token = null
                tecnico = null
            }
        )
    } else {
        LoginScreen { t ->
            Prefs.guardarTecnico(t)
            Prefs.guardarTokenTecnico(ApiService.token ?: "")
            tecnico = t
        }
    }
}

private sealed interface PantallaTec {
    data object Lista : PantallaTec
    data class Detalle(val tarea: Tarea) : PantallaTec
    data class AdministrarAyudantes(val tarea: Tarea) : PantallaTec
    data class Ingresar(val tarea: Tarea) : PantallaTec
    data class SeleccionTablero(val tarea: Tarea) : PantallaTec
    data class FlujoTablero(val tarea: Tarea, val tipo: String, val inspectionId: String) : PantallaTec
    data class Confirmacion(val tarea: Tarea) : PantallaTec
}

private val PantallaTecSaver = listSaver<PantallaTec, String>(
    save = { pantalla ->
        when (pantalla) {
            is PantallaTec.Lista -> listOf("Lista")
            is PantallaTec.Detalle -> listOf("Detalle", pantalla.tarea.id.toString())
            is PantallaTec.AdministrarAyudantes -> listOf("AdministrarAyudantes", pantalla.tarea.id.toString())
            is PantallaTec.Ingresar -> listOf("Ingresar", pantalla.tarea.id.toString())
            is PantallaTec.SeleccionTablero -> listOf("SeleccionTablero", pantalla.tarea.id.toString())
            is PantallaTec.FlujoTablero -> listOf("FlujoTablero", pantalla.tarea.id.toString(), pantalla.tipo, pantalla.inspectionId)
            is PantallaTec.Confirmacion -> listOf("Confirmacion", pantalla.tarea.id.toString())
        }
    },
    restore = { lista ->
        val tarea = lista.getOrNull(1)?.toIntOrNull()
            ?.let { id -> Repositorio.tareas.firstOrNull { it.id == id } }
        when (lista[0]) {
            "Detalle" -> tarea?.let { PantallaTec.Detalle(it) }
            "AdministrarAyudantes" -> tarea?.let { PantallaTec.AdministrarAyudantes(it) }
            "Ingresar" -> tarea?.let { PantallaTec.Ingresar(it) }
            "SeleccionTablero" -> tarea?.let { PantallaTec.SeleccionTablero(it) }
            "FlujoTablero" -> tarea?.let { PantallaTec.FlujoTablero(it, lista.getOrNull(2) ?: "", lista.getOrNull(3) ?: newId()) }
            "Confirmacion" -> tarea?.let { PantallaTec.Confirmacion(it) }
            else -> null
        } ?: PantallaTec.Lista
    }
)

@Composable
fun MainScreenTecnico(tecnico: Tecnico, onCerrarSesion: () -> Unit) {
    val context = LocalContext.current
    var confirmarSalida by remember { mutableStateOf(false) }
    var pantalla by rememberSaveable(stateSaver = PantallaTecSaver) {
        mutableStateOf<PantallaTec>(PantallaTec.Lista)
    }
    var confirmarFinalizar by rememberSaveable { mutableStateOf(false) }
    var tareaFinalizarId by rememberSaveable { mutableIntStateOf(0) }
    var segundosRestantes by rememberSaveable { mutableIntStateOf(60) }
    var errorFinalizar by remember { mutableStateOf("") }
    val progresoCierre = remember { mutableStateMapOf<Int, String>() }
    val scope = rememberCoroutineScope()

    fun refrescarTecnico() {
        scope.launch { runCatching { Repositorio.cargarTecnico(tecnico.id) } }
    }

    LaunchedEffect(Unit) {
        runCatching { Repositorio.cargarTecnico(tecnico.id) }
    }

    LaunchedEffect(tecnico.id) {
        while (true) {
            val estado = withContext(Dispatchers.IO) {
                val ids = OfflineSync.tareasConCierrePendiente(tecnico.id)
                val errors = ids.filter { !OfflineSync.errorCierre(it).isNullOrBlank() }.toSet()
                Triple(ids, ids.filterNot { it in errors }.mapNotNull { tareaId ->
                    val progress = OfflineSync.progresoPersistido(tareaId)
                    if (progress != null) {
                        tareaId to textoProgresoCierre(progress.first, progress.second, progress.third)
                    } else null
                }.toMap(), errors)
            }
            estado.second.forEach { (tareaId, texto) -> progresoCierre[tareaId] = texto }
            progresoCierre.keys.toList().filter { it !in estado.first && !progresoCierre[it].orEmpty().startsWith("100%") }
                .forEach(progresoCierre::remove)
            estado.third.forEach(progresoCierre::remove)
            kotlinx.coroutines.delay(1_000)
        }
    }

    LaunchedEffect(tecnico.id) {
        var actualizacionEnCurso = false
        while (true) {
            kotlinx.coroutines.delay(1_000)
            if (segundosRestantes <= 1) {
                segundosRestantes = 60
                if (!actualizacionEnCurso) {
                    actualizacionEnCurso = true
                    launch {
                        try {
                            runCatching { Repositorio.cargarTecnico(tecnico.id) }
                        } finally {
                            actualizacionEnCurso = false
                        }
                    }
                }
            } else {
                segundosRestantes -= 1
            }
        }
    }

    // Fase 1a: al recuperar conexión se re-programa el envío del outbox y se refresca la vista,
    // de modo que el aviso rojo de sincronización desaparece solo cuando el servidor confirmó.
    DisposableEffect(tecnico.id) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                OfflineSync.reintentarPendientes(appContext)
                refrescarTecnico()
            }
        }
        val peticion = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            manager?.registerNetworkCallback(peticion, callback)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                manager?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    // Fase 1b: cuando la cadena "aring-offline-sync" termina, se refresca el estado en vivo:
    // el chip "Pendiente de sincronizar" desaparece y la tarea pasa a Finalizada sin acciones del usuario.
    DisposableEffect(tecnico.id) {
        val observador = Observer<List<WorkInfo>> { estados ->
            if (estados != null) {
                estados.forEach { info ->
                    val data = info.progress
                    val taskId = data.getInt("tareaId", 0)
                    val actual = data.getInt("actual", 0)
                    val total = data.getInt("total", 0)
                    val fase = data.getString("fase")
                    if (taskId > 0 && !fase.isNullOrBlank()) {
                        progresoCierre[taskId] = textoProgresoCierre(fase, actual, total)
                    }
                }
                if (estados.isNotEmpty() && estados.all { it.state.isFinished }) {
                    estados.forEach { info ->
                        val taskId = info.progress.getInt("tareaId", 0)
                        if (taskId > 0 && info.progress.getString("fase") == "CONFIRMADO") {
                            progresoCierre[taskId] = textoProgresoCierre("CONFIRMADO", 1, 1)
                        } else if (taskId > 0 && (!OfflineSync.pendiente(taskId) || !OfflineSync.errorCierre(taskId).isNullOrBlank())) {
                            progresoCierre.remove(taskId)
                        }
                    }
                    refrescarTecnico()
                }
            }
        }
        val live = WorkManager.getInstance(context.applicationContext)
            .getWorkInfosForUniqueWorkLiveData("aring-offline-sync")
        live.observeForever(observador)
        onDispose { live.removeObserver(observador) }
    }

    fun tareaActual(tarea: Tarea): Tarea =
        Repositorio.tareas.firstOrNull { it.id == tarea.id } ?: tarea

    fun volver() {
        errorFinalizar = ""
        pantalla = when (val actual = pantalla) {
            is PantallaTec.AdministrarAyudantes -> PantallaTec.Detalle(tareaActual(actual.tarea))
            else -> PantallaTec.Lista
        }
    }

    BackHandler(enabled = pantalla is PantallaTec.AdministrarAyudantes) { volver() }

    Column(
        Modifier
            .fillMaxSize()
            .background(FondoGris)
    ) {
        val ocultarTopBar = pantalla is PantallaTec.FlujoTablero || pantalla is PantallaTec.Confirmacion
        if (!ocultarTopBar) {
            TopBar(
                iniciales = inicialesDe(tecnico.nombre, tecnico.apellido),
                nombre = tecnico.nombre,
                apellido = tecnico.apellido,
                segundosRestantes = segundosRestantes,
                onBack = if (pantalla is PantallaTec.Lista) {
                    null
                } else {
                    { volver() }
                },
                onCerrarSesion = { confirmarSalida = true }
            )
        }

        if (errorFinalizar.isNotBlank()) {
            BannerError(errorFinalizar)
            Spacer(Modifier.height(8.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val p = pantalla) {
                is PantallaTec.Lista -> {
                    TareasTecnicoScreen(
                        tecnico = tecnico,
                        onAbrirTarea = { pantalla = PantallaTec.Detalle(it) }
                    )
                }
                is PantallaTec.Detalle -> {
                    val tarea = tareaActual(p.tarea)
                    DetalleTareaTecnico(
                        tarea = tarea,
                        tecnicoId = tecnico.id,
                        cierrePendiente = OfflineSync.pendiente(tarea.id),
                        errorCierre = OfflineSync.errorCierre(tarea.id),
                        progresoCierre = progresoCierre[tarea.id].orEmpty(),
                        onCorregirCierre = {
                            if (OfflineSync.liberarCierreParaCorreccion(tarea.id)) {
                                val editable = tarea.copy(estado = "En proceso", sincronizacionPendiente = false)
                                val index = Repositorio.tareas.indexOfFirst { it.id == tarea.id }
                                if (index >= 0) Repositorio.tareas[index] = editable
                                OfflineTaskCache.save(tecnico.id, Repositorio.tareas.toList())
                                progresoCierre.remove(tarea.id)
                                pantalla = PantallaTec.SeleccionTablero(editable)
                            }
                        },
                        onAdministrarAyudantes = { pantalla = PantallaTec.AdministrarAyudantes(tarea) },
                        onIngresar = {
                            pantalla = if (OfflineSync.pendiente(tarea.id)) {
                                PantallaTec.SeleccionTablero(tarea)
                            } else if (tarea.esInvitado(tecnico.id)) {
                                PantallaTec.SeleccionTablero(tarea)
                            } else {
                                PantallaTec.Ingresar(tarea)
                            }
                        }
                    )
                }
                is PantallaTec.AdministrarAyudantes -> AdministrarAyudantesScreen(
                    tecnico = tecnico,
                    tarea = tareaActual(p.tarea),
                    onGuardado = { actualizada -> pantalla = PantallaTec.Detalle(actualizada) }
                )
                is PantallaTec.Ingresar -> IngresarTareaScreen(
                    tecnico = tecnico,
                    tarea = tareaActual(p.tarea),
                    onContinuar = { pantalla = PantallaTec.SeleccionTablero(p.tarea) }
                )
                is PantallaTec.SeleccionTablero -> {
                    val tarea = tareaActual(p.tarea)
                    if (OfflineSync.pendiente(tarea.id)) {
                        TableroConfirmacion(
                            tarea = tarea,
                            progreso = progresoCierre[tarea.id].orEmpty(),
                            errorCierre = OfflineSync.errorCierre(tarea.id),
                            onVolverATareas = { volver() }
                        )
                    } else {
                        TableroSeleccion(
                            tarea = tarea,
                            borradores = TableroDraftStore.listar(tarea.id),
                            puedeFinalizar = !tarea.esInvitado(tecnico.id),
                            onSeleccionarTipo = { tipo ->
                                val cantidad = maxOf(tarea.tablerosRealizados, TableroDraftStore.listar(tarea.id).size)
                                if (cantidad >= 100) {
                                    errorFinalizar = "Esta tarea alcanzó el límite de 100 tableros."
                                } else {
                                    errorFinalizar = ""
                                    pantalla = PantallaTec.FlujoTablero(p.tarea, tipo, newId())
                                }
                            },
                            onEditar = { borrador -> pantalla = PantallaTec.FlujoTablero(p.tarea, borrador.tipo, borrador.id) },
                            onEliminar = { tablero -> TableroDraftStore.limpiar(tarea.id, tablero.tipo, tablero.id) },
                            onFinalizar = {
                                tareaFinalizarId = p.tarea.id
                                confirmarFinalizar = true
                            },
                            onVolver = { volver() }
                        )
                    }
                }
                is PantallaTec.FlujoTablero -> {
                    if (OfflineSync.pendiente(p.tarea.id)) {
                        TableroConfirmacion(
                            tarea = tareaActual(p.tarea),
                            progreso = progresoCierre[p.tarea.id].orEmpty(),
                            errorCierre = OfflineSync.errorCierre(p.tarea.id),
                            onVolverATareas = { pantalla = PantallaTec.Lista }
                        )
                    } else {
                        TableroFlujo(
                            tipo = p.tipo,
                            tareaId = p.tarea.id,
                            inspectionId = p.inspectionId,
                            onFinalizado = {
                                TableroDraftStore.limpiarIncompletos(p.tarea.id)
                                pantalla = PantallaTec.SeleccionTablero(p.tarea)
                            },
                            onVolver = {
                                TableroDraftStore.limpiarIncompletos(p.tarea.id)
                                pantalla = PantallaTec.SeleccionTablero(p.tarea)
                            }
                        )
                    }
                }
                is PantallaTec.Confirmacion -> TableroConfirmacion(
                    tarea = tareaActual(p.tarea),
                    progreso = progresoCierre[p.tarea.id].orEmpty(),
                    errorCierre = OfflineSync.errorCierre(p.tarea.id),
                    onVolverATareas = {
                        volver()
                    }
                )
            }
        }

        if (!ocultarTopBar) {
            BarraNavegacion(
                items = ITEMS_TECNICO,
                actual = 0,
                onSeleccionar = { volver() }
            )
        }
    }

    if (confirmarSalida) {
        ConfirmarDialog(
            titulo = "Cerrar sesión",
            mensaje = "¿Realmente desea cerrar sesión?",
            confirmarTexto = "Sí",
            cancelarTexto = "No",
            onConfirmar = {
                confirmarSalida = false
                onCerrarSesion()
            },
            onCancelar = { confirmarSalida = false }
        )
    }

    if (confirmarFinalizar) {
        ConfirmarDialog(
            titulo = "Finalizar tarea",
            mensaje = "Revise los tableros realizados. Al confirmar, se enviarán todos los informes y se cerrará la tarea.",
            confirmarTexto = "Sí",
            cancelarTexto = "No",
            onConfirmar = {
                confirmarFinalizar = false
                errorFinalizar = ""
                scope.launch {
                    val tarea = Repositorio.tareas.firstOrNull { it.id == tareaFinalizarId }
                    try {
                        if (tarea == null || !tarea.esCabecera(tecnico.id)) {
                            errorFinalizar = "Solo el técnico responsable puede finalizar la tarea."
                            return@launch
                        }
                        val borradores = TableroDraftStore.listar(tareaFinalizarId)
                        if (borradores.isEmpty() && tarea.tablerosRealizados == 0 && OfflineSync.informes(tareaFinalizarId).isEmpty()) {
                            errorFinalizar = "Esta tarea no tiene tableros inspeccionados. Registre al menos uno antes de finalizar."
                            return@launch
                        }
                        if (borradores.any { !it.terminado }) {
                            errorFinalizar = "Hay inspecciones sin completar. Seleccione el tipo de tablero correspondiente para continuar y terminar su registro antes de cerrar la tarea."
                            return@launch
                        }
                        if (maxOf(tarea.tablerosRealizados, borradores.size) > 100) {
                            errorFinalizar = "Esta tarea supera el límite de 100 tableros. Revise los registros antes de finalizar."
                            return@launch
                        }
                        progresoCierre[tareaFinalizarId] = "Preparando el paquete local…"
                        withContext(Dispatchers.IO) {
                            OfflineSync.finalizarDesdeTableros(tareaFinalizarId, tecnico, borradores.size)
                        }
                        val actualizada = Repositorio.tareas.firstOrNull { it.id == tareaFinalizarId } ?: tarea
                        val finalLocal = actualizada.copy(
                            estado = "En proceso",
                            sincronizacionPendiente = true,
                            tablerosRealizados = maxOf(actualizada.tablerosRealizados, tarea.tablerosRealizados, borradores.size)
                        )
                        val index = Repositorio.tareas.indexOfFirst { it.id == tareaFinalizarId }
                        if (index >= 0) Repositorio.tareas[index] = finalLocal
                        OfflineTaskCache.save(tecnico.id, Repositorio.tareas.toList())
                        pantalla = PantallaTec.Confirmacion(finalLocal)
                    } catch (e: Exception) {
                        progresoCierre.remove(tareaFinalizarId)
                        errorFinalizar = e.message ?: "No se pudo guardar la finalización en el dispositivo."
                        pantalla = if (tarea != null) {
                            PantallaTec.SeleccionTablero(tarea)
                        } else {
                            PantallaTec.Lista
                        }
                    }
                }
            },
            onCancelar = { confirmarFinalizar = false }
        )
    }
}

