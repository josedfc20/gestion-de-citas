package com.gestiondecitas.auth;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    /** Bloquea la fila: si el mismo enlace llega dos veces a la vez, el segundo ya lo ve usado. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    /** Al pedir un enlace nuevo, los anteriores dejan de valer: solo sirve el último. */
    @Modifying
    @Query("UPDATE TokenRecuperacion t SET t.usado = true WHERE t.usuario.id = :usuarioId AND t.usado = false")
    int invalidarPendientes(@Param("usuarioId") Long usuarioId);

    /** RNF-007: al eliminar la cuenta no quedan enlaces suyos. */
    @Modifying
    @Query("DELETE FROM TokenRecuperacion t WHERE t.usuario.id = :usuarioId")
    int borrarDeUsuario(@Param("usuarioId") Long usuarioId);
}
