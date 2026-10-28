package com.gestiondecitas.shared.security;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * Respuestas 401 y 403 de Spring Security con el mismo formato Problem Details
 * que el resto de la API. Estos errores ocurren en los filtros, antes de llegar
 * a los controladores, por eso no pasan por {@code GlobalExceptionHandler}.
 */
@Component
public class ProblemDetailsSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public ProblemDetailsSecurityHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException ex) throws IOException {
        escribir(request, response, HttpStatus.UNAUTHORIZED, "No autenticado", "Hay que iniciar sesión.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        String detalle = TokenActual.debeCambiarPassword(SecurityContextHolder.getContext().getAuthentication())
                ? "Debes cambiar tu contraseña antes de continuar."
                : "No tienes permiso para esta operación.";
        escribir(request, response, HttpStatus.FORBIDDEN, "Sin permiso", detalle);
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus estado,
            String titulo, String detalle) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problema);
    }
}
