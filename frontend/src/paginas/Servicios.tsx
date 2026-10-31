import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'

import { consultaServicios } from '../api/catalogo'
import { Cargando } from '../componentes/Cargando'
import { ErrorConsulta } from '../componentes/ErrorConsulta'
import { formatearDuracion, formatearPrecio } from '../utilidades/formato'
import { useTitulo } from '../utilidades/useTitulo'
import estilos from './Paginas.module.css'

/** CU-005: servicios activos con su duración y precio. */
export function Servicios() {
  useTitulo('Servicios')
  const { data: servicios, isPending, error, refetch } = useQuery(consultaServicios)

  return (
    <>
      <header className={estilos.cabecera}>
        <h1>Servicios y precios</h1>
        <p className={estilos.subtitulo}>Elige un servicio para ver quién lo realiza y sus horas libres.</p>
      </header>
      {isPending ? (
        <Cargando texto="Cargando servicios…" />
      ) : error ? (
        <ErrorConsulta error={error} reintentar={() => refetch()} />
      ) : servicios.length === 0 ? (
        <p>Todavía no hay servicios publicados.</p>
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
              <div className={estilos.acciones}>
                <Link to={`/profesionales?servicio=${servicio.id}`}>Profesionales</Link>
                <Link to={`/horarios?servicio=${servicio.id}`}>Horarios libres</Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
