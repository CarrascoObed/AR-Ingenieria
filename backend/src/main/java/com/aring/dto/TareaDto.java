package com.aring.dto;

import com.aring.model.Tarea;
import com.aring.model.Tecnico;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class TareaDto {

    private Long id;
    private String codigo;
    private String tipo;
    private String direccion;
    private String nombreRecinto;
    private String descripcion;
    private String estado;
    private TecnicoDto tecnicoCabecera;
    private Instant fechaCreacion;
    private Instant fechaInicio;
    private Instant fechaFin;
    private List<Long> tecnicoAyudantesIds;
    private int tablerosRealizados;

    public static TareaDto from(Tarea t) {
        return new TareaDto(
                t.getId(),
                t.getCodigo(),
                t.getTipo().getEtiqueta(),
                t.getDireccion(),
                t.getNombreRecinto(),
                t.getDescripcion(),
                t.getEstado().getEtiqueta(),
                t.getTecnicoCabecera() == null ? null : TecnicoDto.from(t.getTecnicoCabecera()),
                t.getFechaCreacion(),
                t.getFechaInicio(),
                t.getFechaFin(),
                t.getTecnicosInvitados().stream().map(Tecnico::getId).toList(),
                t.getTablerosRealizados()
        );
    }
}
