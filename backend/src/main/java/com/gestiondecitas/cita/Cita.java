package com.gestiondecitas.cita;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import com.gestiondecitas.beneficiario.Beneficiario;
import com.gestiondecitas.servicio.Servicio;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.usuario.Cliente;
import com.gestiondecitas.usuario.Empleado;

/**
 * Cita reservada (CU-008). Guarda su copia del precio y de la hora de fin: cambiar
 * el servicio después no la toca (CU-016, alt. 2). Las transiciones de estado del
 * diagrama {@code Estados-Cita} viven aquí, sin Spring, y cada cambio deja un
 * {@link CambioEstadoCita}.
 */
@Entity
@Table(name = "cita")
public class Cita {

    static final String NO_CONFIRMADA = "Esta cita ya no se puede cambiar: no está confirmada.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id", nullable = false, updatable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false, updatable = false)
    private Servicio servicio;

    /** Vacío si la cita es para el propio cliente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiario_id", updatable = false)
    private Beneficiario beneficiario;

    @Column(nullable = false, updatable = false)
    private Instant inicio;

    @Column(nullable = false, updatable = false)
    private Instant fin;

    @Column(nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado;

    @Column(name = "motivo_cancelacion", length = 300)
    private String motivoCancelacion;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @OneToMany(mappedBy = "cita", cascade = CascadeType.ALL)
    @OrderBy("fecha ASC, id ASC")
    private List<CambioEstadoCita> cambios = new ArrayList<>();

    protected Cita() {
    }

    /**
     * RN-003: la reserva se confirma directamente. Fin y precio salen del servicio,
     * nunca de la petición.
     */
    public static Cita reservar(Cliente cliente, Empleado empleado, Servicio servicio, Beneficiario beneficiario,
            Instant inicio, Long realizadoPor, Instant ahora) {
        Cita cita = new Cita();
        cita.cliente = cliente;
        cita.empleado = empleado;
        cita.servicio = servicio;
        cita.beneficiario = beneficiario;
        cita.inicio = inicio;
        cita.fin = inicio.plus(Duration.ofMinutes(servicio.getDuracionMinutos()));
        cita.precio = servicio.getPrecio();
        cita.estado = EstadoCita.CONFIRMADA;
        cita.creadaEn = ahora;
        cita.cambios.add(new CambioEstadoCita(cita, null, EstadoCita.CONFIRMADA, realizadoPor, ahora));
        return cita;
    }

    /** RN-004: el cliente cancela si faltan {@code plazo} o más (24 h por defecto). */
    public boolean puedeCancelarCliente(Duration plazo, Instant ahora) {
        return estado == EstadoCita.CONFIRMADA && !ahora.isAfter(cancelableHasta(plazo));
    }

    public Instant cancelableHasta(Duration plazo) {
        return inicio.minus(plazo);
    }

    /** CU-012: el motivo es opcional para el cliente. */
    public void cancelarPorCliente(String motivo, Duration plazo, Long realizadoPor, Instant ahora) {
        exigirConfirmada();
        if (ahora.isAfter(cancelableHasta(plazo))) {
            throw new ReglaNegocioException("Ya no puedes cancelar online: faltan menos de " + plazo.toHours()
                    + " horas. Contacta con el negocio.");
        }
        cambiar(EstadoCita.CANCELADA, motivo, realizadoPor, ahora);
    }

    /**
     * CU-015 y CU-019 (empleado o administrador). Completada y No presentado, desde
     * la hora de inicio; Cancelada, en cualquier momento y con motivo.
     */
    public void cambiarEstado(EstadoCita nuevo, String motivo, Long realizadoPor, Instant ahora) {
        exigirConfirmada();
        switch (nuevo) {
            case COMPLETADA, NO_PRESENTADO -> {
                if (ahora.isBefore(inicio)) {
                    throw new ReglaNegocioException("No se puede marcar como "
                            + (nuevo == EstadoCita.COMPLETADA ? "completada" : "no presentado")
                            + " antes de la hora de inicio.");
                }
                cambiar(nuevo, null, realizadoPor, ahora);
            }
            case CANCELADA -> {
                if (motivo == null || motivo.isBlank()) {
                    throw new ReglaNegocioException("Indica el motivo de la cancelación.");
                }
                cambiar(nuevo, motivo, realizadoPor, ahora);
            }
            case CONFIRMADA -> throw new ReglaNegocioException("La cita ya está confirmada.");
        }
    }

    private void exigirConfirmada() {
        if (estado != EstadoCita.CONFIRMADA) {
            throw new ReglaNegocioException(NO_CONFIRMADA);
        }
    }

    private void cambiar(EstadoCita nuevo, String motivo, Long realizadoPor, Instant ahora) {
        cambios.add(new CambioEstadoCita(this, estado, nuevo, realizadoPor, ahora));
        this.estado = nuevo;
        if (nuevo == EstadoCita.CANCELADA) {
            this.motivoCancelacion = motivo == null || motivo.isBlank() ? null : motivo.trim();
        }
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public Beneficiario getBeneficiario() {
        return beneficiario;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFin() {
        return fin;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public List<CambioEstadoCita> getCambios() {
        return List.copyOf(cambios);
    }
}
