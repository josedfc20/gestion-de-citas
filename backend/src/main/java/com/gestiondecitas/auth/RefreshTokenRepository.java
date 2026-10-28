package com.gestiondecitas.auth;

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
}
