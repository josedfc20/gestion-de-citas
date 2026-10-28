package com.gestiondecitas.horario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AjusteHorarioRepository extends JpaRepository<AjusteHorario, AjusteHorario.Clave> {

    List<AjusteHorario> findByEmpleadoId(Long empleadoId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AjusteHorario a WHERE a.empleadoId = :empleadoId")
    void borrarDeEmpleado(Long empleadoId);
}
