# ADR-010 · Eliminar una cuenta es anonimizarla, conservando las citas

- **Estado:** aceptada el 25/10/2026; detallada en el B3 (28/10/2026)
- **Origen:** paso 2.6 y B3 ([cuenta y RGPD](../propuestas/4.3-B3-cuenta-rgpd.md))

## Contexto

El RGPD (RNF-007) da al cliente derecho a acceder a sus datos y a que se
borren. Pero si se borra la fila del usuario, sus citas pasadas desaparecen
o quedan huérfanas, y el negocio pierde su historial y sus estadísticas.

## Decisión

- `GET /me/datos` **exporta** los datos personales en JSON, sin hashes.
- `DELETE /me` (solo clientes, con la contraseña en el cuerpo)
  **anonimiza**: cambia nombre, correo (`eliminado-{id}@anonimo.invalid`),
  teléfono y fecha de nacimiento, desactiva la cuenta, marca `eliminado_en`,
  borra sus tokens y cancela sus citas futuras. Las citas pasadas se
  conservan sin datos personales. Una cuenta eliminada nunca se reactiva.
- Con los beneficiarios, lo mismo: si tienen citas, se anonimizan en vez de
  borrarse.
- Los servicios y empleados **nunca se borran**: se desactivan.

## Alternativas descartadas

- **Borrado físico en cascada:** se pierde el historial del negocio.
- **Marcar como borrado sin anonimizar** (*soft delete*): los datos
  personales seguirían ahí, que es justo lo que el RGPD pide evitar.

## Consecuencias

- Las consultas y las estadísticas siguen funcionando con citas antiguas.
- Las pruebas de integración (`DatosPersonalesTest`, `BeneficiariosTest`)
  comprueban la anonimización contra la base de datos real.
