package com.gestiondecitas.horario;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.empleado.EmpleadoRepository;
import com.gestiondecitas.horario.dto.CitaAfectada;
import com.gestiondecitas.horario.dto.DiaCierreRequest;
import com.gestiondecitas.horario.dto.DiaCierreResponse;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.shared.error.DatoDuplicadoException;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.usuario.Empleado;

/** Días de cierre del negocio (festivos) y de un empleado (vacaciones), CU-018. */
@Service
public class DiaCierreService {

    private static final String YA_EXISTE = "Ese día de cierre ya existe.";

    private final DiaCierreRepository diasCierre;
    private final EmpleadoRepository empleados;
    private final NegocioRepository negocios;
    private final CitasFueraDeHorario citasFuera;
    private final Clock clock;

    public DiaCierreService(DiaCierreRepository diasCierre, EmpleadoRepository empleados,
            NegocioRepository negocios, CitasFueraDeHorario citasFuera, Clock clock) {
        this.diasCierre = diasCierre;
        this.empleados = empleados;
        this.negocios = negocios;
        this.citasFuera = citasFuera;
        this.clock = clock;
    }

    /** Sin {@code desde}, de hoy en adelante. */
    @Transactional(readOnly = true)
    public List<DiaCierreResponse> listar(LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : hoy(negocio());
        List<DiaCierre> cierres = diasCierre.listar(inicio, hasta);
        Map<Long, String> nombres = empleados.findAllById(cierres.stream().map(DiaCierre::getEmpleadoId)
                        .filter(Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Empleado::getId, DiaCierreService::nombreCompleto, (a, b) -> a));
        return cierres.stream().map(d -> respuesta(d, nombres.get(d.getEmpleadoId()), null)).toList();
    }

    @Transactional(readOnly = true)
    public DiaCierreResponse obtener(Long id) {
        DiaCierre cierre = buscar(id);
        return respuesta(cierre, nombreDe(cierre.getEmpleadoId()), null);
    }

    /** Se crea aunque haya citas ese día: la respuesta las lista (CU-018, alt. 1). */
    @Transactional
    public DiaCierreResponse crear(DiaCierreRequest datos) {
        Negocio negocio = negocio();
        if (datos.fecha().isBefore(hoy(negocio))) {
            throw new ReglaNegocioException("La fecha ya ha pasado.");
        }
        Long empleadoId = datos.empleadoId();
        String empleado = null;
        if (empleadoId != null) {
            empleado = empleados.findById(empleadoId).map(DiaCierreService::nombreCompleto)
                    .orElseThrow(() -> new ReglaNegocioException("El empleado no existe."));
        }
        if (diasCierre.existe(empleadoId, datos.fecha())) {
            throw new DatoDuplicadoException(YA_EXISTE);
        }
        String motivo = datos.motivo() == null || datos.motivo().isBlank() ? null : datos.motivo().trim();
        DiaCierre cierre = new DiaCierre(negocio.getId(), empleadoId, datos.fecha(), motivo);
        try {
            diasCierre.saveAndFlush(cierre);
        } catch (DataIntegrityViolationException ex) {
            // Dos altas iguales a la vez: lo para el índice único de V6.
            throw new DatoDuplicadoException(YA_EXISTE);
        }
        List<CitaAfectada> afectadas = citasFuera.buscar(empleadoId, negocio.getZonaHoraria()).stream()
                .filter(c -> c.inicio().toLocalDate().equals(datos.fecha()))
                .toList();
        return respuesta(cierre, empleado, afectadas);
    }

    @Transactional
    public void borrar(Long id) {
        diasCierre.delete(buscar(id));
    }

    private DiaCierre buscar(Long id) {
        return diasCierre.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El día de cierre no existe."));
    }

    private String nombreDe(Long empleadoId) {
        return empleadoId == null ? null
                : empleados.findById(empleadoId).map(DiaCierreService::nombreCompleto).orElse(null);
    }

    private static DiaCierreResponse respuesta(DiaCierre cierre, String empleado, List<CitaAfectada> afectadas) {
        return new DiaCierreResponse(cierre.getId(), cierre.getFecha(), cierre.getEmpleadoId(), empleado,
                cierre.getMotivo(), afectadas);
    }

    private static String nombreCompleto(Empleado empleado) {
        return (empleado.getNombre() + " " + empleado.getApellidos()).trim();
    }

    private LocalDate hoy(Negocio negocio) {
        return LocalDate.now(clock.withZone(negocio.getZonaHoraria()));
    }

    private Negocio negocio() {
        return negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
    }
}
