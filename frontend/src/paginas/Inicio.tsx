import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'

import { consultaNegocio } from '../api/catalogo'
import { EnlaceBoton } from '../componentes/Boton'
import { useTitulo } from '../utilidades/useTitulo'
import estilos from './Paginas.module.css'
import propias from './Inicio.module.css'

const SECCIONES = [
  { a: '/servicios', titulo: 'Servicios y precios', texto: 'Todo lo que hacemos, cuánto dura y cuánto cuesta.' },
  { a: '/profesionales', titulo: 'Profesionales', texto: 'Conoce al equipo y qué servicios realiza cada persona.' },
  { a: '/horarios', titulo: 'Horarios libres', texto: 'Elige servicio, profesional y día, y mira las horas disponibles.' },
]

export function Inicio() {
  const { data: negocio } = useQuery(consultaNegocio)
  useTitulo('Inicio')

  return (
    <>
      <section className={propias.portada}>
        <h1>{negocio?.nombre ?? 'Reserva tu cita'}</h1>
        <p className={estilos.subtitulo}>Consulta nuestros servicios y las horas libres de cada profesional, y reserva tu cita en línea.</p>
        <div className={estilos.acciones}>
          <EnlaceBoton to="/horarios">Ver horarios libres</EnlaceBoton>
          <EnlaceBoton to="/servicios" variante="secundario">
            Ver servicios
          </EnlaceBoton>
        </div>
      </section>
      <ul className={estilos.rejilla}>
        {SECCIONES.map((seccion) => (
          <li key={seccion.a} className={estilos.tarjeta}>
            <h2>
              <Link to={seccion.a}>{seccion.titulo}</Link>
            </h2>
            <p className={estilos.suave}>{seccion.texto}</p>
          </li>
        ))}
      </ul>
    </>
  )
}
