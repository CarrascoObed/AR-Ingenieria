package com.aring.dto;

import com.aring.model.TipoTarea;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TareaRequest {

    @NotNull(message = "Debe indicar el tipo de tarea")
    private TipoTarea tipo;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotBlank(message = "El nombre del recinto es obligatorio")
    private String nombreRecinto;

    @NotBlank(message = "La descripción de la tarea es obligatoria")
    private String descripcion;

    @NotNull(message = "Debe asignar un técnico de cabecera")
    private Long tecnicoCabeceraId;

    /** Opcional: cliente cuya dirección/recinto se usaron para crear la tarea. */
    private Long clienteId;
}
