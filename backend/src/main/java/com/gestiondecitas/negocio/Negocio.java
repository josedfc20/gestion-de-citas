package com.gestiondecitas.negocio;

import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Configuración del negocio (tabla {@code negocio}, una sola fila creada por V2).
 * De momento solo se lee; el administrador podrá cambiarla en B4.
 */
@Entity
@Table(name = "negocio")
public class Negocio {

    @Id
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "zona_horaria", nullable = false)
    private String zonaHoraria;

    @Column(name = "intervalo_minutos", nullable = false)
    private int intervaloMinutos;

    @Column(name = "antelacion_max_dias", nullable = false)
    private int antelacionMaxDias;

    @Column(name = "horas_min_cancelacion", nullable = false)
    private int horasMinCancelacion;

    @Column(name = "edad_minima_cuenta", nullable = false)
    private int edadMinimaCuenta;

    protected Negocio() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public ZoneId getZonaHoraria() {
        return ZoneId.of(zonaHoraria);
    }

    public int getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public int getAntelacionMaxDias() {
        return antelacionMaxDias;
    }

    public int getHorasMinCancelacion() {
        return horasMinCancelacion;
    }

    /** RN-006: edad mínima para crear una cuenta de cliente. */
    public int getEdadMinimaCuenta() {
        return edadMinimaCuenta;
    }
}
