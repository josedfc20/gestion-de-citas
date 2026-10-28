package com.gestiondecitas.auth;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.auth.dto.CambioPasswordRequest;
import com.gestiondecitas.auth.dto.LoginRequest;
import com.gestiondecitas.auth.dto.RegistroRequest;
import com.gestiondecitas.auth.dto.SesionResponse;
import com.gestiondecitas.auth.dto.UsuarioResumen;
import com.gestiondecitas.shared.error.NoAutenticadoException;
import com.gestiondecitas.shared.security.CookieRefreshToken;

@Tag(name = "Autenticación", description = "Registro, inicio y cierre de sesión (CU-001 a CU-004)")
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;
    private final CookieRefreshToken cookie;

    public AuthController(AuthService authService, CookieRefreshToken cookie) {
        this.authService = authService;
        this.cookie = cookie;
    }

    @Operation(summary = "Crear una cuenta de cliente (CU-001)")
    @PostMapping("/auth/registro")
    public ResponseEntity<UsuarioResumen> registrar(@Valid @RequestBody RegistroRequest datos) {
        return ResponseEntity.created(URI.create("/api/v1/me")).body(authService.registrar(datos));
    }

    @Operation(summary = "Iniciar sesión (CU-002)",
            description = "Devuelve el access token y pone el refresh token en una cookie HttpOnly.")
    @PostMapping("/auth/login")
    public ResponseEntity<SesionResponse> login(@Valid @RequestBody LoginRequest credenciales,
            HttpServletRequest peticion) {
        return conCookie(authService.login(credenciales, peticion.getRemoteAddr()));
    }

    @Operation(summary = "Renovar el access token con la cookie del refresh token")
    @PostMapping("/auth/refresh")
    public ResponseEntity<?> renovar(
            @CookieValue(name = CookieRefreshToken.NOMBRE, required = false) String refreshToken) {
        try {
            return conCookie(authService.renovar(refreshToken));
        } catch (NoAutenticadoException ex) {
            // Sesión no válida: además del 401, se borra la cookie del navegador.
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
            problema.setTitle("No autenticado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cookie.borrar().toString())
                    .body(problema);
        }
    }

    @Operation(summary = "Cerrar sesión (CU-003)")
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = CookieRefreshToken.NOMBRE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar().toString()).build();
    }

    @Operation(summary = "Cambiar mi contraseña (CU-004, RN-005)",
            description = "Cierra las demás sesiones y devuelve tokens nuevos.")
    @PutMapping("/me/password")
    public ResponseEntity<SesionResponse> cambiarPassword(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CambioPasswordRequest datos) {
        return conCookie(authService.cambiarPassword(Long.valueOf(jwt.getSubject()), datos));
    }

    private ResponseEntity<SesionResponse> conCookie(SesionEmitida sesion) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.crear(sesion.refreshToken()).toString())
                .body(sesion.respuesta());
    }
}
