import { expect, test } from '@playwright/test'

import { ApiAdmin, hoyEnZona, registrarCliente, sumarDias, type Escenario } from '../utilidades/api.ts'
import { abrirHorarios, iniciarSesion, irA } from '../utilidades/web.ts'

// CU-009 y RN-007: el cliente reserva por un menor a su cargo.

let escenario: Escenario

test.beforeAll(async () => {
  const api = await ApiAdmin.abrir()
  escenario = await api.escenario()
  await api.cerrar()
})

test('un cliente da de alta a un menor y le reserva una cita', async ({ page }) => {
  const { servicio, empleado } = escenario
  const cliente = await registrarCliente()
  const fecha = sumarDias(hoyEnZona(), 3)

  await iniciarSesion(page, cliente.email)

  await test.step('da de alta al menor', async () => {
    await irA(page, 'Menores a mi cargo')
    await expect(page.getByText('Todavía no has añadido ningún menor.')).toBeVisible()
    await page.getByRole('button', { name: 'Añadir un menor' }).click()
    const formulario = page.getByRole('form', { name: 'Añadir un menor a cargo' })
    await formulario.getByLabel('Nombre').fill('Lucas')
    await formulario.getByLabel('Apellidos').fill('García Menor')
    await formulario.getByLabel('Fecha de nacimiento').fill(sumarDias(hoyEnZona(), -8 * 365))
    await formulario.getByRole('button', { name: 'Añadir' }).click()
    await expect(page.getByText('Lucas se ha añadido.')).toBeVisible()
    await expect(page.getByText('Lucas García Menor')).toBeVisible()
  })

  await test.step('le reserva una hora', async () => {
    await abrirHorarios(page, servicio.id, empleado.id, fecha)
    await page.getByRole('link', { name: 'Reservar a las 11:00' }).click()
    await page.getByRole('radio', { name: 'Lucas García Menor' }).check()
    await page.getByRole('button', { name: 'Confirmar reserva' }).click()
    await expect(page.getByText('Cita reservada. ¡Te esperamos!')).toBeVisible()
  })

  await test.step('en "Mis citas" sale para quién es', async () => {
    await irA(page, 'Mis citas')
    const cita = page.getByRole('link', { name: new RegExp(servicio.nombre) })
    await expect(cita).toContainText('Para Lucas')
  })
})
