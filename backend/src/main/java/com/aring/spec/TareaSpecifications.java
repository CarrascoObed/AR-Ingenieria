package com.aring.spec;

import com.aring.model.EstadoTarea;
import com.aring.model.Tarea;
import com.aring.model.Tecnico;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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
}
