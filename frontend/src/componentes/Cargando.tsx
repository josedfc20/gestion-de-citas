import estilos from './Cargando.module.css'

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <div className={estilos.cargando} role="status">
      <span className={estilos.rueda} aria-hidden="true" />
      {texto}
    </div>
  )
}
