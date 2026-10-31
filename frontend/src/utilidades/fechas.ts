// Fechas en la zona horaria del negocio: "hoy" para el negocio puede no ser
// "hoy" para el navegador (por ejemplo, alguien que mira desde Canarias o de viaje).

/** Fecha de hoy ("2026-10-31") en la zona horaria indicada. */
export function hoyEnZona(zonaHoraria: string, ahora: Date = new Date()): string {
  // en-CA formatea como AAAA-MM-DD.
  return new Intl.DateTimeFormat('en-CA', { timeZone: zonaHoraria, year: 'numeric', month: '2-digit', day: '2-digit' }).format(ahora)
}

/** Suma días a una fecha ISO ("2026-10-31" + 2 → "2026-11-02"). */
export function sumarDias(isoFecha: string, dias: number): string {
  const [anio, mes, dia] = isoFecha.split('-').map(Number)
  const fecha = new Date(Date.UTC(anio, mes - 1, dia + dias))
  return fecha.toISOString().slice(0, 10)
}

/** Edad cumplida en una fecha dada (ambas en formato ISO). */
export function edadEn(fechaNacimiento: string, isoHoy: string): number {
  const [an, mn, dn] = fechaNacimiento.split('-').map(Number)
  const [ah, mh, dh] = isoHoy.split('-').map(Number)
  const cumplidoEsteAnio = mh > mn || (mh === mn && dh >= dn)
  return ah - an - (cumplidoEsteAnio ? 0 : 1)
}

/** Desfase en minutos de la zona respecto a UTC en un instante (Madrid: 60 en invierno, 120 en verano). */
export function desfaseEnMinutos(zonaHoraria: string, instante: number): number {
  const partes = new Intl.DateTimeFormat('en-US', { timeZone: zonaHoraria, timeZoneName: 'longOffset' }).formatToParts(new Date(instante))
  const texto = partes.find((parte) => parte.type === 'timeZoneName')?.value ?? 'GMT'
  const coincide = /GMT([+-])(\d{2}):(\d{2})/.exec(texto)
  if (!coincide) return 0 // "GMT" a secas: UTC
  const minutos = Number(coincide[2]) * 60 + Number(coincide[3])
  return coincide[1] === '-' ? -minutos : minutos
}

/**
 * Instante con desfase para POST /citas a partir de la fecha y la hora locales
 * del negocio: ("2026-11-03", "10:00", "Europe/Madrid") → "2026-11-03T10:00:00+01:00".
 * El desfase es el de ese día (verano o invierno), no el de hoy. El servidor
 * comprueba que el instante sea justo una hora libre, así que un error aquí
 * nunca reserva otra hora (daría 409).
 */
export function inicioConDesfase(isoFecha: string, hora: string, zonaHoraria: string): string {
  const [anio, mes, dia] = isoFecha.split('-').map(Number)
  const [hh, mm] = hora.split(':').map(Number)
  const comoSiFueraUtc = Date.UTC(anio, mes - 1, dia, hh, mm)
  // Dos pasadas: el desfase hay que mirarlo en el instante real, no en el aproximado.
  const primero = desfaseEnMinutos(zonaHoraria, comoSiFueraUtc)
  const desfase = desfaseEnMinutos(zonaHoraria, comoSiFueraUtc - primero * 60_000)
  const signo = desfase < 0 ? '-' : '+'
  const absoluto = Math.abs(desfase)
  const dosCifras = (n: number) => String(n).padStart(2, '0')
  return `${isoFecha}T${hora}:00${signo}${dosCifras(Math.floor(absoluto / 60))}:${dosCifras(absoluto % 60)}`
}

/**
 * Fecha y hora locales de un instante que ya viene con el desfase del negocio
 * ("2026-11-03T10:00:00+01:00" → { fecha: "2026-11-03", hora: "10:00" }).
 * Se leen tal cual para no convertirlos a la zona del navegador.
 */
export function partesDeInstante(instante: string): { fecha: string; hora: string } {
  return { fecha: instante.slice(0, 10), hora: instante.slice(11, 16) }
}

/** Día de la semana ISO de una fecha: 1 = lunes ... 7 = domingo. */
export function diaDeLaSemana(isoFecha: string): number {
  const [anio, mes, dia] = isoFecha.split('-').map(Number)
  const domingoCero = new Date(Date.UTC(anio, mes - 1, dia)).getUTCDay()
  return domingoCero === 0 ? 7 : domingoCero
}

/** Lunes de la semana de una fecha ("2026-11-08", domingo → "2026-11-02"). */
export function lunesDe(isoFecha: string): string {
  return sumarDias(isoFecha, 1 - diaDeLaSemana(isoFecha))
}

/**
 * ¿Ha llegado ya la hora de inicio de una cita? El instante lleva su desfase,
 * así que se compara bien esté donde esté el navegador. Solo sirve para no
 * ofrecer un botón que fallaría: quien decide es el servidor.
 */
export function yaHaEmpezado(instante: string, ahora: number = Date.now()): boolean {
  return Date.parse(instante) <= ahora
}
