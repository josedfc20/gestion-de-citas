import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import type { Rol, UsuarioResumen } from '../api/tipos'
import { rutas } from '../rutas'
import { administrador, cliente, empleado, empleadoTemporal, problema, sesionDe } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, manejadoresPorDefecto, servidor } from '../pruebas/servidor'
import { rutaInicialDe } from '../sesion/rutas'

/**
 * Matriz de rutas de la web (RNF-002, paso 5b): cada página con cada perfil.
 * Solo comprueba a dónde llega cada uno (la página, el login, "No tienes acceso",
 * su página de inicio o el cambio de contraseña); lo que muestra cada página ya
 * lo prueban sus propios archivos. La seguridad de verdad la pone el backend
 * (PermisosTest); esto evita que una página privada quede abierta en la web.
 */

type Acceso =
  /** Cualquiera, con sesión o sin ella. */
  | { tipo: 'publica' }
  /** Solo sin sesión (login, registro…); con sesión lleva a la página de inicio de su rol. */
  | { tipo: 'sinSesion' }
  /** Cualquier usuario con sesión. */
  | { tipo: 'sesion' }
  | { tipo: 'roles'; roles: Rol[] }
  /** /mi-area: solo lleva a la página de inicio de cada rol. */
  | { tipo: 'inicioDelRol' }

const PUBLICA: Acceso = { tipo: 'publica' }
const SIN_SESION: Acceso = { tipo: 'sinSesion' }
const SESION: Acceso = { tipo: 'sesion' }
const solo = (...roles: Rol[]): Acceso => ({ tipo: 'roles', roles })

/** Todas las rutas de rutas.tsx, con un ejemplo de dirección para las que llevan parámetros. */
const RUTAS: { ruta: string; ejemplo?: string; acceso: Acceso }[] = [
  { ruta: '/', acceso: PUBLICA },
  { ruta: '/servicios', acceso: PUBLICA },
  { ruta: '/profesionales', acceso: PUBLICA },
  { ruta: '/horarios', acceso: PUBLICA },
  { ruta: '/privacidad', acceso: PUBLICA },
  { ruta: '/restablecer-password', acceso: PUBLICA },
  { ruta: '/*', ejemplo: '/no-existe', acceso: PUBLICA },
  { ruta: '/login', acceso: SIN_SESION },
  { ruta: '/registro', acceso: SIN_SESION },
  { ruta: '/recuperar-password', acceso: SIN_SESION },
  { ruta: '/cambiar-password', acceso: SESION },
  { ruta: '/perfil', acceso: SESION },
  { ruta: '/mi-area', acceso: { tipo: 'inicioDelRol' } },
  { ruta: '/reservar', acceso: solo('CLIENTE') },
  { ruta: '/mis-citas', acceso: solo('CLIENTE') },
  { ruta: '/mis-citas/:id', ejemplo: '/mis-citas/40', acceso: solo('CLIENTE') },
  { ruta: '/menores', acceso: solo('CLIENTE') },
  { ruta: '/empleado/agenda', acceso: solo('EMPLEADO') },
  { ruta: '/empleado/citas', acceso: solo('EMPLEADO') },
  { ruta: '/empleado/citas/:id', ejemplo: '/empleado/citas/40', acceso: solo('EMPLEADO') },
  { ruta: '/empleado/servicios', acceso: solo('EMPLEADO') },
  { ruta: '/admin/reservas', acceso: solo('ADMIN') },
  { ruta: '/admin/reservas/nueva', acceso: solo('ADMIN') },
  { ruta: '/admin/reservas/:id', ejemplo: '/admin/reservas/40', acceso: solo('ADMIN') },
  { ruta: '/admin/servicios', acceso: solo('ADMIN') },
  { ruta: '/admin/servicios/nuevo', acceso: solo('ADMIN') },
  { ruta: '/admin/servicios/:id', ejemplo: '/admin/servicios/1', acceso: solo('ADMIN') },
  { ruta: '/admin/empleados', acceso: solo('ADMIN') },
  { ruta: '/admin/empleados/nuevo', acceso: solo('ADMIN') },
  { ruta: '/admin/empleados/:id', ejemplo: '/admin/empleados/11', acceso: solo('ADMIN') },
  { ruta: '/admin/empleados/:id/horario', ejemplo: '/admin/empleados/11/horario', acceso: solo('ADMIN') },
  { ruta: '/admin/horarios', acceso: solo('ADMIN') },
  { ruta: '/admin/negocio', acceso: solo('ADMIN') },
]

const PERFILES: { nombre: string; usuario: UsuarioResumen | null }[] = [
  { nombre: 'visitante', usuario: null },
  { nombre: 'cliente', usuario: cliente },
  { nombre: 'empleado', usuario: empleado },
  { nombre: 'empleado con contraseña temporal', usuario: empleadoTemporal },
  { nombre: 'admin', usuario: administrador },
]

/** Lo que debe ver cada perfil: la dirección final y si sale "No tienes acceso". */
function esperado(acceso: Acceso, ruta: string, usuario: UsuarioResumen | null): { ruta: string; sinAcceso: boolean } {
  // RN-005: con la contraseña temporal, toda página lleva a cambiarla.
  if (usuario?.debeCambiarPassword) return { ruta: '/cambiar-password', sinAcceso: false }
  switch (acceso.tipo) {
    case 'publica':
      return { ruta, sinAcceso: false }
    case 'sinSesion':
      return { ruta: usuario ? rutaInicialDe(usuario) : ruta, sinAcceso: false }
    case 'sesion':
      return { ruta: usuario ? ruta : '/login', sinAcceso: false }
    case 'inicioDelRol':
      return { ruta: usuario ? rutaInicialDe(usuario) : '/login', sinAcceso: false }
    case 'roles':
      if (!usuario) return { ruta: '/login', sinAcceso: false }
      return { ruta, sinAcceso: !acceso.roles.includes(usuario.rol) }
  }
}

/** Con sesión, la cookie de refresh devuelve ese usuario; el resto de la API responde 404 salvo lo que ya simula servidor.ts. */
function conPerfil(usuario: UsuarioResumen | null) {
  servidor.use(
    http.post(`${API}/auth/refresh`, () =>
      usuario
        ? HttpResponse.json(sesionDe(usuario))
        : HttpResponse.json(problema(401, 'Sesión no válida.'), { status: 401 })),
    // Los de servidor.ts otra vez, para que vayan antes que el 404 de abajo (el refresh de arriba manda).
    ...manejadoresPorDefecto,
    http.all(`${API}/*`, () => HttpResponse.json(problema(404, 'No existe.'), { status: 404 })),
  )
}

const casos = RUTAS.flatMap(({ ruta, ejemplo, acceso }) =>
  PERFILES.map((perfil) => ({ ruta, direccion: ejemplo ?? ruta, acceso, ...perfil })))

describe('matriz de rutas (RNF-002)', () => {
  it.each(casos)('$direccion con $nombre', async ({ direccion, acceso, usuario }) => {
    conPerfil(usuario)
    const { router } = renderizarApp(direccion)
    const objetivo = esperado(acceso, direccion, usuario)

    await waitFor(() => expect(router.state.location.pathname).toBe(objetivo.ruta))
    if (usuario) await screen.findByRole('button', { name: 'Cerrar sesión' })

    // Con sesión, "No tienes acceso" sale en el mismo render que la cabecera con "Cerrar sesión".
    const sinAcceso = screen.queryByRole('heading', { name: 'No tienes acceso a esta página' })
    expect(sinAcceso !== null).toBe(objetivo.sinAcceso)
    expect(router.state.location.pathname).toBe(objetivo.ruta)
  })

  it('la tabla tiene todas las rutas de rutas.tsx', () => {
    const reales = (rutas[0].children ?? []).map((ruta) => (ruta.index ? '/' : `/${ruta.path}`)).sort()
    expect(RUTAS.map(({ ruta }) => ruta).sort()).toEqual(reales)
  })
})
