package com.gestiondecitas.usuario;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.usuario.dto.ActualizarPerfilRequest;
import com.gestiondecitas.usuario.dto.PerfilResponse;

/** "Lo mío" (paso 3.3): el usuario sale del token, nunca de la URL. */
@Tag(name = "Mi perfil", description = "Datos del usuario con sesión (CU-004)")
@RestController
@RequestMapping("/api/v1/me")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @Operation(summary = "Ver mis datos")
    @GetMapping
    public PerfilResponse obtener(@AuthenticationPrincipal Jwt jwt) {
        return perfilService.obtener(Long.valueOf(jwt.getSubject()));
    }

    @Operation(summary = "Modificar mis datos (CU-004)")
    @PutMapping
    public PerfilResponse actualizar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarPerfilRequest datos) {
        return perfilService.actualizar(Long.valueOf(jwt.getSubject()), datos);
    }
}
