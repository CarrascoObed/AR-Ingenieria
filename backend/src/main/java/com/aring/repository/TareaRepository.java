package com.aring.repository;

import com.aring.model.EstadoTarea;
import com.aring.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface TareaRepository extends JpaRepository<Tarea, Long>, JpaSpecificationExecutor<Tarea> {

    Optional<Tarea> findByCodigoIgnoreCase(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tarea t where t.id = :id")
    Optional<Tarea> findByIdForUpdate(@Param("id") Long id);

    @Query("select distinct trim(t.nombreRecinto) from Tarea t " +
            "where t.estado = com.aring.model.EstadoTarea.FINALIZADA " +
            "and lower(trim(t.nombreRecinto)) like lower(concat('%', :consulta, '%')) " +
            "and exists (select i.id from InformeTablero i where i.tarea = t) " +
            "order by trim(t.nombreRecinto)")
    List<String> sugerirEdificiosFinalizados(@Param("consulta") String consulta, Pageable pageable);

    @Query("select t from Tarea t " +
            "where t.estado = com.aring.model.EstadoTarea.FINALIZADA " +
            "and lower(trim(t.nombreRecinto)) = lower(trim(:nombreRecinto)) " +
            "and exists (select i.id from InformeTablero i where i.tarea = t) " +
            "order by t.fechaFin desc, t.id desc")
    List<Tarea> listarFinalizadasConInformesDeEdificio(@Param("nombreRecinto") String nombreRecinto);

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
