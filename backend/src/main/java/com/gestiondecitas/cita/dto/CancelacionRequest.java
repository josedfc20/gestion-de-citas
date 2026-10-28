package com.gestiondecitas.cita.dto;

import jakarta.validation.constraints.Size;

/** CU-012: el motivo es opcional para el cliente. */
public record CancelacionRequest(@Size(max = 300) String motivo) {
}
