import { readFile } from 'node:fs/promises'

import { expect, test } from '@playwright/test'

import { registrarCliente } from '../utilidades/api.ts'
import { PASSWORD } from '../utilidades/entorno.ts'
import { iniciarSesion } from '../utilidades/web.ts'

// CU-004 y RNF-007: descargar los datos personales y eliminar la cuenta.

test('un cliente descarga sus datos y elimina su cuenta', async ({ page }) => {
  const cliente = await registrarCliente(undefined, 'Elena')

  await iniciarSesion(page, cliente.email)
  await page.goto('/perfil')
  await expect(page.getByRole('heading', { name: 'Mi perfil', level: 1 })).toBeVisible()

  await test.step('descarga sus datos en JSON, sin la contraseña', async () => {
    const descarga = page.waitForEvent('download')
    await page.getByRole('button', { name: 'Descargar mis datos' }).click()
    const archivo = await descarga
    expect(archivo.suggestedFilename()).toBe('mis-datos.json')
    const texto = await readFile(await archivo.path(), 'utf8')
    const datos: unknown = JSON.parse(texto)
    expect(datos).toBeInstanceOf(Object)
    expect(texto).toContain(cliente.email)
    expect(texto).toContain(cliente.apellidos)
    // Ni el hash de la contraseña (BCrypt empieza por $2) ni los de las sesiones.
    expect(texto).not.toMatch(/\$2[aby]\$/)
    expect(texto.toLowerCase()).not.toContain('hash')
    expect(texto.toLowerCase()).not.toContain('password')
  })

  await test.step('elimina su cuenta', async () => {
    await page.getByRole('button', { name: 'Quiero eliminar mi cuenta' }).click()
    await page.getByLabel('Escribe tu contraseña para confirmar').fill(PASSWORD)
    await page.getByRole('button', { name: 'Eliminar mi cuenta' }).click()
    await expect(page.getByText('Tu cuenta se ha eliminado.')).toBeVisible()
    await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible()
  })

  await test.step('ya no puede iniciar sesión', async () => {
    await page.goto('/login')
    await page.getByLabel('Correo electrónico').fill(cliente.email)
    await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD)
    await page.getByRole('button', { name: 'Iniciar sesión' }).click()
    await expect(page.getByText('Correo o contraseña no válidos.')).toBeVisible()
  })
})
