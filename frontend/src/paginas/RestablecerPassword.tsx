import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useLocation, useNavigate } from 'react-router'

import { restablecerPassword } from '../api/auth'
import { ApiError } from '../api/errores'
import { Aviso } from '../componentes/Aviso'
import { Boton, EnlaceBoton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { useSesion } from '../sesion/contexto'
import type { EstadoLogin } from '../sesion/rutas'
import { aplicarErroresDelServidor } from '../utilidades/formularios'
import { useTitulo } from '../utilidades/useTitulo'
import { esquemaRestablecerPassword, PASSWORD_MAX, PASSWORD_MIN, type DatosFormularioRestablecer } from './esquemas'
import estilos from './Paginas.module.css'

/** "#token=abc" → "abc". El token va detrás de "#" para que no llegue al servidor web ni a sus registros (B3). */
function tokenDe(hash: string): string | null {
  const token = new URLSearchParams(hash.replace(/^#/, '')).get('token')
  return token ? token : null
}

/** CU-020, paso 4: contraseña nueva con el enlace del correo. */
export function RestablecerPassword() {
  useTitulo('Elegir contraseña nueva')
  const location = useLocation()
  const navigate = useNavigate()
  const { usuario, cerrarSesionLocal } = useSesion()
  // Se lee una sola vez: enseguida se borra de la dirección.
  const [token] = useState(() => tokenDe(location.hash))
  const [enlaceNoValido, setEnlaceNoValido] = useState<string | null>(token ? null : 'El enlace no es válido.')
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormularioRestablecer>({
    resolver: zodResolver(esquemaRestablecerPassword),
    defaultValues: { password: '', confirmacionPassword: '' },
  })

  // El token no se queda en la barra de direcciones ni en el historial.
  useEffect(() => {
    if (location.hash) navigate({ pathname: location.pathname, search: location.search }, { replace: true })
  }, [location.hash, location.pathname, location.search, navigate])

  async function enviar(datos: DatosFormularioRestablecer) {
    if (!token) return
    setErrorGeneral(null)
    try {
      await restablecerPassword({ token, ...datos })
      // El servidor ha cerrado todas las sesiones, también la de esta pestaña si la había.
      if (usuario) cerrarSesionLocal()
      const estado: EstadoLogin = { aviso: 'restablecida' }
      navigate('/login', { replace: true, state: estado })
    } catch (error) {
      if (error instanceof ApiError && error.status === 422) {
        // "El enlace no es válido o ha caducado. Solicita uno nuevo."
        setEnlaceNoValido(error.message)
      } else {
        setErrorGeneral(aplicarErroresDelServidor(error, setError, ['password', 'confirmacionPassword']))
      }
    }
  }

  return (
    <div className={estilos.formulario}>
      <h1>Elegir contraseña nueva</h1>
      {enlaceNoValido ? (
        <>
          <Aviso tipo="error">{enlaceNoValido}</Aviso>
          <EnlaceBoton to="/recuperar-password" ancho>
            Pedir otro enlace
          </EnlaceBoton>
        </>
      ) : (
        <>
          {errorGeneral && <Aviso tipo="error">{errorGeneral}</Aviso>}
          <form onSubmit={handleSubmit(enviar)} noValidate>
            <Campo
              etiqueta="Contraseña nueva"
              type="password"
              autoComplete="new-password"
              ayuda={`De ${PASSWORD_MIN} a ${PASSWORD_MAX} caracteres.`}
              error={errors.password?.message}
              {...register('password')}
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
        </>
      )}
    </div>
  )
}
