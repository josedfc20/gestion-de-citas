// Datos de ejemplo para las pruebas (como los de la demo del backend).

import type { Negocio, Profesional, Servicio, SesionResponse, UsuarioResumen } from '../api/tipos'

export const negocio: Negocio = {
  nombre: 'Peluquería Demo',
  zonaHoraria: 'Europe/Madrid',
  intervaloMinutos: 30,
  antelacionMaxDias: 60,
  horasMinCancelacion: 24,
  edadMinimaCuenta: 14,
}

export const servicios: Servicio[] = [
  { id: 1, nombre: 'Corte de pelo', descripcion: 'Lavado, corte y peinado.', duracionMinutos: 30, precio: 15 },
  { id: 2, nombre: 'Tinte', descripcion: null, duracionMinutos: 90, precio: 45.5 },
]

export const profesionales: Profesional[] = [
  { id: 10, nombre: 'Ana M.', servicios: [{ id: 1, nombre: 'Corte de pelo' }, { id: 2, nombre: 'Tinte' }] },
  { id: 11, nombre: 'Luis G.', servicios: [{ id: 1, nombre: 'Corte de pelo' }] },
]

export const cliente: UsuarioResumen = { id: 5, nombre: 'Marta', rol: 'CLIENTE', debeCambiarPassword: false }
export const empleadoTemporal: UsuarioResumen = { id: 6, nombre: 'Luis', rol: 'EMPLEADO', debeCambiarPassword: true }

export function sesionDe(usuario: UsuarioResumen, token = 'token-1'): SesionResponse {
  return { accessToken: token, tokenType: 'Bearer', expiraEnSegundos: 900, usuario }
}

export function problema(status: number, detail: string, extra: Record<string, unknown> = {}) {
  return { type: 'about:blank', title: 'Error', status, detail, ...extra }
}
