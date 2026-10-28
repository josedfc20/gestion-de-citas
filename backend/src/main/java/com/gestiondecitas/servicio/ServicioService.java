package com.gestiondecitas.servicio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.servicio.dto.ServicioAdminResponse;
import com.gestiondecitas.servicio.dto.ServicioRequest;
import com.gestiondecitas.servicio.dto.ServicioResponse;
import com.gestiondecitas.shared.error.DatoDuplicadoException;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.shared.error.ReglaNegocioException;

/** Catálogo público (CU-005) y gestión de servicios y precios del administrador (CU-016). */
@Service
public class ServicioService {

    private static final String NOMBRE_EN_USO = "Ya existe un servicio con ese nombre.";

    private final ServicioRepository servicios;
    private final NegocioRepository negocios;

    public ServicioService(ServicioRepository servicios, NegocioRepository negocios) {
        this.servicios = servicios;
        this.negocios = negocios;
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listarActivos() {
        return servicios.findByActivoTrueOrderByNombreAsc().stream().map(ServicioResponse::de).toList();
    }

    /** Un servicio inactivo no existe para el público (404). */
    @Transactional(readOnly = true)
    public ServicioResponse obtenerActivo(Long id) {
        return ServicioResponse.de(servicios.findByIdAndActivoTrue(id).orElseThrow(ServicioService::noEncontrado));
    }

    @Transactional(readOnly = true)
    public List<ServicioAdminResponse> listarTodos() {
        Map<Long, Long> empleados = servicios.contarEmpleadosActivos().stream()
                .collect(Collectors.toMap(ServicioRepository.EmpleadosPorServicio::getServicioId,
                        ServicioRepository.EmpleadosPorServicio::getEmpleados));
        return servicios.findAllByOrderByNombreAsc().stream()
                .map(s -> ServicioAdminResponse.de(s, empleados.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicioAdminResponse obtener(Long id) {
        return conEmpleados(buscar(id));
    }

    @Transactional
    public ServicioAdminResponse crear(ServicioRequest datos) {
        String nombre = normalizarNombre(datos.nombre());
        if (servicios.existsByNombreIgnoreCase(nombre)) {
            throw new DatoDuplicadoException(NOMBRE_EN_USO);
        }
        comprobarDuracion(datos.duracionMinutos());
        Servicio servicio = new Servicio(nombre, vacioANull(datos.descripcion()), datos.duracionMinutos(),
                datos.precio());
        guardar(servicio);
        return ServicioAdminResponse.de(servicio, 0);
    }

    /** CU-016, alt. 2: el nuevo precio o duración solo afecta a las reservas nuevas. */
    @Transactional
    public ServicioAdminResponse actualizar(Long id, ServicioRequest datos) {
        Servicio servicio = buscar(id);
        String nombre = normalizarNombre(datos.nombre());
        if (servicios.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new DatoDuplicadoException(NOMBRE_EN_USO);
        }
        comprobarDuracion(datos.duracionMinutos());
        servicio.actualizar(nombre, vacioANull(datos.descripcion()), datos.duracionMinutos(), datos.precio());
        guardar(servicio);
        return conEmpleados(servicio);
    }

    /** CU-016, alt. 1: desactivar lo oculta y deja de poder reservarse; las asignaciones se conservan. */
    @Transactional
    public ServicioAdminResponse cambiarActivo(Long id, boolean activo) {
        Servicio servicio = buscar(id);
        servicio.cambiarActivo(activo);
        return conEmpleados(servicio);
    }

    /**
     * RN-011. Bloquea la fila del negocio para que un cambio de intervalo a la vez
     * no deje este servicio con una duración que ya no es múltiplo.
     */
    private void comprobarDuracion(int duracionMinutos) {
        Negocio negocio = negocios.findFirstForUpdateByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
        if (!DuracionServicio.esMultiplo(duracionMinutos, negocio.getIntervaloMinutos())) {
            throw new ReglaNegocioException("La duración debe ser múltiplo de " + negocio.getIntervaloMinutos()
                    + " minutos, el intervalo de reserva del negocio.");
        }
    }

    private void guardar(Servicio servicio) {
        try {
            servicios.saveAndFlush(servicio);
        } catch (DataIntegrityViolationException ex) {
            // Dos altas con el mismo nombre a la vez: lo para el índice único de V5.
            throw new DatoDuplicadoException(NOMBRE_EN_USO);
        }
    }

    private ServicioAdminResponse conEmpleados(Servicio servicio) {
        long empleados = servicios.contarEmpleadosActivos().stream()
                .filter(fila -> fila.getServicioId().equals(servicio.getId()))
                .mapToLong(ServicioRepository.EmpleadosPorServicio::getEmpleados)
                .findFirst().orElse(0);
        return ServicioAdminResponse.de(servicio, empleados);
    }

    private Servicio buscar(Long id) {
        return servicios.findById(id).orElseThrow(ServicioService::noEncontrado);
    }

    private static RecursoNoEncontradoException noEncontrado() {
        return new RecursoNoEncontradoException("El servicio no existe.");
    }

    /** "  Corte   de pelo " → "Corte de pelo". */
    static String normalizarNombre(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ");
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
