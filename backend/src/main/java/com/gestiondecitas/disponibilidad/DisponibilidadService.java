package com.gestiondecitas.disponibilidad;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.cita.CitaOcupada;
import com.gestiondecitas.cita.OcupacionesProfesional;
import com.gestiondecitas.disponibilidad.CalculoDisponibilidad.Ocupacion;
import com.gestiondecitas.disponibilidad.dto.DisponibilidadResponse;
import com.gestiondecitas.empleado.EmpleadoRepository;
import com.gestiondecitas.horario.ConsultaHorarios;
import com.gestiondecitas.horario.HorarioSemanal;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.servicio.Servicio;
import com.gestiondecitas.servicio.ServicioRepository;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.usuario.Empleado;

/**
 * CU-007: carga el horario, los cierres y las citas del profesional y deja el
 * cálculo a {@link CalculoDisponibilidad}. La reserva lo usa para volver a
 * comprobar el hueco (RF-007).
 */
@Service
public class DisponibilidadService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final NegocioRepository negocios;
    private final ServicioRepository servicios;
    private final EmpleadoRepository empleados;
    private final ConsultaHorarios horarios;
    private final OcupacionesProfesional ocupaciones;
    private final Clock clock;

    public DisponibilidadService(NegocioRepository negocios, ServicioRepository servicios,
            EmpleadoRepository empleados, ConsultaHorarios horarios, OcupacionesProfesional ocupaciones,
            Clock clock) {
        this.negocios = negocios;
        this.servicios = servicios;
        this.empleados = empleados;
        this.horarios = horarios;
        this.ocupaciones = ocupaciones;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse consultar(Long servicioId, Long profesionalId, LocalDate fecha) {
        Consulta consulta = preparar(servicioId, profesionalId, fecha);
        List<LocalTime> horas = horasDe(consulta);
        LocalDate siguiente = horas.isEmpty() ? siguienteConHuecos(consulta) : null;
        return new DisponibilidadResponse(fecha, consulta.zona().getId(),
                horas.stream().map(HH_MM::format).toList(), siguiente);
    }

    /**
     * RF-007: las horas libres de ese día, con las mismas reglas y errores que
     * {@link #consultar}. La usa la reserva para volver a comprobar el hueco.
     */
    @Transactional(readOnly = true)
    public List<LocalTime> horasLibres(Long servicioId, Long profesionalId, LocalDate fecha) {
        return horasDe(preparar(servicioId, profesionalId, fecha));
    }

    /** Valida la petición y carga lo necesario para calcular las horas de ese día. */
    private Consulta preparar(Long servicioId, Long profesionalId, LocalDate fecha) {
        Negocio negocio = negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
        ZoneId zona = negocio.getZonaHoraria();
        LocalDate hoy = LocalDate.now(clock.withZone(zona));
        LocalDate ultimo = hoy.plusDays(negocio.getAntelacionMaxDias());
        // RN-009 (alt. 1).
        if (fecha.isBefore(hoy)) {
            throw new ReglaNegocioException("La fecha ya ha pasado.");
        }
        if (fecha.isAfter(ultimo)) {
            throw new ReglaNegocioException("Solo se puede reservar con " + negocio.getAntelacionMaxDias()
                    + " días de antelación como máximo.");
        }
        Servicio servicio = servicios.findByIdAndActivoTrue(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El servicio no existe."));
        Empleado profesional = empleados.findConServiciosById(profesionalId).filter(Empleado::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("El profesional no existe."));
        if (profesional.getServicios().stream().noneMatch(s -> s.getId().equals(servicioId))) {
            throw new ReglaNegocioException("Ese profesional no realiza este servicio.");
        }
        Dia dia = new Dia(new CalculoDisponibilidad(negocio.getIntervaloMinutos(), zona, clock.instant()),
                horarios.deEmpleado(profesionalId), horarios.cierres(profesionalId, fecha, ultimo),
                servicio.getDuracionMinutos());
        return new Consulta(dia, profesionalId, fecha, ultimo, zona);
    }

    private List<LocalTime> horasDe(Consulta consulta) {
        return consulta.dia().horas(consulta.fecha(),
                ocupacionesEntre(consulta.profesionalId(), consulta.fecha(), consulta.fecha(), consulta.zona()));
    }

    /** CU-007, alt. 2: se cargan de una vez las citas de todo el rango, sin una consulta por día. */
    private LocalDate siguienteConHuecos(Consulta consulta) {
        Dia dia = consulta.dia();
        Long profesionalId = consulta.profesionalId();
        LocalDate fecha = consulta.fecha();
        LocalDate ultimo = consulta.ultimo();
        ZoneId zona = consulta.zona();
        if (!fecha.isBefore(ultimo)) {
            return null;
        }
        List<Ocupacion> delRango = ocupacionesEntre(profesionalId, fecha.plusDays(1), ultimo, zona);
        for (LocalDate d = fecha.plusDays(1); !d.isAfter(ultimo); d = d.plusDays(1)) {
            if (!dia.horas(d, delRango).isEmpty()) {
                return d;
            }
        }
        return null;
    }

    private List<Ocupacion> ocupacionesEntre(Long profesionalId, LocalDate desde, LocalDate hasta, ZoneId zona) {
        Instant inicio = desde.atStartOfDay(zona).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(zona).toInstant();
        return ocupaciones.deEmpleado(profesionalId, inicio, fin).stream()
                .map(DisponibilidadService::aOcupacion)
                .toList();
    }

    private static Ocupacion aOcupacion(CitaOcupada cita) {
        return new Ocupacion(cita.inicio(), cita.fin());
    }

    /** Una consulta ya validada: el día pedido y el último reservable. */
    private record Consulta(Dia dia, Long profesionalId, LocalDate fecha, LocalDate ultimo, ZoneId zona) {
    }

    /** Lo que no cambia de un día a otro para el mismo profesional y servicio. */
    private record Dia(CalculoDisponibilidad calculo, HorarioSemanal horario, Set<LocalDate> cierres,
            int duracionMinutos) {

        List<LocalTime> horas(LocalDate fecha, List<Ocupacion> ocupaciones) {
            if (cierres.contains(fecha)) {
                return List.of();
            }
            return calculo.horas(fecha, horario.tramos(fecha.getDayOfWeek()), duracionMinutos, ocupaciones);
        }
    }
}
