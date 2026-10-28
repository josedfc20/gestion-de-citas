package com.gestiondecitas.empleado;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.servicio.dto.ServicioResponse;

/** Zona del empleado: el empleado sale del token, nunca de la URL. */
@Tag(name = "Empleado", description = "Lo que consulta el empleado con sesión")
@RestController
@RequestMapping("/api/v1/empleado")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @Operation(summary = "Mis servicios asignados (CU-021)")
    @GetMapping("/servicios")
    public List<ServicioResponse> misServicios(@AuthenticationPrincipal Jwt jwt) {
        return empleadoService.serviciosDe(Long.valueOf(jwt.getSubject()));
    }
}
