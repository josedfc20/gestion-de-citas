import { useState } from 'react'

import { Aviso } from './Aviso'
import { Boton } from './Boton'

/**
 * RN-005: la contraseña temporal solo se muestra una vez (el servidor no la
 * guarda en claro). No se guarda en la web: al salir de la página se pierde.
 */
export function PasswordTemporal({ password, nombre }: { password: string; nombre: string }) {
  const [copiada, setCopiada] = useState(false)

  async function copiar() {
    try {
      await navigator.clipboard.writeText(password)
      setCopiada(true)
    } catch {
      setCopiada(false)
    }
  }

  return (
    <Aviso tipo="exito">
      <p>
        Contraseña temporal de {nombre}: <code>{password}</code>
      </p>
      <p>Apúntala y entrégasela al empleado: no se volverá a mostrar. La cambiará la primera vez que entre.</p>
      <Boton variante="secundario" onClick={copiar}>
        {copiada ? 'Copiada' : 'Copiar'}
      </Boton>
    </Aviso>
  )
}
