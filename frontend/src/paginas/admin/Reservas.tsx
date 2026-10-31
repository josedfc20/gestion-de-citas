import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'

import { consultaProfesionales } from '../../api/catalogo'
import { consultaReservas } from '../../api/gestionCitas'
import { Boton, EnlaceBoton } from '../../componentes/Boton'
import { BuscadorCliente } from '../../componentes/BuscadorCliente'
import { CampoSeleccion } from '../../componentes/Campo'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { FiltrosCitas } from '../../componentes/FiltrosCitas'
import { TarjetaCita } from '../../componentes/TarjetaCita'
import { useTitulo } from '../../utilidades/useTitulo'
import propias from '../MisCitas.module.css'
import estilos from '../Paginas.module.css'
import { filtrosDeParametros, numeroDeParametro } from '../parametros'
import admin from './Admin.module.css'

/** CU-019: todas las reservas, con filtros de estado, fechas, profesional y cliente en la URL. */
export function Reservas() {
  useTitulo('Reservas')
  const [parametros, setParametros] = useSearchParams()
  const profesionalId = numeroDeParametro(parametros.get('profesional'))
  const clienteId = numeroDeParametro(parametros.get('cliente'))
  const nombreCliente = parametros.get('nombreCliente') ?? `cliente ${clienteId}`
  const profesionales = useQuery(consultaProfesionales())
  const reservas = useInfiniteQuery(consultaReservas({ ...filtrosDeParametros(parametros), profesionalId, clienteId }))
  const lista = reservas.data?.pages.flatMap((pagina) => pagina.contenido) ?? []

  function cambiar(cambios: Record<string, string | undefined>) {
    const nuevos = new URLSearchParams(parametros)
    for (const [clave, valor] of Object.entries(cambios)) {
      if (valor) nuevos.set(clave, valor)
      else nuevos.delete(clave)
    }
    setParametros(nuevos, { replace: true })
  }

  return (
    <>
      <header className={`${estilos.cabecera} ${admin.cabecera}`}>
        <h1>Reservas</h1>
        <EnlaceBoton to="/horarios">Nueva reserva</EnlaceBoton>
      </header>

      <FiltrosCitas parametros={parametros} setParametros={setParametros} />
      <div className={admin.filtrosExtra}>
        <CampoSeleccion etiqueta="Profesional" value={profesionalId ?? ''} onChange={(evento) => cambiar({ profesional: evento.target.value })}>
          <option value="">Todos</option>
          {profesionales.data?.map((profesional) => (
            <option key={profesional.id} value={profesional.id}>
              {profesional.nombre}
            </option>
          ))}
        </CampoSeleccion>
        {clienteId ? (
          <p className={admin.clienteElegido}>
            Solo las de <strong>{nombreCliente}</strong>{' '}
            <Boton variante="enlace" onClick={() => cambiar({ cliente: undefined, nombreCliente: undefined })}>
              Ver todos los clientes
            </Boton>
          </p>
        ) : (
          <details className={admin.desplegable}>
            <summary>Filtrar por cliente</summary>
            <BuscadorCliente
              onElegir={(cliente) => cambiar({ cliente: String(cliente.id), nombreCliente: `${cliente.nombre} ${cliente.apellidos}` })}
            />
          </details>
        )}
      </div>

      {reservas.isPending ? (
        <Cargando texto="Cargando reservas…" />
      ) : reservas.error ? (
        <ErrorConsulta error={reservas.error} reintentar={() => reservas.refetch()} />
      ) : lista.length === 0 ? (
        <p>No hay reservas con estos filtros.</p>
      ) : (
        <>
          <ul className={`${propias.lista} ${admin.lista}`}>
            {lista.map((cita) => (
              <li key={cita.id}>
                <TarjetaCita cita={cita} enlace={`/admin/reservas/${cita.id}`} vista="gestion" />
              </li>
            ))}
          </ul>
          {reservas.hasNextPage && (
            <Boton variante="secundario" enviando={reservas.isFetchingNextPage} textoEnviando="Cargando…" onClick={() => reservas.fetchNextPage()}>
              Ver más
            </Boton>
          )}
        </>
      )}
    </>
  )
}
