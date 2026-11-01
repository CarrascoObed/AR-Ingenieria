package com.aring.movil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.ApiException
import com.aring.movil.data.Cliente
import com.aring.movil.data.Prefs
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Tarea
import com.aring.movil.data.inicialesDe
import com.aring.movil.ui.components.Avatar
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.BannerOk
import com.aring.movil.ui.components.BtnPrimario
import com.aring.movil.ui.components.BtnRiesgo
import com.aring.movil.ui.components.BtnSecundario
import com.aring.movil.ui.components.CampoSelect
import com.aring.movil.ui.components.CampoTexto
import com.aring.movil.ui.components.ChipEstado
import com.aring.movil.ui.components.ChipTipo
import com.aring.movil.ui.components.ConfirmarDialog
import com.aring.movil.ui.components.HorizontalSeparador
import com.aring.movil.ui.components.ModalDialog
import com.aring.movil.ui.components.ModalPie
import com.aring.movil.ui.components.SeccionFormulario
import com.aring.movil.ui.components.Tarjeta
import com.aring.movil.ui.components.Vacio
import com.aring.movil.ui.theme.FondoGris
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.Violeta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TIPOS = listOf(
    "MANTENIMIENTO" to "Mantenimiento",
    "REPARACION" to "Reparación (próximamente)",
    "COTIZACION" to "Cotización (próximamente)",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TareasScreen(clave: Int) {
    val scope = rememberCoroutineScope()
    val tareas = Repositorio.tareas
    val tecnicos = Repositorio.tecnicos

    var error by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }

    var crearAbierto by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Tarea?>(null) }
    var eliminando by remember { mutableStateOf<Tarea?>(null) }
    var limpiarAbierto by remember { mutableStateOf(false) }
    val ocultas = remember {
        mutableStateOf(Prefs.leerOcultasFinalizadas().mapNotNull { it.toIntOrNull() }.toSet())
    }
    val tareasVisibles = tareas.filter { it.id !in ocultas.value }

    var fDireccion by remember { mutableStateOf("") }
    var fRecinto by remember { mutableStateOf("") }
    var fDescripcion by remember { mutableStateOf("") }
    var fTecnicoId by remember { mutableStateOf("") }
    var fCliente by remember { mutableStateOf<Cliente?>(null) }
    var sugerenciasAbiertas by remember { mutableStateOf(false) }

    var eTecnicoId by remember { mutableStateOf("") }

    fun mostrarAviso(texto: String) {
        aviso = texto
        scope.launch {
            delay(4000)
            aviso = ""
        }
    }

    fun limpiarFinalizadas() {
        val ids = tareasVisibles.filter { it.estado == "Finalizada" }.map { it.id }
        ocultas.value = ocultas.value + ids
        Prefs.guardarOcultasFinalizadas(ocultas.value.toList())
        limpiarAbierto = false
        mostrarAviso("Las tareas finalizadas fueron ocultadas de la lista.")
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            "Tareas",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Crea y administra las tareas asignadas a los técnicos.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextoGris,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))

        BannerError(error)
        BannerOk(aviso)
        Spacer(Modifier.height(4.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BtnPrimario(
                texto = "Crear tarea",
                icono = Icons.Filled.Add
            ) {
                error = ""
                fDireccion = ""
                fRecinto = ""
                fDescripcion = ""
                fTecnicoId = ""
                fCliente = null
                sugerenciasAbiertas = true
                crearAbierto = true
            }
            if (tareasVisibles.any { it.estado == "Finalizada" }) {
                BtnSecundario(
                    texto = "Limpiar finalizadas",
                    icono = Icons.Filled.Delete
                ) {
                    error = ""
                    limpiarAbierto = true
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (tareasVisibles.isEmpty()) {
            Vacio("Aún no hay tareas creadas.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tareasVisibles, key = { it.id }) { t ->
                    val tecnico = tecnicos.find { it.id == t.tecnicoCabeceraId }
                    Tarjeta {
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                t.codigo,
                                color = Violeta,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            ChipTipo(t.tipo)
                            ChipEstado(t.estado)
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            t.nombreRecinto,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextoOscuro
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            t.direccion,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = TextoGris,
                            fontSize = 13.5.sp
                        )
                        Text(
                            t.descripcion,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = TextoGris,
                            fontSize = 13.5.sp
                        )

                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(FondoGris)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Avatar(
                                    inicialesDe(tecnico?.nombre ?: "", tecnico?.apellido ?: ""),
                                    26.dp,
                                    11.sp
                                )
                                Text(
                                    "Técnico de cabecera: ${tecnico?.nombre ?: ""} ${tecnico?.apellido ?: ""}",
                                    fontSize = 13.5.sp,
                                    color = TextoOscuro
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalSeparador()
                        Spacer(Modifier.height(12.dp))

                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (t.estado) {
                                "No iniciada" -> {
                                    BtnSecundario(
                                        "Editar",
                                        chico = true,
                                        icono = Icons.Filled.Edit
                                    ) {
                                        error = ""
                                        editando = t
                                        eTecnicoId = t.tecnicoCabeceraId.toString()
                                    }
                                    BtnRiesgo(
                                        "Eliminar",
                                        chico = true,
                                        icono = Icons.Filled.Delete
                                    ) {
                                        error = ""
                                        eliminando = t
                                    }
                                }
                                "En proceso" -> {
                                    BtnPrimario(
                                        "Finalizar (demo)",
                                        chico = true,
                                        icono = Icons.Filled.Check
                                    ) {
                                        scope.launch {
                                            Repositorio.cambiarEstadoTarea(t.id, "Finalizada")
                                            mostrarAviso(
                                                "La tarea ${t.codigo} fue finalizada. " +
                                                    "Se envió una notificación."
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    ChipEstado("Finalizada")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (crearAbierto) {
        ModalDialog("Crear tarea", onCerrar = { crearAbierto = false }) {
            SeccionFormulario("Datos de la tarea") {
                CampoSelect("Tipo de tarea", TIPOS, "MANTENIMIENTO") { }
                Spacer(Modifier.height(10.dp))
                val sugerenciasDireccion = Repositorio.sugerenciasClientes(fDireccion)
                CampoTexto(
                    "Dirección",
                    fDireccion,
                    { nuevo ->
                        fDireccion = nuevo
                        if (fCliente != null && nuevo != fCliente!!.direccion) fCliente = null
                        sugerenciasAbiertas = true
                    },
                    "Ej: Av. Vicuña Mackena 3001. Ñuñoa."
                )
                if (sugerenciasAbiertas && fCliente?.direccion != fDireccion && sugerenciasDireccion.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        sugerenciasDireccion.forEach { c ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        fCliente = c
                                        fDireccion = c.direccion
                                        fRecinto = c.nombreRecinto
                                        sugerenciasAbiertas = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(c.direccion, color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text(c.nombreRecinto, color = TextoGris, fontSize = 12.sp)
                            }
                            HorizontalSeparador()
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        fCliente != null -> "Cliente seleccionado: ${fCliente!!.direccion} · ${fCliente!!.nombreRecinto}. Puedes editar el nombre del recinto."
                        fDireccion.isNotBlank() && Repositorio.clientes.isNotEmpty() ->
                            "Ningún cliente coincide con la dirección; la tarea se creará sin cliente asociado."
                        else -> "Toca la dirección para ver los clientes registrados; al elegir uno se completa el recinto."
                    },
                    color = if (fCliente != null) Violeta else TextoGris,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                CampoTexto("Nombre del recinto", fRecinto, { fRecinto = it }, "Ej: Edificio Central")
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    "Descripción de la tarea",
                    fDescripcion,
                    { fDescripcion = it },
                    "Describa el detalle de la tarea a realizar",
                    multiline = true
                )
                Spacer(Modifier.height(10.dp))
                val opcionesTecnicos = tecnicos.map { it.id.toString() to "${it.nombre} ${it.apellido}" }
                CampoSelect("Técnico de cabecera", opcionesTecnicos, fTecnicoId) { fTecnicoId = it }
            }
            Spacer(Modifier.height(12.dp))
            val completo = fDireccion.isNotBlank() && fRecinto.isNotBlank() &&
                fDescripcion.isNotBlank() && fTecnicoId.isNotBlank()
            ModalPie(
                izquierda = { BtnSecundario("Cancelar") { crearAbierto = false } },
                derecha = {
                    BtnPrimario(
                        "Crear tarea",
                        enabled = completo,
                        icono = Icons.Filled.Add
                    ) {
                        scope.launch {
                            val res = Repositorio.crearTarea(
                                tipo = "MANTENIMIENTO",
                                direccion = fDireccion,
                                nombreRecinto = fRecinto,
                                descripcion = fDescripcion,
                                tecnicoCabeceraId = fTecnicoId.toInt(),
                                clienteId = fCliente?.id,
                            )
                            crearAbierto = false
                            mostrarAviso("Tarea creada correctamente.")
                        }
                    }
                }
            )
        }
    }

    editando?.let { t ->
        ModalDialog("Editar tarea", onCerrar = { editando = null }) {
            Text(
                "Solo se puede modificar el técnico de cabecera de la tarea ${t.codigo}.",
                color = TextoGris,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            SeccionFormulario("Técnico de cabecera") {
                val opcionesTecnicos = tecnicos.map { it.id.toString() to "${it.nombre} ${it.apellido}" }
                CampoSelect("Técnico de cabecera", opcionesTecnicos, eTecnicoId) { eTecnicoId = it }
            }
            Spacer(Modifier.height(12.dp))
            ModalPie(
                izquierda = { BtnSecundario("Cancelar") { editando = null } },
                derecha = {
                    BtnPrimario(
                        "Guardar cambios",
                        enabled = eTecnicoId.isNotBlank()
                    ) {
                        scope.launch {
                            Repositorio.actualizarCabeceraTarea(t.id, eTecnicoId.toInt())
                            editando = null
                            mostrarAviso("Técnico de la tarea ${t.codigo} actualizado.")
                        }
                    }
                }
            )
        }
    }

    eliminando?.let { t ->
        ConfirmarDialog(
            titulo = "Eliminar tarea",
            mensaje = "¿Seguro que deseas eliminar la tarea ${t.codigo}? " +
                "Esta acción no se puede deshacer.",
            onConfirmar = {
                scope.launch {
                    try {
                        Repositorio.eliminarTarea(t.id)
                        eliminando = null
                        mostrarAviso("Tarea ${t.codigo} eliminada.")
                    } catch (e: ApiException) {
                        eliminando = null
                        error = e.message ?: "No se pudo eliminar la tarea."
                    } catch (e: Exception) {
                        eliminando = null
                        error = "No se pudo eliminar la tarea."
                    }
                }
            },
            onCancelar = { eliminando = null }
        )
    }

    if (limpiarAbierto) {
        ConfirmarDialog(
            titulo = "Limpiar tareas finalizadas",
            mensaje = "¿Está seguro de limpiar la lista de tareas finalizadas? " +
                "Solo se ocultarán de la lista; no se eliminará información.",
            confirmarTexto = "Limpiar",
            onConfirmar = { limpiarFinalizadas() },
            onCancelar = { limpiarAbierto = false }
        )
    }
}
