// Pruebas de producción (paso 7b): a qué dirección van y con qué cuentas.
// Guía: docs/guias/desplegar.md y docs/guias/pruebas-e2e.md.
//
//   PROD_URL              dirección de la web (por defecto https://localhost, el Compose de producción en el PC)
//   PROD_DEMO_PASSWORD    contraseña de las cuentas de demo (DEMO_PASSWORD de .env.prod)
//   PROD_ADMIN_EMAIL      administrador (ADMIN_EMAIL de .env.prod); sin él se saltan las páginas del admin
//   PROD_ADMIN_PASSWORD   su contraseña (ADMIN_PASSWORD)

import { hoyEnZona, sumarDias } from '../utilidades/api.ts'

export const PROD_URL = (process.env.PROD_URL ?? 'https://localhost').replace(/\/$/, '')

/** Con localhost el certificado es de Caddy (tls internal), no de una autoridad conocida. */
export const CERTIFICADO_PROPIO = new URL(PROD_URL).hostname === 'localhost'

export const DEMO_PASSWORD = process.env.PROD_DEMO_PASSWORD ?? ''
export const ADMIN_EMAIL = process.env.PROD_ADMIN_EMAIL ?? ''
export const ADMIN_PASSWORD = process.env.PROD_ADMIN_PASSWORD ?? ''

/** Cuentas que crea el perfil `demo` (DatosDemo). */
export const CLIENTE_DEMO = 'cliente@demo.invalid'
export const EMPLEADO_DEMO = 'luis@demo.invalid'

/** La CSP de la web (frontend/Caddyfile). Si cambia allí, cambia aquí. */
export const CSP =
  "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; font-src 'self'; " +
  "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'"

/** Un día con margen para poder cancelar (RN-004: 24 horas antes). */
export function diaConMargen(): string {
  return sumarDias(hoyEnZona(), 3)
}
