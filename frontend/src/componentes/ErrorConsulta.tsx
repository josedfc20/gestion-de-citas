import { mensajeDeError } from '../api/errores'
import { Aviso } from './Aviso'
import { Boton } from './Boton'

/** Error al cargar datos, con botón para reintentar. */
export function ErrorConsulta({ error, reintentar }: { error: unknown; reintentar?: () => void }) {
  return (
    <Aviso tipo="error">
      <p>{mensajeDeError(error)}</p>
      {reintentar && (
        <Boton variante="secundario" onClick={reintentar}>
          Reintentar
        </Boton>
      )}
    </Aviso>
  )
}
