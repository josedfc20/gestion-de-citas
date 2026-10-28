package com.gestiondecitas.beneficiario.dto;

import java.util.List;

/** Un cliente en la búsqueda del administrador, con sus menores a cargo para elegir para quién reserva. */
public record ClienteEncontradoResponse(Long id, String nombre, String apellidos, String email, String telefono,
        List<BeneficiarioResponse> beneficiarios) {
}
