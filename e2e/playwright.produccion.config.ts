import { defineConfig, devices } from '@playwright/test'

import { CERTIFICADO_PROPIO, DEMO_PASSWORD, PROD_URL } from './produccion/entorno.ts'

// Pruebas de producción (paso 7b): la web tal como la sirve Caddy, por HTTPS, con
// sus cabeceras. No arranca nada: prueba una aplicación ya desplegada, por defecto
// el Compose de producción del PC (https://localhost). En el 7f, la dirección pública.
//
//   cd e2e && PROD_DEMO_PASSWORD=... npm run produccion
//
// La CI las pasa en el trabajo "Producción" contra docker-compose.prod.yml.

if (!DEMO_PASSWORD) {
  throw new Error('Falta PROD_DEMO_PASSWORD: la contraseña de las cuentas de demo (DEMO_PASSWORD de .env.prod).')
}

const enCI = Boolean(process.env.CI)

export default defineConfig({
  testDir: './produccion',
  fullyParallel: true,
  forbidOnly: enCI,
  // Sin reintentos, como las demás: una prueba que falla a veces es un defecto.
  retries: 0,
  workers: enCI ? 2 : undefined,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: enCI ? [['list'], ['html', { open: 'never', outputFolder: 'playwright-report-produccion' }]] : [['list']],
  outputDir: 'test-results-produccion',
  use: {
    baseURL: PROD_URL,
    // Solo con localhost: el certificado de una dirección pública tiene que ser válido.
    ignoreHTTPSErrors: CERTIFICADO_PROPIO,
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'produccion', use: { ...devices['Desktop Chrome'] } }],
})
