package com.aring.repository;

import com.aring.model.SecuenciaTarea;
import com.aring.model.TipoTarea;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SecuenciaTareaRepository extends JpaRepository<SecuenciaTarea, Long> {

    Optional<SecuenciaTarea> findByTipo(TipoTarea tipo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SecuenciaTarea s where s.tipo = :tipo")
    Optional<SecuenciaTarea> findByTipoParaActualizar(@Param("tipo") TipoTarea tipo);
}
