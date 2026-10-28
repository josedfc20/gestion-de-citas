package com.gestiondecitas.beneficiario;

/**
 * Lo que necesita saber y hacer este paquete con las citas de un beneficiario.
 * La implementa el paquete {@code cita}, que ya depende de este (así no hay un
 * ciclo entre paquetes).
 */
public interface CitasDeBeneficiario {

    boolean tieneCitas(Long beneficiarioId);

    /** Cancela sus citas confirmadas que aún no han empezado. */
    void cancelarFuturas(Long beneficiarioId, String motivo, Long realizadoPor);
}
