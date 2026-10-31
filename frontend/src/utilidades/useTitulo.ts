import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'

import { consultaNegocio } from '../api/catalogo'

/** Título de la pestaña: "Servicios · Peluquería Ana". Ayuda también a los lectores de pantalla. */
export function useTitulo(titulo: string): void {
  const { data: negocio } = useQuery(consultaNegocio)
  useEffect(() => {
    document.title = negocio ? `${titulo} · ${negocio.nombre}` : titulo
  }, [titulo, negocio])
}
