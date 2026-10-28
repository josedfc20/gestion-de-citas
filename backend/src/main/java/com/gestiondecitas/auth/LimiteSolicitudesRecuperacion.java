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

/**
 * Límite de solicitudes de "recuperar contraseña" (paso 3.4): 3 por hora por
 * correo y 10 por hora por IP, igual que {@link LimiteIntentosLogin}. Cuenta
 * aunque el correo no exista, así el 429 tampoco revela qué cuentas hay.
 */
@Component
public class LimiteSolicitudesRecuperacion {

    static final int POR_CORREO = 3;
    static final int POR_IP = 10;
    private static final Duration PERIODO = Duration.ofHours(1);

    private final int porCorreo;
    private final int porIp;
    /** Los cubos sin uso desaparecen solos para no llenar la memoria. */
    private final Cache<String, Bucket> cubos = Caffeine.newBuilder()
            .expireAfterAccess(PERIODO.plusMinutes(10))
            .maximumSize(100_000)
            .build();

    public LimiteSolicitudesRecuperacion() {
        this(POR_CORREO, POR_IP);
    }

    LimiteSolicitudesRecuperacion(int porCorreo, int porIp) {
        this.porCorreo = porCorreo;
        this.porIp = porIp;
    }

    /** Gasta una solicitud de la IP y otra del correo; si alguna se ha agotado lanza 429. */
    public void registrarSolicitud(String ip, String emailNormalizado) {
        consumir("ip:" + ip, porIp);
        consumir("email:" + emailNormalizado, porCorreo);
    }

    private void consumir(String clave, int capacidad) {
        ConsumptionProbe resultado = cubos.get(clave, k -> nuevoCubo(capacidad)).tryConsumeAndReturnRemaining(1);
        if (!resultado.isConsumed()) {
            long segundos = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(resultado.getNanosToWaitForRefill()) + 1);
            throw new DemasiadosIntentosException(
                    "Has pedido demasiados enlaces. Espera un rato y vuelve a probar.", segundos);
        }
    }

    private static Bucket nuevoCubo(int capacidad) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacidad)
                        .refillIntervally(capacidad, PERIODO)
                        .build())
                .build();
    }
}
