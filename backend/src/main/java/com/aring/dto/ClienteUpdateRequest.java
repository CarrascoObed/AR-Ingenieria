package com.aring.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteUpdateRequest {

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotBlank(message = "El nombre del recinto es obligatorio")
    private String nombreRecinto;

    /**
     * Si es null o viene en blanco se conserva la imagen actual.
     * Si trae contenido, se reemplaza la imagen del cliente.
     */
    private String imagenBase64;
}
