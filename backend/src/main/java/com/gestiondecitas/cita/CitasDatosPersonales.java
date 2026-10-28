package com.gestiondecitas.cita;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Component;

import com.gestiondecitas.cita.dto.CitaExportada;
import com.gestiondecitas.empleado.EmpleadoService;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.usuario.AportaDatosPersonales;

/** Las citas del cliente en {@code GET /me/datos} y qué pasa con ellas al eliminar la cuenta (RNF-007). */
@Component
class CitasDatosPersonales implements AportaDatosPersonales {

    static final String CUENTA_ELIMINADA = "Cuenta eliminada";

    private final CitaRepository citas;
    private final NegocioRepository negocios;

    CitasDatosPersonales(CitaRepository citas, NegocioRepository negocios) {
        this.citas = citas;
        this.negocios = negocios;
    }

    @Override
    public String apartado() {
        return "citas";
    }

    @Override
    public List<CitaExportada> exportar(Long clienteId) {
        ZoneId zona = negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."))
                .getZonaHoraria();
        return citas.findByClienteIdOrderByInicioAsc(clienteId).stream()
                .map(c -> new CitaExportada(c.getId(), c.getServicio().getNombre(),
                        EmpleadoService.nombrePublico(c.getEmpleado().getNombre(), c.getEmpleado().getApellidos()),
                        c.getBeneficiario() == null ? null : c.getBeneficiario().getNombre(),
                        c.getInicio().atZone(zona).toOffsetDateTime(), c.getFin().atZone(zona).toOffsetDateTime(),
                        c.getPrecio(), c.getEstado(), c.getMotivoCancelacion(),
                        c.getCreadaEn().atZone(zona).toOffsetDateTime()))
                .toList();
    }

    /** Las citas pasadas quedan como historial; las futuras confirmadas se cancelan sin plazo. */
    @Override
    public void alEliminarCuenta(Long clienteId, Instant ahora) {
        citas.findByClienteIdAndEstadoAndInicioAfter(clienteId, EstadoCita.CONFIRMADA, ahora)
                .forEach(c -> c.cambiarEstado(EstadoCita.CANCELADA, CUENTA_ELIMINADA, clienteId, ahora));
    }
}
