package com.gestiondecitas.horario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TramoHorarioRepository extends JpaRepository<TramoHorario, Long> {

    List<TramoHorario> findByEmpleadoIdIsNull();

    List<TramoHorario> findByEmpleadoId(Long empleadoId);

    List<TramoHorario> findByEmpleadoIdIsNotNull();

    /** Borrado directo: así los tramos nuevos se pueden insertar en la misma transacción. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TramoHorario t WHERE t.empleadoId IS NULL")
    void borrarGenerales();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TramoHorario t WHERE t.empleadoId = :empleadoId")
    void borrarDeEmpleado(Long empleadoId);
}
