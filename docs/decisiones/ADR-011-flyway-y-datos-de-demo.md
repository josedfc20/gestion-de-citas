# ADR-011 · Migraciones Flyway inmutables y datos de demo en un perfil aparte

- **Estado:** aceptada el 28/10/2026
- **Origen:** B1 ([esqueleto](../propuestas/4.1-B1-esqueleto-backend.md)) y B5
  ([horarios](../propuestas/4.5-B5-horarios-disponibilidad.md))

## Contexto

El esquema de la base de datos cambió en casi todos los bloques de
desarrollo. Hace falta que cualquier base de datos (la del PC, la de las
pruebas, la de producción) llegue al mismo esquema, y poder enseñar la
aplicación con datos sin que esos datos acaben en producción.

## Decisión

- El esquema lo crea **Flyway** (`db/migration`, V1 a V7). **Una migración
  aplicada nunca se modifica**: cada cambio va en una nueva.
- V2 solo crea el registro de `negocio`. El primer administrador se crea al
  arrancar desde `ADMIN_EMAIL` y `ADMIN_PASSWORD`.
- Los datos de demostración (servicios, empleados, horarios, clientes) los
  crea `DatosDemo` **solo con el perfil `demo`** y solo si no hay servicios,
  con correos `@demo.invalid` y la contraseña `DEMO_PASSWORD`.

## Alternativas descartadas

- **`ddl-auto=update` de Hibernate:** cambia el esquema sin control ni
  historial, y no sirve en producción.
- **Datos de demo en una migración:** acabarían en cualquier base de datos,
  también en la de producción.
- **Contraseñas del administrador o de la demo en el código:** secretos en
  Git.

## Consecuencias

- El historial del esquema está en el repositorio y se revisa en los PR.
- Las pruebas parten de una base de datos limpia con el esquema real.
