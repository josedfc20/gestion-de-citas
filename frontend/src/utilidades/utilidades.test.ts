import { describe, expect, it } from 'vitest'

import { edadEn, hoyEnZona, sumarDias } from './fechas'
import { formatearDuracion, formatearFechaLarga, formatearPrecio } from './formato'

describe('formatos', () => {
  it('formatea duraciones', () => {
    expect(formatearDuracion(45)).toBe('45 min')
    expect(formatearDuracion(60)).toBe('1 h')
    expect(formatearDuracion(90)).toBe('1 h 30 min')
  })

  it('formatea precios en euros', () => {
    expect(formatearPrecio(45.5).replace(/\s/g, ' ')).toBe('45,50 €')
  })

  it('formatea una fecha sin moverla de día', () => {
    expect(formatearFechaLarga('2026-11-03')).toBe('martes, 3 de noviembre de 2026')
  })
})

describe('fechas', () => {
  it('calcula hoy en la zona del negocio, no en la del navegador', () => {
    // 31/10 a las 23:30 UTC ya es 1/11 en Madrid (UTC+1).
    const ahora = new Date('2026-10-31T23:30:00Z')
    expect(hoyEnZona('Europe/Madrid', ahora)).toBe('2026-11-01')
    expect(hoyEnZona('Atlantic/Canary', ahora)).toBe('2026-10-31')
  })

  it('suma días cambiando de mes y de año', () => {
    expect(sumarDias('2026-10-31', 1)).toBe('2026-11-01')
    expect(sumarDias('2026-12-31', 60)).toBe('2027-03-01')
  })

  it('calcula la edad cumplida (el día del cumpleaños ya cuenta)', () => {
    expect(edadEn('2012-10-31', '2026-10-31')).toBe(14)
    expect(edadEn('2012-11-01', '2026-10-31')).toBe(13)
  })
})
