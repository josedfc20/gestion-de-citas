import { createContext, useContext } from 'react'

import type { DatosCambioPassword, UsuarioResumen } from '../api/tipos'

export type MotivoFinSesion = 'caducada' | null

export interface ValorSesion {
  /** true mientras se intenta recuperar la sesión con la cookie al abrir la web. */
  cargando: boolean
  usuario: UsuarioResumen | null
  /** 'caducada' si la sesión se perdió sin que el usuario la cerrara. */
  motivoFin: MotivoFinSesion
  iniciarSesion: (email: string, password: string) => Promise<UsuarioResumen>
  cerrarSesion: () => Promise<void>
  cambiarPassword: (datos: DatosCambioPassword) => Promise<UsuarioResumen>
  /** Tras guardar el perfil: el nombre de la cabecera cambia sin pedir otra sesión. */
  actualizarUsuario: (cambios: Partial<Pick<UsuarioResumen, 'nombre'>>) => void
  /**
   * Borra la sesión solo en la web, cuando el servidor ya la ha cerrado
   * (cuenta eliminada o contraseña restablecida): no hace falta pedir el logout.
   */
  cerrarSesionLocal: () => void
}

export const ContextoSesion = createContext<ValorSesion | null>(null)

export function useSesion(): ValorSesion {
  const valor = useContext(ContextoSesion)
  if (!valor) throw new Error('useSesion debe usarse dentro de <SesionProvider>')
  return valor
}
