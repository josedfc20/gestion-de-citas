package com.gestiondecitas.auth;

import com.gestiondecitas.shared.correo.Correo;

/**
 * Correo con el enlace para restablecer la contraseña (CU-020, paso 3). Se crea
 * dentro de la transacción y se envía cuando ya está confirmada.
 *
 * @param enlace página de la web con el token detrás de {@code #}, para que no
 *               viaje en la petición de la página ni quede en logs o en el {@code Referer}
 */
record CorreoRecuperacion(String email, String nombre, String negocio, String enlace) {

    static String enlace(String urlWeb, String token) {
        String base = urlWeb.endsWith("/") ? urlWeb.substring(0, urlWeb.length() - 1) : urlWeb;
        return base + "/restablecer-password#token=" + token;
    }

    Correo correo() {
        String texto = """
                Hola, %s:

                Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en %s.

                Para elegir una contraseña nueva, abre este enlace:
                %s

                El enlace caduca en %d minutos y solo se puede usar una vez.
                Si no lo has pedido tú, ignora este correo: tu contraseña no cambia.
                """.formatted(nombre, negocio, enlace, RecuperacionPasswordService.DURACION_ENLACE.toMinutes());
        return new Correo(email, "Restablecer tu contraseña · " + negocio, texto);
    }

    /** Sin el enlace: el token nunca se escribe en el log. */
    @Override
    public String toString() {
        return "CorreoRecuperacion[usuario oculto, enlace oculto]";
    }
}
