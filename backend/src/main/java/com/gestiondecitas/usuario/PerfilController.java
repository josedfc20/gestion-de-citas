package com.gestiondecitas.usuario;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.shared.security.CookieRefreshToken;
import com.gestiondecitas.usuario.dto.ActualizarPerfilRequest;
import com.gestiondecitas.usuario.dto.DatosPersonalesResponse;
import com.gestiondecitas.usuario.dto.EliminarCuentaRequest;
import com.gestiondecitas.usuario.dto.PerfilResponse;

/** "Lo mío" (paso 3.3): el usuario sale del token, nunca de la URL. */
@Tag(name = "Mi perfil", description = "Datos del usuario con sesión (CU-004) y RGPD (RNF-007)")
@RestController
@RequestMapping("/api/v1/me")
public class PerfilController {

    private final PerfilService perfilService;
    private final DatosPersonalesService datosPersonalesService;
    private final CookieRefreshToken cookie;

    public PerfilController(PerfilService perfilService, DatosPersonalesService datosPersonalesService,
            CookieRefreshToken cookie) {
        this.perfilService = perfilService;
        this.datosPersonalesService = datosPersonalesService;
        this.cookie = cookie;
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

    @Operation(summary = "Exportar mis datos personales (RGPD)",
            description = "Descarga en JSON los datos personales que guarda el sistema.")
    @GetMapping("/datos")
    public ResponseEntity<DatosPersonalesResponse> exportarDatos(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("mis-datos.json").build().toString())
                .body(datosPersonalesService.exportar(Long.valueOf(jwt.getSubject())));
    }

    @Operation(summary = "Eliminar mi cuenta (CU-004, RGPD)",
            description = "Solo clientes. Pide la contraseña y anonimiza los datos; las citas quedan como historial.")
    @DeleteMapping
    public ResponseEntity<Void> eliminarCuenta(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EliminarCuentaRequest datos) {
        datosPersonalesService.eliminarCuenta(Long.valueOf(jwt.getSubject()), datos.password());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar().toString()).build();
    }
}
