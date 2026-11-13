import { execFileSync } from 'node:child_process'

import { defineConfig } from '@playwright/test'

import { DEMO_PASSWORD } from './capturas/demo.ts'
import base from './playwright.config.ts'

// Capturas de la web para el README y el manual (paso 6c). No son pruebas: la
// CI no las ejecuta. Usa la configuración de las E2E con dos cambios: el perfil
// `demo` (la peluquería de ejemplo) y su propia base de datos, que se vacía al
// empezar porque los datos de demo solo se crean con la base vacía.
//
//   cd e2e && npm run capturas      → docs/capturas/*.png

const BD = 'gestion_de_citas_capturas'
const CONTENEDOR = process.env.CAPTURAS_CONTENEDOR ?? 'gestion-de-citas-db'
const USUARIO_BD = process.env.POSTGRES_USER ?? 'gestion_de_citas'

// Antes de arrancar el backend: base de datos vacía en el PostgreSQL de docker compose.
// Solo en el proceso principal: los workers también leen este archivo y la borrarían
// con el backend ya en marcha.
if (!process.env.TEST_WORKER_INDEX) {
  execFileSync('docker', ['exec', CONTENEDOR, 'dropdb', '-U', USUARIO_BD, '--if-exists', BD], { stdio: 'inherit' })
  execFileSync('docker', ['exec', CONTENEDOR, 'createdb', '-U', USUARIO_BD, BD], { stdio: 'inherit' })
}

const [backend, web] = Array.isArray(base.webServer) ? base.webServer : []

export default defineConfig({
  ...base,
  testDir: './capturas',
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    ...base.use,
    trace: 'off',
    video: 'off',
    screenshot: 'off',
    // Los campos de fecha y hora de Chromium siguen el idioma del sistema, no el de la
    // página: sin esto, en un Linux en inglés salen "02:00 PM" y "11/11/2026" al revés.
    launchOptions: { env: { ...process.env, LANG: 'es_ES.UTF-8', LANGUAGE: 'es' } },
  },
  projects: [{ name: 'capturas' }],
  webServer: [
    {
      ...backend,
      env: {
        ...backend.env,
        DB_URL: `jdbc:postgresql://localhost:5433/${BD}`,
        SPRING_PROFILES_ACTIVE: 'demo',
        DEMO_PASSWORD,
      },
    },
    web,
  ],
})
