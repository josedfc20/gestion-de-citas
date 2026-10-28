package com.gestiondecitas.cita.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.gestiondecitas.cita.EstadoCita;

/** Una cita en la exportación de datos personales ({@code GET /me/datos}). */
public record CitaExportada(Long id, String servicio, String profesional, String beneficiario,
        OffsetDateTime inicio, OffsetDateTime fin, BigDecimal precio, EstadoCita estado, String motivoCancelacion,
        OffsetDateTime reservadaEn) {
}
