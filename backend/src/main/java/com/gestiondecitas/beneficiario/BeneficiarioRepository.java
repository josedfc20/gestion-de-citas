package com.gestiondecitas.beneficiario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Los eliminados (anonimizados) solo siguen existiendo para el historial de citas. */
public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Long> {

    List<Beneficiario> findByClienteIdAndEliminadoEnIsNullOrderByNombreAscApellidosAsc(Long clienteId);

    List<Beneficiario> findByClienteIdAndEliminadoEnIsNull(Long clienteId);

    List<Beneficiario> findByClienteIdInAndEliminadoEnIsNullOrderByNombreAscApellidosAsc(
            Collection<Long> clienteIds);

    Optional<Beneficiario> findByIdAndClienteIdAndEliminadoEnIsNull(Long id, Long clienteId);
}
