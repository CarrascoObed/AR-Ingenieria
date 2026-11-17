package com.aring.movil.data.panel

object ChecklistCommentSuggestions {

    private val conductores = listOf(
        "Conductores sin férulas.",
        "Conductores sin aislamiento.",
        "Conductores con temperatura elevada.",
        "Conductores con evidencia de derretimiento.",
        "Conductores con presencia de sulfato."
    )

    private val porItem = mapOf(
        "tri_c1" to listOf("Rotas.", "Rodadas."),
        "mono_c1" to listOf("Rotas.", "Rodadas."),
        "tri_c2" to listOf("No aterrizado.", "En mal estado.", "Presencia de óxido."),
        "tri_c3" to listOf("No rotulado.", "Rotulado ilegible.", "Rotulado incoherente."),
        "mono_c2" to listOf("No rotulado.", "Rotulado ilegible.", "Rotulado incoherente."),
        "tri_c4" to listOf("Luz piloto (R-S-T) dañada.", "Luz piloto (R-S-T) sin fusibles."),
        "tri_c5" to conductores,
        "tri_c6" to conductores,
        "mono_c3" to conductores,
        "mono_c4" to conductores,
        "tri_c7" to conductores,
        "tri_c8" to conductores,
        "mono_c5" to conductores,
        "tri_c9" to conductores,
        "mono_c6" to conductores,
        "tri_c10" to listOf(
            "Tapa no aterrizada.",
            "Contratapa no aterrizada.",
            "Tapa y contratapa no aterrizadas.",
            "Falta perno de aterrizamiento."
        ),
        "tri_c11" to listOf(
            "Acrílicos quebrados.",
            "Faltan acrílicos.",
            "Acrílicos sin señalización de riesgo eléctrico."
        ),
        "mono_c7" to listOf(
            "Acrílicos quebrados.",
            "Faltan acrílicos.",
            "Acrílicos sin señalización de riesgo eléctrico."
        ),
        "tri_c12" to listOf(
            "Faltan terminales.",
            "Terminales sin aislamiento.",
            "Terminales subdimensionados."
        ),
        "mono_c8" to listOf(
            "Faltan terminales.",
            "Terminales sin aislamiento.",
            "Terminales subdimensionados."
        ),
        "tri_c13" to listOf(
            "Presencia de sulfato.",
            "Conductores sin aislamiento.",
            "Conductores sin terminales.",
            "Temperatura elevada."
        ),
        "tri_c14" to listOf("Equipo con lectura errónea.", "Equipo dañado."),
        "tri_c15" to listOf(
            "Temperatura elevada.",
            "Desconectados.",
            "Quemados.",
            "Reventados.",
            "Contactores quemados."
        ),
        "tri_c16" to listOf("Quemados.", "Desconectados."),
        "tri_c18" to listOf("Falta termostato.", "Sin termostato.", "Desconectado.", "Dañado."),
        "tri_c19" to listOf("Sin ventiladores.", "Ventiladores invertidos.", "Ventiladores dañados."),
        "tri_c21" to listOf("Alta temperatura."),
        "mono_c9" to listOf("Alta temperatura.")
    )

    val observacionesGenerales = listOf(
        "Falta señal de riesgo eléctrico.",
        "Faltan () separadores de fases."
    )

    fun paraItem(itemId: String): List<String> = porItem[itemId].orEmpty()
}
