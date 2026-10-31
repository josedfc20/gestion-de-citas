# Plan de pruebas

> Fase 5 (Pruebas), paso 5a; actualizado en el 5b y el 5c. Plan aprobado el 31/10/2026 en
> `docs/propuestas/5.0-plan-de-pruebas.md`.

## 1. Objetivo

Comprobar que GestionDeCitas cumple sus requisitos (RNF-005: "pruebas que
verifiquen las funcionalidades principales") y dejarlo **demostrado y
repetible**: cualquiera puede ejecutar las pruebas con un comando y la CI las
ejecuta en cada push y en cada PR.

## 2. Niveles de prueba

| Nivel | Qué prueba | Herramientas | Dónde | Cómo se ejecuta |
|---|---|---|---|---|
| **Unitarias (backend)** | Reglas puras, sin Spring ni base de datos: cálculo de la disponibilidad, horario semanal, edad mínima, política de contraseñas, JWT, estados de la cita | JUnit 5, AssertJ | `backend/src/test` (clases sin `@IntegracionTest`) | `./mvnw test` |
| **Integración (backend)** | Cada endpoint con su seguridad, validación, reglas de negocio y la base de datos real (restricciones, migraciones, concurrencia) | JUnit 5, MockMvc, Testcontainers (PostgreSQL 16) | `backend/src/test` (clases con `@IntegracionTest`) | `./mvnw verify` (necesita Docker) |
| **Arquitectura (backend)** | Las reglas del paso 3.2 sobre el código compilado (capas, DTO, dependencias entre paquetes, `Clock`) | ArchUnit | `ArquitecturaTest` | `./mvnw test` |
| **Componentes (web)** | Cada pantalla con la API simulada: formularios, errores del servidor, rutas por rol, sesión y renovación del token | Vitest, Testing Library, MSW | `frontend/src/**/*.test.ts(x)` | `npm test` |
| **De punta a punta** | Los flujos principales con la web, el backend, PostgreSQL y Mailpit reales, en Chromium (5c); móvil, accesibilidad y reserva simultánea (5d) | Playwright, axe | `e2e/pruebas` | `npm test` en `e2e/` (guía `docs/guias/pruebas-e2e.md`) |
| **Manuales** | Lo que una máquina no juzga bien: que se entienda, que se vea bien, los correos | Lista de comprobación con el perfil `demo` | `docs/pruebas/pruebas-manuales.md` (paso 5e) | pendiente |

Las reglas se prueban con PostgreSQL real, que es más fiable que simular los
repositorios. Mockito solo se usa donde una prueba de integración no llega
(decisión 4 del plan): el envío de correo por SMTP y su fallo
(`EnvioCorreoSmtpTest`, `CorreoRecuperacionListenerTest`).

Los **permisos** se prueban con dos matrices: `PermisosTest` (backend, cada
endpoint con cada perfil) y `Permisos.test.tsx` (web, cada ruta con cada
perfil). Las dos fallan si se añade un endpoint o una ruta sin decidir quién
puede usarlo.

Las pruebas **de punta a punta** (paso 5c) son 7 flujos, uno por archivo de
`e2e/pruebas`. Cada una prepara sus datos por la API (un servicio, un empleado
con horario de lunes a domingo de 09:00 a 20:00, un cliente), con nombres y
correos únicos, y usa la web solo para el flujo que prueba; así van en
paralelo y se repiten sin limpiar la base de datos. Buscan los elementos por
su rol y su texto visible, como un lector de pantalla, sin `data-testid`.
Usan una base de datos aparte (`gestion_de_citas_e2e`), su propio
administrador y un límite de inicios de sesión alto
(`APP_SECURITY_INTENTOSLOGINPORMINUTO=1000`), porque todas entran desde la
misma IP; ese límite ya lo prueba `LimiteIntentosLoginTest`. Solo Chromium.

## 3. Cobertura

- **Backend:** JaCoCo. `./mvnw verify` deja el informe en
  `backend/target/site/jacoco/index.html`. No cuentan el arranque
  (`GestionDeCitasApplication`), los DTO ni `shared/config`.
- **Web:** `@vitest/coverage-v8`. `npm run test:cobertura` deja el informe en
  `frontend/coverage/index.html`. No cuentan las pruebas, `src/pruebas/`,
  `main.tsx` ni `api/tipos.ts`.
- **CI:** cada ejecución muestra el resumen en su página ("Summary") y guarda
  los informes como artefactos `cobertura-backend` y `cobertura-web` durante
  14 días.
- **Punto de partida (31/10/2026, paso 5a):**

  | Parte | Pruebas | Líneas | Ramas | Funciones |
  |---|---|---|---|---|
  | Backend | 226 | 97,2 % | 84,8 % | — |
  | Web | 122 | 91,3 % | 80,3 % | 84,9 % |

  En el backend, lo único por debajo del 94 % era `shared/correo` (41,7 %): el
  envío real por SMTP, que las pruebas sustituyen por el buzón en memoria.
- **Tras el paso 5b:**

  | Parte | Pruebas | Líneas | Ramas | Funciones |
  |---|---|---|---|---|
  | Backend | 512 | 98,1 % | 88,7 % | — |
  | Web | 293 | 92,6 % | 82,1 % | 86,0 % |

  Las cifras de pruebas cuentan cada caso de una prueba parametrizada (270 de
  la matriz de permisos en el backend y 165 en la web).
- **Umbral (paso 5b):** el backend exige un **95 % de líneas y un 85 % de
  ramas** (`jacoco:check` en `./mvnw verify`) y la web un **90 % de líneas y
  un 80 % de ramas** (`coverage.thresholds` en `npm run test:cobertura`). Por
  debajo, la CI falla. Están algo por debajo de lo que hay para que una
  refactorización pequeña no rompa la CI, y se revisan al cerrar la fase. No
  se persigue el 100 %.

## 4. Criterios

- Una prueba prueba **una regla** y su nombre la dice en español
  (`elClienteNoCancelaConMenosDe24Horas`, "fuera de plazo no deja cancelar
  online"), con el CU, RF o RN entre paréntesis cuando aplica.
- Las pruebas no dependen del orden ni de la hora real: el backend usa el
  `Clock` inyectado y la web fija la fecha. Las de punta a punta usan la hora
  real, pero reservan dentro de 3 días (fuera del plazo de cancelación) con un
  horario que cubre todos los días, así que valen sea el día que sea.
- Una prueba que falla a veces es un defecto: se arregla en cuanto aparece,
  no se reintenta ni se desactiva.
- Cada defecto que se arregla trae una prueba que lo reproduce.
- La **matriz de trazabilidad** (`matriz-trazabilidad.md`) relaciona cada CU,
  RN y RNF con sus pruebas; se actualiza en cada PR que añade pruebas.

## 5. Entorno

- Backend: JDK 21 y Docker (Testcontainers crea y destruye su PostgreSQL; las
  pruebas nunca usan la base de datos de Docker Compose).
- Web: Node.js 24, sin backend (MSW).
- De punta a punta: Node.js 24, JDK 21, Chromium de Playwright, y PostgreSQL y
  Mailpit en Docker. Playwright arranca el backend (`.jar`) y la web
  (`vite preview`).
- CI: GitHub Actions (`.github/workflows/ci.yml`), un trabajo para el backend,
  otro para la web y, cuando los dos pasan, el de punta a punta, con
  PostgreSQL y Mailpit como servicios. Si falla, guarda el informe de
  Playwright (captura, vídeo y traza) como artefacto `informe-playwright`.

## 6. Pasos de la fase 5

| Paso | Contenido | Estado |
|---|---|---|
| 5a | Este plan, matriz de trazabilidad, cobertura en la CI y prueba intermitente arreglada | hecho (PR #15) |
| 5b | Huecos de la matriz, matrices de permisos (RNF-002), ArchUnit y umbral de cobertura | hecho (PR #16) |
| 5c | Pruebas de punta a punta con Playwright: 7 flujos en Chromium y trabajo `e2e` en la CI | en curso |
| 5d | Móvil, accesibilidad, reserva simultánea y comprobaciones de seguridad | pendiente |
| 5e | Pruebas manuales y defectos | pendiente |
| 5f | Informe de pruebas y cierre de la fase | pendiente |

## 7. Defectos encontrados

| Fecha | Defecto | Causa | Arreglo |
|---|---|---|---|
| 31/10/2026 | La prueba web "desactiva un servicio tras confirmarlo" fallaba de forma intermitente (3 de 9 ejecuciones) | Defecto de la prueba, no de la web: tras desactivar, la página vuelve a pedir el servicio (`invalidarServicios`) y el servidor simulado lo devolvía todavía activo. Si esa respuesta llegaba antes de que la prueba buscase el título, la página volvía a "Desactivar el servicio" | El servidor simulado recuerda el cambio, como haría el backend (5a) |
