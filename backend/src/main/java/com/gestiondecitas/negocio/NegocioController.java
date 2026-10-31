package com.gestiondecitas.negocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.negocio.dto.NegocioResponse;

/**
 * Configuración pública del negocio (B7): la web la usa para el nombre de la
 * cabecera, el límite del selector de fechas y el aviso de la edad mínima. No
 * contiene datos personales.
 */
@Tag(name = "Catálogo", description = "Servicios y profesionales, sin iniciar sesión (CU-005, CU-006)")
@RestController
@RequestMapping("/api/v1/negocio")
public class NegocioController {

    private final NegocioService negocioService;

    public NegocioController(NegocioService negocioService) {
        this.negocioService = negocioService;
    }

    @Operation(summary = "Nombre y reglas públicas del negocio",
            description = "Zona horaria, intervalo, antelación máxima, plazo de cancelación y edad mínima.")
    @GetMapping
    public NegocioResponse obtener() {
        return negocioService.obtener();
    }
}
