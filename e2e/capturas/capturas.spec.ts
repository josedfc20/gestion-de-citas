// Saca las capturas de docs/capturas con la peluquería de demostración (paso 6c).
// Se ejecuta con `npm run capturas`; ver playwright.capturas.config.ts.

import { fileURLToPath } from 'node:url'

import { expect, test, type Page } from '@playwright/test'

import { DEMO_PASSWORD } from './demo.ts'
import { ADMIN_EMAIL, ADMIN_PASSWORD, ZONA } from '../utilidades/entorno.ts'
import { iniciarSesion } from '../utilidades/web.ts'

const CARPETA = fileURLToPath(new URL('../../docs/capturas/', import.meta.url))
const ESCRITORIO = { width: 1280, height: 860 }
const MOVIL = { width: 390, height: 844 }

/** Fecha (AAAA-MM-DD) en la zona del negocio, `dias` después de hoy. */
function dentroDe(dias: number): string {
  const fecha = new Date(Date.now() + dias * 86_400_000)
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA }).format(fecha)
}

/** El próximo miércoles a partir de pasado mañana: todos trabajan y no hay festivos fijos en el horario. */
function proximoMiercoles(): string {
  for (let dias = 2; ; dias++) {
    const fecha = dentroDe(dias)
    if (new Date(`${fecha}T12:00:00Z`).getUTCDay() === 3) return fecha
  }
}

async function capturar(page: Page, nombre: string) {
  // Que no salga nada a medio cargar.
  await expect(page.getByText('Cargando…')).toHaveCount(0)
  await page.screenshot({ path: `${CARPETA}${nombre}.png` })
}

/** Horas libres de Luis para un corte de pelo, eligiendo como lo haría el usuario. */
async function abrirHorariosDeLuis(page: Page, fecha: string) {
  await page.goto('/horarios')
  await page.getByLabel('Servicio').selectOption({ label: 'Corte de pelo · 30 min' })
  await page.getByLabel('Profesional').selectOption({ label: 'Luis G.' })
  await page.getByLabel('Día').fill(fecha)
  await page.getByLabel('Día').blur()
  await expect(page.getByRole('link', { name: /^Reservar a las / }).first()).toBeVisible()
}

test.use({ viewport: ESCRITORIO })

test('visitante: horas libres en claro y en oscuro', async ({ page }) => {
  await abrirHorariosDeLuis(page, proximoMiercoles())
  await capturar(page, 'horarios')
  await page.emulateMedia({ colorScheme: 'dark' })
  await capturar(page, 'horarios-oscuro')
})

test('cliente: reservar, mis citas y detalle', async ({ page }) => {
  await iniciarSesion(page, 'cliente@demo.invalid', DEMO_PASSWORD)
  await abrirHorariosDeLuis(page, proximoMiercoles())
  await page.getByRole('link', { name: 'Reservar a las 10:00' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Reservar cita' })).toBeVisible()
  await capturar(page, 'reservar')

  await page.goto('/mis-citas')
  await expect(page.getByRole('main').getByRole('link', { name: /Lavar y peinar/ }).first()).toBeVisible()
  await capturar(page, 'mis-citas')

  await page.getByRole('main').getByRole('link', { name: /Lavar y peinar/ }).first().click()
  await expect(page.getByRole('button', { name: 'Cancelar cita' })).toBeVisible()
  await capturar(page, 'detalle-cita')
})

test('cliente en el móvil', async ({ page }) => {
  await page.setViewportSize(MOVIL)
  await iniciarSesion(page, 'cliente@demo.invalid', DEMO_PASSWORD)
  await abrirHorariosDeLuis(page, proximoMiercoles())
  await capturar(page, 'movil-horarios')
  await page.goto('/mis-citas')
  await expect(page.getByRole('main').getByRole('link', { name: /Lavar y peinar/ }).first()).toBeVisible()
  await capturar(page, 'movil-mis-citas')
})

test('empleado: agenda y una cita asignada', async ({ page }) => {
  await iniciarSesion(page, 'marta@demo.invalid', DEMO_PASSWORD)
  await page.goto('/empleado/agenda')
  // Las citas de la demo son de esta semana o de la siguiente.
  await expect(page.getByRole('heading', { level: 2 }).first()).toBeVisible()
  if ((await page.getByRole('main').getByRole('link', { name: /Lavar y peinar/ }).count()) === 0) {
    await page.getByRole('button', { name: 'Semana siguiente →' }).click()
  }
  await expect(page.getByRole('main').getByRole('link', { name: /Lavar y peinar/ }).first()).toBeVisible()
  await capturar(page, 'agenda-empleado')

  await page.getByRole('main').getByRole('link', { name: /Lavar y peinar/ }).first().click()
  await expect(page.getByRole('button', { name: 'Cancelar cita' })).toBeVisible()
  await capturar(page, 'empleado-cita')
})

test('administrador: reservas, empleado, horarios y negocio', async ({ page }) => {
  await iniciarSesion(page, ADMIN_EMAIL, ADMIN_PASSWORD)
  await page.goto('/admin/reservas')
  await expect(page.getByRole('heading', { level: 1, name: 'Reservas' })).toBeVisible()
  await expect(page.getByRole('main').getByText('Carmen').first()).toBeVisible()
  await capturar(page, 'admin-reservas')

  await page.goto('/admin/empleados')
  await page.getByRole('main').getByRole('link', { name: /Ana/ }).first().click()
  await expect(page.getByRole('heading', { level: 1, name: /Ana/ })).toBeVisible()
  await capturar(page, 'admin-empleado')

  await page.goto('/admin/horarios')
  await expect(page.getByRole('heading', { level: 1, name: 'Horarios' })).toBeVisible()
  await capturar(page, 'admin-horarios')

  await page.goto('/admin/negocio')
  await expect(page.getByRole('heading', { level: 1, name: 'Configuración del negocio' })).toBeVisible()
  await capturar(page, 'admin-negocio')
})
