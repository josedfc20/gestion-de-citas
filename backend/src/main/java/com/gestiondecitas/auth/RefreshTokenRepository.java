package com.gestiondecitas.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Bloquea la fila hasta el final de la transacción: si llegan dos refresh con
     * el mismo token a la vez, el segundo espera y ya lo ve revocado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Cierra todas las sesiones de un usuario. */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revocado = true WHERE t.usuario.id = :usuarioId AND t.revocado = false")
    int revocarTodos(@Param("usuarioId") Long usuarioId);

    /** Sesiones que aún se pueden renovar, para exportar los datos (RNF-007). */
    @Query("""
            SELECT t FROM RefreshToken t
            WHERE t.usuario.id = :usuarioId AND t.revocado = false AND t.expiraEn > :ahora
            ORDER BY t.creadoEn""")
    List<RefreshToken> sesionesAbiertas(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);

    /** RNF-007: al eliminar la cuenta se borran sus sesiones. */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.usuario.id = :usuarioId")
    int borrarDeUsuario(@Param("usuarioId") Long usuarioId);
}
