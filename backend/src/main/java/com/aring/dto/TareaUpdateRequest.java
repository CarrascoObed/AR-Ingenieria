package com.aring.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TareaUpdateRequest {

    @NotNull(message = "Debe asignar un técnico de cabecera")
    private Long tecnicoCabeceraId;
}
