package com.aring.dto;

import com.aring.model.ChecklistTablero;
import com.aring.model.ImagenTablero;
import com.aring.model.InformeTablero;
import com.aring.model.MedicionTablero;
import com.aring.model.ObservacionTablero;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Getter
@AllArgsConstructor
public class InformeTableroDto {

    private Long id;
    private Long tareaId;
    private String tipoTablero;
    private String tamanioTablero;
    private String descripcion;
    private String ubicacion;
    private String nivel;
    private String reapriete;
    private String aseo;
    private Long tecnicoId;
    private String tecnicoNombre;
    private Instant fechaCreacion;
    private List<MedicionDto> mediciones;
    private List<ChecklistDto> checklist;
    private List<ImagenDto> imagenes;
    private List<String> observaciones;

    public static InformeTableroDto from(InformeTablero informe) {
        return new InformeTableroDto(
                informe.getId(),
                informe.getTarea().getId(),
                informe.getTipoTablero().getEtiqueta(),
                informe.getTamanio(),
                informe.getDescripcion(),
                informe.getUbicacion(),
                informe.getNivel(),
                informe.getReapriete(),
                informe.getAseo(),
                informe.getTecnico() != null ? informe.getTecnico().getId() : null,
                informe.getTecnico() != null
                        ? informe.getTecnico().getNombre() + " " + informe.getTecnico().getApellido()
                        : null,
                informe.getFechaCreacion(),
                informe.getMediciones().stream()
                        .sorted(Comparator.comparing(com.aring.model.MedicionTablero::getId))
                        .map(MedicionDto::from).toList(),
                informe.getChecklist().stream()
                        .sorted(Comparator.comparing(com.aring.model.ChecklistTablero::getId))
                        .map(ChecklistDto::from).toList(),
                informe.getImagenes().stream()
                        .sorted(Comparator.comparing(com.aring.model.ImagenTablero::getId))
                        .map(ImagenDto::from).toList(),
                informe.getObservaciones().stream()
                        .sorted(Comparator.comparing(ObservacionTablero::getOrden))
                        .map(ObservacionTablero::getTexto).toList()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class MedicionDto {
        private Long id;
        private String variableId;
        private String label;
        private String unit;
        private String valor;
        private String fotoBase64;

        public static MedicionDto from(MedicionTablero m) {
            return new MedicionDto(m.getId(), m.getVariableId(), m.getLabel(), m.getUnit(), m.getValor(), m.getFotoBase64());
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ChecklistDto {
        private Long id;
        private String itemId;
        private String label;
        private String status;
        private String fotoBase64;

        public static ChecklistDto from(ChecklistTablero c) {
            return new ChecklistDto(c.getId(), c.getItemId(), c.getLabel(), c.getStatus(), c.getFotoBase64());
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ImagenDto {
        private Long id;
        private String categoria;
        private int indice;
        private String base64;

        public static ImagenDto from(ImagenTablero i) {
            return new ImagenDto(i.getId(), i.getCategoria(), i.getIndice(), i.getBase64());
        }
    }
}
