// Reglas de formato de los formularios, las mismas que valida el backend
// (RegistroRequest, LoginRequest y CambioPasswordRequest). Las reglas de
// negocio (correo repetido, contraseña muy común...) las decide el servidor.

import { z } from 'zod'

import { edadEn } from '../utilidades/fechas'

export const PASSWORD_MIN = 10
export const PASSWORD_MAX = 64

const correo = z
  .string()
  .trim()
  .min(1, 'Escribe tu correo electrónico.')
  .max(254, 'El correo es demasiado largo.')
  .pipe(z.email('Escribe un correo electrónico válido.'))

const passwordNueva = z
  .string()
  .min(PASSWORD_MIN, `Debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres.`)
  .max(PASSWORD_MAX, `Debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres.`)

export const esquemaLogin = z.object({
  email: z.string().trim().min(1, 'Escribe tu correo electrónico.').max(254),
  password: z.string().min(1, 'Escribe tu contraseña.').max(200),
})
export type DatosLogin = z.infer<typeof esquemaLogin>

export function esquemaRegistro(hoy: string, edadMinima: number) {
  return z
    .object({
      nombre: z.string().trim().min(1, 'Escribe tu nombre.').max(80, 'Como mucho 80 caracteres.'),
      apellidos: z.string().trim().min(1, 'Escribe tus apellidos.').max(120, 'Como mucho 120 caracteres.'),
      email: correo,
      telefono: z
        .string()
        .trim()
        .refine((valor) => valor === '' || /^\+?[0-9 ]{6,19}$/.test(valor), 'Teléfono no válido: solo números, espacios y un + inicial.'),
      fechaNacimiento: z
        .string()
        .min(1, 'Indica tu fecha de nacimiento.')
        .refine((valor) => /^\d{4}-\d{2}-\d{2}$/.test(valor) && valor < hoy, 'La fecha debe ser anterior a hoy.')
        .refine((valor) => !/^\d{4}-\d{2}-\d{2}$/.test(valor) || valor >= hoy || edadEn(valor, hoy) >= edadMinima, `Debes tener ${edadMinima} años o más para crear una cuenta.`),
      password: passwordNueva,
      confirmacionPassword: z.string().min(1, 'Repite la contraseña.'),
      aceptaPrivacidad: z.boolean().refine((acepta) => acepta, 'Debes aceptar la política de privacidad.'),
    })
    .refine((datos) => datos.password === datos.confirmacionPassword, {
      path: ['confirmacionPassword'],
      message: 'Las contraseñas no coinciden.',
    })
}
export type DatosFormularioRegistro = z.infer<ReturnType<typeof esquemaRegistro>>

export const esquemaCambioPassword = z
  .object({
    passwordActual: z.string().min(1, 'Escribe tu contraseña actual.'),
    passwordNueva,
    confirmacionPassword: z.string().min(1, 'Repite la contraseña nueva.'),
  })
  .refine((datos) => datos.passwordNueva === datos.confirmacionPassword, {
    path: ['confirmacionPassword'],
    message: 'Las contraseñas no coinciden.',
  })
export type DatosFormularioCambioPassword = z.infer<typeof esquemaCambioPassword>

const FECHA_ISO = /^\d{4}-\d{2}-\d{2}$/

/**
 * Menor a cargo (BeneficiarioRequest). Que sea menor de la edad mínima lo
 * comprueba también el servidor (422), aquí solo se avisa antes.
 */
export function esquemaBeneficiario(hoy: string, edadMinima: number) {
  return z.object({
    nombre: z.string().trim().min(1, 'Escribe su nombre.').max(80, 'Como mucho 80 caracteres.'),
    apellidos: z.string().trim().min(1, 'Escribe sus apellidos.').max(120, 'Como mucho 120 caracteres.'),
    fechaNacimiento: z
      .string()
      .min(1, 'Indica su fecha de nacimiento.')
      .refine((valor) => FECHA_ISO.test(valor) && valor <= hoy, 'La fecha no puede ser futura.')
      .refine(
        (valor) => !FECHA_ISO.test(valor) || valor > hoy || edadEn(valor, hoy) < edadMinima,
        `Con ${edadMinima} años o más necesita su propia cuenta.`,
      ),
  })
}
export type DatosFormularioBeneficiario = z.infer<ReturnType<typeof esquemaBeneficiario>>

export const MOTIVO_MAX = 300

const telefono = z
  .string()
  .trim()
  .refine((valor) => valor === '' || /^\+?[0-9 ]{6,19}$/.test(valor), 'Teléfono no válido: solo números, espacios y un + inicial.')

/** Perfil (ActualizarPerfilRequest): las mismas reglas que el registro. */
export const esquemaPerfil = z.object({
  nombre: z.string().trim().min(1, 'Escribe tu nombre.').max(80, 'Como mucho 80 caracteres.'),
  apellidos: z.string().trim().min(1, 'Escribe tus apellidos.').max(120, 'Como mucho 120 caracteres.'),
  email: correo,
  telefono,
})
export type DatosFormularioPerfil = z.infer<typeof esquemaPerfil>

export const esquemaEliminarCuenta = z.object({
  password: z.string().min(1, 'Escribe tu contraseña para confirmar.'),
})
export type DatosEliminarCuenta = z.infer<typeof esquemaEliminarCuenta>

export const esquemaRecuperarPassword = z.object({ email: correo })
export type DatosRecuperarPassword = z.infer<typeof esquemaRecuperarPassword>

export const esquemaRestablecerPassword = z
  .object({
    password: passwordNueva,
    confirmacionPassword: z.string().min(1, 'Repite la contraseña nueva.'),
  })
  .refine((datos) => datos.password === datos.confirmacionPassword, {
    path: ['confirmacionPassword'],
    message: 'Las contraseñas no coinciden.',
  })
export type DatosFormularioRestablecer = z.infer<typeof esquemaRestablecerPassword>

/** Cancelar como empleado o admin (CU-015, CU-019): el motivo es obligatorio. */
export const esquemaCancelacionGestion = z.object({
  motivo: z.string().trim().min(1, 'Indica el motivo de la cancelación.').max(MOTIVO_MAX, `Como máximo ${MOTIVO_MAX} caracteres.`),
})

// ---- Panel del administrador (B9c). Los campos numéricos se guardan como texto
// en el formulario y se convierten al enviar. ----

/** "15", "15,5" o "15.50" → 15.5; null si no es un precio válido (0 a 9999,99 con dos decimales). */
export function precioDeTexto(texto: string): number | null {
  const limpio = texto.trim().replace(',', '.')
  if (!/^\d{1,4}(\.\d{1,2})?$/.test(limpio)) return null
  return Number(limpio)
}

/** ServicioRequest. RN-011 la asegura la lista de duraciones; el servidor la vuelve a comprobar (422). */
export const esquemaServicio = z.object({
  nombre: z.string().trim().min(1, 'Escribe el nombre del servicio.').max(120, 'Como mucho 120 caracteres.'),
  descripcion: z.string().trim().max(1000, 'Como mucho 1000 caracteres.'),
  duracionMinutos: z.string().min(1, 'Elige la duración.'),
  precio: z.string().refine((valor) => precioDeTexto(valor) !== null, 'Precio no válido: de 0 a 9999,99 con dos decimales como mucho.'),
})
export type DatosFormularioServicio = z.infer<typeof esquemaServicio>

/** DatosEmpleadoRequest y AltaEmpleadoRequest (sin los servicios, que van en casillas). */
export const esquemaEmpleado = z.object({
  nombre: z.string().trim().min(1, 'Escribe el nombre.').max(80, 'Como mucho 80 caracteres.'),
  apellidos: z.string().trim().min(1, 'Escribe los apellidos.').max(120, 'Como mucho 120 caracteres.'),
  email: correo,
  telefono,
})
export type DatosFormularioEmpleado = z.infer<typeof esquemaEmpleado>

/** DiaCierreRequest. Que no sea pasado lo comprueba el servidor (422) en la zona del negocio. */
export const esquemaDiaCierre = z.object({
  fecha: z.string().min(1, 'Elige el día.'),
  empleadoId: z.string(),
  motivo: z.string().trim().max(200, 'Como mucho 200 caracteres.'),
})
export type DatosFormularioDiaCierre = z.infer<typeof esquemaDiaCierre>

export const INTERVALOS = [5, 10, 15, 20, 30, 60] as const

const entero = (minimo: number, maximo: number, mensaje: string) =>
  z.string().refine((valor) => /^\d+$/.test(valor.trim()) && Number(valor) >= minimo && Number(valor) <= maximo, mensaje)

/** NegocioRequest. */
export const esquemaNegocio = z.object({
  nombre: z.string().trim().min(1, 'Escribe el nombre del negocio.').max(120, 'Como mucho 120 caracteres.'),
  zonaHoraria: z.string().min(1, 'Elige la zona horaria.').max(50),
  intervaloMinutos: z.string().refine((valor) => INTERVALOS.some((intervalo) => String(intervalo) === valor), 'Elige un intervalo.'),
  antelacionMaxDias: entero(1, 365, 'Entre 1 y 365 días.'),
  horasMinCancelacion: entero(0, 168, 'Entre 0 y 168 horas (una semana).'),
  edadMinimaCuenta: entero(14, 18, 'Entre 14 y 18 años.'),
})
export type DatosFormularioNegocio = z.infer<typeof esquemaNegocio>
