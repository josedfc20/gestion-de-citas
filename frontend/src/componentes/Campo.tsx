import { useId, type InputHTMLAttributes, type ReactNode, type Ref, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'

import estilos from './Campo.module.css'

interface PropsComunes {
  etiqueta: string
  error?: string
  ayuda?: ReactNode
  opcional?: boolean
}

/** Ids de ayuda y error para enlazarlos con aria-describedby. */
function useDescripcion(error?: string, ayuda?: ReactNode) {
  const base = useId()
  const idAyuda = ayuda ? `${base}-ayuda` : undefined
  const idError = error ? `${base}-error` : undefined
  const describedBy = [idError, idAyuda].filter(Boolean).join(' ') || undefined
  return { id: base, idAyuda, idError, describedBy }
}

function Pie({ error, ayuda, idError, idAyuda }: { error?: string; ayuda?: ReactNode; idError?: string; idAyuda?: string }) {
  return (
    <>
      {error && (
        <p id={idError} className={estilos.error}>
          {error}
        </p>
      )}
      {ayuda && (
        <p id={idAyuda} className={estilos.ayuda}>
          {ayuda}
        </p>
      )}
    </>
  )
}

type PropsCampo = PropsComunes & InputHTMLAttributes<HTMLInputElement> & { ref?: Ref<HTMLInputElement> }

/** Campo de texto con su etiqueta, ayuda y error accesibles. */
export function Campo({ etiqueta, error, ayuda, opcional, ref, ...resto }: PropsCampo) {
  const { id, idAyuda, idError, describedBy } = useDescripcion(error, ayuda)
  return (
    <div className={estilos.campo}>
      <label htmlFor={id} className={estilos.etiqueta}>
        {etiqueta} {opcional && <span className={estilos.opcional}>(opcional)</span>}
      </label>
      <input id={id} ref={ref} className={estilos.control} aria-invalid={error ? true : undefined} aria-describedby={describedBy} {...resto} />
      <Pie error={error} ayuda={ayuda} idError={idError} idAyuda={idAyuda} />
    </div>
  )
}

type PropsSeleccion = PropsComunes & SelectHTMLAttributes<HTMLSelectElement> & { ref?: Ref<HTMLSelectElement> }

export function CampoSeleccion({ etiqueta, error, ayuda, opcional, ref, children, ...resto }: PropsSeleccion) {
  const { id, idAyuda, idError, describedBy } = useDescripcion(error, ayuda)
  return (
    <div className={estilos.campo}>
      <label htmlFor={id} className={estilos.etiqueta}>
        {etiqueta} {opcional && <span className={estilos.opcional}>(opcional)</span>}
      </label>
      <select id={id} ref={ref} className={estilos.control} aria-invalid={error ? true : undefined} aria-describedby={describedBy} {...resto}>
        {children}
      </select>
      <Pie error={error} ayuda={ayuda} idError={idError} idAyuda={idAyuda} />
    </div>
  )
}

type PropsCasilla = Omit<PropsComunes, 'etiqueta' | 'opcional'> &
  InputHTMLAttributes<HTMLInputElement> & { ref?: Ref<HTMLInputElement>; etiqueta: ReactNode }

export function Casilla({ etiqueta, error, ayuda, ref, ...resto }: PropsCasilla) {
  const { id, idAyuda, idError, describedBy } = useDescripcion(error, ayuda)
  return (
    <div className={estilos.campo}>
      <div className={estilos.casilla}>
        <input id={id} ref={ref} type="checkbox" aria-invalid={error ? true : undefined} aria-describedby={describedBy} {...resto} />
        <label htmlFor={id}>{etiqueta}</label>
      </div>
      <Pie error={error} ayuda={ayuda} idError={idError} idAyuda={idAyuda} />
    </div>
  )
}

type PropsArea = PropsComunes & TextareaHTMLAttributes<HTMLTextAreaElement> & { ref?: Ref<HTMLTextAreaElement> }

/** Texto largo (descripción de un servicio). */
export function CampoArea({ etiqueta, error, ayuda, opcional, ref, ...resto }: PropsArea) {
  const { id, idAyuda, idError, describedBy } = useDescripcion(error, ayuda)
  return (
    <div className={estilos.campo}>
      <label htmlFor={id} className={estilos.etiqueta}>
        {etiqueta} {opcional && <span className={estilos.opcional}>(opcional)</span>}
      </label>
      <textarea id={id} ref={ref} rows={3} className={estilos.control} aria-invalid={error ? true : undefined} aria-describedby={describedBy} {...resto} />
      <Pie error={error} ayuda={ayuda} idError={idError} idAyuda={idAyuda} />
    </div>
  )
}
