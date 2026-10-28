package com.gestiondecitas.beneficiario;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.beneficiario.dto.BeneficiarioRequest;
import com.gestiondecitas.beneficiario.dto.BeneficiarioResponse;

@Tag(name = "Mis beneficiarios", description = "Menores a mi cargo para reservarles cita (CU-009)")
@RestController
@RequestMapping("/api/v1/me/beneficiarios")
public class BeneficiarioController {

    private final BeneficiarioService beneficiarioService;

    public BeneficiarioController(BeneficiarioService beneficiarioService) {
        this.beneficiarioService = beneficiarioService;
    }

    @Operation(summary = "Mis menores a cargo")
    @GetMapping
    public List<BeneficiarioResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return beneficiarioService.listar(Long.valueOf(jwt.getSubject()));
    }

    @Operation(summary = "Un menor a mi cargo")
    @GetMapping("/{id}")
    public BeneficiarioResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return beneficiarioService.obtener(Long.valueOf(jwt.getSubject()), id);
    }

    @Operation(summary = "Añadir un menor a cargo",
            description = "422 si ya tiene la edad mínima para crear su propia cuenta.")
    @PostMapping
    public ResponseEntity<BeneficiarioResponse> crear(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BeneficiarioRequest datos) {
        BeneficiarioResponse creado = beneficiarioService.crear(Long.valueOf(jwt.getSubject()), datos);
        return ResponseEntity.created(URI.create("/api/v1/me/beneficiarios/" + creado.id())).body(creado);
    }

    @Operation(summary = "Corregir los datos de un menor a cargo")
    @PutMapping("/{id}")
    public BeneficiarioResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody BeneficiarioRequest datos) {
        return beneficiarioService.actualizar(Long.valueOf(jwt.getSubject()), id, datos);
    }

    @Operation(summary = "Eliminar un menor a cargo",
            description = "Si tiene citas se anonimiza y se cancelan las que aún no han empezado.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        beneficiarioService.eliminar(Long.valueOf(jwt.getSubject()), id);
        return ResponseEntity.noContent().build();
    }
}
