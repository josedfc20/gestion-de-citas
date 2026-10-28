package com.gestiondecitas.servicio;

/**
 * RN-011: la duración de cada servicio es múltiplo del intervalo de reserva
 * (RN-010), para que una cita siempre termine donde puede empezar otra.
 */
public final class DuracionServicio {

    private DuracionServicio() {
    }

    public static boolean esMultiplo(int duracionMinutos, int intervaloMinutos) {
        return duracionMinutos > 0 && duracionMinutos % intervaloMinutos == 0;
    }
}
