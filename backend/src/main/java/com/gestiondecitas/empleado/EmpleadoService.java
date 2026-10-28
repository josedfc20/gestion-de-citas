package com.gestiondecitas.empleado;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.auth.RefreshTokenRepository;
import com.gestiondecitas.cita.CitasFuturas;
import com.gestiondecitas.empleado.dto.AltaEmpleadoRequest;
import com.gestiondecitas.empleado.dto.DatosEmpleadoRequest;
import com.gestiondecitas.empleado.dto.EmpleadoAdminResponse;
import com.gestiondecitas.empleado.dto.EmpleadoCreadoResponse;
import com.gestiondecitas.empleado.dto.PasswordTemporalResponse;
import com.gestiondecitas.empleado.dto.ProfesionalResponse;
import com.gestiondecitas.servicio.Servicio;
import com.gestiondecitas.servicio.ServicioRepository;
import com.gestiondecitas.servicio.dto.ServicioResponse;
import com.gestiondecitas.servicio.dto.ServicioResumen;
import com.gestiondecitas.shared.error.ConflictoException;
import com.gestiondecitas.shared.error.DatoDuplicadoException;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.usuario.Empleado;
import com.gestiondecitas.usuario.Usuario;
import com.gestiondecitas.usuario.UsuarioRepository;

/**
 * Gestión de empleados del administrador (CU-017), profesionales públicos
 * (CU-006) y servicios del empleado (CU-021).
 */
@Service
public class EmpleadoService {

    private static final String CORREO_EN_USO = "Ese correo ya lo usa otra cuenta.";
    private static final Comparator<Servicio> POR_NOMBRE = Comparator.comparing(Servicio::getNombre);

    private final EmpleadoRepository empleados;
    private final UsuarioRepository usuarios;
    private final ServicioRepository servicios;
    private final RefreshTokenRepository refreshTokens;
    private final CitasFuturas citasFuturas;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public EmpleadoService(EmpleadoRepository empleados, UsuarioRepository usuarios, ServicioRepository servicios,
            RefreshTokenRepository refreshTokens, CitasFuturas citasFuturas, PasswordEncoder passwordEncoder,
            Clock clock) {
        this.empleados = empleados;
        this.usuarios = usuarios;
        this.servicios = servicios;
        this.refreshTokens = refreshTokens;
        this.citasFuturas = citasFuturas;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    // ---------- Zona pública y empleado ----------

    /** CU-006: profesionales activos con sus servicios activos; con servicio, solo quienes lo realizan. */
    @Transactional(readOnly = true)
    public List<ProfesionalResponse> profesionales(Long servicioId) {
        if (servicioId != null && servicios.findByIdAndActivoTrue(servicioId).isEmpty()) {
            throw new RecursoNoEncontradoException("El servicio no existe.");
        }
        return empleados.findByActivoTrueOrderByNombreAscApellidosAsc().stream()
                .filter(e -> servicioId == null
                        || e.getServicios().stream().anyMatch(s -> s.getId().equals(servicioId)))
                .map(e -> new ProfesionalResponse(e.getId(), nombrePublico(e.getNombre(), e.getApellidos()),
                        serviciosActivos(e).stream().map(ServicioResumen::de).toList()))
                .toList();
    }

    /** CU-021: los servicios activos que tiene asignados el empleado con sesión. */
    @Transactional(readOnly = true)
    public List<ServicioResponse> serviciosDe(Long empleadoId) {
        return serviciosActivos(buscar(empleadoId)).stream().map(ServicioResponse::de).toList();
    }

    // ---------- Administrador ----------

    @Transactional(readOnly = true)
    public List<EmpleadoAdminResponse> listar() {
        return empleados.findAllByOrderByNombreAscApellidosAsc().stream().map(EmpleadoAdminResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public EmpleadoAdminResponse obtener(Long id) {
        return EmpleadoAdminResponse.de(buscar(id));
    }

    /** CU-017, pasos 2 a 5. RN-005: la contraseña temporal solo sale en esta respuesta. */
    @Transactional
    public EmpleadoCreadoResponse alta(AltaEmpleadoRequest datos) {
        String email = Usuario.normalizarEmail(datos.email());
        if (usuarios.existsByEmail(email)) {
            throw new DatoDuplicadoException(CORREO_EN_USO);
        }
        List<Servicio> asignados = serviciosParaAsignar(datos.servicioIds(), Set.of());
        String passwordTemporal = PasswordTemporal.generar();
        Empleado empleado = new Empleado(datos.nombre().trim(), datos.apellidos().trim(), email,
                vacioANull(datos.telefono()), passwordEncoder.encode(passwordTemporal), clock.instant());
        empleado.asignarServicios(asignados);
        try {
            empleados.saveAndFlush(empleado);
        } catch (DataIntegrityViolationException ex) {
            throw new DatoDuplicadoException(CORREO_EN_USO);
        }
        return new EmpleadoCreadoResponse(EmpleadoAdminResponse.de(empleado), passwordTemporal);
    }

    @Transactional
    public EmpleadoAdminResponse actualizar(Long id, DatosEmpleadoRequest datos) {
        Empleado empleado = buscar(id);
        String email = Usuario.normalizarEmail(datos.email());
        if (usuarios.existsByEmailAndIdNot(email, id)) {
            throw new DatoDuplicadoException(CORREO_EN_USO);
        }
        empleado.actualizarPerfil(datos.nombre().trim(), datos.apellidos().trim(), email,
                vacioANull(datos.telefono()));
        try {
            empleados.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new DatoDuplicadoException(CORREO_EN_USO);
        }
        return EmpleadoAdminResponse.de(empleado);
    }

    /**
     * CU-017, paso 5 y alt. 3: reemplaza la lista. Quitar un servicio con citas
     * futuras está permitido; las citas se mantienen y se avisa al administrador.
     */
    @Transactional
    public EmpleadoAdminResponse asignarServicios(Long id, Set<Long> servicioIds) {
        Empleado empleado = buscar(id);
        List<Servicio> nuevos = serviciosParaAsignar(servicioIds, empleado.getServicios());
        List<String> avisos = new ArrayList<>();
        empleado.getServicios().stream()
                .filter(actual -> !servicioIds.contains(actual.getId()))
                .sorted(POR_NOMBRE)
                .forEach(quitado -> {
                    long citas = citasFuturas.deEmpleadoYServicio(id, quitado.getId(), clock.instant());
                    if (citas > 0) {
                        avisos.add("Tiene " + citas + (citas == 1 ? " cita futura" : " citas futuras")
                                + " de «" + quitado.getNombre() + "»: se mantienen.");
                    }
                });
        empleado.asignarServicios(nuevos);
        return EmpleadoAdminResponse.de(empleado, avisos);
    }

    /**
     * CU-017, alt. 2: no se desactiva a quien tiene citas futuras confirmadas (409);
     * el administrador las cancela o reasigna antes. Al desactivar se cierran sus sesiones.
     */
    @Transactional
    public EmpleadoAdminResponse cambiarActivo(Long id, boolean activo) {
        Empleado empleado = buscar(id);
        if (activo) {
            if (empleado.isEliminado()) {
                throw new ReglaNegocioException("Una cuenta eliminada no se puede reactivar.");
            }
            empleado.activar();
            return EmpleadoAdminResponse.de(empleado);
        }
        long citas = citasFuturas.deEmpleado(id, clock.instant());
        if (citas > 0) {
            throw new ConflictoException("Tiene " + citas + (citas == 1 ? " cita futura" : " citas futuras")
                    + ". Cancélalas o reasígnalas antes de desactivarlo.", Map.of("citasFuturas", citas));
        }
        empleado.desactivar();
        refreshTokens.revocarTodos(id);
        return EmpleadoAdminResponse.de(empleado);
    }

    /** RN-005: otra contraseña temporal si la pierde; la anterior deja de valer y se cierran sus sesiones. */
    @Transactional
    public PasswordTemporalResponse nuevaPasswordTemporal(Long id) {
        Empleado empleado = buscar(id);
        String passwordTemporal = PasswordTemporal.generar();
        empleado.nuevaPasswordTemporal(passwordEncoder.encode(passwordTemporal));
        refreshTokens.revocarTodos(id);
        return new PasswordTemporalResponse(passwordTemporal);
    }

    // ---------- Auxiliares ----------

    /**
     * Solo se añaden servicios existentes y activos (422 si no). Uno desactivado que
     * ya tenía asignado puede seguir en la lista: desactivar no quita asignaciones.
     */
    private List<Servicio> serviciosParaAsignar(Set<Long> ids, Set<Servicio> yaAsignados) {
        Set<Long> idsYaAsignados = yaAsignados.stream().map(Servicio::getId).collect(Collectors.toSet());
        Map<Long, Servicio> encontrados = servicios.findAllById(ids).stream()
                .collect(Collectors.toMap(Servicio::getId, Function.identity()));
        List<Long> noValidos = ids.stream()
                .filter(id -> !encontrados.containsKey(id)
                        || !(encontrados.get(id).isActivo() || idsYaAsignados.contains(id)))
                .sorted()
                .toList();
        if (!noValidos.isEmpty()) {
            throw new ReglaNegocioException("Estos servicios no existen o están desactivados: " + noValidos + ".");
        }
        return List.copyOf(encontrados.values());
    }

    private static List<Servicio> serviciosActivos(Empleado empleado) {
        return empleado.getServicios().stream().filter(Servicio::isActivo).sorted(POR_NOMBRE).toList();
    }

    private Empleado buscar(Long id) {
        return empleados.findConServiciosById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El empleado no existe."));
    }

    /** "Luis", "García López" → "Luis G." (RNF-007: lo mínimo para distinguirlos). */
    public static String nombrePublico(String nombre, String apellidos) {
        String apellido = apellidos == null ? "" : apellidos.trim();
        if (apellido.isEmpty()) {
            return nombre.trim();
        }
        return nombre.trim() + " " + apellido.substring(0, apellido.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT) + ".";
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
