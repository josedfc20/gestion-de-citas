package com.gestiondecitas.disponibilidad.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Horas de inicio libres (CU-007), en hora local del negocio ("09:30"). Si no hay
 * ninguna, {@code siguienteFechaConHuecos} es el primer día posterior con huecos
 * dentro de la antelación máxima, o vacío si no hay ninguno (alt. 2).
 */
public record DisponibilidadResponse(LocalDate fecha, String zonaHoraria, List<String> horas,
        LocalDate siguienteFechaConHuecos) {
}
