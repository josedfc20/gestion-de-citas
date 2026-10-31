import type { RouteObject } from 'react-router'

import { Marco } from './marco/Marco'
import { CambiarPassword } from './paginas/CambiarPassword'
import { Horarios } from './paginas/Horarios'
import { Inicio } from './paginas/Inicio'
import { Login } from './paginas/Login'
import { MiArea } from './paginas/MiArea'
import { NoEncontrada } from './paginas/NoEncontrada'
import { Privacidad } from './paginas/Privacidad'
import { Profesionales } from './paginas/Profesionales'
import { Registro } from './paginas/Registro'
import { Servicios } from './paginas/Servicios'
import { RutaProtegida, SoloSinSesion } from './sesion/RutaProtegida'

/**
 * Todas las rutas de la web. B8 y B9 añaden aquí las suyas dentro de
 * <RutaProtegida roles={[...]}>.
 */
export const rutas: RouteObject[] = [
  {
    element: <Marco />,
    children: [
      { index: true, element: <Inicio /> },
      { path: 'servicios', element: <Servicios /> },
      { path: 'profesionales', element: <Profesionales /> },
      { path: 'horarios', element: <Horarios /> },
      { path: 'privacidad', element: <Privacidad /> },
      { path: 'login', element: <SoloSinSesion><Login /></SoloSinSesion> },
      { path: 'registro', element: <SoloSinSesion><Registro /></SoloSinSesion> },
      { path: 'cambiar-password', element: <RutaProtegida><CambiarPassword /></RutaProtegida> },
      { path: 'mi-area', element: <RutaProtegida><MiArea /></RutaProtegida> },
      { path: '*', element: <NoEncontrada /> },
    ],
  },
]
