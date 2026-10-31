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
