package com.gestiondecitas.auth;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.auth.dto.CambioPasswordRequest;
import com.gestiondecitas.auth.dto.LoginRequest;
import com.gestiondecitas.auth.dto.RegistroRequest;
import com.gestiondecitas.auth.dto.UsuarioResumen;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.shared.error.DatoDuplicadoException;
import com.gestiondecitas.shared.error.NoAutenticadoException;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.shared.error.SinPermisoException;
import com.gestiondecitas.usuario.Cliente;
import com.gestiondecitas.usuario.Usuario;
import com.gestiondecitas.usuario.UsuarioRepository;

/**
 * Registro (CU-001), inicio de sesión (CU-002), renovación, cierre de sesión
 * (CU-003) y cambio de contraseña (CU-004, RN-005).
 */
@Service
public class AuthService {

    static final String CREDENCIALES_NO_VALIDAS = "Correo o contraseña no válidos.";
    static final String SESION_NO_VALIDA = "La sesión ha caducado. Vuelve a iniciar sesión.";

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final NegocioRepository negocios;
    private final PasswordEncoder passwordEncoder;
    private final TokensDeSesion tokens;
    private final LimiteIntentosLogin limiteIntentos;
    private final Clock clock;
    /** Hash con el que se compara cuando el correo no existe, para tardar lo mismo. */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, NegocioRepository negocios,
            PasswordEncoder passwordEncoder, TokensDeSesion tokens, LimiteIntentosLogin limiteIntentos, Clock clock) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.negocios = negocios;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.limiteIntentos = limiteIntentos;
        this.clock = clock;
        this.hashFalso = passwordEncoder.encode("contraseña-que-nunca-coincide");
    }

    /** CU-001. Crea una cuenta de cliente; no inicia sesión (la web lleva al login). */
    @Transactional
    public UsuarioResumen registrar(RegistroRequest datos) {
        String email = Usuario.normalizarEmail(datos.email());
        if (usuarios.existsByEmail(email)) {
            throw new DatoDuplicadoException("Ya existe una cuenta con ese correo.");
        }
        Negocio negocio = negocio();
        LocalDate hoy = LocalDate.now(clock.withZone(negocio.getZonaHoraria()));
        if (!tieneEdadMinima(datos.fechaNacimiento(), hoy, negocio.getEdadMinimaCuenta())) {
            throw new ReglaNegocioException("Debes tener al menos " + negocio.getEdadMinimaCuenta()
                    + " años para crear una cuenta. Un adulto puede reservar citas por ti.");
        }
        Cliente cliente = new Cliente(datos.nombre().trim(), datos.apellidos().trim(), email,
                vacioANull(datos.telefono()), passwordEncoder.encode(datos.password()), datos.fechaNacimiento(),
                clock.instant());
        try {
            return UsuarioResumen.de(usuarios.saveAndFlush(cliente));
        } catch (DataIntegrityViolationException ex) {
            // Dos registros con el mismo correo a la vez: lo para el índice único.
            throw new DatoDuplicadoException("Ya existe una cuenta con ese correo.");
        }
    }

    /** CU-002. Mismo mensaje si falla el correo o la contraseña. */
    @Transactional
    public SesionEmitida login(LoginRequest credenciales, String ip) {
        String email = Usuario.normalizarEmail(credenciales.email());
        limiteIntentos.registrarIntento(ip, email);

        Usuario usuario = usuarios.findByEmail(email).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(credenciales.password(), hashFalso);
            throw new NoAutenticadoException(CREDENCIALES_NO_VALIDAS);
        }
        if (!passwordEncoder.matches(credenciales.password(), usuario.getPasswordHash())) {
            throw new NoAutenticadoException(CREDENCIALES_NO_VALIDAS);
        }
        if (!usuario.isActivo()) {
            throw new SinPermisoException("Tu cuenta está desactivada. Ponte en contacto con el negocio.");
        }
        return tokens.emitir(usuario);
    }

    /**
     * Renueva la sesión con rotación: el token usado se revoca y se crea otro. Si
     * llega un token ya revocado, alguien lo ha copiado: se cierran todas las
     * sesiones del usuario (por eso no se deshace la transacción al lanzar el 401).
     */
    @Transactional(noRollbackFor = NoAutenticadoException.class)
    public SesionEmitida renovar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new NoAutenticadoException(SESION_NO_VALIDA);
        }
        RefreshToken guardado = refreshTokens.findByTokenHash(TokensDeSesion.hash(refreshToken))
                .orElseThrow(() -> new NoAutenticadoException(SESION_NO_VALIDA));
        Usuario usuario = guardado.getUsuario();
        if (guardado.isRevocado()) {
            refreshTokens.revocarTodos(usuario.getId());
            throw new NoAutenticadoException(SESION_NO_VALIDA);
        }
        if (guardado.haCaducado(clock.instant()) || !usuario.isActivo()) {
            guardado.revocar();
            throw new NoAutenticadoException(SESION_NO_VALIDA);
        }
        guardado.revocar();
        return tokens.emitir(usuario);
    }

    /** CU-003. Siempre termina bien: si no hay sesión, no hay nada que cerrar. */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(TokensDeSesion.hash(refreshToken)).ifPresent(RefreshToken::revocar);
    }

    /**
     * CU-004 y RN-005. Cierra todas las sesiones abiertas (por si alguien conocía
     * la contraseña anterior) y abre una nueva para quien la ha cambiado.
     */
    @Transactional
    public SesionEmitida cambiarPassword(Long usuarioId, CambioPasswordRequest datos) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        if (!passwordEncoder.matches(datos.passwordActual(), usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        usuario.cambiarPassword(passwordEncoder.encode(datos.passwordNueva()));
        refreshTokens.revocarTodos(usuarioId);
        return tokens.emitir(usuario);
    }

    /** RN-006: años cumplidos a fecha de hoy en la zona del negocio. */
    static boolean tieneEdadMinima(LocalDate fechaNacimiento, LocalDate hoy, int edadMinima) {
        return Period.between(fechaNacimiento, hoy).getYears() >= edadMinima;
    }

    private Negocio negocio() {
        return negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
