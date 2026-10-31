import { describe, expect, it } from 'vitest'

import { inicioConDesfase, partesDeInstante } from './fechas'
import { formatearInstante } from './formato'

describe('hora de la reserva con el desfase del negocio (B8, decisión 1)', () => {
  it('en invierno Madrid va a +01:00', () => {
    expect(inicioConDesfase('2026-11-03', '10:00', 'Europe/Madrid')).toBe('2026-11-03T10:00:00+01:00')
  })

  it('en verano Madrid va a +02:00', () => {
    expect(inicioConDesfase('2026-07-15', '18:30', 'Europe/Madrid')).toBe('2026-07-15T18:30:00+02:00')
  })

  it('el día del cambio de hora usa el desfase de esa hora, no el de hoy', () => {
    // 25/10/2026: a las 03:00 vuelven a ser las 02:00.
    expect(inicioConDesfase('2026-10-25', '01:30', 'Europe/Madrid')).toBe('2026-10-25T01:30:00+02:00')
    expect(inicioConDesfase('2026-10-25', '10:00', 'Europe/Madrid')).toBe('2026-10-25T10:00:00+01:00')
    // 29/03/2026: a las 02:00 pasan a ser las 03:00.
    expect(inicioConDesfase('2026-03-29', '09:00', 'Europe/Madrid')).toBe('2026-03-29T09:00:00+02:00')
  })

  it('funciona con otras zonas (versatilidad) y con UTC', () => {
    expect(inicioConDesfase('2026-11-03', '10:00', 'America/New_York')).toBe('2026-11-03T10:00:00-05:00')
    expect(inicioConDesfase('2026-11-03', '10:00', 'Atlantic/Canary')).toBe('2026-11-03T10:00:00+00:00')
    expect(inicioConDesfase('2026-11-03', '10:00', 'UTC')).toBe('2026-11-03T10:00:00+00:00')
  })

  it('lee la fecha y la hora de una cita sin pasarlas a la zona del navegador', () => {
    expect(partesDeInstante('2026-11-03T10:00:00+01:00')).toEqual({ fecha: '2026-11-03', hora: '10:00' })
    expect(formatearInstante('2026-11-03T10:00:00+01:00')).toBe('martes, 3 de noviembre de 2026 a las 10:00')
    expect(formatearInstante('2026-11-03T10:00:00+01:00', false)).toBe('martes, 3 de noviembre a las 10:00')
  })
})
