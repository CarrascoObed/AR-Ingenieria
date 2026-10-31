package com.aring.movil.data.panel

import java.util.UUID

const val TYPE_TRIFASICO = "TRIFASICO"
const val TYPE_MONOFASICO = "MONOFASICO"

fun newId(): String = UUID.randomUUID().toString()

enum class ChecklistStatus { OK, CR, NA }

fun ChecklistStatus.label(): String = when (this) {
    ChecklistStatus.OK -> "OK"
    ChecklistStatus.CR -> "C/R"
    ChecklistStatus.NA -> "N/A"
}

data class VariableDef(val id: String, val label: String, val unit: String)

data class ChecklistDef(val id: String, val label: String)

data class PhotoSlot(val index: Int, val path: String? = null)

data class ValidationResult(val valid: Boolean, val message: String? = null) {
    companion object {
        fun ok(): ValidationResult = ValidationResult(valid = true)
        fun fail(message: String): ValidationResult = ValidationResult(valid = false, message = message)
    }
}
