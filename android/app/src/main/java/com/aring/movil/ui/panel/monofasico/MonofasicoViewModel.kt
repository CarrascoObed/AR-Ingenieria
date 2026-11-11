package com.aring.movil.ui.panel.monofasico

import com.aring.movil.data.panel.ChecklistStatus
import com.aring.movil.data.panel.MonofasicoState
import com.aring.movil.data.panel.MonofasicoValidation
import com.aring.movil.data.panel.PhotoSlot
import com.aring.movil.data.panel.TableroDraftStore
import com.aring.movil.data.panel.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MonofasicoViewModel(private val tareaId: Int, private val inspectionId: String) {

    private val borradorInicial = TableroDraftStore.cargarMonofasico(tareaId, inspectionId)
    private val _state = MutableStateFlow(
        borradorInicial ?: MonofasicoState(inspectionId = inspectionId)
    )
    val state: StateFlow<MonofasicoState> = _state.asStateFlow()

    init {
        if (borradorInicial?.finished == true) {
            _state.value = _state.value.copy(finished = false)
            TableroDraftStore.guardarMonofasico(tareaId, _state.value, inspectionId)
        }
    }

    private fun cambiar(transformar: (MonofasicoState) -> MonofasicoState) {
        _state.update(transformar)
        TableroDraftStore.guardarMonofasico(tareaId, _state.value, inspectionId)
    }

    fun startNewInspection() {
        val nuevo = MonofasicoState(inspectionId = inspectionId)
        _state.value = nuevo
        TableroDraftStore.guardarMonofasico(tareaId, nuevo, inspectionId)
    }

    fun setDescripcion(value: String) =
        cambiar { it.copy(registration = it.registration.copy(descripcion = value)) }

    fun descripcionDuplicada(value: String, nombresServidor: List<Pair<String, String?>>): Boolean =
        TableroDraftStore.hayDescripcionDuplicada(tareaId, inspectionId, value, nombresServidor)

    fun setUbicacion(value: String) =
        cambiar { it.copy(registration = it.registration.copy(ubicacion = value)) }

    fun setNivel(value: String) =
        cambiar { it.copy(registration = it.registration.copy(nivel = value)) }

    fun setPreviousPhoto(index: Int, path: String?) =
        cambiar { st ->
            val existentes = st.previousPhotos.any { it.index == index }
            val actualizadas = when {
                existentes && index == 1 -> st.previousPhotos.map { if (it.index == index) it.copy(path = path) else it }
                existentes && path == null -> st.previousPhotos.filterNot { it.index == index }
                existentes -> st.previousPhotos.map { if (it.index == index) it.copy(path = path) else it }
            path != null -> st.previousPhotos + PhotoSlot(index, path)
                else -> st.previousPhotos
            }.sortedBy { it.index }
            st.copy(previousPhotos = actualizadas.ifEmpty { listOf(PhotoSlot(1)) })
        }

    fun setMeasurementValue(variableId: String, value: String) =
        cambiar { st ->
            st.copy(
                measurements = st.measurements.map {
                    if (it.variableId == variableId) it.copy(value = value) else it
                }
            )
        }

    fun setMeasurementPhoto(variableId: String, path: String?) =
        cambiar { st ->
            st.copy(
                measurements = st.measurements.map {
                    if (it.variableId == variableId) it.copy(photoPath = path) else it
                }
            )
        }

    fun setReaprietePhoto(path: String?) = cambiar { it.copy(reaprietePhoto = path) }

    fun setAseoPhoto(path: String?) = cambiar { it.copy(aseoPhoto = path) }

    fun setChecklistStatus(itemId: String, status: ChecklistStatus) =
        cambiar { st ->
            st.copy(
                checklist = st.checklist.map {
                    if (it.itemId == itemId) it.copy(status = status) else it
                }
            )
        }

    fun setChecklistPhoto(itemId: String, path: String?) =
        cambiar { st ->
            st.copy(
                checklist = st.checklist.map {
                    if (it.itemId == itemId) it.copy(photoPath = path) else it
                }
            )
        }

    fun setChecklistObservation(itemId: String, value: String) =
        cambiar { st ->
            st.copy(checklist = st.checklist.map { if (it.itemId == itemId) it.copy(observation = value) else it })
        }

    fun setObservation(index: Int, value: String) =
        cambiar { st ->
            val lista = st.observations.toMutableList()
            while (lista.size <= index) lista.add("")
            lista[index] = value
            st.copy(observations = lista)
        }

    fun addObservation() = cambiar { st -> st.copy(observations = st.observations + "") }

    fun removeObservation(index: Int) =
        cambiar { st ->
            st.copy(observations = st.observations.filterIndexed { i, _ -> i != index })
        }

    fun validate(phase: Int): ValidationResult = MonofasicoValidation.validate(_state.value, phase)

    fun finalize() {
        cambiar { it.copy(finished = true) }
    }
}
