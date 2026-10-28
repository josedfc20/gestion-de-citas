package com.gestiondecitas.servicio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findAllByOrderByNombreAsc();

    List<Servicio> findByActivoTrueOrderByNombreAsc();

    Optional<Servicio> findByIdAndActivoTrue(Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /** Cuántos empleados activos realizan cada servicio (lista del administrador). */
    @Query("""
            SELECT s.id AS servicioId, COUNT(e.id) AS empleados
            FROM Empleado e JOIN e.servicios s
            WHERE e.activo = true
            GROUP BY s.id""")
    List<EmpleadosPorServicio> contarEmpleadosActivos();

    interface EmpleadosPorServicio {

        Long getServicioId();

        long getEmpleados();
    }
}
