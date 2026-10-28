package com.gestiondecitas.servicio.dto;

import java.math.BigDecimal;

import com.gestiondecitas.servicio.Servicio;

/** Servicio en el panel del administrador: incluye el estado y cuántos empleados activos lo realizan. */
public record ServicioAdminResponse(Long id, String nombre, String descripcion, int duracionMinutos,
        BigDecimal precio, boolean activo, long empleados) {

    public static ServicioAdminResponse de(Servicio servicio, long empleados) {
        return new ServicioAdminResponse(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getDuracionMinutos(), servicio.getPrecio(), servicio.isActivo(), empleados);
    }
}
