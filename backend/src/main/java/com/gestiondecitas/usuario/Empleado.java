package com.gestiondecitas.usuario;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import com.gestiondecitas.servicio.Servicio;

/**
 * Profesional dado de alta por el administrador (RN-005, CU-017). Empieza con una
 * contraseña temporal que debe cambiar en su primer inicio de sesión y realiza
 * los servicios que el administrador le asigna.
 */
@Entity
@Table(name = "empleado")
@PrimaryKeyJoinColumn(name = "usuario_id")
@DiscriminatorValue("EMPLEADO")
public class Empleado extends Usuario {

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = true;

    /** RF-013 y RF-019: tabla {@code empleado_servicio}. */
    @ManyToMany
    @JoinTable(name = "empleado_servicio",
            joinColumns = @JoinColumn(name = "empleado_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id"))
    private Set<Servicio> servicios = new HashSet<>();

    protected Empleado() {
    }

    public Empleado(String nombre, String apellidos, String email, String telefono, String passwordHashTemporal,
            Instant ahora) {
        super(nombre, apellidos, email, telefono, passwordHashTemporal, ahora);
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

    /** RN-005: el administrador le da otra contraseña temporal, que tendrá que cambiar al entrar. */
    public void nuevaPasswordTemporal(String passwordHashTemporal) {
        super.cambiarPassword(passwordHashTemporal);
        this.debeCambiarPassword = true;
    }

    /** Reemplaza la lista entera de servicios que realiza. */
    public void asignarServicios(Collection<Servicio> nuevos) {
        servicios.clear();
        servicios.addAll(nuevos);
    }

    public Set<Servicio> getServicios() {
        return Set.copyOf(servicios);
    }
}
