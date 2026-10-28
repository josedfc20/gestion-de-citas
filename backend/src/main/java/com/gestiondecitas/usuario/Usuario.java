package com.gestiondecitas.usuario;

import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Cualquier perfil con cuenta. Herencia JOINED (paso 2.6): la tabla común
 * {@code usuario} y una tabla por subtipo ({@link Cliente}, {@link Empleado}).
 * La columna {@code rol} dice de qué tipo es cada fila. El administrador no tiene
 * datos propios, así que es un {@code Usuario} sin subtabla.
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "rol", discriminatorType = DiscriminatorType.STRING, length = 20)
@DiscriminatorValue("ADMIN")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String apellidos;

    /** Siempre en minúsculas y sin espacios: el índice único no distingue mayúsculas. */
    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected Usuario() {
    }

    /** Crea un administrador. */
    public Usuario(String nombre, String apellidos, String email, String passwordHash, Instant creadoEn) {
        this(nombre, apellidos, email, null, passwordHash, creadoEn);
    }

    protected Usuario(String nombre, String apellidos, String email, String telefono, String passwordHash,
            Instant creadoEn) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = normalizarEmail(email);
        this.telefono = telefono;
        this.passwordHash = passwordHash;
        this.creadoEn = creadoEn;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public Rol getRol() {
        return Rol.ADMIN;
    }

    /** RN-005: solo los empleados pueden tener una contraseña temporal. */
    public boolean debeCambiarPassword() {
        return false;
    }

    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = nuevoHash;
    }

    public void actualizarPerfil(String nombre, String apellidos, String email, String telefono) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = normalizarEmail(email);
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
