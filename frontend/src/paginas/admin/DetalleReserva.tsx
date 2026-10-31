import { useQuery } from '@tanstack/react-query'
import { useLocation, useParams } from 'react-router'

import { consultaCita } from '../../api/citas'
import { ApiError } from '../../api/errores'
import type { CambioCita } from '../../api/tipos'
import { Aviso } from '../../componentes/Aviso'
import { EnlaceBoton } from '../../componentes/Boton'
import { CambioEstado } from '../../componentes/CambioEstado'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { FichaCita } from '../../componentes/FichaCita'
import { formatearInstante, NOMBRE_ESTADO } from '../../utilidades/formato'
import { useTitulo } from '../../utilidades/useTitulo'
import estilos from '../Paginas.module.css'
import { numeroDeParametro } from '../parametros'
import admin from './Admin.module.css'

/** CU-019: detalle de una reserva con el correo del cliente, el historial y el cambio de estado. */
export function DetalleReserva() {
  useTitulo('Detalle de la reserva')
  const id = numeroDeParametro(useParams().id ?? null)
  const reservada = (useLocation().state as { reservada?: boolean } | null)?.reservada ?? false
  const cita = useQuery({ ...consultaCita(id ?? 0), enabled: id !== undefined })

  if (id === undefined || (cita.error instanceof ApiError && cita.error.status === 404)) {
    return (
      <section className={estilos.texto}>
        <h1>Reserva no encontrada</h1>
        <p>No hemos encontrado esa reserva.</p>
        <EnlaceBoton to="/admin/reservas">Ver las reservas</EnlaceBoton>
      </section>
    )
  }
  if (cita.isPending) return <Cargando />
  if (cita.error) return <ErrorConsulta error={cita.error} reintentar={() => cita.refetch()} />

  return (
    <section className={estilos.texto}>
      {reservada && <Aviso tipo="exito">Reserva creada.</Aviso>}
      <FichaCita cita={cita.data} vista="gestion" />
      <CambioEstado cita={cita.data} recargar={() => cita.refetch()} />
      {cita.data.cambios && cita.data.cambios.length > 0 && (
        <section aria-labelledby="titulo-historial" className={admin.seccion}>
          <h2 id="titulo-historial">Historial</h2>
          <ol className={admin.historial}>
            {cita.data.cambios.map((cambio, indice) => (
              <li key={indice}>{textoCambio(cambio)}</li>
            ))}
          </ol>
        </section>
      )}
      <EnlaceBoton to="/admin/reservas" variante="enlace">
        Volver a las reservas
      </EnlaceBoton>
    </section>
  )
}

/** "Lunes, 2 de noviembre a las 10:15 · Confirmada → Cancelada · por Luis G." */
function textoCambio(cambio: CambioCita): string {
  const cuando = formatearInstante(cambio.fecha, false)
  const que = cambio.estadoAnterior
    ? `${NOMBRE_ESTADO[cambio.estadoAnterior]} → ${NOMBRE_ESTADO[cambio.estadoNuevo]}`
    : `Reservada (${NOMBRE_ESTADO[cambio.estadoNuevo].toLowerCase()})`
  const quien = cambio.realizadoPorNombre ? ` · por ${cambio.realizadoPorNombre === 'Cliente' ? 'el cliente' : cambio.realizadoPorNombre}` : ''
  return `${cuando.charAt(0).toUpperCase()}${cuando.slice(1)} · ${que}${quien}`
}
