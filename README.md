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
| Backend | Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA |
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

## Arrancar en local

Se completará en la fase 4, cuando existan el backend y el frontend.

## Autor

Jose Flores
