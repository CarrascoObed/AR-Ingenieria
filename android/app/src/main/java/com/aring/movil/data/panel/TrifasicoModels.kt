package com.aring.movil.data.panel

data class TrifasicoRegistration(
    val descripcion: String = "",
    val ubicacion: String = "",
    val nivel: String = "",
    val tamanioTablero: String = ""
)

data class TrifasicoMeasurement(
    val variableId: String,
    val label: String,
    val unit: String,
    val value: String = "",
    val photoPath: String? = null
)

data class TrifasicoChecklistItem(
    val itemId: String,
    val label: String,
    val status: ChecklistStatus? = null,
    val photoPath: String? = null,
    val observation: String = ""
)

data class TrifasicoState(
    val inspectionId: String = newId(),
    val registration: TrifasicoRegistration = TrifasicoRegistration(),
    val previousPhotos: List<PhotoSlot> = listOf(PhotoSlot(1), PhotoSlot(2)),
    val measurements: List<TrifasicoMeasurement> = TrifasicoConfig.variables
        .map { TrifasicoMeasurement(it.id, it.label, it.unit) },
    val reaprietePhoto: String? = null,
    val aseoPhoto: String? = null,
    val checklist: List<TrifasicoChecklistItem> = TrifasicoConfig.checklist
        .map { TrifasicoChecklistItem(it.id, it.label) },
    val postPhotos: List<PhotoSlot> = listOf(PhotoSlot(1), PhotoSlot(2)),
    val observations: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val finished: Boolean = false
)
