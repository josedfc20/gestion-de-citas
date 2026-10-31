import { useEffect, useRef, type ReactNode } from 'react'

import { Boton } from './Boton'
import estilos from './Confirmacion.module.css'

interface Props {
  titulo: string
  children?: ReactNode
  textoConfirmar: string
  textoVolver?: string
  enviando?: boolean
  textoEnviando?: string
  /** Botón de acción destructiva (eliminar, cancelar): en rojo. */
  peligro?: boolean
  onConfirmar: () => void
  onVolver: () => void
}

/**
 * "¿Seguro?" dentro de la página, en lugar de una ventana emergente (decisión 4
 * del B8). Al abrirse se lleva el foco al título para que el lector de pantalla
 * lo anuncie.
 */
export function Confirmacion({ titulo, children, textoConfirmar, textoVolver = 'No, volver', enviando, textoEnviando, peligro, onConfirmar, onVolver }: Props) {
  const tituloRef = useRef<HTMLHeadingElement>(null)
  useEffect(() => {
    tituloRef.current?.focus()
  }, [])

  return (
    <section className={estilos.confirmacion} aria-label={titulo}>
      <h3 ref={tituloRef} tabIndex={-1} className={estilos.titulo}>
        {titulo}
      </h3>
      {children}
      <div className={estilos.botones}>
        <Boton className={peligro ? estilos.peligro : undefined} enviando={enviando} textoEnviando={textoEnviando} onClick={onConfirmar}>
          {textoConfirmar}
        </Boton>
        <Boton variante="secundario" onClick={onVolver} disabled={enviando}>
          {textoVolver}
        </Boton>
      </div>
    </section>
  )
}
