package com.aring.movil.camera

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Permite a una pantalla de fase abrir la cámara compartida.
 * (phase, category, referenceId, slotIndex, label)
 */
typealias OpenCamera = (phase: Int, category: String, referenceId: String?, slotIndex: Int, label: String) -> Unit

data class PhotoRequest(
    val phase: Int,
    val category: String,
    val referenceId: String?,
    val slotIndex: Int,
    val label: String
)

data class PhotoResult(
    val request: PhotoRequest,
    val filePath: String
)

/**
 * Puente entre las pantallas de fases y la pantalla de cámara compartida.
 * El flujo es:
 * 1. La fase solicita una fotografía: requestPhoto(...)
 * 2. Se navega a la cámara.
 * 3. La cámara confirma: deliverResult(path)
 * 4. La fase consume el resultado.
 */
object PhotoBus {

    const val CATEGORY_PREVIOUS = "PREVIOUS"
    const val CATEGORY_MEASUREMENT = "MEASUREMENT"
    const val CATEGORY_CHECKLIST = "CHECKLIST"
    const val CATEGORY_POST = "POST"
    const val CATEGORY_REAPRIETE = "REAPRIETE"
    const val CATEGORY_ASEO = "ASEO"

    val request = MutableStateFlow<PhotoRequest?>(null)
    val result = MutableStateFlow<PhotoResult?>(null)

    fun requestPhoto(phase: Int, category: String, referenceId: String?, slotIndex: Int, label: String) {
        result.value = null
        request.value = PhotoRequest(phase, category, referenceId, slotIndex, label)
    }

    fun deliverResult(filePath: String) {
        val currentRequest = request.value ?: return
        result.value = PhotoResult(currentRequest, filePath)
    }

    fun consumeResult() {
        result.value = null
    }
}
