# GestionDeCitas

Aplicación web para gestionar y reservar citas en negocios con varios
profesionales y servicios. El primer caso de uso es una peluquería, pero el
diseño es genérico (negocio, profesional, servicio, cita) para adaptarlo a
talleres, consultas, etc.

> **Estado:** fase 4 (Desarrollo). Análisis, diseño y arquitectura completados.

## Funcionalidades principales

- Consulta pública de servicios, precios, profesionales y disponibilidad.
- Registro de clientes (14 años o más) y reserva de citas, también para
  beneficiarios menores de 14 años.
- Cancelación de citas hasta 24 h antes.
- Agenda para empleados y gestión del negocio para el administrador.
- Cumplimiento del RGPD: consentimiento, exportación y anonimización de datos.

## Tecnologías

| Área | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Security (JWT), Spring Data JPA |
| Base de datos | PostgreSQL 16, Flyway |
| Frontend | React, TypeScript, Vite, TanStack Query, React Hook Form + Zod |
| Pruebas | JUnit 5, Mockito, Testcontainers, Vitest, Testing Library |
| Infraestructura | Docker Compose, GitHub Actions |

## Estructura

```
gestion-de-citas/
├── AGENTS.md     reglas y especificaciones del proyecto
├── backend/      API REST con Spring Boot (fase 4)
├── frontend/     web con React + TypeScript (fase 4)
└── docs/         documentación, diagramas UML y propuestas aprobadas
```

## Documentación

- [`docs/Doc.docx`](docs/Doc.docx): documento completo del proyecto.
- [`docs/diagramas/`](docs/diagramas/): casos de uso, clases, secuencia,
  actividad, estados, entidad-relación y arquitectura.
- [`docs/propuestas/`](docs/propuestas/): propuestas aprobadas de cada paso.
- [`docs/guias/instalacion-entorno.md`](docs/guias/instalacion-entorno.md):
  cómo preparar el entorno de desarrollo.
- [`docs/guias/arrancar-backend.md`](docs/guias/arrancar-backend.md):
  cómo arrancar y probar el backend en tu PC.

## Arrancar en local

Requisitos: JDK 21, Docker Desktop y Git.

```bash
cp .env.example .env                    # y cambia contraseñas, JWT_SECRET y ADMIN_*
docker compose up -d                    # PostgreSQL 16 en el puerto 5433
cd backend && ./mvnw spring-boot:run    # API en http://localhost:8080
```

- Swagger: http://localhost:8080/swagger-ui.html (registro, login y botón *Authorize* con el token)
- Estado: http://localhost:8080/actuator/health
- Pruebas: `./mvnw verify` (necesita Docker para Testcontainers)

Guía detallada para Windows: [`docs/guias/arrancar-backend.md`](docs/guias/arrancar-backend.md).
El frontend se añadirá en el bloque B7.

## Autor

Jose Flores
