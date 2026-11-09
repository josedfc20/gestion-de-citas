# API REST

- **`openapi.json`**: la especificación OpenAPI 3 de la API (`/api/v1`), tal
  como la genera el backend. Se puede importar en
  [Swagger Editor](https://editor.swagger.io/) o en Postman sin arrancar nada.
- Con el backend arrancado, la misma API se prueba en Swagger UI:
  http://localhost:8080/swagger-ui.html.
- `OpenApiActualizadoTest` falla si este archivo no coincide con la API real.
  Si cambias un endpoint o un DTO a propósito, regenéralo desde `backend/`:

  ```bash
  ./mvnw test -Dtest=OpenApiActualizadoTest -Dopenapi.actualizar=true
  ```

Por qué se hace así: [ADR-017](../decisiones/ADR-017-openapi-versionado.md).
