package com.aring.repository;

import com.aring.dto.TableroNombreDto;
import com.aring.model.InformeTablero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InformeTableroRepository extends JpaRepository<InformeTablero, Long> {

    List<InformeTablero> findAllByTareaIdOrderByIdAsc(Long tareaId);

    @Query("select new com.aring.dto.TableroNombreDto(i.descripcion, i.clientOperationId) " +
            "from InformeTablero i where i.tarea.id = :tareaId order by i.id asc")
    List<TableroNombreDto> listarNombresPorTarea(@Param("tareaId") Long tareaId);

    Optional<InformeTablero> findByClientOperationId(String clientOperationId);

    boolean existsByTecnicoId(Long tecnicoId);

    void deleteByTecnicoId(Long tecnicoId);

    void deleteByTareaId(Long tareaId);
}
