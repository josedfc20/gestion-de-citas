// Tipos de las respuestas de la API (los DTO del backend, en /api/v1).

export type Rol = 'CLIENTE' | 'EMPLEADO' | 'ADMIN'

/** Lo mínimo del usuario que la web necesita (login, refresh y cambio de contraseña). */
export interface UsuarioResumen {
  id: number
  nombre: string
  rol: Rol
  debeCambiarPassword: boolean
}

/** Respuesta de login, refresh y cambio de contraseña. El refresh token va en una cookie HttpOnly. */
export interface SesionResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiraEnSegundos: number
  usuario: UsuarioResumen
}

/** Configuración pública del negocio (GET /negocio). */
export interface Negocio {
  nombre: string
  zonaHoraria: string
  intervaloMinutos: number
  antelacionMaxDias: number
  horasMinCancelacion: number
  edadMinimaCuenta: number
}

export interface Servicio {
  id: number
  nombre: string
  descripcion: string | null
  duracionMinutos: number
  precio: number
}

export interface ServicioResumen {
  id: number
  nombre: string
}

/** Profesional en la zona pública: nombre e inicial del apellido ("Luis G."). */
export interface Profesional {
  id: number
  nombre: string
  servicios: ServicioResumen[]
}

/** Horas libres en hora local del negocio ("09:30"). */
export interface Disponibilidad {
  fecha: string
  zonaHoraria: string
  horas: string[]
  siguienteFechaConHuecos: string | null
}

export interface DatosRegistro {
  nombre: string
  apellidos: string
  email: string
  telefono: string | null
  fechaNacimiento: string
  password: string
  confirmacionPassword: string
  aceptaPrivacidad: boolean
}

export interface DatosCambioPassword {
  passwordActual: string
  passwordNueva: string
  confirmacionPassword: string
}

/** Lista paginada del backend (`Pagina<T>`). */
export interface Pagina<T> {
  contenido: T[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export type EstadoCita = 'CONFIRMADA' | 'COMPLETADA' | 'CANCELADA' | 'NO_PRESENTADO'

/** Una cita del cliente (lista y detalle). Las fechas llevan el desfase de la zona del negocio. */
export interface Cita {
  id: number
  servicio: { id: number; nombre: string; duracionMinutos: number }
  profesional: { id: number; nombre: string }
  /** null si la cita es para el propio cliente. */
  beneficiario: { id: number; nombre: string } | null
  inicio: string
  fin: string
  precio: number
  estado: EstadoCita
  motivoCancelacion: string | null
  /** RN-004 calculada por el servidor: la web no repite la regla de las 24 h. */
  puedeCancelar: boolean
  cancelableHasta: string
  /** Solo para el empleado y el admin (CU-014, CU-019); el correo, solo para el admin. */
  cliente?: DatosClienteCita
  /** Historial de cambios de estado: solo en el detalle del admin. */
  cambios?: CambioCita[]
}

export interface DatosClienteCita {
  id: number
  nombre: string
  apellidos: string
  telefono?: string | null
  email?: string
}

export interface CambioCita {
  estadoAnterior: EstadoCita | null
  estadoNuevo: EstadoCita
  fecha: string
  realizadoPor: number
  /** "Cliente", "Luis G." o "Administrador" (B9b). */
  realizadoPorNombre: string | null
}

/** Cliente en la búsqueda del admin, con sus menores a cargo (GET /admin/clientes). */
export interface ClienteEncontrado {
  id: number
  nombre: string
  apellidos: string
  email: string
  telefono: string | null
  beneficiarios: Beneficiario[]
}

/** CU-019, alt. 1: el admin reserva en nombre de un cliente registrado. */
export interface DatosReservaAdmin extends DatosReserva {
  clienteId: number
}

/** Tramo de horario en hora del negocio: { inicio: "09:00", fin: "14:00" }. */
export interface Tramo {
  inicio: string
  fin: string
}

/** Un día de la agenda del empleado (CU-013): sin tramos o con cierre, no trabaja. */
export interface AgendaDia {
  fecha: string
  cierre: boolean
  tramos: Tramo[]
  citas: Cita[]
}

/** Cambio de estado del empleado o el admin (CU-015, CU-019). El motivo, obligatorio al cancelar. */
export interface DatosCambioEstado {
  estado: Exclude<EstadoCita, 'CONFIRMADA'>
  motivo?: string
}

export interface DatosReserva {
  servicioId: number
  profesionalId: number
  /** Instante con desfase: "2026-11-03T10:00:00+01:00". */
  inicio: string
  beneficiarioId?: number
}

/** Menor a cargo del cliente (CU-009). */
export interface Beneficiario {
  id: number
  nombre: string
  apellidos: string
  fechaNacimiento: string
}

export interface DatosBeneficiario {
  nombre: string
  apellidos: string
  fechaNacimiento: string
}

/** Datos del usuario actual (GET /me). La fecha de nacimiento solo existe para clientes. */
export interface Perfil {
  id: number
  rol: Rol
  nombre: string
  apellidos: string
  email: string
  telefono: string | null
  fechaNacimiento?: string | null
  debeCambiarPassword: boolean
}

export interface DatosPerfil {
  nombre: string
  apellidos: string
  email: string
  telefono: string | null
}

export interface DatosRestablecerPassword {
  token: string
  password: string
  confirmacionPassword: string
}

// ---- Panel del administrador (B9c) ----

/** Servicio en el panel: también los inactivos y cuántos empleados activos lo realizan. */
export interface ServicioAdmin extends Servicio {
  activo: boolean
  empleados: number
}

export interface DatosServicio {
  nombre: string
  descripcion: string | null
  duracionMinutos: number
  precio: number
}

/** Empleado en el panel. `avisos` solo llega al cambiar sus servicios (CU-017, alt. 3). */
export interface EmpleadoAdmin {
  id: number
  nombre: string
  apellidos: string
  email: string
  telefono: string | null
  activo: boolean
  debeCambiarPassword: boolean
  servicios: ServicioResumen[]
  avisos?: string[]
}

export interface DatosEmpleado {
  nombre: string
  apellidos: string
  email: string
  telefono: string | null
}

export interface DatosAltaEmpleado extends DatosEmpleado {
  servicioIds: number[]
}

/** RN-005: la contraseña temporal solo llega en esta respuesta. */
export interface EmpleadoCreado {
  empleado: EmpleadoAdmin
  passwordTemporal: string
}

/** Cita confirmada que queda fuera del horario o en un día de cierre. No se cancela. */
export interface CitaAfectada {
  id: number
  profesionalId: number
  profesional: string
  inicio: string
  fin: string
}

/** Un día de la semana (1 = lunes ... 7 = domingo) con sus tramos; sin tramos, cerrado o libre. */
export interface DiaHorario {
  diaSemana: number
  tramos: Tramo[]
}

export interface HorarioSemana {
  dias: DiaHorario[]
  citasAfectadas?: CitaAfectada[]
}

/** Horario de un empleado (RN-002): sus días ajustados y la semana que resulta. */
export interface HorarioEmpleado {
  empleadoId: number
  ajustes: DiaHorario[]
  semanaEfectiva: { diaSemana: number; ajustado: boolean; tramos: Tramo[] }[]
  citasAfectadas?: CitaAfectada[]
}

/** Día de cierre: de todo el negocio (sin empleado) o de un empleado. */
export interface DiaCierre {
  id: number
  fecha: string
  empleadoId: number | null
  empleado: string | null
  motivo: string | null
  citasAfectadas?: CitaAfectada[]
}

export interface DatosDiaCierre {
  fecha: string
  empleadoId: number | null
  motivo: string | null
}
