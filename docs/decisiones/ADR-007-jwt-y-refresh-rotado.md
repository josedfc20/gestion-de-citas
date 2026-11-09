# ADR-007 · JWT corto y refresh token rotado en una cookie HttpOnly

- **Estado:** aceptada el 26/10/2026; ajustada en el B2 (28/10/2026)
- **Origen:** paso 3.4 ([seguridad](../propuestas/3.4-seguridad.md)) y B2
  ([autenticación](../propuestas/4.2-B2-autenticacion.md))

## Contexto

La misma API la usarán la web y, más adelante, la app Android. Hay que
iniciar sesión sin guardar sesiones en el servidor para cada petición y sin
dejar un token que dure días al alcance de un script malicioso (XSS).

## Decisión

- **Access token JWT** (HS256, `JWT_SECRET`) de **15 minutos**, en la cabecera
  `Authorization: Bearer`. En la web vive **solo en memoria**, nunca en
  `localStorage`.
- **Refresh token** aleatorio de **7 días** en una cookie `HttpOnly`,
  `SameSite=Strict`, limitada a la ruta `/api/v1/auth`. Se **rota** en cada
  uso. Si alguien reutiliza uno ya usado, se cierran todas las sesiones de
  ese usuario.
- En la base de datos solo se guarda el **SHA-256** del refresh token.
- Contraseñas con **BCrypt de coste 12**, de 10 a 64 caracteres y no comunes.
  Login limitado a 5 intentos por minuto por IP y por correo (429).

## Alternativas descartadas

- **Sesión de servidor con cookie** (`JSESSIONID`): sencilla en la web, pero
  incómoda para Android y con estado en el servidor.
- **Un solo JWT de larga duración en `localStorage`:** si un script lo roba,
  sirve días, y no se puede cerrar la sesión de verdad.
- **BCrypt también para el refresh token** (lo que decía el 3.4): BCrypt usa
  sal, así que no se puede buscar el token por su hash. Como el token ya es
  aleatorio y largo, SHA-256 es igual de seguro aquí.

## Consecuencias

- Al abrir la web, la sesión se recupera con `POST /auth/refresh`. Ante un
  401, la web renueva una vez y repite la petición.
- Como reutilizar un refresh cierra todo, la web garantiza **un solo refresh
  a la vez**, también entre pestañas (`navigator.locks`).
- `seguridad.spec.ts` (E2E) vigila la cookie y que el token no acabe en el
  almacenamiento del navegador.
