package com.aring.movil.ui.panel.common

import com.aring.movil.data.panel.TYPE_MONOFASICO
import com.aring.movil.data.panel.TYPE_TRIFASICO
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun typeLabel(type: String): String = when (type) {
    TYPE_TRIFASICO -> "Tablero Trifásico"
    TYPE_MONOFASICO -> "Tablero Monofásico"
    else -> type
}

fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
