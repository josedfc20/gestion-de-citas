// Panel del administrador (CU-016 a CU-018 y configuración del negocio, B9c).

import { queryOptions } from '@tanstack/react-query'

import { pedir } from './cliente'
import type {
  DatosAltaEmpleado,
  DatosDiaCierre,
  DatosEmpleado,
  DatosServicio,
  DiaCierre,
  DiaHorario,
  EmpleadoAdmin,
  EmpleadoCreado,
  HorarioEmpleado,
  HorarioSemana,
  Negocio,
  ServicioAdmin,
} from './tipos'

// ---- Servicios (CU-016) ----

export const consultaServiciosAdmin = queryOptions({
  queryKey: ['admin', 'servicios'],
  queryFn: ({ signal }) => pedir<ServicioAdmin[]>('/admin/servicios', { signal }),
})

export function consultaServicioAdmin(id: number) {
  return queryOptions({
    queryKey: ['admin', 'servicios', id],
    queryFn: ({ signal }) => pedir<ServicioAdmin>(`/admin/servicios/${id}`, { signal }),
  })
}

export function crearServicio(datos: DatosServicio): Promise<ServicioAdmin> {
  return pedir<ServicioAdmin>('/admin/servicios', { metodo: 'POST', cuerpo: datos })
}

export function actualizarServicio(id: number, datos: DatosServicio): Promise<ServicioAdmin> {
  return pedir<ServicioAdmin>(`/admin/servicios/${id}`, { metodo: 'PUT', cuerpo: datos })
}

export function cambiarActivoServicio(id: number, activo: boolean): Promise<ServicioAdmin> {
  return pedir<ServicioAdmin>(`/admin/servicios/${id}/activo`, { metodo: 'PATCH', cuerpo: { activo } })
}

// ---- Empleados (CU-017) ----

export const consultaEmpleadosAdmin = queryOptions({
  queryKey: ['admin', 'empleados'],
  queryFn: ({ signal }) => pedir<EmpleadoAdmin[]>('/admin/empleados', { signal }),
})

export function consultaEmpleadoAdmin(id: number) {
  return queryOptions({
    queryKey: ['admin', 'empleados', id],
    queryFn: ({ signal }) => pedir<EmpleadoAdmin>(`/admin/empleados/${id}`, { signal }),
  })
}

export function altaEmpleado(datos: DatosAltaEmpleado): Promise<EmpleadoCreado> {
  return pedir<EmpleadoCreado>('/admin/empleados', { metodo: 'POST', cuerpo: datos })
}

export function actualizarEmpleado(id: number, datos: DatosEmpleado): Promise<EmpleadoAdmin> {
  return pedir<EmpleadoAdmin>(`/admin/empleados/${id}`, { metodo: 'PUT', cuerpo: datos })
}

/** Reemplaza la lista completa de servicios del empleado; puede traer `avisos`. */
export function asignarServicios(id: number, servicioIds: number[]): Promise<EmpleadoAdmin> {
  return pedir<EmpleadoAdmin>(`/admin/empleados/${id}/servicios`, { metodo: 'PUT', cuerpo: { servicioIds } })
}

/** 409 con `citasFuturas` si tiene citas futuras confirmadas. Al desactivarlo se cierran sus sesiones. */
export function cambiarActivoEmpleado(id: number, activo: boolean): Promise<EmpleadoAdmin> {
  return pedir<EmpleadoAdmin>(`/admin/empleados/${id}/activo`, { metodo: 'PATCH', cuerpo: { activo } })
}

export function nuevaPasswordTemporal(id: number): Promise<{ passwordTemporal: string }> {
  return pedir<{ passwordTemporal: string }>(`/admin/empleados/${id}/password-temporal`, { metodo: 'POST' })
}

// ---- Horarios y días de cierre (CU-018) ----

export const consultaHorarioGeneral = queryOptions({
  queryKey: ['admin', 'horario-general'],
  queryFn: ({ signal }) => pedir<HorarioSemana>('/admin/horario-general', { signal }),
})

/** Un día que no se envía queda cerrado. */
export function guardarHorarioGeneral(dias: DiaHorario[]): Promise<HorarioSemana> {
  return pedir<HorarioSemana>('/admin/horario-general', { metodo: 'PUT', cuerpo: { dias } })
}

export function consultaHorarioEmpleado(id: number) {
  return queryOptions({
    queryKey: ['admin', 'horario-empleado', id],
    queryFn: ({ signal }) => pedir<HorarioEmpleado>(`/admin/empleados/${id}/horario`, { signal }),
  })
}

/** Solo los días ajustados; los que no se envían siguen el horario general. */
export function guardarHorarioEmpleado(id: number, dias: DiaHorario[]): Promise<HorarioEmpleado> {
  return pedir<HorarioEmpleado>(`/admin/empleados/${id}/horario`, { metodo: 'PUT', cuerpo: { dias } })
}

/** Sin `desde`, de hoy en adelante. */
export function consultaDiasCierre(desde?: string) {
  return queryOptions({
    queryKey: ['admin', 'dias-cierre', desde ?? 'proximos'],
    queryFn: ({ signal }) => pedir<DiaCierre[]>('/admin/dias-cierre', { parametros: { desde }, signal }),
  })
}

export function crearDiaCierre(datos: DatosDiaCierre): Promise<DiaCierre> {
  return pedir<DiaCierre>('/admin/dias-cierre', { metodo: 'POST', cuerpo: datos })
}

export function borrarDiaCierre(id: number): Promise<void> {
  return pedir<void>(`/admin/dias-cierre/${id}`, { metodo: 'DELETE' })
}

// ---- Negocio ----

export function guardarNegocio(datos: Negocio): Promise<Negocio> {
  return pedir<Negocio>('/admin/negocio', { metodo: 'PUT', cuerpo: datos })
}
