package com.gestiondecitas.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Firma y validación de los access tokens (JWT con HS256 y la clave
 * {@code JWT_SECRET}). Al definir aquí el {@link JwtDecoder}, Spring Boot ya no
 * crea el usuario con contraseña generada que aparecía en B1.
 */
@Configuration
public class JwtConfig {

    /** Nombre del claim con el rol del usuario. */
    public static final String CLAIM_ROL = "rol";
    /** Claim "debe cambiar la contraseña" (RN-005). */
    public static final String CLAIM_DEBE_CAMBIAR_PASSWORD = "dcp";

    @Bean
    SecretKey claveJwt(PropiedadesSeguridad propiedades) {
        return new SecretKeySpec(propiedades.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey claveJwt, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(claveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator caducidad = new JwtTimestampValidator();
        caducidad.setClock(clock);
        decoder.setJwtValidator(caducidad);
        return decoder;
    }

    /** Convierte el claim {@code rol} en la autoridad {@code ROLE_<rol>} que usa {@code hasRole}. */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
