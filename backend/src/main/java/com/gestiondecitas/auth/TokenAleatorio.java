package com.gestiondecitas.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens aleatorios de un solo dueño (refresh token y enlace de recuperación):
 * 32 bytes de {@link SecureRandom} en Base64 para URL. En la base de datos solo
 * se guarda su SHA-256, que permite buscarlos pero no reconstruirlos.
 */
final class TokenAleatorio {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokenAleatorio() {
    }

    static String generar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
