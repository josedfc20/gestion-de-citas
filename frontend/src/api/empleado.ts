// Zona del empleado (CU-013, CU-014, CU-021). El empleado sale del token.

import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query'

import { pedir } from './cliente'
import type { AgendaDia, Cita, EstadoCita, Pagina, Servicio } from './tipos'

const TAMANO_PAGINA = 20

/** CU-013: por día, su horario efectivo y sus citas no canceladas (como mucho 31 días). */
export function consultaAgenda(desde: string, hasta: string) {
  return queryOptions({
    queryKey: ['agenda', desde, hasta],
    queryFn: ({ signal }) => pedir<AgendaDia[]>('/empleado/agenda', { parametros: { desde, hasta }, signal }),
  })
}

export interface FiltrosCitas {
  estado?: EstadoCita
  desde?: string
  hasta?: string
}

/** CU-014: sus citas asignadas con filtros, de 20 en 20. Sin `desde`, de hoy en adelante. */
export function consultaCitasAsignadas(filtros: FiltrosCitas) {
  return infiniteQueryOptions({
    queryKey: ['citas-asignadas', filtros],
    queryFn: ({ pageParam, signal }) =>
      pedir<Pagina<Cita>>('/empleado/citas', { parametros: { ...filtros, page: pageParam, size: TAMANO_PAGINA }, signal }),
    initialPageParam: 0,
    getNextPageParam: (ultima) => (ultima.pagina + 1 < ultima.totalPaginas ? ultima.pagina + 1 : undefined),
  })
}

/** CU-021. */
export const consultaMisServicios = queryOptions({
  queryKey: ['mis-servicios'],
  queryFn: ({ signal }) => pedir<Servicio[]>('/empleado/servicios', { signal }),
})
