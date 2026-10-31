import type { Tramo } from '../api/tipos'
import { formatearTramos, NOMBRE_DIA } from '../utilidades/formato'
import { DIAS_SEMANA, MAX_TRAMOS, type Semana } from '../utilidades/horarios'
import { Boton } from './Boton'
import estilos from './EditorSemana.module.css'

interface Props {
  semana: Semana
  onCambiar: (semana: Semana) => void
  errores?: Record<number, string>
  /**
   * Horario de un empleado: cada día puede seguir el horario general (null) o
   * tener el suyo. Se pasa el general para mostrarlo.
   */
  general?: Semana
}

/** Editor de una semana de horario (CU-018): hasta 4 tramos por día, en pasos de 5 minutos. */
export function EditorSemana({ semana, onCambiar, errores = {}, general }: Props) {
  const deEmpleado = general !== undefined

  function cambiarDia(dia: number, tramos: Tramo[] | null) {
    onCambiar({ ...semana, [dia]: tramos })
  }

  return (
    <ul className={estilos.semana}>
      {DIAS_SEMANA.map((dia) => {
        const tramos = semana[dia]
        const nombre = NOMBRE_DIA[dia]
        const idError = `error-dia-${dia}`
        return (
          <li key={dia}>
            <fieldset className={estilos.dia} aria-describedby={errores[dia] ? idError : undefined}>
              <legend>{nombre}</legend>
              {deEmpleado && (
                <div className={estilos.modo}>
                  <label>
                    <input type="radio" name={`modo-${dia}`} checked={tramos === null} onChange={() => cambiarDia(dia, null)} />
                    Como el horario general ({textoDia(general[dia] ?? [], 'cerrado')})
                  </label>
                  <label>
                    <input type="radio" name={`modo-${dia}`} checked={tramos !== null} onChange={() => cambiarDia(dia, general[dia] ?? [])} />
                    Horario propio
                  </label>
                </div>
              )}
              {tramos !== null && (
                <>
                  {tramos.length === 0 ? (
                    <p className={estilos.suave}>{deEmpleado ? 'No trabaja este día.' : 'Cerrado.'}</p>
                  ) : (
                    <ul className={estilos.tramos}>
                      {tramos.map((tramo, indice) => (
                        <li key={indice} className={estilos.tramo}>
                          <input
                            type="time"
                            step={300}
                            aria-label={`${nombre}, tramo ${indice + 1}: inicio`}
                            value={tramo.inicio}
                            onChange={(evento) => cambiarDia(dia, tramos.map((t, i) => (i === indice ? { ...t, inicio: evento.target.value } : t)))}
                          />
                          <span aria-hidden="true">–</span>
                          <input
                            type="time"
                            step={300}
                            aria-label={`${nombre}, tramo ${indice + 1}: fin`}
                            value={tramo.fin}
                            onChange={(evento) => cambiarDia(dia, tramos.map((t, i) => (i === indice ? { ...t, fin: evento.target.value } : t)))}
                          />
                          <Boton
                            variante="enlace"
                            aria-label={`Quitar el tramo ${indice + 1} del ${nombre.toLowerCase()}`}
                            onClick={() => cambiarDia(dia, tramos.filter((_, i) => i !== indice))}
                          >
                            Quitar
                          </Boton>
                        </li>
                      ))}
                    </ul>
                  )}
                  {tramos.length < MAX_TRAMOS && (
                    <Boton
                      variante="enlace"
                      aria-label={`Añadir tramo al ${nombre.toLowerCase()}`}
                      onClick={() => cambiarDia(dia, [...tramos, tramos.length === 0 ? { inicio: '09:00', fin: '14:00' } : { inicio: '', fin: '' }])}
                    >
                      Añadir tramo
                    </Boton>
                  )}
                </>
              )}
              {errores[dia] && (
                <p id={idError} className={estilos.error}>
                  {errores[dia]}
                </p>
              )}
            </fieldset>
          </li>
        )
      })}
    </ul>
  )
}

function textoDia(tramos: Tramo[], vacio: string): string {
  return tramos.length === 0 ? vacio : formatearTramos(tramos)
}
