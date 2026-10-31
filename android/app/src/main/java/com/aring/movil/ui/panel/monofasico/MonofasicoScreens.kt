package com.aring.movil.ui.panel.monofasico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.aring.movil.camera.OpenCamera
import com.aring.movil.camera.PhotoBus
import com.aring.movil.ui.panel.common.ChecklistCard
import com.aring.movil.ui.panel.common.CollectPhotoResult
import com.aring.movil.ui.panel.common.ErrorBanner
import com.aring.movil.ui.panel.common.FormTextField
import com.aring.movil.ui.panel.common.MeasurementCard
import com.aring.movil.ui.panel.common.ObservacionesSection
import com.aring.movil.ui.panel.common.PhaseScreen
import com.aring.movil.ui.panel.common.PhotoSlotCard
import com.aring.movil.ui.panel.common.PhotoViewerDialog
import com.aring.movil.ui.panel.common.PrimaryButton
import com.aring.movil.ui.panel.common.SectionSubtitle
import com.aring.movil.ui.panel.common.SectionTitle
import com.aring.movil.ui.panel.common.Spacing12
import com.aring.movil.ui.panel.common.Spacing16
import com.aring.movil.ui.panel.common.Spacing8

@Composable
fun MonofasicoRegistroScreen(
    vm: MonofasicoViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }

    PhaseScreen(
        step = 1,
        onBack = onBack,
        topBarTitle = "Registro",
        content = {
            SectionTitle("Registro de mantenimiento preventivo")
            Spacing16()
            FormTextField("Descripción", state.registration.descripcion) { vm.setDescripcion(it) }
            Spacing12()
            FormTextField("Ubicación", state.registration.ubicacion) { vm.setUbicacion(it) }
            Spacing12()
            FormTextField("Piso", state.registration.nivel, keyboardType = KeyboardType.Number) { vm.setNivel(it) }
            Spacing16()
            ErrorBanner(error)
        },
        footer = {
            PrimaryButton("Siguiente") {
                val result = vm.validate(1)
                if (result.valid) onNext() else error = result.message
            }
        }
    )
}

@Composable
fun MonofasicoEvidenciasPreviasScreen(
    vm: MonofasicoViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    openCamera: OpenCamera
) {
    val state by vm.state.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    var viewPhotoPath by remember { mutableStateOf<String?>(null) }

    CollectPhotoResult { res ->
        if (res.request.category == PhotoBus.CATEGORY_PREVIOUS) {
            vm.setPreviousPhoto(res.request.slotIndex, res.filePath)
        }
    }

    PhaseScreen(
        step = 2,
        onBack = onBack,
        topBarTitle = "Evidencias previas",
        content = {
            SectionTitle("Evidencias previas")
            SectionSubtitle("Capture las 2 evidencias fotográficas del estado actual del tablero.")
            Spacing16()
            state.previousPhotos.forEach { slot ->
                PhotoSlotCard(
                    title = "Evidencia previa ${slot.index}",
                    subtitle = "Obligatoria",
                    path = slot.path,
                    onTakePhoto = {
                        openCamera(2, PhotoBus.CATEGORY_PREVIOUS, null, slot.index, "Evidencia previa ${slot.index}")
                    },
                    onViewPhoto = { slot.path?.let { viewPhotoPath = it } },
                    onRetakePhoto = {
                        openCamera(2, PhotoBus.CATEGORY_PREVIOUS, null, slot.index, "Evidencia previa ${slot.index}")
                    },
                    onPhotoEdited = { nuevaRuta ->
                        vm.setPreviousPhoto(slot.index, nuevaRuta)
                    }
                )
                Spacing12()
            }
            ErrorBanner(error)
        },
        footer = {
            PrimaryButton("Siguiente") {
                val result = vm.validate(2)
                if (result.valid) onNext() else error = result.message
            }
        }
    )

    viewPhotoPath?.let { PhotoViewerDialog(path = it, onDismiss = { viewPhotoPath = null }) }
}

@Composable
fun MonofasicoVariablesScreen(
    vm: MonofasicoViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    openCamera: OpenCamera
) {
    val state by vm.state.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    var viewPhotoPath by remember { mutableStateOf<String?>(null) }

    CollectPhotoResult { res ->
        when (res.request.category) {
            PhotoBus.CATEGORY_MEASUREMENT ->
                res.request.referenceId?.let { vm.setMeasurementPhoto(it, res.filePath) }
            PhotoBus.CATEGORY_REAPRIETE -> vm.setReaprietePhoto(res.filePath)
            PhotoBus.CATEGORY_ASEO -> vm.setAseoPhoto(res.filePath)
        }
    }

    PhaseScreen(
        step = 3,
        onBack = onBack,
        topBarTitle = "Variables a medir",
        content = {
            SectionTitle("Variables a medir")
            SectionSubtitle("Registre cada variable y tome UNA fotografía por variable.")
            Spacing16()
            state.measurements.forEach { measurement ->
                MeasurementCard(
                    label = measurement.label,
                    unit = measurement.unit,
                    value = measurement.value,
                    onValueChange = { vm.setMeasurementValue(measurement.variableId, it) },
                    photoPath = measurement.photoPath,
                    onTakePhoto = {
                        openCamera(3, PhotoBus.CATEGORY_MEASUREMENT, measurement.variableId, -1, measurement.label)
                    },
                    onViewPhoto = { measurement.photoPath?.let { viewPhotoPath = it } },
                    onRetakePhoto = {
                        openCamera(3, PhotoBus.CATEGORY_MEASUREMENT, measurement.variableId, -1, measurement.label)
                    },
                    onPhotoEdited = { nuevaRuta ->
                        vm.setMeasurementPhoto(measurement.variableId, nuevaRuta)
                    },
                    showValueInput = false
                )
                Spacing12()
            }
            Spacing8()
            PhotoSlotCard(
                title = "Reapriete",
                subtitle = "Obligatoria",
                path = state.reaprietePhoto,
                onTakePhoto = {
                    openCamera(3, PhotoBus.CATEGORY_REAPRIETE, null, -1, "Reapriete")
                },
                onViewPhoto = { state.reaprietePhoto?.let { viewPhotoPath = it } },
                onRetakePhoto = {
                    openCamera(3, PhotoBus.CATEGORY_REAPRIETE, null, -1, "Reapriete")
                },
                onPhotoEdited = { vm.setReaprietePhoto(it) }
            )
            Spacing12()
            PhotoSlotCard(
                title = "Aseo",
                subtitle = "Obligatoria",
                path = state.aseoPhoto,
                onTakePhoto = {
                    openCamera(3, PhotoBus.CATEGORY_ASEO, null, -1, "Aseo")
                },
                onViewPhoto = { state.aseoPhoto?.let { viewPhotoPath = it } },
                onRetakePhoto = {
                    openCamera(3, PhotoBus.CATEGORY_ASEO, null, -1, "Aseo")
                },
                onPhotoEdited = { vm.setAseoPhoto(it) }
            )
            Spacing16()
            ErrorBanner(error)
        },
        footer = {
            PrimaryButton("Siguiente") {
                val result = vm.validate(3)
                if (result.valid) onNext() else error = result.message
            }
        }
    )

    viewPhotoPath?.let { PhotoViewerDialog(path = it, onDismiss = { viewPhotoPath = null }) }
}

@Composable
fun MonofasicoChecklistScreen(
    vm: MonofasicoViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    openCamera: OpenCamera
) {
    val state by vm.state.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    var viewPhotoPath by remember { mutableStateOf<String?>(null) }

    CollectPhotoResult { res ->
        if (res.request.category == PhotoBus.CATEGORY_CHECKLIST) {
            res.request.referenceId?.let { vm.setChecklistPhoto(it, res.filePath) }
        }
    }

    PhaseScreen(
        step = 4,
        onBack = onBack,
        topBarTitle = "Checklist",
        content = {
            SectionTitle("Checklist de inspección")
            SectionSubtitle("Marque el estado de cada elemento. Los elementos C/R requieren fotografía.")
            Spacing16()
            state.checklist.forEach { item ->
                ChecklistCard(
                    label = item.label,
                    status = item.status,
                    onStatusSelected = { vm.setChecklistStatus(item.itemId, it) },
                    photoPath = item.photoPath,
                    onTakePhoto = {
                        openCamera(4, PhotoBus.CATEGORY_CHECKLIST, item.itemId, -1, item.label)
                    },
                    onViewPhoto = { item.photoPath?.let { viewPhotoPath = it } },
                    onRetakePhoto = {
                        openCamera(4, PhotoBus.CATEGORY_CHECKLIST, item.itemId, -1, item.label)
                    },
                    onPhotoEdited = { nuevaRuta ->
                        vm.setChecklistPhoto(item.itemId, nuevaRuta)
                    }
                )
                Spacing12()
            }
            Spacing16()
            ErrorBanner(error)
        },
        footer = {
            PrimaryButton("Siguiente") {
                val result = vm.validate(4)
                if (result.valid) onNext() else error = result.message
            }
        }
    )

    viewPhotoPath?.let { PhotoViewerDialog(path = it, onDismiss = { viewPhotoPath = null }) }
}

@Composable
fun MonofasicoEvidenciasPosterioresScreen(
    vm: MonofasicoViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    openCamera: OpenCamera
) {
    val state by vm.state.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    var viewPhotoPath by remember { mutableStateOf<String?>(null) }

    CollectPhotoResult { res ->
        if (res.request.category == PhotoBus.CATEGORY_POST) {
            vm.setPostPhoto(res.request.slotIndex, res.filePath)
        }
    }

    PhaseScreen(
        step = 5,
        onBack = onBack,
        topBarTitle = "Evidencias posteriores",
        content = {
            SectionTitle("Evidencias posteriores")
            SectionSubtitle("Capture las 2 evidencias fotográficas al finalizar el mantenimiento.")
            Spacing16()
            state.postPhotos.forEach { slot ->
                PhotoSlotCard(
                    title = "Evidencia posterior ${slot.index}",
                    subtitle = "Obligatoria",
                    path = slot.path,
                    onTakePhoto = {
                        openCamera(5, PhotoBus.CATEGORY_POST, null, slot.index, "Evidencia posterior ${slot.index}")
                    },
                    onViewPhoto = { slot.path?.let { viewPhotoPath = it } },
                    onRetakePhoto = {
                        openCamera(5, PhotoBus.CATEGORY_POST, null, slot.index, "Evidencia posterior ${slot.index}")
                    },
                    onPhotoEdited = { nuevaRuta ->
                        vm.setPostPhoto(slot.index, nuevaRuta)
                    }
                )
                Spacing12()
            }
            ErrorBanner(error)
        },
        footer = {
            PrimaryButton("Siguiente") {
                val result = vm.validate(5)
                if (result.valid) onNext() else error = result.message
            }
        }
    )

    viewPhotoPath?.let { PhotoViewerDialog(path = it, onDismiss = { viewPhotoPath = null }) }
}

@Composable
fun MonofasicoObservacionesScreen(
    vm: MonofasicoViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }

    PhaseScreen(
        step = 6,
        onBack = onBack,
        topBarTitle = "Observaciones",
        content = {
            SectionTitle("Observaciones")
            SectionSubtitle("Agregue cualquier observación sobre el mantenimiento realizado.")
            Spacing16()
            ObservacionesSection(
                observaciones = state.observations,
                onValueChange = { index, value -> vm.setObservation(index, value) },
                onAdd = { vm.addObservation() },
                onRemove = { vm.removeObservation(it) }
            )
        },
        footer = {
            PrimaryButton(
                text = "Finalizar tablero",
                loading = state.isSaving,
                onClick = { vm.finalize() }
            )
        }
    )
}
