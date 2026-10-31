import type { Rol, UsuarioResumen } from '../api/tipos'

/** Estado que se pasa al login para volver después a la página de origen. */
export interface EstadoLogin {
  desde?: string
  aviso?: 'caducada' | 'registrado'
  email?: string
}

/** Página a la que va cada usuario al iniciar sesión. B9 cambiará la del empleado y el admin. */
export function rutaInicialDe(usuario: UsuarioResumen): string {
  if (usuario.debeCambiarPassword) return '/cambiar-password'
  const porRol: Record<Rol, string> = {
    CLIENTE: '/mis-citas',
    EMPLEADO: '/mi-area',
    ADMIN: '/mi-area',
  }
  return porRol[usuario.rol]
}

/** Solo se vuelve a rutas internas de la web (nunca a otra dirección). */
export function destinoSeguro(desde: string | undefined): string | null {
  if (!desde || !desde.startsWith('/') || desde.startsWith('//')) return null
  if (desde.startsWith('/login') || desde.startsWith('/registro')) return null
  return desde
}

export const NOMBRE_ROL: Record<Rol, string> = {
  CLIENTE: 'Cliente',
  EMPLEADO: 'Empleado',
  ADMIN: 'Administrador',
}
