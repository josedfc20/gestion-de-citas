package com.gestiondecitas.disponibilidad;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.disponibilidad.dto.DisponibilidadResponse;

/** Consulta pública: no hace falta iniciar sesión (CU-007). */
@Tag(name = "Catálogo", description = "Servicios y profesionales, sin iniciar sesión (CU-005, CU-006)")
@RestController
@RequestMapping("/api/v1/disponibilidad")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @Operation(summary = "Horas libres de un profesional para un servicio en una fecha",
            description = "Horas en hora local del negocio. Si no hay ninguna, siguienteFechaConHuecos dice el "
                    + "próximo día con huecos (CU-007, RN-009, RN-010).")
    @GetMapping
    public DisponibilidadResponse consultar(@RequestParam Long servicioId, @RequestParam Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return disponibilidadService.consultar(servicioId, profesionalId, fecha);
    }
}
