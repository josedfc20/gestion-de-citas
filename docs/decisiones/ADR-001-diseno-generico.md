# ADR-001 · Diseño genérico en lugar de términos de peluquería

- **Estado:** aceptada el 25/10/2026
- **Origen:** fase 1 (registro de decisiones del 25/10) y `AGENTS.md` §1

## Contexto

El primer cliente es una peluquería, pero el problema (reservas por teléfono,
solapamientos, citas olvidadas) es el mismo en un taller, una consulta o un
centro de estética. Si el código habla de "peluqueros" y "cortes", adaptarlo
a otro negocio obliga a renombrar tablas, endpoints y pantallas.

## Decisión

El diseño y el código usan solo términos genéricos: **negocio, profesional,
servicio, cita, cliente**. El producto se llama GestionDeCitas; la peluquería
es el primer caso de aplicación. Lo que cambia de un negocio a otro
(intervalo de reserva, antelación, horas para cancelar, edad mínima, zona
horaria) vive en la tabla `negocio` y lo configura el administrador.

## Alternativas descartadas

- **Hacerlo a medida de la peluquería** y generalizar más tarde: más rápido
  al principio, pero el renombrado posterior toca todas las capas.
- **Un sistema multinegocio desde el principio**: fuera del alcance de la
  primera versión (un único establecimiento).

## Consecuencias

- Adaptarlo a otro sector es cambiar los datos (servicios, horarios) y los
  textos de la web, no el modelo.
- Algunos sectores quedan fuera aunque el modelo valga: las citas médicas
  implican datos de salud (categoría especial del RGPD) y necesitarían medidas
  adicionales.
