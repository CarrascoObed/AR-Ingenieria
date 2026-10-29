package com.aring.movil.data

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("aring_movil", Context.MODE_PRIVATE)
    }

    fun guardarAdmin(a: Admin) {
        prefs.edit()
            .putString("admin_token", a.token)
            .putString("admin_nombre", a.nombre)
            .putString("admin_apellido", a.apellido)
            .putString("admin_iniciales", a.iniciales)
            .putString("admin_username", a.username)
            .apply()
    }

    fun leerAdmin(): Admin? {
        val nombre = prefs.getString("admin_nombre", null) ?: return null
        return Admin(
            token = prefs.getString("admin_token", "").orEmpty(),
            nombre = nombre,
            apellido = prefs.getString("admin_apellido", "").orEmpty(),
            iniciales = prefs.getString("admin_iniciales", "").orEmpty(),
            username = prefs.getString("admin_username", "").orEmpty(),
        )
    }

    fun borrarAdmin() {
        prefs.edit()
            .remove("admin_token")
            .remove("admin_nombre")
            .remove("admin_apellido")
            .remove("admin_iniciales")
            .remove("admin_username")
            .apply()
    }

    fun guardarTecnico(t: Tecnico) {
        prefs.edit()
            .putInt("tecnico_id", t.id)
            .putString("tecnico_nombre", t.nombre)
            .putString("tecnico_apellido", t.apellido)
            .putString("tecnico_rut", t.rut)
            .putString("tecnico_password", t.password)
            .putString("tecnico_contacto_nombre", t.contactoNombre)
            .putString("tecnico_contacto_apellido", t.contactoApellido)
            .putString("tecnico_contacto_telefono", t.contactoTelefono)
            .apply()
    }

    fun leerTecnico(): Tecnico? {
        if (!prefs.contains("tecnico_id")) return null
        return Tecnico(
            id = prefs.getInt("tecnico_id", 0),
            nombre = prefs.getString("tecnico_nombre", "").orEmpty(),
            apellido = prefs.getString("tecnico_apellido", "").orEmpty(),
            rut = prefs.getString("tecnico_rut", "").orEmpty(),
            password = prefs.getString("tecnico_password", "").orEmpty(),
            contactoNombre = prefs.getString("tecnico_contacto_nombre", "").orEmpty(),
            contactoApellido = prefs.getString("tecnico_contacto_apellido", "").orEmpty(),
            contactoTelefono = prefs.getString("tecnico_contacto_telefono", "").orEmpty(),
        )
    }

    fun borrarTecnico() {
        prefs.edit()
            .remove("tecnico_id")
            .remove("tecnico_nombre")
            .remove("tecnico_apellido")
            .remove("tecnico_rut")
            .remove("tecnico_password")
            .remove("tecnico_contacto_nombre")
            .remove("tecnico_contacto_apellido")
            .remove("tecnico_contacto_telefono")
            .apply()
    }

    fun guardarTokenTecnico(token: String) {
        prefs.edit().putString("tecnico_token", token).apply()
    }

    fun leerTokenTecnico(): String? = prefs.getString("tecnico_token", null)

    fun borrarTokenTecnico() {
        prefs.edit().remove("tecnico_token").apply()
    }

    fun guardarCredenciales(usuario: String, password: String) {
        prefs.edit()
            .putString("cred_user", usuario)
            .putString("cred_pass", password)
            .apply()
    }

    fun borrarCredenciales() {
        prefs.edit()
            .remove("cred_user")
            .remove("cred_pass")
            .apply()
    }

    fun leerCredenciales(): Pair<String, String>? {
        val usuario = prefs.getString("cred_user", null) ?: return null
        val password = prefs.getString("cred_pass", "") ?: ""
        return usuario to password
    }

    fun guardarOcultasFinalizadas(ids: List<Int>) {
        prefs.edit()
            .putStringSet("ocultas_finalizadas", ids.map { it.toString() }.toSet())
            .apply()
    }

    fun leerOcultasFinalizadas(): Set<String> =
        prefs.getStringSet("ocultas_finalizadas", emptySet()) ?: emptySet()
}
