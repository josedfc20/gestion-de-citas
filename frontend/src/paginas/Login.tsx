import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation } from 'react-router'

import { ApiError } from '../api/errores'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { useSesion } from '../sesion/contexto'
import type { EstadoLogin } from '../sesion/rutas'
import { aplicarErroresDelServidor } from '../utilidades/formularios'
import { useTitulo } from '../utilidades/useTitulo'
import { esquemaLogin, type DatosLogin } from './esquemas'
import estilos from './Paginas.module.css'

const AVISOS: Record<NonNullable<EstadoLogin['aviso']>, { tipo: 'info' | 'exito'; texto: string }> = {
  caducada: { tipo: 'info', texto: 'Tu sesión ha caducado. Vuelve a iniciar sesión para continuar.' },
  registrado: { tipo: 'exito', texto: 'Cuenta creada. Ya puedes iniciar sesión.' },
  restablecida: { tipo: 'exito', texto: 'Contraseña cambiada. Ya puedes iniciar sesión con la nueva.' },
}

/**
 * CU-002. Al entrar, SoloSinSesion lleva al usuario a la página de la que venía
 * (o a la de su rol, o a cambiar la contraseña temporal).
 */
export function Login() {
  useTitulo('Iniciar sesión')
  const { iniciarSesion } = useSesion()
  const estado = (useLocation().state as EstadoLogin | null) ?? {}
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosLogin>({
    resolver: zodResolver(esquemaLogin),
    defaultValues: { email: estado.email ?? '', password: '' },
  })

  async function enviar(datos: DatosLogin) {
    setErrorGeneral(null)
    try {
      await iniciarSesion(datos.email, datos.password)
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setErrorGeneral('Correo o contraseña no válidos.')
      } else if (error instanceof ApiError && error.status === 429) {
        setErrorGeneral('Demasiados intentos. Espera un minuto antes de volver a probar.')
      } else {
        setErrorGeneral(aplicarErroresDelServidor(error, setError, ['email', 'password']))
      }
    }
  }

  const aviso = estado.aviso ? AVISOS[estado.aviso] : null

  return (
    <div className={estilos.formulario}>
      <h1>Iniciar sesión</h1>
      {aviso && !errorGeneral && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      {errorGeneral && <Aviso tipo="error">{errorGeneral}</Aviso>}
      <form onSubmit={handleSubmit(enviar)} noValidate>
        <Campo etiqueta="Correo electrónico" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <Campo etiqueta="Contraseña" type="password" autoComplete="current-password" error={errors.password?.message} {...register('password')} />
        <Boton type="submit" ancho enviando={isSubmitting} textoEnviando="Entrando…">
          Iniciar sesión
        </Boton>
      </form>
      <p className={estilos.pieFormulario}>
        <Link to="/recuperar-password">¿Has olvidado la contraseña?</Link>
      </p>
      <p className={estilos.pieFormulario}>
        ¿No tienes cuenta? <Link to="/registro">Regístrate</Link>
      </p>
    </div>
  )
}
