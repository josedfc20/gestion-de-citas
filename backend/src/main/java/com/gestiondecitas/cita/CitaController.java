package com.gestiondecitas.cita;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.cita.dto.CancelacionRequest;
import com.gestiondecitas.cita.dto.CitaResponse;
import com.gestiondecitas.cita.dto.ReservaRequest;
import com.gestiondecitas.shared.dto.Pagina;
import com.gestiondecitas.shared.security.JwtConfig;
import com.gestiondecitas.usuario.Rol;

@Tag(name = "Citas", description = "Reservar, consultar y cancelar citas (CU-008 a CU-012)")
@RestController
@RequestMapping("/api/v1")
public class CitaController {

    private final ReservaService reservaService;
    private final CitaService citaService;

    public CitaController(ReservaService reservaService, CitaService citaService) {
        this.reservaService = reservaService;
        this.citaService = citaService;
    }

    @Operation(summary = "Reservar una cita (CU-008, CU-009)",
            description = "La hora debe ser una de GET /disponibilidad. 409 si ya no está libre. "
                    + "Con beneficiarioId, la cita es para un menor a cargo.")
    @PostMapping("/citas")
    public ResponseEntity<CitaResponse> reservar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReservaRequest datos) {
        Long clienteId = Long.valueOf(jwt.getSubject());
        CitaResponse cita = reservaService.reservar(clienteId, datos, clienteId);
        return ResponseEntity.created(URI.create("/api/v1/citas/" + cita.id())).body(cita);
    }

    @Operation(summary = "Mis citas (CU-010)",
            description = "Próximas en orden, en todos los estados. Con pasadas=true, el historial.")
    @GetMapping("/me/citas")
    public Pagina<CitaResponse> misCitas(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean pasadas,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return citaService.misCitas(Long.valueOf(jwt.getSubject()), pasadas, page, size);
    }

    @Operation(summary = "Detalle de una cita (CU-011, CU-014)",
            description = "El cliente ve las suyas, el empleado las asignadas y el administrador todas.")
    @GetMapping("/citas/{id}")
    public CitaResponse detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return citaService.detalle(Long.valueOf(jwt.getSubject()), rol(jwt), id);
    }

    @Operation(summary = "Cancelar mi cita (CU-012)",
            description = "Solo si faltan las horas mínimas del negocio (24 h por defecto, RN-004).")
    @PostMapping("/citas/{id}/cancelacion")
    public CitaResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelacionRequest datos) {
        return citaService.cancelarPorCliente(Long.valueOf(jwt.getSubject()), id,
                datos == null ? null : datos.motivo());
    }

    static Rol rol(Jwt jwt) {
        return Rol.valueOf(jwt.getClaimAsString(JwtConfig.CLAIM_ROL));
    }
}
