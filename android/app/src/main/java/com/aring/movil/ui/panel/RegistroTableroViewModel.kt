package com.aring.movil.ui.panel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aring.movil.data.ApiException
import com.aring.movil.data.ApiService
import com.aring.movil.data.OfflineSync
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Tecnico
import com.aring.movil.data.panel.MonofasicoState
import com.aring.movil.data.panel.TableroRepository
import com.aring.movil.data.panel.TrifasicoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface EstadoEnvio {
    data object Inactivo : EstadoEnvio
    data object Enviando : EstadoEnvio
    data object Exito : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

class RegistroTableroViewModel : ViewModel() {

    var estado by mutableStateOf<EstadoEnvio>(EstadoEnvio.Inactivo)
        private set

    fun reiniciar() {
        estado = EstadoEnvio.Inactivo
    }

    fun enviarTrifasico(tareaId: Int, tecnico: Tecnico, st: TrifasicoState) {
        if (estado is EstadoEnvio.Enviando) return
        ejecutarEnvio(tareaId, tecnico) { TableroRepository.buildTrifasico(tareaId, tecnico.id, st) }
    }

    fun enviarMonofasico(tareaId: Int, tecnico: Tecnico, st: MonofasicoState) {
        if (estado is EstadoEnvio.Enviando) return
        ejecutarEnvio(tareaId, tecnico) { TableroRepository.buildMonofasico(tareaId, tecnico.id, st) }
    }

    private fun ejecutarEnvio(tareaId: Int, tecnico: Tecnico, construir: () -> JSONObject) {
        viewModelScope.launch {
            estado = EstadoEnvio.Enviando
            try {
                val cuerpo = withContext(Dispatchers.Default) { construir() }
                withContext(Dispatchers.IO) { OfflineSync.guardarInforme(tareaId, tecnico, cuerpo) }
                Repositorio.registrarTableroEnDispositivo(tareaId)
                estado = EstadoEnvio.Exito
            } catch (e: Exception) {
                estado = EstadoEnvio.Error(
                    (e as? ApiException)?.message ?: e.message ?: "Error al enviar el informe."
                )
            }
        }
    }
}
