import { expect, test, type Page } from '@playwright/test'

import { ApiAdmin, hoyEnZona, registrarCliente, sumarDias, type Escenario } from '../utilidades/api.ts'
import { ADMIN_EMAIL, ADMIN_PASSWORD } from '../utilidades/entorno.ts'
import { cerrarSesion, iniciarSesion, reservarComoCliente } from '../utilidades/web.ts'

// Seguridad básica (5d), vista desde el navegador y a través del proxy de la web.
// Las cabeceras de la propia web (CSP, HSTS) dependen del servidor de producción: fase 7.

let escenario: Escenario

test.beforeAll(async () => {
  const api = await ApiAdmin.abrir()
  escenario = await api.escenario()
  await api.cerrar()
})

/** Todo lo que el navegador guarda en localStorage y sessionStorage, como texto. */
function almacenamiento(page: Page): Promise<string> {
  return page.evaluate(() => JSON.stringify({ local: { ...localStorage }, sesion: { ...sessionStorage } }))
}

test('la sesión vive en una cookie que JavaScript no puede leer (paso 3.4)', async ({ page, context }) => {
  const cliente = await registrarCliente()
  await iniciarSesion(page, cliente.email)

  await test.step('la cookie del refresh es HttpOnly, Secure, SameSite=Strict y solo para /auth', async () => {
    const cookie = (await context.cookies()).find((c) => c.name === 'refresh_token')
    expect(cookie, 'no hay cookie refresh_token').toBeDefined()
    expect(cookie).toMatchObject({ httpOnly: true, secure: true, sameSite: 'Strict', path: '/api/v1/auth' })
  })

  await test.step('ni document.cookie ni el almacenamiento del navegador tienen tokens', async () => {
    expect(await page.evaluate(() => document.cookie)).not.toContain('refresh_token')
    // Un JWT empieza por "eyJ" (el {"alg"... en Base64).
    expect(await almacenamiento(page)).not.toMatch(/eyJ/)
  })

  await test.step('al recargar, la sesión vuelve con la cookie y sigue sin guardarse nada', async () => {
    await page.reload()
    await expect(page.getByRole('heading', { name: 'Mis citas', level: 1 })).toBeVisible()
    expect(await almacenamiento(page)).not.toMatch(/eyJ/)
  })

  await test.step('cerrar sesión borra la cookie', async () => {
    await cerrarSesion(page)
    expect((await context.cookies()).find((c) => c.name === 'refresh_token')).toBeUndefined()
  })
})

test('el backend manda las cabeceras de seguridad', async ({ request }) => {
  // Por el proxy de la web, como las pide el navegador.
  const respuesta = await request.get('/api/v1/negocio')
  expect(respuesta.ok()).toBe(true)
  const cabeceras = respuesta.headers()
  expect(cabeceras['x-content-type-options']).toBe('nosniff')
  expect(cabeceras['x-frame-options']).toBe('DENY')
  expect(cabeceras['cache-control']).toContain('no-store')
})

test('ninguna respuesta de la API lleva contraseñas ni hashes (RNF-001)', async ({ browser }) => {
  const { servicio, empleado } = escenario
  const fecha = sumarDias(hoyEnZona(), 3)
  const fallos: string[] = []
  let revisadas = 0

  async function nuevaPagina() {
    const page = await (await browser.newContext()).newPage()
    page.on('response', async (respuesta) => {
      if (!respuesta.url().includes('/api/') || !(respuesta.headers()['content-type'] ?? '').includes('json')) return
      const texto = await respuesta.text().catch(() => '')
      revisadas++
      if (/\$2[aby]\$|\$argon2/.test(texto)) fallos.push(`${respuesta.url()}: lleva un hash de contraseña`)
      if (/"[^"]*hash[^"]*"\s*:/i.test(texto)) fallos.push(`${respuesta.url()}: lleva un campo con "hash"`)
      if (/"password"\s*:/i.test(texto)) fallos.push(`${respuesta.url()}: lleva un campo "password"`)
    })
    return page
  }

  await test.step('recorrido del cliente', async () => {
    const page = await nuevaPagina()
    const cliente = await registrarCliente()
    await iniciarSesion(page, cliente.email)
    await reservarComoCliente(page, servicio.id, empleado.id, fecha, '19:00')
    for (const direccion of ['/mis-citas', '/menores', '/perfil']) {
      await page.goto(direccion)
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    }
  })

  await test.step('recorrido del empleado', async () => {
    const page = await nuevaPagina()
    await iniciarSesion(page, empleado.email, empleado.password)
    for (const direccion of [`/empleado/agenda?desde=${fecha}`, '/empleado/citas', '/empleado/servicios', '/perfil']) {
      await page.goto(direccion)
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    }
  })

  await test.step('recorrido del admin', async () => {
    const page = await nuevaPagina()
    await iniciarSesion(page, ADMIN_EMAIL, ADMIN_PASSWORD)
    for (const direccion of ['/admin/reservas', '/admin/empleados', `/admin/empleados/${empleado.id}`, '/admin/servicios', '/admin/horarios', '/admin/negocio']) {
      await page.goto(direccion)
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
      await expect(page.getByText(/^Cargando/)).toHaveCount(0)
    }
  })

  expect(revisadas, 'no se ha revisado ninguna respuesta').toBeGreaterThan(20)
  expect(fallos).toEqual([])
})
