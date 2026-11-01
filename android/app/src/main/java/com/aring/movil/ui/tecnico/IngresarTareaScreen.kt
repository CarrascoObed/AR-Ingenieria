package com.aring.movil.ui.tecnico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.ApiException
import com.aring.movil.data.OfflineSync
import com.aring.movil.data.OfflineTaskCache
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Tarea
import com.aring.movil.data.Tecnico
import com.aring.movil.data.inicialesDe
import com.aring.movil.ui.components.Avatar
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.BannerOk
import com.aring.movil.ui.components.BtnPrimario
import com.aring.movil.ui.components.BtnSecundario
import com.aring.movil.ui.components.CampoSelect
import com.aring.movil.ui.components.HorizontalSeparador
import com.aring.movil.ui.components.SeccionFormulario
import com.aring.movil.ui.components.Tarjeta
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.VioletaOscuro
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

@Composable
fun IngresarTareaScreen(
    tecnico: Tecnico,
    tarea: Tarea,
    onContinuar: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val retomando = tarea.estado == "En proceso"
    val ayudantes = remember { mutableStateListOf("") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    // Inicio encolado sin conexión: se envía solo cuando vuelva internet.
    var inicioPendiente by remember { mutableStateOf(false) }

    fun encolarInicioLocal(ids: List<Int>) {
        OfflineSync.guardarInicio(tarea.id, tecnico, ids)
        val indice = Repositorio.tareas.indexOfFirst { it.id == tarea.id }
        if (indice >= 0) {
            Repositorio.tareas[indice] = tarea.copy(
                estado = "En proceso",
                sincronizacionPendiente = true,
                fechaInicio = OffsetDateTime.now().toString(),
                tecnicoAyudantesIds = ids
            )
        }
        OfflineTaskCache.save(tecnico.id, Repositorio.tareas.toList())
    }

    val opcionesPara = { indice: Int ->
        val tomados = ayudantes
            .mapIndexedNotNull { i, v -> if (i != indice && v.isNotBlank()) v else null }
        Repositorio.tecnicos
            .filter { it.id != tecnico.id && !tomados.contains(it.id.toString()) }
            .map { it.id.toString() to "${it.nombre} ${it.apellido}" }
    }
    val invitadosGuardados = tarea.tecnicoAyudantesIds.mapNotNull { id ->
        Repositorio.tecnicos.firstOrNull { it.id == id }
    }
    val seleccionados = ayudantes.count { it.isNotBlank() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            if (retomando) "Retomar tarea" else "Ingresar a la tarea",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tarea.codigo,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = VioletaOscuro,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(20.dp))

        Tarjeta {
            Text("Técnico de cabecera", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoGris)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Avatar(inicialesDe(tecnico.nombre, tecnico.apellido), 32.dp, 13.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "${tecnico.nombre} ${tecnico.apellido}",
                    color = TextoOscuro,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (retomando) {
            Tarjeta {
                Text("Tarea iniciada", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoGris)
                Spacer(Modifier.height(2.dp))
                Text(
                    Repositorio.formatearFecha(tarea.fechaInicio ?: ""),
                    color = TextoOscuro,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(12.dp))
                HorizontalSeparador()
                Spacer(Modifier.height(12.dp))
                Text("Técnicos invitados", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoGris)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (invitadosGuardados.isEmpty()) {
                        "Sin invitados"
                    } else {
                        invitadosGuardados.joinToString("\n") { "${it.nombre} ${it.apellido}" }
                    },
                    color = TextoOscuro,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            BannerOk("Retomarás la tarea desde el último punto guardado.")

            Spacer(Modifier.height(14.dp))

            BtnPrimario(
                texto = "Continuar con tableros",
                modifier = Modifier.fillMaxWidth(),
                icono = Icons.Filled.PlayArrow,
                onClick = onContinuar
            )
        } else {
            SeccionFormulario("Técnicos invitados (opcional)") {
                ayudantes.forEachIndexed { indice, _ ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            CampoSelect(
                                label = "Técnico invitado ${indice + 1}",
                                opciones = opcionesPara(indice),
                                valor = ayudantes[indice],
                                onSeleccionar = { ayudantes[indice] = it }
                            )
                        }
                        IconButton(onClick = { ayudantes.removeAt(indice) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Quitar invitado",
                                tint = TextoGris
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (ayudantes.size < 5) {
                    BtnSecundario(
                        texto = "Agregar otro técnico invitado",
                        modifier = Modifier.fillMaxWidth(),
                        icono = Icons.Filled.Add
                    ) {
                        ayudantes.add("")
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    "Invitaciones: $seleccionados/5",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TextoGris,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            if (error.isNotBlank()) {
                BannerError(error)
                Spacer(Modifier.height(12.dp))
            }

            if (inicioPendiente) {
                BannerOk(
                    "La sincronización está pendiente · la tarea y las invitaciones se enviarán al recuperar conexión."
                )
                Spacer(Modifier.height(12.dp))
                BtnPrimario(
                    texto = "Continuar con tableros (sin conexión)",
                    modifier = Modifier.fillMaxWidth(),
                    icono = Icons.Filled.PlayArrow,
                    onClick = onContinuar
                )
            } else {
                BtnPrimario(
                    texto = "Iniciar tarea",
                    modifier = Modifier.fillMaxWidth(),
                    cargando = cargando,
                    icono = Icons.Filled.PlayArrow
                ) {
                    scope.launch {
                        cargando = true
                        error = ""
                        val ids = ayudantes.mapNotNull { it.ifBlank { null }?.toInt() }
                        try {
                            Repositorio.iniciarTarea(
                                id = tarea.id,
                                ayudantesIds = ids,
                            )
                            cargando = false
                            onContinuar()
                        } catch (e: ApiException) {
                            cargando = false
                            if (e.codigo == null) {
                                // Fallo de red: la tarea queda iniciada en el dispositivo y el
                                // outbox la envía (con sus invitaciones) al recuperar conexión.
                                encolarInicioLocal(ids)
                                inicioPendiente = true
                            } else {
                                error = e.message ?: "No se pudo iniciar la tarea."
                            }
                        } catch (e: Exception) {
                            cargando = false
                            error = e.message ?: "No se pudo iniciar la tarea."
                        }
                    }
                }
            }
        }
    }
}
