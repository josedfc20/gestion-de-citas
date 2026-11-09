# ADR-013 · Las horas siempre en la zona del negocio

- **Estado:** aceptada el 31/10/2026
- **Origen:** paso 2.6 (`TIMESTAMPTZ`), B7 y B8
  ([zona del cliente](../propuestas/4.8-B8-web-cliente.md), decisión 1)

## Contexto

Una cita a las 10:00 es a las 10:00 en la peluquería, aunque el cliente abra
la web desde otro país o con el móvil en otra zona. Además, el cambio de hora
de verano hace que el desfase (+01:00 o +02:00) dependa del día.

## Decisión

- La base de datos guarda instantes (`TIMESTAMPTZ`) y el negocio tiene su
  zona (`negocio.zona_horaria`, pública en `GET /negocio`).
- El backend da las horas libres como "10:00" en la zona del negocio y envía
  las fechas de las citas como texto en esa zona. **La web las muestra tal
  cual, nunca en la zona del navegador.**
- Al reservar, la web calcula el instante con el desfase **del día de la
  cita** usando `Intl` (`inicioConDesfase`). El backend comprueba que ese
  instante es exactamente una hora libre: si la web se equivocara, el
  resultado sería un 409, nunca una reserva mal hecha.

## Alternativas descartadas

- **Mostrar las horas en la zona del navegador:** un cliente de viaje vería
  horas que no son las del negocio.
- **Que `GET /disponibilidad` devuelva también los instantes completos:**
  igual de válido, pero obligaba a tocar el backend ya terminado.

## Consecuencias

- `inicioConDesfase` tiene pruebas en invierno, en verano y en los días del
  cambio de hora.
