import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { guardarNegocio } from '../../api/admin'
import { consultaNegocio } from '../../api/catalogo'
import { ApiError } from '../../api/errores'
import type { Negocio } from '../../api/tipos'
import { Aviso } from '../../componentes/Aviso'
import { Boton } from '../../componentes/Boton'
import { Campo, CampoSeleccion } from '../../componentes/Campo'
import { Cargando } from '../../componentes/Cargando'
import { Confirmacion } from '../../componentes/Confirmacion'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { aplicarErroresDelServidor } from '../../utilidades/formularios'
import { useTitulo } from '../../utilidades/useTitulo'
import { esquemaNegocio, INTERVALOS, type DatosFormularioNegocio } from '../esquemas'
import estilos from '../Paginas.module.css'
import admin from './Admin.module.css'

const CAMPOS = ['nombre', 'zonaHoraria', 'intervaloMinutos', 'antelacionMaxDias', 'horasMinCancelacion', 'edadMinimaCuenta'] as const

/** Zonas horarias que conoce el navegador, con la actual siempre en la lista. */
function zonasHorarias(actual: string): string[] {
  const zonas = typeof Intl.supportedValuesOf === 'function' ? Intl.supportedValuesOf('timeZone') : []
  return zonas.includes(actual) ? zonas : [actual, ...zonas]
}

/** Configuración del negocio (RN-004, RN-006, RN-009, RN-010). */
export function NegocioAdmin() {
  useTitulo('Negocio')
  const negocio = useQuery(consultaNegocio)

  if (negocio.isPending) return <Cargando />
  if (negocio.error) return <ErrorConsulta error={negocio.error} reintentar={() => negocio.refetch()} />
  return <Formulario negocio={negocio.data} />
}

function Formulario({ negocio }: { negocio: Negocio }) {
  const queryClient = useQueryClient()
  const [aviso, setAviso] = useState<{ tipo: 'exito' | 'error'; texto: string } | null>(null)
  const [pendiente, setPendiente] = useState<DatosFormularioNegocio | null>(null)
  const [enviando, setEnviando] = useState(false)
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<DatosFormularioNegocio>({
    resolver: zodResolver(esquemaNegocio),
    defaultValues: aFormulario(negocio),
  })

  function revisar(datos: DatosFormularioNegocio) {
    setAviso(null)
    // Cambiar el intervalo o la zona cambia las horas que se ofrecen: se confirma antes.
    const delicado = Number(datos.intervaloMinutos) !== negocio.intervaloMinutos || datos.zonaHoraria !== negocio.zonaHoraria
    if (delicado) setPendiente(datos)
    else void guardar(datos)
  }

  async function guardar(datos: DatosFormularioNegocio) {
    setEnviando(true)
    try {
      const guardado = await guardarNegocio({
        nombre: datos.nombre.trim(),
        zonaHoraria: datos.zonaHoraria,
        intervaloMinutos: Number(datos.intervaloMinutos),
        antelacionMaxDias: Number(datos.antelacionMaxDias),
        horasMinCancelacion: Number(datos.horasMinCancelacion),
        edadMinimaCuenta: Number(datos.edadMinimaCuenta),
      })
      // La cabecera, los horarios y las reglas de toda la web usan GET /negocio.
      queryClient.setQueryData(consultaNegocio.queryKey, guardado)
      await queryClient.invalidateQueries({ queryKey: ['disponibilidad'] })
      reset(aFormulario(guardado))
      setAviso({ tipo: 'exito', texto: 'Configuración guardada.' })
    } catch (error) {
      if (error instanceof ApiError && error.status === 422) {
        // Algún servicio dejaría de ser múltiplo del intervalo (RN-011).
        setError('intervaloMinutos', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else {
        const texto = aplicarErroresDelServidor(error, setError, CAMPOS)
        if (texto) setAviso({ tipo: 'error', texto })
      }
    } finally {
      setEnviando(false)
      setPendiente(null)
    }
  }

  return (
    <section className={admin.estrecho}>
      <h1>Configuración del negocio</h1>
      <form className={admin.bloque} onSubmit={handleSubmit(revisar)} noValidate>
        {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
        <Campo etiqueta="Nombre del negocio" ayuda="Sale en la cabecera y en el pie de la web." error={errors.nombre?.message} {...register('nombre')} />
        <div className={estilos.dosColumnas}>
          <CampoSeleccion
            etiqueta="Zona horaria"
            ayuda="Todas las horas de la web y de las citas son de esta zona."
            error={errors.zonaHoraria?.message}
            {...register('zonaHoraria')}
          >
            {zonasHorarias(negocio.zonaHoraria).map((zona) => (
              <option key={zona} value={zona}>
                {zona}
              </option>
            ))}
          </CampoSeleccion>
          <CampoSeleccion
            etiqueta="Intervalo de reserva"
            ayuda="Cada cuánto empiezan las citas (RN-010). La duración de los servicios debe ser múltiplo."
            error={errors.intervaloMinutos?.message}
            {...register('intervaloMinutos')}
          >
            {INTERVALOS.map((minutos) => (
              <option key={minutos} value={minutos}>
                {minutos} minutos
              </option>
            ))}
          </CampoSeleccion>
        </div>
        <div className={estilos.dosColumnas}>
          <Campo
            etiqueta="Antelación máxima (días)"
            inputMode="numeric"
            ayuda="Hasta cuántos días por delante se puede reservar (RN-009)."
            error={errors.antelacionMaxDias?.message}
            {...register('antelacionMaxDias')}
          />
          <Campo
            etiqueta="Plazo para cancelar (horas)"
            inputMode="numeric"
            ayuda="El cliente cancela online hasta estas horas antes (RN-004)."
            error={errors.horasMinCancelacion?.message}
            {...register('horasMinCancelacion')}
          />
        </div>
        <Campo
          etiqueta="Edad mínima para tener cuenta"
          inputMode="numeric"
          ayuda="Por debajo, la cita la reserva un adulto (RN-006; la ley española fija 14 años)."
          error={errors.edadMinimaCuenta?.message}
          {...register('edadMinimaCuenta')}
        />
        {pendiente ? (
          <Confirmacion
            titulo="¿Guardar el cambio de intervalo o de zona horaria?"
            textoConfirmar="Sí, guardar"
            textoEnviando="Guardando…"
            enviando={enviando}
            onConfirmar={() => guardar(pendiente)}
            onVolver={() => setPendiente(null)}
          >
            <p>Cambia las horas que se ofrecen a los clientes. Las citas ya reservadas se mantienen.</p>
          </Confirmacion>
        ) : (
          <Boton type="submit" enviando={enviando} textoEnviando="Guardando…">
            Guardar configuración
          </Boton>
        )}
      </form>
    </section>
  )
}

function aFormulario(negocio: Negocio): DatosFormularioNegocio {
  return {
    nombre: negocio.nombre,
    zonaHoraria: negocio.zonaHoraria,
    intervaloMinutos: String(negocio.intervaloMinutos),
    antelacionMaxDias: String(negocio.antelacionMaxDias),
    horasMinCancelacion: String(negocio.horasMinCancelacion),
    edadMinimaCuenta: String(negocio.edadMinimaCuenta),
  }
}
