package com.aring.movil.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import java.io.FileOutputStream
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aring.movil.ui.panel.common.PrimaryButton
import com.aring.movil.ui.theme.RojoError
import com.aring.movil.ui.theme.Violeta

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun CameraScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val request = PhotoBus.request.value

    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    var captureError by remember { mutableStateOf<String?>(null) }
    var flashOn by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            capturing = true
            captureError = null
            coroutineScope.launch {
                val importedFile = withContext(Dispatchers.IO) {
                    runCatching {
                        val file = PhotoStorage.newPhotoFile(context)
                        try {
                            val input = context.contentResolver.openInputStream(uri)
                                ?: error("No se pudo leer la imagen seleccionada")
                            input.use { source ->
                                FileOutputStream(file).use { destination -> source.copyTo(destination) }
                            }
                            file
                        } catch (error: Exception) {
                            file.delete()
                            throw error
                        }
                    }.getOrNull()
                }
                capturing = false
                if (importedFile != null) {
                    PhotoBus.deliverResult(importedFile.absolutePath)
                    onDone()
                } else {
                    captureError = "No se pudo cargar la fotografía seleccionada"
                }
            }
        }
    }

    val previewView = remember { PreviewView(context) }
    val imageCaptureRef = remember { arrayOfNulls<ImageCapture>(1) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(lifecycleOwner, granted) {
        if (granted) {
            val listener = Runnable {
                runCatching {
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCaptureRef[0] = imageCapture
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                }.onFailure {
                    captureError = "No se pudo iniciar la cámara"
                }
            }
            cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        }
        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    fun takePicture() {
        if (capturing) return
        val imageCapture = imageCaptureRef[0] ?: return
        capturing = true
        captureError = null
        imageCapture.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        val file = PhotoStorage.newPhotoFile(context)
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    capturing = false
                    PhotoBus.deliverResult(file.absolutePath)
                    onDone()
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    captureError = "Error al capturar la fotografía"
                }
            }
        )
    }

    BackHandler(onBack = onDone)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        if (request != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Fotografía: ${request.label}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }

        IconButton(
            onClick = onDone,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cancelar",
                tint = Color.White
            )
        }

        if (captureError != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp),
                color = RojoError.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = captureError ?: "",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }

        when {
            !granted -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Permiso de cámara requerido para continuar",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.size(16.dp))
                    PrimaryButton("Solicitar permiso") {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Text("Elegir de galería", color = Color.White)
                    }
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = onDone) {
                        Text("Cancelar", color = Color.White)
                    }
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        enabled = !capturing
                    ) {
                        Text("Galería", color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, Violeta, CircleShape)
                            .clickable(enabled = !capturing) { takePicture() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                color = Violeta,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDDD9E8))
                            )
                        }
                    }
                    IconButton(
                        onClick = { flashOn = !flashOn },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = if (flashOn) "Apagar flash" else "Encender flash",
                            tint = if (flashOn) Color(0xFFFFD54F) else Color.White
                        )
                    }
                }
            }
        }
    }
}
