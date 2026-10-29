package com.aring.movil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.aring.movil.data.Admin
import com.aring.movil.data.ApiException
import com.aring.movil.data.ApiService
import com.aring.movil.data.Prefs
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Sesion
import com.aring.movil.data.Tarea
import com.aring.movil.data.Tecnico
import com.aring.movil.data.inicialesDe
import com.aring.movil.data.panel.TableroDraftStore
import com.aring.movil.data.panel.TableroRepository
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.BarraNavegacion
import com.aring.movil.ui.components.ConfirmarDialog
import com.aring.movil.ui.components.HojaNotificaciones
import com.aring.movil.ui.components.HojaNotificacionesTecnico
import com.aring.movil.ui.components.TopBar
import com.aring.movil.ui.panel.EnviarInformeScreen
import com.aring.movil.ui.panel.TableroConfirmacion
import com.aring.movil.ui.panel.TableroFlujo
import com.aring.movil.ui.panel.TableroSeleccion
import com.aring.movil.ui.screens.DashboardScreen
import com.aring.movil.ui.screens.LoginScreen
import com.aring.movil.ui.screens.TareasScreen
import com.aring.movil.ui.screens.TecnicosScreen
import com.aring.movil.ui.tecnico.DetalleTareaTecnico
import com.aring.movil.ui.tecnico.DocumentosTecnicoScreen
import com.aring.movil.ui.tecnico.IngresarTareaScreen
import com.aring.movil.ui.tecnico.TareasTecnicoScreen
import com.aring.movil.ui.theme.FondoGris
import kotlinx.coroutines.launch

private val ITEMS_ADMIN = listOf(
    "Dashboard" to Icons.Filled.Home,
    "Técnicos" to Icons.Filled.Person,
    "Tareas" to Icons.Filled.List,
)

private val ITEMS_TECNICO = listOf(
    "Inicio" to Icons.Filled.Home,
    "Tareas" to Icons.Filled.List,
    "Documentos" to Icons.Filled.Description,
)

@Composable
fun App() {
    var admin by remember { mutableStateOf(Prefs.leerAdmin()) }
    var tecnico by remember { mutableStateOf(Prefs.leerTecnico()) }

    LaunchedEffect(Unit) {
        ApiService.token = admin?.token ?: Prefs.leerTokenTecnico()
    }

    when {
        admin != null -> MainScreen(
            admin = admin!!,
            onCerrarSesion = {
                Prefs.borrarAdmin()
                ApiService.token = null
                admin = null
            }
        )
        tecnico != null -> MainScreenTecnico(
            tecnico = tecnico!!,
            onCerrarSesion = {
                Prefs.borrarTecnico()
                Prefs.borrarTokenTecnico()
                ApiService.token = null
                tecnico = null
            }
        )
        else -> LoginScreen { s ->
            when (s) {
                is Sesion.AdminSesion -> {
                    Prefs.guardarAdmin(s.admin)
                    Prefs.borrarTecnico()
                    Prefs.borrarTokenTecnico()
                    ApiService.token = s.admin.token
                    admin = s.admin
                }
                is Sesion.TecnicoSesion -> {
                    Prefs.guardarTecnico(s.tecnico)
                    Prefs.guardarTokenTecnico(ApiService.token ?: "")
                    Prefs.borrarAdmin()
                    tecnico = s.tecnico
                }
            }
        }
    }
}

@Composable
fun MainScreen(admin: Admin, onCerrarSesion: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var clave by remember { mutableIntStateOf(0) }
    var refrescando by remember { mutableStateOf(false) }
    var notifAbierto by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val noLeidas = Repositorio.notificaciones.count { !it.leida }

    LaunchedEffect(Unit) {
        refrescando = true
        runCatching { Repositorio.cargarAdmin() }
        refrescando = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(FondoGris)
    ) {
        TopBar(
            iniciales = admin.iniciales,
            nombre = admin.nombre,
            apellido = admin.apellido,
            refrescando = refrescando,
            noLeidas = noLeidas,
            onRefresh = {
                scope.launch {
                    refrescando = true
                    runCatching { Repositorio.cargarAdmin() }
                    clave++
                    refrescando = false
                }
            },
            onAbrirNotificaciones = { notifAbierto = true },
            onCerrarSesion = { confirmarSalida = true }
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> DashboardScreen(admin, clave)
                1 -> TecnicosScreen(clave)
                else -> TareasScreen(clave)
            }
        }

        BarraNavegacion(items = ITEMS_ADMIN, actual = tab, onSeleccionar = { tab = it })
    }

    if (notifAbierto) {
        HojaNotificaciones(onCerrar = { notifAbierto = false })
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
}

private sealed interface PantallaTec {
    data object Lista : PantallaTec
    data class Detalle(val tarea: Tarea) : PantallaTec
    data class Ingresar(val tarea: Tarea) : PantallaTec
    data class SeleccionTablero(val tarea: Tarea) : PantallaTec
    data class FlujoTablero(val tarea: Tarea, val tipo: String) : PantallaTec
    data class EnviarInforme(val tarea: Tarea, val tipo: String) : PantallaTec
    data class Confirmacion(val tarea: Tarea) : PantallaTec
}

private val PantallaTecSaver = listSaver<PantallaTec, String>(
    save = { pantalla ->
        when (pantalla) {
            is PantallaTec.Lista -> listOf("Lista")
            is PantallaTec.Detalle -> listOf("Detalle", pantalla.tarea.id.toString())
            is PantallaTec.Ingresar -> listOf("Ingresar", pantalla.tarea.id.toString())
            is PantallaTec.SeleccionTablero -> listOf("SeleccionTablero", pantalla.tarea.id.toString())
            is PantallaTec.FlujoTablero -> listOf("FlujoTablero", pantalla.tarea.id.toString(), pantalla.tipo)
            is PantallaTec.EnviarInforme -> listOf("EnviarInforme", pantalla.tarea.id.toString(), pantalla.tipo)
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
            "FlujoTablero" -> tarea?.let { PantallaTec.FlujoTablero(it, lista.getOrNull(2) ?: "") }
            "EnviarInforme" -> tarea?.let { PantallaTec.EnviarInforme(it, lista.getOrNull(2) ?: "") }
            "Confirmacion" -> tarea?.let { PantallaTec.Confirmacion(it) }
            else -> null
        } ?: PantallaTec.Lista
    }
)

@Composable
fun MainScreenTecnico(tecnico: Tecnico, onCerrarSesion: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var refrescando by remember { mutableStateOf(false) }
    var notifAbierto by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    var pantalla by rememberSaveable(stateSaver = PantallaTecSaver) {
        mutableStateOf<PantallaTec>(PantallaTec.Lista)
    }
    var confirmarFinalizar by rememberSaveable { mutableStateOf(false) }
    var tareaFinalizarId by rememberSaveable { mutableIntStateOf(0) }
    var errorFinalizar by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val noLeidas = Repositorio.noLeidasTecnico(tecnico.id)

    LaunchedEffect(Unit) {
        refrescando = true
        runCatching { Repositorio.cargarTecnico(tecnico.id) }
        refrescando = false
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
        val ocultarTopBar = pantalla is PantallaTec.FlujoTablero ||
            pantalla is PantallaTec.EnviarInforme ||
            pantalla is PantallaTec.Confirmacion
        if (!ocultarTopBar) {
            TopBar(
                iniciales = inicialesDe(tecnico.nombre, tecnico.apellido),
                nombre = tecnico.nombre,
                apellido = tecnico.apellido,
                refrescando = refrescando,
                noLeidas = noLeidas,
                onBack = if (pantalla is PantallaTec.Lista) {
                    null
                } else {
                    { volver() }
                },
                onRefresh = {
                    scope.launch {
                        refrescando = true
                        runCatching { Repositorio.cargarTecnico(tecnico.id) }
                        refrescando = false
                    }
                },
                onAbrirNotificaciones = { notifAbierto = true },
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
                    if (tab == 2) {
                        DocumentosTecnicoScreen(tecnico = tecnico)
                    } else {
                        TareasTecnicoScreen(
                            tecnico = tecnico,
                            titulo = if (tab == 0) "Inicio" else "Tareas",
                            subtitulo = if (tab == 0) {
                                "Tus tareas asignadas y su estado."
                            } else {
                                "Entra a una tarea para iniciarla o retomarla."
                            },
                            onAbrirTarea = { pantalla = PantallaTec.Detalle(it) }
                        )
                    }
                }
                is PantallaTec.Detalle -> {
                    val tarea = tareaActual(p.tarea)
                    DetalleTareaTecnico(
                        tarea = tarea,
                        onIngresar = {
                            pantalla = if (tarea.esInvitado(tecnico.id)) {
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
                        puedeFinalizar = !tarea.esInvitado(tecnico.id),
                        onSeleccionarTipo = { pantalla = PantallaTec.FlujoTablero(p.tarea, it) },
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
                    onFinalizado = { pantalla = PantallaTec.EnviarInforme(p.tarea, p.tipo) },
                    onVolver = { pantalla = PantallaTec.SeleccionTablero(p.tarea) }
                )
                is PantallaTec.EnviarInforme -> EnviarInformeScreen(
                    tarea = tareaActual(p.tarea),
                    tipo = p.tipo,
                    tecnico = tecnico,
                    onGuardado = {
                        scope.launch { runCatching { Repositorio.cargarTecnico(tecnico.id) } }
                        TableroDraftStore.limpiar(p.tarea.id, p.tipo)
                        pantalla = PantallaTec.SeleccionTablero(p.tarea)
                    },
                    onVolver = { pantalla = PantallaTec.SeleccionTablero(p.tarea) }
                )
                is PantallaTec.Confirmacion -> TableroConfirmacion(
                    tarea = tareaActual(p.tarea),
                    onIrAlInicio = {
                        tab = 0
                        volver()
                    }
                )
            }
        }

        if (!ocultarTopBar) {
            BarraNavegacion(
                items = ITEMS_TECNICO,
                actual = tab,
                onSeleccionar = {
                    tab = it
                    volver()
                }
            )
        }
    }

    if (notifAbierto) {
        HojaNotificacionesTecnico(
            tecnicoId = tecnico.id,
            onCerrar = { notifAbierto = false },
            onAbrirTarea = { id ->
                val tarea = Repositorio.tareas.firstOrNull { it.id == id }
                if (tarea != null) {
                    notifAbierto = false
                    pantalla = PantallaTec.Detalle(tarea)
                }
            }
        )
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
            mensaje = "¿Realmente desea finalizar la tarea? Se enviarán los tableros pendientes.",
            confirmarTexto = "Sí",
            cancelarTexto = "No",
            onConfirmar = {
                confirmarFinalizar = false
                errorFinalizar = ""
                scope.launch {
                    val error = enviarTablerosPendientes(tareaFinalizarId, tecnico)
                    val tarea = Repositorio.tareas.firstOrNull { it.id == tareaFinalizarId }
                    if (error != null) {
                        errorFinalizar = error
                        pantalla = if (tarea != null) {
                            PantallaTec.SeleccionTablero(tarea)
                        } else {
                            PantallaTec.Lista
                        }
                    } else {
                        Repositorio.cambiarEstadoTarea(tareaFinalizarId, "Finalizada")
                        TableroDraftStore.limpiarTarea(tareaFinalizarId)
                        val tareaFinal = Repositorio.tareas.firstOrNull { it.id == tareaFinalizarId }
                        pantalla = if (tareaFinal != null) {
                            PantallaTec.Confirmacion(tareaFinal)
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

private suspend fun enviarTablerosPendientes(tareaId: Int, tecnico: Tecnico): String? {
    val trifasico = TableroDraftStore.cargarTrifasico(tareaId)
    val monofasico = TableroDraftStore.cargarMonofasico(tareaId)
    return try {
        if (trifasico != null || monofasico != null) {
            val token = ApiService.loginTecnico(tecnico.rut, tecnico.password)
            ApiService.token = token
            if (trifasico != null) {
                ApiService.enviarInforme(
                    token,
                    tareaId.toLong(),
                    TableroRepository.buildTrifasico(tareaId, tecnico.id, trifasico)
                )
            }
            if (monofasico != null) {
                ApiService.enviarInforme(
                    token,
                    tareaId.toLong(),
                    TableroRepository.buildMonofasico(tareaId, tecnico.id, monofasico)
                )
            }
            null
        } else {
            val tableros = ApiService.tablerosTarea(ApiService.token ?: "", tareaId)
            if (tableros.length() == 0) {
                "Esta tarea no tiene tableros inspeccionados para generar el informe. " +
                    "Envíe al menos un tablero antes de finalizar."
            } else {
                null
            }
        }
    } catch (e: Exception) {
        (e as? ApiException)?.message ?: e.message ?: "No se pudo enviar el informe pendiente."
    }
}
