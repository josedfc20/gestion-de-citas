import { expect, test } from '@playwright/test'

import { cerrarSesion, iniciarSesion } from '../utilidades/web.ts'
import { CLIENTE_DEMO, DEMO_PASSWORD, diaConMargen } from './entorno.ts'

// Paso 7b: por HTTPS y a través de Caddy, la cookie Secure del refresh viaja y la
// sesión funciona de principio a fin. Usa el cliente de la demo y deja la cita
// cancelada (en producción, la demo además se restablece cada noche).

test('el cliente de la demo entra, sigue dentro al recargar, reserva, cancela y sale', async ({ page, context }) => {
  await test.step('inicia sesión y la cookie del refresh es segura', async () => {
    await iniciarSesion(page, CLIENTE_DEMO, DEMO_PASSWORD)
    const cookie = (await context.cookies()).find((c) => c.name === 'refresh_token')
    expect(cookie).toMatchObject({ secure: true, httpOnly: true, sameSite: 'Strict', path: '/api/v1/auth' })
  })

  await test.step('al recargar sigue con la sesión', async () => {
    await page.goto('/mis-citas')
    await page.reload()
    await expect(page.getByRole('heading', { name: 'Mis citas', level: 1 })).toBeVisible()
  })

  let detalle = ''
  await test.step('reserva la primera hora libre', async () => {
    const servicios = await (await page.request.get('/api/v1/servicios')).json()
    const servicio = servicios[0].id as number
    const profesionales = await (await page.request.get(`/api/v1/profesionales?servicioId=${servicio}`)).json()
    const profesional = profesionales[0].id as number
    const libres = await (
      await page.request.get(`/api/v1/disponibilidad?servicioId=${servicio}&profesionalId=${profesional}&fecha=${diaConMargen()}`)
    ).json()
    const fecha = libres.horas.length > 0 ? libres.fecha : libres.siguienteFechaConHuecos
    expect(fecha, 'la demo debería tener horas libres').toBeTruthy()

    await page.goto(`/horarios?servicio=${servicio}&profesional=${profesional}&fecha=${fecha}`)
    await page.getByRole('link', { name: /^Reservar a las / }).first().click()
    await page.getByRole('button', { name: 'Confirmar reserva' }).click()
    await expect(page.getByText('Cita reservada. ¡Te esperamos!')).toBeVisible()
    detalle = page.url()
  })

  await test.step('la cancela', async () => {
    await page.goto(detalle)
    await page.getByRole('button', { name: 'Cancelar cita' }).click()
    await page.getByRole('button', { name: 'Sí, cancelar' }).click()
    await expect(page.getByText('Cancelada').first()).toBeVisible()
  })

  await test.step('cierra sesión y la cookie desaparece', async () => {
    await cerrarSesion(page)
    expect((await context.cookies()).find((c) => c.name === 'refresh_token')).toBeUndefined()
  })
})
