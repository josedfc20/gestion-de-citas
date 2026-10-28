package com.gestiondecitas.cita;

import java.time.Clock;
import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.cita.dto.CitaResponse;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.shared.dto.Pagina;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.usuario.Rol;

/**
 * Consultar y cancelar citas (CU-010 a CU-012). Cada usuario solo ve lo suyo:
 * una cita ajena es un 404, para no revelar que existe (paso 3.3).
 */
@Service
public class CitaService {

    private static final String NO_EXISTE = "La cita no existe.";

    private final CitaRepository citas;
    private final NegocioRepository negocios;
    private final Clock clock;

    public CitaService(CitaRepository citas, NegocioRepository negocios, Clock clock) {
        this.citas = citas;
        this.negocios = negocios;
        this.clock = clock;
    }

    /** CU-010: próximas en orden (en todos los estados) o, con {@code pasadas}, el historial. */
    @Transactional(readOnly = true)
    public Pagina<CitaResponse> misCitas(Long clienteId, boolean pasadas, int pagina, int tamano) {
        Negocio negocio = negocio();
        Instant ahora = clock.instant();
        PageRequest peticion = PageRequest.of(pagina, tamano);
        var resultado = pasadas ? citas.pasadasDeCliente(clienteId, ahora, peticion)
                : citas.proximasDeCliente(clienteId, ahora, peticion);
        return Pagina.de(resultado,
                c -> CitaResponse.de(c, negocio.getZonaHoraria(), negocio.plazoCancelacion(), ahora));
    }

    /** CU-011 y CU-014: el cliente ve las suyas, el empleado las que tiene asignadas y el admin, todas. */
    @Transactional(readOnly = true)
    public CitaResponse detalle(Long usuarioId, Rol rol, Long id) {
        Cita cita = citas.findConDatosById(id)
                .filter(c -> esVisible(c, usuarioId, rol))
                .orElseThrow(() -> new RecursoNoEncontradoException(NO_EXISTE));
        return respuesta(cita, rol);
    }

    /** CU-012 y RN-004 ({@code Secuencia-CancelarCita}). */
    @Transactional
    public CitaResponse cancelarPorCliente(Long clienteId, Long id, String motivo) {
        Cita cita = citas.findConDatosById(id)
                .filter(c -> c.getCliente().getId().equals(clienteId))
                .orElseThrow(() -> new RecursoNoEncontradoException(NO_EXISTE));
        Negocio negocio = negocio();
        Instant ahora = clock.instant();
        cita.cancelarPorCliente(motivo, negocio.plazoCancelacion(), clienteId, ahora);
        return CitaResponse.de(cita, negocio.getZonaHoraria(), negocio.plazoCancelacion(), ahora);
    }

    private static boolean esVisible(Cita cita, Long usuarioId, Rol rol) {
        return switch (rol) {
            case CLIENTE -> cita.getCliente().getId().equals(usuarioId);
            case EMPLEADO -> cita.getEmpleado().getId().equals(usuarioId);
            case ADMIN -> true;
        };
    }

    private CitaResponse respuesta(Cita cita, Rol rol) {
        Negocio negocio = negocio();
        Instant ahora = clock.instant();
        return switch (rol) {
            case CLIENTE -> CitaResponse.de(cita, negocio.getZonaHoraria(), negocio.plazoCancelacion(), ahora);
            case EMPLEADO ->
                CitaResponse.paraEmpleado(cita, negocio.getZonaHoraria(), negocio.plazoCancelacion(), ahora);
            case ADMIN -> CitaResponse.paraAdmin(cita, negocio.getZonaHoraria(), negocio.plazoCancelacion(), ahora);
        };
    }

    private Negocio negocio() {
        return negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
    }
}
