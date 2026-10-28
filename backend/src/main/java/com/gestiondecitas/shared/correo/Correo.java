package com.gestiondecitas.shared.correo;

/** Un correo de texto plano para una sola persona. */
public record Correo(String para, String asunto, String texto) {
}
