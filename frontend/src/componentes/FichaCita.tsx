import type { Cita } from '../api/tipos'
import { partesDeInstante } from '../utilidades/fechas'
import { formatearDuracion, formatearInstante, formatearPrecio } from '../utilidades/formato'
import { EstadoCita } from './EstadoCita'
import estilos from './FichaCita.module.css'

interface Props {
  cita: Cita
  /** El cliente ve "Para ti"; el empleado y el admin, los datos del cliente. */
  vista?: 'cliente' | 'gestion'
}

/** Parte común del detalle de una cita para el cliente, el empleado y el admin (B9). */
export function FichaCita({ cita, vista = 'cliente' }: Props) {
  const { cliente } = cita
  return (
    <>
      <header className={estilos.cabecera}>
        <h1>{cita.servicio.nombre}</h1>
        <EstadoCita estado={cita.estado} />
      </header>

      <dl className={estilos.datos}>
        <div>
          <dt>Día y hora</dt>
          <dd>
            {formatearInstante(cita.inicio)} (hasta las {partesDeInstante(cita.fin).hora})
          </dd>
        </div>
        <div>
          <dt>Profesional</dt>
          <dd>{cita.profesional.nombre}</dd>
        </div>
        {vista === 'gestion' && cliente && (
          <div>
            <dt>Cliente</dt>
            <dd>
              {cliente.nombre} {cliente.apellidos}
              {cliente.telefono && (
                <a className={estilos.contacto} href={`tel:${cliente.telefono.replaceAll(' ', '')}`}>
                  {cliente.telefono}
                </a>
              )}
              {cliente.email && (
                <a className={estilos.contacto} href={`mailto:${cliente.email}`}>
                  {cliente.email}
                </a>
              )}
            </dd>
          </div>
        )}
        <div>
          <dt>Para quién es</dt>
          <dd>{paraQuien(cita, vista)}</dd>
        </div>
        <div>
          <dt>Duración y precio</dt>
          <dd>
            {formatearDuracion(cita.servicio.duracionMinutos)} · {formatearPrecio(cita.precio)}
          </dd>
        </div>
        {cita.estado === 'CANCELADA' && cita.motivoCancelacion && (
          <div>
            <dt>Motivo de la cancelación</dt>
            <dd>{cita.motivoCancelacion}</dd>
          </div>
        )}
      </dl>
    </>
  )
}

function paraQuien(cita: Cita, vista: 'cliente' | 'gestion'): string {
  if (vista === 'cliente') return cita.beneficiario ? cita.beneficiario.nombre : 'Para ti'
  return cita.beneficiario ? `${cita.beneficiario.nombre} (menor)` : 'El propio cliente'
}
