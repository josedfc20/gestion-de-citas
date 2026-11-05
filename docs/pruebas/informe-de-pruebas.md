# Informe de pruebas

> Fase 5 (Pruebas), paso 5f. Propuesta aprobada el 05/11/2026 en
> `docs/propuestas/5.7-5f-informe-y-cierre.md`. Cierra la fase 5.
> Cifras de la CI de `main` tras fusionar el PR #21:
> [ejecución nº 43](https://github.com/josedfc20/gestion-de-citas/actions/runs/42004599678).

## 1. Resumen

- GestionDeCitas cumple sus **21 casos de uso, 11 reglas de negocio y los
  requisitos no funcionales** que se pueden probar antes del despliegue. Cada
  uno tiene al menos una prueba (matriz de trazabilidad).
- **Todas las pruebas pasan:** 512 en el backend, 299 en la web
  y 25 de punta a punta. La CI las ejecuta en cada PR y en cada push
  a `main`.
- La cobertura supera el mínimo exigido: **98,1 % de líneas y
  88,7 % de ramas** en el backend (mínimo 95/85) y **92,6 % y 82,2 %**
  en la web (mínimo 90/80).
- Se encontraron **7 defectos** y están todos arreglados, cada uno con una
  prueba que impide que vuelva.
- Queda fuera, a propósito, lo que depende del servidor de producción (fase 7)
  y las mejoras de la prueba manual (después de la primera versión).

## 2. Qué se probó

| Nivel | Qué prueba | Herramientas | Pruebas |
|---|---|---|---|
| Unitarias (backend) | Reglas puras: disponibilidad, horario semanal, edad mínima, contraseñas, JWT, estados de la cita | JUnit 5, AssertJ | 512 en total en el backend (41 clases; 23 de integración) |
| Integración (backend) | Cada endpoint con su seguridad, validación, reglas y PostgreSQL real; la matriz de permisos (54 endpoints × 5 perfiles) | MockMvc, Testcontainers | (incluidas arriba) |
| Arquitectura (backend) | Las reglas del paso 3.2: capas, DTO, dependencias, `Clock` | ArchUnit | (incluidas arriba) |
| Componentes (web) | Cada pantalla con la API simulada; la matriz de rutas por perfil (166 casos) | Vitest, Testing Library, MSW | 299 (13 archivos) |
| De punta a punta | Los flujos principales con todo real, en el escritorio y en un móvil de 390 px; accesibilidad (axe, 33 páginas en claro y oscuro); reserva simultánea; seguridad básica | Playwright, axe | 25 (11 archivos) |
| Manuales | Que se entienda y se vea bien, el móvil de verdad, el teclado, los correos, otro navegador | Lista de una hora con la demo | 8 bloques (A a H), pasada por el usuario |

Cómo está organizado cada nivel y qué criterios siguen las pruebas:
[`plan-de-pruebas.md`](plan-de-pruebas.md).

## 3. Cifras finales

| Parte | Pruebas | Líneas | Ramas | Funciones | Mínimo (líneas / ramas) |
|---|---|---|---|---|---|
| Backend | 512 | 98,1 % | 88,7 % | — | 95 % / 85 % |
| Web | 299 | 92,6 % | 82,2 % | 86,0 % | 90 % / 80 % |
| De punta a punta | 25 | — | — | — | — |

Evolución durante la fase:

| Momento | Backend | Web |
|---|---|---|
| Inicio (5a) | 226 pruebas · 97,2 % líneas · 84,8 % ramas | 122 pruebas · 91,3 % · 80,3 % |
| Tras el 5b | 512 · 98,1 % · 88,7 % | 293 · 92,6 % · 82,1 % |
| Cierre (5f) | 512 · 98,1 % · 88,7 % | 299 · 92,6 % · 82,2 % |

Las cifras cuentan cada caso de una prueba parametrizada. El código del
backend no ha cambiado desde el 5b (los pasos 5c a 5e solo tocaron las
pruebas de punta a punta y la web), por eso sus cifras son las mismas. Lo que se deja sin
cubrir a propósito (líneas defensivas que solo se ejecutan si la base de datos
o el propio `.jar` están rotos) está explicado al final de la matriz.

## 4. Trazabilidad

[`matriz-trazabilidad.md`](matriz-trazabilidad.md) relaciona cada requisito
con sus pruebas:

| Tipo | Total | Cubiertos | Pendientes |
|---|---|---|---|
| Casos de uso (CU-001 a CU-021) y configuración del negocio | 22 | 22 | — |
| Reglas de negocio (RN-001 a RN-011) | 11 | 11 | — |
| Requisitos no funcionales (RNF-001 a RNF-008) | 8 | 7 | RNF-004 (HTTPS), en la fase 7 |

Los 10 huecos que encontró la matriz al empezar (H-1 a H-10) se cubrieron en
el 5b y el 5d.

## 5. Requisitos no funcionales

| RNF | Cómo se comprobó |
|---|---|
| RNF-001 Contraseñas | Se guardan con bcrypt; ninguna respuesta de la API lleva un hash (`seguridad`) |
| RNF-002 Permisos por rol | Dos matrices que fallan si aparece un endpoint o una ruta sin decidir quién puede usarlo |
| RNF-003 Sin reservas incompatibles | Restricción en la base de datos, reserva a la vez en el backend y desde dos navegadores (`concurrencia`) |
| RNF-004 HTTPS | Fase 7, en el servidor de producción |
| RNF-005 Pruebas | Este informe, la matriz y la CI |
| RNF-006 Código modular | ArchUnit sobre el código compilado |
| RNF-007 RGPD | Privacidad obligatoria al registrarse, exportar los datos y eliminar la cuenta con anonimización (`rgpd`) |
| RNF-008 Móvil | Proyecto `movil` de Playwright, tamaño de toque de 44 px y la prueba en el iPhone del usuario |

La accesibilidad (WCAG 2.1 A y AA) se comprueba con axe en 33 páginas de los
cuatro perfiles, en modo claro y oscuro. No queda ningún fallo, ni grave ni
leve.

## 6. Defectos

Todos tienen su ficha en [`registro-de-defectos.md`](registro-de-defectos.md):
qué se vio, la causa, la solución, el PR y la prueba que lo vigila.

| Nº | Qué pasaba | De qué era | Dónde salió | PR |
|---|---|---|---|---|
| D-1 | Una prueba de la web fallaba 3 de cada 9 veces | De la prueba | Pruebas de la web (5a) | #15 |
| D-2 | Los enlaces del menú del móvil medían 40 px | De la aplicación | `movil` (5d) | #18 |
| D-3 | Dos `aria-label` no contenían el texto visible | De la aplicación | `accesibilidad` (5d) | #18 |
| D-4 | Tres pruebas de punta a punta fallaban a veces en la CI | De la prueba | CI (5e) | #20 |
| D-5 | Con el backend parado salía "error inesperado" | De la aplicación | Prueba manual | #21 |
| D-6 | Al registrarse desde una reserva se perdía la hora | De la aplicación | Prueba manual | #21 |
| D-7 | "Buscar cliente" del admin no se entendía | De la aplicación | Prueba manual | #21 |

Lo aprendido:

- **Una prueba que falla a veces es un defecto.** No se reintenta: frenar el
  navegador (`E2E_CPU_LENTA`) reproduce en el PC lo que solo pasa en la CI.
- **Tras pulsar un enlace hay que esperar a algo que solo tenga la página
  nueva.** Que cambie la dirección no basta.
- **Los simulacros tienen que comportarse como lo real**, también después de
  un cambio (D-1) y cuando falta el backend detrás del proxy (D-5).
- **Hay que seguir los datos por todos los caminos.** Probar cada página por
  separado no vio que la hora se perdía al pasar por el registro (D-6).
- **La prueba manual encuentra lo que las automáticas no juzgan.** Tres de
  los siete defectos salieron de ella.

## 7. Prueba manual

La pasó el usuario el 05/11/2026 con la demo: Chrome en Windows y Chrome en un
iPhone 15 Pro Max ([`pruebas-manuales.md`](pruebas-manuales.md)).

| Resultado | Puntos |
|---|---|
| Defectos, arreglados | M-1 (preparación de la demo, en la propia lista), M-2 (D-5), M-5 (D-7), M-7 (D-6) |
| Mejoras, para después de la v1 | M-3 (fotos), M-4 (tarjetas con foto en vez de carrusel), M-8 (ver la contraseña) |
| No eran defectos | M-6 (privacidad en el registro), M-9 (teclado) |

## 8. Lo que queda fuera

| Qué | Por qué | Cuándo |
|---|---|---|
| Pruebas automáticas en Firefox y Safari | Solo Chromium (decisión 2 del 5c); Firefox y Safari se miran a mano (bloques D y G) | — |
| HTTPS (RNF-004), cabeceras de la web (CSP, HSTS) y análisis de dependencias | Dependen del servidor de producción | Fase 7 |
| Pruebas de carga o rendimiento | Una peluquería tiene pocos usuarios a la vez, y la reserva simultánea sí está probada. Medir ahora sería medir un servidor que no es el de producción | No previstas |
| Mejoras M-3, M-4 y M-8 | Funcionan; la fase 5 solo arregla defectos (decisión 2 del 5e) | Después de la v1 |

## 9. Cómo repetirlo

| Qué | Comando | Necesita |
|---|---|---|
| Backend | `cd backend` y `./mvnw verify` | JDK 21 y Docker |
| Web | `cd frontend` y `npm run test:cobertura` | Node.js 24 |
| De punta a punta | `cd e2e` y `npm test` | Lo anterior, y PostgreSQL y Mailpit en Docker ([guía](../guias/pruebas-e2e.md)) |
| Prueba manual | [`pruebas-manuales.md`](pruebas-manuales.md), apartado 0 | La demo |

La CI (`.github/workflows/ci.yml`) hace las tres primeras en cada PR y en cada
push a `main`. Guarda los informes de cobertura y, si falla una prueba de
punta a punta, su informe con captura, vídeo y traza.
