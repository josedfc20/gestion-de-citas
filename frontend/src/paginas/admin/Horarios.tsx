import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { borrarDiaCierre, consultaDiasCierre, consultaEmpleadosAdmin, consultaHorarioGeneral, crearDiaCierre, guardarHorarioGeneral } from '../../api/admin'
import { consultaNegocio } from '../../api/catalogo'
import { ApiError, mensajeDeError } from '../../api/errores'
import type { CitaAfectada, DiaCierre, HorarioSemana } from '../../api/tipos'
import { Aviso } from '../../componentes/Aviso'
import { Boton } from '../../componentes/Boton'
import { Campo, CampoSeleccion, Casilla } from '../../componentes/Campo'
import { Cargando } from '../../componentes/Cargando'
import { CitasAfectadas } from '../../componentes/CitasAfectadas'
import { Confirmacion } from '../../componentes/Confirmacion'
import { EditorSemana } from '../../componentes/EditorSemana'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { hoyEnZona, sumarDias } from '../../utilidades/fechas'
import { formatearFechaLarga } from '../../utilidades/formato'
import { aplicarErroresDelServidor } from '../../utilidades/formularios'
import { diasDe, erroresDeSemana, semanaDe, type Semana } from '../../utilidades/horarios'
import { useTitulo } from '../../utilidades/useTitulo'
import { esquemaDiaCierre, type DatosFormularioDiaCierre } from '../esquemas'
import estilos from '../Paginas.module.css'
import admin from './Admin.module.css'

/** CU-018: horario general del negocio y días de cierre. */
export function HorariosAdmin() {
  useTitulo('Horarios')
  const general = useQuery(consultaHorarioGeneral)

  return (
    <>
      <header className={estilos.cabecera}>
        <h1>Horarios</h1>
        <p className={estilos.subtitulo}>
          El horario general vale para todos los empleados salvo los días en que alguno tenga un horario propio (en su ficha).
        </p>
      </header>
      {general.isPending ? (
        <Cargando />
      ) : general.error ? (
        <ErrorConsulta error={general.error} reintentar={() => general.refetch()} />
      ) : (
        <HorarioGeneral horario={general.data} />
      )}
      <DiasCierre />
    </>
  )
}

function HorarioGeneral({ horario }: { horario: HorarioSemana }) {
  const queryClient = useQueryClient()
  const [semana, setSemana] = useState<Semana>(() => semanaDe(horario.dias, []))
  const [errores, setErrores] = useState<Record<number, string>>({})
  const [enviando, setEnviando] = useState(false)
  const [aviso, setAviso] = useState<{ tipo: 'exito' | 'error'; texto: string } | null>(null)
  const [afectadas, setAfectadas] = useState<CitaAfectada[] | undefined>()

  async function guardar() {
    setAviso(null)
    setAfectadas(undefined)
    const encontrados = erroresDeSemana(semana)
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return
    setEnviando(true)
    try {
      const guardado = await guardarHorarioGeneral(diasDe(semana))
      queryClient.setQueryData(consultaHorarioGeneral.queryKey, { dias: guardado.dias })
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin', 'horario-empleado'] }),
        queryClient.invalidateQueries({ queryKey: ['disponibilidad'] }),
      ])
      setAfectadas(guardado.citasAfectadas)
      setAviso({ tipo: 'exito', texto: 'Horario general guardado.' })
    } catch (error) {
      setAviso({ tipo: 'error', texto: mensajeDeError(error) })
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className={admin.seccion} aria-labelledby="titulo-general">
      <h2 id="titulo-general">Horario general</h2>
      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      <CitasAfectadas citas={afectadas} />
      <EditorSemana semana={semana} onCambiar={setSemana} errores={errores} />
      <Boton enviando={enviando} textoEnviando="Guardando…" onClick={guardar}>
        Guardar horario general
      </Boton>
    </section>
  )
}

function DiasCierre() {
  const queryClient = useQueryClient()
  const negocio = useQuery(consultaNegocio)
  const empleados = useQuery(consultaEmpleadosAdmin)
  const [verPasados, setVerPasados] = useState(false)
  const desde = verPasados && negocio.data ? sumarDias(hoyEnZona(negocio.data.zonaHoraria), -90) : undefined
  const cierres = useQuery(consultaDiasCierre(desde))
  const [aviso, setAviso] = useState<{ tipo: 'exito' | 'error'; texto: string } | null>(null)
  const [afectadas, setAfectadas] = useState<CitaAfectada[] | undefined>()
  const [borrando, setBorrando] = useState<DiaCierre | null>(null)
  const [enviando, setEnviando] = useState(false)
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormularioDiaCierre>({
    resolver: zodResolver(esquemaDiaCierre),
    defaultValues: { fecha: '', empleadoId: '', motivo: '' },
  })

  const invalidar = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['admin', 'dias-cierre'] }),
      queryClient.invalidateQueries({ queryKey: ['disponibilidad'] }),
      queryClient.invalidateQueries({ queryKey: ['agenda'] }),
    ])

  async function crear(datos: DatosFormularioDiaCierre) {
    setAviso(null)
    setAfectadas(undefined)
    try {
      const creado = await crearDiaCierre({
        fecha: datos.fecha,
        empleadoId: datos.empleadoId ? Number(datos.empleadoId) : null,
        motivo: datos.motivo || null,
      })
      await invalidar()
      reset()
      setAfectadas(creado.citasAfectadas)
      setAviso({ tipo: 'exito', texto: `Día de cierre añadido: ${formatearFechaLarga(creado.fecha)}.` })
    } catch (error) {
      if (error instanceof ApiError && (error.status === 409 || error.status === 422)) {
        setError('fecha', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else {
        const texto = aplicarErroresDelServidor(error, setError, ['fecha', 'empleadoId', 'motivo'])
        if (texto) setAviso({ tipo: 'error', texto })
      }
    }
  }

  async function borrar() {
    if (!borrando) return
    setAviso(null)
    setEnviando(true)
    try {
      await borrarDiaCierre(borrando.id)
      await invalidar()
      setAviso({ tipo: 'exito', texto: 'Día de cierre quitado.' })
      setBorrando(null)
    } catch (error) {
      setAviso({ tipo: 'error', texto: mensajeDeError(error) })
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className={admin.seccion} aria-labelledby="titulo-cierres">
      <h2 id="titulo-cierres">Días de cierre</h2>
      <p className={estilos.suave}>Días completos en que no se atiende: de todo el negocio (festivos) o de un empleado (vacaciones).</p>
      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      <CitasAfectadas citas={afectadas} />

      <Casilla etiqueta="Ver también los de los últimos 90 días" checked={verPasados} onChange={(evento) => setVerPasados(evento.target.checked)} />
      {cierres.isPending ? (
        <Cargando />
      ) : cierres.error ? (
        <ErrorConsulta error={cierres.error} reintentar={() => cierres.refetch()} />
      ) : cierres.data.length === 0 ? (
        <p>No hay días de cierre próximos.</p>
      ) : (
        <ul className={admin.cierres} aria-label="Días de cierre">
          {cierres.data.map((cierre) => (
            <li key={cierre.id} className={admin.cierre}>
              <span>
                {formatearFechaLarga(cierre.fecha)} · {cierre.empleado ?? 'Todo el negocio'}
                {cierre.motivo && ` · ${cierre.motivo}`}
              </span>
              <Boton variante="enlace" onClick={() => setBorrando(cierre)} aria-label={`Quitar el cierre del ${formatearFechaLarga(cierre.fecha)}`}>
                Quitar
              </Boton>
            </li>
          ))}
        </ul>
      )}
      {borrando && (
        <Confirmacion
          titulo={`¿Quitar el cierre del ${formatearFechaLarga(borrando.fecha)}?`}
          textoConfirmar="Sí, quitar"
          textoEnviando="Quitando…"
          enviando={enviando}
          onConfirmar={borrar}
          onVolver={() => setBorrando(null)}
        >
          <p>Ese día volverá a tener horas libres para reservar.</p>
        </Confirmacion>
      )}

      <form className={admin.bloque} onSubmit={handleSubmit(crear)} noValidate>
        <h3>Añadir un día de cierre</h3>
        <div className={estilos.dosColumnas}>
          <Campo etiqueta="Día" type="date" error={errors.fecha?.message} {...register('fecha')} />
          <CampoSeleccion etiqueta="Quién cierra" error={errors.empleadoId?.message} {...register('empleadoId')}>
            <option value="">Todo el negocio</option>
            {empleados.data
              ?.filter((empleado) => empleado.activo)
              .map((empleado) => (
                <option key={empleado.id} value={empleado.id}>
                  {empleado.nombre} {empleado.apellidos}
                </option>
              ))}
          </CampoSeleccion>
        </div>
        <Campo etiqueta="Motivo" opcional maxLength={200} error={errors.motivo?.message} {...register('motivo')} />
        <p className={estilos.suave}>Las citas ya reservadas ese día no se cancelan: si hay alguna, te la indicaremos para que la revises.</p>
        <Boton type="submit" enviando={isSubmitting} textoEnviando="Guardando…">
          Añadir día de cierre
        </Boton>
      </form>
    </section>
  )
}
