package com.gestiondecitas.horario;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.empleado.EmpleadoRepository;
import com.gestiondecitas.horario.dto.CitaAfectada;
import com.gestiondecitas.horario.dto.DiaEfectivoDto;
import com.gestiondecitas.horario.dto.DiaHorarioDto;
import com.gestiondecitas.horario.dto.HorarioEmpleadoResponse;
import com.gestiondecitas.horario.dto.HorarioSemanaRequest;
import com.gestiondecitas.horario.dto.HorarioSemanaResponse;
import com.gestiondecitas.horario.dto.TramoDto;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;

/** Horario general y ajuste por empleado (CU-018, RN-002). */
@Service
public class HorarioService {

    private final TramoHorarioRepository tramos;
    private final AjusteHorarioRepository ajustes;
    private final EmpleadoRepository empleados;
    private final NegocioRepository negocios;
    private final ConsultaHorarios consulta;
    private final CitasFueraDeHorario citasFuera;

    public HorarioService(TramoHorarioRepository tramos, AjusteHorarioRepository ajustes,
            EmpleadoRepository empleados, NegocioRepository negocios, ConsultaHorarios consulta,
            CitasFueraDeHorario citasFuera) {
        this.tramos = tramos;
        this.ajustes = ajustes;
        this.empleados = empleados;
        this.negocios = negocios;
        this.consulta = consulta;
        this.citasFuera = citasFuera;
    }

    @Transactional(readOnly = true)
    public HorarioSemanaResponse general() {
        return new HorarioSemanaResponse(semana(consulta.general()), null);
    }

    /**
     * Reemplaza la semana entera; un día que no llega queda cerrado. Se guarda
     * aunque haya citas que queden fuera: la respuesta las lista (CU-018, alt. 1).
     */
    @Transactional
    public HorarioSemanaResponse cambiarGeneral(HorarioSemanaRequest datos) {
        Negocio negocio = negocioBloqueado();
        Map<DayOfWeek, List<Tramo>> dias = aDias(datos);
        ValidadorTramos.comprobar(dias);
        tramos.borrarGenerales();
        dias.forEach((dia, lista) -> lista.forEach(
                tramo -> tramos.save(new TramoHorario(negocio.getId(), null, dia, tramo))));
        return new HorarioSemanaResponse(semana(consulta.general()),
                citasFuera.buscar(null, negocio.getZonaHoraria()));
    }

    @Transactional(readOnly = true)
    public HorarioEmpleadoResponse deEmpleado(Long empleadoId) {
        comprobarEmpleado(empleadoId);
        return respuesta(empleadoId, consulta.deEmpleado(empleadoId), null);
    }

    /**
     * Reemplaza todos los ajustes del empleado: solo llegan los días ajustados (sin
     * tramos = ese día no trabaja); los demás siguen el horario general.
     */
    @Transactional
    public HorarioEmpleadoResponse cambiarDeEmpleado(Long empleadoId, HorarioSemanaRequest datos) {
        comprobarEmpleado(empleadoId);
        Negocio negocio = negocioBloqueado();
        Map<DayOfWeek, List<Tramo>> dias = aDias(datos);
        ValidadorTramos.comprobar(dias);
        tramos.borrarDeEmpleado(empleadoId);
        ajustes.borrarDeEmpleado(empleadoId);
        dias.forEach((dia, lista) -> {
            ajustes.save(new AjusteHorario(empleadoId, dia));
            lista.forEach(tramo -> tramos.save(new TramoHorario(negocio.getId(), empleadoId, dia, tramo)));
        });
        return respuesta(empleadoId, consulta.deEmpleado(empleadoId),
                citasFuera.buscar(empleadoId, negocio.getZonaHoraria()));
    }

    private HorarioEmpleadoResponse respuesta(Long empleadoId, HorarioSemanal horario,
            List<CitaAfectada> citasAfectadas) {
        List<DiaEfectivoDto> efectiva = Arrays.stream(DayOfWeek.values())
                .map(dia -> new DiaEfectivoDto(dia.getValue(), horario.ajustado(dia), aDto(horario.tramos(dia))))
                .toList();
        return new HorarioEmpleadoResponse(empleadoId, dias(horario.ajustes()), efectiva, citasAfectadas);
    }

    /** Los 7 días de lunes a domingo, con los cerrados sin tramos. */
    private static List<DiaHorarioDto> semana(Map<DayOfWeek, List<Tramo>> general) {
        HorarioSemanal horario = HorarioSemanal.general(general);
        return Arrays.stream(DayOfWeek.values())
                .map(dia -> new DiaHorarioDto(dia.getValue(), aDto(horario.tramos(dia))))
                .toList();
    }

    private static List<DiaHorarioDto> dias(Map<DayOfWeek, List<Tramo>> dias) {
        return dias.entrySet().stream()
                .map(e -> new DiaHorarioDto(e.getKey().getValue(), aDto(e.getValue())))
                .toList();
    }

    private static List<TramoDto> aDto(List<Tramo> lista) {
        return lista.stream().map(TramoDto::de).toList();
    }

    private static Map<DayOfWeek, List<Tramo>> aDias(HorarioSemanaRequest datos) {
        Map<DayOfWeek, List<Tramo>> dias = new EnumMap<>(DayOfWeek.class);
        datos.dias().forEach(d -> dias.put(DayOfWeek.of(d.diaSemana()),
                d.tramos().stream().map(TramoDto::aTramo).toList()));
        return dias;
    }

    private void comprobarEmpleado(Long empleadoId) {
        if (!empleados.existsById(empleadoId)) {
            throw new RecursoNoEncontradoException("El empleado no existe.");
        }
    }

    /** Bloquea la fila del negocio: dos cambios de horario a la vez se aplican uno detrás de otro. */
    private Negocio negocioBloqueado() {
        return negocios.findFirstForUpdateByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
    }
}
