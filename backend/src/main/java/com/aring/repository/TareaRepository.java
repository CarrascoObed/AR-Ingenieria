package com.aring.repository;

import com.aring.model.EstadoTarea;
import com.aring.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long>, JpaSpecificationExecutor<Tarea> {

    boolean existsByTecnicoCabeceraIdAndEstadoNot(Long tecnicoId, EstadoTarea estado);

    boolean existsByTecnicosInvitadosIdAndEstadoNot(Long tecnicoId, EstadoTarea estado);

    boolean existsByClienteId(Long clienteId);

    long countByEstado(EstadoTarea estado);

    @Modifying
    @Query("update Tarea t set t.tecnicoCabecera = null where t.tecnicoCabecera.id = :tecnicoId")
    void liberarTareasDeTecnico(@Param("tecnicoId") Long tecnicoId);

    @Query("select distinct t from Tarea t left join t.tecnicosInvitados inv where t.tecnicoCabecera.id = :tecnicoId or inv.id = :tecnicoId order by t.id desc")
    List<Tarea> findAllParaTecnico(@Param("tecnicoId") Long tecnicoId);

    @Modifying
    @Query(value = "DELETE FROM tarea_tecnico_invitado WHERE tecnico_id = :tecnicoId", nativeQuery = true)
    void eliminarInvitacionesDeTecnico(@Param("tecnicoId") Long tecnicoId);

    @Modifying
    @Query(value = "DELETE FROM tarea_tecnico_invitado WHERE tarea_id = :tareaId", nativeQuery = true)
    void eliminarInvitacionesDeTarea(@Param("tareaId") Long tareaId);

    @Modifying
    @Query(value = "DELETE FROM tarea_tecnico_invitado", nativeQuery = true)
    void eliminarTodasLasInvitaciones();
}
