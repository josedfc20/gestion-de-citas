package com.gestiondecitas.cita;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.beneficiario.Beneficiario;
import com.gestiondecitas.beneficiario.BeneficiarioService;
import com.gestiondecitas.cita.dto.CitaResponse;
import com.gestiondecitas.cita.dto.ReservaRequest;
import com.gestiondecitas.disponibilidad.DisponibilidadService;
import com.gestiondecitas.empleado.EmpleadoRepository;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.servicio.Servicio;
import com.gestiondecitas.servicio.ServicioRepository;
import com.gestiondecitas.shared.error.HuecoNoDisponibleException;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.usuario.Cliente;
import com.gestiondecitas.usuario.Empleado;
import com.gestiondecitas.usuario.UsuarioRepository;

/**
 * CU-008 y CU-009 (y CU-019, alt. 1, cuando reserva el administrador), según
 * {@code Secuencia-ReservarCita}: comprueba el hueco con la disponibilidad
 * (RF-007) y deja a la restricción {@code no_solape_profesional} las reservas
 * simultáneas (RNF-003).
 */
@Service
public class ReservaService {

    static final String HUECO_OCUPADO = "Ese horario ya no está disponible. Elige otra hora.";

    private final CitaRepository citas;
    private final UsuarioRepository usuarios;
    private final ServicioRepository servicios;
    private final EmpleadoRepository empleados;
    private final NegocioRepository negocios;
    private final BeneficiarioService beneficiarios;
    private final DisponibilidadService disponibilidad;
    private final Clock clock;

    public ReservaService(CitaRepository citas, UsuarioRepository usuarios, ServicioRepository servicios,
            EmpleadoRepository empleados, NegocioRepository negocios, BeneficiarioService beneficiarios,
            DisponibilidadService disponibilidad, Clock clock) {
        this.citas = citas;
        this.usuarios = usuarios;
        this.servicios = servicios;
        this.empleados = empleados;
        this.negocios = negocios;
        this.beneficiarios = beneficiarios;
        this.disponibilidad = disponibilidad;
        this.clock = clock;
    }

    /**
     * @param clienteId    el titular de la cita
     * @param realizadoPor quien reserva: el propio cliente o el administrador
     */
    @Transactional
    public CitaResponse reservar(Long clienteId, ReservaRequest datos, Long realizadoPor) {
        Negocio negocio = negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
        ZoneId zona = negocio.getZonaHoraria();
        Cliente cliente = usuarios.findById(clienteId)
                .filter(u -> u instanceof Cliente && u.isActivo())
                .map(Cliente.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe."));
        Beneficiario beneficiario = datos.beneficiarioId() == null ? null
                : beneficiarios.paraReservar(clienteId, datos.beneficiarioId());

        // RF-007: la hora tiene que seguir estando entre las libres de ese día.
        Instant inicio = datos.inicio().toInstant();
        ZonedDateTime local = inicio.atZone(zona);
        List<LocalTime> libres = disponibilidad.horasLibres(datos.servicioId(), datos.profesionalId(),
                local.toLocalDate());
        LocalTime hora = local.toLocalTime();
        if (!libres.contains(hora) || !local.toLocalDate().atTime(hora).atZone(zona).toInstant().equals(inicio)) {
            throw new HuecoNoDisponibleException(HUECO_OCUPADO);
        }

        Servicio servicio = servicios.findById(datos.servicioId()).orElseThrow();
        Empleado profesional = empleados.findById(datos.profesionalId()).orElseThrow();
        Instant ahora = clock.instant();
        Cita cita = Cita.reservar(cliente, profesional, servicio, beneficiario, inicio, realizadoPor, ahora);
        comprobarQueLaPersonaEstaLibre(cita, !clienteId.equals(realizadoPor));
        try {
            citas.saveAndFlush(cita);
        } catch (DataIntegrityViolationException ex) {
            // RNF-003: otra reserva del mismo hueco se guardó entre la comprobación y este insert.
            if (String.valueOf(ex.getMessage()).contains("no_solape_profesional")) {
                throw new HuecoNoDisponibleException(HUECO_OCUPADO);
            }
            throw ex;
        }
        return CitaResponse.de(cita, zona, negocio.plazoCancelacion(), ahora);
    }

    /** Decisión 5 del B6: una misma persona no puede estar en dos citas a la vez. */
    private void comprobarQueLaPersonaEstaLibre(Cita cita, boolean reservaElAdmin) {
        Beneficiario beneficiario = cita.getBeneficiario();
        boolean ocupada = beneficiario == null
                ? citas.clienteOcupado(cita.getCliente().getId(), cita.getInicio(), cita.getFin(),
                        OcupacionesProfesionalJpa.QUE_OCUPAN)
                : citas.beneficiarioOcupado(beneficiario.getId(), cita.getInicio(), cita.getFin(),
                        OcupacionesProfesionalJpa.QUE_OCUPAN);
        if (ocupada) {
            String quien = beneficiario != null ? beneficiario.getNombre() + " ya tiene"
                    : reservaElAdmin ? "El cliente ya tiene" : "Ya tienes";
            throw new ReglaNegocioException(quien + " otra cita que coincide con esa hora.");
        }
    }
}
