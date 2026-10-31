import { expect, test } from '@playwright/test'

import { ApiAdmin, hoyEnZona, registrarCliente, sumarDias, type Escenario } from '../utilidades/api.ts'
import { ADMIN_EMAIL, ADMIN_PASSWORD } from '../utilidades/entorno.ts'
import { iniciarSesion } from '../utilidades/web.ts'

// CU-019: el admin reserva en nombre de un cliente registrado.

let escenario: Escenario

test.beforeAll(async () => {
  const api = await ApiAdmin.abrir()
  escenario = await api.escenario()
  await api.cerrar()
})

test('el admin reserva una cita para un cliente y la ve en las reservas', async ({ page }) => {
  const { servicio, empleado } = escenario
  const cliente = await registrarCliente(undefined, 'Rosa')
  const nombreCliente = `${cliente.nombre} ${cliente.apellidos}`
  const fecha = sumarDias(hoyEnZona(), 3)

  await iniciarSesion(page, ADMIN_EMAIL, ADMIN_PASSWORD)

  await test.step('elige la hora en los horarios libres', async () => {
    await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Reservas' }).click()
    await page.getByRole('link', { name: 'Nueva reserva' }).click()
    await expect(page.getByRole('heading', { name: 'Horarios libres', level: 1 })).toBeVisible()
    const buscador = page.getByRole('form', { name: 'Buscar horas libres' })
    await buscador.getByLabel('Servicio').selectOption({ label: `${servicio.nombre} · 30 min` })
    await buscador.getByLabel('Profesional').selectOption({ label: empleado.nombrePublico })
    await buscador.getByLabel('Día').fill(fecha)
    await page.getByRole('link', { name: 'Reservar a las 13:00' }).click()
    await expect(page.getByRole('heading', { name: 'Nueva reserva' })).toBeVisible()
  })

  await test.step('busca al cliente y confirma', async () => {
    await page.getByLabel('Buscar cliente').fill(cliente.email)
    await page.getByRole('button', { name: 'Buscar' }).click()
    await page.getByRole('button', { name: `Elegir a ${nombreCliente}` }).click()
    await expect(page.getByRole('radio', { name: `Para ${cliente.nombre}` })).toBeChecked()
    await page.getByRole('button', { name: 'Confirmar reserva' }).click()
    await expect(page.getByText('Reserva creada.')).toBeVisible()
  })

  await test.step('el detalle muestra el cliente y el historial "por Administrador"', async () => {
    await expect(page.getByRole('heading', { name: servicio.nombre, level: 1 })).toBeVisible()
    await expect(page.getByText(nombreCliente)).toBeVisible()
    const historial = page.getByRole('region', { name: 'Historial' })
    await expect(historial).toContainText('Reservada (confirmada) · por Administrador')
  })

  await test.step('la reserva sale en la lista, filtrada por profesional', async () => {
    await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Reservas' }).click()
    await page.getByLabel('Profesional').selectOption({ label: empleado.nombrePublico })
    const reserva = page.getByRole('link', { name: new RegExp(servicio.nombre) })
    await expect(reserva).toContainText(nombreCliente)
    await expect(reserva).toContainText('Confirmada')
  })
})
