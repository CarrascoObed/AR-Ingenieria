package com.aring.movil.ui.tecnico

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.Tarea
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.BtnPrimario
import com.aring.movil.ui.components.HorizontalSeparador
import com.aring.movil.ui.components.Tarjeta
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.Violeta

@Composable
fun DetalleTareaTecnico(
    tarea: Tarea,
    onIngresar: (Tarea) -> Unit,
) {
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            tarea.codigo,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Violeta
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tarea.tipo,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextoGris,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))

        Tarjeta {
            Text("Nombre del recinto", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoGris)
            Spacer(Modifier.height(2.dp))
            Text(tarea.nombreRecinto, color = TextoOscuro, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            HorizontalSeparador()
            Spacer(Modifier.height(12.dp))
            Text("Dirección", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoGris)
            Spacer(Modifier.height(2.dp))
            Text(tarea.direccion, color = TextoOscuro, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            HorizontalSeparador()
            Spacer(Modifier.height(12.dp))
            Text("Descripción de la tarea", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoGris)
            Spacer(Modifier.height(2.dp))
            Text(tarea.descripcion, color = TextoOscuro, fontSize = 14.sp)
        }

        Spacer(Modifier.height(20.dp))

        if (error.isNotEmpty()) {
            BannerError(error)
            Spacer(Modifier.height(12.dp))
        }

        BtnPrimario(
            texto = "Ir a la dirección",
            modifier = Modifier.fillMaxWidth(),
            icono = Icons.Filled.LocationOn
        ) {
            error = ""
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(tarea.direccion))
            try {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_VIEW, uri),
                        "Abrir con"
                    )
                )
            } catch (_: ActivityNotFoundException) {
                error = "No se encontró una aplicación de mapas instalada."
            }
        }

        Spacer(Modifier.height(12.dp))

        if (tarea.sincronizacionPendiente) {
            Text(
                "Esta tarea ya está terminada en el dispositivo y espera enviar sus datos cuando vuelva Internet.",
                color = TextoGris,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
        }

        if (tarea.estado == "Finalizada" && !tarea.sincronizacionPendiente) {
            Text(
                "Tarea finalizada. El informe completo está disponible en el panel web del administrador.",
                color = TextoGris,
                fontSize = 13.sp
            )
        } else {
            BtnPrimario(
                texto = if (tarea.estado == "En proceso") "Retomar tarea" else "Ingresar a la tarea",
                modifier = Modifier.fillMaxWidth(),
                icono = Icons.Filled.PlayArrow
            ) {
                onIngresar(tarea)
            }
        }
    }
}
