package com.aring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TableroRequest {

    @NotNull(message = "Debe indicar la tarea")
    private Long tareaId;

    @NotBlank(message = "Debe indicar el tipo de tablero")
    private String tipoTablero;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotBlank(message = "La ubicación es obligatoria")
    private String ubicacion;

    @NotBlank(message = "El nivel es obligatorio")
    private String nivel;

    private String tamanioTablero;

    private String reapriete;

    private String aseo;

    private Long tecnicoId;

    private List<MedicionRequest> mediciones = new ArrayList<>();

    private List<ChecklistRequest> checklist = new ArrayList<>();

    private List<ImagenRequest> imagenes = new ArrayList<>();

    private List<String> observaciones = new ArrayList<>();

    @Getter
    @Setter
    public static class MedicionRequest {
        private String variableId;
        private String label;
        private String unit;
        private String valor;
        private String fotoBase64;
    }

    @Getter
    @Setter
    public static class ChecklistRequest {
        private String itemId;
        private String label;
        private String status;
        private String fotoBase64;
    }

    @Getter
    @Setter
    public static class ImagenRequest {
        private String categoria;
        private int indice;
        private String base64;
    }
}
