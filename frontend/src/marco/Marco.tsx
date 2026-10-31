import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, Navigate, NavLink, Outlet, useLocation, useNavigate } from 'react-router'

import { consultaNegocio } from '../api/catalogo'
import { Boton, EnlaceBoton } from '../componentes/Boton'
import { Cargando } from '../componentes/Cargando'
import { useSesion } from '../sesion/contexto'
import estilos from './Marco.module.css'

const ANIO_ACTUAL = new Date().getFullYear()

const ENLACES = [
  { a: '/servicios', texto: 'Servicios' },
  { a: '/profesionales', texto: 'Profesionales' },
  { a: '/horarios', texto: 'Horarios libres' },
]

/** Estructura común de todas las páginas: cabecera con menú, contenido y pie. */
export function Marco() {
  const { cargando, usuario, cerrarSesion } = useSesion()
  const { data: negocio } = useQuery(consultaNegocio)
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [rutaDelMenu, setRutaDelMenu] = useState('')
  const location = useLocation()
  const navigate = useNavigate()

  // Al cambiar de página, el menú del móvil se cierra.
  if (rutaDelMenu !== location.pathname) {
    setRutaDelMenu(location.pathname)
    setMenuAbierto(false)
  }

  // RN-005: con la contraseña temporal solo se puede cambiar la contraseña.
  if (usuario?.debeCambiarPassword && location.pathname !== '/cambiar-password') {
    return <Navigate to="/cambiar-password" replace />
  }

  async function salir() {
    // Primero se sale de la página actual: si es privada, al borrar la sesión
    // llevaría al login en lugar de al inicio.
    await navigate('/', { replace: true })
    await cerrarSesion()
  }

  const nombreNegocio = negocio?.nombre ?? 'Reserva tu cita'

  return (
    <>
      <a className={estilos.saltar} href="#contenido">
        Saltar al contenido
      </a>
      <header className={estilos.cabecera}>
        <div className={estilos.contenidoCabecera}>
          <div className={estilos.barra}>
            <Link to="/" className={estilos.marca}>
              {nombreNegocio}
            </Link>
            <button
              type="button"
              className={estilos.botonMenu}
              aria-expanded={menuAbierto}
              aria-controls="menu-principal"
              onClick={() => setMenuAbierto((abierto) => !abierto)}
            >
              <span className="solo-lector">{menuAbierto ? 'Cerrar menú' : 'Abrir menú'}</span>
              <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                {menuAbierto ? <path d="M6 6l12 12M18 6L6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
              </svg>
            </button>
          </div>
          <nav id="menu-principal" aria-label="Principal" className={`${estilos.menu} ${menuAbierto ? estilos.menuAbierto : ''}`}>
            <ul className={estilos.enlaces}>
              {ENLACES.map((enlace) => (
                <li key={enlace.a}>
                  <NavLink to={enlace.a} className={estilos.enlace}>
                    {enlace.texto}
                  </NavLink>
                </li>
              ))}
            </ul>
            {!cargando && (
              <ul className={estilos.cuenta}>
                {usuario ? (
                  <>
                    <li>
                      <NavLink to="/mi-area" className={estilos.enlace}>
                        Mi área <span className={estilos.saludo}>({usuario.nombre})</span>
                      </NavLink>
                    </li>
                    <li>
                      <Boton variante="secundario" onClick={salir}>
                        Cerrar sesión
                      </Boton>
                    </li>
                  </>
                ) : (
                  <>
                    <li>
                      <EnlaceBoton to="/login" variante="secundario" state={{ desde: location.pathname + location.search }}>
                        Iniciar sesión
                      </EnlaceBoton>
                    </li>
                    <li>
                      <EnlaceBoton to="/registro">Registrarse</EnlaceBoton>
                    </li>
                  </>
                )}
              </ul>
            )}
          </nav>
        </div>
      </header>
      <main id="contenido" className={estilos.principal} tabIndex={-1}>
        {cargando ? <Cargando /> : <Outlet />}
      </main>
      <footer className={estilos.pie}>
        <div>
          <span>© {ANIO_ACTUAL} {nombreNegocio}</span>
          <Link to="/privacidad">Política de privacidad</Link>
        </div>
      </footer>
    </>
  )
}
