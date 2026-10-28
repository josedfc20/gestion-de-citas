package com.gestiondecitas.usuario;

import java.time.Instant;
import java.util.List;

/**
 * Otro paquete que guarda datos personales de un cliente (beneficiarios, citas).
 * {@link DatosPersonalesService} los reúne al exportar y les avisa al eliminar la
 * cuenta, sin depender de esos paquetes (que ya dependen de este).
 */
public interface AportaDatosPersonales {

    /** Nombre del apartado en la exportación ({@code beneficiarios}, {@code citas}). */
    String apartado();

    List<?> exportar(Long clienteId);

    /** Se llama dentro de la transacción que anonimiza la cuenta. */
    void alEliminarCuenta(Long clienteId, Instant ahora);
}
