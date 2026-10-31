// Pasos de la web que repiten varias pruebas.

import { expect, type Page } from '@playwright/test'

import { PASSWORD } from './entorno.ts'

/**
 * En el móvil (proyecto `movil`, 390 px) el menú está plegado detrás del botón
 * "Abrir menú"; en el escritorio ese botón no se ve y no hace falta.
 */
export async function abrirMenu(page: Page) {
  const boton = page.getByRole('button', { name: 'Abrir menú' })
  if (await boton.isVisible()) await boton.click()
}

/** Un enlace del menú principal, como lo pulsaría el usuario en cualquier tamaño de pantalla. */
export async function irA(page: Page, enlace: string) {
  await abrirMenu(page)
  await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: enlace, exact: true }).click()
}

export async function cerrarSesion(page: Page) {
  await abrirMenu(page)
  await page.getByRole('button', { name: 'Cerrar sesión' }).click()
  await expect(page.getByRole('link', { name: 'Iniciar sesión', includeHidden: true })).toBeAttached()
}

/** Comprueba que no hay sesión: en la cabecera (aunque esté plegada) sale "Iniciar sesión". */
export async function esperarSinSesion(page: Page) {
  await expect(page.getByRole('link', { name: 'Iniciar sesión', includeHidden: true })).toBeAttached()
}

/** Inicia sesión desde /login y espera a que la cabecera tenga "Cerrar sesión" (plegada o no). */
export async function iniciarSesion(page: Page, email: string, password = PASSWORD) {
  await page.goto('/login')
  await page.getByLabel('Correo electrónico').fill(email)
  await page.getByLabel('Contraseña', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await expect(page.getByRole('button', { name: 'Cerrar sesión', includeHidden: true })).toBeAttached()
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
