import type { ReactNode } from 'react'

import estilos from './Aviso.module.css'

interface Props {
  tipo?: 'info' | 'error' | 'exito'
  children: ReactNode
}

/** Mensaje destacado. Los errores se anuncian al momento a los lectores de pantalla. */
export function Aviso({ tipo = 'info', children }: Props) {
  return (
    <div className={`${estilos.aviso} ${estilos[tipo]}`} role={tipo === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
