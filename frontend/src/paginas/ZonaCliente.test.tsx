import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import type { DatosReserva } from '../api/tipos'
import { citaDe, cliente, empleado, lucia, paginaDe, problema, sesionDe } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, servidor } from '../pruebas/servidor'

const RESERVA = '/reservar?servicio=1&profesional=10&fecha=2026-11-03&hora=10:00'

function conSesion(usuario = cliente) {
  servidor.use(http.post(`${API}/auth/refresh`, () => HttpResponse.json(sesionDe(usuario))))
}

/** Guarda lo que llega a POST /citas y responde con la cita creada (o con el error indicado). */
function capturarReserva(respuesta?: { status: number; detail: string }) {
  const recibido: { cuerpo?: DatosReserva } = {}
  servidor.use(
    http.post(`${API}/citas`, async ({ request }) => {
      recibido.cuerpo = (await request.json()) as DatosReserva
      if (respuesta) return HttpResponse.json(problema(respuesta.status, respuesta.detail), { status: respuesta.status })
      const cita = citaDe({ beneficiario: recibido.cuerpo.beneficiarioId ? { id: lucia.id, nombre: lucia.nombre } : null })
      return HttpResponse.json(cita, { status: 201 })
    }),
    http.get(`${API}/citas/40`, () => HttpResponse.json(citaDe())),
  )
  return recibido
}

describe('horarios: cada hora lleva a reservar (B8)', () => {
  it('para el cliente las horas son enlaces a /reservar', async () => {
    conSesion()
    renderizarApp('/horarios?servicio=1&profesional=10&fecha=2026-11-03')

    const enlace = await screen.findByRole('link', { name: 'Reservar a las 09:30' })
    expect(enlace).toHaveAttribute('href', '/reservar?servicio=1&profesional=10&fecha=2026-11-03&hora=09%3A30')
  })

  it('el visitante pasa por el login y vuelve a la reserva', async () => {
    const { usuario, router } = renderizarApp('/horarios?servicio=1&profesional=10&fecha=2026-11-03')

    await usuario.click(await screen.findByRole('link', { name: 'Reservar a las 09:00' }))

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(router.state.location.state).toMatchObject({ desde: '/reservar?servicio=1&profesional=10&fecha=2026-11-03&hora=09%3A00' })
  })

  it('para el empleado las horas son solo etiquetas', async () => {
    conSesion(empleado)
    renderizarApp('/horarios?servicio=1&profesional=10&fecha=2026-11-03')

    expect(await screen.findByText('09:30')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Reservar a las/ })).not.toBeInTheDocument()
  })
})

describe('reservar (CU-008 y CU-009)', () => {
  it('reserva para sí mismo con la hora del negocio y va al detalle', async () => {
    conSesion()
    const recibido = capturarReserva()
    const { usuario, router } = renderizarApp(RESERVA)

    expect(await screen.findByText('martes, 3 de noviembre de 2026 a las 10:00')).toBeInTheDocument()
    expect(screen.getByText('Ana M.')).toBeInTheDocument()
    expect(screen.getByLabelText('Para mí')).toBeChecked()
    expect(screen.getByText('Podrás cancelarla online hasta 24 h antes.')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Confirmar reserva' }))

    expect(await screen.findByText('Cita reservada. ¡Te esperamos!')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/mis-citas/40')
    expect(recibido.cuerpo).toEqual({ servicioId: 1, profesionalId: 10, inicio: '2026-11-03T10:00:00+01:00' })
  })

  it('reserva para un menor a su cargo', async () => {
    conSesion()
    servidor.use(http.get(`${API}/me/beneficiarios`, () => HttpResponse.json([lucia])))
    const recibido = capturarReserva()
    const { usuario } = renderizarApp(RESERVA)

    await usuario.click(await screen.findByLabelText('Lucía Pérez'))
    await usuario.click(screen.getByRole('button', { name: 'Confirmar reserva' }))

    expect(await screen.findByText('Cita reservada. ¡Te esperamos!')).toBeInTheDocument()
    expect(recibido.cuerpo?.beneficiarioId).toBe(3)
  })

  it('añade un menor desde la reserva y lo deja elegido', async () => {
    conSesion()
    let menores = [] as (typeof lucia)[]
    servidor.use(
      http.get(`${API}/me/beneficiarios`, () => HttpResponse.json(menores)),
      http.post(`${API}/me/beneficiarios`, () => {
        menores = [lucia]
        return HttpResponse.json(lucia, { status: 201 })
      }),
    )
    const { usuario } = renderizarApp(RESERVA)

    await usuario.click(await screen.findByRole('button', { name: 'Añadir un menor a cargo' }))
    await usuario.type(screen.getByLabelText('Nombre'), 'Lucía')
    await usuario.type(screen.getByLabelText('Apellidos'), 'Pérez')
    await usuario.type(screen.getByLabelText('Fecha de nacimiento'), '2018-05-20')
    await usuario.click(screen.getByRole('button', { name: 'Añadir' }))

    expect(await screen.findByLabelText('Lucía Pérez')).toBeChecked()
    expect(screen.getByRole('button', { name: 'Confirmar reserva' })).toBeEnabled()
  })

  it('si la hora ya no está libre (409) ofrece elegir otra', async () => {
    conSesion()
    capturarReserva({ status: 409, detail: 'Ese horario ya no está disponible.' })
    const { usuario } = renderizarApp(RESERVA)

    await usuario.click(await screen.findByRole('button', { name: 'Confirmar reserva' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ese horario ya no está disponible.')
    expect(screen.getByRole('link', { name: 'Elegir otra hora' })).toHaveAttribute('href', '/horarios?servicio=1&profesional=10&fecha=2026-11-03')
    expect(screen.getByRole('button', { name: 'Confirmar reserva' })).toBeDisabled()
  })

  it('una regla de negocio (422) se muestra tal cual', async () => {
    conSesion()
    capturarReserva({ status: 422, detail: 'Ya tienes otra cita a esa hora.' })
    const { usuario } = renderizarApp(RESERVA)

    await usuario.click(await screen.findByRole('button', { name: 'Confirmar reserva' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya tienes otra cita a esa hora.')
    expect(screen.getByRole('button', { name: 'Confirmar reserva' })).toBeEnabled()
  })

  it('sin hora en la dirección pide elegirla primero', async () => {
    conSesion()
    renderizarApp('/reservar?servicio=1')

    expect(await screen.findByText(/Elige primero un servicio/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver horarios libres' })).toHaveAttribute('href', '/horarios?servicio=1')
  })

  it('el empleado no puede reservar', async () => {
    conSesion(empleado)
    renderizarApp(RESERVA)

    expect(await screen.findByRole('heading', { name: 'No tienes acceso a esta página' })).toBeInTheDocument()
  })
})

describe('mis citas (CU-010)', () => {
  it('muestra las próximas con su estado y para quién son', async () => {
    conSesion()
    servidor.use(
      http.get(`${API}/me/citas`, () =>
        HttpResponse.json(paginaDe([citaDe(), citaDe({ id: 41, estado: 'CANCELADA', beneficiario: { id: 3, nombre: 'Lucía' } })])),
      ),
    )
    renderizarApp('/mis-citas')

    const tarjetas = await screen.findAllByRole('link', { name: /Corte de pelo/ })
    expect(within(tarjetas[0]).getByText('Confirmada')).toBeInTheDocument()
    expect(within(tarjetas[1]).getByText('Cancelada')).toBeInTheDocument()
    expect(within(tarjetas[1]).getByText('Para Lucía')).toBeInTheDocument()
    expect(tarjetas[0]).toHaveAttribute('href', '/mis-citas/40')
  })

  it('el historial pide las pasadas y "Ver más" trae la página siguiente', async () => {
    conSesion()
    const pedidas: string[] = []
    servidor.use(
      http.get(`${API}/me/citas`, ({ request }) => {
        const parametros = new URL(request.url).searchParams
        pedidas.push(parametros.toString())
        const pagina = Number(parametros.get('page'))
        return HttpResponse.json(paginaDe([citaDe({ id: 50 + pagina, estado: 'COMPLETADA' })], pagina, 2))
      }),
    )
    const { usuario, router } = renderizarApp('/mis-citas')

    await usuario.click(await screen.findByRole('button', { name: 'Historial' }))
    expect(router.state.location.search).toBe('?ver=historial')
    await usuario.click(await screen.findByRole('button', { name: 'Ver más' }))

    expect(await screen.findAllByText('Completada')).toHaveLength(2)
    expect(screen.queryByRole('button', { name: 'Ver más' })).not.toBeInTheDocument()
    expect(pedidas).toContain('pasadas=true&page=1&size=20')
  })

  it('sin citas invita a reservar', async () => {
    conSesion()
    renderizarApp('/mis-citas')

    expect(await screen.findByText('No tienes citas próximas.')).toBeInTheDocument()
  })
})

describe('detalle y cancelar (CU-011 y CU-012)', () => {
  it('cancela con motivo dentro del plazo', async () => {
    conSesion()
    let motivo: unknown
    servidor.use(
      http.get(`${API}/citas/40`, () => HttpResponse.json(citaDe())),
      http.post(`${API}/citas/40/cancelacion`, async ({ request }) => {
        motivo = ((await request.json()) as { motivo: string }).motivo
        return HttpResponse.json(citaDe({ estado: 'CANCELADA', puedeCancelar: false, motivoCancelacion: 'Viaje' }))
      }),
    )
    const { usuario } = renderizarApp('/mis-citas/40')

    expect(await screen.findByText(/Puedes cancelarla online hasta el lunes, 2 de noviembre a las 10:00/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Cancelar cita' }))
    await usuario.type(screen.getByLabelText(/Motivo/), 'Viaje')
    await usuario.click(screen.getByRole('button', { name: 'Sí, cancelar' }))

    expect(await screen.findByText('Cita cancelada.')).toBeInTheDocument()
    expect(screen.getAllByText('Cancelada').length).toBeGreaterThan(0)
    expect(screen.queryByRole('button', { name: 'Cancelar cita' })).not.toBeInTheDocument()
    expect(motivo).toBe('Viaje')
  })

  it('fuera de plazo no deja cancelar online', async () => {
    conSesion()
    servidor.use(http.get(`${API}/citas/40`, () => HttpResponse.json(citaDe({ puedeCancelar: false }))))
    renderizarApp('/mis-citas/40')

    expect(await screen.findByText(/Ya no puedes cancelarla online/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar cita' })).not.toBeInTheDocument()
  })

  it('si el plazo vence mientras miraba la página, muestra el 422', async () => {
    conSesion()
    let vencida = false
    servidor.use(
      http.get(`${API}/citas/40`, () => HttpResponse.json(citaDe({ puedeCancelar: !vencida }))),
      http.post(`${API}/citas/40/cancelacion`, () => {
        vencida = true
        return HttpResponse.json(problema(422, 'Ya no puedes cancelar online. Contacta con el negocio.'), { status: 422 })
      }),
    )
    const { usuario } = renderizarApp('/mis-citas/40')

    await usuario.click(await screen.findByRole('button', { name: 'Cancelar cita' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, cancelar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya no puedes cancelar online. Contacta con el negocio.')
    expect(await screen.findByText(/Ya no puedes cancelarla online/)).toBeInTheDocument()
  })

  it('una cita ajena o que no existe (404)', async () => {
    conSesion()
    servidor.use(http.get(`${API}/citas/99`, () => HttpResponse.json(problema(404, 'Cita no encontrada.'), { status: 404 })))
    renderizarApp('/mis-citas/99')

    expect(await screen.findByRole('heading', { name: 'Cita no encontrada' })).toBeInTheDocument()
  })
})

describe('menores a mi cargo (CU-009)', () => {
  it('añade un menor y muestra el 422 de la edad debajo de la fecha', async () => {
    conSesion()
    servidor.use(
      http.post(`${API}/me/beneficiarios`, () =>
        HttpResponse.json(problema(422, 'Ya tiene la edad para crear su propia cuenta.'), { status: 422 }),
      ),
    )
    const { usuario } = renderizarApp('/menores')

    await usuario.click(await screen.findByRole('button', { name: 'Añadir un menor' }))
    await usuario.type(screen.getByLabelText('Nombre'), 'Pablo')
    await usuario.type(screen.getByLabelText('Apellidos'), 'Pérez')
    await usuario.type(screen.getByLabelText('Fecha de nacimiento'), '2015-01-01')
    await usuario.click(screen.getByRole('button', { name: 'Añadir' }))

    expect(await screen.findByText('Ya tiene la edad para crear su propia cuenta.')).toBeInTheDocument()
    expect(screen.getByLabelText('Fecha de nacimiento')).toHaveAttribute('aria-invalid', 'true')
  })

  it('avisa en el navegador si ya tiene la edad mínima', async () => {
    conSesion()
    const { usuario } = renderizarApp('/menores')

    await usuario.click(await screen.findByRole('button', { name: 'Añadir un menor' }))
    await usuario.type(screen.getByLabelText('Nombre'), 'Pablo')
    await usuario.type(screen.getByLabelText('Apellidos'), 'Pérez')
    await usuario.type(screen.getByLabelText('Fecha de nacimiento'), '2000-01-01')
    await usuario.click(screen.getByRole('button', { name: 'Añadir' }))

    expect(await screen.findByText('Con 14 años o más necesita su propia cuenta.')).toBeInTheDocument()
  })

  it('corrige y elimina con confirmación', async () => {
    conSesion()
    let menores = [lucia]
    let corregido: unknown
    servidor.use(
      http.get(`${API}/me/beneficiarios`, () => HttpResponse.json(menores)),
      http.put(`${API}/me/beneficiarios/3`, async ({ request }) => {
        corregido = await request.json()
        menores = [{ ...lucia, apellidos: 'Pérez Gil' }]
        return HttpResponse.json(menores[0])
      }),
      http.delete(`${API}/me/beneficiarios/3`, () => {
        menores = []
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { usuario } = renderizarApp('/menores')

    await usuario.click(await screen.findByRole('button', { name: 'Corregir a Lucía' }))
    await usuario.clear(screen.getByLabelText('Apellidos'))
    await usuario.type(screen.getByLabelText('Apellidos'), 'Pérez Gil')
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(await screen.findByText('Lucía Pérez Gil')).toBeInTheDocument()
    expect(corregido).toEqual({ nombre: 'Lucía', apellidos: 'Pérez Gil', fechaNacimiento: '2018-05-20' })

    await usuario.click(screen.getByRole('button', { name: 'Eliminar a Lucía' }))
    expect(screen.getByText(/Si tiene citas próximas, se cancelarán/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Sí, eliminar' }))

    expect(await screen.findByText('Se ha eliminado a Lucía.')).toBeInTheDocument()
    expect(screen.getByText('Todavía no has añadido ningún menor.')).toBeInTheDocument()
  })
})
