// Pasos de la web que repiten varias pruebas.

import { expect, type Page } from '@playwright/test'

import { PASSWORD } from './entorno.ts'

/** Inicia sesión desde /login y espera a que la cabecera muestre "Cerrar sesión". */
export async function iniciarSesion(page: Page, email: string, password = PASSWORD) {
  await page.goto('/login')
  await page.getByLabel('Correo electrónico').fill(email)
  await page.getByLabel('Contraseña', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible()
}

/** Horas libres de un servicio y un profesional ese día, como las enlaza la propia web. */
export async function abrirHorarios(page: Page, servicioId: number, profesionalId: number, fecha: string) {
  await page.goto(`/horarios?servicio=${servicioId}&profesional=${profesionalId}&fecha=${fecha}`)
  await expect(page.getByRole('heading', { name: /horas? libres?/ })).toBeVisible()
}

/** Desde las horas libres, reserva esa hora como cliente; devuelve la dirección del detalle de la cita. */
export async function reservarComoCliente(page: Page, servicioId: number, profesionalId: number, fecha: string, hora: string): Promise<string> {
  await abrirHorarios(page, servicioId, profesionalId, fecha)
  await page.getByRole('link', { name: `Reservar a las ${hora}` }).click()
  await page.getByRole('button', { name: 'Confirmar reserva' }).click()
  await expect(page.getByText('Cita reservada. ¡Te esperamos!')).toBeVisible()
  return page.url()
}
