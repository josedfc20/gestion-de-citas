import { Navigate, useLocation } from 'react-router'

import { Aviso } from '../componentes/Aviso'
import { EnlaceBoton } from '../componentes/Boton'
import { useSesion } from '../sesion/contexto'
import { NOMBRE_ROL, rutaInicialDe } from '../sesion/rutas'
import { useTitulo } from '../utilidades/useTitulo'
import estilos from './Paginas.module.css'

/**
 * Página provisional del admin hasta el B9b. Para el cliente y el empleado,
 * `/mi-area` solo redirige a su página de inicio (para no romper enlaces).
 */
export function MiArea() {
  useTitulo('Mi área')
  const { usuario } = useSesion()
  const passwordCambiada = (useLocation().state as { passwordCambiada?: boolean } | null)?.passwordCambiada
  if (!usuario) return null
  if (usuario.rol !== 'ADMIN') return <Navigate to={rutaInicialDe(usuario)} replace state={passwordCambiada ? { passwordCambiada } : undefined} />

  return (
    <section className={estilos.texto}>
      {passwordCambiada && <Aviso tipo="exito">Contraseña cambiada correctamente.</Aviso>}
      <h1>Hola, {usuario.nombre}</h1>
      <p className={estilos.suave}>Has iniciado sesión como {NOMBRE_ROL[usuario.rol].toLowerCase()}.</p>
      <p>Aquí gestionarás los servicios, los empleados, los horarios y las reservas. Esta sección está en construcción.</p>
      <div className={estilos.acciones}>
        <EnlaceBoton to="/horarios">Ver horarios libres</EnlaceBoton>
        <EnlaceBoton to="/cambiar-password" variante="secundario">
          Cambiar contraseña
        </EnlaceBoton>
      </div>
    </section>
  )
}
