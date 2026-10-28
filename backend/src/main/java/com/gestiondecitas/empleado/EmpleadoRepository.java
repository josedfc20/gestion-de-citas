package com.gestiondecitas.empleado;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gestiondecitas.usuario.Empleado;

/** Solo ve filas de la tabla {@code empleado}: el id de un cliente o del admin no aparece (→ 404). */
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    @EntityGraph(attributePaths = "servicios")
    List<Empleado> findAllByOrderByNombreAscApellidosAsc();

    @EntityGraph(attributePaths = "servicios")
    List<Empleado> findByActivoTrueOrderByNombreAscApellidosAsc();

    @EntityGraph(attributePaths = "servicios")
    Optional<Empleado> findConServiciosById(Long id);
}
