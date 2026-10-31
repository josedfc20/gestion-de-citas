import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { cliente, problema, sesionDe } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, servidor } from '../pruebas/servidor'

describe('login (CU-002)', () => {
  it('con credenciales correctas vuelve a la página de la que venía', async () => {
    let cuerpo: unknown = null
    servidor.use(
      http.post(`${API}/auth/login`, async ({ request }) => {
        cuerpo = await request.json()
        return HttpResponse.json(sesionDe(cliente))
      }),
    )
    const { usuario, router } = renderizarApp('/mi-area')

    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'marta@demo.invalid')
    await usuario.type(screen.getByLabelText('Contraseña'), 'una frase larga')
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    expect(await screen.findByRole('heading', { name: 'Hola, Marta' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/mi-area')
    expect(cuerpo).toEqual({ email: 'marta@demo.invalid', password: 'una frase larga' })
  })

  it('valida en el navegador antes de enviar', async () => {
    const { usuario } = renderizarApp('/login')

    await usuario.click(await screen.findByRole('button', { name: 'Iniciar sesión' }))

    expect(await screen.findByText('Escribe tu correo electrónico.')).toBeInTheDocument()
    expect(screen.getByText('Escribe tu contraseña.')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo electrónico')).toHaveFocus()
  })

  it('con credenciales incorrectas (401) muestra un mensaje sin decir cuál falla', async () => {
    servidor.use(http.post(`${API}/auth/login`, () => HttpResponse.json(problema(401, 'x'), { status: 401 })))
    const { usuario } = renderizarApp('/login')

    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'marta@demo.invalid')
    await usuario.type(screen.getByLabelText('Contraseña'), 'mala')
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña no válidos.')
  })

  it('con demasiados intentos (429) pide esperar', async () => {
    servidor.use(http.post(`${API}/auth/login`, () => HttpResponse.json(problema(429, 'x'), { status: 429 })))
    const { usuario } = renderizarApp('/login')

    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'marta@demo.invalid')
    await usuario.type(screen.getByLabelText('Contraseña'), 'mala')
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Demasiados intentos')
  })

  it('una cuenta desactivada (403) muestra el motivo del servidor', async () => {
    servidor.use(http.post(`${API}/auth/login`, () => HttpResponse.json(problema(403, 'La cuenta está desactivada.'), { status: 403 })))
    const { usuario } = renderizarApp('/login')

    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'luis@demo.invalid')
    await usuario.type(screen.getByLabelText('Contraseña'), 'una frase larga')
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La cuenta está desactivada.')
  })
})
