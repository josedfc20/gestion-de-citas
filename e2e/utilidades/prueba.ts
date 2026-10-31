import { test as base, type Page } from '@playwright/test'

// D-4 (registro de defectos): cuando una prueba falla, adjunta las peticiones a
// la API que hizo cada página, con su estado y lo que tardaron. Así se ve si la
// web no llegó a llamar, si el backend respondió con un error o si tardó demasiado.
// Las pruebas importan `test` y `expect` de aquí en lugar de '@playwright/test'.
//
// Con E2E_CPU_LENTA=<n> (por ejemplo 20), Chromium va n veces más lento. Sirve
// para sacar a la luz las prisas de las pruebas (D-4) en el PC, sin esperar a
// que la CI vaya cargada. Guía: docs/guias/pruebas-e2e.md

interface Peticion {
  inicio: number
  metodo: string
  url: string
  resultado?: string
  milisegundos?: number
}

const CPU_LENTA = Number(process.env.E2E_CPU_LENTA ?? 0)

async function frenar(page: Page) {
  if (CPU_LENTA <= 1) return
  const cdp = await page.context().newCDPSession(page)
  await cdp.send('Emulation.setCPUThrottlingRate', { rate: CPU_LENTA })
}

function registrar(page: Page, peticiones: Peticion[]) {
  const abiertas = new Map<object, Peticion>()
  page.on('request', (peticion) => {
    if (!peticion.url().includes('/api/')) return
    const entrada: Peticion = { inicio: Date.now(), metodo: peticion.method(), url: new URL(peticion.url()).pathname }
    abiertas.set(peticion, entrada)
    peticiones.push(entrada)
  })
  page.on('requestfinished', async (peticion) => {
    const entrada = abiertas.get(peticion)
    if (!entrada) return
    const respuesta = await peticion.response().catch(() => null)
    entrada.milisegundos = Date.now() - entrada.inicio
    entrada.resultado = respuesta ? String(respuesta.status()) : 'sin respuesta'
    if (respuesta && respuesta.status() >= 400) {
      entrada.resultado += ` ${(await respuesta.text().catch(() => '')).slice(0, 300)}`
    }
  })
  page.on('requestfailed', (peticion) => {
    const entrada = abiertas.get(peticion)
    if (!entrada) return
    entrada.milisegundos = Date.now() - entrada.inicio
    entrada.resultado = `fallo de red: ${peticion.failure()?.errorText ?? '?'}`
  })
}

export const test = base.extend<{ peticionesApi: void }>({
  peticionesApi: [
    async ({ context }, usar, testInfo) => {
      const peticiones: Peticion[] = []
      for (const page of context.pages()) {
        registrar(page, peticiones)
        await frenar(page)
      }
      context.on('page', (page) => {
        registrar(page, peticiones)
        void frenar(page)
      })
      await usar()
      if (testInfo.status !== testInfo.expectedStatus && peticiones.length > 0) {
        const origen = peticiones[0].inicio
        const lineas = peticiones.map(
          (p) => `+${String(p.inicio - origen).padStart(6)} ms  ${p.metodo.padEnd(6)} ${p.url}  →  ${p.resultado ?? 'SIN TERMINAR'}${p.milisegundos === undefined ? '' : ` (${p.milisegundos} ms)`}`,
        )
        await testInfo.attach('peticiones-api', { body: lineas.join('\n'), contentType: 'text/plain' })
      }
    },
    { auto: true },
  ],
})

export { expect, type Locator, type Page } from '@playwright/test'
