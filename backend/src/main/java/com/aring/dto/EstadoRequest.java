package com.aring.dto;

import com.aring.model.EstadoTarea;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstadoRequest {

    @NotNull(message = "Debe indicar el nuevo estado")
    private EstadoTarea estado;
}
