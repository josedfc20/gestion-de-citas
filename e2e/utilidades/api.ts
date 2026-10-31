// Preparar datos por la API, sin pasar por la web: es más rápido y deja que cada
// prueba use la web solo para el flujo que comprueba. Todo lleva nombres y correos
// únicos, así las pruebas no dependen del orden y pueden ir en paralelo.

import { randomUUID } from 'node:crypto'

import { expect, request, type APIRequestContext } from '@playwright/test'

import { ADMIN_EMAIL, ADMIN_PASSWORD, API_URL, PASSWORD, ZONA } from './entorno.ts'

export function unico(base: string): string {
  return `${base} ${randomUUID().slice(0, 6)}`
}

export function correoNuevo(prefijo = 'cliente'): string {
  return `${prefijo}-${randomUUID().slice(0, 8)}@pruebas.local`
}

/** Fecha de hoy en la zona del negocio (AAAA-MM-DD), no en la del ordenador que ejecuta las pruebas. */
export function hoyEnZona(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
}

/** Lunes = 1 … domingo = 7, como en la API y en el editor de horarios. */
export function diaSemana(fecha: string): number {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return new Date(Date.UTC(anio, mes - 1, dia)).getUTCDay() || 7
}

export function sumarDias(fecha: string, dias: number): string {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return new Date(Date.UTC(anio, mes - 1, dia + dias)).toISOString().slice(0, 10)
}

export interface Empleado {
  id: number
  email: string
  password: string
  /** Como sale en el panel del admin: nombre y apellidos. */
  nombreCompleto: string
  /** Como sale en la web pública: nombre e inicial del apellido. */
  nombrePublico: string
}

export interface Escenario {
  servicio: { id: number; nombre: string }
  empleado: Empleado
}

/** Cliente de la API con la sesión del administrador de las pruebas. */
export class ApiAdmin {
  private constructor(
    private readonly http: APIRequestContext,
    private readonly token: string,
  ) {}

  static async abrir(): Promise<ApiAdmin> {
    const http = await request.newContext({ baseURL: `${API_URL}/api/v1/` })
    const sesion = await iniciarSesion(http, ADMIN_EMAIL, ADMIN_PASSWORD)
    return new ApiAdmin(http, sesion)
  }

  async cerrar() {
    await this.http.dispose()
  }

  private async enviar<T>(metodo: 'POST' | 'PUT' | 'PATCH', ruta: string, cuerpo: unknown): Promise<T> {
    const respuesta = await this.http.fetch(ruta, {
      method: metodo,
      headers: { Authorization: `Bearer ${this.token}` },
      data: cuerpo,
    })
    expect(respuesta.ok(), `${metodo} ${ruta}: ${await respuesta.text()}`).toBe(true)
    return (await respuesta.json()) as T
  }

  async crearServicio(nombre = unico('Corte'), duracionMinutos = 30, precio = 15): Promise<{ id: number; nombre: string }> {
    const creado = await this.enviar<{ id: number }>('POST', 'admin/servicios', { nombre, duracionMinutos, precio })
    return { id: creado.id, nombre }
  }

  /** Alta con contraseña temporal; con `definitiva`, además la cambia (RN-005) para que pueda entrar ya. */
  async crearEmpleado(servicioIds: number[], { definitiva = true } = {}): Promise<Empleado> {
    const email = correoNuevo('empleado')
    const nombre = unico('Pro').replace(' ', '')
    const creado = await this.enviar<{ empleado: { id: number }; passwordTemporal: string }>('POST', 'admin/empleados', {
      nombre,
      apellidos: 'Prueba',
      email,
      servicioIds,
    })
    let password = creado.passwordTemporal
    if (definitiva) {
      await cambiarPassword(email, password, PASSWORD)
      password = PASSWORD
    }
    return { id: creado.empleado.id, email, password, nombreCompleto: `${nombre} Prueba`, nombrePublico: `${nombre} P.` }
  }

  /** De lunes a domingo, de 09:00 a 20:00: siempre hay horas libres, sea el día que sea. */
  async horarioCompleto(empleadoId: number) {
    const dias = [1, 2, 3, 4, 5, 6, 7].map((diaSemana) => ({ diaSemana, tramos: [{ inicio: '09:00', fin: '20:00' }] }))
    await this.enviar('PUT', `admin/empleados/${empleadoId}/horario`, { dias })
  }

  /** Un servicio de 30 min y un profesional que lo hace todos los días. */
  async escenario(): Promise<Escenario> {
    const servicio = await this.crearServicio()
    const empleado = await this.crearEmpleado([servicio.id])
    await this.horarioCompleto(empleado.id)
    return { servicio, empleado }
  }
}

async function iniciarSesion(http: APIRequestContext, email: string, password: string): Promise<string> {
  const respuesta = await http.post('auth/login', { data: { email, password } })
  expect(respuesta.ok(), `login de ${email}: ${await respuesta.text()}`).toBe(true)
  return ((await respuesta.json()) as { accessToken: string }).accessToken
}

async function cambiarPassword(email: string, actual: string, nueva: string) {
  const http = await request.newContext({ baseURL: `${API_URL}/api/v1/` })
  const token = await iniciarSesion(http, email, actual)
  const respuesta = await http.put('me/password', {
    headers: { Authorization: `Bearer ${token}` },
    data: { passwordActual: actual, passwordNueva: nueva, confirmacionPassword: nueva },
  })
  expect(respuesta.ok(), await respuesta.text()).toBe(true)
  await http.dispose()
}

/** Registra un cliente adulto por la API (el registro por la web lo prueba cliente.spec.ts). */
export async function registrarCliente(email = correoNuevo(), nombre = 'Ana'): Promise<{ email: string; nombre: string; apellidos: string }> {
  const http = await request.newContext({ baseURL: `${API_URL}/api/v1/` })
  const respuesta = await http.post('auth/registro', {
    data: {
      nombre,
      apellidos: 'García Prueba',
      email,
      telefono: '600123123',
      fechaNacimiento: '1990-05-20',
      password: PASSWORD,
      confirmacionPassword: PASSWORD,
      aceptaPrivacidad: true,
    },
  })
  expect(respuesta.status(), await respuesta.text()).toBe(201)
  await http.dispose()
  return { email, nombre, apellidos: 'García Prueba' }
}
