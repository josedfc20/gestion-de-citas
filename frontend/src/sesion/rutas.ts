import type { Rol, UsuarioResumen } from '../api/tipos'

/** Estado que se pasa al login para volver después a la página de origen. */
export interface EstadoLogin {
  desde?: string
  aviso?: 'caducada' | 'registrado' | 'restablecida'
  email?: string
}

/** Página a la que va cada usuario al iniciar sesión. */
export function rutaInicialDe(usuario: UsuarioResumen): string {
  if (usuario.debeCambiarPassword) return '/cambiar-password'
  const porRol: Record<Rol, string> = {
    CLIENTE: '/mis-citas',
    EMPLEADO: '/empleado/agenda',
    ADMIN: '/admin/reservas',
  }
  return porRol[usuario.rol]
}

/** Solo se vuelve a rutas internas de la web (nunca a otra dirección). */
export function destinoSeguro(desde: string | undefined): string | null {
  if (!desde || !desde.startsWith('/') || desde.startsWith('//')) return null
  if (['/login', '/registro', '/recuperar-password', '/restablecer-password'].some((ruta) => desde.startsWith(ruta))) return null
  return desde
}

export const NOMBRE_ROL: Record<Rol, string> = {
  CLIENTE: 'Cliente',
  EMPLEADO: 'Empleado',
  ADMIN: 'Administrador',
}
