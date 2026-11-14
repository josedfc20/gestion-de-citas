# Documentación de GestionDeCitas

Todo lo que no es código. Si es la primera vez, sigue este orden:

1. **[README del proyecto](../README.md)**: qué es, capturas y cómo
   arrancarlo con la demo.
2. **[Decisiones técnicas (ADR)](decisiones/README.md)**: por qué la
   aplicación es como es, en fichas de una pantalla.
3. **[Diagramas](#diagramas)**: el modelo, la base de datos y cómo encajan
   las piezas.
4. **[Manual de usuario](guias/manual-de-usuario.md)**: qué hace cada
   perfil, pantalla a pantalla.
5. **[Informe de pruebas](pruebas/informe-de-pruebas.md)**: qué se probó y
   con qué resultado.

## Carpetas

| Carpeta | Qué hay |
|---|---|
| [`decisiones/`](decisiones/README.md) | ADR-001 a ADR-018: contexto, decisión, alternativas descartadas y consecuencias |
| [`diagramas/`](diagramas/) | UML y base de datos (los `.mmd` y `.drawio` son los originales; los `.png`, las imágenes) |
| [`api/`](api/README.md) | `openapi.json`, la especificación de la API, vigilada por una prueba |
| [`guias/`](guias/) | Cómo instalar el entorno, arrancar el backend y la web, pasar las E2E, usar la aplicación y desplegarla |
| [`pruebas/`](pruebas/) | Plan, matriz de trazabilidad, pruebas manuales, registro de defectos e informe |
| [`propuestas/`](propuestas/) | La propuesta aprobada de cada paso, de la fase 2 a la 6: la historia del proyecto |
| [`capturas/`](capturas/) | Capturas de la web con la demo, para el README y el manual |
| [`Doc.docx`](Doc.docx) | El documento completo del proyecto: requisitos, casos de uso, diseño, arquitectura, pruebas y registro de decisiones |

## Diagramas

| Diagrama | Archivo | Qué muestra |
|---|---|---|
| Casos de uso | [`CasosDeUso.drawio`](diagramas/CasosDeUso.drawio) y `CU-diagrama-1..5.png` | Qué hace cada perfil (CU-001 a CU-021) |
| Clases | [`Clases.png`](diagramas/Clases.png) | El modelo de dominio |
| Entidad-relación | [`ER-BaseDeDatos.png`](diagramas/ER-BaseDeDatos.png) | Las tablas de PostgreSQL (migraciones V1 a V7) |
| Contenedores | [`Contenedores.png`](diagramas/Contenedores.png) | Web, backend, base de datos, correo y CI |
| Arquitectura del backend | [`Arquitectura-Backend.png`](diagramas/Arquitectura-Backend.png) | Las capas de cada módulo |
| Secuencias | [`Secuencia-ReservarCita.png`](diagramas/Secuencia-ReservarCita.png), [`Secuencia-CancelarCita.png`](diagramas/Secuencia-CancelarCita.png) | Reservar y cancelar paso a paso |
| Actividad | [`Actividad-Disponibilidad.png`](diagramas/Actividad-Disponibilidad.png) | Cómo se calculan las horas libres |
| Estados | [`Estados-Cita.png`](diagramas/Estados-Cita.png) | Los estados de una cita |

## Guías

| Guía | Para qué |
|---|---|
| [`instalacion-entorno.md`](guias/instalacion-entorno.md) | Instalar JDK, Node.js, Docker y Git en Windows |
| [`arrancar-backend.md`](guias/arrancar-backend.md) | Arrancar y probar el backend, con Swagger y Mailpit |
| [`arrancar-web.md`](guias/arrancar-web.md) | Arrancar la web con los datos de demostración |
| [`pruebas-e2e.md`](guias/pruebas-e2e.md) | Pasar las pruebas de punta a punta en el PC |
| [`manual-de-usuario.md`](guias/manual-de-usuario.md) | Usar la aplicación con cada perfil |
| [`desplegar.md`](guias/desplegar.md) | Arrancar la versión de producción (en el PC y, desde el 7e, en un servidor) |

## Las propuestas y lo construido

Las propuestas cuentan lo que se aprobó en cada momento y no se reescriben.
Las que el desarrollo dejó atrás (2.4, 2.6, 3.2, 3.3 y 3.4) llevan un aviso
al principio que lleva a lo construido: los ADR, los diagramas al día y
`openapi.json`.
