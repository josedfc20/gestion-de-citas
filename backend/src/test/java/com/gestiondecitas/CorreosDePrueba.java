package com.gestiondecitas;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gestiondecitas.shared.correo.Correo;
import com.gestiondecitas.shared.correo.EnvioCorreo;

/**
 * Buzón en memoria para las pruebas: guarda los correos en lugar de enviarlos.
 * Cada prueba usa correos distintos, así que no hace falta vaciarlo.
 */
public class CorreosDePrueba implements EnvioCorreo {

    private static final Pattern TOKEN = Pattern.compile("#token=([A-Za-z0-9_-]+)");

    private final List<Correo> enviados = new CopyOnWriteArrayList<>();

    @Override
    public void enviar(Correo correo) {
        enviados.add(correo);
    }

    public List<Correo> para(String email) {
        return enviados.stream().filter(c -> c.para().equalsIgnoreCase(email)).toList();
    }

    /** Token del último enlace de recuperación enviado a ese correo. */
    public String ultimoToken(String email) {
        List<Correo> recibidos = para(email);
        if (recibidos.isEmpty()) {
            throw new AssertionError("No se ha enviado ningún correo a " + email);
        }
        Matcher m = TOKEN.matcher(recibidos.getLast().texto());
        if (!m.find()) {
            throw new AssertionError("El correo no tiene enlace: " + recibidos.getLast().texto());
        }
        return m.group(1);
    }
}
