import { screen } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { problema } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, servidor } from '../pruebas/servidor'

async function rellenar(usuario: UserEvent, cambios: Partial<Record<string, string>> = {}) {
  const datos = {
    Nombre: 'Marta',
    Apellidos: 'López Ruiz',
    'Correo electrónico': 'marta@ejemplo.com',
    'Fecha de nacimiento': '1990-05-20',
    Contraseña: 'una frase larga',
    'Repite la contraseña': 'una frase larga',
    ...cambios,
  }
  for (const [etiqueta, valor] of Object.entries(datos)) {
    if (valor) await usuario.type(await screen.findByLabelText(etiqueta), valor)
  }
}

describe('registro (CU-001)', () => {
  it('crea la cuenta y lleva al login con el aviso y el correo puesto', async () => {
    let cuerpo: Record<string, unknown> = {}
    servidor.use(
      http.post(`${API}/auth/registro`, async ({ request }) => {
        cuerpo = (await request.json()) as Record<string, unknown>
        return HttpResponse.json({ id: 9, nombre: 'Marta', rol: 'CLIENTE', debeCambiarPassword: false }, { status: 201 })
      }),
    )
    const { usuario, router } = renderizarApp('/registro')

    await rellenar(usuario)
    await usuario.click(screen.getByLabelText(/He leído y acepto/))
    await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('Cuenta creada. Ya puedes iniciar sesión.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(screen.getByLabelText('Correo electrónico')).toHaveValue('marta@ejemplo.com')
    expect(cuerpo).toMatchObject({ nombre: 'Marta', telefono: null, aceptaPrivacidad: true, fechaNacimiento: '1990-05-20' })
  })

  it('valida en el navegador: contraseñas distintas y privacidad sin aceptar', async () => {
    const { usuario } = renderizarApp('/registro')

    await rellenar(usuario, { 'Repite la contraseña': 'otra frase distinta' })
    await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('Las contraseñas no coinciden.')).toBeInTheDocument()
    expect(screen.getByText('Debes aceptar la política de privacidad.')).toBeInTheDocument()
  })

  it('avisa en el navegador si no tiene la edad mínima del negocio', async () => {
    const { usuario } = renderizarApp('/registro')
    const haceDiezAnios = `${new Date().getFullYear() - 10}-01-01`

    await rellenar(usuario, { 'Fecha de nacimiento': haceDiezAnios })
    await usuario.click(screen.getByLabelText(/He leído y acepto/))
    await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('Debes tener 14 años o más para crear una cuenta.')).toBeInTheDocument()
  })

  it('un correo ya registrado (409) se marca en el campo del correo', async () => {
    servidor.use(
      http.post(`${API}/auth/registro`, () => HttpResponse.json(problema(409, 'Ya existe una cuenta con ese correo.'), { status: 409 })),
    )
    const { usuario } = renderizarApp('/registro')

    await rellenar(usuario)
    await usuario.click(screen.getByLabelText(/He leído y acepto/))
    await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('Ya existe una cuenta con ese correo.')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo electrónico')).toHaveAttribute('aria-invalid', 'true')
  })

  it('la edad mínima del servidor (422) sale en un aviso', async () => {
    servidor.use(
      http.post(`${API}/auth/registro`, () => HttpResponse.json(problema(422, 'Debes tener al menos 14 años.'), { status: 422 })),
    )
    const { usuario } = renderizarApp('/registro')

    await rellenar(usuario)
    await usuario.click(screen.getByLabelText(/He leído y acepto/))
    await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Debes tener al menos 14 años.')
  })

  it('los errores 400 del servidor salen debajo de su campo (contraseña muy común)', async () => {
    servidor.use(
      http.post(`${API}/auth/registro`, () =>
        HttpResponse.json(
          problema(400, 'Hay datos con formato incorrecto.', { errores: [{ campo: 'password', mensaje: 'Es una contraseña demasiado común.' }] }),
          { status: 400 },
        ),
      ),
    )
    const { usuario } = renderizarApp('/registro')

    await rellenar(usuario)
    await usuario.click(screen.getByLabelText(/He leído y acepto/))
    await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('Es una contraseña demasiado común.')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
