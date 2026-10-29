package com.aring.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TecnicoRequest {

    @NotBlank(message = "El nombre del técnico es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido del técnico es obligatorio")
    private String apellido;

    @NotBlank(message = "El RUT del técnico es obligatorio")
    private String rut;

    @NotBlank(message = "Debe asignar una contraseña al técnico")
    private String password;

    @NotBlank(message = "El nombre del contacto de emergencia es obligatorio")
    private String contactoNombre;

    @NotBlank(message = "El apellido del contacto de emergencia es obligatorio")
    private String contactoApellido;

    @NotBlank(message = "El teléfono del contacto de emergencia es obligatorio")
    private String contactoTelefono;
}
