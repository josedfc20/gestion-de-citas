package com.gestiondecitas.horario;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Día completo sin atención (CU-018): sin empleado cierra todo el negocio
 * (festivo); con empleado, solo a ese empleado (vacaciones).
 */
@Entity
@Table(name = "dia_cierre")
public class DiaCierre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "negocio_id", nullable = false)
    private Long negocioId;

    @Column(name = "empleado_id")
    private Long empleadoId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 200)
    private String motivo;

    protected DiaCierre() {
    }

    public DiaCierre(Long negocioId, Long empleadoId, LocalDate fecha, String motivo) {
        this.negocioId = negocioId;
        this.empleadoId = empleadoId;
        this.fecha = fecha;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }
}
