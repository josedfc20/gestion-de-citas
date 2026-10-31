import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { problema } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, servidor } from '../pruebas/servidor'
import { hoyEnZona, sumarDias } from '../utilidades/fechas'

describe('zona pública', () => {
  it('el inicio muestra el nombre del negocio (GET /negocio)', async () => {
    renderizarApp('/')

    expect(await screen.findByRole('heading', { level: 1, name: 'Peluquería Demo' })).toBeInTheDocument()
  })

  it('servicios con duración y precio formateados (CU-005)', async () => {
    renderizarApp('/servicios')

    const tinte = (await screen.findByRole('heading', { name: 'Tinte' })).closest('li')!
    expect(within(tinte).getByText('1 h 30 min')).toBeInTheDocument()
    expect(within(tinte).getByText(/45,50/)).toBeInTheDocument()
    expect(screen.getByText('Lavado, corte y peinado.')).toBeInTheDocument()
  })

  it('si la API falla muestra el error y deja reintentar', async () => {
    servidor.use(http.get(`${API}/servicios`, () => HttpResponse.json(problema(500, 'Ha ocurrido un error inesperado.'), { status: 500 })))
    renderizarApp('/servicios')

    expect(await screen.findByRole('alert')).toHaveTextContent('Ha ocurrido un error inesperado')
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('profesionales con sus servicios y filtro por servicio (CU-006)', async () => {
    const { usuario, router } = renderizarApp('/profesionales')

    expect(await screen.findByRole('heading', { name: 'Luis G.' })).toBeInTheDocument()
    await usuario.selectOptions(screen.getByLabelText('Filtrar por servicio'), 'Tinte')

    expect(await screen.findByRole('link', { name: 'Ver sus horas libres' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Luis G.' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Ana M.' })).toBeInTheDocument()
    expect(router.state.location.search).toBe('?servicio=2')
  })

  it('horarios: servicio → profesional → día → horas libres (CU-007)', async () => {
    let consulta = ''
    servidor.use(
      http.get(`${API}/disponibilidad`, ({ request }) => {
        consulta = new URL(request.url).search
        const fecha = new URL(request.url).searchParams.get('fecha')
        return HttpResponse.json({ fecha, zonaHoraria: 'Europe/Madrid', horas: ['09:00', '09:30', '16:00'], siguienteFechaConHuecos: null })
      }),
    )
    const { usuario, router } = renderizarApp('/horarios')
    const hoy = hoyEnZona('Europe/Madrid')

    await screen.findByRole('option', { name: 'Corte de pelo · 30 min' })
    await usuario.selectOptions(screen.getByLabelText('Servicio'), 'Corte de pelo · 30 min')
    const profesional = screen.getByLabelText('Profesional')
    await screen.findByRole('option', { name: 'Luis G.' })
    await usuario.selectOptions(profesional, 'Luis G.')

    expect(await screen.findByRole('heading', { name: /3 horas libres/ })).toBeInTheDocument()
    expect(screen.getByText('16:00')).toBeInTheDocument()
    expect(screen.getByText(/Europe\/Madrid/)).toBeInTheDocument()
    expect(consulta).toBe(`?servicioId=1&profesionalId=11&fecha=${hoy}`)
    expect(router.state.location.search).toBe('?servicio=1&profesional=11')
    const dia = screen.getByLabelText('Día')
    expect(dia).toHaveAttribute('min', hoy)
    expect(dia).toHaveAttribute('max', sumarDias(hoy, 60))
  })

  it('horarios: sin horas ese día ofrece ir al siguiente día con huecos', async () => {
    servidor.use(
      http.get(`${API}/disponibilidad`, ({ request }) => {
        const fecha = new URL(request.url).searchParams.get('fecha')
        return fecha === '2030-01-08'
          ? HttpResponse.json({ fecha, zonaHoraria: 'Europe/Madrid', horas: ['10:00'], siguienteFechaConHuecos: null })
          : HttpResponse.json({ fecha, zonaHoraria: 'Europe/Madrid', horas: [], siguienteFechaConHuecos: '2030-01-08' })
      }),
    )
    const { usuario, router } = renderizarApp('/horarios?servicio=1&profesional=10&fecha=2030-01-06')

    expect(await screen.findByText('No hay horas libres el domingo, 6 de enero de 2030.')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: /martes, 8 de enero de 2030/ }))

    expect(await screen.findByRole('heading', { name: /1 hora libre el martes, 8 de enero de 2030/ })).toBeInTheDocument()
    expect(router.state.location.search).toContain('fecha=2030-01-08')
  })

  it('horarios: un profesional de la URL que no realiza el servicio no se usa', async () => {
    renderizarApp('/horarios?servicio=2&profesional=11')

    await screen.findByRole('option', { name: 'Ana M.' })
    expect(screen.getByLabelText('Profesional')).toHaveValue('')
    expect(screen.queryByRole('heading', { name: /horas libres/ })).not.toBeInTheDocument()
  })

  it('horarios: muestra el error de una regla del servidor (422)', async () => {
    servidor.use(
      http.get(`${API}/disponibilidad`, () =>
        HttpResponse.json(problema(422, 'La fecha supera la antelación máxima de 60 días.'), { status: 422 }),
      ),
    )
    renderizarApp('/horarios?servicio=1&profesional=10&fecha=2099-01-01')

    expect(await screen.findByRole('alert')).toHaveTextContent('La fecha supera la antelación máxima de 60 días.')
  })

  it('una ruta que no existe muestra "Página no encontrada"', async () => {
    renderizarApp('/no-existe')

    expect(await screen.findByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
  })
})
