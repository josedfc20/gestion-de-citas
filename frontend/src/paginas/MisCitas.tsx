import { useInfiniteQuery } from '@tanstack/react-query'
import { Link, useLocation, useSearchParams } from 'react-router'

import { consultaMisCitas } from '../api/citas'
import type { Cita } from '../api/tipos'
import { Aviso } from '../componentes/Aviso'
import { Boton, EnlaceBoton } from '../componentes/Boton'
import { Cargando } from '../componentes/Cargando'
import { ErrorConsulta } from '../componentes/ErrorConsulta'
import { EstadoCita } from '../componentes/EstadoCita'
import { formatearInstante, formatearPrecio } from '../utilidades/formato'
import { useTitulo } from '../utilidades/useTitulo'
import propias from './MisCitas.module.css'
import estilos from './Paginas.module.css'

/** CU-010: próximas citas (también las que canceló el negocio) e historial. */
export function MisCitas() {
  useTitulo('Mis citas')
  const [parametros, setParametros] = useSearchParams()
  const passwordCambiada = (useLocation().state as { passwordCambiada?: boolean } | null)?.passwordCambiada
  const historial = parametros.get('ver') === 'historial'
  const citas = useInfiniteQuery(consultaMisCitas(historial))

  const lista = citas.data?.pages.flatMap((pagina) => pagina.contenido) ?? []

  return (
    <>
      {passwordCambiada && <Aviso tipo="exito">Contraseña cambiada correctamente.</Aviso>}
      <header className={`${estilos.cabecera} ${propias.cabecera}`}>
        <h1>Mis citas</h1>
        <EnlaceBoton to="/horarios">Reservar una cita</EnlaceBoton>
      </header>

      <nav className={propias.pestanas} aria-label="Qué citas ver">
        <button type="button" className={propias.pestana} aria-current={!historial ? 'page' : undefined} onClick={() => setParametros({})}>
          Próximas
        </button>
        <button
          type="button"
          className={propias.pestana}
          aria-current={historial ? 'page' : undefined}
          onClick={() => setParametros({ ver: 'historial' })}
        >
          Historial
        </button>
      </nav>

      {citas.isPending ? (
        <Cargando texto="Cargando tus citas…" />
      ) : citas.error ? (
        <ErrorConsulta error={citas.error} reintentar={() => citas.refetch()} />
      ) : lista.length === 0 ? (
        <div className={estilos.texto}>
          <p>{historial ? 'Todavía no tienes citas pasadas.' : 'No tienes citas próximas.'}</p>
          {!historial && <EnlaceBoton to="/horarios">Reservar una cita</EnlaceBoton>}
        </div>
      ) : (
        <>
          <ul className={propias.lista}>
            {lista.map((cita) => (
              <li key={cita.id}>
                <TarjetaCita cita={cita} />
              </li>
            ))}
          </ul>
          {citas.hasNextPage && (
            <Boton variante="secundario" enviando={citas.isFetchingNextPage} textoEnviando="Cargando…" onClick={() => citas.fetchNextPage()}>
              Ver más
            </Boton>
          )}
        </>
      )}
    </>
  )
}

function TarjetaCita({ cita }: { cita: Cita }) {
  return (
    <Link to={`/mis-citas/${cita.id}`} className={propias.tarjeta}>
      <span className={propias.cuando}>{formatearInstante(cita.inicio)}</span>
      <span className={propias.que}>
        {cita.servicio.nombre} <span className={estilos.suave}>con {cita.profesional.nombre}</span>
      </span>
      {cita.beneficiario && <span className={propias.para}>Para {cita.beneficiario.nombre}</span>}
      <span className={propias.pie}>
        <EstadoCita estado={cita.estado} />
        <span>{formatearPrecio(cita.precio)}</span>
      </span>
    </Link>
  )
}
