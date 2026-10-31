import { fireEvent, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import type { CitaAfectada, DatosServicio, DiaCierre, DiaHorario, EmpleadoAdmin, Negocio, ServicioAdmin } from '../api/tipos'
import { administrador, negocio, problema, sesionDe } from '../pruebas/datos'
import { renderizarApp } from '../pruebas/renderizar'
import { API, servidor } from '../pruebas/servidor'

const corte: ServicioAdmin = { id: 1, nombre: 'Corte de pelo', descripcion: null, duracionMinutos: 30, precio: 15, activo: true, empleados: 2 }
const tinte: ServicioAdmin = { id: 2, nombre: 'Tinte', descripcion: null, duracionMinutos: 90, precio: 45.5, activo: true, empleados: 1 }
const peinado: ServicioAdmin = { id: 3, nombre: 'Peinado', descripcion: null, duracionMinutos: 30, precio: 10, activo: false, empleados: 0 }

const luis: EmpleadoAdmin = {
  id: 11,
  nombre: 'Luis',
  apellidos: 'García',
  email: 'luis@demo.invalid',
  telefono: null,
  activo: true,
  debeCambiarPassword: false,
  servicios: [{ id: 1, nombre: 'Corte de pelo' }],
}

const afectada: CitaAfectada = { id: 40, profesionalId: 11, profesional: 'Luis G.', inicio: '2026-11-07T10:00:00+01:00', fin: '2026-11-07T10:30:00+01:00' }

/** Horario general de la demo: de lunes a viernes, mañana y tarde; sábado por la mañana. */
const general: DiaHorario[] = [1, 2, 3, 4, 5]
  .map((dia) => ({ diaSemana: dia, tramos: [{ inicio: '09:00', fin: '14:00' }, { inicio: '16:00', fin: '20:00' }] }))
  .concat([{ diaSemana: 6, tramos: [{ inicio: '09:00', fin: '14:00' }] }, { diaSemana: 7, tramos: [] }])

function conAdmin() {
  servidor.use(
    http.post(`${API}/auth/refresh`, () => HttpResponse.json(sesionDe(administrador))),
    http.get(`${API}/admin/servicios`, () => HttpResponse.json([corte, tinte, peinado])),
    http.get(`${API}/admin/servicios/1`, () => HttpResponse.json(corte)),
    http.get(`${API}/admin/empleados`, () => HttpResponse.json([luis])),
    http.get(`${API}/admin/empleados/11`, () => HttpResponse.json(luis)),
    http.get(`${API}/admin/horario-general`, () => HttpResponse.json({ dias: general })),
    http.get(`${API}/admin/dias-cierre`, () => HttpResponse.json([])),
  )
}

/** Guarda el cuerpo de la última petición que llega a un manejador. */
function capturar<T>() {
  return { cuerpo: undefined as T | undefined }
}

describe('menú del admin (B9c)', () => {
  it('lleva a servicios, empleados, horarios y negocio', async () => {
    conAdmin()
    renderizarApp('/admin/reservas')

    const menu = await screen.findByRole('navigation', { name: 'Principal' })
    for (const [texto, ruta] of [
      ['Servicios', '/admin/servicios'],
      ['Empleados', '/admin/empleados'],
      ['Horarios', '/admin/horarios'],
      ['Negocio', '/admin/negocio'],
    ]) {
      expect(within(menu).getByRole('link', { name: texto })).toHaveAttribute('href', ruta)
    }
  })
})

describe('servicios (CU-016)', () => {
  it('oculta los desactivados hasta que se piden', async () => {
    conAdmin()
    const { usuario } = renderizarApp('/admin/servicios')

    expect(await screen.findByRole('heading', { name: 'Tinte' })).toBeInTheDocument()
    expect(screen.getByText('2 profesionales')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Peinado' })).not.toBeInTheDocument()

    await usuario.click(screen.getByLabelText('Ver también los desactivados'))
    expect(screen.getByRole('heading', { name: 'Peinado' })).toBeInTheDocument()
    expect(screen.getByText('Desactivado')).toBeInTheDocument()
  })

  it('crea un servicio con una duración múltiplo del intervalo y un precio con coma', async () => {
    conAdmin()
    const recibido = capturar<DatosServicio>()
    servidor.use(
      http.post(`${API}/admin/servicios`, async ({ request }) => {
        recibido.cuerpo = (await request.json()) as DatosServicio
        return HttpResponse.json({ ...recibido.cuerpo, id: 9, activo: true, empleados: 0 }, { status: 201 })
      }),
      http.get(`${API}/admin/servicios/9`, () => HttpResponse.json({ ...recibido.cuerpo, id: 9, activo: true, empleados: 0 })),
    )
    const { usuario, router } = renderizarApp('/admin/servicios/nuevo')

    const duracion = await screen.findByLabelText('Duración')
    const opciones = within(duracion).getAllByRole('option').map((opcion) => opcion.textContent)
    expect(opciones).toContain('1 h 30 min')
    expect(opciones).not.toContain('45 min')

    await usuario.click(screen.getByRole('button', { name: 'Crear servicio' }))
    expect(screen.getByText('Escribe el nombre del servicio.')).toBeInTheDocument()

    await usuario.type(screen.getByLabelText('Nombre'), 'Barba')
    await usuario.selectOptions(duracion, '60')
    await usuario.type(screen.getByLabelText('Precio (€)'), '12,5')
    await usuario.click(screen.getByRole('button', { name: 'Crear servicio' }))

    expect(await screen.findByText('Servicio creado.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/admin/servicios/9')
    expect(recibido.cuerpo).toEqual({ nombre: 'Barba', descripcion: null, duracionMinutos: 60, precio: 12.5 })
  })

  it('un nombre repetido (409) sale debajo del nombre', async () => {
    conAdmin()
    servidor.use(http.put(`${API}/admin/servicios/1`, () => HttpResponse.json(problema(409, 'Ya existe un servicio con ese nombre.'), { status: 409 })))
    const { usuario } = renderizarApp('/admin/servicios/1')

    await usuario.click(await screen.findByRole('button', { name: 'Guardar cambios' }))

    expect(await screen.findByText('Ya existe un servicio con ese nombre.')).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre')).toHaveAttribute('aria-invalid', 'true')
  })

  it('desactiva un servicio tras confirmarlo', async () => {
    conAdmin()
    let activo: boolean | undefined
    servidor.use(
      http.patch(`${API}/admin/servicios/1/activo`, async ({ request }) => {
        activo = ((await request.json()) as { activo: boolean }).activo
        return HttpResponse.json({ ...corte, activo })
      }),
    )
    const { usuario } = renderizarApp('/admin/servicios/1')

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, desactivar' }))

    expect(await screen.findByRole('heading', { name: 'Servicio desactivado' })).toBeInTheDocument()
    expect(activo).toBe(false)
  })
})

describe('empleados (CU-017, RN-005)', () => {
  it('da de alta un empleado y muestra su contraseña temporal una vez', async () => {
    conAdmin()
    const recibido = capturar<Record<string, unknown>>()
    servidor.use(
      http.post(`${API}/admin/empleados`, async ({ request }) => {
        recibido.cuerpo = (await request.json()) as Record<string, unknown>
        return HttpResponse.json({ empleado: { ...luis, id: 12, nombre: 'Ana', apellidos: 'Martín', email: 'ana@demo.invalid' }, passwordTemporal: 'Xk3pQ9mZt2Lw' }, { status: 201 })
      }),
    )
    const { usuario } = renderizarApp('/admin/empleados/nuevo')

    await usuario.type(await screen.findByLabelText('Nombre'), 'Ana')
    await usuario.type(screen.getByLabelText('Apellidos'), 'Martín')
    await usuario.type(screen.getByLabelText('Correo electrónico'), 'ana@demo.invalid')
    expect(screen.queryByLabelText(/Peinado/)).not.toBeInTheDocument()
    await usuario.click(await screen.findByLabelText('Tinte (1 h 30 min)'))
    await usuario.click(screen.getByRole('button', { name: 'Dar de alta' }))

    expect(await screen.findByText('Xk3pQ9mZt2Lw')).toBeInTheDocument()
    expect(screen.getByText(/no se volverá a mostrar/)).toBeInTheDocument()
    expect(recibido.cuerpo).toEqual({ nombre: 'Ana', apellidos: 'Martín', email: 'ana@demo.invalid', telefono: null, servicioIds: [2] })
    expect(screen.getByRole('link', { name: 'Ver su ficha' })).toHaveAttribute('href', '/admin/empleados/12')
  })

  it('al quitarle un servicio con citas futuras muestra el aviso del servidor', async () => {
    conAdmin()
    let servicioIds: number[] | undefined
    servidor.use(
      http.put(`${API}/admin/empleados/11/servicios`, async ({ request }) => {
        servicioIds = ((await request.json()) as { servicioIds: number[] }).servicioIds
        return HttpResponse.json({ ...luis, servicios: [], avisos: ['Tiene 2 citas futuras de Corte de pelo; se mantienen.'] })
      }),
    )
    const { usuario } = renderizarApp('/admin/empleados/11')

    await usuario.click(await screen.findByLabelText('Corte de pelo (30 min)'))
    await usuario.click(screen.getByRole('button', { name: 'Guardar servicios' }))

    expect(await screen.findByText('Tiene 2 citas futuras de Corte de pelo; se mantienen.')).toBeInTheDocument()
    expect(servicioIds).toEqual([])
  })

  it('no se desactiva con citas futuras (409) y enlaza a ellas', async () => {
    conAdmin()
    servidor.use(
      http.patch(`${API}/admin/empleados/11/activo`, () =>
        HttpResponse.json(problema(409, 'Tiene 3 citas futuras. Cancélalas o reasígnalas antes de desactivarlo.', { citasFuturas: 3 }), { status: 409 }),
      ),
    )
    const { usuario } = renderizarApp('/admin/empleados/11')

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, desactivar' }))

    expect(await screen.findByText('Tiene 3 citas futuras. Cancélalas o reasígnalas antes de desactivarlo.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver sus citas confirmadas' })).toHaveAttribute('href', '/admin/reservas?profesional=11&estado=CONFIRMADA')
  })

  it('genera otra contraseña temporal tras confirmarlo', async () => {
    conAdmin()
    servidor.use(http.post(`${API}/admin/empleados/11/password-temporal`, () => HttpResponse.json({ passwordTemporal: 'Nueva123abcd' })))
    const { usuario } = renderizarApp('/admin/empleados/11')

    await usuario.click(await screen.findByRole('button', { name: 'Generar otra contraseña temporal' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, generar' }))

    expect(await screen.findByText('Nueva123abcd')).toBeInTheDocument()
  })
})

describe('horarios (CU-018, RN-002)', () => {
  it('el horario general valida los tramos, los envía y lista las citas afectadas', async () => {
    conAdmin()
    const recibido = capturar<{ dias: DiaHorario[] }>()
    servidor.use(
      http.put(`${API}/admin/horario-general`, async ({ request }) => {
        recibido.cuerpo = (await request.json()) as { dias: DiaHorario[] }
        return HttpResponse.json({ dias: recibido.cuerpo.dias, citasAfectadas: [afectada] })
      }),
    )
    const { usuario } = renderizarApp('/admin/horarios')

    const sabado = await screen.findByRole('group', { name: 'Sábado' })
    expect(within(sabado).getByLabelText('Sábado, tramo 1: inicio')).toHaveValue('09:00')
    expect(within(screen.getByRole('group', { name: 'Domingo' })).getByText('Cerrado.')).toBeInTheDocument()

    // Un tramo al revés no se envía.
    fireEvent.change(within(sabado).getByLabelText('Sábado, tramo 1: fin'), { target: { value: '08:00' } })
    await usuario.click(screen.getByRole('button', { name: 'Guardar horario general' }))
    expect(within(sabado).getByText('La hora de inicio debe ser anterior a la de fin.')).toBeInTheDocument()
    expect(recibido.cuerpo).toBeUndefined()

    // El sábado queda cerrado y el domingo abre por la mañana.
    await usuario.click(within(sabado).getByRole('button', { name: 'Quitar el tramo 1 del sábado' }))
    await usuario.click(screen.getByRole('button', { name: 'Añadir un tramo al domingo' }))
    await usuario.click(screen.getByRole('button', { name: 'Guardar horario general' }))

    expect(await screen.findByText('Horario general guardado.')).toBeInTheDocument()
    expect(recibido.cuerpo?.dias.find((dia) => dia.diaSemana === 6)).toEqual({ diaSemana: 6, tramos: [] })
    expect(recibido.cuerpo?.dias.find((dia) => dia.diaSemana === 7)).toEqual({ diaSemana: 7, tramos: [{ inicio: '09:00', fin: '14:00' }] })
    expect(screen.getByText(/Esta cita queda fuera del horario/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'sábado, 7 de noviembre de 2026 a las 10:00, con Luis G.' })).toHaveAttribute('href', '/admin/reservas/40')
  })

  it('un solape que rechaza el servidor (422) se muestra', async () => {
    conAdmin()
    servidor.use(
      http.put(`${API}/admin/horario-general`, () =>
        HttpResponse.json(problema(422, 'Los tramos del lunes se solapan o se tocan.'), { status: 422 }),
      ),
    )
    const { usuario } = renderizarApp('/admin/horarios')

    await usuario.click(await screen.findByRole('button', { name: 'Guardar horario general' }))

    expect(await screen.findByText('Los tramos del lunes se solapan o se tocan.')).toBeInTheDocument()
  })

  it('el horario de un empleado solo envía los días con horario propio', async () => {
    conAdmin()
    const recibido = capturar<{ dias: DiaHorario[] }>()
    const efectiva = general.map((dia) => ({ ...dia, ajustado: false }))
    servidor.use(
      http.get(`${API}/admin/empleados/11/horario`, () => HttpResponse.json({ empleadoId: 11, ajustes: [], semanaEfectiva: efectiva })),
      http.put(`${API}/admin/empleados/11/horario`, async ({ request }) => {
        recibido.cuerpo = (await request.json()) as { dias: DiaHorario[] }
        return HttpResponse.json({
          empleadoId: 11,
          ajustes: recibido.cuerpo.dias,
          semanaEfectiva: efectiva.map((dia) => (dia.diaSemana === 1 ? { diaSemana: 1, ajustado: true, tramos: [] } : dia)),
        })
      }),
    )
    const { usuario } = renderizarApp('/admin/empleados/11/horario')

    const lunes = await screen.findByRole('group', { name: 'Lunes' })
    expect(within(lunes).getByLabelText('Como el horario general (09:00–14:00 · 16:00–20:00)')).toBeChecked()
    await usuario.click(within(lunes).getByLabelText('Horario propio'))
    await usuario.click(within(lunes).getByRole('button', { name: 'Quitar el tramo 2 del lunes' }))
    await usuario.click(within(lunes).getByRole('button', { name: 'Quitar el tramo 1 del lunes' }))
    expect(within(lunes).getByText('No trabaja este día.')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Guardar horario' }))

    expect(await screen.findByText('Horario guardado.')).toBeInTheDocument()
    expect(recibido.cuerpo).toEqual({ dias: [{ diaSemana: 1, tramos: [] }] })
    expect(screen.getByText('Lunes: no trabaja (horario propio)')).toBeInTheDocument()
  })
})

describe('días de cierre (CU-018)', () => {
  it('añade un cierre de un empleado, avisa de un día repetido (409) y quita otro', async () => {
    conAdmin()
    const festivo: DiaCierre = { id: 5, fecha: '2026-12-08', empleadoId: null, empleado: null, motivo: 'Festivo' }
    const recibido = capturar<Record<string, unknown>>()
    let borrado = false
    let repetido = false
    servidor.use(
      http.get(`${API}/admin/dias-cierre`, () => HttpResponse.json(borrado ? [] : [festivo])),
      http.post(`${API}/admin/dias-cierre`, async ({ request }) => {
        recibido.cuerpo = (await request.json()) as Record<string, unknown>
        if (repetido) return HttpResponse.json(problema(409, 'Ya hay un cierre ese día.'), { status: 409 })
        repetido = true
        return HttpResponse.json({ id: 6, fecha: '2026-11-20', empleadoId: 11, empleado: 'Luis García', motivo: 'Vacaciones', citasAfectadas: [] }, { status: 201 })
      }),
      http.delete(`${API}/admin/dias-cierre/5`, () => {
        borrado = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { usuario } = renderizarApp('/admin/horarios')

    const lista = await screen.findByRole('list', { name: 'Días de cierre' })
    expect(within(lista).getByText('martes, 8 de diciembre de 2026 · Todo el negocio · Festivo')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Día'), { target: { value: '2026-11-20' } })
    await usuario.selectOptions(await screen.findByLabelText('Quién cierra'), '11')
    await usuario.type(screen.getByLabelText(/Motivo/), 'Vacaciones')
    await usuario.click(screen.getByRole('button', { name: 'Añadir día de cierre' }))
    expect(await screen.findByText('Día de cierre añadido: viernes, 20 de noviembre de 2026.')).toBeInTheDocument()
    expect(recibido.cuerpo).toEqual({ fecha: '2026-11-20', empleadoId: 11, motivo: 'Vacaciones' })

    fireEvent.change(screen.getByLabelText('Día'), { target: { value: '2026-11-20' } })
    await usuario.click(screen.getByRole('button', { name: 'Añadir día de cierre' }))
    expect(await screen.findByText('Ya hay un cierre ese día.')).toBeInTheDocument()

    await usuario.click(within(lista).getByRole('button', { name: 'Quitar el cierre del martes, 8 de diciembre de 2026' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, quitar' }))
    expect(await screen.findByText('Día de cierre quitado.')).toBeInTheDocument()
    expect(await screen.findByText('No hay días de cierre próximos.')).toBeInTheDocument()
  })
})

describe('configuración del negocio', () => {
  it('cambiar el intervalo pide confirmación y el nombre nuevo sale en la cabecera', async () => {
    conAdmin()
    const recibido = capturar<Negocio>()
    servidor.use(
      http.put(`${API}/admin/negocio`, async ({ request }) => {
        recibido.cuerpo = (await request.json()) as Negocio
        return HttpResponse.json(recibido.cuerpo)
      }),
    )
    const { usuario } = renderizarApp('/admin/negocio')

    const nombre = await screen.findByLabelText('Nombre del negocio')
    expect(screen.getByLabelText('Zona horaria')).toHaveValue('Europe/Madrid')
    await usuario.clear(nombre)
    await usuario.type(nombre, 'Estudio Nuevo')
    await usuario.selectOptions(screen.getByLabelText('Intervalo de reserva'), '15')
    await usuario.click(screen.getByRole('button', { name: 'Guardar configuración' }))

    expect(screen.getByText('¿Guardar el cambio de intervalo o de zona horaria?')).toBeInTheDocument()
    expect(recibido.cuerpo).toBeUndefined()
    await usuario.click(screen.getByRole('button', { name: 'Sí, guardar' }))

    expect(await screen.findByText('Configuración guardada.')).toBeInTheDocument()
    expect(recibido.cuerpo).toEqual({ ...negocio, nombre: 'Estudio Nuevo', intervaloMinutos: 15 })
    expect(screen.getByRole('link', { name: 'Estudio Nuevo' })).toBeInTheDocument()
  })

  it('valida los números y muestra el 422 del intervalo debajo de su campo', async () => {
    conAdmin()
    servidor.use(
      http.put(`${API}/admin/negocio`, () =>
        HttpResponse.json(problema(422, 'Estos servicios no son múltiplo de 60 minutos: Corte de pelo.'), { status: 422 }),
      ),
    )
    const { usuario } = renderizarApp('/admin/negocio')

    const antelacion = await screen.findByLabelText('Antelación máxima (días)')
    await usuario.clear(antelacion)
    await usuario.type(antelacion, '400')
    await usuario.click(screen.getByRole('button', { name: 'Guardar configuración' }))
    expect(screen.getByText('Entre 1 y 365 días.')).toBeInTheDocument()

    await usuario.clear(antelacion)
    await usuario.type(antelacion, '30')
    await usuario.selectOptions(screen.getByLabelText('Intervalo de reserva'), '60')
    await usuario.click(screen.getByRole('button', { name: 'Guardar configuración' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, guardar' }))

    expect(await screen.findByText('Estos servicios no son múltiplo de 60 minutos: Corte de pelo.')).toBeInTheDocument()
    expect(screen.getByLabelText('Intervalo de reserva')).toHaveAttribute('aria-invalid', 'true')
  })
})
