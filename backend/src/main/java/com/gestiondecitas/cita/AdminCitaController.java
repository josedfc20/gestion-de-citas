package com.gestiondecitas.cita;

import java.net.URI;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.cita.dto.AdminReservaRequest;
import com.gestiondecitas.cita.dto.CitaResponse;
import com.gestiondecitas.shared.dto.Pagina;
import com.gestiondecitas.usuario.Rol;

@Tag(name = "Admin · Reservas", description = "Todas las reservas del negocio (CU-019)")
@RestController
@RequestMapping("/api/v1/admin/citas")
public class AdminCitaController {

    private final CitaService citaService;
    private final ReservaService reservaService;

    public AdminCitaController(CitaService citaService, ReservaService reservaService) {
        this.citaService = citaService;
        this.reservaService = reservaService;
    }

    @Operation(summary = "Reservas con filtros",
            description = "En orden de fecha. Sin desde, de hoy en adelante. Para cancelar o cambiar el estado, "
                    + "PATCH /citas/{id}/estado.")
    @GetMapping
    public Pagina<CitaResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return citaService.listar(Rol.ADMIN, profesionalId, clienteId, estado, desde, hasta, page, size);
    }

    @Operation(summary = "Reservar en nombre de un cliente (CU-019, alt. 1)",
            description = "Mismas reglas y misma comprobación del hueco que POST /citas.")
    @PostMapping
    public ResponseEntity<CitaResponse> reservar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReservaRequest datos) {
        CitaResponse cita = reservaService.reservarComoAdmin(Long.valueOf(jwt.getSubject()), datos.clienteId(),
                datos.reserva());
        return ResponseEntity.created(URI.create("/api/v1/citas/" + cita.id())).body(cita);
    }
}
