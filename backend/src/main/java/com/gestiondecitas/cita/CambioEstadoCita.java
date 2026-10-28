package com.gestiondecitas.cita;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Historial de una cita: cada cambio de estado con su fecha y quién lo hizo (CU-015, CU-019). */
@Entity
@Table(name = "cambio_estado_cita")
public class CambioEstadoCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false, updatable = false)
    private Cita cita;

    /** Vacío al crear la cita. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20, updatable = false)
    private EstadoCita estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20, updatable = false)
    private EstadoCita estadoNuevo;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @Column(name = "realizado_por", nullable = false, updatable = false)
    private Long realizadoPor;

    protected CambioEstadoCita() {
    }

    CambioEstadoCita(Cita cita, EstadoCita estadoAnterior, EstadoCita estadoNuevo, Long realizadoPor,
            Instant fecha) {
        this.cita = cita;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.realizadoPor = realizadoPor;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public EstadoCita getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoCita getEstadoNuevo() {
        return estadoNuevo;
    }

    public Instant getFecha() {
        return fecha;
    }

    public Long getRealizadoPor() {
        return realizadoPor;
    }
}
