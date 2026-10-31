// Gestión de citas del empleado y el admin (CU-015, CU-019).

import { infiniteQueryOptions, queryOptions, type QueryClient } from '@tanstack/react-query'

import { pedir } from './cliente'
import type { Cita, ClienteEncontrado, DatosCambioEstado, DatosReservaAdmin, EstadoCita, Pagina } from './tipos'

/**
 * Completada y No presentado, desde la hora de inicio; Cancelada, siempre y con
 * motivo. Las reglas las decide el servidor (422).
 */
export function cambiarEstado(id: number, datos: DatosCambioEstado): Promise<Cita> {
  const motivo = datos.motivo?.trim()
  return pedir<Cita>(`/citas/${id}/estado`, { metodo: 'PATCH', cuerpo: { estado: datos.estado, motivo: motivo || null } })
}

/** Tras cambiar el estado cambian la agenda, las listas y (al cancelar) las horas libres. */
export function invalidarTrasGestion(queryClient: QueryClient): Promise<void> {
  return Promise.all(
    ['agenda', 'citas-asignadas', 'reservas', 'mis-citas', 'disponibilidad'].map((clave) => queryClient.invalidateQueries({ queryKey: [clave] })),
  ).then(() => undefined)
}

const TAMANO_PAGINA = 20

export interface FiltrosReservas {
  estado?: EstadoCita
  desde?: string
  hasta?: string
  profesionalId?: number
  clienteId?: number
}

/** CU-019: todas las reservas con filtros, de 20 en 20. Sin `desde`, de hoy en adelante. */
export function consultaReservas(filtros: FiltrosReservas) {
  return infiniteQueryOptions({
    queryKey: ['reservas', filtros],
    queryFn: ({ pageParam, signal }) =>
      pedir<Pagina<Cita>>('/admin/citas', { parametros: { ...filtros, page: pageParam, size: TAMANO_PAGINA }, signal }),
    initialPageParam: 0,
    getNextPageParam: (ultima) => (ultima.pagina + 1 < ultima.totalPaginas ? ultima.pagina + 1 : undefined),
  })
}

/** Busca clientes por nombre, apellidos, correo o teléfono (3 letras o más; como mucho 20). */
export function consultaClientes(buscar: string) {
  return queryOptions({
    queryKey: ['clientes', buscar],
    queryFn: ({ signal }) => pedir<ClienteEncontrado[]>('/admin/clientes', { parametros: { buscar }, signal }),
    enabled: buscar.length >= 3,
  })
}

/** CU-019, alt. 1: mismas reglas y misma comprobación del hueco que POST /citas. */
export function reservarComoAdmin(datos: DatosReservaAdmin): Promise<Cita> {
  return pedir<Cita>('/admin/citas', { metodo: 'POST', cuerpo: datos })
}
