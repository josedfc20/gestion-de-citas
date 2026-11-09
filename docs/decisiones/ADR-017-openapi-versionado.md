# ADR-017 · La especificación OpenAPI se versiona y una prueba la vigila

- **Estado:** aceptada el 09/11/2026
- **Origen:** 6b ([técnica al día](../propuestas/6.2-6b-tecnica-al-dia.md),
  decisión 2)

## Contexto

La API se diseñó en el paso 3.3, pero durante el desarrollo se añadieron
endpoints (`GET /negocio`, `GET /admin/clientes`) y campos
(`realizadoPorNombre`) que la propuesta ya no recoge. Swagger está al día,
pero solo se ve con el backend arrancado. Cualquier documento de la API
escrito a mano se desfasa con el primer cambio.

## Decisión

- La especificación que genera springdoc se guarda en
  **`docs/api/openapi.json`**, con las claves ordenadas y sin la dirección del
  servidor, para que solo cambie cuando cambia la API.
- **`OpenApiActualizadoTest`** arranca el backend, pide `/v3/api-docs` y la
  compara con el archivo. **Si no coinciden, falla** y dice cómo
  regenerarlo:
  `./mvnw test -Dtest=OpenApiActualizadoTest -Dopenapi.actualizar=true`.
- Las descripciones (`@Tag`, `@Operation`) están en español, como el resto
  del proyecto.

## Alternativas descartadas

- **Poner al día `3.3-api-rest.md` a mano:** se volvería a quedar atrás.
- **Que la CI regenere el archivo y lo guarde como artefacto, sin fallar:**
  más cómodo, pero el archivo del repositorio podría quedarse viejo sin que
  nadie lo notara.
- **Generar el cliente de la web desde la especificación:** útil con varios
  clientes; hoy la web tiene su propio cliente pequeño y la app Android
  todavía no existe.

## Consecuencias

- Un PR que cambia la API trae también el cambio de `openapi.json`, y en la
  revisión se ve qué cambió del contrato.
- La especificación se puede importar en Postman o Swagger Editor sin
  arrancar nada.
