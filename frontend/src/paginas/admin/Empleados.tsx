import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'

import { consultaEmpleadosAdmin } from '../../api/admin'
import { EnlaceBoton } from '../../componentes/Boton'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { useTitulo } from '../../utilidades/useTitulo'
import estilos from '../Paginas.module.css'
import admin from './Admin.module.css'

/** CU-017: los empleados, activos y desactivados, con sus servicios. */
export function Empleados() {
  useTitulo('Empleados')
  const { data, isPending, error, refetch } = useQuery(consultaEmpleadosAdmin)

  return (
    <>
      <header className={`${estilos.cabecera} ${admin.cabecera}`}>
        <h1>Empleados</h1>
        <EnlaceBoton to="/admin/empleados/nuevo">Dar de alta</EnlaceBoton>
      </header>
      {isPending ? (
        <Cargando texto="Cargando empleados…" />
      ) : error ? (
        <ErrorConsulta error={error} reintentar={() => refetch()} />
      ) : data.length === 0 ? (
        <p>Todavía no hay empleados. Da de alta al primero.</p>
      ) : (
        <ul className={estilos.rejilla}>
          {data.map((empleado) => (
            <li key={empleado.id} className={`${estilos.tarjeta} ${empleado.activo ? '' : admin.inactivo}`}>
              <h2>
                {empleado.nombre} {empleado.apellidos}
              </h2>
              <p className={estilos.suave}>{empleado.email}</p>
              <ul className={estilos.etiquetas} aria-label="Etiquetas">
                {!empleado.activo && <li className={estilos.etiqueta}>Desactivado</li>}
                {empleado.debeCambiarPassword && <li className={estilos.etiqueta}>Contraseña temporal pendiente</li>}
              </ul>
              <p>{empleado.servicios.length === 0 ? 'Sin servicios asignados' : empleado.servicios.map((s) => s.nombre).join(', ')}</p>
              <Link to={`/admin/empleados/${empleado.id}`} aria-label={`Ver ficha de ${empleado.nombre} ${empleado.apellidos}`}>
                Ver ficha
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
