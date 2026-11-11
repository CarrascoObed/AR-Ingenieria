package com.aring.movil.ui.panel

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.Tarea
import com.aring.movil.ui.components.BtnPrimario
import com.aring.movil.ui.theme.FondoGris
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.VerdeOk
import com.aring.movil.ui.theme.VioletaClaro

@Composable
fun TableroConfirmacion(
    tarea: Tarea,
    progreso: String = "",
    errorCierre: String? = null,
    onVolverATareas: () -> Unit
) {
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
            Icon(
                if (!errorCierre.isNullOrBlank()) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (!errorCierre.isNullOrBlank()) androidx.compose.ui.graphics.Color(0xFFC62828) else VerdeOk,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            when {
                !errorCierre.isNullOrBlank() -> "El servidor requiere corregir informes"
                tarea.sincronizacionPendiente -> "Cierre de tarea en proceso"
                else -> "Mantenimiento: ${tarea.codigo} finalizado correctamente"
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 21.sp,
            color = TextoOscuro
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (!errorCierre.isNullOrBlank())
                "El servidor no aceptó todos los informes. Vuelve a tareas, abre la tarea y usa Corregir tableros. Los datos del paquete se conservan en el teléfono.\n\n$errorCierre"
            else if (tarea.sincronizacionPendiente)
                "El paquete del mantenimiento quedó guardado en este dispositivo. Se enviará al servidor y la tarea aparecerá finalizada en ambos lugares solo cuando el servidor confirme la recepción completa."
            else "Todos los tableros realizados fueron registrados y la tarea quedó finalizada.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextoGris,
            fontSize = 14.sp
        )
        if (progreso.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                progreso,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = TextoGris,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(32.dp))
        BtnPrimario(
            texto = "Volver a tareas",
            modifier = Modifier.fillMaxWidth(),
            onClick = onVolverATareas
        )
    }
}
