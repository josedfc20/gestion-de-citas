import { expect, test, type APIResponse } from '@playwright/test'

import { CSP } from './entorno.ts'

// Paso 7b: las cabeceras de seguridad que pone Caddy (frontend/Caddyfile) en la
// web y en la API, y la caché de los archivos de la web.

function comprobarSeguridad(respuesta: APIResponse, direccion: string) {
  const cabeceras = respuesta.headers()
  expect(cabeceras['strict-transport-security'], direccion).toBe('max-age=31536000')
  expect(cabeceras['content-security-policy'], direccion).toBe(CSP)
  expect(cabeceras['x-content-type-options'], direccion).toBe('nosniff')
  expect(cabeceras['referrer-policy'], direccion).toBe('strict-origin-when-cross-origin')
  expect(cabeceras['permissions-policy'], direccion).toBe('camera=(), microphone=(), geolocation=(), payment=(), usb=()')
  expect(cabeceras['cross-origin-opener-policy'], direccion).toBe('same-origin')
  // No se anuncia qué servidor hay.
  expect(cabeceras['server'], direccion).toBeUndefined()
  expect(cabeceras['via'], direccion).toBeUndefined()
}

test('la web lleva las cabeceras de seguridad y no se guarda en caché', async ({ request }) => {
  for (const direccion of ['/', '/mis-citas', '/admin/reservas']) {
    const respuesta = await request.get(direccion)
    expect(respuesta.status(), direccion).toBe(200)
    comprobarSeguridad(respuesta, direccion)
    // index.html sin caché: una versión nueva se ve al momento.
    expect(respuesta.headers()['cache-control'], direccion).toBe('no-cache')
  }
})

test('la API lleva las cabeceras de seguridad y las suyas propias', async ({ request }) => {
  const respuesta = await request.get('/api/v1/negocio')
  expect(respuesta.status()).toBe(200)
  comprobarSeguridad(respuesta, '/api/v1/negocio')
  // Las del backend se conservan (5d).
  expect(respuesta.headers()['cache-control']).toContain('no-store')
  expect(respuesta.headers()['x-frame-options']).toBe('DENY')
})

test('los archivos de /assets se guardan un año', async ({ request }) => {
  const html = await (await request.get('/')).text()
  const archivos = [...html.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g)].map((coincidencia) => coincidencia[1])
  expect(archivos.length).toBeGreaterThan(0)
  for (const archivo of archivos) {
    const respuesta = await request.get(archivo)
    expect(respuesta.status(), archivo).toBe(200)
    expect(respuesta.headers()['cache-control'], archivo).toBe('public, max-age=31536000, immutable')
    comprobarSeguridad(respuesta, archivo)
  }
})
