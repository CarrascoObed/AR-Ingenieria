package com.aring.movil.ui.panel.trifasico

import com.aring.movil.data.panel.ChecklistStatus
import com.aring.movil.data.panel.TableroDraftStore
import com.aring.movil.data.panel.TrifasicoState
import com.aring.movil.data.panel.TrifasicoValidation
import com.aring.movil.data.panel.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TrifasicoViewModel(private val tareaId: Int, private val inspectionId: String) {

    private val borradorInicial = TableroDraftStore.cargarTrifasico(tareaId, inspectionId)
    private val _state = MutableStateFlow(
        borradorInicial ?: TrifasicoState(inspectionId = inspectionId)
    )
    val state: StateFlow<TrifasicoState> = _state.asStateFlow()

    init {
        if (borradorInicial?.finished == true) {
            _state.value = _state.value.copy(finished = false)
            TableroDraftStore.guardarTrifasico(tareaId, _state.value, inspectionId)
        }
    }

    private fun cambiar(transformar: (TrifasicoState) -> TrifasicoState) {
        _state.update(transformar)
        TableroDraftStore.guardarTrifasico(tareaId, _state.value, inspectionId)
    }

    fun startNewInspection() {
        val nuevo = TrifasicoState(inspectionId = inspectionId)
        _state.value = nuevo
        TableroDraftStore.guardarTrifasico(tareaId, nuevo, inspectionId)
    }

    fun setDescripcion(value: String) =
        cambiar { it.copy(registration = it.registration.copy(descripcion = value)) }

    fun descripcionDuplicada(value: String, nombresServidor: List<Pair<String, String?>>): Boolean =
        TableroDraftStore.hayDescripcionDuplicada(tareaId, inspectionId, value, nombresServidor)

    fun setUbicacion(value: String) =
        cambiar { it.copy(registration = it.registration.copy(ubicacion = value)) }

    fun setNivel(value: String) =
        cambiar { it.copy(registration = it.registration.copy(nivel = value)) }

    fun setTamanioTablero(value: String) =
        cambiar { it.copy(registration = it.registration.copy(tamanioTablero = value)) }

    fun setPreviousPhoto(index: Int, path: String?) =
        cambiar { st ->
            st.copy(
                previousPhotos = st.previousPhotos.map {
                    if (it.index == index) it.copy(path = path) else it
                }
            )
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

    fun setPostPhoto(index: Int, path: String?) =
        cambiar { st ->
            st.copy(
                postPhotos = st.postPhotos.map {
                    if (it.index == index) it.copy(path = path) else it
                }
            )
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

    fun validate(phase: Int): ValidationResult = TrifasicoValidation.validate(_state.value, phase)

    fun finalize() {
        cambiar { it.copy(finished = true) }
    }
}
