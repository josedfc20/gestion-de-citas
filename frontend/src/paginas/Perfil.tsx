import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useLocation, useNavigate } from 'react-router'

import { ApiError, mensajeDeError } from '../api/errores'
import { consultaPerfil, eliminarCuenta, exportarDatos, guardarPerfil } from '../api/perfil'
import type { Perfil as DatosPerfilActual } from '../api/tipos'
import { Aviso } from '../componentes/Aviso'
import { Boton, EnlaceBoton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { Cargando } from '../componentes/Cargando'
import { Confirmacion } from '../componentes/Confirmacion'
import { ErrorConsulta } from '../componentes/ErrorConsulta'
import { useSesion } from '../sesion/contexto'
import { guardarArchivo } from '../utilidades/descargas'
import { aplicarErroresDelServidor } from '../utilidades/formularios'
import { formatearFechaLarga } from '../utilidades/formato'
import { useTitulo } from '../utilidades/useTitulo'
import {
  esquemaEliminarCuenta,
  esquemaPerfil,
  type DatosEliminarCuenta,
  type DatosFormularioPerfil,
} from './esquemas'
import estilos from './Paginas.module.css'
import propias from './Perfil.module.css'

const CAMPOS_PERFIL = ['nombre', 'apellidos', 'email', 'telefono'] as const

/** CU-004 y RNF-007: datos, contraseña, exportar y eliminar la cuenta. Sirve para los tres roles. */
export function Perfil() {
  useTitulo('Mi perfil')
  const perfil = useQuery(consultaPerfil)
  const passwordCambiada = (useLocation().state as { passwordCambiada?: boolean } | null)?.passwordCambiada

  if (perfil.isPending) return <Cargando />
  if (perfil.error) return <ErrorConsulta error={perfil.error} reintentar={() => perfil.refetch()} />

  return (
    <div className={propias.perfil}>
      <h1>Mi perfil</h1>
      {passwordCambiada && <Aviso tipo="exito">Contraseña cambiada correctamente.</Aviso>}
      <MisDatos perfil={perfil.data} />
      <section className={propias.bloque} aria-labelledby="titulo-password">
        <h2 id="titulo-password">Contraseña</h2>
        <p className={estilos.suave}>Al cambiarla se cierran tus sesiones en otros dispositivos.</p>
        <EnlaceBoton to="/cambiar-password" variante="secundario">
          Cambiar contraseña
        </EnlaceBoton>
      </section>
      <DescargarDatos />
      {perfil.data.rol === 'CLIENTE' && <EliminarCuenta />}
    </div>
  )
}

function MisDatos({ perfil }: { perfil: DatosPerfilActual }) {
  const queryClient = useQueryClient()
  const { actualizarUsuario } = useSesion()
  const [aviso, setAviso] = useState<{ tipo: 'exito' | 'error'; texto: string } | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormularioPerfil>({
    resolver: zodResolver(esquemaPerfil),
    defaultValues: { nombre: perfil.nombre, apellidos: perfil.apellidos, email: perfil.email, telefono: perfil.telefono ?? '' },
  })

  async function guardar(datos: DatosFormularioPerfil) {
    setAviso(null)
    try {
      const guardado = await guardarPerfil({ ...datos, telefono: datos.telefono || null })
      queryClient.setQueryData(consultaPerfil.queryKey, guardado)
      actualizarUsuario({ nombre: guardado.nombre })
      reset({ nombre: guardado.nombre, apellidos: guardado.apellidos, email: guardado.email, telefono: guardado.telefono ?? '' })
      setAviso({ tipo: 'exito', texto: 'Datos guardados.' })
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        // "Ese correo ya lo usa otra cuenta."
        setError('email', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else {
        const texto = aplicarErroresDelServidor(error, setError, CAMPOS_PERFIL)
        if (texto) setAviso({ tipo: 'error', texto })
      }
    }
  }

  return (
    <section className={propias.bloque} aria-labelledby="titulo-datos">
      <h2 id="titulo-datos">Mis datos</h2>
      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      <form onSubmit={handleSubmit(guardar)} noValidate>
        <div className={estilos.dosColumnas}>
          <Campo etiqueta="Nombre" autoComplete="given-name" error={errors.nombre?.message} {...register('nombre')} />
          <Campo etiqueta="Apellidos" autoComplete="family-name" error={errors.apellidos?.message} {...register('apellidos')} />
        </div>
        <div className={estilos.dosColumnas}>
          <Campo etiqueta="Correo electrónico" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
          <Campo etiqueta="Teléfono" type="tel" autoComplete="tel" opcional error={errors.telefono?.message} {...register('telefono')} />
        </div>
        {perfil.fechaNacimiento && (
          <p className={estilos.suave}>Fecha de nacimiento: {formatearFechaLarga(perfil.fechaNacimiento)} (no se puede cambiar).</p>
        )}
        <Boton type="submit" enviando={isSubmitting} textoEnviando="Guardando…">
          Guardar datos
        </Boton>
      </form>
    </section>
  )
}

function DescargarDatos() {
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function descargar() {
    setError(null)
    setEnviando(true)
    try {
      guardarArchivo(await exportarDatos(), 'mis-datos.json')
    } catch (fallo) {
      setError(mensajeDeError(fallo))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className={propias.bloque} aria-labelledby="titulo-descargar">
      <h2 id="titulo-descargar">Descargar mis datos</h2>
      <p className={estilos.suave}>
        Un archivo JSON con todos los datos personales que guardamos de ti (y, si eres cliente, de tus citas y tus menores a cargo).
      </p>
      {error && <Aviso tipo="error">{error}</Aviso>}
      <Boton variante="secundario" enviando={enviando} textoEnviando="Preparando…" onClick={descargar}>
        Descargar mis datos
      </Boton>
    </section>
  )
}

function EliminarCuenta() {
  const navigate = useNavigate()
  const [confirmando, setConfirmando] = useState(false)
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<DatosEliminarCuenta>({ resolver: zodResolver(esquemaEliminarCuenta), defaultValues: { password: '' } })

  async function eliminar({ password }: DatosEliminarCuenta) {
    setErrorGeneral(null)
    try {
      await eliminarCuenta(password)
      // La sesión la borra el Marco cuando ya se ve el inicio: si se borrara
      // aquí, esta página privada llevaría al login antes de salir de ella.
      await navigate('/', { replace: true, state: { cuentaEliminada: true, cerrarSesion: true } })
    } catch (error) {
      if (error instanceof ApiError && error.status === 422) {
        // "La contraseña no es correcta."
        setError('password', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else {
        setErrorGeneral(mensajeDeError(error))
      }
    }
  }

  function volver() {
    reset()
    setErrorGeneral(null)
    setConfirmando(false)
  }

  return (
    <section className={`${propias.bloque} ${propias.peligro}`} aria-labelledby="titulo-eliminar">
      <h2 id="titulo-eliminar">Eliminar mi cuenta</h2>
      <p>
        Se borrarán tus datos personales y los de tus menores a cargo, y se cancelarán tus citas futuras. Las citas pasadas se
        guardan sin tus datos. <strong>No se puede deshacer.</strong>
      </p>
      {confirmando ? (
        <Confirmacion
          titulo="¿Seguro que quieres eliminar tu cuenta?"
          textoConfirmar="Eliminar mi cuenta"
          textoEnviando="Eliminando…"
          enviando={isSubmitting}
          peligro
          onConfirmar={handleSubmit(eliminar)}
          onVolver={volver}
        >
          {errorGeneral && <Aviso tipo="error">{errorGeneral}</Aviso>}
          <Campo
            etiqueta="Escribe tu contraseña para confirmar"
            type="password"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password')}
          />
        </Confirmacion>
      ) : (
        <Boton variante="secundario" onClick={() => setConfirmando(true)}>
          Quiero eliminar mi cuenta
        </Boton>
      )}
    </section>
  )
}
