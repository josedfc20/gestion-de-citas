// Consultas públicas: negocio, servicios, profesionales y horas libres (CU-005 a CU-007).

import { queryOptions } from '@tanstack/react-query'

import { pedir } from './cliente'
import type { Disponibilidad, Negocio, Profesional, Servicio } from './tipos'

const CINCO_MINUTOS = 5 * 60 * 1000

export const consultaNegocio = queryOptions({
  queryKey: ['negocio'],
  queryFn: ({ signal }) => pedir<Negocio>('/negocio', { signal }),
  staleTime: CINCO_MINUTOS,
})

export const consultaServicios = queryOptions({
  queryKey: ['servicios'],
  queryFn: ({ signal }) => pedir<Servicio[]>('/servicios', { signal }),
  staleTime: CINCO_MINUTOS,
})

export function consultaProfesionales(servicioId?: number) {
  return queryOptions({
    queryKey: ['profesionales', servicioId ?? 'todos'],
    queryFn: ({ signal }) => pedir<Profesional[]>('/profesionales', { parametros: { servicioId }, signal }),
    staleTime: CINCO_MINUTOS,
  })
}

/** Las horas libres cambian con cada reserva: sin caché larga. */
export function consultaDisponibilidad(servicioId: number, profesionalId: number, fecha: string) {
  return queryOptions({
    queryKey: ['disponibilidad', servicioId, profesionalId, fecha],
    queryFn: ({ signal }) =>
      pedir<Disponibilidad>('/disponibilidad', { parametros: { servicioId, profesionalId, fecha }, signal }),
    staleTime: 0,
  })
}
