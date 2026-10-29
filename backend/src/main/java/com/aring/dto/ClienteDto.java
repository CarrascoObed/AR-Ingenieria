package com.aring.dto;

import com.aring.model.Cliente;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ClienteDto {

    private Long id;
    private String direccion;
    private String nombreRecinto;
    private String imagenTipo;
    private String imagenBase64;
    private Instant fechaCreacion;

    public static ClienteDto from(Cliente c) {
        return new ClienteDto(
                c.getId(),
                c.getDireccion(),
                c.getNombreRecinto(),
                c.getImagenTipo(),
                c.getImagenBase64(),
                c.getFechaCreacion()
        );
    }
}
