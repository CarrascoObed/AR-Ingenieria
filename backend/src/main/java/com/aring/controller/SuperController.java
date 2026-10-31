package com.aring.controller;

import com.aring.dto.AdminDto;
import com.aring.dto.AdminRequest;
import com.aring.dto.CambiarPasswordRequest;
import com.aring.dto.PantallaDto;
import com.aring.service.AdminService;
import com.aring.service.DocumentoService;
import com.aring.service.PantallaConfigService;
import com.aring.service.TareaService;
import com.aring.service.TecnicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/super")
@PreAuthorize("hasRole('SUPER')")
public class SuperController {

    private static final String WORD_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final ZoneId ZONA_CHILE = ZoneId.of("America/Santiago");

    private final TareaService tareaService;
    private final TecnicoService tecnicoService;
    private final AdminService adminService;
    private final PantallaConfigService pantallaConfigService;
    private final DocumentoService documentoService;

    public SuperController(TareaService tareaService,
                           TecnicoService tecnicoService,
                           AdminService adminService,
                           PantallaConfigService pantallaConfigService,
                           DocumentoService documentoService) {
        this.tareaService = tareaService;
        this.tecnicoService = tecnicoService;
        this.adminService = adminService;
        this.pantallaConfigService = pantallaConfigService;
        this.documentoService = documentoService;
    }

    @DeleteMapping("/tareas")
    public void eliminarTodasLasTareas() {
        tareaService.eliminarTodas();
    }

    @DeleteMapping("/tareas/{id}")
    public void eliminarTarea(@PathVariable Long id) {
        tareaService.eliminarCualquiera(id);
    }

    @DeleteMapping("/tecnicos/{id}")
    public void eliminarTecnico(@PathVariable Long id) {
        tecnicoService.eliminarSinRestricciones(id);
    }

    @GetMapping("/administradores")
    public List<AdminDto> listarAdministradores() {
        return adminService.listar().stream().map(AdminDto::from).toList();
    }

    @PostMapping("/administradores")
    public AdminDto crearAdministrador(@Valid @RequestBody AdminRequest request) {
        return AdminDto.from(adminService.crear(request));
    }

    @PutMapping("/administradores/{id}/password")
    public void cambiarPassword(@PathVariable Long id, @Valid @RequestBody CambiarPasswordRequest request) {
        adminService.cambiarPassword(id, request.getPassword());
    }

    @DeleteMapping("/administradores/{id}")
    public void eliminarAdministrador(@PathVariable Long id) {
        adminService.eliminar(id);
    }

    @GetMapping("/pantallas")
    public List<PantallaDto> listarPantallas() {
        return pantallaConfigService.listar();
    }

    @PutMapping("/pantallas")
    public List<PantallaDto> actualizarPantallas(@RequestBody List<PantallaDto> cambios) {
        return pantallaConfigService.actualizar(cambios);
    }

    @GetMapping("/informe.docx")
    public ResponseEntity<byte[]> informeWord(
            @RequestParam(required = false) String idTarea,
            @RequestParam(required = false) String nombreTecnico,
            @RequestParam(required = false) String nombreRecinto,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta) {
        byte[] bytes = documentoService.informeTareasWord(
                idTarea, nombreTecnico, nombreRecinto, inicioDelDia(fechaDesde), finDelDia(fechaHasta));
        String nombre = "informe-super-usuario" + (idTarea != null && !idTarea.isBlank() ? "-" + idTarea.trim() : "")
                + ".docx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType(WORD_MEDIA_TYPE))
                .contentLength(bytes.length)
                .body(bytes);
    }

    @GetMapping("/documentos-finalizadas.zip")
    public ResponseEntity<byte[]> documentosFinalizadasZip(
            @RequestParam int anio,
            @RequestParam(required = false) Integer mes) {
        byte[] bytes = documentoService.documentosFinalizadasZip(anio, mes);
        String periodo = mes == null ? String.valueOf(anio) : String.format("%d-%02d", anio, mes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"documentos-tareas-finalizadas-" + periodo + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(bytes.length)
                .body(bytes);
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
}
