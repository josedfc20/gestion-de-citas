# Matriz de trazabilidad

> Fase 5, paso 5a (31/10/2026); actualizada en el 5b y el 5c. Relaciona cada caso de uso, regla de negocio
> y requisito no funcional con las pruebas que lo cubren. Se actualiza en cada
> PR que añade pruebas.
>
> **Backend:** clase de prueba de `backend/src/test` (método entre paréntesis
> cuando solo cubre una parte). **Web:** archivo de `frontend/src` › bloque
> (`describe`). **E2E:** archivo de `e2e/pruebas` (paso 5c), sin el `.spec.ts`.
>
> Estado: ✅ cubierto · 🟡 cubierto en parte (ver huecos) · ❌ sin prueba ·
> — no se puede probar todavía

## 1. Casos de uso

| CU | Caso de uso | Backend | Web | E2E | Estado |
|---|---|---|---|---|---|
| CU-001 | Registrarse | `RegistroTest`, `EdadMinimaTest`, `PoliticaPasswordTest` | `Registro.test` › registro | `cliente` | ✅ |
| CU-002 | Iniciar sesión | `LoginTest`, `LimiteIntentosLoginTest`, `SesionTest`, `JwtServiceTest` | `Login.test` › login, `sesion.test`, `cliente.test` | `cliente` (y todas las demás) | ✅ |
| CU-003 | Cerrar sesión | `SesionTest` (elLogout…) | `sesion.test` (cerrar sesión…) | `cliente` | ✅ |
| CU-004 | Modificar perfil | `PerfilYPasswordTest` | `Cuenta.test` › perfil | `rgpd` (descargar datos y eliminar la cuenta) | ✅ |
| CU-005 | Consultar servicios y precios | `ServiciosTest` (…SaleEnElCatalogoPublico, desactivarLoOcultaDelPublico…), `SeguridadTest` (lasConsultasDelCatalogoSonPublicas) | `ZonaPublica.test` | `cliente` | ✅ |
| CU-006 | Consultar profesionales | `EmpleadosTest` (losProfesionalesPublicos…, desactivarCierraSusSesionesYLoOcultaDelPublico) | `ZonaPublica.test` | `cliente` | ✅ |
| CU-007 | Consultar horarios disponibles | `CalculoDisponibilidadTest`, `DisponibilidadTest` | `ZonaPublica.test` (horarios: …) | `cliente`, `horarios` | ✅ |
| CU-008 | Reservar cita | `ReservasTest`, `CitaTest` (alReservar…), `RestriccionSolapeTest` | `ZonaCliente.test` › reservar | `cliente`, `menor`, `empleado` | ✅ |
| CU-009 | Reservar para un menor a cargo | `ReservasTest` (reservaParaUnMenorACargo, elBeneficiarioTieneQueSerSuyo…), `BeneficiariosTest`, `BeneficiarioTest` | `ZonaCliente.test` › reservar, menores a mi cargo | `menor` | ✅ |
| CU-010 | Consultar próximas citas | `CitasClienteTest` (misCitas…) | `ZonaCliente.test` › mis citas | `cliente`, `menor` | ✅ |
| CU-011 | Consultar detalle de una cita | `CitasClienteTest` (cadaUnoVeElDetalleDeLoSuyo) | `ZonaCliente.test` › detalle y cancelar | `cliente` | ✅ |
| CU-012 | Cancelar cita | `CitaTest`, `CitasClienteTest` (cancelar…, conMenosDe24Horas…, soloElCliente…) | `ZonaCliente.test` › detalle y cancelar | `cliente` | ✅ |
| CU-013 | Consultar agenda | `AgendaEmpleadoTest` | `ZonaEmpleado.test` › agenda | `empleado` | ✅ |
| CU-014 | Consultar citas asignadas | `AgendaEmpleadoTest` (laListaDeCitasFiltra…) | `ZonaEmpleado.test` › citas asignadas | — (la agenda sí, en `empleado`) | ✅ |
| CU-015 | Actualizar estado de una cita | `EstadosCitaTest`, `CitaTest` | `ZonaEmpleado.test` › detalle y cambio de estado | `empleado` (solo cancelar con motivo) | ✅ |
| CU-016 | Gestionar servicios y precios | `ServiciosTest`, `DuracionServicioTest` | `ZonaAdminGestion.test` › servicios, servicios: volver a activar | `empleado` (alta) | ✅ |
| CU-017 | Gestionar empleados | `EmpleadosTest`, `PasswordTemporalTest` | `ZonaAdminGestion.test` › empleados, empleados: datos y reactivar | `empleado` (alta y contraseña temporal) | ✅ |
| CU-018 | Configurar horarios | `HorariosTest`, `HorarioSemanalTest`, `DiasCierreTest` | `ZonaAdminGestion.test` › horarios, días de cierre, días de cierre pasados | `horarios` | ✅ |
| CU-019 | Consultar y gestionar reservas | `AdminCitasTest`, `EstadosCitaTest` (elAdminCancela…) | `ZonaAdmin.test` › reservas, gestionar una reserva, reservar por un cliente | `reserva-admin` | ✅ |
| CU-020 | Recuperar contraseña | `RecuperarPasswordTest`, `EnlaceRecuperacionTest`, `LimiteSolicitudesRecuperacionTest` | `Cuenta.test` › recuperar la contraseña | `recuperar-password` | ✅ |
| CU-021 | Consultar servicios asignados | `EmpleadosTest` (soloElAdminGestionaEmpleadosYSoloElEmpleadoVeSusServicios) | `ZonaEmpleado.test` › mis servicios | `empleado` | ✅ |
| — | Configuración del negocio | `NegocioTest` | `ZonaAdminGestion.test` › configuración del negocio, `ZonaPublica.test` (GET /negocio) | — | ✅ |

## 2. Reglas de negocio

| RN | Regla | Backend | Web | Estado |
|---|---|---|---|---|
| RN-001 | Duración | `CitaTest` (alReservarCalculaElFin…), `ReservasTest` (reservaConFinYPrecioDelServidor…), `CalculoDisponibilidadTest` (elServicioTieneQueCaberEntero…) | — (lo decide el servidor) | ✅ |
| RN-002 | Horario general ajustado por empleado | `HorariosTest` (elAjusteDeUnEmpleado…), `HorarioSemanalTest`, `DisponibilidadTest` (usaElAjusteDelEmpleado…) | `ZonaAdminGestion.test` › horarios; E2E: `horarios` | ✅ |
| RN-003 | Confirmación automática | `ReservasTest` (reservaConFinYPrecio…), `CitaTest` | `ZonaCliente.test` › reservar | ✅ |
| RN-004 | Cancelar hasta 24 h antes | `CitaTest` (…JustoEnElLimite…, …ConMenosDe24Horas, elEmpleadoCancela…), `CitasClienteTest`, `EstadosCitaTest` (elAdminCancela…) | `ZonaCliente.test` (fuera de plazo…, si el plazo vence…) | ✅ |
| RN-005 | Cuentas de empleado y contraseña temporal | `EmpleadosTest`, `PerfilYPasswordTest` (elEmpleadoConContrasenaTemporal…), `RecuperarPasswordTest` (elEmpleadoQueRecupera…) | `sesion.test` (con contraseña temporal…), `ZonaAdminGestion.test` › empleados; E2E: `empleado` | ✅ |
| RN-006 | Edad mínima 14 años, configurable | `EdadMinimaTest`, `RegistroTest` (menorDeCatorce…) | `Registro.test` (avisa…edad mínima, 422) | ✅ |
| RN-007 | Menores sin cuenta propia | `BeneficiariosTest`, `BeneficiarioTest`, `ReservasTest` (elBeneficiarioTieneQueSerSuyoYSeguirSiendoMenor) | `ZonaCliente.test` › menores a mi cargo; E2E: `menor` | ✅ |
| RN-008 | Estados de la cita | `CitaTest` (losEstadosFinalesNoCambian, noSePuedeVolverAConfirmar…), `EstadosCitaTest` | `ZonaEmpleado.test` › detalle y cambio de estado | ✅ |
| RN-009 | Antelación máxima | `DisponibilidadTest` (soloAdmiteFechasDeHoyHastaLaAntelacionMaxima), `ReservasTest` (reglasDeFecha…) | — (lo decide el servidor) | ✅ |
| RN-010 | Intervalos | `CalculoDisponibilidadTest` (generaHorasCadaIntervalo…, respetaOtrosIntervalos, …AlineadasAlReloj…), `NegocioTest` | `ZonaAdminGestion.test` › configuración del negocio | ✅ |
| RN-011 | Duración múltiplo del intervalo | `ServiciosTest` (laDuracionDebeSerMultiplo…), `NegocioTest` (noSeCambiaElIntervalo…) | `utilidades.test` › panel del admin, `ZonaAdminGestion.test` › servicios | ✅ |

## 3. Requisitos no funcionales

| RNF | Requisito | Pruebas | Estado |
|---|---|---|---|
| RNF-001 | Contraseñas con hash seguro | `RegistroTest` (…ConLaContrasenaEnBcrypt), `AdministradorInicialTest`, `SesionTest` (enLaBaseDeDatosSoloSeGuardaElHash…), `DatosPersonalesTest` (…SinHashes), `EnlaceRecuperacionTest` (elTokenNoApareceEnElLog) | ✅ |
| RNF-002 | Permisos por rol | **`PermisosTest`**: los 54 endpoints con 5 perfiles (anónimo, cliente, empleado, empleado con contraseña temporal y admin) y la comprobación de que la tabla tiene todos los endpoints; `SeguridadTest` (tokens manipulados o caducados) y las pruebas "solo…" de cada clase (lo ajeno es 404). Web: **`Permisos.test`**, las 33 rutas con 5 perfiles y la comprobación de que la tabla tiene todas las rutas | ✅ |
| RNF-003 | Sin reservas incompatibles, también a la vez | `ReservasTest` (dosReservasALaVezDelMismoHuecoSoloConfirmanUna, laRestriccionDeLaBaseDeDatos…), `RestriccionSolapeTest` | 🟡 H-6 |
| RNF-004 | HTTPS en el entorno desplegado | — (fase 7) | — |
| RNF-005 | Pruebas de las funcionalidades principales | Esta matriz, la cobertura en la CI y el informe de la fase 5 | 🟡 fase 5 |
| RNF-006 | Código modular | **`ArquitecturaTest`** (ArchUnit): los controllers no usan repositories ni devuelven entidades, `usuario` y `beneficiario` no dependen de `cita`, nadie lee la hora sin el `Clock` y `CalculoDisponibilidad` no depende de Spring | ✅ |
| RNF-007 | RGPD y LOPDGDD | `RegistroTest` (sinAceptarLaPrivacidad…), `DatosPersonalesTest`, `BeneficiariosTest` (conCitasSeAnonimiza…), `CorreoRecuperacionListenerTest` (el token nunca va al log); web: `Cuenta.test`; E2E: `rgpd` (el JSON descargado no lleva contraseñas) | ✅ |
| RNF-008 | Web responsive y usable en el móvil | Ninguna automática (jsdom no tiene pantalla) | ❌ H-8 |

## 4. Huecos encontrados

| Hueco | Qué faltaba | Estado |
|---|---|---|
| H-1 | Web: volver a **activar** un servicio desactivado. | ✅ 5b (`ZonaAdminGestion.test` › servicios: volver a activar) |
| H-2 | Web: volver a **activar** un empleado y editar sus datos personales. | ✅ 5b (`ZonaAdminGestion.test` › empleados: datos y reactivar) |
| H-3 | Web: los días de cierre de los últimos 90 días. | ✅ 5b (`ZonaAdminGestion.test` › días de cierre pasados) |
| H-4 | Web: el admin **cambia el estado** de una reserva desde su detalle. | ✅ 5b (`ZonaAdmin.test` › gestionar una reserva) |
| H-5 | Una **matriz de permisos** con todos los endpoints y perfiles. | ✅ 5b (`PermisosTest`, `Permisos.test`) |
| H-6 | La reserva simultánea desde **dos navegadores** reales. | 5d |
| H-7 | Reglas de arquitectura del paso 3.2. | ✅ 5b (`ArquitecturaTest`) |
| H-8 | Móvil (390 px) y accesibilidad (axe). | 5d |
| H-9 | El envío real de correo y el fallo del envío. | ✅ 5b (`EnvioCorreoSmtpTest` y `CorreoRecuperacionListenerTest`, con Mockito) |
| H-10 | Reglas sin cubrir que marcaba JaCoCo. | ✅ 5b: editar con el correo o el nombre de otro (409), `GET /admin/servicios/{id}` (ninguna prueba lo llamaba), singular y plural de las citas futuras, enlace de recuperación de una cuenta desactivada, último día sin sugerencia, teléfono y motivo en blanco |
| — | Flujos completos de punta a punta. | ✅ 5c (`e2e/pruebas`, 7 flujos en Chromium) |

**Lo que se deja sin probar a propósito:** las líneas defensivas que solo se
ejecutan si la base de datos está rota (el registro del negocio de V2), si
falta un archivo del propio `.jar` o si Java no tiene SHA-256, y los `catch`
de dos peticiones iguales a la vez, que ya cubre la restricción de la base de
datos.
