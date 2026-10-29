package com.aring.movil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = Violeta,
    onPrimary = Blanco,
    primaryContainer = VioletaClaro,
    onPrimaryContainer = VioletaOscuro,
    secondary = VioletaOscuro,
    onSecondary = Blanco,
    background = FondoGris,
    onBackground = TextoOscuro,
    surface = Blanco,
    onSurface = TextoOscuro,
    surfaceVariant = FondoChip,
    onSurfaceVariant = TextoGris,
    error = RojoError,
    onError = Blanco,
)

private val Tipografia = Typography()

@Composable
fun ARIngMovilTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Tipografia,
        content = content
    )
}
