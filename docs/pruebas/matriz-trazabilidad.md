# Matriz de trazabilidad

> Fase 5, paso 5a (31/10/2026). Relaciona cada caso de uso, regla de negocio
> y requisito no funcional con las pruebas que lo cubren. Se actualiza en cada
> PR que añade pruebas.
>
> **Backend:** clase de prueba de `backend/src/test` (método entre paréntesis
> cuando solo cubre una parte). **Web:** archivo de `frontend/src` › bloque
> (`describe`). **E2E:** pruebas de punta a punta (paso 5c).
>
> Estado: ✅ cubierto · 🟡 cubierto en parte (ver huecos) · ❌ sin prueba ·
> — no se puede probar todavía

## 1. Casos de uso

| CU | Caso de uso | Backend | Web | E2E | Estado |
|---|---|---|---|---|---|
| CU-001 | Registrarse | `RegistroTest`, `EdadMinimaTest`, `PoliticaPasswordTest` | `Registro.test` › registro | 5c | ✅ |
| CU-002 | Iniciar sesión | `LoginTest`, `LimiteIntentosLoginTest`, `SesionTest`, `JwtServiceTest` | `Login.test` › login, `sesion.test`, `cliente.test` | 5c | ✅ |
| CU-003 | Cerrar sesión | `SesionTest` (elLogout…) | `sesion.test` (cerrar sesión…) | 5c | ✅ |
| CU-004 | Modificar perfil | `PerfilYPasswordTest` | `Cuenta.test` › perfil | 5c | ✅ |
| CU-005 | Consultar servicios y precios | `ServiciosTest` (…SaleEnElCatalogoPublico, desactivarLoOcultaDelPublico…), `SeguridadTest` (lasConsultasDelCatalogoSonPublicas) | `ZonaPublica.test` | 5c | ✅ |
| CU-006 | Consultar profesionales | `EmpleadosTest` (losProfesionalesPublicos…, desactivarCierraSusSesionesYLoOcultaDelPublico) | `ZonaPublica.test` | 5c | ✅ |
| CU-007 | Consultar horarios disponibles | `CalculoDisponibilidadTest`, `DisponibilidadTest` | `ZonaPublica.test` (horarios: …) | 5c | ✅ |
| CU-008 | Reservar cita | `ReservasTest`, `CitaTest` (alReservar…), `RestriccionSolapeTest` | `ZonaCliente.test` › reservar | 5c | ✅ |
| CU-009 | Reservar para un menor a cargo | `ReservasTest` (reservaParaUnMenorACargo, elBeneficiarioTieneQueSerSuyo…), `BeneficiariosTest`, `BeneficiarioTest` | `ZonaCliente.test` › reservar, menores a mi cargo | 5c | ✅ |
| CU-010 | Consultar próximas citas | `CitasClienteTest` (misCitas…) | `ZonaCliente.test` › mis citas | 5c | ✅ |
| CU-011 | Consultar detalle de una cita | `CitasClienteTest` (cadaUnoVeElDetalleDeLoSuyo) | `ZonaCliente.test` › detalle y cancelar | 5c | ✅ |
| CU-012 | Cancelar cita | `CitaTest`, `CitasClienteTest` (cancelar…, conMenosDe24Horas…, soloElCliente…) | `ZonaCliente.test` › detalle y cancelar | 5c | ✅ |
| CU-013 | Consultar agenda | `AgendaEmpleadoTest` | `ZonaEmpleado.test` › agenda | 5c | ✅ |
| CU-014 | Consultar citas asignadas | `AgendaEmpleadoTest` (laListaDeCitasFiltra…) | `ZonaEmpleado.test` › citas asignadas | 5c | ✅ |
| CU-015 | Actualizar estado de una cita | `EstadosCitaTest`, `CitaTest` | `ZonaEmpleado.test` › detalle y cambio de estado | 5c (solo cancelar) | ✅ |
| CU-016 | Gestionar servicios y precios | `ServiciosTest`, `DuracionServicioTest` | `ZonaAdminGestion.test` › servicios | 5c | 🟡 H-1 |
| CU-017 | Gestionar empleados | `EmpleadosTest`, `PasswordTemporalTest` | `ZonaAdminGestion.test` › empleados | 5c | 🟡 H-2 |
| CU-018 | Configurar horarios | `HorariosTest`, `HorarioSemanalTest`, `DiasCierreTest` | `ZonaAdminGestion.test` › horarios, días de cierre | 5c | 🟡 H-3 |
| CU-019 | Consultar y gestionar reservas | `AdminCitasTest`, `EstadosCitaTest` (elAdminCancela…) | `ZonaAdmin.test` › reservas, reservar por un cliente | 5c | 🟡 H-4 |
| CU-020 | Recuperar contraseña | `RecuperarPasswordTest`, `EnlaceRecuperacionTest`, `LimiteSolicitudesRecuperacionTest` | `Cuenta.test` › recuperar la contraseña | 5c | ✅ |
| CU-021 | Consultar servicios asignados | `EmpleadosTest` (soloElAdminGestionaEmpleadosYSoloElEmpleadoVeSusServicios) | `ZonaEmpleado.test` › mis servicios | 5c | ✅ |
| — | Configuración del negocio | `NegocioTest` | `ZonaAdminGestion.test` › configuración del negocio, `ZonaPublica.test` (GET /negocio) | — | ✅ |

## 2. Reglas de negocio

| RN | Regla | Backend | Web | Estado |
|---|---|---|---|---|
| RN-001 | Duración | `CitaTest` (alReservarCalculaElFin…), `ReservasTest` (reservaConFinYPrecioDelServidor…), `CalculoDisponibilidadTest` (elServicioTieneQueCaberEntero…) | — (lo decide el servidor) | ✅ |
| RN-002 | Horario general ajustado por empleado | `HorariosTest` (elAjusteDeUnEmpleado…), `HorarioSemanalTest`, `DisponibilidadTest` (usaElAjusteDelEmpleado…) | `ZonaAdminGestion.test` › horarios | ✅ |
| RN-003 | Confirmación automática | `ReservasTest` (reservaConFinYPrecio…), `CitaTest` | `ZonaCliente.test` › reservar | ✅ |
| RN-004 | Cancelar hasta 24 h antes | `CitaTest` (…JustoEnElLimite…, …ConMenosDe24Horas, elEmpleadoCancela…), `CitasClienteTest`, `EstadosCitaTest` (elAdminCancela…) | `ZonaCliente.test` (fuera de plazo…, si el plazo vence…) | ✅ |
| RN-005 | Cuentas de empleado y contraseña temporal | `EmpleadosTest`, `PerfilYPasswordTest` (elEmpleadoConContrasenaTemporal…), `RecuperarPasswordTest` (elEmpleadoQueRecupera…) | `sesion.test` (con contraseña temporal…), `ZonaAdminGestion.test` › empleados | ✅ |
| RN-006 | Edad mínima 14 años, configurable | `EdadMinimaTest`, `RegistroTest` (menorDeCatorce…) | `Registro.test` (avisa…edad mínima, 422) | ✅ |
| RN-007 | Menores sin cuenta propia | `BeneficiariosTest`, `BeneficiarioTest`, `ReservasTest` (elBeneficiarioTieneQueSerSuyoYSeguirSiendoMenor) | `ZonaCliente.test` › menores a mi cargo | ✅ |
| RN-008 | Estados de la cita | `CitaTest` (losEstadosFinalesNoCambian, noSePuedeVolverAConfirmar…), `EstadosCitaTest` | `ZonaEmpleado.test` › detalle y cambio de estado | ✅ |
| RN-009 | Antelación máxima | `DisponibilidadTest` (soloAdmiteFechasDeHoyHastaLaAntelacionMaxima), `ReservasTest` (reglasDeFecha…) | — (lo decide el servidor) | ✅ |
| RN-010 | Intervalos | `CalculoDisponibilidadTest` (generaHorasCadaIntervalo…, respetaOtrosIntervalos, …AlineadasAlReloj…), `NegocioTest` | `ZonaAdminGestion.test` › configuración del negocio | ✅ |
| RN-011 | Duración múltiplo del intervalo | `ServiciosTest` (laDuracionDebeSerMultiplo…), `NegocioTest` (noSeCambiaElIntervalo…) | `utilidades.test` › panel del admin, `ZonaAdminGestion.test` › servicios | ✅ |

## 3. Requisitos no funcionales

| RNF | Requisito | Pruebas | Estado |
|---|---|---|---|
| RNF-001 | Contraseñas con hash seguro | `RegistroTest` (…ConLaContrasenaEnBcrypt), `AdministradorInicialTest`, `SesionTest` (enLaBaseDeDatosSoloSeGuardaElHash…), `DatosPersonalesTest` (…SinHashes), `EnlaceRecuperacionTest` (elTokenNoApareceEnElLog) | ✅ |
| RNF-002 | Permisos por rol | `SeguridadTest` y una prueba "solo…" en cada clase de integración (`soloElAdminGestionaServicios`, `soloLosEmpleadosTienenAgenda`…); web: `ZonaAdmin.test`, `ZonaEmpleado.test`, `ZonaCliente.test` (rutas por rol) | 🟡 H-5 |
| RNF-003 | Sin reservas incompatibles, también a la vez | `ReservasTest` (dosReservasALaVezDelMismoHuecoSoloConfirmanUna, laRestriccionDeLaBaseDeDatos…), `RestriccionSolapeTest` | 🟡 H-6 |
| RNF-004 | HTTPS en el entorno desplegado | — (fase 7) | — |
| RNF-005 | Pruebas de las funcionalidades principales | Esta matriz, la cobertura en la CI y el informe de la fase 5 | 🟡 fase 5 |
| RNF-006 | Código modular | Sin prueba automática de las reglas de arquitectura (capas, dependencias entre paquetes) | ❌ H-7 |
| RNF-007 | RGPD y LOPDGDD | `RegistroTest` (sinAceptarLaPrivacidad…), `DatosPersonalesTest`, `BeneficiariosTest` (conCitasSeAnonimiza…); web: `Cuenta.test` | ✅ |
| RNF-008 | Web responsive y usable en el móvil | Ninguna automática (jsdom no tiene pantalla) | ❌ H-8 |

## 4. Huecos encontrados

| Hueco | Qué falta | Dónde se cubre |
|---|---|---|
| H-1 | Web: volver a **activar** un servicio desactivado (el backend sí lo prueba). | 5b |
| H-2 | Web: volver a **activar** un empleado y editar sus datos personales (el backend sí lo prueba). | 5b |
| H-3 | Web: los días de cierre de los últimos 90 días ("ver anteriores") y quitar un día de cierre del negocio. | 5b |
| H-4 | Web: el admin **cambia el estado** de una reserva desde su detalle (solo se prueba en la zona del empleado, que comparte `<CambioEstado>`). | 5b |
| H-5 | Una **matriz de permisos** que recorra todos los endpoints con los cuatro perfiles (anónimo, cliente, empleado, admin). Hoy cada clase prueba los suyos y un endpoint nuevo podría quedar abierto por olvido. | 5b |
| H-6 | La reserva simultánea desde **dos navegadores** reales. | 5d |
| H-7 | Reglas de arquitectura del paso 3.2 sin prueba: un controller nunca usa un repository; `usuario` y `beneficiario` no dependen de `cita`. Propuesta: ArchUnit (pruebas JUnit, sin cambiar el código). | 5b (si lo apruebas) |
| H-8 | Móvil (390 px) y accesibilidad (axe). | 5d |
| H-9 | El envío real de correo (`EnvioCorreoSmtp`, 41,7 % de sus líneas) y el fallo del envío en `CorreoRecuperacionListener`: las pruebas usan el buzón en memoria. Es el caso para Mockito (decisión 4) o para Mailpit en las E2E. | 5b / 5c |
| H-10 | Ramas sin cubrir en el backend (84,8 % de ramas): sobre todo `EmpleadoService` (6 líneas), `ServicioService` (4) y `DiaCierreService` (3). Se revisan una a una en el 5b; solo se prueban las que son reglas. | 5b |
| — | Flujos completos de punta a punta. | 5c |
