package com.aring.movil.data.panel

/**
 * Validaciones EXCLUSIVAS del flujo monofásico.
 */
object MonofasicoValidation {

    fun validate(state: MonofasicoState, phase: Int): ValidationResult = when (phase) {
        1 -> validateRegistro(state)
        2 -> validateEvidenciasPrevias(state)
        3 -> validateVariables(state)
        4 -> validateChecklist(state)
        else -> ValidationResult.ok()
    }

    private fun validateRegistro(state: MonofasicoState): ValidationResult {
        val r = state.registration
        if (r.descripcion.isBlank()) return ValidationResult.fail("Complete el campo Descripción")
        if (r.ubicacion.isBlank()) return ValidationResult.fail("Complete el campo Ubicación")
        if (r.nivel.isBlank()) return ValidationResult.fail("Complete el campo Piso")
        return ValidationResult.ok()
    }

    private fun validateEvidenciasPrevias(state: MonofasicoState): ValidationResult {
        if (state.previousPhotos.none { it.path != null }) {
            return ValidationResult.fail("Debe tomar al menos una evidencia previa")
        }
        return ValidationResult.ok()
    }

    private fun validateVariables(state: MonofasicoState): ValidationResult {
        state.measurements.forEach { m ->
            if (m.photoPath == null) {
                return ValidationResult.fail("Debe tomar la fotografía de ${m.label}")
            }
        }
        if (state.reaprietePhoto == null) {
            return ValidationResult.fail("Debe tomar la fotografía de Reapriete")
        }
        if (state.aseoPhoto == null) {
            return ValidationResult.fail("Debe tomar la fotografía de Aseo")
        }
        return ValidationResult.ok()
    }

    private fun validateChecklist(state: MonofasicoState): ValidationResult {
        state.checklist.forEach { item ->
            if (item.status == null) {
                return ValidationResult.fail("Seleccione estado para: ${item.label}")
            }
            if (item.status == ChecklistStatus.CR && item.photoPath == null) {
                return ValidationResult.fail("Evidencia fotográfica requerida para: ${item.label}")
            }
        }
        return ValidationResult.ok()
    }

}
