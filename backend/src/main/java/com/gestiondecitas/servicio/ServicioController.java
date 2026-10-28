package com.gestiondecitas.servicio;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.servicio.dto.ServicioResponse;

/** Catálogo público: no hace falta iniciar sesión (CU-005). */
@Tag(name = "Catálogo", description = "Servicios y profesionales, sin iniciar sesión (CU-005, CU-006)")
@RestController
@RequestMapping("/api/v1/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @Operation(summary = "Servicios activos con su duración y precio")
    @GetMapping
    public List<ServicioResponse> listar() {
        return servicioService.listarActivos();
    }

    @Operation(summary = "Un servicio activo")
    @GetMapping("/{id}")
    public ServicioResponse obtener(@PathVariable Long id) {
        return servicioService.obtenerActivo(id);
    }
}
