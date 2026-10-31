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
