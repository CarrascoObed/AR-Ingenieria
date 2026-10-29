package com.aring.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TecnicoUpdateRequest {

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El nombre del contacto de emergencia es obligatorio")
    private String contactoNombre;

    @NotBlank(message = "El apellido del contacto de emergencia es obligatorio")
    private String contactoApellido;

    @NotBlank(message = "El teléfono del contacto de emergencia es obligatorio")
    private String contactoTelefono;
}
