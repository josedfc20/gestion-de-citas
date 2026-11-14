import { expect, test, type Page } from '@playwright/test'

import { iniciarSesion } from '../utilidades/web.ts'
import { ADMIN_EMAIL, ADMIN_PASSWORD, CLIENTE_DEMO, DEMO_PASSWORD, EMPLEADO_DEMO, diaConMargen } from './entorno.ts'

// Paso 7b: ninguna página rompe la Content-Security-Policy. La CSP no admite scripts
// ni estilos en línea ni nada de otra dirección: si alguien añade un style={{...}},
// un <script> o una librería que inyecte estilos, el navegador lo bloquea y esta
// prueba falla antes de que llegue a producción. Recorre las páginas de los cuatro
// perfiles con los datos de demo.

/** Apunta cada infracción de la CSP que vea el navegador en esta página. */
function vigilarCsp(page: Page): string[] {
  const infracciones: string[] = []
  page.on('console', (mensaje) => {
    if (/Content Security Policy/i.test(mensaje.text())) infracciones.push(`${page.url()}: ${mensaje.text()}`)
  })
  return infracciones
}

/** Abre la página y espera a que termine de cargar, como en accesibilidad.spec.ts. */
async function visitar(page: Page, direccion: string) {
  await page.goto(direccion)
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  await expect(page.getByText(/^Cargando/)).toHaveCount(0)
}

/** Abre el primer enlace de una lista (el detalle de una cita), si lo hay. */
async function visitarPrimero(page: Page, lista: string, enlace: RegExp) {
  await visitar(page, lista)
  const primero = page.getByRole('main').getByRole('link', { name: enlace }).first()
  if ((await primero.count()) === 0) return
  await primero.click()
  await expect(page.getByText(/^Cargando/)).toHaveCount(0)
}

/** Un servicio de la demo y uno de sus profesionales, por la API pública. */
async function servicioYProfesional(page: Page): Promise<{ servicio: number; profesional: number }> {
  const servicios = await (await page.request.get('/api/v1/servicios')).json()
  const servicio = servicios[0].id as number
  const profesionales = await (await page.request.get(`/api/v1/profesionales?servicioId=${servicio}`)).json()
  return { servicio, profesional: profesionales[0].id as number }
}

test('páginas públicas', async ({ page }) => {
  const infracciones = vigilarCsp(page)
  const { servicio, profesional } = await servicioYProfesional(page)
  for (const direccion of [
    '/',
    '/servicios',
    '/profesionales',
    `/horarios?servicio=${servicio}&profesional=${profesional}&fecha=${diaConMargen()}`,
    '/login',
    '/registro',
    '/recuperar-password',
    '/privacidad',
    '/no-existe',
  ]) {
    await visitar(page, direccion)
  }
  expect(infracciones).toEqual([])
})

test('páginas del cliente', async ({ page }) => {
  const infracciones = vigilarCsp(page)
  const { servicio, profesional } = await servicioYProfesional(page)
  await iniciarSesion(page, CLIENTE_DEMO, DEMO_PASSWORD)
  for (const direccion of [
    `/reservar?servicio=${servicio}&profesional=${profesional}&fecha=${diaConMargen()}&hora=18:00`,
    '/menores',
    '/perfil',
    '/cambiar-password',
  ]) {
    await visitar(page, direccion)
  }
  await visitarPrimero(page, '/mis-citas', /.+/)
  expect(infracciones).toEqual([])
})

test('páginas del empleado', async ({ page }) => {
  const infracciones = vigilarCsp(page)
  await iniciarSesion(page, EMPLEADO_DEMO, DEMO_PASSWORD)
  for (const direccion of ['/empleado/agenda', '/empleado/servicios']) {
    await visitar(page, direccion)
  }
  await visitarPrimero(page, '/empleado/citas', /.+/)
  expect(infracciones).toEqual([])
})

test('páginas del admin', async ({ page }) => {
  test.skip(!ADMIN_EMAIL || !ADMIN_PASSWORD, 'Sin PROD_ADMIN_EMAIL y PROD_ADMIN_PASSWORD no se prueban las páginas del admin.')
  const infracciones = vigilarCsp(page)
  const { servicio, profesional } = await servicioYProfesional(page)
  await iniciarSesion(page, ADMIN_EMAIL, ADMIN_PASSWORD)
  for (const direccion of [
    `/admin/reservas/nueva?servicio=${servicio}&profesional=${profesional}&fecha=${diaConMargen()}&hora=18:00`,
    '/admin/servicios',
    '/admin/servicios/nuevo',
    `/admin/servicios/${servicio}`,
    '/admin/empleados',
    '/admin/empleados/nuevo',
    `/admin/empleados/${profesional}`,
    `/admin/empleados/${profesional}/horario`,
    '/admin/horarios',
    '/admin/negocio',
  ]) {
    await visitar(page, direccion)
  }
  await visitarPrimero(page, '/admin/reservas', /.+/)
  expect(infracciones).toEqual([])
})
