import { Navigate, useLocation } from 'react-router'

import { useSesion } from '../sesion/contexto'
import { rutaInicialDe } from '../sesion/rutas'

/**
 * `/mi-area` fue la página provisional del empleado y el admin (B7). Desde el
 * B9 solo lleva a la página de inicio de cada rol, para no romper enlaces.
 */
export function MiArea() {
  const { usuario } = useSesion()
  const passwordCambiada = (useLocation().state as { passwordCambiada?: boolean } | null)?.passwordCambiada
  if (!usuario) return null
  return <Navigate to={rutaInicialDe(usuario)} replace state={passwordCambiada ? { passwordCambiada } : undefined} />
}
