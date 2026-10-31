import { Link } from 'react-router'

import type { Cita } from '../api/tipos'
import { formatearInstante, formatearPrecio, personaDeLaCita } from '../utilidades/formato'
import { EstadoCita } from './EstadoCita'
import estilos from './TarjetaCita.module.css'

interface Props {
  cita: Cita
  /** Ruta del detalle de la cita. */
  enlace: string
  /** El empleado y el admin ven para quién es; el cliente, con qué profesional. */
  vista?: 'cliente' | 'gestion'
}

/** Una cita en una lista (mis citas, citas asignadas, reservas). */
export function TarjetaCita({ cita, enlace, vista = 'cliente' }: Props) {
  return (
    <Link to={enlace} className={estilos.tarjeta}>
      <span className={estilos.cuando}>{formatearInstante(cita.inicio)}</span>
      <span>
        {cita.servicio.nombre} <span className={estilos.suave}>con {cita.profesional.nombre}</span>
      </span>
      {vista === 'cliente'
        ? cita.beneficiario && <span className={estilos.para}>Para {cita.beneficiario.nombre}</span>
        : (
            <span className={estilos.para}>
              {personaDeLaCita(cita)}
              {cita.cliente?.telefono && <span className={estilos.suave}> · {cita.cliente.telefono}</span>}
            </span>
          )}
      <span className={estilos.pie}>
        <EstadoCita estado={cita.estado} />
        <span>{formatearPrecio(cita.precio)}</span>
      </span>
    </Link>
  )
}
