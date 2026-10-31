import { expect, test } from '@playwright/test'

import { ApiAdmin, diaSemana, hoyEnZona, sumarDias, type Escenario } from '../utilidades/api.ts'
import { ADMIN_EMAIL, ADMIN_PASSWORD } from '../utilidades/entorno.ts'
import { abrirHorarios, iniciarSesion, irA } from '../utilidades/web.ts'

// CU-018, RN-002 y CU-007. Solo cambia el horario y los cierres de su propio
// empleado, nunca el horario general: las demás pruebas van en paralelo.

const NOMBRE_DIA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']

let escenario: Escenario

test.beforeAll(async () => {
  const api = await ApiAdmin.abrir()
  escenario = await api.escenario()
  await api.cerrar()
})

test('el horario propio y un día de cierre cambian las horas libres', async ({ page }) => {
  const { servicio, empleado } = escenario
  const fecha = sumarDias(hoyEnZona(), 3)
  const dia = NOMBRE_DIA[diaSemana(fecha)]

  await iniciarSesion(page, ADMIN_EMAIL, ADMIN_PASSWORD)

  await test.step('antes, ese día trabaja desde las 09:00', async () => {
    await abrirHorarios(page, servicio.id, empleado.id, fecha)
    await expect(page.getByRole('link', { name: 'Reservar a las 09:00' })).toBeVisible()
  })

  await test.step('el admin le pone ese día de 16:00 a 18:00 (CU-018)', async () => {
    await page.goto(`/admin/empleados/${empleado.id}/horario`)
    await expect(page.getByRole('heading', { name: `Horario de ${empleado.nombreCompleto}` })).toBeVisible()
    await page.getByLabel(`${dia}, tramo 1: inicio`).fill('16:00')
    await page.getByLabel(`${dia}, tramo 1: fin`).fill('18:00')
    await page.getByRole('button', { name: 'Guardar horario' }).click()
    await expect(page.getByText('Horario guardado.')).toBeVisible()
    await expect(page.getByText(`${dia}: 16:00–18:00 (horario propio)`)).toBeVisible()
  })

  await test.step('las horas libres de ese día empiezan a las 16:00 (RN-002)', async () => {
    await abrirHorarios(page, servicio.id, empleado.id, fecha)
    await expect(page.getByRole('link', { name: 'Reservar a las 16:00' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Reservar a las 09:00' })).toHaveCount(0)
    await expect(page.getByRole('link', { name: 'Reservar a las 18:00' })).toHaveCount(0)
  })

  await test.step('el admin cierra ese día para el empleado', async () => {
    await irA(page, 'Horarios')
    await page.getByLabel('Día', { exact: true }).fill(fecha)
    await page.getByLabel('Quién cierra').selectOption({ label: empleado.nombreCompleto })
    await page.getByLabel(/^Motivo/).fill('Vacaciones')
    await page.getByRole('button', { name: 'Añadir día de cierre' }).click()
    await expect(page.getByText(/^Día de cierre añadido/)).toBeVisible()
    await expect(page.getByRole('list', { name: 'Días de cierre' })).toContainText(`${empleado.nombreCompleto} · Vacaciones`)
  })

  await test.step('ese día no hay horas y se ofrece el siguiente (CU-007)', async () => {
    await page.goto(`/horarios?servicio=${servicio.id}&profesional=${empleado.id}&fecha=${fecha}`)
    await expect(page.getByText(/^No hay horas libres el/)).toBeVisible()
    await page.getByRole('button', { name: /^Ver el siguiente día con horas/ }).click()
    await expect(page).toHaveURL(new RegExp(`fecha=${sumarDias(fecha, 1)}`))
    await expect(page.getByRole('link', { name: 'Reservar a las 09:00' })).toBeVisible()
  })
})
