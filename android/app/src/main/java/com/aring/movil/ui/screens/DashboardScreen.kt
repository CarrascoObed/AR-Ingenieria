package com.aring.movil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.Admin
import com.aring.movil.data.DashboardStats
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Tecnico
import com.aring.movil.data.inicialesDe
import com.aring.movil.ui.components.Avatar
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.Cargando
import com.aring.movil.ui.components.ChipLegenda
import com.aring.movil.ui.components.Tarjeta
import com.aring.movil.ui.components.Vacio
import com.aring.movil.ui.theme.Amarillo
import com.aring.movil.ui.theme.FondoGris
import com.aring.movil.ui.theme.RojoError
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.VerdeOk

@Composable
fun DashboardScreen(admin: Admin, clave: Int) {
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var tecnicos by remember { mutableStateOf<List<Tecnico>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(clave) {
        cargando = true
        error = ""
        try {
            stats = Repositorio.stats()
            tecnicos = Repositorio.tecnicos.toList()
        } catch (e: Exception) {
            error = e.message ?: "Ocurrió un error inesperado."
        } finally {
            cargando = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            "Dashboard",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Resumen general del sistema de tareas.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextoGris,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))

        BannerError(error)

        if (cargando) {
            Cargando("Cargando dashboard…")
        } else {
            val datos = listOf(
                Triple("En proceso", stats?.enProceso ?: 0, Amarillo),
                Triple("Finalizadas", stats?.finalizadas ?: 0, VerdeOk),
                Triple("No iniciadas", stats?.noIniciadas ?: 0, RojoError),
            )

            Tarjeta {
                Text(
                    "Estado de las tareas",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextoOscuro
                )
                Spacer(Modifier.height(16.dp))
                GraficoBarras(datos)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    datos.forEach { (nombre, valor, color) ->
                        ChipLegenda(color, valor.toString())
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Tarjeta {
                Text(
                    "Técnicos del sistema",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextoOscuro
                )
                Spacer(Modifier.height(16.dp))
                if (tecnicos.isEmpty()) {
                    Vacio("Aún no hay técnicos registrados.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        tecnicos.forEach { t ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(FondoGris)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Avatar(inicialesDe(t.nombre, t.apellido), 38.dp, 15.sp)
                                Column {
                                    Text(
                                        "${t.nombre} ${t.apellido}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextoOscuro
                                    )
                                    Text(
                                        "RUT ${t.rut}",
                                        color = TextoGris,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraficoBarras(datos: List<Triple<String, Int, Color>>) {
    val max = datos.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Row(
        Modifier
            .fillMaxWidth()
            .height(190.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        datos.forEach { (nombre, valor, color) ->
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    valor.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextoOscuro
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    val fraccion = valor.toFloat() / max.toFloat()
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(fraccion)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                .background(color)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        nombre,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        color = TextoGris,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
