// Menores a cargo del cliente (CU-009, RN-007).

import { queryOptions } from '@tanstack/react-query'

import { pedir } from './cliente'
import type { Beneficiario, DatosBeneficiario } from './tipos'

export const consultaBeneficiarios = queryOptions({
  queryKey: ['beneficiarios'],
  queryFn: ({ signal }) => pedir<Beneficiario[]>('/me/beneficiarios', { signal }),
})

export function crearBeneficiario(datos: DatosBeneficiario): Promise<Beneficiario> {
  return pedir<Beneficiario>('/me/beneficiarios', { metodo: 'POST', cuerpo: datos })
}

export function corregirBeneficiario(id: number, datos: DatosBeneficiario): Promise<Beneficiario> {
  return pedir<Beneficiario>(`/me/beneficiarios/${id}`, { metodo: 'PUT', cuerpo: datos })
}

/** Si tiene citas se anonimiza y se cancelan sus citas futuras (lo hace el servidor). */
export function eliminarBeneficiario(id: number): Promise<void> {
  return pedir<void>(`/me/beneficiarios/${id}`, { metodo: 'DELETE' })
}
