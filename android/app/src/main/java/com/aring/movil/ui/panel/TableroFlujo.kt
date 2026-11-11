package com.aring.movil.ui.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aring.movil.camera.CameraScreen
import com.aring.movil.camera.OpenCamera
import com.aring.movil.camera.PhotoBus
import com.aring.movil.data.ApiService
import com.aring.movil.data.panel.TYPE_MONOFASICO
import com.aring.movil.data.panel.TYPE_TRIFASICO
import com.aring.movil.data.panel.newId
import com.aring.movil.ui.panel.monofasico.MonofasicoChecklistScreen
import com.aring.movil.ui.panel.monofasico.MonofasicoEvidenciasPreviasScreen
import com.aring.movil.ui.panel.monofasico.MonofasicoObservacionesScreen
import com.aring.movil.ui.panel.monofasico.MonofasicoRegistroScreen
import com.aring.movil.ui.panel.monofasico.MonofasicoVariablesScreen
import com.aring.movil.ui.panel.monofasico.MonofasicoViewModel
import com.aring.movil.ui.panel.trifasico.TrifasicoChecklistScreen
import com.aring.movil.ui.panel.trifasico.TrifasicoEvidenciasPreviasScreen
import com.aring.movil.ui.panel.trifasico.TrifasicoObservacionesScreen
import com.aring.movil.ui.panel.trifasico.TrifasicoRegistroScreen
import com.aring.movil.ui.panel.trifasico.TrifasicoVariablesScreen
import com.aring.movil.ui.panel.trifasico.TrifasicoViewModel

private sealed interface SubPantalla {
    data object Registro : SubPantalla
    data object EvidenciasPrevias : SubPantalla
    data object Variables : SubPantalla
    data object Checklist : SubPantalla
    data object Observaciones : SubPantalla
    data object Camara : SubPantalla
}

private val SubPantallaSaver = listSaver<SubPantalla, String>(
    save = { sub -> listOf(sub::class.simpleName ?: "Registro") },
    restore = { lista ->
        when (lista[0]) {
            "EvidenciasPrevias" -> SubPantalla.EvidenciasPrevias
            "Variables" -> SubPantalla.Variables
            "Checklist" -> SubPantalla.Checklist
            "EvidenciasPosteriores" -> SubPantalla.Observaciones
            "Observaciones" -> SubPantalla.Observaciones
            "Camara" -> SubPantalla.Camara
            else -> SubPantalla.Registro
        }
    }
)

@Composable
fun TableroFlujo(
    tipo: String,
    tareaId: Int,
    inspectionId: String = newId(),
    onFinalizado: () -> Unit,
    onVolver: () -> Unit
) {
    val trifasicoVm = remember(tareaId, inspectionId) { TrifasicoViewModel(tareaId, inspectionId) }
    val monofasicoVm = remember(tareaId, inspectionId) { MonofasicoViewModel(tareaId, inspectionId) }
    var nombresServidor by remember(tareaId) { mutableStateOf(emptyList<Pair<String, String?>>()) }
    LaunchedEffect(tareaId) {
        nombresServidor = runCatching { ApiService.nombresTableros(tareaId) }.getOrDefault(emptyList())
    }
    var sub by rememberSaveable(stateSaver = SubPantallaSaver) {
        mutableStateOf<SubPantalla>(SubPantalla.Registro)
    }
    var fasePrevia by rememberSaveable { mutableIntStateOf(2) }

    val openCamera: OpenCamera = { phase, category, referenceId, slotIndex, label ->
        fasePrevia = phase
        PhotoBus.requestPhoto(phase, category, referenceId, slotIndex, label)
        sub = SubPantalla.Camara
    }

    fun volverDeCamara() {
        sub = when (fasePrevia) {
            2 -> SubPantalla.EvidenciasPrevias
            3 -> SubPantalla.Variables
            4 -> SubPantalla.Checklist
            else -> SubPantalla.Registro
        }
    }

    val faseActual: SubPantalla = when (sub) {
        SubPantalla.Camara -> when (fasePrevia) {
            2 -> SubPantalla.EvidenciasPrevias
            3 -> SubPantalla.Variables
            4 -> SubPantalla.Checklist
            else -> SubPantalla.Registro
        }
        else -> sub
    }

    Box(Modifier.fillMaxSize()) {
        when (faseActual) {
            SubPantalla.Camara -> Unit

            SubPantalla.Registro -> {
            if (tipo == TYPE_TRIFASICO) {
                TrifasicoRegistroScreen(
                    vm = trifasicoVm,
                    nombresServidor = nombresServidor,
                    onNext = { sub = SubPantalla.EvidenciasPrevias },
                    onBack = onVolver
                )
            } else {
                MonofasicoRegistroScreen(
                    vm = monofasicoVm,
                    nombresServidor = nombresServidor,
                    onNext = { sub = SubPantalla.EvidenciasPrevias },
                    onBack = onVolver
                )
            }
        }

        SubPantalla.EvidenciasPrevias -> {
            if (tipo == TYPE_TRIFASICO) {
                TrifasicoEvidenciasPreviasScreen(
                    vm = trifasicoVm,
                    onNext = { sub = SubPantalla.Variables },
                    onBack = onVolver,
                    openCamera = openCamera
                )
            } else {
                MonofasicoEvidenciasPreviasScreen(
                    vm = monofasicoVm,
                    onNext = { sub = SubPantalla.Variables },
                    onBack = onVolver,
                    openCamera = openCamera
                )
            }
        }

        SubPantalla.Variables -> {
            if (tipo == TYPE_TRIFASICO) {
                TrifasicoVariablesScreen(
                    vm = trifasicoVm,
                    onNext = { sub = SubPantalla.Checklist },
                    onBack = onVolver,
                    openCamera = openCamera
                )
            } else {
                MonofasicoVariablesScreen(
                    vm = monofasicoVm,
                    onNext = { sub = SubPantalla.Checklist },
                    onBack = onVolver,
                    openCamera = openCamera
                )
            }
        }

        SubPantalla.Checklist -> {
            if (tipo == TYPE_TRIFASICO) {
                TrifasicoChecklistScreen(
                    vm = trifasicoVm,
                    onNext = { sub = SubPantalla.Observaciones },
                    onBack = onVolver,
                    openCamera = openCamera
                )
            } else {
                MonofasicoChecklistScreen(
                    vm = monofasicoVm,
                    onNext = { sub = SubPantalla.Observaciones },
                    onBack = onVolver,
                    openCamera = openCamera
                )
            }
        }

        SubPantalla.Observaciones -> {
            if (tipo == TYPE_TRIFASICO) {
                TrifasicoObservacionesScreen(
                    vm = trifasicoVm,
                    onBack = onVolver,
                    onFinished = onFinalizado
                )
            } else {
                MonofasicoObservacionesScreen(
                    vm = monofasicoVm,
                    onBack = onVolver,
                    onFinished = onFinalizado
                )
            }
        }
        }

        if (sub == SubPantalla.Camara) {
            CameraScreen(onDone = { volverDeCamara() })
        }
    }
}
