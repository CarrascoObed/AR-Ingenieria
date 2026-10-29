package com.aring.dto;

import com.aring.model.Tecnico;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class TecnicoDto {

    private Long id;
    private String nombre;
    private String apellido;
    private String rut;
    private String iniciales;
    private String contactoNombre;
    private String contactoApellido;
    private String contactoTelefono;
    private Instant fechaCreacion;

    public static TecnicoDto from(Tecnico t) {
        return new TecnicoDto(
                t.getId(),
                t.getNombre(),
                t.getApellido(),
                t.getRut(),
                iniciales(t.getNombre(), t.getApellido()),
                t.getContactoNombre(),
                t.getContactoApellido(),
                t.getContactoTelefono(),
                t.getFechaCreacion()
        );
    }

    public static String iniciales(String nombre, String apellido) {
        String n = nombre == null || nombre.isBlank() ? "" : nombre.substring(0, 1);
        String a = apellido == null || apellido.isBlank() ? "" : apellido.substring(0, 1);
        return (n + a).toUpperCase();
    }
}
