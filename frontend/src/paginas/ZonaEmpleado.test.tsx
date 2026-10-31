import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { AgendaDia, DatosCambioEstado } from '../api/tipos'
import { agendaVacia, citaAsignada, cliente, empleado, paginaDe, problema, servicios, sesionDe } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, servidor } from '../pruebas/servidor'

function conSesion(usuario = empleado) {
  servidor.use(http.post(`${API}/auth/refresh`, () => HttpResponse.json(sesionDe(usuario))))
}

// Solo se finge la fecha (no los temporizadores, que usa user-event).
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-11-02T08:00:00Z')) // lunes 2 de noviembre, 09:00 en Madrid
})
afterEach(() => {
  vi.useRealTimers()
})

describe('rutas y menú del empleado', () => {
  it('su página de inicio es la agenda y /mi-area lleva a ella', async () => {
    conSesion()
    const { router } = renderizarApp('/mi-area')

    expect(await screen.findByRole('heading', { name: 'Mi agenda', level: 1 })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/empleado/agenda')
    const menu = screen.getByRole('navigation', { name: 'Principal' })
    expect(within(menu).getByRole('link', { name: 'Mis citas' })).toHaveAttribute('href', '/empleado/citas')
    expect(within(menu).getByRole('link', { name: 'Mis servicios' })).toHaveAttribute('href', '/empleado/servicios')
    expect(within(menu).queryByRole('link', { name: 'Mi área' })).not.toBeInTheDocument()
  })

  it('un cliente no entra en la zona del empleado', async () => {
    conSesion(cliente)
    renderizarApp('/empleado/agenda')

    expect(await screen.findByRole('heading', { name: 'No tienes acceso a esta página' })).toBeInTheDocument()
  })
})

describe('agenda (CU-013)', () => {
  it('muestra la semana con sus horarios, los días libres, los cierres y las citas', async () => {
    conSesion()
    const pedidas: string[] = []
    servidor.use(
      http.get(`${API}/empleado/agenda`, ({ request }) => {
        pedidas.push(new URL(request.url).search)
        const dias: AgendaDia[] = agendaVacia('2026-11-02', '2026-11-08')
        dias[1].citas = [citaAsignada({ beneficiario: { id: 3, nombre: 'Lucía' } })]
        dias[2] = { ...dias[2], cierre: true, tramos: [] }
        return HttpResponse.json(dias)
      }),
    )
    renderizarApp('/empleado/agenda')

    const lunes = await screen.findByRole('region', { name: /lunes, 2 de noviembre/ })
    expect(pedidas).toEqual(['?desde=2026-11-02&hasta=2026-11-08'])
    expect(within(lunes).getByText('Hoy')).toBeInTheDocument()
    expect(within(lunes).getByText('09:00–14:00 · 16:00–20:00')).toBeInTheDocument()

    const martes = screen.getByRole('region', { name: /martes, 3 de noviembre/ })
    const cita = within(martes).getByRole('link', { name: /10:00–10:30 Corte de pelo/ })
    expect(cita).toHaveAttribute('href', '/empleado/citas/40')
    expect(within(cita).getByText('Lucía (menor), a cargo de Marta Pérez Gil')).toBeInTheDocument()

    expect(within(screen.getByRole('region', { name: /miércoles, 4 de noviembre/ })).getByText('Cerrado (día de cierre)')).toBeInTheDocument()
    expect(within(screen.getByRole('region', { name: /sábado, 7 de noviembre/ })).getByText('No trabajas')).toBeInTheDocument()
  })

  it('cambia de semana y una fecha que no es lunes empieza en su lunes', async () => {
    conSesion()
    const { usuario, router } = renderizarApp('/empleado/agenda?desde=2026-11-12')

    expect(await screen.findByRole('region', { name: /lunes, 9 de noviembre/ })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Semana siguiente →' }))

    expect(await screen.findByRole('region', { name: /lunes, 16 de noviembre/ })).toBeInTheDocument()
    expect(router.state.location.search).toBe('?desde=2026-11-16')

    await usuario.click(screen.getByRole('button', { name: 'Hoy' }))
    expect(await screen.findByRole('region', { name: /lunes, 2 de noviembre/ })).toBeInTheDocument()
    expect(router.state.location.search).toBe('')
  })
})

describe('citas asignadas (CU-014)', () => {
  it('muestra el cliente y su teléfono, filtra por estado y carga más', async () => {
    conSesion()
    const pedidas: URLSearchParams[] = []
    servidor.use(
      http.get(`${API}/empleado/citas`, ({ request }) => {
        const parametros = new URL(request.url).searchParams
        pedidas.push(parametros)
        const pagina = Number(parametros.get('page'))
        return HttpResponse.json(paginaDe([citaAsignada({ id: 60 + pagina })], pagina, 2))
      }),
    )
    const { usuario, router } = renderizarApp('/empleado/citas')

    const tarjeta = await screen.findByRole('link', { name: /Marta Pérez Gil/ })
    expect(within(tarjeta).getByText('· 600 000 000')).toBeInTheDocument()
    expect(tarjeta).toHaveAttribute('href', '/empleado/citas/60')

    await usuario.click(screen.getByRole('button', { name: 'Ver más' }))
    expect(await screen.findAllByRole('link', { name: /Marta Pérez Gil/ })).toHaveLength(2)

    await usuario.selectOptions(screen.getByLabelText('Estado'), 'COMPLETADA')
    expect(router.state.location.search).toBe('?estado=COMPLETADA')
    await screen.findAllByRole('link', { name: /Marta Pérez Gil/ })
    expect(pedidas.at(-1)?.get('estado')).toBe('COMPLETADA')
  })
})

describe('detalle y cambio de estado (CU-015)', () => {
  /** Responde al detalle con la cita y guarda lo que llega al PATCH. */
  function conCita(inicio: string, respuestaPatch?: { status: number; detail: string }) {
    const recibido: { cuerpo?: DatosCambioEstado } = {}
    const cita = citaAsignada({ inicio, fin: inicio.replace(':00:00', ':30:00') })
    servidor.use(
      http.get(`${API}/citas/40`, () => HttpResponse.json(cita)),
      http.patch(`${API}/citas/40/estado`, async ({ request }) => {
        recibido.cuerpo = (await request.json()) as DatosCambioEstado
        if (respuestaPatch) return HttpResponse.json(problema(respuestaPatch.status, respuestaPatch.detail), { status: respuestaPatch.status })
        return HttpResponse.json({ ...cita, estado: recibido.cuerpo.estado, motivoCancelacion: recibido.cuerpo.motivo ?? null })
      }),
    )
    return recibido
  }

  it('muestra el cliente con su teléfono y, antes de la hora, no ofrece completarla', async () => {
    conSesion()
    conCita('2026-11-03T10:00:00+01:00')
    renderizarApp('/empleado/citas/40')

    expect(await screen.findByRole('heading', { name: 'Corte de pelo' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '600 000 000' })).toHaveAttribute('href', 'tel:600000000')
    expect(screen.getByText('El propio cliente')).toBeInTheDocument()
    expect(screen.getByText(/a partir de las 10:00 del día de la cita/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Marcar como completada' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelar cita' })).toBeInTheDocument()
  })

  it('desde la hora de inicio la marca como completada', async () => {
    conSesion()
    const recibido = conCita('2026-11-02T09:00:00+01:00')
    const { usuario } = renderizarApp('/empleado/citas/40')

    await usuario.click(await screen.findByRole('button', { name: 'Marcar como completada' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, completada' }))

    expect(await screen.findByText('Cita marcada como completada.')).toBeInTheDocument()
    expect(recibido.cuerpo).toEqual({ estado: 'COMPLETADA', motivo: null })
    expect(screen.getByText('Completada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar cita' })).not.toBeInTheDocument()
  })

  it('cancelar pide el motivo y lo envía', async () => {
    conSesion()
    const recibido = conCita('2026-11-03T10:00:00+01:00')
    const { usuario } = renderizarApp('/empleado/citas/40')

    await usuario.click(await screen.findByRole('button', { name: 'Cancelar cita' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, cancelar' }))
    expect(screen.getByText('Indica el motivo de la cancelación.')).toBeInTheDocument()
    expect(screen.getByLabelText('Motivo')).toHaveAttribute('aria-invalid', 'true')
    expect(recibido.cuerpo).toBeUndefined()

    await usuario.type(screen.getByLabelText('Motivo'), 'Profesional enfermo')
    await usuario.click(screen.getByRole('button', { name: 'Sí, cancelar' }))

    expect(await screen.findByText('Cita cancelada.')).toBeInTheDocument()
    expect(recibido.cuerpo).toEqual({ estado: 'CANCELADA', motivo: 'Profesional enfermo' })
    expect(screen.getByText('Profesional enfermo')).toBeInTheDocument()
  })

  it('muestra el mensaje del servidor si rechaza el cambio (422)', async () => {
    conSesion()
    conCita('2026-11-02T09:00:00+01:00', { status: 422, detail: 'La cita ya no está confirmada.' })
    const { usuario } = renderizarApp('/empleado/citas/40')

    await usuario.click(await screen.findByRole('button', { name: 'No se presentó' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, no se presentó' }))

    expect(await screen.findByText('La cita ya no está confirmada.')).toBeInTheDocument()
  })

  it('una cita de otro empleado no se encuentra (404)', async () => {
    conSesion()
    servidor.use(http.get(`${API}/citas/99`, () => HttpResponse.json(problema(404, 'Cita no encontrada.'), { status: 404 })))
    renderizarApp('/empleado/citas/99')

    expect(await screen.findByRole('heading', { name: 'Cita no encontrada' })).toBeInTheDocument()
  })
})

describe('mis servicios (CU-021)', () => {
  it('sin servicios lo explica', async () => {
    conSesion()
    renderizarApp('/empleado/servicios')

    expect(await screen.findByText('Aún no tienes servicios asignados. Habla con el administrador.')).toBeInTheDocument()
  })

  it('lista sus servicios con duración y precio', async () => {
    conSesion()
    servidor.use(http.get(`${API}/empleado/servicios`, () => HttpResponse.json(servicios)))
    renderizarApp('/empleado/servicios')

    expect(await screen.findByRole('heading', { name: 'Tinte' })).toBeInTheDocument()
    expect(screen.getByText('1 h 30 min')).toBeInTheDocument()
  })
})
