import { describe, expect, it } from 'vitest'

import { edadEn, hoyEnZona, lunesDe, sumarDias, yaHaEmpezado } from './fechas'
import { precioDeTexto } from '../paginas/esquemas'
import { duracionesPosibles, formatearDuracion, formatearFechaLarga, formatearPrecio, formatearTramos, personaDeLaCita } from './formato'
import { diasDe, erroresDeSemana, semanaDe } from './horarios'

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

describe('panel del admin (B9c)', () => {
  it('duraciones múltiplo del intervalo hasta 8 horas (RN-011)', () => {
    expect(duracionesPosibles(30).slice(0, 3)).toEqual([30, 60, 90])
    expect(duracionesPosibles(30).at(-1)).toBe(480)
    expect(duracionesPosibles(60)).toHaveLength(8)
  })

  it('lee precios con coma o punto y como mucho dos decimales', () => {
    expect(precioDeTexto('15')).toBe(15)
    expect(precioDeTexto(' 15,5 ')).toBe(15.5)
    expect(precioDeTexto('9999.99')).toBe(9999.99)
    expect(precioDeTexto('15,555')).toBeNull()
    expect(precioDeTexto('10000')).toBeNull()
    expect(precioDeTexto('-3')).toBeNull()
  })

  it('convierte la semana del editor y comprueba sus tramos', () => {
    const semana = semanaDe([{ diaSemana: 1, tramos: [{ inicio: '09:00', fin: '14:00' }] }], null)
    expect(semana[1]).toHaveLength(1)
    expect(semana[2]).toBeNull()
    expect(diasDe(semana)).toEqual([{ diaSemana: 1, tramos: [{ inicio: '09:00', fin: '14:00' }] }])
    expect(diasDe(semanaDe([], []))).toHaveLength(7)

    expect(erroresDeSemana({ 1: [{ inicio: '09:00', fin: '14:00' }] })).toEqual({})
    expect(erroresDeSemana({ 1: [{ inicio: '14:00', fin: '09:00' }] })[1]).toMatch(/anterior/)
    expect(erroresDeSemana({ 2: [{ inicio: '09:02', fin: '14:00' }] })[2]).toMatch(/múltiplos de 5/)
    expect(erroresDeSemana({ 3: [{ inicio: '', fin: '14:00' }] })[3]).toMatch(/Indica/)
  })
})
