package com.aring.movil.data.panel

/**
 * Configuración EXCLUSIVA del tablero trifásico.
 * No debe utilizarse en el flujo monofásico.
 */
object TrifasicoConfig {

    const val TYPE = "TRIFASICO"

    val variables: List<VariableDef> = listOf(
        VariableDef("tri_corriente1", "Corriente I (1)", "Amp"),
        VariableDef("tri_corriente2", "Corriente I (2)", "Amp"),
        VariableDef("tri_corriente3", "Corriente I (3)", "Amp"),
        VariableDef("tri_vf1f2", "Voltaje F1 - F2", "V"),
        VariableDef("tri_vf1f3", "Voltaje F1 - F3", "V"),
        VariableDef("tri_vf2f3", "Voltaje F2 - F3", "V"),
        VariableDef("tri_vf1n", "Voltaje F1 - N", "V"),
        VariableDef("tri_vf2n", "Voltaje F2 - N", "V"),
        VariableDef("tri_vf3n", "Voltaje F3 - N", "V"),
        VariableDef("tri_vnt", "Voltaje N - T", "V"),
        VariableDef("tri_temp", "Temperatura", "°C")
    )

    val checklist: List<ChecklistDef> = listOf(
        ChecklistDef("tri_c1", "Estado de chapas / pomeles / estructura"),
        ChecklistDef("tri_c2", "Chasis de tablero"),
        ChecklistDef("tri_c3", "Rotulado"),
        ChecklistDef("tri_c4", "Luces piloto"),
        ChecklistDef("tri_c5", "Barras de distribución"),
        ChecklistDef("tri_c6", "Barras de tierra"),
        ChecklistDef("tri_c7", "Barra de servicio"),
        ChecklistDef("tri_c8", "Orden conexionado"),
        ChecklistDef("tri_c9", "Conductores (estructura), temperatura"),
        ChecklistDef("tri_c10", "Aterizado de puertas y contratapa"),
        ChecklistDef("tri_c11", "Acrílicos protectores"),
        ChecklistDef("tri_c12", "Terminales y conexiones"),
        ChecklistDef("tri_c13", "Cajas moldeadas"),
        ChecklistDef("tri_c14", "Equipo de medida"),
        ChecklistDef("tri_c15", "Condensadores"),
        ChecklistDef("tri_c16", "Contactores"),
        ChecklistDef("tri_c17", "Relé de factor de potencia"),
        ChecklistDef("tri_c18", "Termostato"),
        ChecklistDef("tri_c19", "Ventiladores"),
        ChecklistDef("tri_c20", "Reloj horario"),
        ChecklistDef("tri_c21", "Termografía")
    )
}
