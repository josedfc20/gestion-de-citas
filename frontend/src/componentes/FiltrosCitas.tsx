import type { SetURLSearchParams } from 'react-router'

import { ESTADOS, filtrosDeParametros } from '../paginas/parametros'
import { Campo, CampoSeleccion } from './Campo'
import estilos from './FiltrosCitas.module.css'

interface Props {
  parametros: URLSearchParams
  setParametros: SetURLSearchParams
}

/** Estado y fechas de una lista de citas, guardados en la URL para poder volver atrás. */
export function FiltrosCitas({ parametros, setParametros }: Props) {
  const filtros = filtrosDeParametros(parametros)

  function cambiar(clave: string, valor: string) {
    const nuevos = new URLSearchParams(parametros)
    if (valor) nuevos.set(clave, valor)
    else nuevos.delete(clave)
    setParametros(nuevos, { replace: true })
  }

  return (
    <div className={estilos.filtros} role="group" aria-label="Filtros">
      <CampoSeleccion etiqueta="Estado" value={filtros.estado ?? ''} onChange={(evento) => cambiar('estado', evento.target.value)}>
        <option value="">Todos</option>
        {ESTADOS.map((opcion) => (
          <option key={opcion.valor} value={opcion.valor}>
            {opcion.texto}
          </option>
        ))}
      </CampoSeleccion>
      <Campo etiqueta="Desde" type="date" ayuda="Vacío: desde hoy." value={filtros.desde ?? ''} onChange={(evento) => cambiar('desde', evento.target.value)} />
      <Campo etiqueta="Hasta" type="date" value={filtros.hasta ?? ''} onChange={(evento) => cambiar('hasta', evento.target.value)} />
    </div>
  )
}
