import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useLocation, useNavigate, useParams } from 'react-router'

import { actualizarServicio, cambiarActivoServicio, consultaServicioAdmin, crearServicio } from '../../api/admin'
import { consultaNegocio } from '../../api/catalogo'
import { ApiError, mensajeDeError } from '../../api/errores'
import type { Negocio, ServicioAdmin } from '../../api/tipos'
import { Aviso } from '../../componentes/Aviso'
import { Boton, EnlaceBoton } from '../../componentes/Boton'
import { Campo, CampoArea, CampoSeleccion } from '../../componentes/Campo'
import { Cargando } from '../../componentes/Cargando'
import { Confirmacion } from '../../componentes/Confirmacion'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { aplicarErroresDelServidor } from '../../utilidades/formularios'
import { duracionesPosibles, formatearDuracion } from '../../utilidades/formato'
import { useTitulo } from '../../utilidades/useTitulo'
import { esquemaServicio, precioDeTexto, type DatosFormularioServicio } from '../esquemas'
import { numeroDeParametro } from '../parametros'
import admin from './Admin.module.css'

const CAMPOS = ['nombre', 'descripcion', 'duracionMinutos', 'precio'] as const

function invalidarServicios(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['admin'] }),
    queryClient.invalidateQueries({ queryKey: ['servicios'] }),
    queryClient.invalidateQueries({ queryKey: ['profesionales'] }),
  ])
}

/** CU-016: alta (`/admin/servicios/nuevo`) o edición (`/admin/servicios/:id`) de un servicio. */
export function FichaServicio() {
  const parametro = useParams().id
  const id = parametro === undefined ? undefined : numeroDeParametro(parametro)
  useTitulo(id ? 'Editar servicio' : 'Nuevo servicio')
  const negocio = useQuery(consultaNegocio)
  const servicio = useQuery({ ...consultaServicioAdmin(id ?? 0), enabled: id !== undefined })

  if (parametro !== undefined && (id === undefined || (servicio.error instanceof ApiError && servicio.error.status === 404))) {
    return (
      <section className={admin.estrecho}>
        <h1>Servicio no encontrado</h1>
        <EnlaceBoton to="/admin/servicios">Ver los servicios</EnlaceBoton>
      </section>
    )
  }
  const fallida = [negocio, ...(id ? [servicio] : [])].find((consulta) => consulta.error)
  if (fallida) return <ErrorConsulta error={fallida.error} reintentar={() => fallida.refetch()} />
  if (!negocio.data || (id && !servicio.data)) return <Cargando />

  return (
    <section className={admin.estrecho}>
      <h1>{servicio.data ? servicio.data.nombre : 'Nuevo servicio'}</h1>
      <Formulario key={servicio.data?.id ?? 'nuevo'} negocio={negocio.data} servicio={servicio.data} />
      {servicio.data && <Activacion servicio={servicio.data} />}
      <EnlaceBoton to="/admin/servicios" variante="enlace">
        Volver a los servicios
      </EnlaceBoton>
    </section>
  )
}

function Formulario({ negocio, servicio }: { negocio: Negocio; servicio?: ServicioAdmin }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const creado = (useLocation().state as { creado?: boolean } | null)?.creado ?? false
  const [aviso, setAviso] = useState<{ tipo: 'exito' | 'error'; texto: string } | null>(creado ? { tipo: 'exito', texto: 'Servicio creado.' } : null)
  const duraciones = duracionesPosibles(negocio.intervaloMinutos)
  // Un servicio antiguo puede tener una duración que ya no está en la lista: se muestra igualmente.
  if (servicio && !duraciones.includes(servicio.duracionMinutos)) duraciones.unshift(servicio.duracionMinutos)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormularioServicio>({
    resolver: zodResolver(esquemaServicio),
    defaultValues: {
      nombre: servicio?.nombre ?? '',
      descripcion: servicio?.descripcion ?? '',
      duracionMinutos: servicio ? String(servicio.duracionMinutos) : '',
      precio: servicio ? String(servicio.precio).replace('.', ',') : '',
    },
  })

  async function guardar(datos: DatosFormularioServicio) {
    setAviso(null)
    const cuerpo = {
      nombre: datos.nombre,
      descripcion: datos.descripcion || null,
      duracionMinutos: Number(datos.duracionMinutos),
      precio: precioDeTexto(datos.precio) ?? 0,
    }
    try {
      const guardado = servicio ? await actualizarServicio(servicio.id, cuerpo) : await crearServicio(cuerpo)
      await invalidarServicios(queryClient)
      if (servicio) setAviso({ tipo: 'exito', texto: 'Servicio guardado.' })
      else navigate(`/admin/servicios/${guardado.id}`, { replace: true, state: { creado: true } })
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setError('nombre', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else if (error instanceof ApiError && error.status === 422) {
        setError('duracionMinutos', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else {
        const texto = aplicarErroresDelServidor(error, setError, CAMPOS)
        if (texto) setAviso({ tipo: 'error', texto })
      }
    }
  }

  return (
    <form className={admin.bloque} onSubmit={handleSubmit(guardar)} noValidate>
      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      <Campo etiqueta="Nombre" error={errors.nombre?.message} {...register('nombre')} />
      <CampoArea etiqueta="Descripción" opcional error={errors.descripcion?.message} {...register('descripcion')} />
      <div className={admin.casillas}>
        <CampoSeleccion
          etiqueta="Duración"
          ayuda={`Múltiplo del intervalo de reserva (${negocio.intervaloMinutos} min).`}
          error={errors.duracionMinutos?.message}
          {...register('duracionMinutos')}
        >
          <option value="">Elige…</option>
          {duraciones.map((minutos) => (
            <option key={minutos} value={minutos}>
              {formatearDuracion(minutos)}
            </option>
          ))}
        </CampoSeleccion>
        <Campo etiqueta="Precio (€)" inputMode="decimal" ayuda="Por ejemplo, 15 o 15,50." error={errors.precio?.message} {...register('precio')} />
      </div>
      <Boton type="submit" enviando={isSubmitting} textoEnviando="Guardando…">
        {servicio ? 'Guardar cambios' : 'Crear servicio'}
      </Boton>
    </form>
  )
}

function Activacion({ servicio }: { servicio: ServicioAdmin }) {
  const queryClient = useQueryClient()
  const [confirmando, setConfirmando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function cambiar() {
    setError(null)
    setEnviando(true)
    try {
      const actualizado = await cambiarActivoServicio(servicio.id, !servicio.activo)
      queryClient.setQueryData(consultaServicioAdmin(servicio.id).queryKey, actualizado)
      await invalidarServicios(queryClient)
      setConfirmando(false)
    } catch (fallo) {
      setError(mensajeDeError(fallo))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className={admin.bloque} aria-labelledby="titulo-activacion">
      <h2 id="titulo-activacion">{servicio.activo ? 'Desactivar el servicio' : 'Servicio desactivado'}</h2>
      {error && <Aviso tipo="error">{error}</Aviso>}
      <p>
        {servicio.activo
          ? 'Dejará de ofrecerse en la web. Las citas ya reservadas se mantienen. Puedes volver a activarlo cuando quieras.'
          : 'No se ofrece en la web. Al activarlo vuelve a aparecer con los profesionales que lo tienen asignado.'}
      </p>
      {confirmando ? (
        <Confirmacion
          titulo={servicio.activo ? '¿Desactivar este servicio?' : '¿Activar este servicio?'}
          textoConfirmar={servicio.activo ? 'Sí, desactivar' : 'Sí, activar'}
          textoEnviando="Guardando…"
          enviando={enviando}
          peligro={servicio.activo}
          onConfirmar={cambiar}
          onVolver={() => setConfirmando(false)}
        />
      ) : (
        <Boton variante="secundario" onClick={() => setConfirmando(true)}>
          {servicio.activo ? 'Desactivar' : 'Activar'}
        </Boton>
      )}
    </section>
  )
}
