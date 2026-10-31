import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'

import { consultaDisponibilidad, consultaNegocio, consultaProfesionales, consultaServicios } from '../api/catalogo'
import type { Disponibilidad, Negocio } from '../api/tipos'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo, CampoSeleccion } from '../componentes/Campo'
import { Cargando } from '../componentes/Cargando'
import { ErrorConsulta } from '../componentes/ErrorConsulta'
import { hoyEnZona, sumarDias } from '../utilidades/fechas'
import { formatearDuracion, formatearFechaLarga } from '../utilidades/formato'
import { useTitulo } from '../utilidades/useTitulo'
import propias from './Horarios.module.css'
import estilos from './Paginas.module.css'
import { fechaDeParametro, numeroDeParametro } from './parametros'

/**
 * CU-007: servicio → profesional → fecha → horas libres. Los pasos van en la URL
 * (?servicio=&profesional=&fecha=) para poder compartirla y volver atrás.
 */
export function Horarios() {
  useTitulo('Horarios libres')
  const negocio = useQuery(consultaNegocio)

  if (negocio.isPending) return <Cargando />
  if (negocio.error) return <ErrorConsulta error={negocio.error} reintentar={() => negocio.refetch()} />
  return <ConsultaHorarios negocio={negocio.data} />
}

function ConsultaHorarios({ negocio }: { negocio: Negocio }) {
  const [parametros, setParametros] = useSearchParams()
  const hoy = hoyEnZona(negocio.zonaHoraria)
  const ultimoDia = sumarDias(hoy, negocio.antelacionMaxDias)

  const servicioId = numeroDeParametro(parametros.get('servicio'))
  const fecha = fechaDeParametro(parametros.get('fecha')) ?? hoy

  const servicios = useQuery(consultaServicios)
  const profesionales = useQuery({ ...consultaProfesionales(servicioId), enabled: servicioId !== undefined })

  // Si el profesional de la URL no realiza este servicio, se pide elegir otro.
  const idEnUrl = numeroDeParametro(parametros.get('profesional'))
  const profesionalId = profesionales.data?.some((p) => p.id === idEnUrl) ? idEnUrl : undefined

  const servicio = servicios.data?.find((s) => s.id === servicioId)
  const listo = servicioId !== undefined && profesionalId !== undefined
  const disponibilidad = useQuery({
    ...consultaDisponibilidad(servicioId ?? 0, profesionalId ?? 0, fecha),
    enabled: listo,
  })

  function cambiar(cambios: Record<string, string | undefined>) {
    const nuevos = new URLSearchParams(parametros)
    for (const [clave, valor] of Object.entries(cambios)) {
      if (valor) nuevos.set(clave, valor)
      else nuevos.delete(clave)
    }
    setParametros(nuevos, { replace: true })
  }

  return (
    <>
      <header className={estilos.cabecera}>
        <h1>Horarios libres</h1>
        <p className={estilos.subtitulo}>Elige un servicio, un profesional y un día para ver las horas disponibles.</p>
      </header>

      <form className={propias.panel} onSubmit={(evento) => evento.preventDefault()} aria-label="Buscar horas libres">
        <CampoSeleccion
          etiqueta="Servicio"
          value={servicioId ?? ''}
          disabled={!servicios.data}
          onChange={(evento) => cambiar({ servicio: evento.target.value, profesional: undefined })}
        >
          <option value="">Elige un servicio</option>
          {servicios.data?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.nombre} · {formatearDuracion(s.duracionMinutos)}
            </option>
          ))}
        </CampoSeleccion>

        <CampoSeleccion
          etiqueta="Profesional"
          value={profesionalId ?? ''}
          disabled={!servicioId || !profesionales.data}
          onChange={(evento) => cambiar({ profesional: evento.target.value })}
          ayuda={servicioId && profesionales.data?.length === 0 ? 'Ahora mismo nadie realiza este servicio.' : undefined}
        >
          <option value="">{servicioId ? 'Elige un profesional' : 'Primero elige un servicio'}</option>
          {profesionales.data?.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nombre}
            </option>
          ))}
        </CampoSeleccion>

        <Campo
          etiqueta="Día"
          type="date"
          value={fecha}
          min={hoy}
          max={ultimoDia}
          required
          onChange={(evento) => cambiar({ fecha: evento.target.value || undefined })}
        />
      </form>

      {servicios.error && <ErrorConsulta error={servicios.error} reintentar={() => servicios.refetch()} />}
      {profesionales.error && <ErrorConsulta error={profesionales.error} reintentar={() => profesionales.refetch()} />}

      {listo && (
        <section className={propias.resultado} aria-live="polite">
          {disponibilidad.isPending ? (
            <Cargando texto="Buscando horas libres…" />
          ) : disponibilidad.error ? (
            <ErrorConsulta error={disponibilidad.error} />
          ) : (
            <Resultado
              datos={disponibilidad.data}
              nombreServicio={servicio?.nombre}
              antelacionMaxDias={negocio.antelacionMaxDias}
              irA={(otraFecha) => cambiar({ fecha: otraFecha })}
            />
          )}
        </section>
      )}
    </>
  )
}

interface PropsResultado {
  datos: Disponibilidad
  nombreServicio?: string
  antelacionMaxDias: number
  irA: (fecha: string) => void
}

function Resultado({ datos, nombreServicio, antelacionMaxDias, irA }: PropsResultado) {
  const dia = formatearFechaLarga(datos.fecha)

  if (datos.horas.length === 0) {
    return (
      <Aviso>
        <p>
          <strong>No hay horas libres el {dia}.</strong>
        </p>
        {datos.siguienteFechaConHuecos ? (
          <Boton variante="secundario" onClick={() => irA(datos.siguienteFechaConHuecos!)}>
            Ver el siguiente día con horas: {formatearFechaLarga(datos.siguienteFechaConHuecos)}
          </Boton>
        ) : (
          <p>No quedan horas libres en los próximos {antelacionMaxDias} días con este profesional. Prueba con otro.</p>
        )}
      </Aviso>
    )
  }

  return (
    <>
      <h2>
        {datos.horas.length === 1 ? '1 hora libre' : `${datos.horas.length} horas libres`} el {dia}
        {nombreServicio && <span className={estilos.suave}> · {nombreServicio}</span>}
      </h2>
      <ul className={propias.horas}>
        {datos.horas.map((hora) => (
          <li key={hora} className={propias.hora}>
            {hora}
          </li>
        ))}
      </ul>
      <p className={propias.nota}>Horas locales del negocio ({datos.zonaHoraria}).</p>
    </>
  )
}
