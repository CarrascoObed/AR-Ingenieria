package com.aring.repository;

import com.aring.model.Notificacion;
import com.aring.model.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findAllByTipoOrderByFechaDesc(TipoNotificacion tipo);

    List<Notificacion> findAllByTecnicoDestinoIdOrderByFechaDesc(Long tecnicoDestinoId);

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.leida = false")
    int marcarTodasLeidas();

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.leida = false and n.tecnicoDestinoId = :tecnicoId")
    int marcarTodasLeidasTecnico(@Param("tecnicoId") Long tecnicoId);

    @Modifying
    @Query("delete from Notificacion n where n.tipo = com.aring.model.TipoNotificacion.TAREA_FINALIZADA")
    int borrarFinalizadas();

    @Modifying
    @Query("delete from Notificacion n where n.tecnicoDestinoId = :tecnicoId")
    int borrarDeTecnico(@Param("tecnicoId") Long tecnicoId);

    @Modifying
    @Query("delete from Notificacion n where n.tareaId = :tareaId")
    int borrarDeTarea(@Param("tareaId") Long tareaId);

    @Modifying
    @Query("delete from Notificacion n where n.tareaId = :tareaId "
            + "and n.tecnicoDestinoId = :tecnicoId "
            + "and n.tipo = com.aring.model.TipoNotificacion.INVITACION")
    int borrarInvitacionDeTarea(@Param("tareaId") Long tareaId, @Param("tecnicoId") Long tecnicoId);
}
