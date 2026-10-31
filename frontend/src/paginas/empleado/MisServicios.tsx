import { useQuery } from '@tanstack/react-query'

import { consultaMisServicios } from '../../api/empleado'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { formatearDuracion, formatearPrecio } from '../../utilidades/formato'
import { useTitulo } from '../../utilidades/useTitulo'
import estilos from '../Paginas.module.css'

/** CU-021: los servicios que el administrador ha asignado al empleado. */
export function MisServicios() {
  useTitulo('Mis servicios')
  const { data: servicios, isPending, error, refetch } = useQuery(consultaMisServicios)

  return (
    <>
      <header className={estilos.cabecera}>
        <h1>Mis servicios</h1>
        <p className={estilos.subtitulo}>Los servicios que puedes realizar. Los asigna el administrador.</p>
      </header>
      {isPending ? (
        <Cargando texto="Cargando tus servicios…" />
      ) : error ? (
        <ErrorConsulta error={error} reintentar={() => refetch()} />
      ) : servicios.length === 0 ? (
        <p>Aún no tienes servicios asignados. Habla con el administrador.</p>
      ) : (
        <ul className={estilos.rejilla}>
          {servicios.map((servicio) => (
            <li key={servicio.id} className={estilos.tarjeta}>
              <h2>{servicio.nombre}</h2>
              {servicio.descripcion && <p className={estilos.suave}>{servicio.descripcion}</p>}
              <div className={estilos.datos}>
                <span>{formatearDuracion(servicio.duracionMinutos)}</span>
                <span className={estilos.precio}>{formatearPrecio(servicio.precio)}</span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
