package com.aring.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteRequest {

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotBlank(message = "El nombre del recinto es obligatorio")
    private String nombreRecinto;

    private String imagenBase64;
}
