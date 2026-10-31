// API simulada con MSW: las pruebas no necesitan el backend ni Docker.
// Cada prueba puede cambiar una respuesta con servidor.use(...).

import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

import { agendaVacia, negocio, paginaDe, problema, profesionales, servicios } from './datos'

export const API = '/api/v1'

export const manejadoresPorDefecto = [
  http.get(`${API}/negocio`, () => HttpResponse.json(negocio)),
  http.get(`${API}/servicios`, () => HttpResponse.json(servicios)),
  http.get(`${API}/profesionales`, ({ request }) => {
    const servicioId = new URL(request.url).searchParams.get('servicioId')
    const lista = servicioId ? profesionales.filter((p) => p.servicios.some((s) => s.id === Number(servicioId))) : profesionales
    return HttpResponse.json(lista)
  }),
  http.get(`${API}/disponibilidad`, ({ request }) => {
    const fecha = new URL(request.url).searchParams.get('fecha')
    return HttpResponse.json({ fecha, zonaHoraria: 'Europe/Madrid', horas: ['09:00', '09:30', '16:00'], siguienteFechaConHuecos: null })
  }),
  // Zona del cliente (B8): por defecto, sin citas ni menores.
  http.get(`${API}/me/citas`, () => HttpResponse.json(paginaDe([]))),
  http.get(`${API}/me/beneficiarios`, () => HttpResponse.json([])),
  // Zona del empleado (B9a): por defecto, una semana sin citas.
  http.get(`${API}/empleado/agenda`, ({ request }) => {
    const parametros = new URL(request.url).searchParams
    return HttpResponse.json(agendaVacia(parametros.get('desde') ?? '2026-11-02', parametros.get('hasta') ?? '2026-11-08'))
  }),
  http.get(`${API}/empleado/citas`, () => HttpResponse.json(paginaDe([]))),
  http.get(`${API}/empleado/servicios`, () => HttpResponse.json([])),
  // Reservas del admin (B9b): por defecto, sin reservas ni clientes.
  http.get(`${API}/admin/citas`, () => HttpResponse.json(paginaDe([]))),
  http.get(`${API}/admin/clientes`, () => HttpResponse.json([])),
  // Por defecto, sin cookie: visitante.
  http.post(`${API}/auth/refresh`, () => HttpResponse.json(problema(401, 'Sesión no válida.'), { status: 401 })),
  http.post(`${API}/auth/logout`, () => new HttpResponse(null, { status: 204 })),
]

export const servidor = setupServer(...manejadoresPorDefecto)
