package com.aring.movil.data.panel

data class MonofasicoRegistration(
    val descripcion: String = "",
    val ubicacion: String = "",
    val nivel: String = ""
)

data class MonofasicoMeasurement(
    val variableId: String,
    val label: String,
    val unit: String,
    val value: String = "",
    val photoPath: String? = null
)

data class MonofasicoChecklistItem(
    val itemId: String,
    val label: String,
    val status: ChecklistStatus? = null,
    val photoPath: String? = null
)

data class MonofasicoState(
    val inspectionId: String = newId(),
    val registration: MonofasicoRegistration = MonofasicoRegistration(),
    val previousPhotos: List<PhotoSlot> = listOf(PhotoSlot(1), PhotoSlot(2)),
    val measurements: List<MonofasicoMeasurement> = MonofasicoConfig.variables
        .map { MonofasicoMeasurement(it.id, it.label, it.unit) },
    val reaprietePhoto: String? = null,
    val aseoPhoto: String? = null,
    val checklist: List<MonofasicoChecklistItem> = MonofasicoConfig.checklist
        .map { MonofasicoChecklistItem(it.id, it.label) },
    val postPhotos: List<PhotoSlot> = listOf(PhotoSlot(1), PhotoSlot(2)),
    val observations: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val finished: Boolean = false
)
