package com.aring.repository;

import com.aring.model.InformeTablero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InformeTableroRepository extends JpaRepository<InformeTablero, Long> {

    List<InformeTablero> findAllByTareaIdOrderByIdAsc(Long tareaId);

    Optional<InformeTablero> findByClientOperationId(String clientOperationId);

    boolean existsByTecnicoId(Long tecnicoId);

    void deleteByTecnicoId(Long tecnicoId);

    void deleteByTareaId(Long tareaId);
}
