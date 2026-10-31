import { EnlaceBoton } from '../componentes/Boton'
import { useTitulo } from '../utilidades/useTitulo'
import estilos from './Paginas.module.css'

export function NoEncontrada() {
  useTitulo('Página no encontrada')
  return (
    <section className={estilos.texto}>
      <h1>Página no encontrada</h1>
      <p>La dirección no existe o ha cambiado.</p>
      <EnlaceBoton to="/">Volver al inicio</EnlaceBoton>
    </section>
  )
}
