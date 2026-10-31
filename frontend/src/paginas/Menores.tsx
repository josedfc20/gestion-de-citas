import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import { consultaBeneficiarios, eliminarBeneficiario } from '../api/beneficiarios'
import { consultaNegocio } from '../api/catalogo'
import { mensajeDeError } from '../api/errores'
import type { Beneficiario, Negocio } from '../api/tipos'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Cargando } from '../componentes/Cargando'
import { Confirmacion } from '../componentes/Confirmacion'
import { ErrorConsulta } from '../componentes/ErrorConsulta'
import { FormularioMenor } from '../componentes/FormularioMenor'
import { edadEn, hoyEnZona } from '../utilidades/fechas'
import { useTitulo } from '../utilidades/useTitulo'
import propias from './Menores.module.css'
import estilos from './Paginas.module.css'

type Edicion = { tipo: 'nuevo' } | { tipo: 'corregir'; menor: Beneficiario } | { tipo: 'eliminar'; menor: Beneficiario } | null

/** CU-009 y RN-007: los menores por los que el cliente reserva. */
export function Menores() {
  useTitulo('Menores a mi cargo')
  const negocio = useQuery(consultaNegocio)
  const menores = useQuery(consultaBeneficiarios)

  const fallida = negocio.error ? negocio : menores.error ? menores : null
  if (fallida) return <ErrorConsulta error={fallida.error} reintentar={() => fallida.refetch()} />
  if (!negocio.data || !menores.data) return <Cargando />
  return <ListaMenores negocio={negocio.data} menores={menores.data} />
}

function ListaMenores({ negocio, menores }: { negocio: Negocio; menores: Beneficiario[] }) {
  const queryClient = useQueryClient()
  const hoy = hoyEnZona(negocio.zonaHoraria)
  const [edicion, setEdicion] = useState<Edicion>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [eliminando, setEliminando] = useState(false)

  function abrir(nueva: Edicion) {
    setAviso(null)
    setError(null)
    setEdicion(nueva)
  }

  async function eliminar(menor: Beneficiario) {
    setError(null)
    setEliminando(true)
    try {
      await eliminarBeneficiario(menor.id)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: consultaBeneficiarios.queryKey }),
        queryClient.invalidateQueries({ queryKey: ['mis-citas'] }),
      ])
      setEdicion(null)
      setAviso(`Se ha eliminado a ${menor.nombre}.`)
    } catch (fallo) {
      setError(mensajeDeError(fallo))
    } finally {
      setEliminando(false)
    }
  }

  return (
    <section className={estilos.texto}>
      <header className={estilos.cabecera}>
        <h1>Menores a mi cargo</h1>
        <p className={estilos.subtitulo}>
          Los menores de {negocio.edadMinimaCuenta} años no tienen cuenta propia: reservas tú por ellos y figuras como responsable.
          Solo guardamos su nombre, apellidos y fecha de nacimiento.
        </p>
      </header>

      {aviso && <Aviso tipo="exito">{aviso}</Aviso>}
      {error && <Aviso tipo="error">{error}</Aviso>}

      {menores.length === 0 ? (
        <p>Todavía no has añadido ningún menor.</p>
      ) : (
        <ul className={propias.lista}>
          {menores.map((menor) => (
            <li key={menor.id} className={propias.menor}>
              <div className={propias.fila}>
                <div>
                  <strong>
                    {menor.nombre} {menor.apellidos}
                  </strong>
                  <span className={`${estilos.suave} ${propias.edad}`}>{textoEdad(menor, hoy)}</span>
                </div>
                <div className={propias.acciones}>
                  <Boton variante="enlace" aria-label={`Corregir a ${menor.nombre}`} onClick={() => abrir({ tipo: 'corregir', menor })}>
                    Corregir
                  </Boton>
                  <Boton variante="enlace" aria-label={`Eliminar a ${menor.nombre}`} onClick={() => abrir({ tipo: 'eliminar', menor })}>
                    Eliminar
                  </Boton>
                </div>
              </div>
              {edicion?.tipo === 'corregir' && edicion.menor.id === menor.id && (
                <FormularioMenor
                  negocio={negocio}
                  menor={menor}
                  onGuardado={(guardado) => {
                    setEdicion(null)
                    setAviso(`Datos de ${guardado.nombre} guardados.`)
                  }}
                  onCancelar={() => setEdicion(null)}
                />
              )}
              {edicion?.tipo === 'eliminar' && edicion.menor.id === menor.id && (
                <Confirmacion
                  titulo={`¿Eliminar a ${menor.nombre}?`}
                  textoConfirmar="Sí, eliminar"
                  textoEnviando="Eliminando…"
                  enviando={eliminando}
                  peligro
                  onConfirmar={() => eliminar(menor)}
                  onVolver={() => setEdicion(null)}
                >
                  <p>Si tiene citas próximas, se cancelarán. Esto no se puede deshacer.</p>
                </Confirmacion>
              )}
            </li>
          ))}
        </ul>
      )}

      {edicion?.tipo === 'nuevo' ? (
        <FormularioMenor
          negocio={negocio}
          onGuardado={(guardado) => {
            setEdicion(null)
            setAviso(`${guardado.nombre} se ha añadido. Ya puedes reservar citas para ${guardado.nombre}.`)
          }}
          onCancelar={() => setEdicion(null)}
        />
      ) : (
        <Boton onClick={() => abrir({ tipo: 'nuevo' })}>Añadir un menor</Boton>
      )}
    </section>
  )
}

function textoEdad(menor: Beneficiario, hoy: string): string {
  const edad = edadEn(menor.fechaNacimiento, hoy)
  if (edad < 1) return 'Menos de 1 año'
  return edad === 1 ? '1 año' : `${edad} años`
}
