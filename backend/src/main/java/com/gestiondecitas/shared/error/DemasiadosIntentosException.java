package com.gestiondecitas.shared.error;

/**
 * Se ha superado el límite de intentos (paso 3.4 → 429). Indica cuántos
 * segundos hay que esperar, que se envían en la cabecera {@code Retry-After}.
 */
public class DemasiadosIntentosException extends RuntimeException {

    private final long segundosDeEspera;

    public DemasiadosIntentosException(String mensaje, long segundosDeEspera) {
        super(mensaje);
        this.segundosDeEspera = segundosDeEspera;
    }

    public long getSegundosDeEspera() {
        return segundosDeEspera;
    }
}
