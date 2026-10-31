// Gestión de citas del empleado y el admin (CU-015, CU-019).

import type { QueryClient } from '@tanstack/react-query'

import { pedir } from './cliente'
import type { Cita, DatosCambioEstado } from './tipos'

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
