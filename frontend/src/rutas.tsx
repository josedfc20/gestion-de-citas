import type { RouteObject } from 'react-router'

import { Marco } from './marco/Marco'
import { CambiarPassword } from './paginas/CambiarPassword'
import { DetalleCita } from './paginas/DetalleCita'
import { Horarios } from './paginas/Horarios'
import { Inicio } from './paginas/Inicio'
import { Login } from './paginas/Login'
import { Menores } from './paginas/Menores'
import { Perfil } from './paginas/Perfil'
import { MiArea } from './paginas/MiArea'
import { MisCitas } from './paginas/MisCitas'
import { NoEncontrada } from './paginas/NoEncontrada'
import { Privacidad } from './paginas/Privacidad'
import { Profesionales } from './paginas/Profesionales'
import { RecuperarPassword } from './paginas/RecuperarPassword'
import { Registro } from './paginas/Registro'
import { Reservar } from './paginas/Reservar'
import { RestablecerPassword } from './paginas/RestablecerPassword'
import { Servicios } from './paginas/Servicios'
import { DetalleReserva } from './paginas/admin/DetalleReserva'
import { NuevaReserva } from './paginas/admin/NuevaReserva'
import { Reservas } from './paginas/admin/Reservas'
import { Agenda } from './paginas/empleado/Agenda'
import { CitasAsignadas } from './paginas/empleado/CitasAsignadas'
import { DetalleCitaEmpleado } from './paginas/empleado/DetalleCitaEmpleado'
import { MisServicios } from './paginas/empleado/MisServicios'
import { RutaProtegida, SoloSinSesion } from './sesion/RutaProtegida'

/** Todas las rutas de la web; las privadas, dentro de <RutaProtegida roles={[...]}>. */
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
      // Antigua página provisional (B7): ahora solo lleva a la página de inicio de cada rol.
      { path: 'mi-area', element: <RutaProtegida><MiArea /></RutaProtegida> },
      // B8 · zona del cliente
      { path: 'reservar', element: <RutaProtegida roles={['CLIENTE']}><Reservar /></RutaProtegida> },
      { path: 'mis-citas', element: <RutaProtegida roles={['CLIENTE']}><MisCitas /></RutaProtegida> },
      { path: 'mis-citas/:id', element: <RutaProtegida roles={['CLIENTE']}><DetalleCita /></RutaProtegida> },
      { path: 'menores', element: <RutaProtegida roles={['CLIENTE']}><Menores /></RutaProtegida> },
      // B8b · cuenta (los tres roles) y recuperar la contraseña
      { path: 'perfil', element: <RutaProtegida><Perfil /></RutaProtegida> },
      { path: 'recuperar-password', element: <SoloSinSesion><RecuperarPassword /></SoloSinSesion> },
      { path: 'restablecer-password', element: <RestablecerPassword /> },
      // B9a · zona del empleado
      { path: 'empleado/agenda', element: <RutaProtegida roles={['EMPLEADO']}><Agenda /></RutaProtegida> },
      { path: 'empleado/citas', element: <RutaProtegida roles={['EMPLEADO']}><CitasAsignadas /></RutaProtegida> },
      { path: 'empleado/citas/:id', element: <RutaProtegida roles={['EMPLEADO']}><DetalleCitaEmpleado /></RutaProtegida> },
      { path: 'empleado/servicios', element: <RutaProtegida roles={['EMPLEADO']}><MisServicios /></RutaProtegida> },
      // B9b · reservas del administrador
      { path: 'admin/reservas', element: <RutaProtegida roles={['ADMIN']}><Reservas /></RutaProtegida> },
      { path: 'admin/reservas/nueva', element: <RutaProtegida roles={['ADMIN']}><NuevaReserva /></RutaProtegida> },
      { path: 'admin/reservas/:id', element: <RutaProtegida roles={['ADMIN']}><DetalleReserva /></RutaProtegida> },
      { path: '*', element: <NoEncontrada /> },
    ],
  },
]
