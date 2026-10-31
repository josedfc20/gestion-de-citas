import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useLocation, useParams } from 'react-router'

import { cancelar, consultaCita, invalidarTrasCambioDeCita } from '../api/citas'
import { ApiError, mensajeDeError } from '../api/errores'
import type { Cita } from '../api/tipos'
import { Aviso } from '../componentes/Aviso'
import { Boton, EnlaceBoton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { Cargando } from '../componentes/Cargando'
import { Confirmacion } from '../componentes/Confirmacion'
import { ErrorConsulta } from '../componentes/ErrorConsulta'
import { FichaCita } from '../componentes/FichaCita'
import { formatearInstante } from '../utilidades/formato'
import { useTitulo } from '../utilidades/useTitulo'
import { MOTIVO_MAX } from './esquemas'
import estilos from './Paginas.module.css'
import { numeroDeParametro } from './parametros'
import propias from './Reservar.module.css'

/** CU-011 y CU-012: detalle de una cita y cancelarla si aún está en plazo (RN-004). */
export function DetalleCita() {
  useTitulo('Detalle de la cita')
  const id = numeroDeParametro(useParams().id ?? null)
  const cita = useQuery({ ...consultaCita(id ?? 0), enabled: id !== undefined })

  if (id === undefined || (cita.error instanceof ApiError && cita.error.status === 404)) return <NoEncontrada />
  if (cita.isPending) return <Cargando />
  if (cita.error) return <ErrorConsulta error={cita.error} reintentar={() => cita.refetch()} />
  return <Detalle cita={cita.data} recargar={() => cita.refetch()} />
}

function NoEncontrada() {
  return (
    <section className={estilos.texto}>
      <h1>Cita no encontrada</h1>
      <p>No hemos encontrado esa cita entre las tuyas.</p>
      <EnlaceBoton to="/mis-citas">Ver mis citas</EnlaceBoton>
    </section>
  )
}

function Detalle({ cita, recargar }: { cita: Cita; recargar: () => void }) {
  const queryClient = useQueryClient()
  const reservada = (useLocation().state as { reservada?: boolean } | null)?.reservada ?? false
  const [confirmando, setConfirmando] = useState(false)
  const [motivo, setMotivo] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [cancelada, setCancelada] = useState(false)

  async function confirmarCancelacion() {
    setError(null)
    setEnviando(true)
    try {
      const actualizada = await cancelar(cita.id, motivo)
      queryClient.setQueryData(consultaCita(cita.id).queryKey, actualizada)
      await invalidarTrasCambioDeCita(queryClient)
      setCancelada(true)
      setConfirmando(false)
    } catch (fallo) {
      setError(mensajeDeError(fallo))
      // Si el plazo venció mientras miraba la página, la cita ya dice que no se puede cancelar.
      if (fallo instanceof ApiError && fallo.status === 422) {
        setConfirmando(false)
        recargar()
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className={propias.reserva}>
      {reservada && !cancelada && <Aviso tipo="exito">Cita reservada. ¡Te esperamos!</Aviso>}
      {cancelada && <Aviso tipo="exito">Cita cancelada.</Aviso>}
      <FichaCita cita={cita} />

      {error && <Aviso tipo="error">{error}</Aviso>}

      {cita.estado === 'CONFIRMADA' &&
        (cita.puedeCancelar ? (
          <div className={estilos.texto}>
            <p>Puedes cancelarla online hasta el {formatearInstante(cita.cancelableHasta, false)}.</p>
            {confirmando ? (
              <Confirmacion
                titulo="¿Seguro que quieres cancelar esta cita?"
                textoConfirmar="Sí, cancelar"
                textoEnviando="Cancelando…"
                enviando={enviando}
                peligro
                onConfirmar={confirmarCancelacion}
                onVolver={() => setConfirmando(false)}
              >
                <Campo
                  etiqueta="Motivo"
                  opcional
                  maxLength={MOTIVO_MAX}
                  value={motivo}
                  onChange={(evento) => setMotivo(evento.target.value)}
                />
              </Confirmacion>
            ) : (
              <Boton variante="secundario" onClick={() => setConfirmando(true)}>
                Cancelar cita
              </Boton>
            )}
          </div>
        ) : (
          <Aviso>Ya no puedes cancelarla online. Si no puedes venir, contacta con el negocio.</Aviso>
        ))}

      <p>
        <EnlaceBoton to="/mis-citas" variante="enlace">
          Volver a mis citas
        </EnlaceBoton>
      </p>
    </section>
  )
}
