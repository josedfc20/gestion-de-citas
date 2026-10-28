package com.gestiondecitas.usuario;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** Persona registrada de 14 años o más (RN-006). */
@Entity
@Table(name = "cliente")
@PrimaryKeyJoinColumn(name = "usuario_id")
@DiscriminatorValue("CLIENTE")
public class Cliente extends Usuario {

    /** Vacía solo en las cuentas eliminadas (V4). */
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /** RNF-007: cuándo aceptó la política de privacidad. */
    @Column(name = "privacidad_aceptada_en", nullable = false)
    private Instant privacidadAceptadaEn;

    protected Cliente() {
    }

    public Cliente(String nombre, String apellidos, String email, String telefono, String passwordHash,
            LocalDate fechaNacimiento, Instant ahora) {
        super(nombre, apellidos, email, telefono, passwordHash, ahora);
        this.fechaNacimiento = fechaNacimiento;
        this.privacidadAceptadaEn = ahora;
    }

    @Override
    public Rol getRol() {
        return Rol.CLIENTE;
    }

    /** Además de lo común, la fecha de nacimiento. El consentimiento se conserva como prueba. */
    @Override
    public void anonimizar(String passwordHashInutilizable, Instant ahora) {
        super.anonimizar(passwordHashInutilizable, ahora);
        this.fechaNacimiento = null;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public Instant getPrivacidadAceptadaEn() {
        return privacidadAceptadaEn;
    }
}
