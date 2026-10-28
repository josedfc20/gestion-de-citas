# AGENTS.md — GestionDeCitas

> Reglas y especificaciones del proyecto para cualquier asistente de IA
> (Claude, Codex, Copilot...). Se mantiene corto y ordenado por temas.
> El detalle de cada fase está en `docs/Doc.docx`.

## 1. Proyecto

- **Nombre:** GestionDeCitas
- **Descripción:** sistema versátil de gestión y reserva de citas.
- **Primer caso de aplicación:** una peluquería moderna con varios profesionales
  y servicios.
- **Versatilidad:** usar términos genéricos en diseño y código (negocio,
  profesional, servicio, cita), nunca términos propios de la peluquería, para
  poder adaptarlo a talleres, consultas, etc.
- **Alcance de la primera versión:** un único establecimiento.

## 2. Reglas de trabajo

El usuario actúa como la empresa y como senior/tech lead. Cada requisito sigue
este ciclo:

1. La empresa plantea la funcionalidad que necesita.
2. El asistente propone cómo resolverla.
3. El usuario revisa la propuesta como lo haría un tech lead.
4. Juntos se detectan errores o mejoras.
5. El asistente implementa la solución.
6. El usuario revisa el código.
7. Se pasa al siguiente requisito.

Normas adicionales:

- No se escribe código sin una propuesta aprobada.
- Pedir permiso antes de editar código o configuración, hacer push o abrir PRs.
- Responder en español.
- Tras cada paso aprobado, actualizar `AGENTS.md` y `docs/Doc.docx`.

## 3. Metodología por fases

El proyecto avanza en orden **sin saltarse ninguna fase**. Cada respuesta de
trabajo anuncia la fase actual ("Estamos en la fase N") y se destaca cuando se
avanza a la siguiente.

| Fase | Nombre        | Trabajo a realizar                                       |
|------|---------------|----------------------------------------------------------|
| 1    | Análisis      | Definir el problema, los usuarios y los requisitos.      |
| 2    | Diseño        | Crear los diagramas UML y diseñar la base de datos.      |
| 3    | Arquitectura  | Elegir tecnologías, estructura y componentes.            |
| 4    | Desarrollo    | Implementar las funcionalidades por pequeñas tareas.     |
| 5    | Pruebas       | Comprobar que la aplicación funciona correctamente.      |
| 6    | Documentación | Registrar decisiones técnicas y preparar el README.      |
| 7    | Despliegue    | Preparar una versión que pueda utilizarse y demostrarse. |

**Fase actual: 4. Desarrollo**

- [x] FASE 1: Análisis
- [x] FASE 2: Diseño
  - [x] 2.1 Lista de casos de uso (sección 10)
  - [x] 2.2 Diagramas de casos de uso (`docs/diagramas/CasosDeUso.drawio`)
  - [x] 2.3 Especificación de casos de uso (4 lotes, en Doc.docx)
  - [x] 2.4 Diagrama de clases (`docs/diagramas/Clases.drawio`)
  - [x] 2.5 Diagramas de secuencia, actividad y estados (`docs/diagramas/*.mmd`)
  - [x] 2.6 Diseño de la base de datos (`docs/propuestas/2.6-esquema-borrador.sql`)
- [x] FASE 3: Arquitectura
  - [x] 3.1 Tecnologías y stack
  - [x] 3.2 Arquitectura del backend (`docs/propuestas/3.2-arquitectura-backend.md`)
  - [x] 3.3 Diseño de la API REST (`docs/propuestas/3.3-api-rest.md`)
  - [x] 3.4 Seguridad y autenticación (`docs/propuestas/3.4-seguridad.md`)
  - [x] 3.5 Estructura del repositorio y entorno (`docs/propuestas/3.5-repositorio-y-entorno.md`)
- [ ] FASE 4: Desarrollo (plan en `docs/propuestas/4.0-plan-de-bloques.md`)
  - [x] 4.0 Plan de bloques: backend primero (B1 a B6) y web después (B7 a B9)
  - [x] B1 · Esqueleto: Spring Boot, docker-compose, Flyway V1/V2, CI (`feature/esqueleto-backend`, `docs/propuestas/4.1-B1-esqueleto-backend.md`)
  - [x] B2 · Autenticación: registro, login, JWT + refresh con rotación, roles, /me, admin inicial (`feature/autenticacion`, `docs/propuestas/4.2-B2-autenticacion.md`)
  - [x] B3 · Cuenta y RGPD: recuperar contraseña por correo, exportar datos y eliminar cuenta con anonimización (`feature/recuperar-password-rgpd`, `docs/propuestas/4.3-B3-cuenta-rgpd.md`)
  - [x] B4 · Catálogo y empleados: servicios, empleados y sus servicios, configuración del negocio, `/servicios` y `/profesionales` (`feature/catalogo-empleados`, `docs/propuestas/4.4-B4-catalogo-empleados.md`)
  - [x] B5 · Horarios y disponibilidad: horario general, ajuste por empleado, días de cierre, `GET /disponibilidad` y datos de demo (`feature/horarios-disponibilidad`, `docs/propuestas/4.5-B5-horarios-disponibilidad.md`)
  - [x] B6 · Citas (`docs/propuestas/4.6-B6-citas.md`)
    - [x] B6a · Cliente: beneficiarios, reservar, mis citas, detalle, cancelar y pendientes de B3–B5 (`feature/citas`)
    - [x] B6b · Empleado y administrador: agenda, estados, gestión de reservas y búsqueda de clientes (`feature/citas-gestion`)
  - [ ] B7 · Web: base y zona pública (`feature/web-base`)
  - [ ] B8 · Web: cliente (`feature/web-cliente`)
  - [ ] B9 · Web: empleado y administrador (`feature/web-empleado-admin`)
- [ ] FASE 5: Pruebas
- [ ] FASE 6: Documentación
- [ ] FASE 7: Despliegue

## 4. Problema y objetivo

- **Problema:** muchos negocios que trabajan con citas las gestionan por
  teléfono, WhatsApp o en una libreta: reservas duplicadas, solapamientos,
  citas olvidadas y tiempo perdido atendiendo llamadas.
- **Objetivo del producto:** un sistema versátil donde los clientes reserven en
  línea según la disponibilidad real de cada profesional, y el personal y el
  administrador gestionen agendas, servicios y horarios desde un solo lugar.
- **Objetivo formativo:** aplicar análisis, UML, bases de datos, arquitectura,
  desarrollo y pruebas en un proyecto completo.

## 5. Tecnologías (aprobadas en el paso 3.1; Spring Boot 4.1 desde el 28/10)

| Área                  | Elección |
|-----------------------|----------|
| Backend               | Java 21 + Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security), API REST |
| Base de datos         | PostgreSQL 16 + Flyway |
| Construcción          | Maven |
| Pruebas backend       | JUnit 5, Mockito, Testcontainers |
| Documentación API     | Swagger / OpenAPI |
| Frontend web          | React + TypeScript (Vite), React Router, TanStack Query, React Hook Form + Zod |
| Herramientas (PC)     | Git, JDK 21 (Temurin), Node.js 24 LTS, Docker Desktop (WSL 2); guía en `docs/guias/instalacion-entorno.md` |
| Pruebas frontend      | Vitest, Testing Library |
| Entorno local         | Docker Compose (PostgreSQL y Mailpit para ver los correos en http://localhost:8025) |
| Control de versiones  | Git + GitHub |
| Aplicación posterior  | Android con Kotlin y Jetpack Compose (Room opcional) |

## 5b. Reglas de arquitectura (paso 3.2)

- Monolito modular por funcionalidad: `auth`, `usuario`, `beneficiario`, `servicio`, `empleado`, `horario`, `disponibilidad`, `cita`, `negocio`, `shared`.
- Capas: Controller → Service → Repository. Un controller nunca accede a un repository.
- La API usa DTOs; nunca devolver entidades.
- Formato en DTOs (`@Valid`); reglas de negocio en servicios.
- Errores con Problem Details: 400, 401, 403, 404, 409 (hueco ocupado, dato duplicado), 422 (regla de negocio), 429 (demasiados intentos).
- Usar un `Clock` inyectado; nunca `now()` directo.
- Escrituras en métodos `@Transactional` del servicio.
- API: `/api/v1`, rutas por rol (`/auth`, `/me`, públicas, `/empleado`, `/admin`); recursos ajenos → 404; fin y precio los calcula el servidor.
- Seguridad: JWT 15 min (HS256, `JWT_SECRET`) + refresh token 7 días en cookie HttpOnly `SameSite=Strict` con ruta `/api/v1/auth` y rotación (en la BD solo su SHA-256; reutilizar uno revocado cierra todas las sesiones); BCrypt coste 12; contraseñas de 10 a 64 caracteres y no comunes; roles por ruta y propiedad en servicios; con contraseña temporal (RN-005) solo `GET /me` y `PUT /me/password`; 5 intentos de login por minuto por IP y por correo (429); nunca registrar contraseñas ni tokens.
- Cuenta y RGPD (B3): recuperar contraseña con enlace de un solo uso de 30 min (en la BD su SHA-256; token detrás de `#` en el enlace), respuesta 202 idéntica exista o no el correo, correo en segundo plano tras el commit, 3 solicitudes por hora por correo y 10 por IP (429); restablecer cierra todas las sesiones y no inicia sesión. `GET /me/datos` exporta en JSON sin hashes. `DELETE /me` solo para clientes, con la contraseña en el cuerpo: anonimiza (nombre, correo `eliminado-{id}@anonimo.invalid`, teléfono, fecha de nacimiento), desactiva, marca `eliminado_en` y borra sus tokens. Una cuenta eliminada nunca se reactiva.
- Catálogo y empleados (B4): nombre de servicio único sin distinguir mayúsculas; duración múltiplo del intervalo (RN-011, 422); los servicios y empleados nunca se borran, se desactivan. El alta de empleado genera una contraseña temporal de 12 caracteres que solo se devuelve en esa respuesta (y en `POST /admin/empleados/{id}/password-temporal`). Desactivar un empleado con citas futuras confirmadas → 409 con `citasFuturas`; al desactivarlo se cierran sus sesiones. Quitarle un servicio con citas futuras se permite con un aviso. En público, el profesional solo muestra nombre e inicial del apellido ("Luis G."). El intervalo solo admite 5, 10, 15, 20, 30 o 60 y no cambia si algún servicio dejaría de ser múltiplo (422); la edad mínima va de 14 a 18.
- Horarios y disponibilidad (B5): tramos `HH:mm` en múltiplos de 5 minutos, como mucho 4 por día, sin solaparse ni tocarse (422). El ajuste de un empleado es por día de la semana (tabla `ajuste_horario`): un día ajustado usa sus tramos (sin tramos = no trabaja) y los demás, el horario general; el ajuste puede salirse del general. Los días de cierre son días completos, del negocio (sin empleado) o de un empleado; no se crean en el pasado (422) ni repetidos (409). Guardar un horario o un cierre nunca cancela citas: la respuesta trae `citasAfectadas`. La disponibilidad calcula "hoy" en la zona del negocio, alinea las horas al reloj según el intervalo, exige que el servicio quepa entero en el tramo, descarta las horas ya empezadas y las que se solapan con citas `CONFIRMADA` o `COMPLETADA`, y si no hay huecos sugiere `siguienteFechaConHuecos`. El cálculo está en `CalculoDisponibilidad`, sin Spring.
- Citas (B6): al reservar se vuelve a pedir la disponibilidad del B5 (RF-007): una hora que no está en la lista es 409 "Ese horario ya no está disponible", igual que cuando la restricción `no_solape_profesional` para una reserva simultánea (RNF-003). Fin y precio los copia el servidor. Una misma persona (el cliente para sí mismo o un beneficiario) no tiene dos citas que se solapen (422). Beneficiario = menor de `edadMinimaCuenta`, comprobado al crearlo y al reservar; si tiene citas no se borra, se anonimiza y se cancelan sus citas futuras. El cliente cancela si faltan `horasMinCancelacion` o más (422 si no); el empleado (sus citas) y el admin, siempre y con motivo (`PATCH /citas/{id}/estado`); Completada y No presentado, desde la hora de inicio. Cada cambio deja un `cambio_estado_cita`. `GET /citas/{id}`: el cliente ve las suyas, el empleado las asignadas (con nombre y teléfono del cliente) y el admin todas (con correo e historial); lo ajeno es 404. Las fechas salen en la zona del negocio y las listas van paginadas (`Pagina`, como mucho 100) y, sin `desde`, empiezan hoy. La agenda del empleado (`GET /empleado/agenda`) da por día sus tramos, si es día de cierre y sus citas no canceladas, como mucho 31 días. El admin reserva por un cliente (`POST /admin/citas`, mismas reglas) y lo busca con `GET /admin/clientes?buscar=` (3 letras o más, 20 resultados, con sus beneficiarios). Rangos de fechas al revés o demasiado largos → 400 (`PeticionNoValidaException`). Los paquetes `usuario` y `beneficiario` no dependen de `cita`: definen interfaces (`AportaDatosPersonales`, `CitasDeBeneficiario`) que implementa `cita`.
- Correo detrás de la interfaz `EnvioCorreo` (`shared/correo`); en las pruebas, buzón en memoria (`CorreosDePrueba`). El proveedor SMTP real se elige en la fase 7.
- Configuración técnica y secretos en `application.yml` / variables de entorno, nunca en Git.
- El backend lee el `.env` de la raíz al arrancar; los valores del negocio están en la tabla `negocio`.
- Migraciones Flyway en `backend/src/main/resources/db/migration`: una migración aplicada nunca se modifica, los cambios van en una nueva (V3, V4...).
- Datos iniciales: V2 solo crea el negocio; el primer administrador se crea al arrancar desde `ADMIN_EMAIL` y `ADMIN_PASSWORD`; los datos de demostración van en el perfil `demo` (`DatosDemo`: solo si no hay servicios; cuentas con `DEMO_PASSWORD` y correos `@demo.invalid`).
- Pruebas de integración con `@IntegracionTest` (Testcontainers, PostgreSQL 16 real); los tests nunca usan la base de datos de Docker Compose.

## 5c. Continuidad entre hilos

Cada hilo nuevo debe leer primero este archivo, `docs/Doc.docx` y la carpeta
`docs/propuestas/`. Plan: un hilo nuevo para la fase 4 y uno por bloque de
desarrollo.

## 5d. Repositorio y Git (paso 3.5)

- Repositorio: https://github.com/josedfc20/gestion-de-citas (monorepo: `backend/`, `frontend/`, `docs/`).
- `main` siempre estable; cada tarea en una rama `feature/...` o `fix/...` que se integra con PR revisado por el usuario.
- Commits con Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).
- Nunca subir secretos: `.env` está en `.gitignore`; solo se versiona `.env.example`.
- Documentación del proyecto en `docs/`.

## 6. Perfiles de usuario

- **Visitante:** persona sin cuenta; consulta servicios, precios, profesionales
  y horarios.
- **Cliente:** persona registrada de 14 años o más; reserva, consulta y cancela
  sus citas y las de menores a su cargo.
- **Empleado:** profesional dado de alta por el administrador (no se registra
  solo); consulta su agenda y servicios y actualiza el estado de sus citas.
- **Administrador:** dueño o encargado; gestiona servicios, empleados, horarios
  y reservas.
- Los menores de 14 años no son usuarios: figuran como beneficiarios de una cita.
- "Usuario" = cualquier perfil con cuenta (cliente, empleado o administrador).

## 7. Requisitos funcionales

- RF-001: Registrar una cuenta de cliente.
- RF-002: Iniciar y cerrar sesión.
- RF-003: El visitante o el cliente consulta los servicios y sus precios.
- RF-004: El visitante o el cliente consulta los profesionales disponibles.
- RF-005: El visitante o el cliente consulta los horarios disponibles para un servicio y profesional.
- RF-006: El cliente reserva una cita eligiendo servicio, profesional, fecha y hora.
- RF-007: El sistema comprueba la disponibilidad antes de confirmar una reserva.
- RF-008: El cliente consulta sus próximas citas.
- RF-009: El cliente cancela una cita según las reglas del negocio.
- RF-010: El empleado consulta su agenda.
- RF-011: El empleado actualiza el estado de sus citas, según sus permisos.
- RF-012: El administrador gestiona los servicios y sus precios.
- RF-013: El administrador gestiona los empleados, incluida la creación de sus cuentas y la asignación de servicios.
- RF-014: El administrador configura los horarios de atención.
- RF-015: El administrador consulta y gestiona las reservas.
- RF-016: El usuario modifica los datos de su perfil.
- RF-017: El cliente reserva citas para un menor de 14 años a su cargo.
- RF-018: El usuario recupera su contraseña.
- RF-019: El empleado consulta los servicios que tiene asignados.

## 8. Requisitos no funcionales

- RNF-001: Contraseñas con hash seguro con sal, nunca en texto plano ni cifradas de forma reversible.
- RNF-002: Control de permisos de clientes, empleados y administradores.
- RNF-003: No se confirman dos reservas incompatibles para un mismo profesional, incluso cuando dos clientes reserven al mismo tiempo.
- RNF-004: HTTPS entre clientes y backend en el entorno desplegado.
- RNF-005: Pruebas que verifiquen las funcionalidades principales.
- RNF-006: Código modular, fácil de mantener y ampliar.
- RNF-007: Cumplimiento del RGPD y la LOPDGDD: consentimiento en el registro, consulta y eliminación de datos personales, y datos de menores según RN-006 y RN-007.
- RNF-008: La web es responsive y usable desde el móvil.

## 9. Reglas de negocio

| Código | Regla               | Descripción |
|--------|---------------------|-------------|
| RN-001 | Duración            | Cada servicio tiene una duración determinada. |
| RN-002 | Horarios            | Hay un horario general que el administrador ajusta por empleado. |
| RN-003 | Confirmación        | La reserva se confirma automáticamente si hay disponibilidad. |
| RN-004 | Cancelaciones       | El cliente puede cancelar hasta 24 horas antes. Pasado ese plazo, solo el administrador o el empleado pueden cancelarla. |
| RN-005 | Cuentas de empleado | Solo el administrador las crea. El empleado recibe una contraseña temporal y la cambia en su primer inicio de sesión. |
| RN-006 | Edad mínima         | Solo personas de 14 años o más pueden crear una cuenta de cliente (art. 7 LOPDGDD). Configurable. |
| RN-007 | Menores de 14 años  | No tienen cuenta propia: un cliente adulto reserva por ellos y figura como responsable. |
| RN-008 | Estados de cita     | Confirmada, Completada, Cancelada o No presentado. |
| RN-009 | Antelación máxima   | Máximo 60 días de antelación (configurable). |
| RN-010 | Intervalos          | Las citas empiezan en intervalos de 30 minutos (configurable). |
| RN-011 | Duración múltiplo   | La duración de cada servicio es múltiplo del intervalo de reserva (RN-010). |

## 10. Casos de uso

IDs fijos: nunca se reutilizan ni se renumeran; si un caso se elimina, se marca
**Obsoleto**. Cliente, Empleado y Administrador heredan del actor general
**Usuario** (generalización), que reúne los casos comunes de cuenta.

| ID     | Caso de uso                                        | Actor              | Requisito      |
|--------|----------------------------------------------------|--------------------|----------------|
| CU-001 | Registrarse                                        | Visitante          | RF-001         |
| CU-002 | Iniciar sesión                                     | Usuario            | RF-002         |
| CU-003 | Cerrar sesión                                      | Usuario            | RF-002         |
| CU-004 | Modificar perfil                                   | Usuario            | RF-016         |
| CU-005 | Consultar servicios y precios                      | Visitante, Cliente | RF-003         |
| CU-006 | Consultar profesionales                            | Visitante, Cliente | RF-004         |
| CU-007 | Consultar horarios disponibles                     | Visitante, Cliente | RF-005         |
| CU-008 | Reservar cita                                      | Cliente            | RF-006, RF-007 |
| CU-009 | Reservar cita para un menor a cargo                | Cliente            | RF-017, RN-007 |
| CU-010 | Consultar próximas citas                           | Cliente            | RF-008         |
| CU-011 | Consultar detalle de una cita                      | Cliente            | RF-008         |
| CU-012 | Cancelar cita                                      | Cliente            | RF-009, RN-004 |
| CU-013 | Consultar agenda                                   | Empleado           | RF-010         |
| CU-014 | Consultar citas asignadas                          | Empleado           | RF-010         |
| CU-015 | Actualizar estado de una cita                      | Empleado           | RF-011, RN-008 |
| CU-016 | Gestionar servicios y precios                      | Administrador      | RF-012         |
| CU-017 | Gestionar empleados (cuentas y servicios asignados)| Administrador      | RF-013, RN-005 |
| CU-018 | Configurar horarios                                | Administrador      | RF-014         |
| CU-019 | Consultar y gestionar reservas                     | Administrador      | RF-015         |
| CU-020 | Recuperar contraseña                               | Usuario            | RF-018         |
| CU-021 | Consultar servicios asignados                      | Empleado           | RF-019         |

## 11. Fuera del alcance inicial

- Pagos online, aplicación iOS y varios establecimientos.
- Promociones avanzadas, mensajería externa y automatizaciones con n8n.
- Notificaciones y recordatorios automáticos (solo se envía el correo de recuperación de contraseña).
- Modificar una cita (en el MVP se cancela y se vuelve a reservar).
- Bloqueo de horarios por parte del empleado.
- Adaptación a citas médicas (datos de salud = categoría especial del RGPD).

## 12. Temas pendientes de decidir

- Jose hará más adelante una revisión más profunda del diagrama de clases (paso 2.4).

## 13. Registro de decisiones

| Fecha      | Decisión |
|------------|----------|
| 2026-10-24 | El proyecto avanza por fases sin saltarse ninguna y se anuncia la fase actual. |
| 2026-10-24 | Solo el administrador crea cuentas de empleado. |
| 2026-10-24 | Edad mínima de cuenta: 14 años; los menores de 14 reservan a través de un adulto. |
| 2026-10-25 | Fase 1 revisada desde cero: Visitante, RF-018/019, RNF-007/008, RN-008 a RN-010. Fase 1 cerrada. |
| 2026-10-25 | El producto pasa a llamarse GestionDeCitas y se diseña de forma genérica; la peluquería es el primer caso. |
| 2026-10-25 | Paso 2.1 aprobado: casos de uso CU-001 a CU-021. |
| 2026-10-25 | Paso 2.2 aprobado: diagramas de casos de uso en `docs/diagramas/`. |
| 2026-10-25 | Paso 2.3, lote 1 aprobado: especificación de CU-001 a CU-004 y CU-020 (en Doc.docx). |
| 2026-10-25 | Paso 2.3, lote 2 aprobado: CU-005 a CU-012. RN-010 pasa de 15 a 30 minutos. |
| 2026-10-25 | Paso 2.3, lote 3 aprobado: CU-013 a CU-015 y CU-021. |
| 2026-10-25 | Paso 2.3 cerrado (lote 4: CU-016 a CU-019). Nueva RN-011: duración múltiplo del intervalo. |
| 2026-10-25 | Paso 2.4 aprobado: diagrama de clases (modelo de dominio). |
| 2026-10-25 | Paso 2.5 aprobado: secuencias Reservar/Cancelar, actividad Disponibilidad y estados de la cita. |
| 2026-10-25 | Paso 2.6 aprobado: base de datos. FASE 2 cerrada; empieza la FASE 3 (Arquitectura). |
| 2026-10-25 | Paso 3.1 aprobado: stack tecnológico; frontend React + TypeScript (Vite). |
| 2026-10-26 | Paso 3.2 aprobado: arquitectura del backend. Plan de un hilo por fase/bloque a partir de la fase 4. |
| 2026-10-26 | Paso 3.3 aprobado: diseño de la API REST. |
| 2026-10-26 | Paso 3.4 aprobado: seguridad. Nueva tabla refresh_token; nuevos endpoints /auth/refresh y /me/datos. |
| 2026-10-26 | Paso 3.5 aprobado: monorepo `gestion-de-citas` en GitHub, ramas feature/* con PR, Conventional Commits y CI con GitHub Actions. FASE 3 cerrada; empieza la FASE 4 (Desarrollo). |
| 2026-10-27 | Entorno instalado en el PC de Jose. Node.js 24 LTS en lugar de 22 (la 22 pasó a mantenimiento). |
| 2026-10-28 | Repositorio creado en GitHub; la documentación pasa a `docs/`. |
| 2026-10-28 | Paso 4.0 aprobado: plan de bloques B1 a B9; primero el backend (B1-B6) y después la web (B7-B9). |
| 2026-10-28 | Spring Boot 4.1 en lugar de 3.x (la 3.5 está al final de su soporte gratuito). |
| 2026-10-28 | Datos iniciales: V2 solo con el registro de `negocio`; primer administrador desde `ADMIN_EMAIL`/`ADMIN_PASSWORD` al arrancar (B2); datos de demo en el perfil `demo`. |
| 2026-10-28 | B1 aprobado: esqueleto del backend en `feature/esqueleto-backend`. |
| 2026-10-28 | B1 fusionado (PR #1). PostgreSQL de Docker en el puerto 5433 del PC para convivir con el PostgreSQL de Odoo de Jose. |
| 2026-10-28 | B2 aprobado: autenticación en `feature/autenticacion`. Cambios sobre el 3.4: refresh token con SHA-256 (no BCrypt), cookie en `/api/v1/auth`, logout público con la cookie y nuevo código 429. El registro no inicia sesión. |
| 2026-10-28 | B2 fusionado (PR #3). |
| 2026-10-28 | B3 aprobado: cuenta y RGPD en `feature/recuperar-password-rgpd`. Migración V4 (`usuario.eliminado_en`, fecha de nacimiento que admite vacío); solo los clientes eliminan su cuenta; al eliminarla, B6 cancelará sus citas futuras; `DELETE /me` con la contraseña en el cuerpo; Mailpit para el correo en local. |
| 2026-10-28 | B4 aprobado: catálogo y empleados en `feature/catalogo-empleados`. Migración V5; profesionales públicos con nombre e inicial del apellido; 409 al desactivar un empleado con citas futuras; endpoint para otra contraseña temporal; intervalo limitado a divisores de 60; datos de demo en el B5. |
| 2026-10-28 | B4 fusionado (PR #5). |
| 2026-10-28 | B5 aprobado: horarios y disponibilidad en `feature/horarios-disponibilidad`. Migración V6 (`ajuste_horario`, cierre único); ajuste por empleado día a día; guardar horarios o cierres con citas afectadas se permite y las lista; horas alineadas al reloj; el ajuste puede salirse del horario general; datos de demo con `DEMO_PASSWORD`. |
| 2026-10-28 | B5 fusionado (PR #6). |
| 2026-10-28 | B6 aprobado: citas en dos PR (`feature/citas` para el cliente y `feature/citas-gestion` para el empleado y el admin). Migración V7 (beneficiario anonimizable); al reservar se vuelve a comprobar la disponibilidad (409); el admin cancela con `PATCH /citas/{id}/estado`; nuevo `GET /admin/clientes?buscar=`; una misma persona no tiene dos citas solapadas; edad del menor según `edadMinimaCuenta`; "Mis citas" incluye las canceladas futuras. |
| 2026-10-28 | B6a fusionado (PR #7). |
