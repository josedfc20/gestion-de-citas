package com.gestiondecitas.cita;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.gestiondecitas.beneficiario.CitasDeBeneficiario;

@Component
class CitasDeBeneficiarioJpa implements CitasDeBeneficiario {

    private final CitaRepository citas;
    private final Clock clock;

    CitasDeBeneficiarioJpa(CitaRepository citas, Clock clock) {
        this.citas = citas;
        this.clock = clock;
    }

    @Override
    public boolean tieneCitas(Long beneficiarioId) {
        return citas.existsByBeneficiarioId(beneficiarioId);
    }

    @Override
    public void cancelarFuturas(Long beneficiarioId, String motivo, Long realizadoPor) {
        Instant ahora = clock.instant();
        citas.findByBeneficiarioIdAndEstadoAndInicioAfter(beneficiarioId, EstadoCita.CONFIRMADA, ahora)
                .forEach(c -> c.cambiarEstado(EstadoCita.CANCELADA, motivo, realizadoPor, ahora));
    }
}
