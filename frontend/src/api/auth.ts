import { establecerSesion, pedir } from './cliente'
import type { DatosCambioPassword, DatosRegistro, DatosRestablecerPassword, SesionResponse, UsuarioResumen } from './tipos'

/** CU-002. Guarda el access token en memoria; el refresh token llega en la cookie. */
export async function login(email: string, password: string): Promise<SesionResponse> {
  const sesion = await pedir<SesionResponse>('/auth/login', {
    metodo: 'POST',
    cuerpo: { email, password },
    renovarSiCaduca: false,
  })
  establecerSesion(sesion)
  return sesion
}

/** CU-003. Revoca el refresh token (cookie) aunque el access token haya caducado. */
export async function logout(): Promise<void> {
  try {
    await pedir<void>('/auth/logout', { metodo: 'POST', renovarSiCaduca: false })
  } finally {
    establecerSesion(null)
  }
}

/** CU-001. No inicia sesión: después hay que hacer login. */
export function registrar(datos: DatosRegistro): Promise<UsuarioResumen> {
  return pedir<UsuarioResumen>('/auth/registro', { metodo: 'POST', cuerpo: datos, renovarSiCaduca: false })
}

/** CU-004 / RN-005. El backend cierra las demás sesiones y devuelve tokens nuevos. */
export async function cambiarPassword(datos: DatosCambioPassword): Promise<SesionResponse> {
  const sesion = await pedir<SesionResponse>('/me/password', { metodo: 'PUT', cuerpo: datos })
  establecerSesion(sesion)
  return sesion
}

/** CU-020, paso 1. El servidor responde 202 igual exista o no el correo. */
export async function recuperarPassword(email: string): Promise<void> {
  await pedir<unknown>('/auth/recuperar-password', { metodo: 'POST', cuerpo: { email }, renovarSiCaduca: false })
}

/** CU-020, paso 4. Cierra todas las sesiones y no inicia sesión. */
export function restablecerPassword(datos: DatosRestablecerPassword): Promise<void> {
  return pedir<void>('/auth/restablecer-password', { metodo: 'POST', cuerpo: datos, renovarSiCaduca: false })
}
