import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useParams } from 'react-router'

import {
  actualizarEmpleado,
  asignarServicios,
  cambiarActivoEmpleado,
  consultaEmpleadoAdmin,
  consultaServiciosAdmin,
  nuevaPasswordTemporal,
} from '../../api/admin'
import { ApiError, mensajeDeError } from '../../api/errores'
import type { EmpleadoAdmin } from '../../api/tipos'
import { Aviso } from '../../componentes/Aviso'
import { Boton, EnlaceBoton } from '../../componentes/Boton'
import { Campo } from '../../componentes/Campo'
import { Cargando } from '../../componentes/Cargando'
import { Confirmacion } from '../../componentes/Confirmacion'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { PasswordTemporal } from '../../componentes/PasswordTemporal'
import { aplicarErroresDelServidor } from '../../utilidades/formularios'
import { useTitulo } from '../../utilidades/useTitulo'
import { esquemaEmpleado, type DatosFormularioEmpleado } from '../esquemas'
import estilos from '../Paginas.module.css'
import { numeroDeParametro } from '../parametros'
import admin from './Admin.module.css'
import { CasillasServicios } from './CasillasServicios'

const CAMPOS = ['nombre', 'apellidos', 'email', 'telefono'] as const

/** Tras cambiar un empleado: su ficha, la lista, los profesionales públicos y los servicios (cuántos lo hacen). */
function guardarEnCache(queryClient: QueryClient, empleado: EmpleadoAdmin) {
  queryClient.setQueryData(consultaEmpleadoAdmin(empleado.id).queryKey, empleado)
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: consultaServiciosAdmin.queryKey }),
    queryClient.invalidateQueries({ queryKey: ['admin', 'empleados'], exact: true }),
    queryClient.invalidateQueries({ queryKey: ['profesionales'] }),
  ])
}

/** CU-017: datos, servicios, cuenta y horario de un empleado. */
export function FichaEmpleado() {
  useTitulo('Ficha de empleado')
  const id = numeroDeParametro(useParams().id ?? null)
  const empleado = useQuery({ ...consultaEmpleadoAdmin(id ?? 0), enabled: id !== undefined })

  if (id === undefined || (empleado.error instanceof ApiError && empleado.error.status === 404)) {
    return (
      <section className={admin.estrecho}>
        <h1>Empleado no encontrado</h1>
        <EnlaceBoton to="/admin/empleados">Ver los empleados</EnlaceBoton>
      </section>
    )
  }
  if (empleado.isPending) return <Cargando />
  if (empleado.error) return <ErrorConsulta error={empleado.error} reintentar={() => empleado.refetch()} />
  const datos = empleado.data

  return (
    <section className={admin.estrecho}>
      <h1>
        {datos.nombre} {datos.apellidos}
      </h1>
      {!datos.activo && <Aviso>Este empleado está desactivado: no sale en la web ni puede iniciar sesión.</Aviso>}
      <Datos key={`datos-${datos.id}`} empleado={datos} />
      <Servicios key={`servicios-${datos.id}`} empleado={datos} />
      <section className={admin.bloque} aria-labelledby="titulo-horario">
        <h2 id="titulo-horario">Horario</h2>
        <p>Por defecto sigue el horario general del negocio. Puedes darle un horario propio para algunos días.</p>
        <EnlaceBoton to={`/admin/empleados/${datos.id}/horario`} variante="secundario">
          Ver y ajustar su horario
        </EnlaceBoton>
      </section>
      <Cuenta empleado={datos} />
      <EnlaceBoton to="/admin/empleados" variante="enlace">
        Volver a los empleados
      </EnlaceBoton>
    </section>
  )
}

function Datos({ empleado }: { empleado: EmpleadoAdmin }) {
  const queryClient = useQueryClient()
  const [aviso, setAviso] = useState<{ tipo: 'exito' | 'error'; texto: string } | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormularioEmpleado>({
    resolver: zodResolver(esquemaEmpleado),
    defaultValues: { nombre: empleado.nombre, apellidos: empleado.apellidos, email: empleado.email, telefono: empleado.telefono ?? '' },
  })

  async function guardar(datos: DatosFormularioEmpleado) {
    setAviso(null)
    try {
      const guardado = await actualizarEmpleado(empleado.id, { ...datos, telefono: datos.telefono || null })
      await guardarEnCache(queryClient, guardado)
      setAviso({ tipo: 'exito', texto: 'Datos guardados.' })
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setError('email', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else {
        const texto = aplicarErroresDelServidor(error, setError, CAMPOS)
        if (texto) setAviso({ tipo: 'error', texto })
      }
    }
  }

  return (
    <section className={admin.bloque} aria-labelledby="titulo-datos">
      <h2 id="titulo-datos">Datos</h2>
      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      <form onSubmit={handleSubmit(guardar)} noValidate>
        <div className={estilos.dosColumnas}>
          <Campo etiqueta="Nombre" autoComplete="off" error={errors.nombre?.message} {...register('nombre')} />
          <Campo etiqueta="Apellidos" autoComplete="off" error={errors.apellidos?.message} {...register('apellidos')} />
        </div>
        <div className={estilos.dosColumnas}>
          <Campo etiqueta="Correo electrónico" type="email" autoComplete="off" error={errors.email?.message} {...register('email')} />
          <Campo etiqueta="Teléfono" type="tel" opcional error={errors.telefono?.message} {...register('telefono')} />
        </div>
        <Boton type="submit" enviando={isSubmitting} textoEnviando="Guardando…">
          Guardar datos
        </Boton>
      </form>
    </section>
  )
}

function Servicios({ empleado }: { empleado: EmpleadoAdmin }) {
  const queryClient = useQueryClient()
  const servicios = useQuery(consultaServiciosAdmin)
  const [elegidos, setElegidos] = useState(() => empleado.servicios.map((servicio) => servicio.id))
  const [enviando, setEnviando] = useState(false)
  const [resultado, setResultado] = useState<{ tipo: 'exito' | 'error'; textos: string[] } | null>(null)

  async function guardar() {
    setResultado(null)
    setEnviando(true)
    try {
      // Solo se asignan servicios activos: uno desactivado que tuviera se le quita.
      const activos = new Set(servicios.data?.filter((servicio) => servicio.activo).map((servicio) => servicio.id))
      const guardado = await asignarServicios(empleado.id, elegidos.filter((id) => activos.has(id)))
      await guardarEnCache(queryClient, guardado)
      setResultado({ tipo: 'exito', textos: ['Servicios guardados.', ...(guardado.avisos ?? [])] })
    } catch (error) {
      setResultado({ tipo: 'error', textos: [mensajeDeError(error)] })
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className={admin.bloque} aria-labelledby="titulo-servicios">
      <h2 id="titulo-servicios">Servicios</h2>
      {resultado && (
        <Aviso tipo={resultado.tipo}>
          {resultado.textos.map((texto) => (
            <p key={texto}>{texto}</p>
          ))}
        </Aviso>
      )}
      {servicios.isPending ? (
        <Cargando texto="Cargando servicios…" />
      ) : servicios.error ? (
        <ErrorConsulta error={servicios.error} reintentar={() => servicios.refetch()} />
      ) : (
        <CasillasServicios servicios={servicios.data} elegidos={elegidos} onCambiar={setElegidos} />
      )}
      <Boton enviando={enviando} textoEnviando="Guardando…" disabled={!servicios.data} onClick={guardar}>
        Guardar servicios
      </Boton>
    </section>
  )
}

function Cuenta({ empleado }: { empleado: EmpleadoAdmin }) {
  const queryClient = useQueryClient()
  const [accion, setAccion] = useState<'password' | 'activo' | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [password, setPassword] = useState<string | null>(null)
  const [error, setError] = useState<{ texto: string; conCitas: boolean } | null>(null)
  const nombre = `${empleado.nombre} ${empleado.apellidos}`

  async function confirmar() {
    setError(null)
    setEnviando(true)
    try {
      if (accion === 'password') {
        const respuesta = await nuevaPasswordTemporal(empleado.id)
        setPassword(respuesta.passwordTemporal)
        await guardarEnCache(queryClient, { ...empleado, debeCambiarPassword: true })
      } else {
        await guardarEnCache(queryClient, await cambiarActivoEmpleado(empleado.id, !empleado.activo))
      }
      setAccion(null)
    } catch (fallo) {
      setError({ texto: mensajeDeError(fallo), conCitas: fallo instanceof ApiError && fallo.status === 409 })
      setAccion(null)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className={admin.bloque} aria-labelledby="titulo-cuenta">
      <h2 id="titulo-cuenta">Cuenta</h2>
      {empleado.debeCambiarPassword && !password && <p>Aún no ha cambiado su contraseña temporal.</p>}
      {password && <PasswordTemporal password={password} nombre={nombre} />}
      {error && (
        <Aviso tipo="error">
          <p>{error.texto}</p>
          {error.conCitas && (
            <EnlaceBoton to={`/admin/reservas?profesional=${empleado.id}&estado=CONFIRMADA`} variante="secundario">
              Ver sus citas confirmadas
            </EnlaceBoton>
          )}
        </Aviso>
      )}
      {accion ? (
        <Confirmacion
          titulo={
            accion === 'password'
              ? '¿Generar otra contraseña temporal?'
              : empleado.activo
                ? `¿Desactivar a ${nombre}?`
                : `¿Activar a ${nombre}?`
          }
          textoConfirmar={accion === 'password' ? 'Sí, generar' : empleado.activo ? 'Sí, desactivar' : 'Sí, activar'}
          textoEnviando="Guardando…"
          enviando={enviando}
          peligro={accion === 'activo' && empleado.activo}
          onConfirmar={confirmar}
          onVolver={() => setAccion(null)}
        >
          <p>
            {accion === 'password'
              ? 'La contraseña actual dejará de servir y tendrá que cambiar la nueva al entrar.'
              : empleado.activo
                ? 'No saldrá en la web ni podrá iniciar sesión, y se cerrarán sus sesiones abiertas.'
                : 'Volverá a salir en la web y podrá iniciar sesión.'}
          </p>
        </Confirmacion>
      ) : (
        <div className={estilos.acciones}>
          {empleado.activo && (
            <Boton variante="secundario" onClick={() => setAccion('password')}>
              Generar otra contraseña temporal
            </Boton>
          )}
          <Boton variante="secundario" onClick={() => setAccion('activo')}>
            {empleado.activo ? 'Desactivar' : 'Activar'}
          </Boton>
        </div>
      )}
    </section>
  )
}
