package com.gestiondecitas.servicio.dto;

import java.math.BigDecimal;

import com.gestiondecitas.servicio.Servicio;

/** Servicio tal como lo ve el público y el empleado (CU-005, CU-021). */
public record ServicioResponse(Long id, String nombre, String descripcion, int duracionMinutos, BigDecimal precio) {

    public static ServicioResponse de(Servicio servicio) {
        return new ServicioResponse(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getDuracionMinutos(), servicio.getPrecio());
    }
}
