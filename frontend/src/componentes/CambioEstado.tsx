import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import { consultaCita } from '../api/citas'
import { mensajeDeError } from '../api/errores'
import { cambiarEstado, invalidarTrasGestion } from '../api/gestionCitas'
import type { Cita, DatosCambioEstado } from '../api/tipos'
import { esquemaCancelacionGestion, MOTIVO_MAX } from '../paginas/esquemas'
import { partesDeInstante, yaHaEmpezado } from '../utilidades/fechas'
import { Aviso } from './Aviso'
import { Boton } from './Boton'
import { Campo } from './Campo'
import estilos from './CambioEstado.module.css'
import { Confirmacion } from './Confirmacion'

type Accion = DatosCambioEstado['estado']

const TEXTO_HECHO: Record<Accion, string> = {
  COMPLETADA: 'Cita marcada como completada.',
  NO_PRESENTADO: 'Cita marcada como no presentado.',
  CANCELADA: 'Cita cancelada.',
}

const PREGUNTA: Record<Accion, string> = {
  COMPLETADA: '¿Marcar la cita como completada?',
  NO_PRESENTADO: '¿Marcar que el cliente no se presentó?',
  CANCELADA: '¿Seguro que quieres cancelar esta cita?',
}

const CONFIRMAR: Record<Accion, string> = {
  COMPLETADA: 'Sí, completada',
  NO_PRESENTADO: 'Sí, no se presentó',
  CANCELADA: 'Sí, cancelar',
}

interface Props {
  cita: Cita
  /** Para recargar la cita si el servidor la rechaza (por ejemplo, otro ya la cambió). */
  recargar: () => void
}

/**
 * CU-015 y CU-019 (RN-008): el empleado (sus citas) y el admin cambian el estado
 * de una cita confirmada. Completada y No presentado solo se ofrecen desde la
 * hora de inicio (decisión 5 del B9); cancelar pide siempre el motivo.
 */
export function CambioEstado({ cita, recargar }: Props) {
  const queryClient = useQueryClient()
  const [accion, setAccion] = useState<Accion | null>(null)
  const [motivo, setMotivo] = useState('')
  const [errorMotivo, setErrorMotivo] = useState<string | undefined>()
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [hecho, setHecho] = useState<string | null>(null)

  async function confirmar() {
    if (!accion) return
    setError(null)
    if (accion === 'CANCELADA') {
      const resultado = esquemaCancelacionGestion.safeParse({ motivo })
      if (!resultado.success) {
        setErrorMotivo(resultado.error.issues[0]?.message)
        return
      }
    }
    setErrorMotivo(undefined)
    setEnviando(true)
    try {
      const actualizada = await cambiarEstado(cita.id, { estado: accion, motivo: accion === 'CANCELADA' ? motivo : undefined })
      queryClient.setQueryData(consultaCita(cita.id).queryKey, actualizada)
      await invalidarTrasGestion(queryClient)
      setHecho(TEXTO_HECHO[accion])
      setAccion(null)
      setMotivo('')
    } catch (fallo) {
      setError(mensajeDeError(fallo))
      setAccion(null)
      recargar()
    } finally {
      setEnviando(false)
    }
  }

  const empezada = yaHaEmpezado(cita.inicio)

  return (
    <div className={estilos.cambio}>
      {hecho && <Aviso tipo="exito">{hecho}</Aviso>}
      {error && <Aviso tipo="error">{error}</Aviso>}
      {cita.estado === 'CONFIRMADA' && (
        <section aria-labelledby="titulo-cambio-estado" className={estilos.cambio}>
          <h2 id="titulo-cambio-estado">Actualizar la cita</h2>
          {accion ? (
            <Confirmacion
              titulo={PREGUNTA[accion]}
              textoConfirmar={CONFIRMAR[accion]}
              textoEnviando="Guardando…"
              enviando={enviando}
              peligro={accion === 'CANCELADA'}
              onConfirmar={confirmar}
              onVolver={() => {
                setAccion(null)
                setErrorMotivo(undefined)
              }}
            >
              {accion === 'CANCELADA' && (
                <Campo
                  etiqueta="Motivo"
                  ayuda="El cliente lo verá en el detalle de su cita."
                  maxLength={MOTIVO_MAX}
                  value={motivo}
                  error={errorMotivo}
                  onChange={(evento) => setMotivo(evento.target.value)}
                />
              )}
            </Confirmacion>
          ) : (
            <>
              {!empezada && (
                <p>Podrás marcarla como completada o no presentado a partir de las {partesDeInstante(cita.inicio).hora} del día de la cita.</p>
              )}
              <div className={estilos.botones}>
                {empezada && (
                  <>
                    <Boton onClick={() => setAccion('COMPLETADA')}>Marcar como completada</Boton>
                    <Boton variante="secundario" onClick={() => setAccion('NO_PRESENTADO')}>
                      No se presentó
                    </Boton>
                  </>
                )}
                <Boton variante="secundario" onClick={() => setAccion('CANCELADA')}>
                  Cancelar cita
                </Boton>
              </div>
            </>
          )}
        </section>
      )}
    </div>
  )
}
