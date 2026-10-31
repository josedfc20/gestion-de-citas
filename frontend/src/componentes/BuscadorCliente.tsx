import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'

import { consultaClientes } from '../api/gestionCitas'
import type { ClienteEncontrado } from '../api/tipos'
import { Boton } from './Boton'
import estilos from './BuscadorCliente.module.css'
import { Campo } from './Campo'
import { Cargando } from './Cargando'
import { ErrorConsulta } from './ErrorConsulta'

const MINIMO = 3

interface Props {
  onElegir: (cliente: ClienteEncontrado) => void
  etiqueta?: string
}

/**
 * Buscar un cliente registrado (CU-019) por nombre, apellidos, correo o
 * teléfono: 3 letras o más y hasta 20 resultados, con sus menores a cargo.
 * Busca al pulsar "Buscar", no con cada letra.
 */
export function BuscadorCliente({ onElegir, etiqueta = 'Buscar cliente' }: Props) {
  const [texto, setTexto] = useState('')
  const [buscado, setBuscado] = useState('')
  const [aviso, setAviso] = useState<string | undefined>()
  const clientes = useQuery(consultaClientes(buscado))

  function buscar(evento: FormEvent) {
    evento.preventDefault()
    const limpio = texto.trim()
    if (limpio.length < MINIMO) {
      setAviso(`Escribe al menos ${MINIMO} letras.`)
      return
    }
    setAviso(undefined)
    setBuscado(limpio)
  }

  return (
    <div className={estilos.buscador}>
      <form className={estilos.fila} onSubmit={buscar} role="search">
        <Campo
          etiqueta={etiqueta}
          ayuda="Nombre, apellidos, correo o teléfono."
          value={texto}
          error={aviso}
          onChange={(evento) => setTexto(evento.target.value)}
        />
        <Boton type="submit" variante="secundario">
          Buscar
        </Boton>
      </form>
      {buscado.length >= MINIMO &&
        (clientes.isPending ? (
          <Cargando texto="Buscando…" />
        ) : clientes.error ? (
          <ErrorConsulta error={clientes.error} reintentar={() => clientes.refetch()} />
        ) : clientes.data.length === 0 ? (
          <p className={estilos.suave}>No hay ningún cliente con «{buscado}». Para reservar a su nombre, antes debe registrarse.</p>
        ) : (
          <ul className={estilos.resultados} aria-label="Clientes encontrados">
            {clientes.data.map((cliente) => (
              <li key={cliente.id} className={estilos.resultado}>
                <span className={estilos.datos}>
                  <strong>
                    {cliente.nombre} {cliente.apellidos}
                  </strong>
                  <span className={estilos.suave}>
                    {cliente.email}
                    {cliente.telefono && ` · ${cliente.telefono}`}
                  </span>
                </span>
                <Boton variante="secundario" onClick={() => onElegir(cliente)} aria-label={`Elegir a ${cliente.nombre} ${cliente.apellidos}`}>
                  Elegir
                </Boton>
              </li>
            ))}
          </ul>
        ))}
    </div>
  )
}
