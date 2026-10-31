// Reservas del cliente (CU-008 a CU-012).

import { infiniteQueryOptions, queryOptions, type QueryClient } from '@tanstack/react-query'

import { pedir } from './cliente'
import type { Cita, DatosReserva, Pagina } from './tipos'

const TAMANO_PAGINA = 20

/** CU-010: próximas (en todos los estados) o historial, de 20 en 20. */
export function consultaMisCitas(pasadas: boolean) {
  return infiniteQueryOptions({
    queryKey: ['mis-citas', pasadas ? 'historial' : 'proximas'],
    queryFn: ({ pageParam, signal }) =>
      pedir<Pagina<Cita>>('/me/citas', { parametros: { pasadas, page: pageParam, size: TAMANO_PAGINA }, signal }),
    initialPageParam: 0,
    getNextPageParam: (ultima) => (ultima.pagina + 1 < ultima.totalPaginas ? ultima.pagina + 1 : undefined),
  })
}

/** CU-011. */
export function consultaCita(id: number) {
  return queryOptions({
    queryKey: ['cita', id],
    queryFn: ({ signal }) => pedir<Cita>(`/citas/${id}`, { signal }),
  })
}

/** CU-008 y CU-009. El fin y el precio los calcula el servidor. */
export function reservar(datos: DatosReserva): Promise<Cita> {
  return pedir<Cita>('/citas', { metodo: 'POST', cuerpo: datos })
}

/** CU-012. Solo si faltan 24 h o más (RN-004); si no, 422. */
export function cancelar(id: number, motivo: string): Promise<Cita> {
  return pedir<Cita>(`/citas/${id}/cancelacion`, { metodo: 'POST', cuerpo: { motivo: motivo.trim() || null } })
}

/** Tras reservar o cancelar cambian las listas y las horas libres. */
export function invalidarTrasCambioDeCita(queryClient: QueryClient): Promise<void> {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['mis-citas'] }),
    queryClient.invalidateQueries({ queryKey: ['disponibilidad'] }),
  ]).then(() => undefined)
}
