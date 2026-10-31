// Direcciones y cuentas de las pruebas de punta a punta (las usan la configuración y las pruebas).

export const API_URL = 'http://localhost:8080'
export const WEB_URL = 'http://localhost:4173'
export const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? 'http://localhost:8025'

/** Administrador propio de las pruebas: lo crea el backend al arrancar (la base de datos e2e solo se usa aquí). */
export const ADMIN_EMAIL = 'admin-e2e@pruebas.local'
export const ADMIN_PASSWORD = 'Admin-de-las-pruebas-e2e'

/** Contraseña de los clientes y empleados que crean las pruebas. */
export const PASSWORD = 'Clave-de-prueba-e2e-2026'

/** Zona del negocio (la de V2): las horas de la web y de la API van en ella. */
export const ZONA = 'Europe/Madrid'
