package com.gestiondecitas.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Activa {@code @Async}: el correo de recuperación se envía en segundo plano para
 * que la respuesta tarde lo mismo exista o no la cuenta (CU-020, paso 4.3).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
