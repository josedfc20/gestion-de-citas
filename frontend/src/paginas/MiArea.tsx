import { useLocation } from 'react-router'

import { Aviso } from '../componentes/Aviso'
import { EnlaceBoton } from '../componentes/Boton'
import { useSesion } from '../sesion/contexto'
import { NOMBRE_ROL } from '../sesion/rutas'
import { useTitulo } from '../utilidades/useTitulo'
import estilos from './Paginas.module.css'

const TEXTO_POR_ROL = {
  CLIENTE: 'Aquí verás tus próximas citas, podrás reservar y gestionar tu perfil.',
  EMPLEADO: 'Aquí verás tu agenda, tus citas asignadas y tus servicios.',
  ADMIN: 'Aquí gestionarás los servicios, los empleados, los horarios y las reservas.',
} as const

/** Página provisional tras iniciar sesión. B8 (cliente) y B9 (empleado y admin) la sustituyen. */
export function MiArea() {
  useTitulo('Mi área')
  const { usuario } = useSesion()
  const passwordCambiada = (useLocation().state as { passwordCambiada?: boolean } | null)?.passwordCambiada
  if (!usuario) return null

  return (
    <section className={estilos.texto}>
      {passwordCambiada && <Aviso tipo="exito">Contraseña cambiada correctamente.</Aviso>}
      <h1>Hola, {usuario.nombre}</h1>
      <p className={estilos.suave}>Has iniciado sesión como {NOMBRE_ROL[usuario.rol].toLowerCase()}.</p>
      <p>{TEXTO_POR_ROL[usuario.rol]} Esta sección está en construcción.</p>
      <div className={estilos.acciones}>
        <EnlaceBoton to="/horarios">Ver horarios libres</EnlaceBoton>
        <EnlaceBoton to="/cambiar-password" variante="secundario">
          Cambiar contraseña
        </EnlaceBoton>
      </div>
    </section>
  )
}
