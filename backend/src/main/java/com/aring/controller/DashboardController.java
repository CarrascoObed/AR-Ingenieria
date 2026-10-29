package com.aring.controller;

import com.aring.dto.DashboardStatsDto;
import com.aring.dto.TecnicoDto;
import com.aring.model.EstadoTarea;
import com.aring.model.Tecnico;
import com.aring.repository.TareaRepository;
import com.aring.service.TecnicoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TareaRepository tareaRepository;
    private final TecnicoService tecnicoService;

    public DashboardController(TareaRepository tareaRepository, TecnicoService tecnicoService) {
        this.tareaRepository = tareaRepository;
        this.tecnicoService = tecnicoService;
    }

    @GetMapping("/stats")
    public DashboardStatsDto stats() {
        return new DashboardStatsDto(
                tareaRepository.countByEstado(EstadoTarea.NO_INICIADA),
                tareaRepository.countByEstado(EstadoTarea.EN_PROCESO),
                tareaRepository.countByEstado(EstadoTarea.FINALIZADA)
        );
    }

    @GetMapping("/tecnicos")
    public List<TecnicoDto> tecnicos() {
        List<Tecnico> tecnicos = tecnicoService.listar();
        return tecnicos.stream().map(TecnicoDto::from).toList();
    }
}
