package com.aring.repository;

import com.aring.model.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {

    boolean existsByRut(String rut);

    Optional<Tecnico> findByRut(String rut);

    List<Tecnico> findAllByOrderByNombreAscApellidoAsc();
}
