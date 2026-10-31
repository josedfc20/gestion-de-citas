import type { ServicioAdmin } from '../../api/tipos'
import { Casilla } from '../../componentes/Campo'
import { formatearDuracion } from '../../utilidades/formato'
import admin from './Admin.module.css'

interface Props {
  servicios: ServicioAdmin[]
  elegidos: number[]
  onCambiar: (elegidos: number[]) => void
}

/** Servicios que realiza un empleado (solo los activos se pueden asignar). */
export function CasillasServicios({ servicios, elegidos, onCambiar }: Props) {
  const activos = servicios.filter((servicio) => servicio.activo)
  return (
    <fieldset className={admin.casillas}>
      <legend>Servicios que realiza</legend>
      {activos.length === 0 && <p>No hay servicios activos. Créalos antes en "Servicios".</p>}
      {activos.map((servicio) => (
        <Casilla
          key={servicio.id}
          etiqueta={`${servicio.nombre} (${formatearDuracion(servicio.duracionMinutos)})`}
          checked={elegidos.includes(servicio.id)}
          onChange={(evento) =>
            onCambiar(evento.target.checked ? [...elegidos, servicio.id] : elegidos.filter((id) => id !== servicio.id))
          }
        />
      ))}
    </fieldset>
  )
}
