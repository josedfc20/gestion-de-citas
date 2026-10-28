package com.gestiondecitas.shared.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de seguridad PROVISIONAL (bloque B1). Solo son públicos Swagger y
 * {@code /actuator/health}; el resto de la API responde 401 hasta que el bloque B2
 * añada el login con JWT y los roles por ruta (paso 3.4).
 */
@Configuration
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
        "/actuator/health",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ProblemDetailsSecurityHandler problemDetailsHandler) throws Exception {
        http
                // API sin sesiones y con tokens: no hay cookie de sesión que proteger con CSRF.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(problemDetailsHandler)
                        .accessDeniedHandler(problemDetailsHandler))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /** Solo la web propia puede llamar a la API desde el navegador (paso 3.4). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> origenesPermitidos) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenesPermitidos);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(true); // la cookie del refresh token (B2)
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }
}
