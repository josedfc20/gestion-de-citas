import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'

import { ApiError, mensajeDeError } from '../api/errores'

/**
 * Pone debajo de cada campo los errores 400 que devuelve el backend (por
 * ejemplo, "contraseña demasiado común", que el navegador no puede saber) y
 * devuelve el mensaje general, o null si todos los errores tenían su campo.
 */
export function aplicarErroresDelServidor<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  campos: readonly Path<T>[],
): string | null {
  if (!(error instanceof ApiError) || error.errores.length === 0) return mensajeDeError(error)
  let sinCampo = false
  let primero = true
  for (const { campo, mensaje } of error.errores) {
    if ((campos as readonly string[]).includes(campo)) {
      setError(campo as Path<T>, { type: 'servidor', message: mensaje }, { shouldFocus: primero })
      primero = false
    } else {
      sinCampo = true
    }
  }
  return sinCampo ? error.message : null
}
