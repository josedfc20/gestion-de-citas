// Formatos en español para precios, duraciones y fechas.

import type { EstadoCita } from '../api/tipos'
import { partesDeInstante } from './fechas'

const euros = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' })

export function formatearPrecio(precio: number): string {
  return euros.format(precio)
}

/** 45 → "45 min"; 60 → "1 h"; 90 → "1 h 30 min". */
export function formatearDuracion(minutos: number): string {
  const horas = Math.floor(minutos / 60)
  const resto = minutos % 60
  if (horas === 0) return `${resto} min`
  return resto === 0 ? `${horas} h` : `${horas} h ${resto} min`
}

/** Convierte "2026-11-03" en una fecha sin que la zona horaria del navegador la mueva de día. */
function fechaLocal(isoFecha: string): Date {
  const [anio, mes, dia] = isoFecha.split('-').map(Number)
  return new Date(Date.UTC(anio, mes - 1, dia, 12))
}

/** "2026-11-03" → "martes, 3 de noviembre de 2026". */
export function formatearFechaLarga(isoFecha: string): string {
  return new Intl.DateTimeFormat('es-ES', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' }).format(
    fechaLocal(isoFecha),
  )
}

/** "2026-11-03" → "martes, 3 de noviembre" (sin el año, para las citas). */
export function formatearDia(isoFecha: string): string {
  return new Intl.DateTimeFormat('es-ES', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' }).format(fechaLocal(isoFecha))
}

/** "2026-11-03T10:00:00+01:00" → "martes, 3 de noviembre de 2026 a las 10:00" (hora del negocio). */
export function formatearInstante(instante: string, conAnio = true): string {
  const { fecha, hora } = partesDeInstante(instante)
  return `${conAnio ? formatearFechaLarga(fecha) : formatearDia(fecha)} a las ${hora}`
}

/** Para quién es una cita, visto por el empleado o el admin: "Marta Pérez" o "Lucía (menor), a cargo de Marta Pérez". */
export function personaDeLaCita(cita: { cliente?: { nombre: string; apellidos: string }; beneficiario: { nombre: string } | null }): string {
  const cliente = cita.cliente ? `${cita.cliente.nombre} ${cita.cliente.apellidos}` : ''
  if (!cita.beneficiario) return cliente
  return cliente ? `${cita.beneficiario.nombre} (menor), a cargo de ${cliente}` : `${cita.beneficiario.nombre} (menor)`
}

/** Tramos de un día: "09:00–14:00 · 16:00–20:00". */
export function formatearTramos(tramos: { inicio: string; fin: string }[]): string {
  return tramos.map((tramo) => `${tramo.inicio}–${tramo.fin}`).join(' · ')
}

/** Nombre de cada estado de una cita (RN-008). */
export const NOMBRE_ESTADO: Record<EstadoCita, string> = {
  CONFIRMADA: 'Confirmada',
  COMPLETADA: 'Completada',
  CANCELADA: 'Cancelada',
  NO_PRESENTADO: 'No presentado',
}

/** RN-011: duraciones posibles de un servicio, múltiplos del intervalo, hasta 8 horas. */
export function duracionesPosibles(intervaloMinutos: number, maximo = 480): number[] {
  const lista: number[] = []
  for (let minutos = intervaloMinutos; minutos <= maximo; minutos += intervaloMinutos) lista.push(minutos)
  return lista
}

export const NOMBRE_DIA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'] as const
