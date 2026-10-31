import { Link } from 'react-router'

import type { CitaAfectada } from '../api/tipos'
import { formatearInstante } from '../utilidades/formato'
import { Aviso } from './Aviso'

/**
 * Guardar un horario o un día de cierre nunca cancela citas (B5): si alguna
 * confirmada queda fuera, se listan para que el admin las revise.
 */
export function CitasAfectadas({ citas }: { citas: CitaAfectada[] | undefined }) {
  if (!citas || citas.length === 0) return null
  return (
    <Aviso>
      <p>
        {citas.length === 1 ? 'Esta cita queda' : `Estas ${citas.length} citas quedan`} fuera del horario. No se han cancelado: revísalas.
      </p>
      <ul>
        {citas.map((cita) => (
          <li key={cita.id}>
            <Link to={`/admin/reservas/${cita.id}`}>
              {formatearInstante(cita.inicio)}, con {cita.profesional}
            </Link>
          </li>
        ))}
      </ul>
    </Aviso>
  )
}
