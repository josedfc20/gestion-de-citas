package com.gestiondecitas.shared.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único de la aplicación (paso 3.2, 4.4). El código nunca llama a
 * {@code now()} sin reloj: inyecta este {@link Clock} y las pruebas usan uno fijo.
 * Trabaja en UTC; la zona del negocio se aplica al calcular horarios.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
