package com.aring.service;

import com.aring.dto.PantallaDto;
import com.aring.exception.ApiException;
import com.aring.model.PantallaConfig;
import com.aring.repository.PantallaConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PantallaConfigService {

    private final PantallaConfigRepository pantallaConfigRepository;

    public PantallaConfigService(PantallaConfigRepository pantallaConfigRepository) {
        this.pantallaConfigRepository = pantallaConfigRepository;
    }

    @Transactional(readOnly = true)
    public List<PantallaDto> listar() {
        return pantallaConfigRepository.findAll().stream()
                .map(PantallaDto::from)
                .toList();
    }

    @Transactional
    public List<PantallaDto> actualizar(List<PantallaDto> cambios) {
        for (PantallaDto cambio : cambios) {
            if (cambio.getNombre() == null || cambio.getNombre().isBlank()) {
                throw new ApiException("El nombre de la pantalla es obligatorio.", HttpStatus.BAD_REQUEST);
            }
            PantallaConfig config = pantallaConfigRepository.findByNombre(cambio.getNombre())
                    .orElseThrow(() -> new ApiException("La pantalla configurada no existe.", HttpStatus.NOT_FOUND));
            config.setHabilitada(cambio.isHabilitada());
            pantallaConfigRepository.save(config);
        }
        return listar();
    }
}
