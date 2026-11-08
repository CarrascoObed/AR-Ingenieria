package com.aring.movil

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
import com.aring.movil.data.panel.TableroRepository
import com.aring.movil.data.panel.TYPE_MONOFASICO
import com.aring.movil.data.panel.TYPE_TRIFASICO
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
import com.aring.movil.ui.tecnico.IngresarTareaScreen
import com.aring.movil.ui.tecnico.TareasTecnicoScreen
import com.aring.movil.ui.theme.FondoGris
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ITEMS_TECNICO = listOf(
    "Tareas" to Icons.Filled.List,
)

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
    var errorFinalizar by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refrescarTecnico() {
        scope.launch { runCatching { Repositorio.cargarTecnico(tecnico.id) } }
    }

    LaunchedEffect(Unit) {
        runCatching { Repositorio.cargarTecnico(tecnico.id) }
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
            if (estados != null && estados.any { it.state.isFinished }) {
                refrescarTecnico()
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
        pantalla = PantallaTec.Lista
    }

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
                        onIngresar = {
                            pantalla = if (OfflineSync.pendiente(tarea.id) && tarea.esCabecera(tecnico.id)) {
                                PantallaTec.SeleccionTablero(tarea)
                            } else if (tarea.esInvitado(tecnico.id)) {
                                PantallaTec.SeleccionTablero(tarea)
                            } else {
                                PantallaTec.Ingresar(tarea)
                            }
                        }
                    )
                }
                is PantallaTec.Ingresar -> IngresarTareaScreen(
                    tecnico = tecnico,
                    tarea = tareaActual(p.tarea),
                    onContinuar = { pantalla = PantallaTec.SeleccionTablero(p.tarea) }
                )
                is PantallaTec.SeleccionTablero -> {
                    val tarea = tareaActual(p.tarea)
                    TableroSeleccion(
                        tarea = tarea,
                        borradores = TableroDraftStore.listar(tarea.id),
                        puedeFinalizar = !tarea.esInvitado(tecnico.id),
                        onSeleccionarTipo = { tipo -> pantalla = PantallaTec.FlujoTablero(p.tarea, tipo, newId()) },
                        onEditar = { borrador -> pantalla = PantallaTec.FlujoTablero(p.tarea, borrador.tipo, borrador.id) },
                        onEliminar = { tablero -> TableroDraftStore.limpiar(tarea.id, tablero.tipo, tablero.id) },
                        onFinalizar = {
                            tareaFinalizarId = p.tarea.id
                            confirmarFinalizar = true
                        },
                        onVolver = { volver() }
                    )
                }
                is PantallaTec.FlujoTablero -> TableroFlujo(
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
                is PantallaTec.Confirmacion -> TableroConfirmacion(
                    tarea = tareaActual(p.tarea),
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
                        val cierreYaPendiente = OfflineSync.pendiente(tareaFinalizarId)
                        borradores.forEach { borrador ->
                            val informe = withContext(Dispatchers.Default) {
                                when (borrador.tipo) {
                                    TYPE_TRIFASICO -> TableroDraftStore.cargarTrifasico(tareaFinalizarId, borrador.id)?.let { TableroRepository.buildTrifasico(tareaFinalizarId, tecnico.id, it) }
                                    TYPE_MONOFASICO -> TableroDraftStore.cargarMonofasico(tareaFinalizarId, borrador.id)?.let { TableroRepository.buildMonofasico(tareaFinalizarId, tecnico.id, it) }
                                    else -> null
                                }
                            } ?: error("No se pudo recuperar uno de los tableros guardados.")
                            withContext(Dispatchers.IO) { OfflineSync.guardarInforme(tareaFinalizarId, tecnico, informe, deferUntilFinalization = true) }
                        }
                        OfflineSync.finalizar(tareaFinalizarId, tecnico)
                        val actualizada = Repositorio.tareas.firstOrNull { it.id == tareaFinalizarId } ?: tarea
                        val finalLocal = actualizada.copy(
                            estado = "Finalizada",
                            sincronizacionPendiente = true,
                            tablerosRealizados = maxOf(
                                actualizada.tablerosRealizados,
                                tarea.tablerosRealizados + if (cierreYaPendiente) 0 else borradores.size
                            )
                        )
                        val index = Repositorio.tareas.indexOfFirst { it.id == tareaFinalizarId }
                        if (index >= 0) Repositorio.tareas[index] = finalLocal
                        OfflineTaskCache.save(tecnico.id, Repositorio.tareas.toList())
                        pantalla = PantallaTec.Confirmacion(finalLocal)
                    } catch (e: Exception) {
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

