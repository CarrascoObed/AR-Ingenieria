package com.aring.movil.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aring.movil.data.Prefs
import com.aring.movil.data.Repositorio
import com.aring.movil.data.Sesion
import com.aring.movil.ui.components.BannerError
import com.aring.movil.ui.components.BtnPrimario
import com.aring.movil.ui.components.CampoTexto
import com.aring.movil.ui.components.LogoRayo
import com.aring.movil.ui.theme.Blanco
import com.aring.movil.ui.theme.BordeSuave
import com.aring.movil.ui.theme.TextoFooter
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.Violeta
import com.aring.movil.ui.theme.VioletaMedio
import com.aring.movil.ui.theme.VioletaOscuro
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onIngresar: (Sesion) -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var recordar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val cred = Prefs.leerCredenciales()
        if (cred != null) {
            usuario = cred.first
            password = cred.second
            recordar = true
        }
    }

    val completo = usuario.isNotBlank() && password.isNotBlank()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(VioletaMedio, Violeta, VioletaOscuro))
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.width * 0.5f,
                center = Offset(size.width * 1.05f, size.height * 0.08f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.0f, size.height * 0.95f)
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Blanco.copy(alpha = 0.96f),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LogoRayo(64.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "AR-Ingeniería",
                            color = VioletaOscuro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Gestión de mantenimiento eléctrico",
                            color = TextoGris,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    CampoTexto(
                        label = "Usuario",
                        value = usuario,
                        onValueChange = { usuario = it },
                        placeholder = "Ingrese su usuario",
                        keyboardType = KeyboardType.Number,
                        capitalization = KeyboardCapitalization.None
                    )

                    Spacer(Modifier.height(14.dp))

                    CampoTexto(
                        label = "Contraseña",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Ingrese su contraseña",
                        password = true,
                        keyboardType = KeyboardType.NumberPassword
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        Modifier
                            .clickable { recordar = !recordar }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (recordar) Violeta else Blanco)
                                .border(
                                    1.5.dp,
                                    if (recordar) Violeta else BordeSuave,
                                    RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (recordar) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Recordar credenciales",
                            color = TextoOscuro,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    if (error.isNotEmpty()) {
                        BannerError(error)
                        Spacer(Modifier.height(12.dp))
                    }

                    BtnPrimario(
                        texto = "Iniciar sesión",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = completo,
                        cargando = cargando,
                        icono = Icons.Filled.Check
                    ) {
                        scope.launch {
                            cargando = true
                            error = ""
                            try {
                                val sesion = Repositorio.login(usuario.trim(), password)
                                if (recordar) {
                                    Prefs.guardarCredenciales(usuario.trim(), password)
                                } else {
                                    Prefs.borrarCredenciales()
                                }
                                onIngresar(sesion)
                            } catch (e: Exception) {
                                error = e.message ?: "Ocurrió un error inesperado."
                            } finally {
                                cargando = false
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "© 2026 · Creado por Eliezer Carrasco para AR-Ingeniería · Versión 1.0 · " +
                    "Todos los derechos reservados.",
                modifier = Modifier.fillMaxWidth(),
                color = TextoFooter,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
