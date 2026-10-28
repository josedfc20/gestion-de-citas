package com.gestiondecitas.usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.auth.RefreshTokenRepository;
import com.gestiondecitas.auth.TokenRecuperacionRepository;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.usuario.dto.DatosPersonalesResponse;

/**
 * RNF-007 (RGPD): exportar los datos personales del usuario y eliminar la cuenta
 * anonimizándola (paso 2.6). B6 añadirá los beneficiarios y las citas a ambas.
 */
@Service
public class DatosPersonalesService {

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final TokenRecuperacionRepository tokensRecuperacion;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DatosPersonalesService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens,
            TokenRecuperacionRepository tokensRecuperacion, PasswordEncoder passwordEncoder, Clock clock) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.tokensRecuperacion = tokensRecuperacion;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DatosPersonalesResponse exportar(Long usuarioId) {
        Usuario usuario = buscar(usuarioId);
        Instant ahora = clock.instant();
        var cuenta = new DatosPersonalesResponse.Cuenta(usuario.getId(), usuario.getRol(), usuario.getNombre(),
                usuario.getApellidos(), usuario.getEmail(), usuario.getTelefono(), usuario.getCreadoEn());
        var cliente = usuario instanceof Cliente c
                ? new DatosPersonalesResponse.DatosCliente(c.getFechaNacimiento(), c.getPrivacidadAceptadaEn())
                : null;
        var sesiones = refreshTokens.sesionesAbiertas(usuarioId, ahora).stream()
                .map(t -> new DatosPersonalesResponse.Sesion(t.getCreadoEn(), t.getExpiraEn()))
                .toList();
        return new DatosPersonalesResponse(ahora, cuenta, cliente, sesiones, List.of(), List.of());
    }

    /**
     * CU-004, flujo alternativo 3. Pide la contraseña como confirmación, anonimiza
     * la cuenta y borra sus sesiones y enlaces de recuperación. Solo para clientes
     * (lo comprueba la seguridad por ruta).
     */
    @Transactional
    public void eliminarCuenta(Long usuarioId, String password) {
        Usuario usuario = buscar(usuarioId);
        if (usuario.isEliminado() || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña no es correcta.");
        }
        refreshTokens.borrarDeUsuario(usuarioId);
        tokensRecuperacion.borrarDeUsuario(usuarioId);
        // Una contraseña aleatoria que nadie conoce: la cuenta ya no puede iniciar sesión.
        usuario.anonimizar(passwordEncoder.encode(UUID.randomUUID().toString()), clock.instant());
    }

    private Usuario buscar(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }
}
