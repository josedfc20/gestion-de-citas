import type { EstadoCita } from '../api/tipos'

/** Lee un id de la URL: un número entero positivo, o undefined si falta o no es válido. */
export function numeroDeParametro(valor: string | null): number | undefined {
  if (!valor || !/^\d+$/.test(valor)) return undefined
  const numero = Number(valor)
  return numero > 0 ? numero : undefined
}

/** Lee una fecha AAAA-MM-DD de la URL, o undefined si no tiene ese formato. */
export function fechaDeParametro(valor: string | null): string | undefined {
  return valor && /^\d{4}-\d{2}-\d{2}$/.test(valor) ? valor : undefined
}

/** Lee una hora HH:mm de la URL, o undefined si no tiene ese formato. */
export function horaDeParametro(valor: string | null): string | undefined {
  return valor && /^([01]\d|2[0-3]):[0-5]\d$/.test(valor) ? valor : undefined
}

export const ESTADOS: { valor: EstadoCita; texto: string }[] = [
  { valor: 'CONFIRMADA', texto: 'Confirmadas' },
  { valor: 'COMPLETADA', texto: 'Completadas' },
  { valor: 'CANCELADA', texto: 'Canceladas' },
  { valor: 'NO_PRESENTADO', texto: 'No presentado' },
]

/** Lee los filtros comunes de la URL (?estado=&desde=&hasta=). */
export function filtrosDeParametros(parametros: URLSearchParams) {
  const estado = ESTADOS.find((opcion) => opcion.valor === parametros.get('estado'))?.valor
  return {
    estado,
    desde: fechaDeParametro(parametros.get('desde')),
    hasta: fechaDeParametro(parametros.get('hasta')),
  }
}

/** Servicio, profesional, día y hora elegidos en /horarios (B8 y B9b). */
export interface Eleccion {
  servicioId: number
  profesionalId: number
  fecha: string
  hora: string
}

/** Dirección de /horarios con lo ya elegido, para cambiar la hora o elegir otra. */
export function enlaceHorarios(eleccion: Partial<Eleccion>): string {
  const parametros = new URLSearchParams()
  if (eleccion.servicioId) parametros.set('servicio', String(eleccion.servicioId))
  if (eleccion.profesionalId) parametros.set('profesional', String(eleccion.profesionalId))
  if (eleccion.fecha) parametros.set('fecha', eleccion.fecha)
  const texto = parametros.toString()
  return texto ? `/horarios?${texto}` : '/horarios'
}

/** Lee la elección de la URL (?servicio=&profesional=&fecha=&hora=). */
export function eleccionDeParametros(parametros: URLSearchParams): Partial<Eleccion> {
  return {
    servicioId: numeroDeParametro(parametros.get('servicio')),
    profesionalId: numeroDeParametro(parametros.get('profesional')),
    fecha: fechaDeParametro(parametros.get('fecha')),
    hora: horaDeParametro(parametros.get('hora')),
  }
}

export function eleccionCompleta(eleccion: Partial<Eleccion>): eleccion is Eleccion {
  return Boolean(eleccion.servicioId && eleccion.profesionalId && eleccion.fecha && eleccion.hora)
}
