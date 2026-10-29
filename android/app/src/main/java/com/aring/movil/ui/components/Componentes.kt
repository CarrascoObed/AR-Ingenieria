package com.aring.movil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aring.movil.R
import com.aring.movil.ui.theme.Amarillo
import com.aring.movil.ui.theme.Blanco
import com.aring.movil.ui.theme.BordeBannerOk
import com.aring.movil.ui.theme.BordeBannerError
import com.aring.movil.ui.theme.BordeSuave
import com.aring.movil.ui.theme.FondoBannerError
import com.aring.movil.ui.theme.FondoBannerOk
import com.aring.movil.ui.theme.FondoChip
import com.aring.movil.ui.theme.FondoChipAmarillo
import com.aring.movil.ui.theme.FondoChipRojo
import com.aring.movil.ui.theme.FondoChipVerde
import com.aring.movil.ui.theme.FondoGris
import com.aring.movil.ui.theme.GrisDeshabilitado
import com.aring.movil.ui.theme.RojoError
import com.aring.movil.ui.theme.TextoChipAmarillo
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.VerdeOk
import com.aring.movil.ui.theme.Violeta
import com.aring.movil.ui.theme.VioletaClaro
import com.aring.movil.ui.theme.VioletaOscuro

@Composable
fun LogoRayo(tamanio: Dp = 56.dp) {
    Image(
        painter = painterResource(R.drawable.logo_transparent),
        contentDescription = "Logo AR-Ingeniería",
        modifier = Modifier.size(tamanio)
    )
}

@Composable
fun IconoOjo(mostrando: Boolean, tint: Color = TextoGris, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val ancho = size.width
        val alto = size.height
        val centro = Offset(ancho / 2f, alto / 2f)
        val rx = ancho * 0.38f
        val ry = alto * 0.25f
        val trazo = ancho * 0.08f
        drawOval(
            color = tint,
            topLeft = Offset(centro.x - rx, centro.y - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = trazo)
        )
        drawCircle(color = tint, radius = ancho * 0.085f, center = centro)
        if (mostrando) {
            drawLine(
                color = tint,
                start = Offset(ancho * 0.16f, alto * 0.84f),
                end = Offset(ancho * 0.84f, alto * 0.16f),
                strokeWidth = trazo
            )
        }
    }
}

@Composable
fun Avatar(iniciales: String, size: Dp = 40.dp, fontSize: TextUnit = 15.sp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Violeta),
        contentAlignment = Alignment.Center
    ) {
        Text(
            iniciales.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize
        )
    }
}

@Composable
fun ChipEstado(estado: String) {
    val (fondo, texto, punto) = when (estado) {
        "No iniciada" -> Triple(FondoChipRojo, RojoError, RojoError)
        "En proceso" -> Triple(FondoChipAmarillo, TextoChipAmarillo, Amarillo)
        "Finalizada" -> Triple(FondoChipVerde, VerdeOk, VerdeOk)
        else -> Triple(FondoChip, TextoGris, TextoGris)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(fondo)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(punto)
        )
        Text(estado, color = texto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ChipTipo(tipo: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(VioletaClaro)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tipo, color = VioletaOscuro, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ChipLegenda(color: Color, texto: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(FondoChip)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(texto, color = TextoGris, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Tarjeta(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Blanco)
            .border(1.dp, BordeSuave, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        content()
    }
}

@Composable
fun BannerError(texto: String?) {
    if (texto.isNullOrBlank()) return
    Text(
        texto,
        modifier = Modifier
            .fillMaxWidth()
            .background(FondoBannerError)
            .border(1.dp, BordeBannerError, RoundedCornerShape(10.dp))
            .padding(12.dp),
        color = RojoError,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun BannerOk(texto: String?) {
    if (texto.isNullOrBlank()) return
    Text(
        texto,
        modifier = Modifier
            .fillMaxWidth()
            .background(FondoBannerOk)
            .border(1.dp, BordeBannerOk, RoundedCornerShape(10.dp))
            .padding(12.dp),
        color = VerdeOk,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun Cargando(texto: String = "Cargando…") {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = TextoGris,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(10.dp))
        Text(texto, color = TextoGris)
    }
}

@Composable
fun Vacio(texto: String) {
    Text(
        texto,
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp),
        color = TextoGris,
        textAlign = TextAlign.Center
    )
}

@Composable
fun BtnPrimario(
    texto: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false,
    chico: Boolean = false,
    icono: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val padH = if (chico) 14.dp else 18.dp
    val padV = if (chico) 8.dp else 12.dp
    val fondo = if (enabled) {
        Brush.linearGradient(listOf(Violeta, VioletaOscuro))
    } else {
        SolidColor(GrisDeshabilitado)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(fondo)
            .clickable(enabled = enabled && !cargando, onClick = onClick)
            .padding(horizontal = padH, vertical = padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (icono != null) {
                Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(
                texto,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (chico) 13.sp else 14.sp
            )
        }
    }
}

@Composable
fun BtnSecundario(
    texto: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    chico: Boolean = false,
    icono: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val padH = if (chico) 12.dp else 18.dp
    val padV = if (chico) 7.dp else 12.dp
    val color = if (enabled) Violeta else TextoGris
    val borde = if (enabled) Violeta else GrisDeshabilitado
    Box(
        modifier = modifier
            .clip(shape)
            .background(Blanco)
            .border(1.5.dp, borde, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = padH, vertical = padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            }
            Text(
                texto,
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (chico) 13.sp else 14.sp
            )
        }
    }
}

@Composable
fun BtnRiesgo(
    texto: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    chico: Boolean = false,
    icono: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val padH = if (chico) 12.dp else 18.dp
    val padV = if (chico) 7.dp else 12.dp
    val color = if (enabled) RojoError else TextoGris
    val borde = if (enabled) RojoError else GrisDeshabilitado
    Box(
        modifier = modifier
            .clip(shape)
            .background(Blanco)
            .border(1.5.dp, borde, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = padH, vertical = padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            }
            Text(
                texto,
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (chico) 13.sp else 14.sp
            )
        }
    }
}

@Composable
fun ModalDialog(titulo: String, onCerrar: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onCerrar) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Blanco,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(24.dp)
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        titulo,
                        modifier = Modifier.align(Alignment.Center),
                        color = TextoOscuro,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = onCerrar,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(38.dp)
                    ) {
                        Icon(Icons.Filled.Close, "Cerrar", tint = TextoGris)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ConfirmarDialog(
    titulo: String,
    mensaje: String,
    confirmarTexto: String = "Eliminar",
    cancelarTexto: String = "Cancelar",
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    Dialog(onDismissRequest = onCancelar) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Blanco,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    titulo,
                    modifier = Modifier.fillMaxWidth(),
                    color = TextoOscuro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    mensaje,
                    modifier = Modifier.fillMaxWidth(),
                    color = TextoGris,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BtnSecundario(cancelarTexto, onClick = onCancelar)
                    BtnRiesgo(confirmarTexto, onClick = onConfirmar)
                }
            }
        }
    }
}

@Composable
fun SeccionFormulario(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FondoGris)
            .border(1.dp, BordeSuave, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            titulo,
            modifier = Modifier.fillMaxWidth(),
            color = VioletaOscuro,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun ModalPie(
    izquierda: (@Composable () -> Unit)? = null,
    derecha: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        izquierda?.let { it() }
        derecha()
    }
}

@Composable
fun CampoTexto(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    password: Boolean = false,
    multiline: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    var enfocado by remember { mutableStateOf(false) }
    var mostrar by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val borde = if (enfocado) Violeta else BordeSuave
    val halo = if (enfocado) Violeta.copy(alpha = 0.15f) else Color.Transparent

    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextoOscuro)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(halo)
                .padding(3.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(Blanco)
                    .border(1.5.dp, borde, RoundedCornerShape(7.dp))
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .padding(end = if (password) 46.dp else 0.dp),
                    singleLine = !multiline,
                    minLines = if (multiline) 3 else 1,
                    textStyle = TextStyle(color = TextoOscuro, fontSize = 15.sp, lineHeight = 20.sp),
                    visualTransformation = if (password && !mostrar) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        capitalization = if (password) KeyboardCapitalization.None else capitalization,
                        imeAction = if (multiline) ImeAction.Default else ImeAction.Next
                    ),
                    cursorBrush = SolidColor(Violeta),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    placeholder,
                                    color = TextoGris,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                            }
                            inner()
                        }
                    }
                )
                if (password) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { mostrar = !mostrar },
                        contentAlignment = Alignment.Center
                    ) {
                        IconoOjo(
                            mostrando = mostrar,
                            tint = TextoGris,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CampoSelect(
    label: String,
    opciones: List<Pair<String, String>>,
    valor: String,
    onSeleccionar: (String) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    var anchoCampo by remember { mutableStateOf(0.dp) }
    val densidad = LocalDensity.current
    val seleccion = opciones.find { it.first == valor }
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextoOscuro)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { anchoCampo = with(densidad) { it.width.toDp() } }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Blanco)
                    .border(1.5.dp, BordeSuave, RoundedCornerShape(10.dp))
                    .clickable { abierto = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    seleccion?.second ?: "Seleccione una opción",
                    color = if (seleccion == null) TextoGris else TextoOscuro,
                    fontSize = 15.sp
                )
            }
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = TextoGris,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
            DropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false },
                modifier = Modifier.width(if (anchoCampo > 0.dp) anchoCampo else 300.dp)
            ) {
                opciones.forEach { (k, v) ->
                    DropdownMenuItem(
                        text = { Text(v) },
                        onClick = {
                            onSeleccionar(k)
                            abierto = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalSeparador() {
    HorizontalDivider(color = FondoGris)
}
