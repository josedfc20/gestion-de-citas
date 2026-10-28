package com.gestiondecitas.horario;

import java.io.Serializable;
import java.time.DayOfWeek;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Día de la semana en que un empleado no sigue el horario general (V6, RN-002).
 * Sus tramos de ese día están en {@link TramoHorario}; si no tiene ninguno, ese
 * día no trabaja.
 */
@Entity
@Table(name = "ajuste_horario")
@IdClass(AjusteHorario.Clave.class)
public class AjusteHorario {

    @Id
    @Column(name = "empleado_id")
    private Long empleadoId;

    @Id
    @Column(name = "dia_semana")
    private short diaSemana;

    protected AjusteHorario() {
    }

    public AjusteHorario(Long empleadoId, DayOfWeek dia) {
        this.empleadoId = empleadoId;
        this.diaSemana = (short) dia.getValue();
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public DayOfWeek getDia() {
        return DayOfWeek.of(diaSemana);
    }

    public record Clave(Long empleadoId, short diaSemana) implements Serializable {
    }
}
