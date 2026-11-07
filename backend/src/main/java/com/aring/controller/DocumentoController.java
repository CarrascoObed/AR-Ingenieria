package com.aring.controller;

import com.aring.service.DocumentoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String WORD_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final ZoneId ZONA_CHILE = ZoneId.of("America/Santiago");
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter
            .ofPattern("MMMM-yyyy", Locale.forLanguageTag("es-CL"))
            .withZone(ZONA_CHILE);

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping("/edificios/sugerencias")
    public List<String> sugerenciasEdificios(@RequestParam("q") String consulta) {
        return documentoService.sugerirEdificios(consulta);
    }

    @GetMapping("/edificios/tareas")
    public List<DocumentoService.TareaInforme> tareasDeEdificio(
            @RequestParam(required = false) String nombreRecinto,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String nombreTecnico,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta) {
        return documentoService.buscarTareasFinalizadasParaInforme(nombreRecinto, direccion, nombreTecnico,
                inicioDelDia(fechaDesde), finDelDia(fechaHasta));
    }

    @GetMapping("/historial.xlsx")
    public ResponseEntity<byte[]> historialExcel(
            @RequestParam(required = false) String idTarea,
            @RequestParam(required = false) String nombreTecnico,
            @RequestParam(required = false) String nombreRecinto,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta) {
        byte[] bytes = documentoService.historialExcel(
                idTarea, nombreTecnico, nombreRecinto, inicioDelDia(fechaDesde), finDelDia(fechaHasta));
        return descargar(bytes, "historial-tareas.xlsx", EXCEL_MEDIA_TYPE);
    }

    private Instant inicioDelDia(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha).atStartOfDay(ZONA_CHILE).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Instant finDelDia(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha).atTime(LocalTime.MAX).atZone(ZONA_CHILE).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @GetMapping("/finalizadas.xlsx")
    public ResponseEntity<byte[]> finalizadasExcel(
            @RequestParam(required = false) String idTarea,
            @RequestParam(required = false) String nombreTecnico,
            @RequestParam(required = false) String nombreRecinto) {
        byte[] bytes = documentoService.finalizadasExcel(idTarea, nombreTecnico, nombreRecinto);
        String nombre = nombreArchivo(documentoService.datosArchivoTarea(idTarea), ".xlsx",
                "Rendición de servicios.xlsx");
        return descargar(bytes, nombre, EXCEL_MEDIA_TYPE);
    }

    @GetMapping("/finalizadas.docx")
    public ResponseEntity<byte[]> finalizadasWord(
            @RequestParam(required = false) String idTarea,
            @RequestParam(required = false) String nombreTecnico,
            @RequestParam(required = false) String nombreRecinto) {
        byte[] bytes = documentoService.finalizadasWord(idTarea, nombreTecnico, nombreRecinto);
        String nombre = nombreArchivo(documentoService.datosArchivoTarea(idTarea), ".docx",
                "Informe de tarea finalizada.docx");
        return descargar(bytes, nombre, WORD_MEDIA_TYPE);
    }

    @GetMapping("/informe-tarea.docx")
    public ResponseEntity<byte[]> informeTareaWord(@RequestParam("tareaId") Long tareaId) {
        DocumentoService.InformeTarea informe = documentoService.informeTareaWord(tareaId);
        return descargar(informe.bytes(), nombreArchivo(informe.tarea(), ".docx",
                "informe-tareas-finalizadas.docx"), WORD_MEDIA_TYPE);
    }

    private String nombreArchivo(DocumentoService.DatosArchivoTarea tarea, String extension, String respaldo) {
        if (tarea == null) {
            return respaldo;
        }
        Instant fecha = tarea.fechaFin() != null ? tarea.fechaFin()
                : tarea.fechaInicio() != null ? tarea.fechaInicio() : tarea.fechaCreacion();
        if (fecha == null) {
            return respaldo;
        }
        String mesAno = MES_ANO.format(fecha);
        mesAno = mesAno.substring(0, 1).toUpperCase(Locale.forLanguageTag("es-CL")) + mesAno.substring(1);
        String recinto = tarea.nombreRecinto() == null ? "" : tarea.nombreRecinto().trim();
        recinto = recinto.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "-")
                .replaceAll("[. ]+$", "");
        if (recinto.isBlank()) {
            recinto = "Tarea";
        }
        return recinto + "-" + mesAno + "-" + tarea.codigo() + extension;
    }

    private ResponseEntity<byte[]> descargar(byte[] bytes, String nombreArchivo, String mediaType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(nombreArchivo, StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.parseMediaType(mediaType))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
