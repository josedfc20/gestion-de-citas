// Cliente HTTP de la API (paso 3.4 y propuesta 4.7).
//
// - El access token vive solo en memoria (esta variable), nunca en localStorage.
// - El refresh token es una cookie HttpOnly que el navegador envía solo a /api/v1/auth.
// - Si una petición devuelve 401 con token, se renueva la sesión y se repite UNA vez.
// - Solo hay un refresh a la vez: el backend rota el refresh token y, si recibe uno
//   ya usado, cierra todas las sesiones. Dentro de la pestaña se comparte la misma
//   promesa y entre pestañas se usa un candado del navegador (Web Locks API).

import { ApiError, errorDeRed, errorDesdeRespuesta } from './errores'
import type { SesionResponse } from './tipos'

const BASE = '/api/v1'
const CANDADO_REFRESH = 'gestion-de-citas:refresh'

let accessToken: string | null = null
let refrescoEnCurso: Promise<SesionResponse | null> | null = null
const oyentes = new Set<(sesion: SesionResponse | null) => void>()

export function tokenActual(): string | null {
  return accessToken
}

/** Guarda la sesión recibida (login, refresh o cambio de contraseña) o la borra con null. */
export function establecerSesion(sesion: SesionResponse | null): void {
  accessToken = sesion?.accessToken ?? null
}

/**
 * Avisa cuando el cliente renueva la sesión por su cuenta (usuario nuevo) o la
 * pierde (null) en mitad de una petición. Lo usa el SesionProvider.
 */
export function escucharSesion(oyente: (sesion: SesionResponse | null) => void): () => void {
  oyentes.add(oyente)
  return () => {
    oyentes.delete(oyente)
  }
}

function avisar(sesion: SesionResponse | null): void {
  oyentes.forEach((oyente) => oyente(sesion))
}

function url(ruta: string): string {
  // URL absoluta: funciona igual en el navegador y en las pruebas (Node).
  return new URL(BASE + ruta, window.location.origin).toString()
}

/** Ejecuta la función dentro del candado entre pestañas, si el navegador lo admite. */
function conCandado<T>(funcion: () => Promise<T>): Promise<T> {
  const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined
  return locks ? locks.request(CANDADO_REFRESH, funcion) : funcion()
}

async function llamarRefresh(): Promise<SesionResponse | null> {
  let respuesta: Response
  try {
    respuesta = await fetch(url('/auth/refresh'), { method: 'POST', credentials: 'same-origin' })
  } catch {
    throw errorDeRed()
  }
  if (respuesta.ok) {
    const sesion = (await respuesta.json()) as SesionResponse
    establecerSesion(sesion)
    return sesion
  }
  if (respuesta.status === 401) {
    establecerSesion(null)
    return null
  }
  throw await errorDesdeRespuesta(respuesta)
}

/**
 * Pide un access token nuevo con la cookie. Devuelve la sesión, o null si la
 * cookie no existe o ya no vale. Las llamadas simultáneas comparten la misma petición.
 */
export function renovarSesion(): Promise<SesionResponse | null> {
  refrescoEnCurso ??= conCandado(llamarRefresh).finally(() => {
    refrescoEnCurso = null
  })
  return refrescoEnCurso
}

export interface OpcionesPeticion {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  cuerpo?: unknown
  /** Parámetros de la URL; los vacíos se omiten. */
  parametros?: Record<string, string | number | boolean | null | undefined>
  /** false en login y registro: un 401 ahí no es una sesión caducada. */
  renovarSiCaduca?: boolean
  signal?: AbortSignal
}

function conParametros(ruta: string, parametros?: OpcionesPeticion['parametros']): string {
  if (!parametros) return ruta
  const busqueda = new URLSearchParams()
  for (const [clave, valor] of Object.entries(parametros)) {
    if (valor !== undefined && valor !== null && valor !== '') busqueda.set(clave, String(valor))
  }
  const texto = busqueda.toString()
  return texto ? `${ruta}?${texto}` : ruta
}

async function enviar(ruta: string, opciones: OpcionesPeticion): Promise<Response> {
  const cabeceras: Record<string, string> = { Accept: 'application/json' }
  if (opciones.cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'
  if (accessToken) cabeceras.Authorization = `Bearer ${accessToken}`
  try {
    return await fetch(url(conParametros(ruta, opciones.parametros)), {
      method: opciones.metodo ?? 'GET',
      headers: cabeceras,
      body: opciones.cuerpo === undefined ? undefined : JSON.stringify(opciones.cuerpo),
      credentials: 'same-origin',
      signal: opciones.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw errorDeRed()
  }
}

/** Hace una petición a la API y devuelve el JSON de la respuesta (o undefined si no hay cuerpo). */
export async function pedir<T>(ruta: string, opciones: OpcionesPeticion = {}): Promise<T> {
  const conToken = accessToken !== null
  let respuesta = await enviar(ruta, opciones)

  if (respuesta.status === 401 && conToken && opciones.renovarSiCaduca !== false) {
    let sesion: SesionResponse | null = null
    try {
      sesion = await renovarSesion()
    } catch (error) {
      if (error instanceof ApiError && error.esDeRed) throw error
    }
    avisar(sesion)
    if (!sesion) throw await errorDesdeRespuesta(respuesta)
    respuesta = await enviar(ruta, opciones)
  }

  if (!respuesta.ok) throw await errorDesdeRespuesta(respuesta)
  if (respuesta.status === 204) return undefined as T
  const texto = await respuesta.text()
  return (texto ? JSON.parse(texto) : undefined) as T
}
