package com.gestiondecitas.horario;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DiaCierreRepository extends JpaRepository<DiaCierre, Long> {

    /** Todos los cierres (del negocio y de empleados) desde una fecha; {@code hasta} es opcional. */
    @Query("""
            SELECT d FROM DiaCierre d
            WHERE d.fecha >= :desde AND (CAST(:hasta AS LocalDate) IS NULL OR d.fecha <= :hasta)
            ORDER BY d.fecha, d.empleadoId NULLS FIRST""")
    List<DiaCierre> listar(LocalDate desde, LocalDate hasta);

    /** Los que afectan a un empleado: los del negocio y los suyos. */
    @Query("""
            SELECT d FROM DiaCierre d
            WHERE (d.empleadoId IS NULL OR d.empleadoId = :empleadoId)
              AND d.fecha BETWEEN :desde AND :hasta""")
    List<DiaCierre> queAfectanA(Long empleadoId, LocalDate desde, LocalDate hasta);

    @Query("""
            SELECT COUNT(d) > 0 FROM DiaCierre d
            WHERE d.fecha = :fecha
              AND ((:empleadoId IS NULL AND d.empleadoId IS NULL) OR d.empleadoId = :empleadoId)""")
    boolean existe(Long empleadoId, LocalDate fecha);
}
