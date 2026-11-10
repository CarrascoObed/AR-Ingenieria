package com.aring.controller;

import com.aring.dto.InformeTableroDto;
import com.aring.dto.TableroNombreDto;
import com.aring.dto.TableroRequest;
import com.aring.security.JwtUtil;
import com.aring.service.TableroService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tareas/{tareaId}/tableros")
public class TableroController {

    private final TableroService tableroService;
    private final JwtUtil jwtUtil;

    public TableroController(TableroService tableroService, JwtUtil jwtUtil) {
        this.tableroService = tableroService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public InformeTableroDto crear(@PathVariable Long tareaId,
                                   @RequestHeader(value = "Authorization", required = false) String authorization,
                                   @Valid @RequestBody TableroRequest request) {
        return tableroService.crear(tareaId, request, tecnicoIdDeToken(authorization));
    }

    @GetMapping
    public List<InformeTableroDto> listar(@PathVariable Long tareaId) {
        return tableroService.listarPorTarea(tareaId);
    }

    @GetMapping("/nombres")
    public List<TableroNombreDto> listarNombres(@PathVariable Long tareaId) {
        return tableroService.listarNombresPorTarea(tareaId);
    }

    private Long tecnicoIdDeToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (jwtUtil.isValid(token)) {
                return jwtUtil.extractTecnicoId(token);
            }
        }
        return null;
    }
}
