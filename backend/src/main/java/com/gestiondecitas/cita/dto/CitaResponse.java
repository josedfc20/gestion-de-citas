package com.gestiondecitas.cita.dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.gestiondecitas.cita.CambioEstadoCita;
import com.gestiondecitas.cita.Cita;
import com.gestiondecitas.cita.EstadoCita;
import com.gestiondecitas.empleado.EmpleadoService;
import com.gestiondecitas.usuario.Cliente;

/**
 * Una cita, con las fechas en la zona del negocio. {@code puedeCancelar} y
 * {@code cancelableHasta} aplican RN-004 al cliente, así la web no repite la regla.
 * {@code cliente} solo sale al empleado y al admin, y {@code cambios} solo al
 * admin (minimización, RNF-007).
 */
public record CitaResponse(
        Long id,
        ServicioCita servicio,
        Referencia profesional,
        Referencia beneficiario,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        BigDecimal precio,
        EstadoCita estado,
        String motivoCancelacion,
        boolean puedeCancelar,
        OffsetDateTime cancelableHasta,
        @JsonInclude(JsonInclude.Include.NON_NULL) DatosCliente cliente,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<Cambio> cambios) {

    public record ServicioCita(Long id, String nombre, long duracionMinutos) {
    }

    public record Referencia(Long id, String nombre) {
    }

    /** Lo necesario para atender la cita (CU-014). El correo, solo para el admin. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DatosCliente(Long id, String nombre, String apellidos, String telefono, String email) {
    }

    public record Cambio(EstadoCita estadoAnterior, EstadoCita estadoNuevo, OffsetDateTime fecha,
            Long realizadoPor) {
    }

    /** Lo que ve el cliente (CU-010, CU-011). */
    public static CitaResponse de(Cita cita, ZoneId zona, Duration plazoCancelacion, Instant ahora) {
        return de(cita, zona, plazoCancelacion, ahora, null, null);
    }

    /** CU-014: el empleado ve además el nombre y el teléfono del cliente. */
    public static CitaResponse paraEmpleado(Cita cita, ZoneId zona, Duration plazoCancelacion, Instant ahora) {
        Cliente cliente = cita.getCliente();
        return de(cita, zona, plazoCancelacion, ahora, new DatosCliente(cliente.getId(), cliente.getNombre(),
                cliente.getApellidos(), cliente.getTelefono(), null), null);
    }

    /** CU-019: el administrador ve también el correo y el historial de cambios. */
    public static CitaResponse paraAdmin(Cita cita, ZoneId zona, Duration plazoCancelacion, Instant ahora) {
        Cliente cliente = cita.getCliente();
        List<Cambio> cambios = cita.getCambios().stream()
                .map(c -> new Cambio(c.getEstadoAnterior(), c.getEstadoNuevo(), enZona(c.getFecha(), zona),
                        c.getRealizadoPor()))
                .toList();
        return de(cita, zona, plazoCancelacion, ahora, new DatosCliente(cliente.getId(), cliente.getNombre(),
                cliente.getApellidos(), cliente.getTelefono(), cliente.getEmail()), cambios);
    }

    private static CitaResponse de(Cita cita, ZoneId zona, Duration plazo, Instant ahora, DatosCliente cliente,
            List<Cambio> cambios) {
        var servicio = new ServicioCita(cita.getServicio().getId(), cita.getServicio().getNombre(),
                Duration.between(cita.getInicio(), cita.getFin()).toMinutes());
        var profesional = new Referencia(cita.getEmpleado().getId(),
                EmpleadoService.nombrePublico(cita.getEmpleado().getNombre(), cita.getEmpleado().getApellidos()));
        var beneficiario = cita.getBeneficiario() == null ? null
                : new Referencia(cita.getBeneficiario().getId(), cita.getBeneficiario().getNombre());
        return new CitaResponse(cita.getId(), servicio, profesional, beneficiario, enZona(cita.getInicio(), zona),
                enZona(cita.getFin(), zona), cita.getPrecio(), cita.getEstado(), cita.getMotivoCancelacion(),
                cita.puedeCancelarCliente(plazo, ahora), enZona(cita.cancelableHasta(plazo), zona), cliente,
                cambios);
    }

    private static OffsetDateTime enZona(Instant instante, ZoneId zona) {
        return instante.atZone(zona).toOffsetDateTime();
    }
}
