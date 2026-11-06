@file:OptIn(ExperimentalMaterial3Api::class)

package com.aring.movil.ui.panel.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aring.movil.camera.PhotoBus
import com.aring.movil.camera.PhotoResult
import com.aring.movil.data.panel.ChecklistStatus
import com.aring.movil.data.panel.label
import com.aring.movil.ui.theme.Amarillo
import com.aring.movil.ui.theme.BordeSuave
import com.aring.movil.ui.theme.FondoGris
import com.aring.movil.ui.theme.RojoError
import com.aring.movil.ui.theme.TextoGris
import com.aring.movil.ui.theme.TextoOscuro
import com.aring.movil.ui.theme.VerdeOk
import com.aring.movil.ui.theme.Violeta
import com.aring.movil.ui.theme.VioletaClaro
import com.aring.movil.ui.theme.VioletaOscuro
import kotlinx.coroutines.flow.collect

val StepLabels = listOf(
    "Registro",
    "Evidencias",
    "Variables",
    "Checklist",
    "Evidencias",
    "Observaciones"
)

@Composable
fun Spacing8() {
    Spacer(Modifier.height(8.dp))
}

@Composable
fun Spacing12() {
    Spacer(Modifier.height(12.dp))
}

@Composable
fun Spacing16() {
    Spacer(Modifier.height(16.dp))
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextoOscuro)
}

@Composable
fun SectionSubtitle(text: String) {
    Text(text, fontSize = 14.sp, color = TextoGris)
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Violeta,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ProgressStepper(currentStep: Int, labels: List<String>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = "Paso $currentStep de ${labels.size}",
            fontSize = 13.sp,
            color = TextoGris,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            labels.forEachIndexed { index, label ->
                val step = index + 1
                val isCurrent = step == currentStep
                val isDone = step < currentStep
                val circleColor = when {
                    isCurrent -> Violeta
                    isDone -> VioletaClaro
                    else -> BordeSuave
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(circleColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = VioletaOscuro,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "$step",
                                color = if (isCurrent) Color.White else TextoGris,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = if (isCurrent) Violeta else TextoGris,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PhaseScreen(
    step: Int,
    onBack: () -> Unit,
    topBarTitle: String,
    content: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = FondoGris,
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextoOscuro) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = TextoOscuro)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                ) {
                    footer()
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ProgressStepper(step, StepLabels)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun FormTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { if (placeholder != null) Text(placeholder, color = TextoGris) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (keyboardType == KeyboardType.Text) capitalization else KeyboardCapitalization.None
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Violeta,
            unfocusedBorderColor = BordeSuave,
            focusedLabelColor = Violeta
        )
    )
}

@Composable
fun MultilineField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        placeholder = { Text(placeholder, color = TextoGris) },
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Violeta,
            unfocusedBorderColor = BordeSuave
        ),
        textStyle = TextStyle(fontSize = 15.sp, color = TextoOscuro)
    )
}

@Composable
fun ObservacionesSection(
    observaciones: List<String>,
    onValueChange: (index: Int, value: String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (index: Int) -> Unit
) {
    val lista = if (observaciones.isEmpty()) listOf("") else observaciones
    lista.forEachIndexed { index, texto ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Observación ${index + 1}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoOscuro,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onRemove(index) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp), tint = Violeta)
                Spacer(Modifier.width(4.dp))
                Text("Quitar", color = Violeta, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        MultilineField(
            placeholder = "Escriba aquí sus observaciones...",
            value = texto,
            onValueChange = { onValueChange(index, it) }
        )
        Spacer(Modifier.height(16.dp))
    }
    OutlinedButton(
        onClick = onAdd,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = Violeta)
        Spacer(Modifier.width(8.dp))
        Text("Agregar observación", color = Violeta, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TamanioTableroField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val options = listOf("Pequeño", "Mediano", "Grande", "Especial")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tamaño de tablero") },
            placeholder = { Text("Seleccione...", color = TextoGris) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violeta,
                unfocusedBorderColor = BordeSuave,
                focusedLabelColor = Violeta
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 15.sp, color = TextoOscuro) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ErrorBanner(message: String?) {
    if (message != null) {
        Surface(
            color = RojoError.copy(alpha = 0.10f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = RojoError,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(message, color = RojoError, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PhotoSection(
    photoPath: String?,
    onTakePhoto: () -> Unit,
    onViewPhoto: () -> Unit,
    onRetakePhoto: () -> Unit,
    onPhotoEdited: (String) -> Unit,
    onPhotoDeleted: () -> Unit
) {
    var anotando by remember { mutableStateOf(false) }
    if (photoPath == null) {
        OutlinedButton(
            onClick = onTakePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Violeta)
            Spacer(Modifier.width(8.dp))
            Text("Tomar foto", color = Violeta, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilePathImage(
                path = photoPath,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Fotografía tomada",
                    color = VerdeOk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onViewPhoto, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.Visibility, contentDescription = "Ver foto", modifier = Modifier.size(22.dp), tint = Violeta)
                    }
                    IconButton(onClick = onRetakePhoto, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.Replay, contentDescription = "Tomar foto nuevamente", modifier = Modifier.size(22.dp), tint = Violeta)
                    }
                    IconButton(onClick = { anotando = true }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Dibujar sobre la foto", tint = Violeta, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onPhotoDeleted, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Eliminar foto", tint = RojoError, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
    if (anotando && photoPath != null) {
        AnotadorFotoDialog(
            path = photoPath,
            onConfirm = { nuevaRuta ->
                anotando = false
                onPhotoEdited(nuevaRuta)
            },
            onDismiss = { anotando = false }
        )
    }
}

@Composable
fun PhotoViewerDialog(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            FilePathImage(
                path = path,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}

@Composable
fun MeasurementCard(
    label: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit,
    photoPath: String?,
    onTakePhoto: () -> Unit,
    onViewPhoto: () -> Unit,
    onRetakePhoto: () -> Unit,
    onPhotoEdited: (String) -> Unit,
    onPhotoDeleted: () -> Unit = {},
    showValueInput: Boolean = true,
    showError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Speed, contentDescription = null, tint = Violeta, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextoOscuro,
                    modifier = Modifier.weight(1f)
                )
            }
            if (showValueInput) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        isError = showError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violeta,
                            unfocusedBorderColor = BordeSuave
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = unit,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextoGris,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
                if (showError) {
                    Spacer(Modifier.height(4.dp))
                    Text("Requerido", color = RojoError, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            PhotoSection(photoPath, onTakePhoto, onViewPhoto, onRetakePhoto, onPhotoEdited, onPhotoDeleted)
        }
    }
}

@Composable
fun ChecklistCard(
    label: String,
    status: ChecklistStatus?,
    onStatusSelected: (ChecklistStatus) -> Unit,
    observation: String,
    onObservationChange: (String) -> Unit,
    photoPath: String?,
    onTakePhoto: () -> Unit,
    onViewPhoto: () -> Unit,
    onRetakePhoto: () -> Unit,
    onPhotoEdited: (String) -> Unit,
    onPhotoDeleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextoOscuro)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChecklistStatus.entries.forEach { option ->
                    val selected = status == option
                    FilterChip(
                        selected = selected,
                        onClick = { onStatusSelected(option) },
                        label = {
                            Text(
                                option.label(),
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Violeta,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F0F7),
                            labelColor = TextoOscuro
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Violeta
                        )
                    )
                }
            }
            if (status == ChecklistStatus.CR) {
                if (photoPath == null) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Amarillo, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Evidencia fotográfica requerida",
                            color = Amarillo,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                PhotoSection(photoPath, onTakePhoto, onViewPhoto, onRetakePhoto, onPhotoEdited, onPhotoDeleted)
                Spacer(Modifier.height(12.dp))
                Text("Observación de este punto", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextoOscuro)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = observation,
                    onValueChange = onObservationChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describa el detalle encontrado...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violeta,
                        unfocusedBorderColor = BordeSuave,
                        focusedLabelColor = Violeta
                    )
                )
            }
        }
    }
}

@Composable
fun ChecklistObservationsSection(items: List<Pair<String, String>>) {
    if (items.isEmpty()) return
    Column {
        Text("Observaciones del checklist", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextoOscuro)
        Spacer(Modifier.height(6.dp))
        Text("Se agregarán al final de las observaciones del informe.", fontSize = 12.sp, color = TextoGris)
        items.forEach { (label, observation) ->
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VioletaClaro),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = VioletaOscuro)
                    Spacer(Modifier.height(4.dp))
                    Text(observation, fontSize = 14.sp, color = TextoOscuro)
                }
            }
        }
    }
}

@Composable
fun PhotoSlotCard(
    title: String,
    subtitle: String?,
    path: String?,
    onTakePhoto: () -> Unit,
    onViewPhoto: () -> Unit,
    onRetakePhoto: () -> Unit,
    onPhotoEdited: (String) -> Unit,
    onPhotoDeleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(VioletaClaro),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Violeta, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextoOscuro)
                    if (subtitle != null) {
                        Text(subtitle, fontSize = 12.sp, color = TextoGris)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PhotoSection(path, onTakePhoto, onViewPhoto, onRetakePhoto, onPhotoEdited, onPhotoDeleted)
        }
    }
}

@Composable
fun CollectPhotoResult(onResult: (PhotoResult) -> Unit) {
    LaunchedEffect(Unit) {
        PhotoBus.result.collect { res ->
            if (res != null) {
                onResult(res)
                PhotoBus.consumeResult()
            }
        }
    }
}
