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
