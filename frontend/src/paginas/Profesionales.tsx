import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'

import { consultaProfesionales, consultaServicios } from '../api/catalogo'
import { CampoSeleccion } from '../componentes/Campo'
import { Cargando } from '../componentes/Cargando'
import { ErrorConsulta } from '../componentes/ErrorConsulta'
import { useTitulo } from '../utilidades/useTitulo'
import estilos from './Paginas.module.css'
import { numeroDeParametro } from './parametros'

/** CU-006: profesionales con sus servicios, con filtro por servicio. */
export function Profesionales() {
  useTitulo('Profesionales')
  const [parametros, setParametros] = useSearchParams()
  const servicioId = numeroDeParametro(parametros.get('servicio'))
  const servicios = useQuery(consultaServicios)
  const profesionales = useQuery(consultaProfesionales(servicioId))

  function elegirServicio(valor: string) {
    setParametros(valor ? { servicio: valor } : {}, { replace: true })
  }

  return (
    <>
      <header className={estilos.cabecera}>
        <h1>Profesionales</h1>
        <p className={estilos.subtitulo}>Nuestro equipo y los servicios que realiza cada persona.</p>
      </header>

      <div className={estilos.filtro}>
        <CampoSeleccion etiqueta="Filtrar por servicio" value={servicioId ?? ''} onChange={(evento) => elegirServicio(evento.target.value)} disabled={!servicios.data}>
          <option value="">Todos los servicios</option>
          {servicios.data?.map((servicio) => (
            <option key={servicio.id} value={servicio.id}>
              {servicio.nombre}
            </option>
          ))}
        </CampoSeleccion>
      </div>

      {profesionales.isPending ? (
        <Cargando texto="Cargando profesionales…" />
      ) : profesionales.error ? (
        <ErrorConsulta error={profesionales.error} reintentar={() => profesionales.refetch()} />
      ) : profesionales.data.length === 0 ? (
        <p>{servicioId ? 'Ahora mismo nadie realiza este servicio.' : 'Todavía no hay profesionales publicados.'}</p>
      ) : (
        <ul className={estilos.rejilla}>
          {profesionales.data.map((profesional) => (
            <li key={profesional.id} className={estilos.tarjeta}>
              <h2>{profesional.nombre}</h2>
              <ul className={estilos.etiquetas} aria-label={`Servicios de ${profesional.nombre}`}>
                {profesional.servicios.map((servicio) => (
                  <li key={servicio.id} className={estilos.etiqueta}>
                    {servicio.nombre}
                  </li>
                ))}
              </ul>
              {servicioId && (
                <div className={estilos.datos}>
                  <Link to={`/horarios?servicio=${servicioId}&profesional=${profesional.id}`}>Ver sus horas libres</Link>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
