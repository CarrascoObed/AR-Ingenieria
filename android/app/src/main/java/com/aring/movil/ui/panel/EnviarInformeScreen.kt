package com.aring.movil.ui.panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aring.movil.data.Tarea
import com.aring.movil.data.Tecnico
import com.aring.movil.data.panel.MonofasicoState
import com.aring.movil.data.panel.TYPE_MONOFASICO
import com.aring.movil.data.panel.TYPE_TRIFASICO
import com.aring.movil.data.panel.TableroDraftStore
import com.aring.movil.data.panel.TrifasicoState
import com.aring.movil.ui.theme.FondoGris
import com.aring.movil.ui.theme.RojoError
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.VerdeOk
import com.aring.movil.ui.theme.Violeta
import com.aring.movil.ui.theme.VioletaClaro

@Composable
fun EnviarInformeScreen(
    tarea: Tarea,
    tipo: String,
    tecnico: Tecnico,
    onGuardado: () -> Unit,
    onVolver: () -> Unit
) {
    val vm: RegistroTableroViewModel = viewModel()
    val estado = vm.estado

    val estadoTrifasico = remember(tarea.id, tipo) {
        if (tipo == TYPE_TRIFASICO) {
            TableroDraftStore.cargarTrifasico(tarea.id) ?: TrifasicoState()
        } else {
            null
        }
    }
    val estadoMonofasico = remember(tarea.id, tipo) {
        if (tipo == TYPE_MONOFASICO) {
            TableroDraftStore.cargarMonofasico(tarea.id) ?: MonofasicoState()
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        if (vm.estado is EstadoEnvio.Enviando) return@LaunchedEffect
        vm.reiniciar()
        when (tipo) {
            TYPE_TRIFASICO -> estadoTrifasico?.let { vm.enviarTrifasico(tarea.id, tecnico, it) }
            TYPE_MONOFASICO -> estadoMonofasico?.let { vm.enviarMonofasico(tarea.id, tecnico, it) }
        }
    }

    LaunchedEffect(estado) {
        if (estado is EstadoEnvio.Exito) onGuardado()
    }

    BackHandler(enabled = estado !is EstadoEnvio.Enviando, onBack = onVolver)

    Column(
        Modifier
            .fillMaxSize()
            .background(FondoGris)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(VioletaClaro),
            contentAlignment = Alignment.Center
        ) {
            when (estado) {
                is EstadoEnvio.Inactivo, is EstadoEnvio.Enviando -> CircularProgressIndicator(
                    color = Violeta,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
                is EstadoEnvio.Exito -> Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = VerdeOk,
                    modifier = Modifier.size(56.dp)
                )
                is EstadoEnvio.Error -> Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = RojoError,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (estado is EstadoEnvio.Exito) "Informe guardado en el teléfono" else "Preparando informe",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 21.sp,
            color = TextoOscuro
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tarea ${tarea.codigo} · ${tipoNombre(tipo)}",
            color = TextoGris,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))

        when (estado) {
            is EstadoEnvio.Inactivo -> {
                Text(
                    "Preparando el informe para enviarlo al servidor...",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TextoGris,
                    fontSize = 14.sp
                )
            }
            is EstadoEnvio.Enviando -> {
                Text(
                    "Subiendo mediciones, checklist, evidencias y observaciones. No cierres la aplicación.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TextoGris,
                    fontSize = 14.sp
                )
            }
            is EstadoEnvio.Exito -> {
                Text(
                    "El informe quedó guardado en este dispositivo. Se enviará al servidor automáticamente cuando vuelva la conexión.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TextoGris,
                    fontSize = 14.sp
                )
            }
            is EstadoEnvio.Error -> {
                Text(
                    estado.mensaje,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RojoError.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    textAlign = TextAlign.Center,
                    color = RojoError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (estado is EstadoEnvio.Error) {
            Spacer(Modifier.height(32.dp))
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.material3.Button(
                    onClick = {
                        when (tipo) {
                            TYPE_TRIFASICO -> estadoTrifasico?.let {
                                vm.enviarTrifasico(tarea.id, tecnico, it)
                            }
                            TYPE_MONOFASICO -> estadoMonofasico?.let {
                                vm.enviarMonofasico(tarea.id, tecnico, it)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Violeta,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Reintentar envío", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onVolver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Volver sin enviar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun tipoNombre(tipo: String): String = when (tipo) {
    TYPE_TRIFASICO -> "Trifásico"
    TYPE_MONOFASICO -> "Monofásico"
    else -> tipo
}
