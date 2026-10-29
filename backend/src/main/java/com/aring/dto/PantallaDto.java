package com.aring.dto;

import com.aring.model.PantallaConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PantallaDto {

    private String nombre;
    private boolean habilitada;

    public static PantallaDto from(PantallaConfig config) {
        return new PantallaDto(config.getNombre(), config.isHabilitada());
    }
}
