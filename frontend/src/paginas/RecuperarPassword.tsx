import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'

import { recuperarPassword } from '../api/auth'
import { ApiError } from '../api/errores'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { aplicarErroresDelServidor } from '../utilidades/formularios'
import { useTitulo } from '../utilidades/useTitulo'
import { esquemaRecuperarPassword, type DatosRecuperarPassword } from './esquemas'
import estilos from './Paginas.module.css'

/**
 * CU-020, paso 1. El servidor responde lo mismo exista o no el correo, así que
 * la web tampoco dice si la cuenta existe.
 */
export function RecuperarPassword() {
  useTitulo('Recuperar contraseña')
  const [enviado, setEnviado] = useState(false)
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosRecuperarPassword>({ resolver: zodResolver(esquemaRecuperarPassword), defaultValues: { email: '' } })

  async function enviar({ email }: DatosRecuperarPassword) {
    setErrorGeneral(null)
    try {
      await recuperarPassword(email)
      setEnviado(true)
    } catch (error) {
      if (error instanceof ApiError && error.status === 429) {
        setErrorGeneral('Has pedido demasiados enlaces. Espera un rato antes de volver a probar.')
      } else {
        setErrorGeneral(aplicarErroresDelServidor(error, setError, ['email']))
      }
    }
  }

  return (
    <div className={estilos.formulario}>
      <h1>Recuperar contraseña</h1>
      {enviado ? (
        <Aviso tipo="exito">
          <p>Si el correo está registrado, recibirás un enlace en unos minutos. Revisa también la carpeta de correo no deseado.</p>
          <p>El enlace caduca en 30 minutos y solo sirve una vez.</p>
        </Aviso>
      ) : (
        <>
          <p className={estilos.suave}>Escribe el correo de tu cuenta y te enviaremos un enlace para elegir una contraseña nueva.</p>
          {errorGeneral && <Aviso tipo="error">{errorGeneral}</Aviso>}
          <form onSubmit={handleSubmit(enviar)} noValidate>
            <Campo etiqueta="Correo electrónico" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
            <Boton type="submit" ancho enviando={isSubmitting} textoEnviando="Enviando…">
              Enviar enlace
            </Boton>
          </form>
        </>
      )}
      <p className={estilos.pieFormulario}>
        <Link to="/login">Volver a iniciar sesión</Link>
      </p>
    </div>
  )
}
