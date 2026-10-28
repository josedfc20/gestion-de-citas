package com.gestiondecitas.negocio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository<Negocio, Long> {

    /** La primera versión tiene un único establecimiento: la fila que crea V2. */
    Optional<Negocio> findFirstByOrderByIdAsc();
}
