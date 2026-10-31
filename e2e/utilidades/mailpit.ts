// Lee los correos que el backend envía a Mailpit (http://localhost:8025), el
// servidor de correo de pruebas: nada sale a Internet.

import { expect, request } from '@playwright/test'

import { MAILPIT_URL } from './entorno.ts'

interface Resumen {
  ID: string
}

/** Espera al último correo enviado a esa dirección y devuelve su texto. */
export async function ultimoCorreo(para: string): Promise<string> {
  const http = await request.newContext({ baseURL: MAILPIT_URL })
  let id: string | undefined
  await expect(async () => {
    const respuesta = await http.get('/api/v1/search', { params: { query: `to:"${para}"` } })
    expect(respuesta.ok()).toBe(true)
    const { messages } = (await respuesta.json()) as { messages: Resumen[] }
    expect(messages.length).toBeGreaterThan(0)
    id = messages[0].ID
  }).toPass({ timeout: 15_000 })
  const mensaje = await http.get(`/api/v1/message/${id}`)
  const { Text } = (await mensaje.json()) as { Text: string }
  await http.dispose()
  return Text
}
