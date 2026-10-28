package com.gestiondecitas.cita;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.cita.dto.AgendaDiaResponse;
import com.gestiondecitas.cita.dto.CitaResponse;
import com.gestiondecitas.horario.ConsultaHorarios;
import com.gestiondecitas.horario.HorarioSemanal;
import com.gestiondecitas.horario.dto.TramoDto;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.shared.error.PeticionNoValidaException;

/** CU-013: la jornada del empleado (su horario efectivo) y, encima, sus citas no canceladas. */
@Service
public class AgendaService {

    static final int MAX_DIAS = 31;

    private final CitaRepository citas;
    private final ConsultaHorarios horarios;
    private final NegocioRepository negocios;
    private final Clock clock;

    public AgendaService(CitaRepository citas, ConsultaHorarios horarios, NegocioRepository negocios, Clock clock) {
        this.citas = citas;
        this.horarios = horarios;
        this.negocios = negocios;
        this.clock = clock;
    }

    /** Sin fechas, hoy; sin {@code hasta}, solo el día {@code desde}. Como mucho {@value #MAX_DIAS} días. */
    @Transactional(readOnly = true)
    public List<AgendaDiaResponse> agenda(Long empleadoId, LocalDate desde, LocalDate hasta) {
        Negocio negocio = negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
        ZoneId zona = negocio.getZonaHoraria();
        LocalDate primero = desde != null ? desde : LocalDate.now(clock.withZone(zona));
        LocalDate ultimo = hasta != null ? hasta : primero;
        if (ultimo.isBefore(primero)) {
            throw new PeticionNoValidaException("La fecha final es anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(primero, ultimo) >= MAX_DIAS) {
            throw new PeticionNoValidaException("La agenda se consulta como mucho " + MAX_DIAS + " días seguidos.");
        }
        HorarioSemanal horario = horarios.deEmpleado(empleadoId);
        Set<LocalDate> cierres = horarios.cierres(empleadoId, primero, ultimo);
        Instant ahora = clock.instant();
        Map<LocalDate, List<CitaResponse>> porDia = citas.agenda(empleadoId, primero.atStartOfDay(zona).toInstant(),
                        ultimo.plusDays(1).atStartOfDay(zona).toInstant()).stream()
                .collect(Collectors.groupingBy(c -> c.getInicio().atZone(zona).toLocalDate(),
                        Collectors.mapping(c -> CitaResponse.paraEmpleado(c, zona, negocio.plazoCancelacion(), ahora),
                                Collectors.toList())));
        List<AgendaDiaResponse> dias = new ArrayList<>();
        for (LocalDate dia = primero; !dia.isAfter(ultimo); dia = dia.plusDays(1)) {
            boolean cierre = cierres.contains(dia);
            List<TramoDto> tramos = cierre ? List.of()
                    : horario.tramos(dia.getDayOfWeek()).stream().map(TramoDto::de).toList();
            dias.add(new AgendaDiaResponse(dia, cierre, tramos, porDia.getOrDefault(dia, List.of())));
        }
        return dias;
    }
}
