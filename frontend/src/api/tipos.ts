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
