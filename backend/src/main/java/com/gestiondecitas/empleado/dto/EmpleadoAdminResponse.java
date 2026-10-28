package com.gestiondecitas.empleado.dto;

import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.gestiondecitas.servicio.dto.ServicioResumen;
import com.gestiondecitas.usuario.Empleado;

/**
 * Empleado en el panel del administrador. {@code avisos} solo aparece al cambiar
 * sus servicios (CU-017, alt. 3).
 */
public record EmpleadoAdminResponse(Long id, String nombre, String apellidos, String email, String telefono,
        boolean activo, boolean debeCambiarPassword, List<ServicioResumen> servicios,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> avisos) {

    public static EmpleadoAdminResponse de(Empleado empleado) {
        return de(empleado, List.of());
    }

    public static EmpleadoAdminResponse de(Empleado empleado, List<String> avisos) {
        List<ServicioResumen> servicios = empleado.getServicios().stream()
                .sorted(Comparator.comparing(s -> s.getNombre()))
                .map(ServicioResumen::de)
                .toList();
        return new EmpleadoAdminResponse(empleado.getId(), empleado.getNombre(), empleado.getApellidos(),
                empleado.getEmail(), empleado.getTelefono(), empleado.isActivo(), empleado.debeCambiarPassword(),
                servicios, avisos);
    }
}
