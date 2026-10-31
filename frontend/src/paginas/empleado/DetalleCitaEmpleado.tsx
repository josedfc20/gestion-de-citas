import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router'

import { consultaCita } from '../../api/citas'
import { ApiError } from '../../api/errores'
import { EnlaceBoton } from '../../componentes/Boton'
import { CambioEstado } from '../../componentes/CambioEstado'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { FichaCita } from '../../componentes/FichaCita'
import { lunesDe, partesDeInstante } from '../../utilidades/fechas'
import { useTitulo } from '../../utilidades/useTitulo'
import estilos from '../Paginas.module.css'
import { numeroDeParametro } from '../parametros'

/** CU-014 y CU-015: detalle de una cita asignada y cambio de su estado. */
export function DetalleCitaEmpleado() {
  useTitulo('Detalle de la cita')
  const id = numeroDeParametro(useParams().id ?? null)
  const cita = useQuery({ ...consultaCita(id ?? 0), enabled: id !== undefined })

  if (id === undefined || (cita.error instanceof ApiError && cita.error.status === 404)) {
    return (
      <section className={estilos.texto}>
        <h1>Cita no encontrada</h1>
        <p>No hemos encontrado esa cita entre las que tienes asignadas.</p>
        <EnlaceBoton to="/empleado/agenda">Ir a mi agenda</EnlaceBoton>
      </section>
    )
  }
  if (cita.isPending) return <Cargando />
  if (cita.error) return <ErrorConsulta error={cita.error} reintentar={() => cita.refetch()} />

  const semana = lunesDe(partesDeInstante(cita.data.inicio).fecha)
  return (
    <section className={estilos.texto}>
      <FichaCita cita={cita.data} vista="gestion" />
      <CambioEstado cita={cita.data} recargar={() => cita.refetch()} />
      <div className={estilos.acciones}>
        <EnlaceBoton to={`/empleado/agenda?desde=${semana}`} variante="enlace">
          Ver la semana en la agenda
        </EnlaceBoton>
        <EnlaceBoton to="/empleado/citas" variante="enlace">
          Ver mis citas asignadas
        </EnlaceBoton>
      </div>
    </section>
  )
}
