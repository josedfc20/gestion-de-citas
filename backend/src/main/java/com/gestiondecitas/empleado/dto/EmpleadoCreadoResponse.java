package com.gestiondecitas.empleado.dto;

/**
 * Respuesta del alta (RN-005): la contraseña temporal solo se devuelve aquí,
 * una vez. No se guarda en claro ni se escribe en el log.
 */
public record EmpleadoCreadoResponse(EmpleadoAdminResponse empleado, String passwordTemporal) {
}
