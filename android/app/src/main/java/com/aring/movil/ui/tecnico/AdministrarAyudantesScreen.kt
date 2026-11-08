package com.aring.movil.ui.tecnico

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.ApiException
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Tarea
import com.aring.movil.data.Tecnico
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.BtnPrimario
import com.aring.movil.ui.components.BtnSecundario
import com.aring.movil.ui.components.CampoSelect
import com.aring.movil.ui.components.SeccionFormulario
import com.aring.movil.ui.components.Tarjeta
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.VioletaOscuro
import kotlinx.coroutines.launch

@Composable
fun AdministrarAyudantesScreen(
    tecnico: Tecnico,
    tarea: Tarea,
    onGuardado: (Tarea) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val ayudantes = remember(tarea.id) {
        mutableStateListOf<String>().apply {
            addAll(tarea.tecnicoAyudantesIds.map { it.toString() })
            if (isEmpty()) add("")
        }
    }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun opcionesPara(indice: Int): List<Pair<String, String>> {
        val yaSeleccionados = ayudantes.mapIndexedNotNull { posicion, valor ->
            if (posicion != indice) valor.toIntOrNull() else null
        }.toSet()
        return Repositorio.tecnicos
            .filter { it.id != tecnico.id && it.id !in yaSeleccionados }
            .map { it.id.toString() to "${it.nombre} ${it.apellido}" }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            "Administrar ayudantes",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tarea.codigo,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = VioletaOscuro,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Puedes agregar o quitar hasta cinco ayudantes mientras la tarea siga en proceso.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextoGris,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(18.dp))

        SeccionFormulario("Ayudantes asignados a la tarea") {
            ayudantes.forEachIndexed { indice, _ ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        CampoSelect(
                            label = "Ayudante ${indice + 1}",
                            opciones = opcionesPara(indice),
                            valor = ayudantes[indice],
                            onSeleccionar = { ayudantes[indice] = it }
                        )
                    }
                    IconButton(onClick = { ayudantes.removeAt(indice) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar ayudante", tint = TextoGris)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (ayudantes.size < 5) {
                BtnSecundario(
                    texto = "Agregar ayudante",
                    modifier = Modifier.fillMaxWidth(),
                    icono = Icons.Filled.Add
                ) {
                    ayudantes.add("")
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Tarjeta {
            Text(
                "Los cambios se guardarán en el servidor y los ayudantes agregados recibirán una notificación. " +
                    "Requiere conexión a internet. Al cerrar la tarea ya no se podrán modificar.",
                color = TextoGris,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(16.dp))

        if (error.isNotBlank()) {
            BannerError(error)
            Spacer(Modifier.height(12.dp))
        }

        BtnPrimario(
            texto = "Guardar ayudantes",
            modifier = Modifier.fillMaxWidth(),
            cargando = guardando
        ) {
            val ids = ayudantes.mapNotNull { it.toIntOrNull() }.distinct()
            if (ayudantes.any { it.isNotBlank() && it.toIntOrNull() == null } || ids.size > 5) {
                error = "Selecciona ayudantes válidos antes de guardar."
                return@BtnPrimario
            }
            scope.launch {
                guardando = true
                error = ""
                try {
                    val actualizada = Repositorio.actualizarAyudantes(tarea.id, ids)
                    onGuardado(actualizada)
                } catch (e: ApiException) {
                    error = e.message ?: "No se pudieron guardar los ayudantes."
                } catch (e: Exception) {
                    error = e.message ?: "No se pudieron guardar los ayudantes."
                } finally {
                    guardando = false
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BtnSecundario(
            texto = "Cancelar",
            modifier = Modifier.fillMaxWidth(),
            onClick = { onGuardado(tarea) }
        )
    }
}
