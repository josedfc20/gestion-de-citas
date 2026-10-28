package com.gestiondecitas;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;

@IntegracionTest
class ArranqueTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationContext contexto;

    @Test
    void flywayAplicaTodasLasMigraciones() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("6");
    }

    /**
     * Pendiente del B1: sin un {@link UserDetailsService}, Spring Boot ya no crea el
     * usuario de pruebas ni escribe "Using generated security password" al arrancar.
     */
    @Test
    void noSeCreaElUsuarioConContrasenaGenerada() {
        assertThat(contexto.getBeanNamesForType(UserDetailsService.class)).isEmpty();
    }

    @Test
    void existeElRegistroDelNegocioConLosValoresPorDefecto() {
        var negocio = jdbc.queryForMap("SELECT * FROM negocio");

        assertThat(negocio)
                .containsEntry("zona_horaria", "Europe/Madrid")
                .containsEntry("intervalo_minutos", 30)
                .containsEntry("antelacion_max_dias", 60)
                .containsEntry("horas_min_cancelacion", 24)
                .containsEntry("edad_minima_cuenta", 14);
    }
}
