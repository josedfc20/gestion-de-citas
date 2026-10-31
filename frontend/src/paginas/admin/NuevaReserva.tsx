import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { consultaNegocio, consultaProfesionales, consultaServicios } from '../../api/catalogo'
import { ApiError, mensajeDeError } from '../../api/errores'
import { invalidarTrasGestion, reservarComoAdmin } from '../../api/gestionCitas'
import type { ClienteEncontrado, Negocio } from '../../api/tipos'
import { Aviso } from '../../componentes/Aviso'
import { Boton, EnlaceBoton } from '../../componentes/Boton'
import { BuscadorCliente } from '../../componentes/BuscadorCliente'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { inicioConDesfase } from '../../utilidades/fechas'
import { formatearDuracion, formatearFechaLarga, formatearPrecio } from '../../utilidades/formato'
import { useTitulo } from '../../utilidades/useTitulo'
import estilos from '../Paginas.module.css'
import { eleccionCompleta, eleccionDeParametros, enlaceHorarios, type Eleccion } from '../parametros'
import propias from '../Reservar.module.css'

/**
 * CU-019, alternativa 1: el admin elige la hora en /horarios (decisión 2 del B9)
 * y aquí elige al cliente registrado y para quién es la cita.
 */
export function NuevaReserva() {
  useTitulo('Nueva reserva')
  const [parametros] = useSearchParams()
  const negocio = useQuery(consultaNegocio)
  const eleccion = eleccionDeParametros(parametros)

  if (!eleccionCompleta(eleccion)) {
    return (
      <section className={estilos.texto}>
        <h1>Nueva reserva</h1>
        <Aviso>Elige primero un servicio, un profesional y una hora libre.</Aviso>
        <EnlaceBoton to={enlaceHorarios(eleccion)}>Ver horarios libres</EnlaceBoton>
      </section>
    )
  }
  if (negocio.isPending) return <Cargando />
  if (negocio.error) return <ErrorConsulta error={negocio.error} reintentar={() => negocio.refetch()} />
  return <Confirmar negocio={negocio.data} eleccion={eleccion} />
}

function Confirmar({ negocio, eleccion }: { negocio: Negocio; eleccion: Eleccion }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const servicios = useQuery(consultaServicios)
  const profesionales = useQuery(consultaProfesionales(eleccion.servicioId))
  const [cliente, setCliente] = useState<ClienteEncontrado | null>(null)
  const [para, setPara] = useState<number | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<{ texto: string; horaOcupada: boolean } | null>(null)

  const fallida = [servicios, profesionales].find((consulta) => consulta.error)
  if (fallida) return <ErrorConsulta error={fallida.error} reintentar={() => fallida.refetch()} />
  if (!servicios.data || !profesionales.data) return <Cargando />

  const servicio = servicios.data.find((s) => s.id === eleccion.servicioId)
  const profesional = profesionales.data.find((p) => p.id === eleccion.profesionalId)
  const otraHora = enlaceHorarios(eleccion)
  if (!servicio || !profesional) {
    return (
      <section className={estilos.texto}>
        <h1>Nueva reserva</h1>
        <Aviso>Ese servicio o profesional ya no está disponible.</Aviso>
        <EnlaceBoton to="/horarios">Ver horarios libres</EnlaceBoton>
      </section>
    )
  }

  function elegir(elegido: ClienteEncontrado | null) {
    setCliente(elegido)
    setPara(null)
    setError(null)
  }

  async function confirmar() {
    if (!cliente) return
    setError(null)
    setEnviando(true)
    try {
      const cita = await reservarComoAdmin({
        clienteId: cliente.id,
        servicioId: eleccion.servicioId,
        profesionalId: eleccion.profesionalId,
        inicio: inicioConDesfase(eleccion.fecha, eleccion.hora, negocio.zonaHoraria),
        beneficiarioId: para ?? undefined,
      })
      await invalidarTrasGestion(queryClient)
      navigate(`/admin/reservas/${cita.id}`, { replace: true, state: { reservada: true } })
    } catch (fallo) {
      const horaOcupada = fallo instanceof ApiError && fallo.status === 409
      if (horaOcupada) await queryClient.invalidateQueries({ queryKey: ['disponibilidad'] })
      setError({ texto: mensajeDeError(fallo), horaOcupada })
      setEnviando(false)
    }
  }

  return (
    <section className={propias.reserva}>
      <header className={estilos.cabecera}>
        <h1>Nueva reserva</h1>
        <p className={estilos.subtitulo}>Reserva en nombre de un cliente registrado, con las mismas reglas que en la web.</p>
      </header>

      <dl className={propias.resumen}>
        <div>
          <dt>Servicio</dt>
          <dd>
            {servicio.nombre} <span className={estilos.suave}>· {formatearDuracion(servicio.duracionMinutos)}</span>
          </dd>
        </div>
        <div>
          <dt>Profesional</dt>
          <dd>{profesional.nombre}</dd>
        </div>
        <div>
          <dt>Día y hora</dt>
          <dd>
            {formatearFechaLarga(eleccion.fecha)} a las {eleccion.hora}
            <span className={propias.nota}>Hora del negocio ({negocio.zonaHoraria}).</span>
          </dd>
        </div>
        <div>
          <dt>Precio</dt>
          <dd>{formatearPrecio(servicio.precio)}</dd>
        </div>
      </dl>
      <p>
        <EnlaceBoton to={otraHora} variante="enlace">
          Cambiar la hora
        </EnlaceBoton>
      </p>

      <fieldset className={propias.para}>
        <legend>Cliente</legend>
        {cliente ? (
          <p>
            <strong>
              {cliente.nombre} {cliente.apellidos}
            </strong>{' '}
            <span className={estilos.suave}>· {cliente.email}</span>{' '}
            <Boton variante="enlace" onClick={() => elegir(null)}>
              Cambiar de cliente
            </Boton>
          </p>
        ) : (
          <BuscadorCliente onElegir={elegir} />
        )}
      </fieldset>

      {cliente && (
        <fieldset className={propias.para}>
          <legend>¿Para quién es la cita?</legend>
          <label className={propias.opcion}>
            <input type="radio" name="para" checked={para === null} onChange={() => setPara(null)} />
            Para {cliente.nombre}
          </label>
          {cliente.beneficiarios.map((menor) => (
            <label key={menor.id} className={propias.opcion}>
              <input type="radio" name="para" checked={para === menor.id} onChange={() => setPara(menor.id)} />
              {menor.nombre} {menor.apellidos} (menor)
            </label>
          ))}
          <p className={estilos.suave}>Los menores a cargo los da de alta el propio cliente desde su cuenta.</p>
        </fieldset>
      )}

      {error && (
        <Aviso tipo="error">
          <p>{error.texto}</p>
          {error.horaOcupada && (
            <EnlaceBoton to={otraHora} variante="secundario">
              Elegir otra hora
            </EnlaceBoton>
          )}
        </Aviso>
      )}

      <Boton ancho enviando={enviando} textoEnviando="Reservando…" disabled={!cliente || error?.horaOcupada} onClick={confirmar}>
        Confirmar reserva
      </Boton>
    </section>
  )
}
