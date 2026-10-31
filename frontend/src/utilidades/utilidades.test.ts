import { describe, expect, it } from 'vitest'

import { edadEn, hoyEnZona, lunesDe, sumarDias, yaHaEmpezado } from './fechas'
import { formatearDuracion, formatearFechaLarga, formatearPrecio, formatearTramos, personaDeLaCita } from './formato'

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

describe('semana y hora de inicio (B9a)', () => {
  it('el lunes de la semana, también si es domingo o ya es lunes', () => {
    expect(lunesDe('2026-11-05')).toBe('2026-11-02')
    expect(lunesDe('2026-11-08')).toBe('2026-11-02')
    expect(lunesDe('2026-11-02')).toBe('2026-11-02')
    expect(lunesDe('2027-01-01')).toBe('2026-12-28')
  })

  it('compara la hora de inicio con su desfase', () => {
    const ahora = Date.parse('2026-11-03T09:00:00Z')
    expect(yaHaEmpezado('2026-11-03T10:00:00+01:00', ahora)).toBe(true)
    expect(yaHaEmpezado('2026-11-03T10:30:00+01:00', ahora)).toBe(false)
  })

  it('para quién es la cita, visto por el empleado', () => {
    const cliente = { nombre: 'Marta', apellidos: 'Pérez' }
    expect(personaDeLaCita({ cliente, beneficiario: null })).toBe('Marta Pérez')
    expect(personaDeLaCita({ cliente, beneficiario: { nombre: 'Lucía' } })).toBe('Lucía (menor), a cargo de Marta Pérez')
    expect(formatearTramos([{ inicio: '09:00', fin: '14:00' }, { inicio: '16:00', fin: '20:00' }])).toBe('09:00–14:00 · 16:00–20:00')
  })
})
