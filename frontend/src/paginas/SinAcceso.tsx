import { EnlaceBoton } from '../componentes/Boton'
import { useTitulo } from '../utilidades/useTitulo'
import estilos from './Paginas.module.css'

export function SinAcceso() {
  useTitulo('Sin acceso')
  return (
    <section className={estilos.texto}>
      <h1>No tienes acceso a esta página</h1>
      <p>Tu cuenta no tiene permiso para ver esta sección.</p>
      <EnlaceBoton to="/">Volver al inicio</EnlaceBoton>
    </section>
  )
}
