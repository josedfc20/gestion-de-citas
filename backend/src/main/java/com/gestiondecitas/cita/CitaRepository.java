package com.gestiondecitas.cita;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

public interface CitaRepository extends JpaRepository<Cita, Long>, JpaSpecificationExecutor<Cita> {

    /** Lo que se muestra de una cita: servicio, profesional, cliente y beneficiario en una consulta. */
    @EntityGraph(attributePaths = {"servicio", "empleado", "cliente", "beneficiario"})
    Optional<Cita> findConDatosById(Long id);

    /** Listas con filtros del empleado y del administrador ({@link FiltroCitas}). */
    @Override
    @EntityGraph(attributePaths = {"servicio", "empleado", "cliente", "beneficiario"})
    Page<Cita> findAll(Specification<Cita> filtro, Pageable pagina);

    /** CU-013: las citas no canceladas del empleado que empiezan en [desde, hasta). */
    @EntityGraph(attributePaths = {"servicio", "empleado", "cliente", "beneficiario"})
    @Query("""
            SELECT c FROM Cita c
            WHERE c.empleado.id = :empleadoId AND c.estado <> com.gestiondecitas.cita.EstadoCita.CANCELADA
              AND c.inicio >= :desde AND c.inicio < :hasta
            ORDER BY c.inicio""")
    List<Cita> agenda(@Param("empleadoId") Long empleadoId, @Param("desde") Instant desde,
            @Param("hasta") Instant hasta);

    /** CU-010: próximas, en orden. */
    @EntityGraph(attributePaths = {"servicio", "empleado", "beneficiario"})
    @Query("SELECT c FROM Cita c WHERE c.cliente.id = :clienteId AND c.inicio >= :ahora ORDER BY c.inicio ASC, c.id ASC")
    Page<Cita> proximasDeCliente(@Param("clienteId") Long clienteId, @Param("ahora") Instant ahora,
            Pageable pagina);

    /** Historial, de la más reciente a la más antigua. */
    @EntityGraph(attributePaths = {"servicio", "empleado", "beneficiario"})
    @Query("SELECT c FROM Cita c WHERE c.cliente.id = :clienteId AND c.inicio < :ahora ORDER BY c.inicio DESC, c.id DESC")
    Page<Cita> pasadasDeCliente(@Param("clienteId") Long clienteId, @Param("ahora") Instant ahora,
            Pageable pagina);

    /** Exportación RGPD: todas las del cliente. */
    @EntityGraph(attributePaths = {"servicio", "empleado", "beneficiario"})
    List<Cita> findByClienteIdOrderByInicioAsc(Long clienteId);

    /**
     * Citas que ocupan al empleado y se solapan con [desde, hasta). Los estados son
     * los de la restricción {@code no_solape_profesional}.
     */
    @EntityGraph(attributePaths = "empleado")
    @Query("""
            SELECT c FROM Cita c
            WHERE c.empleado.id = :empleadoId AND c.estado IN :estados
              AND c.inicio < :hasta AND c.fin > :desde
            ORDER BY c.inicio""")
    List<Cita> solapadasDeEmpleado(@Param("empleadoId") Long empleadoId, @Param("desde") Instant desde,
            @Param("hasta") Instant hasta, @Param("estados") Collection<EstadoCita> estados);

    /** Confirmadas que empiezan después de {@code desde}; sin empleado, las de todos. */
    @EntityGraph(attributePaths = "empleado")
    @Query("""
            SELECT c FROM Cita c
            WHERE (:empleadoId IS NULL OR c.empleado.id = :empleadoId)
              AND c.estado = com.gestiondecitas.cita.EstadoCita.CONFIRMADA AND c.inicio > :desde
            ORDER BY c.inicio""")
    List<Cita> confirmadasFuturas(@Param("empleadoId") Long empleadoId, @Param("desde") Instant desde);

    long countByEmpleadoIdAndEstadoAndInicioAfter(Long empleadoId, EstadoCita estado, Instant desde);

    long countByEmpleadoIdAndServicioIdAndEstadoAndInicioAfter(Long empleadoId, Long servicioId,
            EstadoCita estado, Instant desde);

    /** La misma persona (el cliente para sí mismo) ya tiene una cita activa que se solapa. */
    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.cliente.id = :clienteId AND c.beneficiario IS NULL
              AND c.estado IN :estados AND c.inicio < :fin AND c.fin > :inicio""")
    boolean clienteOcupado(@Param("clienteId") Long clienteId, @Param("inicio") Instant inicio,
            @Param("fin") Instant fin, @Param("estados") Collection<EstadoCita> estados);

    /** El mismo beneficiario ya tiene una cita activa que se solapa. */
    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.beneficiario.id = :beneficiarioId
              AND c.estado IN :estados AND c.inicio < :fin AND c.fin > :inicio""")
    boolean beneficiarioOcupado(@Param("beneficiarioId") Long beneficiarioId, @Param("inicio") Instant inicio,
            @Param("fin") Instant fin, @Param("estados") Collection<EstadoCita> estados);

    boolean existsByBeneficiarioId(Long beneficiarioId);

    List<Cita> findByBeneficiarioIdAndEstadoAndInicioAfter(Long beneficiarioId, EstadoCita estado, Instant desde);

    List<Cita> findByClienteIdAndEstadoAndInicioAfter(Long clienteId, EstadoCita estado, Instant desde);
}
