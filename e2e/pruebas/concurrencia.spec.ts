import { expect, test } from '@playwright/test'

import { ApiAdmin, hoyEnZona, registrarCliente, sumarDias, type Escenario } from '../utilidades/api.ts'
import { iniciarSesion } from '../utilidades/web.ts'

// RNF-003 (5d, H-6): dos clientes, cada uno en su navegador, confirman a la vez
// la misma hora del mismo profesional. El backend ya lo prueba con dos peticiones
// a la vez (ReservasTest); aquí se comprueba que la web lo cuenta bien.

let escenario: Escenario

test.beforeAll(async () => {
  const api = await ApiAdmin.abrir()
  escenario = await api.escenario()
  await api.cerrar()
})

test('dos clientes reservan la misma hora a la vez y solo uno la consigue', async ({ browser }) => {
  const { servicio, empleado } = escenario
  const fecha = sumarDias(hoyEnZona(), 3)
  const hora = '11:30'
  const reserva = `/reservar?servicio=${servicio.id}&profesional=${empleado.id}&fecha=${fecha}&hora=${hora}`

  const paginas = await Promise.all(
    ['Irene', 'Hugo'].map(async (nombre) => {
      const cliente = await registrarCliente(undefined, nombre)
      const page = await (await browser.newContext()).newPage()
      await iniciarSesion(page, cliente.email)
      await page.goto(reserva)
      await expect(page.getByRole('button', { name: 'Confirmar reserva' })).toBeEnabled()
      return page
    }),
  )

  await test.step('los dos pulsan "Confirmar reserva" a la vez', async () => {
    await Promise.all(paginas.map((page) => page.getByRole('button', { name: 'Confirmar reserva' }).click()))
  })

  const reservada = (page: (typeof paginas)[number]) => page.getByText('Cita reservada. ¡Te esperamos!')
  const ocupada = (page: (typeof paginas)[number]) => page.getByText('Ese horario ya no está disponible. Elige otra hora.')

  await test.step('uno ve su cita y el otro, que la hora ya no está libre', async () => {
    await expect(async () => {
      const resultados = await Promise.all(paginas.map(async (page) => ({ ok: await reservada(page).isVisible(), ko: await ocupada(page).isVisible() })))
      expect(resultados.filter((r) => r.ok)).toHaveLength(1)
      expect(resultados.filter((r) => r.ko)).toHaveLength(1)
    }).toPass({ timeout: 10_000 })
  })

  const perdedora = (await reservada(paginas[0]).isVisible()) ? paginas[1] : paginas[0]

  await test.step('el que llegó tarde no puede volver a confirmar y elige otra hora', async () => {
    await expect(perdedora.getByRole('button', { name: 'Confirmar reserva' })).toBeDisabled()
    await perdedora.getByRole('link', { name: 'Elegir otra hora' }).click()
    await expect(perdedora).toHaveURL(/\/horarios\?/)
    await expect(perdedora.getByRole('link', { name: 'Reservar a las 12:00' })).toBeVisible()
    await expect(perdedora.getByRole('link', { name: `Reservar a las ${hora}` })).toHaveCount(0)
  })

  await test.step('en la agenda del profesional solo hay una cita a esa hora', async () => {
    const page = await (await browser.newContext()).newPage()
    await iniciarSesion(page, empleado.email, empleado.password)
    await page.goto(`/empleado/agenda?desde=${fecha}`)
    await expect(page.getByRole('link', { name: new RegExp(`${hora}–12:00 ${servicio.nombre}`) })).toHaveCount(1)
  })
})
