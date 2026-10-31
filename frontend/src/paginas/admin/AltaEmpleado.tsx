import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { altaEmpleado, consultaServiciosAdmin } from '../../api/admin'
import { ApiError } from '../../api/errores'
import type { EmpleadoCreado } from '../../api/tipos'
import { Aviso } from '../../componentes/Aviso'
import { Boton, EnlaceBoton } from '../../componentes/Boton'
import { Campo } from '../../componentes/Campo'
import { Cargando } from '../../componentes/Cargando'
import { ErrorConsulta } from '../../componentes/ErrorConsulta'
import { PasswordTemporal } from '../../componentes/PasswordTemporal'
import { aplicarErroresDelServidor } from '../../utilidades/formularios'
import { useTitulo } from '../../utilidades/useTitulo'
import { esquemaEmpleado, type DatosFormularioEmpleado } from '../esquemas'
import estilos from '../Paginas.module.css'
import admin from './Admin.module.css'
import { CasillasServicios } from './CasillasServicios'

const CAMPOS = ['nombre', 'apellidos', 'email', 'telefono'] as const

/**
 * CU-017 y RN-005: alta de un empleado. El servidor genera la contraseña
 * temporal, que se muestra aquí una sola vez.
 */
export function AltaEmpleado() {
  useTitulo('Alta de empleado')
  const queryClient = useQueryClient()
  const servicios = useQuery(consultaServiciosAdmin)
  const [servicioIds, setServicioIds] = useState<number[]>([])
  const [creado, setCreado] = useState<EmpleadoCreado | null>(null)
  const [error, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormularioEmpleado>({
    resolver: zodResolver(esquemaEmpleado),
    defaultValues: { nombre: '', apellidos: '', email: '', telefono: '' },
  })

  if (creado) {
    const { empleado } = creado
    return (
      <section className={admin.estrecho}>
        <h1>Empleado dado de alta</h1>
        <PasswordTemporal password={creado.passwordTemporal} nombre={`${empleado.nombre} ${empleado.apellidos}`} />
        <p>
          Entrará con <strong>{empleado.email}</strong> y esta contraseña; la web le pedirá cambiarla.
        </p>
        <div className={estilos.acciones}>
          <EnlaceBoton to={`/admin/empleados/${empleado.id}`}>Ver su ficha</EnlaceBoton>
          <EnlaceBoton to={`/admin/empleados/${empleado.id}/horario`} variante="secundario">
            Ajustar su horario
          </EnlaceBoton>
        </div>
      </section>
    )
  }

  async function guardar(datos: DatosFormularioEmpleado) {
    setErrorGeneral(null)
    try {
      const respuesta = await altaEmpleado({ ...datos, telefono: datos.telefono || null, servicioIds })
      await queryClient.invalidateQueries({ queryKey: ['admin'] })
      await queryClient.invalidateQueries({ queryKey: ['profesionales'] })
      setCreado(respuesta)
    } catch (fallo) {
      if (fallo instanceof ApiError && fallo.status === 409) {
        setError('email', { type: 'servidor', message: fallo.message }, { shouldFocus: true })
      } else {
        const texto = aplicarErroresDelServidor(fallo, setError, CAMPOS)
        if (texto) setErrorGeneral(texto)
      }
    }
  }

  return (
    <section className={admin.estrecho}>
      <h1>Alta de empleado</h1>
      <form className={admin.bloque} onSubmit={handleSubmit(guardar)} noValidate>
        {error && <Aviso tipo="error">{error}</Aviso>}
        <div className={estilos.dosColumnas}>
          <Campo etiqueta="Nombre" autoComplete="off" error={errors.nombre?.message} {...register('nombre')} />
          <Campo etiqueta="Apellidos" autoComplete="off" error={errors.apellidos?.message} {...register('apellidos')} />
        </div>
        <div className={estilos.dosColumnas}>
          <Campo etiqueta="Correo electrónico" type="email" autoComplete="off" error={errors.email?.message} {...register('email')} />
          <Campo etiqueta="Teléfono" type="tel" opcional error={errors.telefono?.message} {...register('telefono')} />
        </div>
        {servicios.isPending ? (
          <Cargando texto="Cargando servicios…" />
        ) : servicios.error ? (
          <ErrorConsulta error={servicios.error} reintentar={() => servicios.refetch()} />
        ) : (
          <CasillasServicios servicios={servicios.data} elegidos={servicioIds} onCambiar={setServicioIds} />
        )}
        <p className={estilos.suave}>Al guardar se genera una contraseña temporal que solo verás una vez.</p>
        <Boton type="submit" enviando={isSubmitting} textoEnviando="Guardando…">
          Dar de alta
        </Boton>
      </form>
      <EnlaceBoton to="/admin/empleados" variante="enlace">
        Volver a los empleados
      </EnlaceBoton>
    </section>
  )
}
