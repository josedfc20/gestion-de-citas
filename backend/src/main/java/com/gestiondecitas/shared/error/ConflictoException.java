package com.gestiondecitas.shared.error;

import java.util.Map;

/**
 * La operación choca con el estado actual de los datos (→ 409) y la respuesta
 * lleva datos extra para que la web lo explique, por ejemplo {@code citasFuturas}
 * al desactivar a un empleado con citas pendientes (CU-017, alt. 2).
 */
public class ConflictoException extends RuntimeException {

    private final transient Map<String, Object> propiedades;

    public ConflictoException(String mensaje, Map<String, Object> propiedades) {
        super(mensaje);
        this.propiedades = Map.copyOf(propiedades);
    }

    public Map<String, Object> getPropiedades() {
        return propiedades;
    }
}
