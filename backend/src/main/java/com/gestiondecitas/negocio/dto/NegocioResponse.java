package com.gestiondecitas.negocio.dto;

import com.gestiondecitas.negocio.Negocio;

public record NegocioResponse(String nombre, String zonaHoraria, int intervaloMinutos, int antelacionMaxDias,
        int horasMinCancelacion, int edadMinimaCuenta) {

    public static NegocioResponse de(Negocio negocio) {
        return new NegocioResponse(negocio.getNombre(), negocio.getZonaHoraria().getId(),
                negocio.getIntervaloMinutos(), negocio.getAntelacionMaxDias(), negocio.getHorasMinCancelacion(),
                negocio.getEdadMinimaCuenta());
    }
}
