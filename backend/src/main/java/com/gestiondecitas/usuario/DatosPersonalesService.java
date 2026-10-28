package com.gestiondecitas.usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
 * anonimizándola (paso 2.6). Los beneficiarios y las citas los aportan sus
 * paquetes con {@link AportaDatosPersonales}.
 */
@Service
public class DatosPersonalesService {

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final TokenRecuperacionRepository tokensRecuperacion;
    private final PasswordEncoder passwordEncoder;
    private final List<AportaDatosPersonales> otrosDatos;
    private final Clock clock;

    public DatosPersonalesService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens,
            TokenRecuperacionRepository tokensRecuperacion, PasswordEncoder passwordEncoder,
            List<AportaDatosPersonales> otrosDatos, Clock clock) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.tokensRecuperacion = tokensRecuperacion;
        this.passwordEncoder = passwordEncoder;
        this.otrosDatos = otrosDatos;
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
        Map<String, List<?>> apartados = usuario instanceof Cliente
                ? otrosDatos.stream()
                        .collect(Collectors.toMap(AportaDatosPersonales::apartado, o -> o.exportar(usuarioId)))
                : Map.of();
        return new DatosPersonalesResponse(ahora, cuenta, cliente, sesiones,
                apartados.getOrDefault("beneficiarios", List.of()), apartados.getOrDefault("citas", List.of()));
    }

    /**
     * CU-004, flujo alternativo 3. Pide la contraseña como confirmación, anonimiza
     * la cuenta y borra sus sesiones y enlaces de recuperación. Sus beneficiarios se
     * anonimizan y sus citas futuras confirmadas se cancelan con el motivo "Cuenta
     * eliminada". Solo para clientes (lo comprueba la seguridad por ruta).
     */
    @Transactional
    public void eliminarCuenta(Long usuarioId, String password) {
        Usuario usuario = buscar(usuarioId);
        if (usuario.isEliminado() || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña no es correcta.");
        }
        refreshTokens.borrarDeUsuario(usuarioId);
        tokensRecuperacion.borrarDeUsuario(usuarioId);
        Instant ahora = clock.instant();
        otrosDatos.forEach(o -> o.alEliminarCuenta(usuarioId, ahora));
        // Una contraseña aleatoria que nadie conoce: la cuenta ya no puede iniciar sesión.
        usuario.anonimizar(passwordEncoder.encode(UUID.randomUUID().toString()), ahora);
    }

    private Usuario buscar(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }
}
