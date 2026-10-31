// Datos de ejemplo para las pruebas (como los de la demo del backend).

import type { AgendaDia, Beneficiario, Cita, Negocio, Pagina, Perfil, Profesional, Servicio, SesionResponse, UsuarioResumen } from '../api/tipos'
import { sumarDias } from '../utilidades/fechas'

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

export const empleado: UsuarioResumen = { id: 7, nombre: 'Ana', rol: 'EMPLEADO', debeCambiarPassword: false }

export const lucia: Beneficiario = { id: 3, nombre: 'Lucía', apellidos: 'Pérez', fechaNacimiento: '2018-05-20' }

/** Cita confirmada que aún se puede cancelar; se cambia lo necesario en cada prueba. */
export function citaDe(cambios: Partial<Cita> = {}): Cita {
  return {
    id: 40,
    servicio: { id: 1, nombre: 'Corte de pelo', duracionMinutos: 30 },
    profesional: { id: 10, nombre: 'Ana M.' },
    beneficiario: null,
    inicio: '2026-11-03T10:00:00+01:00',
    fin: '2026-11-03T10:30:00+01:00',
    precio: 15,
    estado: 'CONFIRMADA',
    motivoCancelacion: null,
    puedeCancelar: true,
    cancelableHasta: '2026-11-02T10:00:00+01:00',
    ...cambios,
  }
}

export function paginaDe<T>(contenido: T[], pagina = 0, totalPaginas = 1): Pagina<T> {
  return { contenido, pagina, tamano: 20, totalElementos: contenido.length, totalPaginas }
}

export const perfilCliente: Perfil = {
  id: 5,
  rol: 'CLIENTE',
  nombre: 'Marta',
  apellidos: 'Pérez Gil',
  email: 'marta@ejemplo.es',
  telefono: '600000000',
  fechaNacimiento: '1990-04-12',
  debeCambiarPassword: false,
}

/** Cita vista por el empleado: con el nombre y el teléfono del cliente. */
export function citaAsignada(cambios: Partial<Cita> = {}): Cita {
  return citaDe({ cliente: { id: 5, nombre: 'Marta', apellidos: 'Pérez Gil', telefono: '600 000 000' }, ...cambios })
}

/** Días de desde a hasta: de lunes a viernes de 09:00 a 14:00 y de 16:00 a 20:00; sábado y domingo libres. */
export function agendaVacia(desde: string, hasta: string): AgendaDia[] {
  const dias: AgendaDia[] = []
  for (let fecha = desde, i = 0; fecha <= hasta; fecha = sumarDias(fecha, 1), i++) {
    const laborable = i % 7 < 5
    dias.push({
      fecha,
      cierre: false,
      tramos: laborable ? [{ inicio: '09:00', fin: '14:00' }, { inicio: '16:00', fin: '20:00' }] : [],
      citas: [],
    })
  }
  return dias
}
