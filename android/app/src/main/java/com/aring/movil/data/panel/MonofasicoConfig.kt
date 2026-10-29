package com.aring.movil.data.panel

/**
 * Configuración EXCLUSIVA del tablero monofásico.
 * No debe utilizarse en el flujo trifásico.
 */
object MonofasicoConfig {

    const val TYPE = "MONOFASICO"

    val variables: List<VariableDef> = listOf(
        VariableDef("mono_corriente", "Corriente I", "Amp"),
        VariableDef("mono_vfn", "Voltaje F - N", "V"),
        VariableDef("mono_vnt", "Voltaje N - T", "V"),
        VariableDef("mono_termografia", "Termografía", "°C")
    )

    val checklist: List<ChecklistDef> = listOf(
        ChecklistDef("mono_c1", "Estado de anclaje / panelería / estructura"),
        ChecklistDef("mono_c2", "Rotulado"),
        ChecklistDef("mono_c3", "Barras de distribución"),
        ChecklistDef("mono_c4", "Barras de tierra"),
        ChecklistDef("mono_c5", "Orden de conexionado"),
        ChecklistDef("mono_c6", "Conductores (estructura), temperatura"),
        ChecklistDef("mono_c7", "Acrílicos protectores"),
        ChecklistDef("mono_c8", "Terminales y conexionado"),
        ChecklistDef("mono_c9", "Termografía")
    )
}
