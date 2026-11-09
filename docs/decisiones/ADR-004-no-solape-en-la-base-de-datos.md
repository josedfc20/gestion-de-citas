# ADR-004 · Las citas solapadas las impide la base de datos

- **Estado:** aceptada el 25/10/2026
- **Origen:** paso 2.6 ([diseño](../propuestas/2.6-diseno-base-de-datos.md)) y
  B6 ([citas](../propuestas/4.6-B6-citas.md)); probada en el 5d con dos
  navegadores a la vez

## Contexto

RNF-003 pide que nunca se confirmen dos citas incompatibles del mismo
profesional. Comprobarlo solo en la aplicación ("¿está libre? → guardo") deja
una ventana: dos clientes que pulsan "Reservar" a la vez pasan los dos la
comprobación.

## Decisión

Dos barreras:

1. **La aplicación** vuelve a calcular la disponibilidad al reservar
   (RF-007). Si la hora ya no está en la lista, responde 409 "Ese horario ya
   no está disponible".
2. **PostgreSQL** tiene la restricción de exclusión `no_solape_profesional`
   (`EXCLUDE USING gist`, extensión `btree_gist`) sobre el rango
   `[inicio, fin)` de las citas `CONFIRMADA` o `COMPLETADA` de un mismo
   profesional. Si dos reservas llegan a la vez, solo se guarda una; la otra
   recibe el mismo 409.

## Alternativas descartadas

- **Solo la comprobación en la aplicación:** falla con reservas simultáneas.
- **Bloquear la fila del profesional** (`SELECT … FOR UPDATE`) en cada
  reserva: funciona, pero depende de que todo el código lo haga siempre y
  serializa reservas que no chocan.

## Consecuencias

- La regla no depende de que el código sea correcto: ni un error futuro ni
  un script directo contra la base de datos pueden crear un solape.
- Una cita cancelada libera el hueco sola, y 10:00–10:30 no choca con
  10:30–11:00.
- Ata el proyecto a PostgreSQL (las restricciones de exclusión no existen en
  MySQL).
