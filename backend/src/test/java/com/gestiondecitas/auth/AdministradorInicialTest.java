package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import com.gestiondecitas.IntegracionTest;

/** Primer administrador desde ADMIN_EMAIL y ADMIN_PASSWORD (en las pruebas, application-test.yml). */
@IntegracionTest
class AdministradorInicialTest {

    @Autowired
    private AdministradorInicial administradorInicial;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void seCreaAlArrancarConLaContrasenaEnBcrypt() {
        var admin = jdbc.queryForMap("SELECT rol, nombre, password_hash FROM usuario WHERE email = ?",
                "admin@pruebas.local");

        assertThat(admin.get("rol")).isEqualTo("ADMIN");
        assertThat(admin.get("nombre")).isEqualTo("Admin Pruebas");
        assertThat((String) admin.get("password_hash")).startsWith("$2a$12$").doesNotContain("Admin-de-pruebas");
    }

    @Test
    void volverAArrancarNoLoDuplicaNiLoModifica() {
        String hashAntes = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE email = ?", String.class,
                "admin@pruebas.local");

        administradorInicial.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = ?", Integer.class,
                "admin@pruebas.local")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT password_hash FROM usuario WHERE email = ?", String.class,
                "admin@pruebas.local")).isEqualTo(hashAntes);
    }
}
