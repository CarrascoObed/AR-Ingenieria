package com.aring.spec;

import com.aring.model.EstadoTarea;
import com.aring.model.InformeTablero;
import com.aring.model.Tarea;
import com.aring.model.Tecnico;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TareaSpecifications {

    private TareaSpecifications() {
    }

    public static Specification<Tarea> conFiltros(String codigo, String nombreTecnico, String nombreRecinto,
                                                  EstadoTarea estado, Instant fechaDesde, Instant fechaHasta) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), fechaHasta));
            }
            if (codigo != null && !codigo.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("codigo")),
                        "%" + codigo.trim().toLowerCase() + "%"));
            }
            if (nombreRecinto != null && !nombreRecinto.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("nombreRecinto")),
                        "%" + nombreRecinto.trim().toLowerCase() + "%"));
            }
            if (nombreTecnico != null && !nombreTecnico.isBlank()) {
                Join<Tarea, Tecnico> tecnico = root.join("tecnicoCabecera", JoinType.INNER);
                String patron = "%" + nombreTecnico.trim().toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(tecnico.get("nombre")), patron),
                        cb.like(cb.lower(tecnico.get("apellido")), patron)
                ));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    public static Specification<Tarea> documentosFinalizados(String nombreRecinto, String direccion,
                                                              String nombreTecnico, Instant fechaDesde,
                                                              Instant fechaHasta) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("estado"), EstadoTarea.FINALIZADA));
            if (nombreRecinto != null && !nombreRecinto.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("nombreRecinto")),
                        "%" + nombreRecinto.trim().toLowerCase() + "%"));
            }
            if (direccion != null && !direccion.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("direccion")),
                        "%" + direccion.trim().toLowerCase() + "%"));
            }
            if (nombreTecnico != null && !nombreTecnico.isBlank()) {
                Join<Tarea, Tecnico> tecnico = root.join("tecnicoCabecera", JoinType.LEFT);
                String patron = "%" + nombreTecnico.trim().toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(tecnico.get("nombre")), patron),
                        cb.like(cb.lower(tecnico.get("apellido")), patron),
                        cb.like(cb.lower(cb.concat(cb.concat(tecnico.get("nombre"), " "),
                                tecnico.get("apellido"))), patron)));
            }
            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaFin"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaFin"), fechaHasta));
            }
            if (query != null) {
                Subquery<Long> informes = query.subquery(Long.class);
                Root<InformeTablero> informe = informes.from(InformeTablero.class);
                informes.select(informe.get("id")).where(cb.equal(informe.get("tarea"), root));
                predicados.add(cb.exists(informes));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
