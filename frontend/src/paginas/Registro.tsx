import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'

import { registrar } from '../api/auth'
import { consultaNegocio } from '../api/catalogo'
import { ApiError } from '../api/errores'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo, Casilla } from '../componentes/Campo'
import type { EstadoLogin } from '../sesion/rutas'
import { hoyEnZona } from '../utilidades/fechas'
import { aplicarErroresDelServidor } from '../utilidades/formularios'
import { useTitulo } from '../utilidades/useTitulo'
import { esquemaRegistro, PASSWORD_MAX, PASSWORD_MIN, type DatosFormularioRegistro } from './esquemas'
import estilos from './Paginas.module.css'

/** Valores por defecto de V2 mientras llega la configuración del negocio. */
const EDAD_MINIMA_POR_DEFECTO = 14
const ZONA_POR_DEFECTO = 'Europe/Madrid'

const CAMPOS = ['nombre', 'apellidos', 'email', 'telefono', 'fechaNacimiento', 'password', 'confirmacionPassword', 'aceptaPrivacidad'] as const

/** CU-001. El registro no inicia sesión: al terminar lleva al login. */
export function Registro() {
  useTitulo('Crear cuenta')
  const navigate = useNavigate()
  const { data: negocio } = useQuery(consultaNegocio)
  const edadMinima = negocio?.edadMinimaCuenta ?? EDAD_MINIMA_POR_DEFECTO
  const hoy = hoyEnZona(negocio?.zonaHoraria ?? ZONA_POR_DEFECTO)
  const esquema = useMemo(() => esquemaRegistro(hoy, edadMinima), [hoy, edadMinima])
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormularioRegistro>({
    resolver: zodResolver(esquema),
    defaultValues: {
      nombre: '',
      apellidos: '',
      email: '',
      telefono: '',
      fechaNacimiento: '',
      password: '',
      confirmacionPassword: '',
      aceptaPrivacidad: false,
    },
  })

  async function enviar(datos: DatosFormularioRegistro) {
    setErrorGeneral(null)
    try {
      await registrar({ ...datos, telefono: datos.telefono || null })
      const estado: EstadoLogin = { aviso: 'registrado', email: datos.email }
      navigate('/login', { replace: true, state: estado })
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setError('email', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else {
        // 422: por debajo de la edad mínima. 400: errores de cada campo.
        setErrorGeneral(aplicarErroresDelServidor(error, setError, CAMPOS))
      }
    }
  }

  return (
    <div className={`${estilos.formulario} ${estilos.formularioAncho}`}>
      <h1>Crear cuenta</h1>
      <p className={estilos.suave}>Con tu cuenta podrás reservar citas para ti y para menores a tu cargo.</p>
      {errorGeneral && <Aviso tipo="error">{errorGeneral}</Aviso>}
      <form onSubmit={handleSubmit(enviar)} noValidate>
        <div className={estilos.dosColumnas}>
          <Campo etiqueta="Nombre" autoComplete="given-name" error={errors.nombre?.message} {...register('nombre')} />
          <Campo etiqueta="Apellidos" autoComplete="family-name" error={errors.apellidos?.message} {...register('apellidos')} />
        </div>
        <Campo etiqueta="Correo electrónico" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <div className={estilos.dosColumnas}>
          <Campo etiqueta="Teléfono" opcional type="tel" autoComplete="tel" inputMode="tel" error={errors.telefono?.message} {...register('telefono')} />
          <Campo
            etiqueta="Fecha de nacimiento"
            type="date"
            max={hoy}
            autoComplete="bday"
            ayuda={`Debes tener ${edadMinima} años o más.`}
            error={errors.fechaNacimiento?.message}
            {...register('fechaNacimiento')}
          />
        </div>
        <div className={estilos.dosColumnas}>
          <Campo
            etiqueta="Contraseña"
            type="password"
            autoComplete="new-password"
            ayuda={`De ${PASSWORD_MIN} a ${PASSWORD_MAX} caracteres. Mejor una frase fácil de recordar.`}
            error={errors.password?.message}
            {...register('password')}
          />
          <Campo
            etiqueta="Repite la contraseña"
            type="password"
            autoComplete="new-password"
            error={errors.confirmacionPassword?.message}
            {...register('confirmacionPassword')}
          />
        </div>
        <Casilla
          etiqueta={
            <>
              He leído y acepto la{' '}
              <Link to="/privacidad" target="_blank" rel="noopener">
                política de privacidad
              </Link>
              .
            </>
          }
          error={errors.aceptaPrivacidad?.message}
          {...register('aceptaPrivacidad')}
        />
        <Boton type="submit" ancho enviando={isSubmitting} textoEnviando="Creando la cuenta…">
          Crear cuenta
        </Boton>
      </form>
      <p className={estilos.pieFormulario}>
        ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
      </p>
    </div>
  )
}
