package com.aring.repository;

import com.aring.model.PantallaConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PantallaConfigRepository extends JpaRepository<PantallaConfig, Long> {

    Optional<PantallaConfig> findByNombre(String nombre);
}
