import { expect, test, type Locator, type Page } from '@playwright/test'

import { ApiAdmin, hoyEnZona, registrarCliente, sumarDias, type Escenario } from '../utilidades/api.ts'
import { abrirMenu, iniciarSesion, reservarComoCliente } from '../utilidades/web.ts'

// RNF-008 (5d, H-8): solo en el proyecto `movil` (390 px). Los flujos del cliente
// ya pasan en este tamaño; aquí se comprueba que nada se sale de la pantalla y
// que lo que se toca tiene un tamaño cómodo para el dedo.

/** Medida mínima de toque recomendada (WCAG 2.5.5 y las guías de Apple y Google). */
const TOQUE_MINIMO = 44

let escenario: Escenario

test.beforeAll(async () => {
  const api = await ApiAdmin.abrir()
  escenario = await api.escenario()
  await api.cerrar()
})

async function sinDesplazamientoHorizontal(page: Page, direccion: string) {
  const { ancho, contenido } = await page.evaluate(() => ({
    ancho: window.innerWidth,
    contenido: document.documentElement.scrollWidth,
  }))
  expect(contenido, `${direccion}: el contenido mide ${contenido} px en una pantalla de ${ancho} px`).toBeLessThanOrEqual(ancho)
}

async function tamanoDeToque(elementos: Locator, descripcion: string) {
  await expect(elementos.first(), `no hay ${descripcion}`).toBeVisible()
  const cuantos = await elementos.count()
  for (let i = 0; i < cuantos; i++) {
    const elemento = elementos.nth(i)
    const caja = await elemento.boundingBox()
    const texto = (await elemento.innerText()).trim() || (await elemento.getAttribute('aria-label'))
    expect(caja, `${descripcion} «${texto}» no se ve`).not.toBeNull()
    expect.soft(caja!.height, `${descripcion} «${texto}»: alto`).toBeGreaterThanOrEqual(TOQUE_MINIMO)
    expect.soft(caja!.width, `${descripcion} «${texto}»: ancho`).toBeGreaterThanOrEqual(TOQUE_MINIMO)
  }
}

test('ninguna página se sale de la pantalla del móvil', async ({ page }) => {
  const { servicio, empleado } = escenario
  const fecha = sumarDias(hoyEnZona(), 3)
  const horarios = `/horarios?servicio=${servicio.id}&profesional=${empleado.id}&fecha=${fecha}`

  for (const direccion of ['/', '/servicios', '/profesionales', horarios, '/login', '/registro', '/recuperar-password', '/privacidad']) {
    await page.goto(direccion)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await sinDesplazamientoHorizontal(page, direccion)
  }

  const cliente = await registrarCliente()
  await iniciarSesion(page, cliente.email)
  const detalle = new URL(await reservarComoCliente(page, servicio.id, empleado.id, fecha, '15:00')).pathname
  const reservar = `/reservar?servicio=${servicio.id}&profesional=${empleado.id}&fecha=${fecha}&hora=16:00`

  for (const direccion of ['/mis-citas', detalle, reservar, '/menores', '/perfil', '/cambiar-password']) {
    await page.goto(direccion)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await sinDesplazamientoHorizontal(page, direccion)
  }
})

test('lo que se toca mide al menos 44 × 44 px', async ({ page }) => {
  const { servicio, empleado } = escenario
  const fecha = sumarDias(hoyEnZona(), 3)

  await page.goto('/')
  await tamanoDeToque(page.getByRole('button', { name: 'Abrir menú' }), 'el botón del menú')
  await abrirMenu(page)
  await tamanoDeToque(page.getByRole('navigation', { name: 'Principal' }).getByRole('link'), 'el enlace del menú')

  await page.goto('/login')
  await tamanoDeToque(page.getByRole('main').getByRole('button'), 'el botón')
  await tamanoDeToque(page.getByRole('main').getByRole('textbox'), 'el campo')

  await page.goto(`/horarios?servicio=${servicio.id}&profesional=${empleado.id}&fecha=${fecha}`)
  await tamanoDeToque(page.getByRole('main').getByRole('combobox'), 'el desplegable')
  await tamanoDeToque(page.getByRole('link', { name: /^Reservar a las/ }), 'la hora')
})
