import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import type { DatosReservaAdmin } from '../api/tipos'
import { administrador, citaAsignada, empleado, martaEncontrada, paginaDe, problema, sesionDe } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, servidor } from '../pruebas/servidor'

function conSesion(usuario = administrador) {
  servidor.use(http.post(`${API}/auth/refresh`, () => HttpResponse.json(sesionDe(usuario))))
}

const NUEVA = '/admin/reservas/nueva?servicio=1&profesional=10&fecha=2026-11-03&hora=10:00'

describe('rutas y menú del admin (B9b)', () => {
  it('su página de inicio son las reservas y /mi-area lleva a ellas', async () => {
    conSesion()
    const { router } = renderizarApp('/mi-area')

    expect(await screen.findByRole('heading', { name: 'Reservas', level: 1 })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/admin/reservas')
    const menu = screen.getByRole('navigation', { name: 'Principal' })
    expect(within(menu).getByRole('link', { name: 'Nueva reserva' })).toHaveAttribute('href', '/horarios')
  })

  it('un empleado no entra en las reservas del admin', async () => {
    conSesion(empleado)
    renderizarApp('/admin/reservas')

    expect(await screen.findByRole('heading', { name: 'No tienes acceso a esta página' })).toBeInTheDocument()
  })

  it('en los horarios libres, cada hora lleva al admin a la nueva reserva', async () => {
    conSesion()
    renderizarApp('/horarios?servicio=1&profesional=10&fecha=2026-11-03')

    const enlace = await screen.findByRole('link', { name: 'Reservar a las 09:30' })
    expect(enlace).toHaveAttribute('href', '/admin/reservas/nueva?servicio=1&profesional=10&fecha=2026-11-03&hora=09%3A30')
  })
})

describe('reservas (CU-019)', () => {
  it('lista con el cliente y filtra por profesional y por cliente', async () => {
    conSesion()
    const pedidas: URLSearchParams[] = []
    servidor.use(
      http.get(`${API}/admin/citas`, ({ request }) => {
        pedidas.push(new URL(request.url).searchParams)
        return HttpResponse.json(paginaDe([citaAsignada()]))
      }),
      http.get(`${API}/admin/clientes`, ({ request }) =>
        HttpResponse.json(new URL(request.url).searchParams.get('buscar') === 'mar' ? [martaEncontrada] : []),
      ),
    )
    const { usuario, router } = renderizarApp('/admin/reservas')

    const tarjeta = await screen.findByRole('link', { name: /Marta Pérez Gil/ })
    expect(tarjeta).toHaveAttribute('href', '/admin/reservas/40')

    await usuario.selectOptions(screen.getByLabelText('Profesional'), '11')
    expect(router.state.location.search).toBe('?profesional=11')

    await usuario.click(screen.getByText('Filtrar por cliente'))
    await usuario.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(screen.getByText('Escribe al menos 3 letras.')).toBeInTheDocument()
    await usuario.type(screen.getByLabelText('Buscar cliente'), 'mar')
    await usuario.click(screen.getByRole('button', { name: 'Buscar' }))
    await usuario.click(await screen.findByRole('button', { name: 'Elegir a Marta Pérez Gil' }))

    expect(await screen.findByText('Marta Pérez Gil', { selector: 'strong' })).toBeInTheDocument()
    await screen.findByRole('link', { name: /Marta Pérez Gil/ })
    expect(pedidas.at(-1)?.get('clienteId')).toBe('5')
    expect(pedidas.at(-1)?.get('profesionalId')).toBe('11')
  })

  it('el detalle muestra el correo y el historial con quién hizo cada cambio', async () => {
    conSesion()
    servidor.use(
      http.get(`${API}/citas/40`, () =>
        HttpResponse.json(
          citaAsignada({
            estado: 'CANCELADA',
            motivoCancelacion: 'Baja del profesional',
            cliente: { id: 5, nombre: 'Marta', apellidos: 'Pérez Gil', telefono: '600 000 000', email: 'marta@ejemplo.es' },
            cambios: [
              { estadoAnterior: null, estadoNuevo: 'CONFIRMADA', fecha: '2026-10-30T18:00:00+01:00', realizadoPor: 5, realizadoPorNombre: 'Cliente' },
              { estadoAnterior: 'CONFIRMADA', estadoNuevo: 'CANCELADA', fecha: '2026-11-02T09:15:00+01:00', realizadoPor: 11, realizadoPorNombre: 'Luis G.' },
            ],
          }),
        ),
      ),
    )
    renderizarApp('/admin/reservas/40')

    expect(await screen.findByRole('link', { name: 'marta@ejemplo.es' })).toHaveAttribute('href', 'mailto:marta@ejemplo.es')
    const historial = screen.getByRole('region', { name: 'Historial' })
    expect(within(historial).getByText('Viernes, 30 de octubre a las 18:00 · Reservada (confirmada) · por el cliente')).toBeInTheDocument()
    expect(within(historial).getByText('Lunes, 2 de noviembre a las 09:15 · Confirmada → Cancelada · por Luis G.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar cita' })).not.toBeInTheDocument()
  })
})

describe('reservar por un cliente (CU-019, alternativa 1)', () => {
  /** Guarda lo que llega a POST /admin/citas y responde con la cita creada (o con el error indicado). */
  function capturar(respuesta?: { status: number; detail: string }) {
    const recibido: { cuerpo?: DatosReservaAdmin } = {}
    servidor.use(
      http.get(`${API}/admin/clientes`, () => HttpResponse.json([martaEncontrada])),
      http.post(`${API}/admin/citas`, async ({ request }) => {
        recibido.cuerpo = (await request.json()) as DatosReservaAdmin
        if (respuesta) return HttpResponse.json(problema(respuesta.status, respuesta.detail), { status: respuesta.status })
        return HttpResponse.json(citaAsignada(), { status: 201 })
      }),
      http.get(`${API}/citas/40`, () => HttpResponse.json(citaAsignada())),
    )
    return recibido
  }

  async function elegirMarta(usuario: ReturnType<typeof renderizarApp>['usuario']) {
    await usuario.type(await screen.findByLabelText('Buscar cliente'), 'marta')
    await usuario.click(screen.getByRole('button', { name: 'Buscar' }))
    await usuario.click(await screen.findByRole('button', { name: 'Elegir a Marta Pérez Gil' }))
  }

  it('busca al cliente, elige a su menor y envía la hora con el desfase del negocio', async () => {
    conSesion()
    const recibido = capturar()
    const { usuario, router } = renderizarApp(NUEVA)

    expect(await screen.findByText('martes, 3 de noviembre de 2026 a las 10:00')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar reserva' })).toBeDisabled()
    await elegirMarta(usuario)
    expect(screen.getByLabelText('Para Marta')).toBeChecked()
    await usuario.click(screen.getByLabelText('Lucía Pérez (menor)'))
    await usuario.click(screen.getByRole('button', { name: 'Confirmar reserva' }))

    expect(await screen.findByText('Reserva creada.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/admin/reservas/40')
    expect(recibido.cuerpo).toEqual({ clienteId: 5, servicioId: 1, profesionalId: 10, inicio: '2026-11-03T10:00:00+01:00', beneficiarioId: 3 })
  })

  it('si la hora ya no está libre (409) ofrece elegir otra', async () => {
    conSesion()
    capturar({ status: 409, detail: 'Ese horario ya no está disponible. Elige otra hora.' })
    const { usuario } = renderizarApp(NUEVA)

    await elegirMarta(usuario)
    await usuario.click(screen.getByRole('button', { name: 'Confirmar reserva' }))

    expect(await screen.findByText('Ese horario ya no está disponible. Elige otra hora.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Elegir otra hora' })).toHaveAttribute('href', '/horarios?servicio=1&profesional=10&fecha=2026-11-03')
  })

  it('sin coincidencias explica que el cliente debe registrarse', async () => {
    conSesion()
    const { usuario } = renderizarApp(NUEVA)

    await usuario.type(await screen.findByLabelText('Buscar cliente'), 'nadie')
    await usuario.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText(/antes debe registrarse/)).toBeInTheDocument()
  })
})
