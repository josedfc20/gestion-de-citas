package com.gestiondecitas.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Política mínima de contraseñas (paso 3.4, recomendación del NIST): longitud y
 * no estar en la lista de contraseñas más comunes. Sin reglas de símbolos.
 */
public final class PoliticaPassword {

    public static final int LONGITUD_MINIMA = 10;
    /** BCrypt solo usa los primeros 72 bytes; 64 caracteres deja margen para tildes y ñ. */
    public static final int LONGITUD_MAXIMA = 64;

    private static final Set<String> COMUNES = cargarComunes();

    private PoliticaPassword() {
    }

    /** Devuelve el motivo por el que la contraseña no es válida, o vacío si cumple la política. */
    public static Optional<String> incumplimiento(String password) {
        if (password == null || password.length() < LONGITUD_MINIMA || password.length() > LONGITUD_MAXIMA) {
            return Optional.of("Debe tener entre " + LONGITUD_MINIMA + " y " + LONGITUD_MAXIMA + " caracteres.");
        }
        if (COMUNES.contains(password.toLowerCase(Locale.ROOT))) {
            return Optional.of("Es una contraseña demasiado común; elige otra.");
        }
        return Optional.empty();
    }

    private static Set<String> cargarComunes() {
        try (InputStream entrada = PoliticaPassword.class.getResourceAsStream("passwords-comunes.txt")) {
            if (entrada == null) {
                throw new IllegalStateException("Falta el recurso passwords-comunes.txt");
            }
            try (BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
                return lector.lines()
                        .map(String::trim)
                        .filter(linea -> !linea.isEmpty() && !linea.startsWith("#"))
                        .map(linea -> linea.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
