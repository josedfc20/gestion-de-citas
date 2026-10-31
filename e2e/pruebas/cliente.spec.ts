import { expect, test } from '@playwright/test'

import { ApiAdmin, correoNuevo, hoyEnZona, sumarDias, type Escenario } from '../utilidades/api.ts'
import { PASSWORD } from '../utilidades/entorno.ts'
import { abrirHorarios, cerrarSesion, irA } from '../utilidades/web.ts'

// CU-001 a CU-003, CU-005 a CU-008 y CU-010 a CU-012: el recorrido completo de un cliente.

let escenario: Escenario

test.beforeAll(async () => {
  const api = await ApiAdmin.abrir()
  escenario = await api.escenario()
  await api.cerrar()
})

test('un cliente se registra, reserva una hora libre y la cancela', async ({ page }) => {
  const { servicio, empleado } = escenario
  const email = correoNuevo()
  const fecha = sumarDias(hoyEnZona(), 3)

  await test.step('se registra (CU-001)', async () => {
    await page.goto('/registro')
    await page.getByLabel('Nombre').fill('Marta')
    await page.getByLabel('Apellidos').fill('Pérez Prueba')
    await page.getByLabel('Correo electrónico').fill(email)
    await page.getByLabel('Fecha de nacimiento').fill('1992-04-12')
    await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD)
    await page.getByLabel('Repite la contraseña').fill(PASSWORD)
    await page.getByLabel(/He leído y acepto/).check()
    await page.getByRole('button', { name: 'Crear cuenta' }).click()

    await expect(page.getByText('Cuenta creada. Ya puedes iniciar sesión.')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByLabel('Correo electrónico')).toHaveValue(email)
  })

  await test.step('inicia sesión y llega a "Mis citas" (CU-002)', async () => {
    await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD)
    await page.getByRole('button', { name: 'Iniciar sesión' }).click()
    await expect(page.getByRole('heading', { name: 'Mis citas', level: 1 })).toBeVisible()
    await expect(page.getByText('No tienes citas próximas.')).toBeVisible()
  })

  await test.step('ve el servicio y el profesional (CU-005, CU-006)', async () => {
    await irA(page, 'Servicios')
    await expect(page.getByRole('heading', { name: servicio.nombre })).toBeVisible()
    await irA(page, 'Profesionales')
    await expect(page.getByRole('heading', { name: empleado.nombrePublico })).toBeVisible()
  })

  await test.step('elige servicio, profesional y día en los horarios libres (CU-007)', async () => {
    await irA(page, 'Reservar')
    await expect(page.getByRole('heading', { name: 'Horarios libres', level: 1 })).toBeVisible()
    const buscador = page.getByRole('form', { name: 'Buscar horas libres' })
    await buscador.getByLabel('Servicio').selectOption({ label: `${servicio.nombre} · 30 min` })
    await buscador.getByLabel('Profesional').selectOption({ label: empleado.nombrePublico })
    await buscador.getByLabel('Día').fill(fecha)
    await expect(page.getByRole('link', { name: 'Reservar a las 10:00' })).toBeVisible()
  })

  await test.step('reserva las 10:00 (CU-008)', async () => {
    await page.getByRole('link', { name: 'Reservar a las 10:00' }).click()
    await expect(page.getByRole('heading', { name: 'Reservar cita' })).toBeVisible()
    await page.getByRole('button', { name: 'Confirmar reserva' }).click()
    await expect(page.getByText('Cita reservada. ¡Te esperamos!')).toBeVisible()
    await expect(page.getByText(/a las 10:00/).first()).toBeVisible()
  })

  await test.step('la ve en "Mis citas" y en su detalle (CU-010, CU-011)', async () => {
    await page.goto('/mis-citas')
    const cita = page.getByRole('link', { name: new RegExp(servicio.nombre) })
    await expect(cita).toBeVisible()
    await expect(cita).toContainText('Confirmada')
    await cita.click()
    await expect(page.getByText(/Puedes cancelarla online hasta/)).toBeVisible()
  })

  await test.step('la cancela y la hora vuelve a estar libre (CU-012)', async () => {
    await page.getByRole('button', { name: 'Cancelar cita' }).click()
    await page.getByRole('button', { name: 'Sí, cancelar' }).click()
    await expect(page.getByText('Cita cancelada.')).toBeVisible()

    await abrirHorarios(page, servicio.id, empleado.id, fecha)
    await expect(page.getByRole('link', { name: 'Reservar a las 10:00' })).toBeVisible()
  })

  await test.step('cierra sesión (CU-003)', async () => {
    await cerrarSesion(page)
    await page.goto('/mis-citas')
    await expect(page).toHaveURL(/\/login$/)
  })
})
