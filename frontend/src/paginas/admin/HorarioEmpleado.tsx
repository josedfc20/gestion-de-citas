import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router'

import { consultaEmpleadoAdmin, consultaHorarioEmpleado, consultaHorarioGeneral, guardarHorarioEmpleado } from '../../api/admin'
import { ApiError, mensajeDeError } from '../../api/errores'
import type { CitaAfectada, EmpleadoAdmin, HorarioEmpleado as Horario, HorarioSemana } from '../../api/tipos'
import { Aviso } from '../../componentes/Aviso'
import { Boton, EnlaceBoton } from '../../componentes/Boton'
import { Cargando } from '../../componentes/Cargando'
import { CitasAfectadas } from '../../componentes/CitasAfectadas'
import { EditorSemana } from '../../componentes/EditorSemana'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { formatearTramos, NOMBRE_DIA } from '../../utilidades/formato'
import { diasDe, erroresDeSemana, semanaDe, type Semana } from '../../utilidades/horarios'
import { useTitulo } from '../../utilidades/useTitulo'
import { numeroDeParametro } from '../parametros'
import admin from './Admin.module.css'

/** CU-018 y RN-002: el horario propio de un empleado, día a día sobre el horario general. */
export function HorarioEmpleado() {
  useTitulo('Horario del empleado')
  const id = numeroDeParametro(useParams().id ?? null)
  const empleado = useQuery({ ...consultaEmpleadoAdmin(id ?? 0), enabled: id !== undefined })
  const horario = useQuery({ ...consultaHorarioEmpleado(id ?? 0), enabled: id !== undefined })
  const general = useQuery(consultaHorarioGeneral)

  if (id === undefined || (empleado.error instanceof ApiError && empleado.error.status === 404)) {
    return (
      <section className={admin.estrecho}>
        <h1>Empleado no encontrado</h1>
        <EnlaceBoton to="/admin/empleados">Ver los empleados</EnlaceBoton>
      </section>
    )
  }
  const fallida = [empleado, horario, general].find((consulta) => consulta.error)
  if (fallida) return <ErrorConsulta error={fallida.error} reintentar={() => fallida.refetch()} />
  if (!empleado.data || !horario.data || !general.data) return <Cargando />
  return <Editor empleado={empleado.data} horario={horario.data} general={general.data} />
}

function Editor({ empleado, horario, general }: { empleado: EmpleadoAdmin; horario: Horario; general: HorarioSemana }) {
  const queryClient = useQueryClient()
  const [semana, setSemana] = useState<Semana>(() => semanaDe(horario.ajustes, null))
  const [errores, setErrores] = useState<Record<number, string>>({})
  const [enviando, setEnviando] = useState(false)
  const [aviso, setAviso] = useState<{ tipo: 'exito' | 'error'; texto: string } | null>(null)
  const [afectadas, setAfectadas] = useState<CitaAfectada[] | undefined>()
  const [efectiva, setEfectiva] = useState(horario.semanaEfectiva)

  async function guardar() {
    setAviso(null)
    setAfectadas(undefined)
    const encontrados = erroresDeSemana(semana)
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return
    setEnviando(true)
    try {
      const guardado = await guardarHorarioEmpleado(empleado.id, diasDe(semana))
      queryClient.setQueryData(consultaHorarioEmpleado(empleado.id).queryKey, { ...guardado, citasAfectadas: undefined })
      await queryClient.invalidateQueries({ queryKey: ['disponibilidad'] })
      setEfectiva(guardado.semanaEfectiva)
      setAfectadas(guardado.citasAfectadas)
      setAviso({ tipo: 'exito', texto: 'Horario guardado.' })
    } catch (error) {
      setAviso({ tipo: 'error', texto: mensajeDeError(error) })
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section>
      <h1>
        Horario de {empleado.nombre} {empleado.apellidos}
      </h1>
      <p className={admin.estrecho}>
        Cada día sigue el horario general o tiene un horario propio, que puede salirse del general. Un día propio sin
        tramos es un día que no trabaja.
      </p>
      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      <CitasAfectadas citas={afectadas} />
      <EditorSemana semana={semana} onCambiar={setSemana} errores={errores} general={semanaDe(general.dias, [])} />
      <Boton enviando={enviando} textoEnviando="Guardando…" onClick={guardar}>
        Guardar horario
      </Boton>

      <section className={admin.seccion} aria-labelledby="titulo-efectiva">
        <h2 id="titulo-efectiva">Lo que trabaja cada semana</h2>
        <ul>
          {efectiva.map((dia) => (
            <li key={dia.diaSemana}>
              {NOMBRE_DIA[dia.diaSemana]}: {dia.tramos.length === 0 ? 'no trabaja' : formatearTramos(dia.tramos)}
              {dia.ajustado && ' (horario propio)'}
            </li>
          ))}
        </ul>
      </section>
      <EnlaceBoton to={`/admin/empleados/${empleado.id}`} variante="enlace">
        Volver a su ficha
      </EnlaceBoton>
    </section>
  )
}
