package com.gestiondecitas.empleado;

import java.security.SecureRandom;

import com.gestiondecitas.auth.PoliticaPassword;

/**
 * RN-005: contraseña temporal que el administrador entrega en persona. 12
 * caracteres de {@link SecureRandom} sin los que se confunden al leerlos
 * (0/O, 1/l/I), y que cumple la política de B2.
 */
final class PasswordTemporal {

    static final int LONGITUD = 12;
    static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private PasswordTemporal() {
    }

    static String generar() {
        String password;
        do {
            StringBuilder texto = new StringBuilder(LONGITUD);
            for (int i = 0; i < LONGITUD; i++) {
                texto.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
            }
            password = texto.toString();
        } while (PoliticaPassword.incumplimiento(password).isPresent());
        return password;
    }
}
