import { expect, test } from '../utilidades/prueba.ts'

import { registrarCliente } from '../utilidades/api.ts'
import { PASSWORD, WEB_URL } from '../utilidades/entorno.ts'
import { ultimoCorreo } from '../utilidades/mailpit.ts'
import { cerrarSesion, iniciarSesion, pulsarEnlace } from '../utilidades/web.ts'

// CU-020: recuperar la contraseña con el enlace del correo (Mailpit hace de buzón).

test('un cliente recupera la contraseña con el enlace del correo', async ({ page }) => {
  const cliente = await registrarCliente(undefined, 'Pablo')
  const nueva = 'Otra-clave-de-prueba-2026'

  await test.step('pide el enlace', async () => {
    await page.goto('/login')
    await pulsarEnlace(page, '¿Has olvidado la contraseña?', 'Recuperar contraseña')
    await page.getByLabel('Correo electrónico').fill(cliente.email)
    await page.getByRole('button', { name: 'Enviar enlace' }).click()
    await expect(page.getByText(/Si el correo está registrado, recibirás un enlace/)).toBeVisible()
  })

  let enlace = ''
  await test.step('el correo trae el enlace a la web', async () => {
    const texto = await ultimoCorreo(cliente.email)
    expect(texto).toContain(`Hola, ${cliente.nombre}:`)
    enlace = new RegExp(`${WEB_URL}/restablecer-password#token=\\S+`).exec(texto)?.[0] ?? ''
    expect(enlace).not.toBe('')
  })

  await test.step('elige una contraseña nueva', async () => {
    await page.goto(enlace)
    await expect(page.getByRole('heading', { name: 'Elegir contraseña nueva' })).toBeVisible()
    // El token no se queda en la barra de direcciones.
    await expect(page).toHaveURL(/\/restablecer-password$/)
    await page.getByLabel('Contraseña nueva', { exact: true }).fill(nueva)
    await page.getByLabel('Repite la contraseña nueva').fill(nueva)
    await page.getByRole('button', { name: 'Guardar contraseña' }).click()
    await expect(page.getByText('Contraseña cambiada. Ya puedes iniciar sesión con la nueva.')).toBeVisible()
  })

  await test.step('la antigua ya no sirve y la nueva sí', async () => {
    await page.getByLabel('Correo electrónico').fill(cliente.email)
    await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD)
    await page.getByRole('button', { name: 'Iniciar sesión' }).click()
    await expect(page.getByText('Correo o contraseña no válidos.')).toBeVisible()

    await iniciarSesion(page, cliente.email, nueva)
    await expect(page.getByRole('heading', { name: 'Mis citas', level: 1 })).toBeVisible()
  })

  await test.step('el enlace solo sirve una vez', async () => {
    await cerrarSesion(page)
    await page.goto(enlace)
    await page.getByLabel('Contraseña nueva', { exact: true }).fill(PASSWORD)
    await page.getByLabel('Repite la contraseña nueva').fill(PASSWORD)
    await page.getByRole('button', { name: 'Guardar contraseña' }).click()
    await expect(page.getByText('El enlace no es válido o ha caducado. Solicita uno nuevo.')).toBeVisible()
  })
})
