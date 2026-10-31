package com.gestiondecitas.cita;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.cita.dto.CambioEstadoRequest;
import com.gestiondecitas.cita.dto.CitaResponse;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.shared.dto.Pagina;
import com.gestiondecitas.shared.error.PeticionNoValidaException;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.usuario.Rol;

/**
 * Consultar, cancelar y cambiar el estado de las citas (CU-010 a CU-012, CU-014,
 * CU-015 y CU-019). Cada usuario solo ve lo suyo:
 * una cita ajena es un 404, para no revelar que existe (paso 3.3).
 */
@Service
public class CitaService {

    private static final String NO_EXISTE = "La cita no existe.";

    private final CitaRepository citas;
    private final NegocioRepository negocios;
    private final AutoresDeCambios autores;
    private final Clock clock;

    public CitaService(CitaRepository citas, NegocioRepository negocios, AutoresDeCambios autores, Clock clock) {
        this.citas = citas;
        this.negocios = negocios;
        this.autores = autores;
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

    /** CU-015 y CU-019: el empleado, en sus citas; el administrador, en todas. */
    @Transactional
    public CitaResponse cambiarEstado(Long usuarioId, Rol rol, Long id, CambioEstadoRequest datos) {
        Cita cita = citas.findConDatosById(id)
                .filter(c -> rol != Rol.CLIENTE && esVisible(c, usuarioId, rol))
                .orElseThrow(() -> new RecursoNoEncontradoException(NO_EXISTE));
        cita.cambiarEstado(datos.estado(), datos.motivo(), usuarioId, clock.instant());
        return respuesta(cita, rol);
    }

    /**
     * CU-014 y CU-019: lista con filtros, en orden. Las fechas son días en la zona del
     * negocio; sin {@code desde}, de hoy en adelante.
     */
    @Transactional(readOnly = true)
    public Pagina<CitaResponse> listar(Rol rol, Long empleadoId, Long clienteId, EstadoCita estado,
            LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        Negocio negocio = negocio();
        ZoneId zona = negocio.getZonaHoraria();
        LocalDate inicio = desde != null ? desde : LocalDate.now(clock.withZone(zona));
        if (hasta != null && hasta.isBefore(inicio)) {
            throw new PeticionNoValidaException("La fecha final es anterior a la inicial.");
        }
        var filtro = new FiltroCitas(empleadoId, clienteId, estado, inicio.atStartOfDay(zona).toInstant(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(zona).toInstant());
        var peticion = PageRequest.of(pagina, tamano, Sort.by("inicio", "id"));
        return Pagina.de(citas.findAll(filtro.comoSpecification(), peticion), c -> respuesta(c, rol, negocio,
                clock.instant(), null));
    }

    private static boolean esVisible(Cita cita, Long usuarioId, Rol rol) {
        return switch (rol) {
            case CLIENTE -> cita.getCliente().getId().equals(usuarioId);
            case EMPLEADO -> cita.getEmpleado().getId().equals(usuarioId);
            case ADMIN -> true;
        };
    }

    private CitaResponse respuesta(Cita cita, Rol rol) {
        return respuesta(cita, rol, negocio(), clock.instant(), rol == Rol.ADMIN ? autores.de(cita) : null);
    }

    /**
     * Lo que ve cada rol de una cita. Con {@code autoresCambios} (solo en el detalle
     * del admin) sale el historial con el nombre de quien hizo cada cambio.
     */
    static CitaResponse respuesta(Cita cita, Rol rol, Negocio negocio, Instant ahora,
            Map<Long, String> autoresCambios) {
        ZoneId zona = negocio.getZonaHoraria();
        return switch (rol) {
            case CLIENTE -> CitaResponse.de(cita, zona, negocio.plazoCancelacion(), ahora);
            case EMPLEADO -> CitaResponse.paraEmpleado(cita, zona, negocio.plazoCancelacion(), ahora);
            case ADMIN -> CitaResponse.paraAdmin(cita, zona, negocio.plazoCancelacion(), ahora, autoresCambios);
        };
    }

    Negocio negocio() {
        return negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
    }
}
