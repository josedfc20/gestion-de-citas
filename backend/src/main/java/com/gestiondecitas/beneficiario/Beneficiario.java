package com.gestiondecitas.beneficiario;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Menor sin cuenta propia a cargo de un cliente (CU-009, RN-007). Solo guarda los
 * datos imprescindibles. Si tiene citas no se borra: se anonimiza (V7).
 */
@Entity
@Table(name = "beneficiario")
public class Beneficiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Long clienteId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String apellidos;

    /** Vacía solo en los beneficiarios eliminados. */
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "eliminado_en")
    private Instant eliminadoEn;

    protected Beneficiario() {
    }

    public Beneficiario(Long clienteId, String nombre, String apellidos, LocalDate fechaNacimiento) {
        this.clienteId = clienteId;
        actualizar(nombre, apellidos, fechaNacimiento);
    }

    public void actualizar(String nombre, String apellidos, LocalDate fechaNacimiento) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
    }

    /** RN-007: es menor si aún no tiene la edad mínima para crear una cuenta (RN-006). */
    public boolean esMenor(LocalDate hoy, int edadMinimaCuenta) {
        return fechaNacimiento != null && Period.between(fechaNacimiento, hoy).getYears() < edadMinimaCuenta;
    }

    /** RNF-007: borra los datos del menor sin borrar la fila, que sus citas siguen necesitando. */
    public void anonimizar(Instant ahora) {
        this.nombre = "Beneficiario";
        this.apellidos = "eliminado";
        this.fechaNacimiento = null;
        this.eliminadoEn = ahora;
    }

    public boolean isEliminado() {
        return eliminadoEn != null;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }
}
