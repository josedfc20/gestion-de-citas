import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'

import type { Rol } from '../api/tipos'
import { Cargando } from '../componentes/Cargando'
import { SinAcceso } from '../paginas/SinAcceso'
import { useSesion } from './contexto'
import { destinoSeguro, rutaInicialDe, type EstadoLogin } from './rutas'

interface Props {
  /** Roles que pueden entrar; sin indicar, cualquiera con sesión. */
  roles?: Rol[]
  children: ReactNode
}

/** Sin sesión → login (y vuelta aquí después); con otro rol → "No tienes acceso". */
export function RutaProtegida({ roles, children }: Props) {
  const { cargando, usuario, motivoFin } = useSesion()
  const location = useLocation()

  if (cargando) return <Cargando />
  if (!usuario) {
    const estado: EstadoLogin = {
      desde: location.pathname + location.search,
      aviso: motivoFin === 'caducada' ? 'caducada' : undefined,
    }
    return <Navigate to="/login" replace state={estado} />
  }
  if (roles && !roles.includes(usuario.rol)) return <SinAcceso />
  return children
}

/**
 * Login y registro: con sesión ya iniciada no tienen sentido. Al iniciar sesión
 * es este componente el que lleva al usuario a la página de la que venía (o a
 * la de su rol), así no compiten dos redirecciones.
 */
export function SoloSinSesion({ children }: { children: ReactNode }) {
  const { cargando, usuario } = useSesion()
  const location = useLocation()
  if (cargando) return <Cargando />
  if (usuario) {
    const desde = destinoSeguro((location.state as EstadoLogin | null)?.desde)
    const destino = usuario.debeCambiarPassword ? '/cambiar-password' : (desde ?? rutaInicialDe(usuario))
    return <Navigate to={destino} replace />
  }
  return children
}
