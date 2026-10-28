package com.gestiondecitas.negocio;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.negocio.dto.NegocioRequest;
import com.gestiondecitas.negocio.dto.NegocioResponse;

@Tag(name = "Admin · Negocio", description = "Configuración del negocio (RN-004, RN-006, RN-009, RN-010)")
@RestController
@RequestMapping("/api/v1/admin/negocio")
public class AdminNegocioController {

    private final NegocioService negocioService;

    public AdminNegocioController(NegocioService negocioService) {
        this.negocioService = negocioService;
    }

    @Operation(summary = "Ver la configuración del negocio")
    @GetMapping
    public NegocioResponse obtener() {
        return negocioService.obtener();
    }

    @Operation(summary = "Cambiar la configuración del negocio",
            description = "Si algún servicio dejaría de ser múltiplo del nuevo intervalo (RN-011), 422 con la lista.")
    @PutMapping
    public NegocioResponse actualizar(@Valid @RequestBody NegocioRequest datos) {
        return negocioService.actualizar(datos);
    }
}
