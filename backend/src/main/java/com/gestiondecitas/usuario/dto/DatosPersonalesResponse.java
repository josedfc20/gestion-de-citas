package com.gestiondecitas.usuario.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.gestiondecitas.usuario.Rol;

/**
 * Exportación de los datos personales ({@code GET /me/datos}, derecho de acceso
 * del RGPD). Nunca lleva el hash de la contraseña ni los de los tokens.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatosPersonalesResponse(
        Instant generadoEn,
        Cuenta cuenta,
        DatosCliente cliente,
        List<Sesion> sesionesAbiertas,
        List<?> beneficiarios,
        List<?> citas) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Cuenta(Long id, Rol rol, String nombre, String apellidos, String email, String telefono,
            Instant creadoEn) {
    }

    /** Solo para clientes. */
    public record DatosCliente(LocalDate fechaNacimiento, Instant privacidadAceptadaEn) {
    }

    public record Sesion(Instant iniciadaEn, Instant expiraEn) {
    }
}
