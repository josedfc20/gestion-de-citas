import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'

import { ApiError } from '../api/errores'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { useSesion } from '../sesion/contexto'
import { rutaInicialDe } from '../sesion/rutas'
import { aplicarErroresDelServidor } from '../utilidades/formularios'
import { useTitulo } from '../utilidades/useTitulo'
import { esquemaCambioPassword, PASSWORD_MAX, PASSWORD_MIN, type DatosFormularioCambioPassword } from './esquemas'
import estilos from './Paginas.module.css'

const CAMPOS = ['passwordActual', 'passwordNueva', 'confirmacionPassword'] as const

/**
 * Cambio de contraseña. Obligatorio mientras el usuario tenga una contraseña
 * temporal (RN-005); el perfil completo (CU-004) llega en el B8.
 */
export function CambiarPassword() {
  useTitulo('Cambiar contraseña')
  const { usuario, cambiarPassword } = useSesion()
  const navigate = useNavigate()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const obligatoria = usuario?.debeCambiarPassword ?? false
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormularioCambioPassword>({
    resolver: zodResolver(esquemaCambioPassword),
    defaultValues: { passwordActual: '', passwordNueva: '', confirmacionPassword: '' },
  })

  async function enviar(datos: DatosFormularioCambioPassword) {
    setErrorGeneral(null)
    try {
      const actualizado = await cambiarPassword(datos)
      navigate(rutaInicialDe(actualizado), { replace: true, state: { passwordCambiada: true } })
    } catch (error) {
      if (error instanceof ApiError && error.status === 422) {
        // "La contraseña actual no es correcta."
        setError('passwordActual', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else {
        setErrorGeneral(aplicarErroresDelServidor(error, setError, CAMPOS))
      }
    }
  }

  return (
    <div className={estilos.formulario}>
      <h1>Cambiar contraseña</h1>
      {obligatoria && (
        <Aviso>
          Estás usando una contraseña temporal. Elige una contraseña nueva para continuar.
        </Aviso>
      )}
      {errorGeneral && <Aviso tipo="error">{errorGeneral}</Aviso>}
      <form onSubmit={handleSubmit(enviar)} noValidate>
        <Campo
          etiqueta={obligatoria ? 'Contraseña temporal' : 'Contraseña actual'}
          type="password"
          autoComplete="current-password"
          error={errors.passwordActual?.message}
          {...register('passwordActual')}
        />
        <Campo
          etiqueta="Contraseña nueva"
          type="password"
          autoComplete="new-password"
          ayuda={`De ${PASSWORD_MIN} a ${PASSWORD_MAX} caracteres.`}
          error={errors.passwordNueva?.message}
          {...register('passwordNueva')}
        />
        <Campo
          etiqueta="Repite la contraseña nueva"
          type="password"
          autoComplete="new-password"
          error={errors.confirmacionPassword?.message}
          {...register('confirmacionPassword')}
        />
        <Boton type="submit" ancho enviando={isSubmitting} textoEnviando="Guardando…">
          Guardar contraseña
        </Boton>
      </form>
    </div>
  )
}
