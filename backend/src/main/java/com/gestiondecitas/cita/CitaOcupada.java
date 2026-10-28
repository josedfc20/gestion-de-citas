package com.gestiondecitas.cita;

import java.time.Instant;

/** Una cita que ocupa a un profesional de {@code inicio} a {@code fin}. */
public record CitaOcupada(Long id, Long empleadoId, String profesional, Instant inicio, Instant fin) {
}
