import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

import { ApiAdmin, hoyEnZona, registrarCliente, sumarDias, type Escenario } from '../utilidades/api.ts'
import { ADMIN_EMAIL, ADMIN_PASSWORD } from '../utilidades/entorno.ts'
import { iniciarSesion, reservarComoCliente } from '../utilidades/web.ts'

// RNF-008 (5d, H-8): axe revisa cada página con las reglas WCAG 2.1 A y AA, en
// modo claro y oscuro (decisión 3 del 5d). Fallan los fallos graves y críticos;
// los moderados y leves se anotan en el informe. Lo que axe no ve (el orden del
// foco, que se entienda) va en las pruebas manuales del 5e.

const REGLAS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']
const GRAVES = new Set(['serious', 'critical'])

interface Datos {
  escenario: Escenario
  fecha: string
  cliente: { email: string }
  citaId: number
}

let datos: Datos

// Una cita real para que las páginas de detalle, la agenda y las reservas tengan contenido.
test.beforeAll(async ({ browser }) => {
  const api = await ApiAdmin.abrir()
  const escenario = await api.escenario()
  await api.cerrar()
  const cliente = await registrarCliente()
  const fecha = sumarDias(hoyEnZona(), 3)
  const contexto = await browser.newContext()
  const page = await contexto.newPage()
  await iniciarSesion(page, cliente.email)
  const detalle = await reservarComoCliente(page, escenario.servicio.id, escenario.empleado.id, fecha, '17:00')
  await contexto.close()
  datos = { escenario, fecha, cliente, citaId: Number(/\/mis-citas\/(\d+)/.exec(detalle)?.[1]) }
})

/** Abre la página, espera a que cargue y comprueba que axe no encuentra fallos graves. */
async function revisar(page: Page, direccion: string) {
  await page.goto(direccion)
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  await expect(page.getByText(/^Cargando/)).toHaveCount(0)

  const { violations } = await new AxeBuilder({ page }).withTags(REGLAS).analyze()
  const graves = violations.filter((fallo) => GRAVES.has(fallo.impact ?? ''))
  for (const fallo of violations.filter((v) => !GRAVES.has(v.impact ?? ''))) {
    test.info().annotations.push({ type: `axe (${fallo.impact})`, description: `${direccion}: ${fallo.id} · ${fallo.help}` })
  }
  const resumen = graves.map((fallo) => `${fallo.id} (${fallo.impact}): ${fallo.help} · ${fallo.helpUrl}\n    ${fallo.nodes.map((nodo) => nodo.target.join(' ')).join('\n    ')}`)
  expect.soft(resumen, `${direccion}\n  ${resumen.join('\n  ')}`).toEqual([])
}

for (const colorScheme of ['light', 'dark'] as const) {
  test.describe(`modo ${colorScheme === 'light' ? 'claro' : 'oscuro'}`, () => {
    test.use({ colorScheme })

    test('páginas públicas', async ({ page }) => {
      const { escenario, fecha } = datos
      for (const direccion of [
        '/',
        '/servicios',
        '/profesionales',
        '/horarios',
        `/horarios?servicio=${escenario.servicio.id}&profesional=${escenario.empleado.id}&fecha=${fecha}`,
        '/login',
        '/registro',
        '/recuperar-password',
        '/restablecer-password',
        '/privacidad',
        '/no-existe',
      ]) {
        await revisar(page, direccion)
      }
    })

    test('páginas del cliente', async ({ page }) => {
      const { escenario, fecha, cliente, citaId } = datos
      await iniciarSesion(page, cliente.email)
      for (const direccion of [
        '/mis-citas',
        `/mis-citas/${citaId}`,
        `/reservar?servicio=${escenario.servicio.id}&profesional=${escenario.empleado.id}&fecha=${fecha}&hora=18:00`,
        '/menores',
        '/perfil',
        '/cambiar-password',
      ]) {
        await revisar(page, direccion)
      }
    })

    test('páginas del empleado', async ({ page }) => {
      const { escenario, fecha, citaId } = datos
      await iniciarSesion(page, escenario.empleado.email, escenario.empleado.password)
      for (const direccion of [`/empleado/agenda?desde=${fecha}`, '/empleado/citas', `/empleado/citas/${citaId}`, '/empleado/servicios']) {
        await revisar(page, direccion)
      }
    })

    test('páginas del admin', async ({ page }) => {
      const { escenario, fecha, citaId } = datos
      await iniciarSesion(page, ADMIN_EMAIL, ADMIN_PASSWORD)
      for (const direccion of [
        '/admin/reservas',
        `/admin/reservas/${citaId}`,
        `/admin/reservas/nueva?servicio=${escenario.servicio.id}&profesional=${escenario.empleado.id}&fecha=${fecha}&hora=18:00`,
        '/admin/servicios',
        '/admin/servicios/nuevo',
        `/admin/servicios/${escenario.servicio.id}`,
        '/admin/empleados',
        '/admin/empleados/nuevo',
        `/admin/empleados/${escenario.empleado.id}`,
        `/admin/empleados/${escenario.empleado.id}/horario`,
        '/admin/horarios',
        '/admin/negocio',
      ]) {
        await revisar(page, direccion)
      }
    })
  })
}
