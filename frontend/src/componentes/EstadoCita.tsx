import type { EstadoCita as Estado } from '../api/tipos'
import estilos from './EstadoCita.module.css'

const NOMBRE_ESTADO: Record<Estado, string> = {
  CONFIRMADA: 'Confirmada',
  COMPLETADA: 'Completada',
  CANCELADA: 'Cancelada',
  NO_PRESENTADO: 'No presentado',
}

const CLASE: Record<Estado, string> = {
  CONFIRMADA: estilos.confirmada,
  COMPLETADA: estilos.completada,
  CANCELADA: estilos.cancelada,
  NO_PRESENTADO: estilos.noPresentado,
}

/** Estado de una cita (RN-008): siempre con texto, el color solo acompaña. */
export function EstadoCita({ estado }: { estado: Estado }) {
  return <span className={`${estilos.estado} ${CLASE[estado]}`}>{NOMBRE_ESTADO[estado]}</span>
}
