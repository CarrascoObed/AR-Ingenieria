package com.aring.movil.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.ApiException
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Tecnico
import com.aring.movil.data.inicialesDe
import com.aring.movil.ui.components.Avatar
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.BannerOk
import com.aring.movil.ui.components.BtnPrimario
import com.aring.movil.ui.components.BtnRiesgo
import com.aring.movil.ui.components.BtnSecundario
import com.aring.movil.ui.components.CampoTexto
import com.aring.movil.ui.components.ConfirmarDialog
import com.aring.movil.ui.components.HorizontalSeparador
import com.aring.movil.ui.components.ModalDialog
import com.aring.movil.ui.components.ModalPie
import com.aring.movil.ui.components.SeccionFormulario
import com.aring.movil.ui.components.Tarjeta
import com.aring.movil.ui.components.Vacio
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TecnicosScreen(clave: Int) {
    val scope = rememberCoroutineScope()
    val tecnicos = Repositorio.tecnicos

    var error by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }

    var crearAbierto by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Tecnico?>(null) }
    var eliminando by remember { mutableStateOf<Tecnico?>(null) }

    var fNombre by remember { mutableStateOf("") }
    var fApellido by remember { mutableStateOf("") }
    var fRut by remember { mutableStateOf("") }
    var fPassword by remember { mutableStateOf("") }
    var fContactoNombre by remember { mutableStateOf("") }
    var fContactoApellido by remember { mutableStateOf("") }
    var fContactoTelefono by remember { mutableStateOf("") }

    var eContactoNombre by remember { mutableStateOf("") }
    var eContactoApellido by remember { mutableStateOf("") }
    var eContactoTelefono by remember { mutableStateOf("") }
    var ePassword by remember { mutableStateOf("") }

    fun mostrarAviso(texto: String) {
        aviso = texto
        scope.launch {
            delay(4000)
            aviso = ""
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            "Técnicos",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Administra los técnicos registrados en el sistema.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextoGris,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))

        BannerError(error)
        BannerOk(aviso)
        Spacer(Modifier.height(4.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            BtnPrimario(
                texto = "Crear técnico",
                icono = Icons.Filled.Add
            ) {
                error = ""
                fNombre = ""
                fApellido = ""
                fRut = ""
                fPassword = ""
                fContactoNombre = ""
                fContactoApellido = ""
                fContactoTelefono = ""
                crearAbierto = true
            }
        }
        Spacer(Modifier.height(16.dp))

        if (tecnicos.isEmpty()) {
            Vacio("Aún no hay técnicos creados.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(170.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tecnicos.toList(), key = { it.id }) { t ->
                    Tarjeta {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Avatar(inicialesDe(t.nombre, t.apellido), 46.dp, 18.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${t.nombre} ${t.apellido}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                color = TextoOscuro
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "RUT ${t.rut}",
                                color = TextoGris,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalSeparador()
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BtnSecundario(
                                "Editar",
                                chico = true,
                                icono = Icons.Filled.Edit
                            ) {
                                error = ""
                                editando = t
                                eContactoNombre = t.contactoNombre
                                eContactoApellido = t.contactoApellido
                                eContactoTelefono = t.contactoTelefono
                                ePassword = ""
                            }
                            BtnRiesgo(
                                "Eliminar",
                                chico = true,
                                icono = Icons.Filled.Delete
                            ) {
                                error = ""
                                eliminando = t
                            }
                        }
                    }
                }
            }
        }
    }

    if (crearAbierto) {
        ModalDialog("Crear técnico", onCerrar = { crearAbierto = false }) {
            SeccionFormulario("Datos del técnico") {
                CampoTexto("Nombre del técnico", fNombre, { fNombre = it }, "Ej: Juan")
                Spacer(Modifier.height(10.dp))
                CampoTexto("Apellido del técnico", fApellido, { fApellido = it }, "Ej: Pérez")
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    "RUT del técnico",
                    fRut,
                    { fRut = it },
                    "Ej: 12345678-9",
                    keyboardType = KeyboardType.Number,
                    capitalization = KeyboardCapitalization.None
                )
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    "Contraseña del técnico",
                    fPassword,
                    { fPassword = it },
                    "Asignar contraseña",
                    password = true
                )
            }
            Spacer(Modifier.height(12.dp))
            SeccionFormulario("Datos de contacto de emergencia") {
                CampoTexto("Nombre del contacto", fContactoNombre, { fContactoNombre = it }, "Ej: Carlos")
                Spacer(Modifier.height(10.dp))
                CampoTexto("Apellido del contacto", fContactoApellido, { fContactoApellido = it }, "Ej: Pérez")
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    "Número telefónico",
                    fContactoTelefono,
                    { fContactoTelefono = it },
                    "Ej: +56 9 1234 5678",
                    keyboardType = KeyboardType.Phone,
                    capitalization = KeyboardCapitalization.None
                )
            }
            Spacer(Modifier.height(12.dp))
            val completo = listOf(
                fNombre, fApellido, fRut, fPassword, fContactoNombre, fContactoApellido, fContactoTelefono
            ).all { it.isNotBlank() }
            ModalPie(
                izquierda = { BtnSecundario("Cancelar") { crearAbierto = false } },
                derecha = {
                    BtnPrimario(
                        "Crear técnico",
                        enabled = completo,
                        icono = Icons.Filled.Add
                    ) {
                        scope.launch {
                            Repositorio.crearTecnico(
                                fNombre, fApellido, fRut, fPassword,
                                fContactoNombre, fContactoApellido, fContactoTelefono
                            )
                            crearAbierto = false
                            mostrarAviso("Técnico creado correctamente.")
                        }
                    }
                }
            )
        }
    }

    editando?.let { t ->
        ModalDialog("Editar técnico", onCerrar = { editando = null }) {
            Text(
                "Editar ${t.nombre} ${t.apellido} (RUT ${t.rut})",
                color = TextoGris,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            SeccionFormulario("Datos de contacto de emergencia") {
                CampoTexto("Nombre del contacto", eContactoNombre, { eContactoNombre = it })
                Spacer(Modifier.height(10.dp))
                CampoTexto("Apellido del contacto", eContactoApellido, { eContactoApellido = it })
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    "Número telefónico",
                    eContactoTelefono,
                    { eContactoTelefono = it },
                    keyboardType = KeyboardType.Phone,
                    capitalization = KeyboardCapitalization.None
                )
            }
            Spacer(Modifier.height(12.dp))
            SeccionFormulario("Contraseña del técnico") {
                CampoTexto(
                    "Nueva contraseña",
                    ePassword,
                    { ePassword = it },
                    "Escriba la nueva contraseña",
                    password = true
                )
            }
            Spacer(Modifier.height(12.dp))
            val completo = listOf(eContactoNombre, eContactoApellido, eContactoTelefono, ePassword)
                .all { it.isNotBlank() }
            ModalPie(
                izquierda = { BtnSecundario("Cancelar") { editando = null } },
                derecha = {
                    BtnPrimario(
                        "Guardar cambios",
                        enabled = completo
                    ) {
                        scope.launch {
                            Repositorio.actualizarTecnico(
                                t.id, eContactoNombre, eContactoApellido, eContactoTelefono, ePassword
                            )
                            editando = null
                            mostrarAviso("Técnico actualizado correctamente.")
                        }
                    }
                }
            )
        }
    }

    eliminando?.let { t ->
        ConfirmarDialog(
            titulo = "Eliminar técnico",
            mensaje = "¿Seguro que deseas eliminar a ${t.nombre} ${t.apellido}? " +
                "Esta acción no se puede deshacer.",
            onConfirmar = {
                scope.launch {
                    try {
                        Repositorio.eliminarTecnico(t.id)
                        eliminando = null
                        mostrarAviso("Técnico eliminado correctamente.")
                    } catch (e: ApiException) {
                        eliminando = null
                        error = e.message ?: "No se pudo eliminar el técnico."
                    } catch (e: Exception) {
                        eliminando = null
                        error = "No se pudo eliminar el técnico."
                    }
                }
            },
            onCancelar = { eliminando = null }
        )
    }
}
