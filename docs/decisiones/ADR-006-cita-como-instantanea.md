# ADR-006 · La cita guarda su fin y su precio (instantánea)

- **Estado:** aceptada el 25/10/2026
- **Origen:** paso 2.4 ([clases](../propuestas/2.4-explicacion-diagrama-clases.md))
  y paso 3.3 (el servidor calcula fin y precio)

## Contexto

El administrador puede cambiar la duración y el precio de un servicio en
cualquier momento. Si la cita solo apuntara al servicio, una subida de precio
cambiaría lo que ve el cliente en una cita ya reservada, y el historial
mostraría precios que nunca se cobraron.

## Decisión

Al reservar, **el servidor copia en la cita el precio del servicio y calcula
su fin** (inicio + duración). El cliente nunca los envía. Es la técnica
habitual llamada *snapshot* o instantánea.

## Alternativas descartadas

- **Leer siempre el precio y la duración del servicio:** los históricos y las
  estadísticas quedarían falseados.
- **Versionar los servicios** (una fila por cada cambio de precio): resuelve
  lo mismo con mucha más complejidad.

## Consecuencias

- Cambiar un servicio solo afecta a las reservas nuevas.
- La restricción de solapes (ADR-004) trabaja con el `fin` guardado, sin
  consultar el servicio.
