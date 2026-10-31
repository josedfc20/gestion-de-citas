// Perfil y datos personales del usuario (CU-004, RNF-007).

import { queryOptions } from '@tanstack/react-query'

import { pedir, pedirArchivo } from './cliente'
import type { DatosPerfil, Perfil } from './tipos'

export const consultaPerfil = queryOptions({
  queryKey: ['perfil'],
  queryFn: ({ signal }) => pedir<Perfil>('/me', { signal }),
})

export function guardarPerfil(datos: DatosPerfil): Promise<Perfil> {
  return pedir<Perfil>('/me', { metodo: 'PUT', cuerpo: datos })
}

/** RGPD: todos sus datos en JSON (sin contraseñas ni tokens). */
export function exportarDatos(): Promise<Blob> {
  return pedirArchivo('/me/datos')
}

/** Solo clientes. Anonimiza sus datos y los de sus menores y cancela sus citas futuras. */
export function eliminarCuenta(password: string): Promise<void> {
  return pedir<void>('/me', { metodo: 'DELETE', cuerpo: { password } })
}
