import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'

import { consultaServiciosAdmin } from '../../api/admin'
import { EnlaceBoton } from '../../componentes/Boton'
import { Casilla } from '../../componentes/Campo'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { formatearDuracion, formatearPrecio } from '../../utilidades/formato'
import { useTitulo } from '../../utilidades/useTitulo'
import estilos from '../Paginas.module.css'
import admin from './Admin.module.css'

/** CU-016: todos los servicios, también los desactivados (que nunca se borran). */
export function ServiciosAdmin() {
  useTitulo('Servicios')
  const [parametros, setParametros] = useSearchParams()
  const verInactivos = parametros.get('inactivos') === 'si'
  const { data, isPending, error, refetch } = useQuery(consultaServiciosAdmin)
  const lista = data?.filter((servicio) => verInactivos || servicio.activo) ?? []

  return (
    <>
      <header className={`${estilos.cabecera} ${admin.cabecera}`}>
        <h1>Servicios</h1>
        <EnlaceBoton to="/admin/servicios/nuevo">Nuevo servicio</EnlaceBoton>
      </header>
      <div className={admin.filtroCasilla}>
        <Casilla
          etiqueta="Ver también los desactivados"
          checked={verInactivos}
          onChange={(evento) => setParametros(evento.target.checked ? { inactivos: 'si' } : {}, { replace: true })}
        />
      </div>
      {isPending ? (
        <Cargando texto="Cargando servicios…" />
      ) : error ? (
        <ErrorConsulta error={error} reintentar={() => refetch()} />
      ) : lista.length === 0 ? (
        <p>Todavía no hay servicios. Crea el primero.</p>
      ) : (
        <ul className={estilos.rejilla}>
          {lista.map((servicio) => (
            <li key={servicio.id} className={`${estilos.tarjeta} ${servicio.activo ? '' : admin.inactivo}`}>
              <h2>{servicio.nombre}</h2>
              {!servicio.activo && <p className={estilos.etiqueta}>Desactivado</p>}
              <p className={estilos.suave}>
                {servicio.empleados === 1 ? '1 profesional' : `${servicio.empleados} profesionales`}
              </p>
              <div className={estilos.datos}>
                <span>{formatearDuracion(servicio.duracionMinutos)}</span>
                <span className={estilos.precio}>{formatearPrecio(servicio.precio)}</span>
              </div>
              <Link to={`/admin/servicios/${servicio.id}`} aria-label={`Editar ${servicio.nombre}`}>
                Editar
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
