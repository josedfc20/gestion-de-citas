import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { tokenActual } from '../api/cliente'
import { cliente, empleadoTemporal, problema, sesionDe } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, servidor } from '../pruebas/servidor'

function conCookieDe(usuario = cliente) {
  servidor.use(http.post(`${API}/auth/refresh`, () => HttpResponse.json(sesionDe(usuario))))
}

describe('sesión', () => {
  it('al abrir la web con una cookie válida entra sin pedir la contraseña', async () => {
    conCookieDe(cliente)
    renderizarApp('/mis-citas')

    expect(await screen.findByRole('heading', { name: 'Mis citas', level: 1 })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument()
    expect(tokenActual()).toBe('token-1')
  })

  it('sin cookie es visitante y una ruta protegida lleva al login', async () => {
    const { router } = renderizarApp('/mi-area')

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(router.state.location.state).toMatchObject({ desde: '/mi-area' })
    expect(screen.getByRole('link', { name: 'Registrarse' })).toBeInTheDocument()
  })

  it('con contraseña temporal cualquier página lleva a cambiarla (RN-005)', async () => {
    conCookieDe(empleadoTemporal)
    const { router } = renderizarApp('/servicios')

    expect(await screen.findByText(/Estás usando una contraseña temporal/)).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/cambiar-password')
  })

  it('tras cambiar la contraseña temporal lleva a su área', async () => {
    conCookieDe(empleadoTemporal)
    servidor.use(
      http.put(`${API}/me/password`, () => HttpResponse.json(sesionDe({ ...empleadoTemporal, debeCambiarPassword: false }, 'token-2'))),
    )
    const { usuario, router } = renderizarApp('/mi-area')

    await usuario.type(await screen.findByLabelText('Contraseña temporal'), 'Temporal123')
    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'una frase larga')
    await usuario.type(screen.getByLabelText('Repite la contraseña nueva'), 'una frase larga')
    await usuario.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('Contraseña cambiada correctamente.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/empleado/agenda')
    expect(tokenActual()).toBe('token-2')
  })

  it('una contraseña temporal incorrecta se marca en su campo (422)', async () => {
    conCookieDe(empleadoTemporal)
    servidor.use(
      http.put(`${API}/me/password`, () => HttpResponse.json(problema(422, 'La contraseña actual no es correcta.'), { status: 422 })),
    )
    const { usuario } = renderizarApp('/cambiar-password')

    await usuario.type(await screen.findByLabelText('Contraseña temporal'), 'mal')
    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'una frase larga')
    await usuario.type(screen.getByLabelText('Repite la contraseña nueva'), 'una frase larga')
    await usuario.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('La contraseña actual no es correcta.')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña temporal')).toHaveAttribute('aria-invalid', 'true')
  })

  it('cerrar sesión revoca la cookie, borra el token y vuelve al inicio (CU-003)', async () => {
    conCookieDe(cliente)
    let logout = false
    servidor.use(
      http.post(`${API}/auth/logout`, () => {
        logout = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { usuario, router } = renderizarApp('/mi-area')

    await usuario.click(await screen.findByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('link', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(logout).toBe(true)
    expect(tokenActual()).toBeNull()
    expect(router.state.location.pathname).toBe('/')
  })

  it('si la sesión caduca en mitad del uso, lleva al login con un aviso', async () => {
    conCookieDe(cliente)
    const { router } = renderizarApp('/mis-citas')
    await screen.findByRole('heading', { name: 'Mis citas', level: 1 })

    // La cookie ya no vale y el token tampoco: cualquier petición da 401.
    servidor.use(
      http.post(`${API}/auth/refresh`, () => HttpResponse.json(problema(401, 'Sesión no válida.'), { status: 401 })),
      http.get(`${API}/servicios`, () => HttpResponse.json(problema(401, 'Token caducado'), { status: 401 })),
    )
    await router.navigate('/servicios')
    await screen.findByRole('heading', { name: 'Servicios y precios' })
    await router.navigate('/mis-citas')

    expect(await screen.findByText(/Tu sesión ha caducado/)).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
  })
})
