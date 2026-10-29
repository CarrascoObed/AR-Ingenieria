package com.aring.movil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.Repositorio
import com.aring.movil.ui.theme.Blanco
import com.aring.movil.ui.theme.FondoGris
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.Violeta
import com.aring.movil.ui.theme.VioletaClaro
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaNotificaciones(onCerrar: () -> Unit) {
    val scope = rememberCoroutineScope()
    val notifs = Repositorio.notificaciones
    val noLeidas = notifs.count { !it.leida }

    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notificaciones",
                    color = TextoOscuro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (notifs.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (noLeidas > 0) {
                            BtnSecundario(
                                "Leer todas",
                                chico = true
                            ) {
                                scope.launch { Repositorio.marcarTodasLeidas() }
                            }
                        }
                        BtnRiesgo(
                            "Limpiar",
                            chico = true,
                            icono = Icons.Filled.Delete
                        ) {
                            scope.launch {
                                Repositorio.limpiarNotificaciones()
                                onCerrar()
                            }
                        }
                    }
                }
            }

            if (notifs.isEmpty()) {
                Vacio("No tienes notificaciones.")
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(count = notifs.size, key = { notifs[it].id }) { i ->
                        val n = notifs[i]
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!n.leida) scope.launch { Repositorio.marcarLeida(n.id) }
                                }
                                .background(if (n.leida) Blanco else VioletaClaro)
                                .padding(14.dp)
                        ) {
                            Text(n.mensaje, color = TextoOscuro, fontSize = 13.5.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                Repositorio.formatearFecha(n.fecha),
                                color = TextoGris,
                                fontSize = 12.sp
                            )
                        }
                        HorizontalDivider(color = FondoGris)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaNotificacionesTecnico(
    tecnicoId: Int,
    onCerrar: () -> Unit,
    onAbrirTarea: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val notifs = Repositorio.notificacionesDeTecnico(tecnicoId)
    val tareas = Repositorio.tareasDeTecnico(tecnicoId)
    val hayNoLeidas = notifs.any { !it.leida }

    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(Modifier.fillMaxWidth()) {
            if (notifs.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Notificaciones",
                        color = TextoOscuro,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hayNoLeidas) {
                            BtnSecundario(
                                "Leer todas",
                                chico = true
                            ) {
                                scope.launch { Repositorio.marcarTodasLeidasTecnico(tecnicoId) }
                            }
                        }
                        BtnRiesgo(
                            "Limpiar",
                            chico = true,
                            icono = Icons.Filled.Delete
                        ) {
                            scope.launch {
                                Repositorio.limpiarNotificacionesTecnico(tecnicoId)
                                onCerrar()
                            }
                        }
                    }
                }
            }

            if (notifs.isEmpty() && tareas.isEmpty()) {
                Vacio("No tienes notificaciones.")
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    if (notifs.isNotEmpty()) {
                        items(count = notifs.size, key = { "n${notifs[it].id}" }) { i ->
                            val n = notifs[i]
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!n.leida) scope.launch { Repositorio.marcarLeida(n.id) }
                                        n.tareaId?.let(onAbrirTarea)
                                    }
                                    .background(if (n.leida) Blanco else VioletaClaro)
                                    .padding(14.dp)
                            ) {
                                Text(n.mensaje, color = TextoOscuro, fontSize = 13.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    Repositorio.formatearFecha(n.fecha),
                                    color = TextoGris,
                                    fontSize = 12.sp
                                )
                            }
                            HorizontalDivider(color = FondoGris)
                        }
                    }
                    if (tareas.isNotEmpty()) {
                        item(key = "titulo-tareas") {
                            Text(
                                "Tareas asignadas",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                color = TextoOscuro,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        items(count = tareas.size, key = { "t${tareas[it].id}" }) { i ->
                            val t = tareas[i]
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onAbrirTarea(t.id) }
                                    .background(Blanco)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    t.codigo,
                                    color = Violeta,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(t.nombreRecinto, color = TextoGris, fontSize = 12.sp)
                            }
                            HorizontalDivider(color = FondoGris)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
