package com.gestiondecitas.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.auth.dto.RestablecerPasswordRequest;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.usuario.Usuario;
import com.gestiondecitas.usuario.UsuarioRepository;

/** CU-020 · Recuperar contraseña con un enlace de un solo uso que caduca en 30 minutos. */
@Service
public class RecuperacionPasswordService {

    static final Duration DURACION_ENLACE = Duration.ofMinutes(30);
    static final String MENSAJE_SOLICITUD =
            "Si el correo está registrado, recibirás un enlace en unos minutos.";
    static final String ENLACE_NO_VALIDO = "El enlace no es válido o ha caducado. Solicita uno nuevo.";

    private final UsuarioRepository usuarios;
    private final TokenRecuperacionRepository tokensRecuperacion;
    private final RefreshTokenRepository refreshTokens;
    private final NegocioRepository negocios;
    private final PasswordEncoder passwordEncoder;
    private final LimiteSolicitudesRecuperacion limite;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;
    private final String urlWeb;

    public RecuperacionPasswordService(UsuarioRepository usuarios, TokenRecuperacionRepository tokensRecuperacion,
            RefreshTokenRepository refreshTokens, NegocioRepository negocios, PasswordEncoder passwordEncoder,
            LimiteSolicitudesRecuperacion limite, ApplicationEventPublisher eventos, Clock clock,
            @Value("${app.web-url}") String urlWeb) {
        this.usuarios = usuarios;
        this.tokensRecuperacion = tokensRecuperacion;
        this.refreshTokens = refreshTokens;
        this.negocios = negocios;
        this.passwordEncoder = passwordEncoder;
        this.limite = limite;
        this.eventos = eventos;
        this.clock = clock;
        this.urlWeb = urlWeb;
    }

    /**
     * Pasos 1 a 3. No dice si la cuenta existe: el controlador responde siempre
     * lo mismo y el correo sale en segundo plano, así tarda lo mismo en ambos casos.
     */
    @Transactional
    public void solicitar(String email, String ip) {
        String normalizado = Usuario.normalizarEmail(email);
        limite.registrarSolicitud(ip, normalizado);

        Usuario usuario = usuarios.findByEmail(normalizado).orElse(null);
        if (usuario == null || !usuario.isActivo() || usuario.isEliminado()) {
            return;
        }
        tokensRecuperacion.invalidarPendientes(usuario.getId());
        String token = TokenAleatorio.generar();
        Instant expira = clock.instant().plus(DURACION_ENLACE);
        tokensRecuperacion.save(new TokenRecuperacion(usuario, TokenAleatorio.sha256(token), expira));

        String negocio = negocios.findFirstByOrderByIdAsc().map(Negocio::getNombre).orElse("GestionDeCitas");
        eventos.publishEvent(new CorreoRecuperacion(usuario.getEmail(), usuario.getNombre(), negocio,
                CorreoRecuperacion.enlace(urlWeb, token)));
    }

    /**
     * Pasos 4 a 6. Guarda la nueva contraseña, gasta el enlace y cierra todas las
     * sesiones por si alguien más tenía la cuenta. No inicia sesión.
     */
    @Transactional
    public void restablecer(RestablecerPasswordRequest datos) {
        TokenRecuperacion token = tokensRecuperacion.findByTokenHash(TokenAleatorio.sha256(datos.token()))
                .filter(t -> t.esValido(clock.instant()))
                .orElseThrow(() -> new ReglaNegocioException(ENLACE_NO_VALIDO));
        Usuario usuario = token.getUsuario();
        if (!usuario.isActivo() || usuario.isEliminado()) {
            throw new ReglaNegocioException(ENLACE_NO_VALIDO);
        }
        // Si era un empleado con contraseña temporal, la nueva ya es suya (RN-005).
        usuario.cambiarPassword(passwordEncoder.encode(datos.password()));
        token.usar();
        refreshTokens.revocarTodos(usuario.getId());
    }
}
