package com.gestiondecitas.negocio;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface NegocioRepository extends JpaRepository<Negocio, Long> {

    /** La primera versión tiene un único establecimiento: la fila que crea V2. */
    Optional<Negocio> findFirstByOrderByIdAsc();

    /**
     * La misma fila, bloqueada hasta el final de la transacción. La usan el cambio
     * de intervalo y el alta o edición de servicios, para que RN-011 se cumpla
     * aunque ocurran a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Negocio> findFirstForUpdateByOrderByIdAsc();
}
