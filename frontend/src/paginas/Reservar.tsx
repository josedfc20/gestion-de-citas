import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { consultaBeneficiarios } from '../api/beneficiarios'
import { consultaNegocio, consultaProfesionales, consultaServicios } from '../api/catalogo'
import { invalidarTrasCambioDeCita, reservar } from '../api/citas'
import { ApiError, mensajeDeError } from '../api/errores'
import type { Negocio } from '../api/tipos'
import { Aviso } from '../componentes/Aviso'
import { Boton, EnlaceBoton } from '../componentes/Boton'
import { Cargando } from '../componentes/Cargando'
import { ErrorConsulta } from '../componentes/ErrorConsulta'
import { FormularioMenor } from '../componentes/FormularioMenor'
import { inicioConDesfase } from '../utilidades/fechas'
import { formatearDuracion, formatearFechaLarga, formatearPrecio } from '../utilidades/formato'
import { useTitulo } from '../utilidades/useTitulo'
import estilos from './Paginas.module.css'
import { fechaDeParametro, horaDeParametro, numeroDeParametro } from './parametros'
import propias from './Reservar.module.css'

interface Eleccion {
  servicioId: number
  profesionalId: number
  fecha: string
  hora: string
}

/** Dirección de /horarios con lo ya elegido, para cambiar la hora o elegir otra. */
function enlaceHorarios(eleccion: Partial<Eleccion>): string {
  const parametros = new URLSearchParams()
  if (eleccion.servicioId) parametros.set('servicio', String(eleccion.servicioId))
  if (eleccion.profesionalId) parametros.set('profesional', String(eleccion.profesionalId))
  if (eleccion.fecha) parametros.set('fecha', eleccion.fecha)
  const texto = parametros.toString()
  return texto ? `/horarios?${texto}` : '/horarios'
}

/**
 * CU-008 y CU-009: confirmar la hora elegida en /horarios, para el cliente o
 * para un menor a su cargo. La disponibilidad la vuelve a comprobar el servidor.
 */
export function Reservar() {
  useTitulo('Reservar cita')
  const [parametros] = useSearchParams()
  const negocio = useQuery(consultaNegocio)

  const eleccion: Partial<Eleccion> = {
    servicioId: numeroDeParametro(parametros.get('servicio')),
    profesionalId: numeroDeParametro(parametros.get('profesional')),
    fecha: fechaDeParametro(parametros.get('fecha')),
    hora: horaDeParametro(parametros.get('hora')),
  }
  const completa = eleccion.servicioId && eleccion.profesionalId && eleccion.fecha && eleccion.hora

  if (!completa) return <SinHora enlace={enlaceHorarios(eleccion)} />
  if (negocio.isPending) return <Cargando />
  if (negocio.error) return <ErrorConsulta error={negocio.error} reintentar={() => negocio.refetch()} />
  return <ConfirmarReserva negocio={negocio.data} eleccion={eleccion as Eleccion} />
}

function SinHora({ enlace }: { enlace: string }) {
  return (
    <section className={estilos.texto}>
      <h1>Reservar cita</h1>
      <Aviso>Elige primero un servicio, un profesional y una hora libre.</Aviso>
      <EnlaceBoton to={enlace}>Ver horarios libres</EnlaceBoton>
    </section>
  )
}

function ConfirmarReserva({ negocio, eleccion }: { negocio: Negocio; eleccion: Eleccion }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const servicios = useQuery(consultaServicios)
  const profesionales = useQuery(consultaProfesionales(eleccion.servicioId))
  const menores = useQuery(consultaBeneficiarios)

  const [para, setPara] = useState<number | null>(null)
  const [anadiendo, setAnadiendo] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<{ texto: string; horaOcupada: boolean } | null>(null)

  const consultas = [servicios, profesionales, menores]
  const fallida = consultas.find((consulta) => consulta.error)
  if (fallida) return <ErrorConsulta error={fallida.error} reintentar={() => fallida.refetch()} />
  if (!servicios.data || !profesionales.data || !menores.data) return <Cargando />

  const servicio = servicios.data.find((s) => s.id === eleccion.servicioId)
  const profesional = profesionales.data.find((p) => p.id === eleccion.profesionalId)
  if (!servicio || !profesional) return <SinHora enlace={enlaceHorarios({ servicioId: servicio?.id })} />

  async function confirmar() {
    setError(null)
    setEnviando(true)
    try {
      const cita = await reservar({
        servicioId: eleccion.servicioId,
        profesionalId: eleccion.profesionalId,
        inicio: inicioConDesfase(eleccion.fecha, eleccion.hora, negocio.zonaHoraria),
        beneficiarioId: para ?? undefined,
      })
      await invalidarTrasCambioDeCita(queryClient)
      navigate(`/mis-citas/${cita.id}`, { replace: true, state: { reservada: true } })
    } catch (fallo) {
      const horaOcupada = fallo instanceof ApiError && fallo.status === 409
      if (horaOcupada) await queryClient.invalidateQueries({ queryKey: ['disponibilidad'] })
      setError({ texto: mensajeDeError(fallo), horaOcupada })
      setEnviando(false)
    }
  }

  const otraHora = enlaceHorarios(eleccion)

  return (
    <section className={propias.reserva}>
      <header className={estilos.cabecera}>
        <h1>Reservar cita</h1>
        <p className={estilos.subtitulo}>Revisa los datos y confirma la reserva.</p>
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
        <legend>¿Para quién es la cita?</legend>
        <label className={propias.opcion}>
          <input type="radio" name="para" checked={para === null} onChange={() => setPara(null)} />
          Para mí
        </label>
        {menores.data.map((menor) => (
          <label key={menor.id} className={propias.opcion}>
            <input type="radio" name="para" checked={para === menor.id} onChange={() => setPara(menor.id)} />
            {menor.nombre} {menor.apellidos}
          </label>
        ))}
        {anadiendo ? (
          <FormularioMenor
            negocio={negocio}
            onGuardado={(menor) => {
              setPara(menor.id)
              setAnadiendo(false)
            }}
            onCancelar={() => setAnadiendo(false)}
          />
        ) : (
          <Boton variante="enlace" onClick={() => setAnadiendo(true)}>
            Añadir un menor a cargo
          </Boton>
        )}
      </fieldset>

      <p className={estilos.suave}>Podrás cancelarla online hasta {negocio.horasMinCancelacion} h antes.</p>

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

      <Boton ancho enviando={enviando} textoEnviando="Reservando…" disabled={anadiendo || error?.horaOcupada} onClick={confirmar}>
        Confirmar reserva
      </Boton>
    </section>
  )
}
