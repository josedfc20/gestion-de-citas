# Decisiones técnicas (ADR)

Un **ADR** (*Architecture Decision Record*, registro de decisión de
arquitectura) es una ficha corta por cada decisión técnica importante. Cada
una cuenta cuatro cosas: el **contexto** (qué problema había), la
**decisión**, las **alternativas descartadas** y sus **consecuencias**.

Estas fichas se escribieron en la fase 6 a partir de las propuestas aprobadas
(`docs/propuestas/`) y del registro de decisiones de `AGENTS.md`. Las
propuestas cuentan **cómo se fue decidiendo**; los ADR cuentan **por qué la
aplicación es como es hoy**. Cuando una propuesta no recogía las
alternativas, se han añadido las razonables al escribir la ficha.

## Índice

| N.º | Decisión | Fase |
|---|---|---|
| [ADR-001](ADR-001-diseno-generico.md) | Diseño genérico (negocio, profesional, servicio) en lugar de términos de peluquería | 1 |
| [ADR-002](ADR-002-monolito-modular.md) | Monolito modular por funcionalidad, en tres capas | 3 |
| [ADR-003](ADR-003-java-spring-boot-4.md) | Java 21 y Spring Boot 4.1 | 3 |
| [ADR-004](ADR-004-no-solape-en-la-base-de-datos.md) | Las citas solapadas las impide la base de datos | 2 |
| [ADR-005](ADR-005-herencia-y-beneficiarios.md) | Herencia por tablas y menores como beneficiarios sin cuenta | 2 |
| [ADR-006](ADR-006-cita-como-instantanea.md) | La cita guarda su fin y su precio (instantánea) | 2 |
| [ADR-007](ADR-007-jwt-y-refresh-rotado.md) | JWT corto y refresh token rotado en una cookie HttpOnly | 3 |
| [ADR-008](ADR-008-problem-details-y-404.md) | Errores en Problem Details y 404 para lo ajeno | 3 |
| [ADR-009](ADR-009-reloj-inyectable.md) | Un reloj inyectable (`Clock`) para todo lo que depende de la fecha | 3 |
| [ADR-010](ADR-010-anonimizar-en-vez-de-borrar.md) | Eliminar una cuenta es anonimizarla, conservando las citas | 2 y 4 |
| [ADR-011](ADR-011-flyway-y-datos-de-demo.md) | Migraciones Flyway inmutables y datos de demo en un perfil aparte | 4 |
| [ADR-012](ADR-012-stack-de-la-web.md) | Web con React, Vite y TanStack Query, sin librerías de componentes | 3 y 4 |
| [ADR-013](ADR-013-horas-en-la-zona-del-negocio.md) | Las horas siempre en la zona del negocio | 4 |
| [ADR-014](ADR-014-pruebas-con-postgresql-real.md) | Pruebas de integración con PostgreSQL real; Mockito solo para el correo | 4 y 5 |
| [ADR-015](ADR-015-matrices-de-permisos-y-archunit.md) | Matrices de permisos y ArchUnit que fallan por olvido | 5 |
| [ADR-016](ADR-016-playwright-para-e2e.md) | Playwright para las pruebas de punta a punta | 5 |
| [ADR-017](ADR-017-openapi-versionado.md) | La especificación OpenAPI se versiona y una prueba la vigila | 6 |
| [ADR-018](ADR-018-docker-compose-y-caddy.md) | Producción con Docker Compose y Caddy delante | 7 |

## Cómo se añade uno

- Una decisión técnica nueva que cambie cómo se construye la aplicación lleva
  su ADR en el mismo PR: `ADR-NNN-titulo-corto.md`, con el número siguiente y
  las mismas cuatro partes. Se añade a este índice.
- Un ADR **nunca se borra ni se reescribe**. Si una decisión cambia, se
  escribe un ADR nuevo y el antiguo pasa a *Estado: sustituida por ADR-NNN*.
- Las decisiones de proceso (fechas, PR fusionados, cómo se trabaja) siguen
  en el registro de decisiones de `AGENTS.md` y `docs/Doc.docx`.
