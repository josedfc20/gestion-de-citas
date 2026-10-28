package com.gestiondecitas.auth;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import org.springframework.stereotype.Component;

import com.gestiondecitas.shared.error.DemasiadosIntentosException;
import com.gestiondecitas.shared.security.PropiedadesSeguridad;

/**
 * Límite de intentos de login por IP y por correo (paso 3.4), con Bucket4j en
 * memoria: cada clave tiene un "cubo" de N fichas que se rellena cada minuto.
 * Vale para un solo servidor; con varios habría que guardarlo en Redis o similar.
 */
@Component
public class LimiteIntentosLogin {

    private static final Duration PERIODO = Duration.ofMinutes(1);

    private final int intentosPorMinuto;
    /** Los cubos sin uso desaparecen solos para no llenar la memoria. */
    private final Cache<String, Bucket> cubos = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    public LimiteIntentosLogin(PropiedadesSeguridad propiedades) {
        this.intentosPorMinuto = propiedades.intentosLoginPorMinuto();
    }

    /** Gasta un intento de la IP y otro del correo; si alguno se ha agotado lanza 429. */
    public void registrarIntento(String ip, String emailNormalizado) {
        consumir("ip:" + ip);
        consumir("email:" + emailNormalizado);
    }

    private void consumir(String clave) {
        ConsumptionProbe resultado = cubos.get(clave, k -> nuevoCubo()).tryConsumeAndReturnRemaining(1);
        if (!resultado.isConsumed()) {
            long segundos = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(resultado.getNanosToWaitForRefill()) + 1);
            throw new DemasiadosIntentosException(
                    "Demasiados intentos de inicio de sesión. Espera un minuto y vuelve a probar.", segundos);
        }
    }

    private Bucket nuevoCubo() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(intentosPorMinuto)
                        .refillIntervally(intentosPorMinuto, PERIODO)
                        .build())
                .build();
    }
}
