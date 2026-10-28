package com.gestiondecitas.empleado.dto;

import java.util.List;

import com.gestiondecitas.servicio.dto.ServicioResumen;

/**
 * Profesional en la zona pública (CU-006). Solo el nombre y la inicial del
 * primer apellido ("Luis G."): nada de correo ni teléfono (RNF-007).
 */
public record ProfesionalResponse(Long id, String nombre, List<ServicioResumen> servicios) {
}
