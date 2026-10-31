import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'

import * as auth from '../api/auth'
import { escucharSesion, renovarSesion } from '../api/cliente'
import type { DatosCambioPassword, UsuarioResumen } from '../api/tipos'
import { ContextoSesion, type MotivoFinSesion, type ValorSesion } from './contexto'

/**
 * Guarda quién ha iniciado sesión. Al abrir la web intenta recuperar la sesión
 * con la cookie del refresh token (si sigue viva, el usuario entra sin escribir
 * la contraseña). El token nunca pasa por aquí: vive en el cliente de la API.
 */
export function SesionProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [cargando, setCargando] = useState(true)
  const [usuario, setUsuario] = useState<UsuarioResumen | null>(null)
  const [motivoFin, setMotivoFin] = useState<MotivoFinSesion>(null)

  useEffect(() => {
    let activo = true
    // En desarrollo, React ejecuta este efecto dos veces: renovarSesion()
    // comparte la misma petición, así que solo sale un refresh.
    renovarSesion()
      .catch(() => null)
      .then((sesion) => {
        if (!activo) return
        setUsuario(sesion?.usuario ?? null)
        setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [])

  const usuarioActual = useRef(usuario)
  useEffect(() => {
    usuarioActual.current = usuario
  }, [usuario])

  // El cliente de la API renueva o pierde la sesión por su cuenta en mitad de una petición.
  useEffect(
    () =>
      escucharSesion((sesion) => {
        if (sesion) {
          setUsuario(sesion.usuario)
          return
        }
        if (usuarioActual.current) {
          setMotivoFin('caducada')
          queryClient.clear()
        }
        setUsuario(null)
      }),
    [queryClient],
  )

  const iniciarSesion = useCallback(
    async (email: string, password: string) => {
      const sesion = await auth.login(email, password)
      queryClient.clear()
      setMotivoFin(null)
      setUsuario(sesion.usuario)
      return sesion.usuario
    },
    [queryClient],
  )

  const cerrarSesion = useCallback(async () => {
    try {
      await auth.logout()
    } catch {
      // Aunque el servidor no responda, en esta pestaña la sesión se cierra.
    }
    queryClient.clear()
    setMotivoFin(null)
    setUsuario(null)
  }, [queryClient])

  const cambiarPassword = useCallback(async (datos: DatosCambioPassword) => {
    const sesion = await auth.cambiarPassword(datos)
    setUsuario(sesion.usuario)
    return sesion.usuario
  }, [])

  const valor = useMemo<ValorSesion>(
    () => ({ cargando, usuario, motivoFin, iniciarSesion, cerrarSesion, cambiarPassword }),
    [cargando, usuario, motivoFin, iniciarSesion, cerrarSesion, cambiarPassword],
  )

  return <ContextoSesion.Provider value={valor}>{children}</ContextoSesion.Provider>
}
