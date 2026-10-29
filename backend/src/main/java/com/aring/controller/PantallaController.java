package com.aring.controller;

import com.aring.dto.PantallaDto;
import com.aring.service.PantallaConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pantallas")
public class PantallaController {

    private final PantallaConfigService pantallaConfigService;

    public PantallaController(PantallaConfigService pantallaConfigService) {
        this.pantallaConfigService = pantallaConfigService;
    }

    @GetMapping
    public List<PantallaDto> listar() {
        return pantallaConfigService.listar();
    }
}
