package com.gestiondecitas.empleado;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.empleado.dto.ProfesionalResponse;

@Tag(name = "Catálogo")
@RestController
@RequestMapping("/api/v1/profesionales")
public class ProfesionalController {

    private final EmpleadoService empleadoService;

    public ProfesionalController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @Operation(summary = "Profesionales activos y sus servicios (CU-006)",
            description = "Con servicioId, solo quienes realizan ese servicio. Nombre e inicial del apellido.")
    @GetMapping
    public List<ProfesionalResponse> listar(@RequestParam(required = false) Long servicioId) {
        return empleadoService.profesionales(servicioId);
    }
}
