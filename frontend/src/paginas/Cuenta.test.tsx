import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, onTestFinished, vi } from 'vitest'

import { tokenActual } from '../api/cliente'
import type { Perfil } from '../api/tipos'
import { cliente, empleado, perfilCliente, problema, sesionDe } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, servidor } from '../pruebas/servidor'

function conSesion(usuario = cliente, perfil: Perfil = perfilCliente) {
  servidor.use(
    http.post(`${API}/auth/refresh`, () => HttpResponse.json(sesionDe(usuario))),
    http.get(`${API}/me`, () => HttpResponse.json(perfil)),
  )
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('perfil (CU-004)', () => {
  it('guarda los datos y el nombre de la cabecera cambia', async () => {
    conSesion()
    let recibido: unknown
    servidor.use(
      http.put(`${API}/me`, async ({ request }) => {
        recibido = await request.json()
        return HttpResponse.json({ ...perfilCliente, nombre: 'Marta Luisa', telefono: null })
      }),
    )
    const { usuario } = renderizarApp('/perfil')

    const nombre = await screen.findByLabelText('Nombre')
    expect(nombre).toHaveValue('Marta')
    expect(screen.getByText(/12 de abril de 1990/)).toBeInTheDocument()
    await usuario.clear(nombre)
    await usuario.type(nombre, 'Marta Luisa')
    await usuario.clear(screen.getByLabelText(/Teléfono/))
    await usuario.click(screen.getByRole('button', { name: 'Guardar datos' }))

    expect(await screen.findByText('Datos guardados.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Mi perfil (Marta Luisa)' })).toBeInTheDocument()
    expect(recibido).toEqual({ nombre: 'Marta Luisa', apellidos: 'Pérez Gil', email: 'marta@ejemplo.es', telefono: null })
  })

  it('un correo que ya usa otra cuenta (409) sale debajo del correo', async () => {
    conSesion()
    servidor.use(http.put(`${API}/me`, () => HttpResponse.json(problema(409, 'Ese correo ya lo usa otra cuenta.'), { status: 409 })))
    const { usuario } = renderizarApp('/perfil')

    await usuario.click(await screen.findByRole('button', { name: 'Guardar datos' }))

    expect(await screen.findByText('Ese correo ya lo usa otra cuenta.')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo electrónico')).toHaveAttribute('aria-invalid', 'true')
  })

  it('cambiar la contraseña desde el perfil vuelve al perfil', async () => {
    conSesion()
    servidor.use(http.put(`${API}/me/password`, () => HttpResponse.json(sesionDe(cliente, 'token-2'))))
    const { usuario, router } = renderizarApp('/perfil')

    await usuario.click(await screen.findByRole('link', { name: 'Cambiar contraseña' }))
    await usuario.type(await screen.findByLabelText('Contraseña actual'), 'la de antes 123')
    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'una frase larga')
    await usuario.type(screen.getByLabelText('Repite la contraseña nueva'), 'una frase larga')
    await usuario.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('Contraseña cambiada correctamente.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/perfil')
  })

  it('descarga mis datos en JSON con el token (RGPD)', async () => {
    conSesion()
    let autorizacion: string | null = null
    servidor.use(
      http.get(`${API}/me/datos`, ({ request }) => {
        autorizacion = request.headers.get('Authorization')
        return HttpResponse.json({ nombre: 'Marta' })
      }),
    )
    const crear = vi.fn<(archivo: Blob) => string>(() => 'blob:mis-datos')
    const original = { crear: URL.createObjectURL, revocar: URL.revokeObjectURL }
    URL.createObjectURL = crear
    URL.revokeObjectURL = vi.fn<(direccion: string) => void>()
    onTestFinished(() => {
      URL.createObjectURL = original.crear
      URL.revokeObjectURL = original.revocar
    })
    const pulsar = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const { usuario } = renderizarApp('/perfil')

    await usuario.click(await screen.findByRole('button', { name: 'Descargar mis datos' }))

    await vi.waitFor(() => expect(pulsar).toHaveBeenCalled())
    expect(await crear.mock.calls[0][0].text()).toBe('{"nombre":"Marta"}')
    expect((pulsar.mock.contexts[0] as HTMLAnchorElement).download).toBe('mis-datos.json')
    expect(autorizacion).toBe('Bearer token-1')
  })

  it('eliminar la cuenta: con la contraseña mal, error en el campo (422)', async () => {
    conSesion()
    servidor.use(http.delete(`${API}/me`, () => HttpResponse.json(problema(422, 'La contraseña no es correcta.'), { status: 422 })))
    const { usuario } = renderizarApp('/perfil')

    await usuario.click(await screen.findByRole('button', { name: 'Quiero eliminar mi cuenta' }))
    await usuario.type(screen.getByLabelText('Escribe tu contraseña para confirmar'), 'mal')
    await usuario.click(screen.getByRole('button', { name: 'Eliminar mi cuenta' }))

    expect(await screen.findByText('La contraseña no es correcta.')).toBeInTheDocument()
    expect(screen.getByLabelText('Escribe tu contraseña para confirmar')).toHaveAttribute('aria-invalid', 'true')
  })

  it('eliminar la cuenta: cierra la sesión y vuelve al inicio con un aviso', async () => {
    conSesion()
    let password: unknown
    servidor.use(
      http.delete(`${API}/me`, async ({ request }) => {
        password = ((await request.json()) as { password: string }).password
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { usuario, router } = renderizarApp('/perfil')

    await usuario.click(await screen.findByRole('button', { name: 'Quiero eliminar mi cuenta' }))
    await usuario.type(screen.getByLabelText('Escribe tu contraseña para confirmar'), 'mi contraseña')
    await usuario.click(screen.getByRole('button', { name: 'Eliminar mi cuenta' }))

    expect(await screen.findByText(/Tu cuenta se ha eliminado/)).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/')
    expect(await screen.findByRole('link', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(tokenActual()).toBeNull()
    expect(password).toBe('mi contraseña')
  })

  it('el empleado tiene perfil pero no puede eliminar su cuenta', async () => {
    conSesion(empleado, { ...perfilCliente, id: 7, rol: 'EMPLEADO', nombre: 'Ana', fechaNacimiento: undefined })
    renderizarApp('/perfil')

    expect(await screen.findByRole('heading', { name: 'Mis datos' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Eliminar mi cuenta' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Agenda' })).toBeInTheDocument()
  })
})

describe('recuperar la contraseña (CU-020)', () => {
  it('el login enlaza a recuperar la contraseña', async () => {
    const { usuario, router } = renderizarApp('/login')

    await usuario.click(await screen.findByRole('link', { name: '¿Has olvidado la contraseña?' }))

    expect(router.state.location.pathname).toBe('/recuperar-password')
  })

  it('siempre responde lo mismo, exista o no el correo', async () => {
    let recibido: unknown
    servidor.use(
      http.post(`${API}/auth/recuperar-password`, async ({ request }) => {
        recibido = await request.json()
        return HttpResponse.json({ mensaje: 'Si el correo está registrado...' }, { status: 202 })
      }),
    )
    const { usuario } = renderizarApp('/recuperar-password')

    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'nadie@ejemplo.es')
    await usuario.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(await screen.findByText(/Si el correo está registrado, recibirás un enlace/)).toBeInTheDocument()
    expect(recibido).toEqual({ email: 'nadie@ejemplo.es' })
  })

  it('demasiadas solicitudes (429)', async () => {
    servidor.use(
      http.post(`${API}/auth/recuperar-password`, () => HttpResponse.json(problema(429, 'Demasiadas solicitudes.'), { status: 429 })),
    )
    const { usuario } = renderizarApp('/recuperar-password')

    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'marta@ejemplo.es')
    await usuario.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Has pedido demasiados enlaces')
  })

  it('restablecer: quita el token de la dirección y lleva al login con un aviso', async () => {
    let recibido: unknown
    servidor.use(
      http.post(`${API}/auth/restablecer-password`, async ({ request }) => {
        recibido = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { usuario, router } = renderizarApp('/restablecer-password#token=abc123')

    await screen.findByRole('heading', { name: 'Elegir contraseña nueva' })
    expect(router.state.location.hash).toBe('')
    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'una frase larga')
    await usuario.type(screen.getByLabelText('Repite la contraseña nueva'), 'una frase larga')
    await usuario.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('Contraseña cambiada. Ya puedes iniciar sesión con la nueva.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(recibido).toEqual({ token: 'abc123', password: 'una frase larga', confirmacionPassword: 'una frase larga' })
  })

  it('restablecer con sesión abierta la cierra en la web (el servidor ya cerró todas)', async () => {
    conSesion()
    servidor.use(http.post(`${API}/auth/restablecer-password`, () => new HttpResponse(null, { status: 204 })))
    const { usuario } = renderizarApp('/restablecer-password#token=abc123')

    await usuario.type(await screen.findByLabelText('Contraseña nueva'), 'una frase larga')
    await usuario.type(screen.getByLabelText('Repite la contraseña nueva'), 'una frase larga')
    await usuario.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(tokenActual()).toBeNull()
  })

  it('un enlace caducado (422) ofrece pedir otro', async () => {
    servidor.use(
      http.post(`${API}/auth/restablecer-password`, () =>
        HttpResponse.json(problema(422, 'El enlace no es válido o ha caducado. Solicita uno nuevo.'), { status: 422 }),
      ),
    )
    const { usuario } = renderizarApp('/restablecer-password#token=viejo')

    await usuario.type(await screen.findByLabelText('Contraseña nueva'), 'una frase larga')
    await usuario.type(screen.getByLabelText('Repite la contraseña nueva'), 'una frase larga')
    await usuario.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    const aviso = await screen.findByRole('alert')
    expect(aviso).toHaveTextContent('El enlace no es válido o ha caducado.')
    expect(screen.getByRole('link', { name: 'Pedir otro enlace' })).toHaveAttribute('href', '/recuperar-password')
  })

  it('sin token en la dirección no muestra el formulario', async () => {
    renderizarApp('/restablecer-password')

    expect(await screen.findByRole('alert')).toHaveTextContent('El enlace no es válido.')
    expect(within(screen.getByRole('main')).queryByLabelText('Contraseña nueva')).not.toBeInTheDocument()
  })
})
