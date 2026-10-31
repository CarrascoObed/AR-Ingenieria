package com.aring.movil.ui.tecnico

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.OfflineSync
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Tarea
import com.aring.movil.data.Tecnico
import com.aring.movil.ui.components.ChipEstado
import com.aring.movil.ui.components.ChipTipo
import com.aring.movil.ui.components.Tarjeta
import com.aring.movil.ui.components.Vacio
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.Violeta

@Composable
fun TareasTecnicoScreen(
    tecnico: Tecnico,
    onAbrirTarea: (Tarea) -> Unit,
) {
    val tareas = Repositorio.tareasDeTecnico(tecnico.id)

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
            "Entra a una tarea para iniciarla o retomarla.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextoGris,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))

        val hayPendientes = runCatching { OfflineSync.hayPendientes() }.getOrDefault(false)
        val ultimoError = runCatching { OfflineSync.ultimoError() }.getOrDefault("")
        if (hayPendientes) {
            val detalle = if (ultimoError.isNotBlank()) {
                val pistaCredenciales =
                    ultimoError.indexOf("contrase", 0, ignoreCase = true) >= 0 ||
                        ultimoError.indexOf("Usuario o", 0, ignoreCase = true) >= 0
                if (pistaCredenciales)
                    "$ultimoError · Cierre sesión y vuelva a entrar para actualizar sus credenciales."
                else
                    "Último error: $ultimoError"
            } else {
                ""
            }
            Text(
                "Pendiente de sincronizar · se enviará al recuperar conexión" +
                    if (detalle.isNotBlank()) "\n$detalle" else "",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(8.dp))

        if (tareas.isEmpty()) {
            Vacio("No tienes tareas asignadas.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tareas, key = { it.id }) { t ->
                    Tarjeta(
                        Modifier.clickable { onAbrirTarea(t) }
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                t.codigo,
                                color = Violeta,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                if (t.esInvitado(tecnico.id)) {
                                    ChipTipo("Invitado")
                                }
                                ChipEstado(t.estado)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (t.sincronizacionPendiente) {
                            Text("Pendiente de sincronizar · se enviará al recuperar conexión", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                        }
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
                    }
                }
            }
        }
    }
}
