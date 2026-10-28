package com.gestiondecitas.horario;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.horario.dto.HorarioEmpleadoResponse;
import com.gestiondecitas.horario.dto.HorarioSemanaRequest;
import com.gestiondecitas.horario.dto.HorarioSemanaResponse;

@Tag(name = "Admin · Horarios", description = "Horario general y ajuste por empleado (CU-018, RN-002)")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminHorarioController {

    private final HorarioService horarioService;

    public AdminHorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @Operation(summary = "Horario general de lunes a domingo (un día sin tramos está cerrado)")
    @GetMapping("/horario-general")
    public HorarioSemanaResponse general() {
        return horarioService.general();
    }

    @Operation(summary = "Reemplazar el horario general",
            description = "Un día que no se envía queda cerrado. Se guarda aunque haya citas que queden fuera: "
                    + "llegan en citasAfectadas y se mantienen.")
    @PutMapping("/horario-general")
    public HorarioSemanaResponse cambiarGeneral(@Valid @RequestBody HorarioSemanaRequest datos) {
        return horarioService.cambiarGeneral(datos);
    }

    @Operation(summary = "Ajustes de un empleado y su semana efectiva")
    @GetMapping("/empleados/{id}/horario")
    public HorarioEmpleadoResponse deEmpleado(@PathVariable Long id) {
        return horarioService.deEmpleado(id);
    }

    @Operation(summary = "Reemplazar los ajustes de un empleado",
            description = "Solo los días ajustados; un día con tramos vacíos = no trabaja. Lista vacía = vuelve "
                    + "al horario general. Las citas que quedan fuera llegan en citasAfectadas.")
    @PutMapping("/empleados/{id}/horario")
    public HorarioEmpleadoResponse cambiarDeEmpleado(@PathVariable Long id,
            @Valid @RequestBody HorarioSemanaRequest datos) {
        return horarioService.cambiarDeEmpleado(id, datos);
    }
}
