import { useInfiniteQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'

import { consultaCitasAsignadas } from '../../api/empleado'
import { Boton } from '../../componentes/Boton'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { FiltrosCitas } from '../../componentes/FiltrosCitas'
import { TarjetaCita } from '../../componentes/TarjetaCita'
import { useTitulo } from '../../utilidades/useTitulo'
import propias from '../MisCitas.module.css'
import estilos from '../Paginas.module.css'
import { filtrosDeParametros } from '../parametros'

/** CU-014: las citas asignadas al empleado, con filtros de estado y fechas. */
export function CitasAsignadas() {
  useTitulo('Mis citas asignadas')
  const [parametros, setParametros] = useSearchParams()
  const citas = useInfiniteQuery(consultaCitasAsignadas(filtrosDeParametros(parametros)))
  const lista = citas.data?.pages.flatMap((pagina) => pagina.contenido) ?? []

  return (
    <>
      <header className={estilos.cabecera}>
        <h1>Mis citas asignadas</h1>
      </header>
      <FiltrosCitas parametros={parametros} setParametros={setParametros} />

      {citas.isPending ? (
        <Cargando texto="Cargando tus citas…" />
      ) : citas.error ? (
        <ErrorConsulta error={citas.error} reintentar={() => citas.refetch()} />
      ) : lista.length === 0 ? (
        <p>No hay citas con estos filtros.</p>
      ) : (
        <>
          <ul className={propias.lista}>
            {lista.map((cita) => (
              <li key={cita.id}>
                <TarjetaCita cita={cita} enlace={`/empleado/citas/${cita.id}`} vista="gestion" />
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
