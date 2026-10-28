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
 * Token de refresco (paso 3.4). Solo se guarda el SHA-256 del token: quien lea
 * la base de datos no puede usarlo. Cada uso lo revoca y crea otro (rotación).
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

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
    private boolean revocado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected RefreshToken() {
    }

    public RefreshToken(Usuario usuario, String tokenHash, Instant creadoEn, Instant expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    public boolean haCaducado(Instant ahora) {
        return !ahora.isBefore(expiraEn);
    }

    public void revocar() {
        this.revocado = true;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public boolean isRevocado() {
        return revocado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }
}
