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

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String WORD_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final ZoneId ZONA_CHILE = ZoneId.of("America/Santiago");

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
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
        return descargar(bytes, "Rendición de servicios.xlsx", EXCEL_MEDIA_TYPE);
    }

    @GetMapping("/finalizadas.docx")
    public ResponseEntity<byte[]> finalizadasWord(
            @RequestParam(required = false) String idTarea,
            @RequestParam(required = false) String nombreTecnico,
            @RequestParam(required = false) String nombreRecinto) {
        byte[] bytes = documentoService.finalizadasWord(idTarea, nombreTecnico, nombreRecinto);
        return descargar(bytes, "Informe de tarea finalizada.docx", WORD_MEDIA_TYPE);
    }

    @GetMapping("/informe-tarea.docx")
    public ResponseEntity<byte[]> informeTareaWord(@RequestParam("tareaId") Long tareaId) {
        DocumentoService.InformeTarea informe = documentoService.informeTareaWord(tareaId);
        return descargar(informe.bytes(),
                "informe-tareas-finalizadas-" + informe.codigo() + ".docx", WORD_MEDIA_TYPE);
    }

    private ResponseEntity<byte[]> descargar(byte[] bytes, String nombreArchivo, String mediaType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType(mediaType))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
