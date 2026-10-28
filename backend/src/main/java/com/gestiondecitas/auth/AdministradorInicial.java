package com.gestiondecitas.auth;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.usuario.Usuario;
import com.gestiondecitas.usuario.UsuarioRepository;

/**
 * Crea el primer administrador al arrancar a partir de {@code ADMIN_EMAIL} y
 * {@code ADMIN_PASSWORD} (decisión del 28/10), para no guardar contraseñas en Git.
 * Si el correo ya existe no toca nada: cambiar el .env después no cambia la contraseña.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String email;
    private final String password;
    private final String nombre;

    public AdministradorInicial(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, Clock clock,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password,
            @Value("${app.admin.nombre:Administrador}") String nombre) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.email = Usuario.normalizarEmail(email);
        this.password = password;
        this.nombre = nombre;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("ADMIN_EMAIL o ADMIN_PASSWORD están vacíos: no se crea el administrador inicial. "
                    + "Rellénalos en el .env si aún no tienes ninguno.");
            return;
        }
        if (usuarios.existsByEmail(email)) {
            log.info("El administrador inicial ya existe; no se modifica.");
            return;
        }
        PoliticaPassword.incumplimiento(password).ifPresent(motivo -> {
            throw new IllegalStateException("ADMIN_PASSWORD no cumple la política de contraseñas: " + motivo);
        });
        Usuario admin = usuarios.save(new Usuario(nombre, "", email, passwordEncoder.encode(password),
                clock.instant()));
        log.info("Administrador inicial creado (id {}).", admin.getId());
    }
}
