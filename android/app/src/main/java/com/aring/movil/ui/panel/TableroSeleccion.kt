package com.aring.movil.ui.panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.Tarea
import com.aring.movil.data.panel.TableroDraftStore
import com.aring.movil.data.panel.TYPE_MONOFASICO
import com.aring.movil.data.panel.TYPE_TRIFASICO
import com.aring.movil.ui.components.BtnPrimario
import com.aring.movil.ui.components.Tarjeta
import com.aring.movil.ui.theme.Blanco
import com.aring.movil.ui.theme.BordeSuave
import com.aring.movil.ui.theme.FondoGris
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.VerdeOk
import com.aring.movil.ui.theme.Violeta
import com.aring.movil.ui.theme.VioletaClaro
import com.aring.movil.ui.theme.VioletaOscuro

@Composable
private fun TarjetaTipoTablero(
    titulo: String,
    subtitulo: String,
    detalles: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Blanco)
            .border(1.dp, BordeSuave, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(VioletaClaro),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = Violeta, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextoOscuro)
                Text(subtitulo, fontSize = 13.sp, color = TextoGris)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            detalles,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13.sp,
            color = VioletaOscuro,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TableroSeleccion(
    tarea: Tarea,
    borradores: List<TableroDraftStore.Borrador> = emptyList(),
    puedeFinalizar: Boolean = true,
    onSeleccionarTipo: (String) -> Unit,
    onEditar: (TableroDraftStore.Borrador) -> Unit = {},
    onFinalizar: () -> Unit,
    onVolver: () -> Unit
) {
    BackHandler(onBack = onVolver)
    var mostrarIncompletos by rememberSaveable(tarea.id) { mutableStateOf(false) }
    val completados = borradores.filter { it.terminado }
    val incompletos = borradores.filterNot { it.terminado }

    Column(
        Modifier
            .fillMaxSize()
            .background(FondoGris)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            "Tableros de la tarea",
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
        Spacer(Modifier.height(8.dp))
        Text(
            "Seleccione el tipo de tablero a inspeccionar.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextoGris,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))

        if (completados.isNotEmpty() || incompletos.isNotEmpty()) {
            if (completados.isNotEmpty()) {
                Text("Revisar tableros guardados", fontWeight = FontWeight.Bold, color = TextoOscuro, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
            }
            completados.forEachIndexed { index, b ->
                Tarjeta {
                    Column(Modifier.fillMaxWidth().clickable { onEditar(b) }.padding(4.dp)) {
                        Text("${if (b.tipo == TYPE_TRIFASICO) "Trifásico" else "Monofásico"} · Tablero ${index + 1}", fontWeight = FontWeight.Bold, color = TextoOscuro)
                        Text(listOf(b.ubicacion, b.descripcion).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Sin ubicación ni descripción" }, color = TextoGris, fontSize = 13.sp)
                        Text("Tocar para revisar o corregir", color = VioletaOscuro, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (incompletos.isNotEmpty()) {
                Text(
                    if (mostrarIncompletos) "Ocultar borradores incompletos" else "Mostrar ${incompletos.size} borrador(es) incompleto(s)",
                    modifier = Modifier.clickable { mostrarIncompletos = !mostrarIncompletos }.padding(vertical = 8.dp),
                    color = VioletaOscuro,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
            if (mostrarIncompletos) {
                incompletos.forEachIndexed { index, b ->
                    Tarjeta {
                        Column(Modifier.fillMaxWidth().clickable { onEditar(b) }.padding(4.dp)) {
                            Text("${if (b.tipo == TYPE_TRIFASICO) "Trifásico" else "Monofásico"} · Borrador ${completados.size + index + 1}", fontWeight = FontWeight.Bold, color = TextoOscuro)
                            Text(listOf(b.ubicacion, b.descripcion).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Sin ubicación ni descripción" }, color = TextoGris, fontSize = 13.sp)
                            Text("Tocar para continuar este borrador", color = VioletaOscuro, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        TarjetaTipoTablero(
            titulo = "Tablero Trifásico",
            subtitulo = "Alimentación trifásica",
            detalles = "12 variables · 21 puntos de checklist",
            icono = Icons.Rounded.ElectricalServices,
            onClick = { onSeleccionarTipo(TYPE_TRIFASICO) }
        )
        Spacer(Modifier.height(14.dp))
        TarjetaTipoTablero(
            titulo = "Tablero Monofásico",
            subtitulo = "Alimentación monofásica",
            detalles = "4 variables · 9 puntos de checklist",
            icono = Icons.Rounded.Bolt,
            onClick = { onSeleccionarTipo(TYPE_MONOFASICO) }
        )

        Spacer(Modifier.height(20.dp))

        Tarjeta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VioletaClaro),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerdeOk, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Tableros realizados", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextoGris)
                    Text(
                        "${tarea.tablerosRealizados + completados.size}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = TextoOscuro
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (puedeFinalizar) {
            BtnPrimario(
                texto = "Finalizar tarea",
                modifier = Modifier.fillMaxWidth(),
                icono = Icons.Rounded.Flag,
                onClick = onFinalizar
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Revise los tableros guardados antes de confirmar el cierre.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = TextoGris,
                fontSize = 12.sp
            )
        } else {
            Text(
                "Solo el técnico de cabecera puede finalizar esta tarea.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = TextoGris,
                fontSize = 12.sp
            )
        }
    }
}
