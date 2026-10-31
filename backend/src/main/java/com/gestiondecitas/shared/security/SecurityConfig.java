package com.gestiondecitas.shared.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad de la API (paso 3.4): sin sesiones en el servidor, access token JWT
 * en la cabecera {@code Authorization: Bearer ...} y roles por ruta. La propiedad
 * de cada recurso (solo ves lo tuyo) se comprueba en los servicios.
 */
@Configuration
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
        "/actuator/health",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/error"
    };

    private static final String[] AUTH_PUBLICO = {
        "/api/v1/auth/registro",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        "/api/v1/auth/recuperar-password",
        "/api/v1/auth/restablecer-password"
    };

    /** Catálogo público (B4, B5) y configuración pública del negocio (B7). */
    private static final String[] CONSULTAS_PUBLICAS = {
        "/api/v1/negocio",
        "/api/v1/servicios/**",
        "/api/v1/profesionales/**",
        "/api/v1/disponibilidad"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ProblemDetailsSecurityHandler problemDetailsHandler,
            JwtAuthenticationConverter jwtConverter)
            throws Exception {
        http
                // API sin sesiones y con tokens: no hay cookie de sesión que proteger con CSRF.
                // La cookie del refresh token se protege con SameSite=Strict.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(problemDetailsHandler)
                        .accessDeniedHandler(problemDetailsHandler))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(problemDetailsHandler)
                        .accessDeniedHandler(problemDetailsHandler))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.POST, AUTH_PUBLICO).permitAll()
                        .requestMatchers(HttpMethod.GET, CONSULTAS_PUBLICAS).permitAll()
                        // RN-005: con la contraseña temporal solo se puede ver el perfil y cambiarla.
                        .requestMatchers(HttpMethod.GET, "/api/v1/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/me/password").authenticated()
                        // RNF-007: solo los clientes eliminan su cuenta (empleados y admin, no).
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/me")
                                .access(AuthorizationManagers.allOf(rol("CLIENTE"), conPasswordDefinitiva()))
                        // CU-008 a CU-012: lo que reserva y cancela el cliente.
                        .requestMatchers("/api/v1/me/beneficiarios/**")
                                .access(AuthorizationManagers.allOf(rol("CLIENTE"), conPasswordDefinitiva()))
                        .requestMatchers(HttpMethod.GET, "/api/v1/me/citas")
                                .access(AuthorizationManagers.allOf(rol("CLIENTE"), conPasswordDefinitiva()))
                        .requestMatchers(HttpMethod.POST, "/api/v1/citas", "/api/v1/citas/*/cancelacion")
                                .access(AuthorizationManagers.allOf(rol("CLIENTE"), conPasswordDefinitiva()))
                        // CU-015 y CU-019: cambia el estado el empleado (sus citas) o el administrador.
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/citas/*/estado")
                                .access(AuthorizationManagers.allOf(
                                        AuthorizationManagers.anyOf(rol("EMPLEADO"), rol("ADMIN")),
                                        conPasswordDefinitiva()))
                        .requestMatchers("/api/v1/admin/**")
                                .access(AuthorizationManagers.allOf(rol("ADMIN"), conPasswordDefinitiva()))
                        .requestMatchers("/api/v1/empleado/**")
                                .access(AuthorizationManagers.allOf(rol("EMPLEADO"), conPasswordDefinitiva()))
                        .anyRequest().access(conPasswordDefinitiva()));
        return http.build();
    }

    /** RNF-001: BCrypt con factor de coste 12 (paso 3.4). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Solo la web propia puede llamar a la API desde el navegador (paso 3.4). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> origenesPermitidos) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenesPermitidos);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(true); // la cookie del refresh token
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }

    private static AuthorizationManager<RequestAuthorizationContext> rol(String rol) {
        return AuthorityAuthorizationManager.hasRole(rol);
    }

    /** Usuario con sesión que ya no tiene una contraseña temporal pendiente de cambiar. */
    private static AuthorizationManager<RequestAuthorizationContext> conPasswordDefinitiva() {
        return (autenticacion, contexto) ->
                new AuthorizationDecision(TokenActual.esJwt(autenticacion.get())
                        && !TokenActual.debeCambiarPassword(autenticacion.get()));
    }
}
