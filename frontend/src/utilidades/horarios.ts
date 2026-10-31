// Editor de horarios (CU-018): una semana de lunes (1) a domingo (7).

import type { DiaHorario, Tramo } from '../api/tipos'

/** Tramos de cada día. En el horario de un empleado, null = "como el horario general". */
export type Semana = Record<number, Tramo[] | null>

export const DIAS_SEMANA = [1, 2, 3, 4, 5, 6, 7] as const
export const MAX_TRAMOS = 4

/** De la respuesta de la API (solo los días que trae) a una semana completa. */
export function semanaDe(dias: DiaHorario[], sinDato: [] | null): Semana {
  const semana: Semana = {}
  for (const dia of DIAS_SEMANA) semana[dia] = sinDato === null ? null : []
  for (const dia of dias) semana[dia.diaSemana] = dia.tramos
  return semana
}

/** De la semana a la API: los días con null no se envían (siguen el horario general). */
export function diasDe(semana: Semana): DiaHorario[] {
  return DIAS_SEMANA.flatMap((dia) => {
    const tramos = semana[dia]
    return tramos === null || tramos === undefined ? [] : [{ diaSemana: dia, tramos }]
  })
}

const minutos = (hora: string) => Number(hora.slice(0, 2)) * 60 + Number(hora.slice(3, 5))

/**
 * Lo básico antes de enviar (lo mismo que TramoDto): las dos horas, inicio antes
 * que fin y múltiplos de 5 minutos. Que no se solapen ni se toquen lo decide el
 * servidor (422), que nombra el día.
 */
export function erroresDeSemana(semana: Semana): Record<number, string> {
  const errores: Record<number, string> = {}
  for (const dia of DIAS_SEMANA) {
    for (const tramo of semana[dia] ?? []) {
      if (!/^\d{2}:\d{2}$/.test(tramo.inicio) || !/^\d{2}:\d{2}$/.test(tramo.fin)) {
        errores[dia] = 'Indica la hora de inicio y la de fin de cada tramo.'
      } else if (minutos(tramo.inicio) >= minutos(tramo.fin)) {
        errores[dia] = 'La hora de inicio debe ser anterior a la de fin.'
      } else if (minutos(tramo.inicio) % 5 !== 0 || minutos(tramo.fin) % 5 !== 0) {
        errores[dia] = 'Las horas van en múltiplos de 5 minutos (09:00, 09:05...).'
      }
      if (errores[dia]) break
    }
  }
  return errores
}
