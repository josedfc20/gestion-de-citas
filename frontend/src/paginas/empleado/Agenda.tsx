import { useQuery } from '@tanstack/react-query'
import { Link, useLocation, useSearchParams } from 'react-router'

import { consultaNegocio } from '../../api/catalogo'
import { consultaAgenda } from '../../api/empleado'
import type { AgendaDia, Negocio } from '../../api/tipos'
import { Aviso } from '../../componentes/Aviso'
import { Boton } from '../../componentes/Boton'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { EstadoCita } from '../../componentes/EstadoCita'
import { hoyEnZona, lunesDe, partesDeInstante, sumarDias } from '../../utilidades/fechas'
import { formatearDia, formatearTramos, personaDeLaCita } from '../../utilidades/formato'
import { useTitulo } from '../../utilidades/useTitulo'
import estilos from '../Paginas.module.css'
import { fechaDeParametro } from '../parametros'
import propias from './Agenda.module.css'

/** CU-013: la semana del empleado, de lunes a domingo (decisión 4 del B9). */
export function Agenda() {
  useTitulo('Mi agenda')
  const negocio = useQuery(consultaNegocio)

  if (negocio.isPending) return <Cargando />
  if (negocio.error) return <ErrorConsulta error={negocio.error} reintentar={() => negocio.refetch()} />
  return <Semana negocio={negocio.data} />
}

function Semana({ negocio }: { negocio: Negocio }) {
  const [parametros, setParametros] = useSearchParams()
  const passwordCambiada = (useLocation().state as { passwordCambiada?: boolean } | null)?.passwordCambiada
  const hoy = hoyEnZona(negocio.zonaHoraria)
  const desde = lunesDe(fechaDeParametro(parametros.get('desde')) ?? hoy)
  const hasta = sumarDias(desde, 6)
  const agenda = useQuery(consultaAgenda(desde, hasta))

  const irA = (lunes: string) => setParametros(lunes === lunesDe(hoy) ? {} : { desde: lunes })

  return (
    <>
      {passwordCambiada && <Aviso tipo="exito">Contraseña cambiada correctamente.</Aviso>}
      <header className={estilos.cabecera}>
        <h1>Mi agenda</h1>
        <p className={estilos.subtitulo}>
          Semana del {formatearDia(desde)} al {formatearDia(hasta)}. Horas del negocio ({negocio.zonaHoraria}).
        </p>
      </header>

      <nav className={propias.navegacion} aria-label="Cambiar de semana">
        <Boton variante="secundario" onClick={() => irA(sumarDias(desde, -7))}>
          ← Semana anterior
        </Boton>
        <Boton variante="secundario" onClick={() => irA(lunesDe(hoy))} disabled={desde === lunesDe(hoy)}>
          Hoy
        </Boton>
        <Boton variante="secundario" onClick={() => irA(sumarDias(desde, 7))}>
          Semana siguiente →
        </Boton>
      </nav>

      {agenda.isPending ? (
        <Cargando texto="Cargando tu agenda…" />
      ) : agenda.error ? (
        <ErrorConsulta error={agenda.error} reintentar={() => agenda.refetch()} />
      ) : (
        <ol className={propias.semana}>
          {agenda.data.map((dia) => (
            <li key={dia.fecha}>
              <Dia dia={dia} esHoy={dia.fecha === hoy} />
            </li>
          ))}
        </ol>
      )}
    </>
  )
}

function Dia({ dia, esHoy }: { dia: AgendaDia; esHoy: boolean }) {
  const idTitulo = `dia-${dia.fecha}`
  return (
    <section aria-labelledby={idTitulo} className={`${propias.dia} ${esHoy ? propias.hoy : ''}`}>
      <h2 id={idTitulo}>
        {formatearDia(dia.fecha)}
        {esHoy && <span className={propias.etiquetaHoy}>Hoy</span>}
      </h2>
      <p className={propias.horario}>{dia.cierre ? 'Cerrado (día de cierre)' : dia.tramos.length === 0 ? 'No trabajas' : formatearTramos(dia.tramos)}</p>
      {dia.citas.length === 0 ? (
        !dia.cierre && dia.tramos.length > 0 && <p className={propias.sinCitas}>Sin citas</p>
      ) : (
        <ul className={propias.citas}>
          {dia.citas.map((cita) => (
            <li key={cita.id}>
              <Link to={`/empleado/citas/${cita.id}`} className={propias.cita}>
                <span>
                  <span className={propias.hora}>
                    {partesDeInstante(cita.inicio).hora}–{partesDeInstante(cita.fin).hora}
                  </span>{' '}
                  {cita.servicio.nombre}
                </span>
                <span>{personaDeLaCita(cita)}</span>
                {cita.estado !== 'CONFIRMADA' && (
                  <span>
                    <EstadoCita estado={cita.estado} />
                  </span>
                )}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
