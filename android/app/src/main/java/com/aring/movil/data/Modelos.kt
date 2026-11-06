package com.aring.movil.data

data class Tecnico(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val rut: String,
    val password: String,
    val contactoNombre: String,
    val contactoApellido: String,
    val contactoTelefono: String,
)

data class Tarea(
    val id: Int,
    val codigo: String,
    val tipo: String,
    val estado: String,
    val nombreRecinto: String,
    val direccion: String,
    val descripcion: String,
    val tecnicoCabeceraId: Int,
    val tecnicoAyudantesIds: List<Int> = emptyList(),
    val fechaInicio: String? = null,
    val tablerosRealizados: Int = 0,
    val sincronizacionPendiente: Boolean = false,
) {
    fun esCabecera(tecnicoId: Int): Boolean = tecnicoCabeceraId == tecnicoId

    fun esInvitado(tecnicoId: Int): Boolean = tecnicoAyudantesIds.contains(tecnicoId)

    fun esDeTecnico(tecnicoId: Int): Boolean = esCabecera(tecnicoId) || esInvitado(tecnicoId)
}

fun inicialesDe(nombre: String, apellido: String): String =
    (nombre.firstOrNull()?.toString() ?: "") + (apellido.firstOrNull()?.toString() ?: "")
