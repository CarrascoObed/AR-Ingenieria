package com.aring.movil.ui.panel.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aring.movil.camera.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

/**
 * Diálogo para dibujar con lápiz sobre una fotografía de evidencia
 * (interacción similar a las anotaciones de WhatsApp).
 *
 * Los trazos se guardan en coordenadas unitarias (0..1) sobre el área
 * dibujada de la imagen, de modo que se escalan correctamente al confirmar.
 *
 * Al confirmar se escribe SIEMPRE un archivo nuevo (la foto original no se
 * altera) y se entrega la nueva ruta por [onConfirm]. Cancelar cierra el
 * diálogo sin modificar la imagen original.
 */
@Composable
fun AnotadorFotoDialog(
    path: String,
    onConfirm: (nuevaRuta: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var cargaTerminada by remember(path) { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) { decodeSampledBitmap(path, 1600) }
        cargaTerminada = true
    }
    val cargando = !cargaTerminada
    val fallo = cargaTerminada && bitmap == null

    val trazos = remember { mutableStateListOf<List<Offset>>() }
    var trazoActual by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var guardando by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (fallo) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No se pudo cargar la imagen",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar", color = Color.White)
                    }
                }
            } else {
                val bmp = bitmap
                if (bmp == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    // Edición a pantalla completa: la imagen se ajusta proporcionalmente
                    // (ContentScale Fit, sin estirar ni recortar) al espacio disponible.
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val aspecto = bmp.width.toFloat() / bmp.height.toFloat()
                        var ancho = maxWidth
                        var alto = ancho / aspecto
                        if (alto > maxHeight) {
                            alto = maxHeight
                            ancho = alto * aspecto
                        }

                        Canvas(
                            modifier = Modifier
                                .width(ancho)
                                .height(alto)
                                .pointerInput(bmp) {
                                    detectDragGestures(
                                        onDragStart = { pos ->
                                            trazoActual = listOf(
                                                Offset(
                                                    pos.x / size.width.toFloat(),
                                                    pos.y / size.height.toFloat()
                                                )
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            trazoActual = trazoActual + Offset(
                                                change.position.x / size.width.toFloat(),
                                                change.position.y / size.height.toFloat()
                                            )
                                        },
                                        onDragEnd = {
                                            if (trazoActual.size > 1) trazos.add(trazoActual)
                                            trazoActual = emptyList()
                                        },
                                        onDragCancel = {
                                            trazoActual = emptyList()
                                        }
                                    )
                                }
                        ) {
                            drawImage(
                                image = bmp.asImageBitmap(),
                                dstSize = IntSize(size.width.toInt(), size.height.toInt())
                            )
                            val stroke = Stroke(
                                width = (size.width * 0.006f).coerceAtLeast(3f)
                            )
                            trazos.forEach { trazo -> dibujarTrazo(trazo, stroke) }
                            if (trazoActual.size > 1) {
                                dibujarTrazo(trazoActual, stroke)
                            }
                        }

                        // Controles accesibles superpuestos sobre la imagen.
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .navigationBarsPadding()
                                .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 72.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    enabled = trazos.isNotEmpty() && !guardando,
                                    onClick = { trazos.removeAt(trazos.lastIndex) }
                                ) {
                                    Icon(
                                        Icons.Rounded.Undo,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Deshacer", color = Color.White)
                                }
                                TextButton(
                                    enabled = trazos.isNotEmpty() && !guardando,
                                    onClick = { trazos.clear() }
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Borrar todo", color = Color.White)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(enabled = !guardando, onClick = onDismiss) {
                                    Text("Cancelar", color = Color.White)
                                }
                                Button(
                                    enabled = !guardando,
                                    onClick = {
                                        val base = bitmap ?: return@Button
                                        if (trazos.isEmpty()) {
                                            // Sin anotaciones: conservar la foto original tal cual.
                                            onDismiss()
                                            return@Button
                                        }
                                        guardando = true
                                        scope.launch {
                                            val nueva = withContext(Dispatchers.IO) {
                                                runCatching {
                                                    val copia = base.copy(Bitmap.Config.ARGB_8888, true)
                                                        ?: return@runCatching null
                                                    val canvas = Canvas(copia)
                                                    val paint = Paint().apply {
                                                        color = android.graphics.Color.RED
                                                        style = Paint.Style.STROKE
                                                        strokeWidth = copia.width * 0.008f
                                                        isAntiAlias = true
                                                        strokeCap = Paint.Cap.ROUND
                                                        strokeJoin = Paint.Join.ROUND
                                                    }
                                                    trazos.forEach { trazo ->
                                                        if (trazo.size > 1) {
                                                            val ruta = Path()
                                                            ruta.moveTo(
                                                                trazo.first().x * copia.width,
                                                                trazo.first().y * copia.height
                                                            )
                                                            trazo.drop(1).forEach { punto ->
                                                                ruta.lineTo(
                                                                    punto.x * copia.width,
                                                                    punto.y * copia.height
                                                                )
                                                            }
                                                            canvas.drawPath(ruta, paint)
                                                        }
                                                    }
                                                    val archivo = PhotoStorage.newPhotoFile(context)
                                                    FileOutputStream(archivo).use { out ->
                                                        copia.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                                    }
                                                    archivo.absolutePath
                                                }.getOrNull()
                                            }
                                            guardando = false
                                            if (nueva != null) {
                                                onConfirm(nueva)
                                            } else {
                                                // Si falla la generación no se altera la foto original.
                                                onDismiss()
                                            }
                                        }
                                    }
                                ) {
                                    if (guardando) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White
                                        )
                                    } else {
                                        Text("Confirmar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.dibujarTrazo(
    trazo: List<Offset>,
    stroke: Stroke
) {
    if (trazo.size < 2) return
    val ruta = ComposePath().apply {
        moveTo(trazo.first().x * size.width, trazo.first().y * size.height)
        trazo.drop(1).forEach { punto ->
            lineTo(punto.x * size.width, punto.y * size.height)
        }
    }
    drawPath(ruta, Color.Red, style = stroke)
}
