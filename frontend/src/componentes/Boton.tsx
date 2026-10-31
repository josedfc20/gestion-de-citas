import type { ButtonHTMLAttributes } from 'react'
import { Link, type LinkProps } from 'react-router'

import estilos from './Boton.module.css'

type Variante = 'primario' | 'secundario' | 'enlace'

function clases(variante: Variante, ancho?: boolean, extra?: string): string {
  return [estilos.boton, estilos[variante], ancho ? estilos.ancho : '', extra ?? ''].filter(Boolean).join(' ')
}

interface PropsBoton extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante
  ancho?: boolean
  /** Muestra el texto de espera y desactiva el botón mientras se envía. */
  enviando?: boolean
  textoEnviando?: string
}

export function Boton({ variante = 'primario', ancho, enviando, textoEnviando, className, children, disabled, type = 'button', ...resto }: PropsBoton) {
  return (
    <button type={type} className={clases(variante, ancho, className)} disabled={disabled || enviando} aria-busy={enviando || undefined} {...resto}>
      {enviando ? (textoEnviando ?? 'Enviando…') : children}
    </button>
  )
}

interface PropsEnlace extends LinkProps {
  variante?: Variante
  ancho?: boolean
}

/** Enlace con aspecto de botón (para navegar, no para acciones). */
export function EnlaceBoton({ variante = 'primario', ancho, className, ...resto }: PropsEnlace) {
  return <Link className={clases(variante, ancho, className)} {...resto} />
}
