package com.aring.movil.data

data class Admin(
    val token: String,
    val nombre: String,
    val apellido: String,
    val iniciales: String,
    val username: String,
)

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

data class Cliente(
    val id: Int,
    val direccion: String,
    val nombreRecinto: String,
)

sealed class Sesion {
    data class AdminSesion(val admin: Admin) : Sesion()
    data class TecnicoSesion(val tecnico: Tecnico) : Sesion()
}

data class Notificacion(
    val id: Int,
    val mensaje: String,
    val fecha: String,
    val leida: Boolean,
    val tareaId: Int? = null,
    val tecnicoDestinoId: Int? = null,
)

data class DashboardStats(
    val enProceso: Int,
    val finalizadas: Int,
    val noIniciadas: Int,
)

fun inicialesDe(nombre: String, apellido: String): String =
    (nombre.firstOrNull()?.toString() ?: "") + (apellido.firstOrNull()?.toString() ?: "")
