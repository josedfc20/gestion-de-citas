package com.gestiondecitas.servicio.dto;

import com.gestiondecitas.servicio.Servicio;

/** Referencia corta a un servicio, dentro de otras respuestas. */
public record ServicioResumen(Long id, String nombre) {

    public static ServicioResumen de(Servicio servicio) {
        return new ServicioResumen(servicio.getId(), servicio.getNombre());
    }
}
