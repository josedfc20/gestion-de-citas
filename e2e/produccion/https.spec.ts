import { expect, test } from '@playwright/test'

import { PROD_URL } from './entorno.ts'

// Paso 7b, RNF-004: solo HTTPS, y desde fuera solo se ven la web y /api.

test('HTTP redirige a HTTPS con la misma ruta', async ({ request }) => {
  const http = new URL(PROD_URL)
  http.protocol = 'http:'
  http.port = '' // Caddy escucha HTTP en el 80
  const respuesta = await request.get(`${http.origin}/mis-citas?desde=2026-01-01`, { maxRedirects: 0 })
  expect(respuesta.status()).toBe(308)
  expect(respuesta.headers()['location']).toBe(`https://${new URL(PROD_URL).host}/mis-citas?desde=2026-01-01`)
})

test('Swagger y actuator no se ven desde fuera', async ({ request }) => {
  // Caddy solo reenvía /api/*: el resto lo contesta la web (index.html), nunca el backend.
  for (const direccion of ['/v3/api-docs', '/swagger-ui/index.html', '/actuator/health']) {
    const respuesta = await request.get(direccion)
    expect(respuesta.headers()['content-type'], direccion).toContain('text/html')
    expect(await respuesta.text(), direccion).not.toMatch(/openapi|swagger|"status"/i)
  }
})
