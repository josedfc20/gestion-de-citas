package com.gestiondecitas.beneficiario.dto;

import java.time.LocalDate;

import com.gestiondecitas.beneficiario.Beneficiario;

public record BeneficiarioResponse(Long id, String nombre, String apellidos, LocalDate fechaNacimiento) {

    public static BeneficiarioResponse de(Beneficiario beneficiario) {
        return new BeneficiarioResponse(beneficiario.getId(), beneficiario.getNombre(), beneficiario.getApellidos(),
                beneficiario.getFechaNacimiento());
    }
}
