# ADR-014 · Pruebas de integración con PostgreSQL real; Mockito solo para el correo

- **Estado:** aceptada el 28/10/2026 (B1); Mockito acotado en el 5.0
  (31/10/2026)
- **Origen:** B1 ([esqueleto](../propuestas/4.1-B1-esqueleto-backend.md)) y 5.0
  ([plan de pruebas](../propuestas/5.0-plan-de-pruebas.md), decisión 4)

## Contexto

Parte de las reglas más importantes viven en PostgreSQL: la restricción de
solapes (ADR-004), `TIMESTAMPTZ`, el correo único con `lower(email)`. Una
base de datos en memoria distinta no las tiene, y los mocks de los
repositories prueban que el código llama a lo que se espera, no que
funcione.

## Decisión

- Las pruebas de integración (`@IntegracionTest`) arrancan **PostgreSQL 16
  real con Testcontainers**. Nunca usan la base de datos de Docker Compose del
  PC.
- La lógica pura (`CalculoDisponibilidad`, `HorarioSemanal`, `JwtService`) va
  en pruebas unitarias sin Spring.
- **Mockito solo donde una prueba de integración no llega**: hoy, el envío
  de correo (`EnvioCorreoSmtp`).

## Alternativas descartadas

- **H2 en memoria:** más rápida, pero sin restricciones de exclusión ni el
  mismo SQL; las pruebas pasarían y producción fallaría.
- **Mocks de los repositories en todas las pruebas de servicios:** pruebas
  frágiles que no detectan errores de consultas ni de transacciones.

## Consecuencias

- Las pruebas del backend necesitan Docker, en el PC y en la CI.
- Son más lentas que las unitarias, pero cubren el 98 % de las líneas con
  la base de datos real.
