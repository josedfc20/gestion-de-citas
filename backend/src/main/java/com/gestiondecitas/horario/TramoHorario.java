package com.gestiondecitas.horario;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tramo de un día de la semana (tabla {@code tramo_horario}). Sin empleado es
 * del horario general; con empleado, de su ajuste de ese día (RN-002).
 */
@Entity
@Table(name = "tramo_horario")
public class TramoHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "negocio_id", nullable = false)
    private Long negocioId;

    @Column(name = "empleado_id")
    private Long empleadoId;

    /** 1 = lunes ... 7 = domingo, como {@link DayOfWeek#getValue()}. */
    @Column(name = "dia_semana", nullable = false)
    private short diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    protected TramoHorario() {
    }

    public TramoHorario(Long negocioId, Long empleadoId, DayOfWeek dia, Tramo tramo) {
        this.negocioId = negocioId;
        this.empleadoId = empleadoId;
        this.diaSemana = (short) dia.getValue();
        this.horaInicio = tramo.inicio();
        this.horaFin = tramo.fin();
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public DayOfWeek getDia() {
        return DayOfWeek.of(diaSemana);
    }

    public Tramo getTramo() {
        return new Tramo(horaInicio, horaFin);
    }
}
