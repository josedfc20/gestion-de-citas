# Plan de pruebas

> Fase 5 (Pruebas), paso 5a. Plan aprobado el 31/10/2026 en
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
| **Componentes (web)** | Cada pantalla con la API simulada: formularios, errores del servidor, rutas por rol, sesión y renovación del token | Vitest, Testing Library, MSW | `frontend/src/**/*.test.ts(x)` | `npm test` |
| **De punta a punta** | Los flujos principales con la web, el backend y PostgreSQL reales; móvil, accesibilidad y reserva simultánea | Playwright, axe | `e2e/` (paso 5c) | pendiente |
| **Manuales** | Lo que una máquina no juzga bien: que se entienda, que se vea bien, los correos | Lista de comprobación con el perfil `demo` | `docs/pruebas/pruebas-manuales.md` (paso 5e) | pendiente |

Mockito está en el stack pero no se usa: las reglas se prueban con PostgreSQL
real, que es más fiable que simular los repositorios. Solo se usará donde una
prueba de integración no llegue (decisión 4 del plan).

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

  En el backend, lo único por debajo del 94 % es `shared/correo` (41,7 %): el
  envío real por SMTP, que las pruebas sustituyen por el buzón en memoria.
- **Umbral:** se fijará en el paso 5b, después de cubrir los huecos
  (propuesta: 80 % de líneas en el backend y 85 % en la web). No se persigue
  el 100 %.

## 4. Criterios

- Una prueba prueba **una regla** y su nombre la dice en español
  (`elClienteNoCancelaConMenosDe24Horas`, "fuera de plazo no deja cancelar
  online"), con el CU, RF o RN entre paréntesis cuando aplica.
- Las pruebas no dependen del orden ni de la hora real: el backend usa el
  `Clock` inyectado y la web fija la fecha.
- Una prueba que falla a veces es un defecto: se arregla en cuanto aparece,
  no se reintenta ni se desactiva.
- Cada defecto que se arregla trae una prueba que lo reproduce.
- La **matriz de trazabilidad** (`matriz-trazabilidad.md`) relaciona cada CU,
  RN y RNF con sus pruebas; se actualiza en cada PR que añade pruebas.

## 5. Entorno

- Backend: JDK 21 y Docker (Testcontainers crea y destruye su PostgreSQL; las
  pruebas nunca usan la base de datos de Docker Compose).
- Web: Node.js 24, sin backend (MSW).
- CI: GitHub Actions (`.github/workflows/ci.yml`), un trabajo para el backend
  y otro para la web.

## 6. Pasos de la fase 5

| Paso | Contenido | Estado |
|---|---|---|
| 5a | Este plan, matriz de trazabilidad, cobertura en la CI y prueba intermitente arreglada | en curso |
| 5b | Huecos de la matriz, matriz de permisos (RNF-002) y umbral de cobertura | pendiente |
| 5c | Pruebas de punta a punta con Playwright | pendiente |
| 5d | Móvil, accesibilidad, reserva simultánea y comprobaciones de seguridad | pendiente |
| 5e | Pruebas manuales y defectos | pendiente |
| 5f | Informe de pruebas y cierre de la fase | pendiente |

## 7. Defectos encontrados

| Fecha | Defecto | Causa | Arreglo |
|---|---|---|---|
| 31/10/2026 | La prueba web "desactiva un servicio tras confirmarlo" fallaba de forma intermitente (3 de 9 ejecuciones) | Defecto de la prueba, no de la web: tras desactivar, la página vuelve a pedir el servicio (`invalidarServicios`) y el servidor simulado lo devolvía todavía activo. Si esa respuesta llegaba antes de que la prueba buscase el título, la página volvía a "Desactivar el servicio" | El servidor simulado recuerda el cambio, como haría el backend (5a) |
