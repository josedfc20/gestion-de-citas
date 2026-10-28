package com.gestiondecitas.cita;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.cita.dto.AgendaDiaResponse;
import com.gestiondecitas.cita.dto.CitaResponse;
import com.gestiondecitas.shared.dto.Pagina;
import com.gestiondecitas.usuario.Rol;

/** Agenda y citas del empleado con sesión (CU-013, CU-014): el empleado sale del token. */
@Tag(name = "Empleado · Agenda", description = "Mi jornada y mis citas asignadas (CU-013, CU-014)")
@RestController
@RequestMapping("/api/v1/empleado")
public class EmpleadoCitaController {

    private final AgendaService agendaService;
    private final CitaService citaService;

    public EmpleadoCitaController(AgendaService agendaService, CitaService citaService) {
        this.agendaService = agendaService;
        this.citaService = citaService;
    }

    @Operation(summary = "Mi agenda (CU-013)",
            description = "Por día, mi horario efectivo y mis citas no canceladas. Sin fechas, hoy; "
                    + "como mucho 31 días.")
    @GetMapping("/agenda")
    public List<AgendaDiaResponse> agenda(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return agendaService.agenda(Long.valueOf(jwt.getSubject()), desde, hasta);
    }

    @Operation(summary = "Mis citas asignadas (CU-014)",
            description = "En orden de fecha, con filtros. Sin desde, de hoy en adelante.")
    @GetMapping("/citas")
    public Pagina<CitaResponse> citas(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return citaService.listar(Rol.EMPLEADO, Long.valueOf(jwt.getSubject()), null, estado, desde, hasta, page,
                size);
    }
}
