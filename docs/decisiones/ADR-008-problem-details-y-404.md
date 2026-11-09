# ADR-008 · Errores en Problem Details y 404 para lo ajeno

- **Estado:** aceptada el 26/10/2026
- **Origen:** pasos 3.2 ([arquitectura](../propuestas/3.2-arquitectura-backend.md))
  y 3.3 ([API](../propuestas/3.3-api-rest.md))

## Contexto

La web (y después Android) tiene que saber qué ha fallado para mostrar un
mensaje útil y poner cada error debajo de su campo. Además, una API no debe
revelar si existe una cita de otro cliente.

## Decisión

- Todos los errores salen en formato **Problem Details (RFC 9457)**, desde un
  único `@RestControllerAdvice`, con un significado fijo por código: 400
  (formato), 401, 403, 404, 409 (hueco ocupado o dato duplicado), 422 (regla
  de negocio) y 429 (demasiados intentos). Los errores de validación traen
  la lista de campos.
- Un recurso que existe pero **no es tuyo responde 404**, igual que uno que
  no existe.

## Alternativas descartadas

- **Un formato de error propio:** habría que documentarlo y mantenerlo;
  Spring ya trae Problem Details.
- **403 para lo ajeno:** confirma a quien prueba identificadores que el
  recurso existe.

## Consecuencias

- La web convierte cualquier error en `ApiError` y los formularios colocan
  cada mensaje en su campo.
- La matriz de permisos (ADR-015) comprueba los 401/403/404 de todos los
  endpoints.
