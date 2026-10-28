package com.gestiondecitas.shared.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Convierte las excepciones en respuestas Problem Details (RFC 9457), paso 3.2, 4.3.
 * Los errores propios de Spring MVC (JSON mal formado, método no permitido...) los
 * resuelve la clase base con el mismo formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Error de un campo del DTO, que la web muestra junto a ese campo. */
    public record ErrorCampo(String campo, String mensaje) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, "Hay datos con formato incorrecto.");
        problema.setTitle("Datos no válidos");
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorCampo(error.getField(), error.getDefaultMessage()))
                .toList();
        problema.setProperty("errores", errores);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    @ExceptionHandler(PeticionNoValidaException.class)
    public ProblemDetail peticionNoValida(PeticionNoValidaException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Petición no válida", ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(HuecoNoDisponibleException.class)
    public ProblemDetail huecoNoDisponible(HuecoNoDisponibleException ex) {
        return problema(HttpStatus.CONFLICT, "Hueco no disponible", ex.getMessage());
    }

    @ExceptionHandler(DatoDuplicadoException.class)
    public ProblemDetail datoDuplicado(DatoDuplicadoException ex) {
        return problema(HttpStatus.CONFLICT, "Dato duplicado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail conflicto(ConflictoException ex) {
        ProblemDetail problema = problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
        ex.getPropiedades().forEach(problema::setProperty);
        return problema;
    }

    @ExceptionHandler(NoAutenticadoException.class)
    public ProblemDetail noAutenticado(NoAutenticadoException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage());
    }

    @ExceptionHandler(SinPermisoException.class)
    public ProblemDetail sinPermisoNegocio(SinPermisoException ex) {
        return problema(HttpStatus.FORBIDDEN, "Sin permiso", ex.getMessage());
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ProblemDetail> demasiadosIntentos(DemasiadosIntentosException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosDeEspera()))
                .body(problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos", ex.getMessage()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio incumplida", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail sinSesion(AuthenticationException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado", "Hay que iniciar sesión.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail sinPermiso(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Sin permiso", "No tienes permiso para esta operación.");
    }

    /** Cualquier error inesperado: se registra en el log y al cliente solo se le da un mensaje genérico. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ha ocurrido un error inesperado.");
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
