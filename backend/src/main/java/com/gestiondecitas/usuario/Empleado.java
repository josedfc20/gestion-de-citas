package com.gestiondecitas.usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Profesional dado de alta por el administrador (RN-005). Empieza con una
 * contraseña temporal que debe cambiar en su primer inicio de sesión.
 * La gestión de empleados llega en B4.
 */
@Entity
@Table(name = "empleado")
@PrimaryKeyJoinColumn(name = "usuario_id")
@DiscriminatorValue("EMPLEADO")
public class Empleado extends Usuario {

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = true;

    protected Empleado() {
    }

    public Empleado(String nombre, String apellidos, String email, String passwordHashTemporal, Instant ahora) {
        super(nombre, apellidos, email, null, passwordHashTemporal, ahora);
    }

    @Override
    public Rol getRol() {
        return Rol.EMPLEADO;
    }

    @Override
    public boolean debeCambiarPassword() {
        return debeCambiarPassword;
    }

    /** Al elegir su propia contraseña deja de ser temporal. */
    @Override
    public void cambiarPassword(String nuevoHash) {
        super.cambiarPassword(nuevoHash);
        this.debeCambiarPassword = false;
    }
}
