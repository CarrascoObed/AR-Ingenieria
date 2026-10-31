package com.aring.movil.ui.tecnico

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.ApiService
import com.aring.movil.data.Prefs
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Tarea
import com.aring.movil.data.Tecnico
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.BannerOk
import com.aring.movil.ui.components.BtnPrimario
import com.aring.movil.ui.components.BtnSecundario
import com.aring.movil.ui.components.ChipEstado
import com.aring.movil.ui.components.ConfirmarDialog
import com.aring.movil.ui.components.HorizontalSeparador
import com.aring.movil.ui.components.Tarjeta
import com.aring.movil.ui.components.Vacio
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.Violeta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DocumentosTecnicoScreen(tecnico: Tecnico) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var error by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }
    var limpiarAbierto by remember { mutableStateOf(false) }
    var informePendiente by remember { mutableStateOf<com.aring.movil.data.DescargaInforme?>(null) }

    val ocultas = remember {
        mutableStateOf(Prefs.leerOcultasFinalizadas().mapNotNull { it.toIntOrNull() }.toSet())
    }
    val finalizadas = Repositorio.tareas
        .filter { it.estado == "Finalizada" && it.id !in ocultas.value }
        .sortedBy { it.id }

    fun mostrarAviso(texto: String) {
        aviso = texto
        scope.launch {
            delay(4000)
            aviso = ""
        }
    }

    val crearDocumento = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) { uri ->
        val informe = informePendiente
        informePendiente = null
        if (uri == null || informe == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(informe.bytes)
                } ?: throw IllegalStateException("No se pudo abrir el archivo.")
                mostrarAviso("Informe generado y descargado.")
            } catch (e: Exception) {
                runCatching { context.contentResolver.delete(uri, null, null) }
                error = e.message?.takeIf { it.isNotBlank() } ?: "No se pudo generar el informe."
            }
        }
    }

    fun limpiarFinalizadas() {
        val ids = finalizadas.map { it.id }
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
            "Documentos",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Descarga el informe de cada tarea finalizada.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextoGris,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))

        BannerError(error)
        BannerOk(aviso)
        Spacer(Modifier.height(4.dp))

        if (finalizadas.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
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

        if (finalizadas.isEmpty()) {
            Vacio("No hay tareas finalizadas.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(finalizadas, key = { it.id }) { t ->
                    TarjetaFinalizada(
                        t,
                        onDescargar = {
                            error = ""
                            scope.launch {
                                try {
                                    val informe = ApiService.informeTarea(t.id)
                                    informePendiente = informe
                                    crearDocumento.launch(informe.nombreArchivo)
                                } catch (e: Exception) {
                                    error = e.message?.takeIf { it.isNotBlank() }
                                        ?: "No se pudo generar el informe."
                                }
                            }
                        }
                    )
                }
            }
        }
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

@Composable
private fun TarjetaFinalizada(t: Tarea, onDescargar: () -> Unit) {
    Tarjeta {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                t.tipo,
                color = Violeta,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                t.codigo,
                color = TextoOscuro,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            ChipEstado(t.estado)
            Spacer(Modifier.height(14.dp))
            BtnPrimario(
                texto = "Descargar informe",
                icono = Icons.Filled.Download,
                chico = true
            ) {
                onDescargar()
            }
        }
        HorizontalSeparador()
    }
}

