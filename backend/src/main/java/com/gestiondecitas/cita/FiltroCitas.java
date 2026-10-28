package com.gestiondecitas.cita;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros de las listas de citas (CU-014 y CU-019). Un filtro vacío no restringe.
 * Las fechas ya llegan como instantes: [desde, hasta).
 */
record FiltroCitas(Long empleadoId, Long clienteId, EstadoCita estado, Instant desde, Instant hasta) {

    Specification<Cita> comoSpecification() {
        return (cita, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (empleadoId != null) {
                condiciones.add(cb.equal(cita.get("empleado").get("id"), empleadoId));
            }
            if (clienteId != null) {
                condiciones.add(cb.equal(cita.get("cliente").get("id"), clienteId));
            }
            if (estado != null) {
                condiciones.add(cb.equal(cita.get("estado"), estado));
            }
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(cita.get("inicio"), desde));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThan(cita.get("inicio"), hasta));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
