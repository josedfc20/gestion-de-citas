package com.gestiondecitas.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.gestiondecitas.usuario.Usuario;

/**
 * Enlace para restablecer la contraseña (CU-020): de un solo uso y con
 * caducidad. Como el refresh token, solo se guarda el SHA-256 del token.
 */
@Entity
@Table(name = "token_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 100, updatable = false)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false, updatable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean usado;

    protected TokenRecuperacion() {
    }

    public TokenRecuperacion(Usuario usuario, String tokenHash, Instant expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
    }

    /** Sirve si no se ha usado y aún no ha llegado la hora de caducidad. */
    public boolean esValido(Instant ahora) {
        return !usado && ahora.isBefore(expiraEn);
    }

    public void usar() {
        this.usado = true;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
