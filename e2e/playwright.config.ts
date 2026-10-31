import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import { defineConfig, devices } from '@playwright/test'

import { ADMIN_EMAIL, ADMIN_PASSWORD, API_URL, WEB_URL } from './utilidades/entorno.ts'

// Pruebas de punta a punta (fase 5, paso 5c): la web y el backend de verdad,
// contra un PostgreSQL y un Mailpit que ya están en marcha (docker compose en el
// PC, servicios de GitHub Actions en la CI). Guía: docs/guias/pruebas-e2e.md

const raiz = fileURLToPath(new URL('..', import.meta.url))

/** Lee el .env de la raíz (solo existe en el PC) sin pisar lo que ya venga del entorno. */
function leerEnv(): Record<string, string> {
  const ruta = `${raiz}.env`
  if (!existsSync(ruta)) return {}
  const valores: Record<string, string> = {}
  for (const linea of readFileSync(ruta, 'utf8').split(/\r?\n/)) {
    const coincide = /^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/.exec(linea)
    if (coincide) valores[coincide[1]] = coincide[2]
  }
  return valores
}

const env = { ...leerEnv(), ...process.env }
const enCI = Boolean(process.env.CI)

/** Variables del backend: todas explícitas, para que el .env de desarrollo no cuele su base de datos. */
const entornoBackend: Record<string, string> = {
  // En el PC, una base de datos aparte en el mismo contenedor (decisión 3 del 5c).
  DB_URL: env.E2E_DB_URL ?? 'jdbc:postgresql://localhost:5433/gestion_de_citas_e2e',
  POSTGRES_USER: env.POSTGRES_USER ?? 'gestion_de_citas',
  POSTGRES_PASSWORD: env.POSTGRES_PASSWORD ?? '',
  JWT_SECRET: 'secreto-solo-para-las-pruebas-e2e-de-gestion-de-citas',
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  ADMIN_NOMBRE: 'Administrador',
  MAIL_HOST: 'localhost',
  MAIL_PORT: '1025',
  MAIL_USERNAME: '',
  MAIL_PASSWORD: '',
  MAIL_FROM: 'no-responder@gestiondecitas.local',
  APP_WEB_URL: WEB_URL,
  CORS_ALLOWED_ORIGINS: WEB_URL,
  COOKIE_SECURE: 'true',
  // Todas las pruebas entran desde 127.0.0.1: sin esto chocarían con el límite de 5 por minuto
  // (decisión 1 del 5c). El límite ya lo prueba LimiteIntentosLoginTest en el backend.
  APP_SECURITY_INTENTOSLOGINPORMINUTO: '1000',
  // Nunca los datos de demo, aunque el .env de desarrollo (que el backend también lee) active ese perfil.
  SPRING_PROFILES_ACTIVE: 'default',
}

export default defineConfig({
  testDir: './pruebas',
  fullyParallel: true,
  forbidOnly: enCI,
  // Sin reintentos: una prueba que falla a veces es un defecto (plan de pruebas, §4).
  retries: 0,
  workers: enCI ? 2 : undefined,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: enCI ? [['list'], ['html', { open: 'never' }]] : [['list'], ['html', { open: 'on-failure' }]],
  use: {
    baseURL: WEB_URL,
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  // Solo Chromium (decisión 2 del 5c).
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // Siempre arranca los suyos (nunca reutiliza): un backend de desarrollo en el 8080
  // usaría otra base de datos y otro administrador. Si el puerto está ocupado, avisa.
  webServer: [
    {
      name: 'backend',
      command: 'java -jar ../backend/target/gestion-de-citas-backend-0.1.0-SNAPSHOT.jar',
      url: `${API_URL}/actuator/health`,
      env: entornoBackend,
      timeout: 120_000,
      reuseExistingServer: false,
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      name: 'web',
      // La versión de producción, con el proxy de /api de vite preview (vite.config.ts).
      command: 'npm --prefix ../frontend run build && npm --prefix ../frontend run preview -- --strictPort',
      url: WEB_URL,
      env: { VITE_API_PROXY: API_URL },
      timeout: 120_000,
      reuseExistingServer: false,
    },
  ],
})
