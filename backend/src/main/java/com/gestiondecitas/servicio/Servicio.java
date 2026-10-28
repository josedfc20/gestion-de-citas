package com.gestiondecitas.servicio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Servicio del catálogo (CU-016). Nunca se borra: se desactiva, para conservar
 * el historial de citas. Cada cita guarda su copia del precio y de la duración,
 * así que cambiarlos solo afecta a las reservas nuevas.
 */
@Entity
@Table(name = "servicio")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    /** RN-001 y RN-011: múltiplo del intervalo de reserva del negocio. */
    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean activo = true;

    protected Servicio() {
    }

    public Servicio(String nombre, String descripcion, int duracionMinutos, BigDecimal precio) {
        actualizar(nombre, descripcion, duracionMinutos, precio);
    }

    public void actualizar(String nombre, String descripcion, int duracionMinutos, BigDecimal precio) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionMinutos = duracionMinutos;
        this.precio = precio;
    }

    public void cambiarActivo(boolean activo) {
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public boolean isActivo() {
        return activo;
    }
}
