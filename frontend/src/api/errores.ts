// Errores de la API. El backend responde siempre con Problem Details (RFC 9457):
// { status, title, detail, errores?: [{ campo, mensaje }] }.

export interface ErrorCampo {
  campo: string
  mensaje: string
}

export class ApiError extends Error {
  readonly status: number
  readonly titulo: string
  readonly errores: ErrorCampo[]
  readonly datos: Record<string, unknown>

  constructor(status: number, detalle: string, titulo = '', errores: ErrorCampo[] = [], datos = {}) {
    super(detalle)
    this.name = 'ApiError'
    this.status = status
    this.titulo = titulo
    this.errores = errores
    this.datos = datos
  }

  /** Sin respuesta del servidor (apagado, sin red...). */
  get esDeRed(): boolean {
    return this.status === 0
  }
}

export const MENSAJE_SIN_CONEXION = 'No se puede conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.'
const MENSAJE_GENERICO = 'Ha ocurrido un error inesperado. Inténtalo de nuevo más tarde.'

export function errorDeRed(): ApiError {
  return new ApiError(0, MENSAJE_SIN_CONEXION, 'Sin conexión')
}

/** Convierte una respuesta no 2xx en un ApiError, tenga o no cuerpo Problem Details. */
export async function errorDesdeRespuesta(respuesta: Response): Promise<ApiError> {
  let cuerpo: Record<string, unknown> = {}
  try {
    const texto = await respuesta.text()
    cuerpo = texto ? (JSON.parse(texto) as Record<string, unknown>) : {}
  } catch {
    cuerpo = {}
  }
  const { detail, title, errores, status: _status, type: _type, instance: _instance, ...resto } = cuerpo
  const detalle = typeof detail === 'string' && detail ? detail : respuesta.status >= 500 ? MENSAJE_GENERICO : `Error ${respuesta.status}`
  const lista = Array.isArray(errores) ? (errores as ErrorCampo[]).map(normalizarCampo) : []
  return new ApiError(respuesta.status, detalle, typeof title === 'string' ? title : '', lista, resto)
}

/**
 * Las validaciones de clase del backend (por ejemplo, "las contraseñas no
 * coinciden") llegan con el nombre del método: isConfirmacionPasswordCoincide
 * → confirmacionPasswordCoincide. Se asignan al campo que ve el usuario.
 */
function normalizarCampo(error: ErrorCampo): ErrorCampo {
  const campo = error.campo.endsWith('Coincide') ? error.campo.slice(0, -'Coincide'.length) : error.campo
  return { campo, mensaje: error.mensaje }
}

/** Texto para mostrar al usuario a partir de cualquier error. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof ApiError) return error.message
  return MENSAJE_GENERICO
}
