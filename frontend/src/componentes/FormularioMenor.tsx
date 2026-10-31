import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { consultaBeneficiarios, corregirBeneficiario, crearBeneficiario } from '../api/beneficiarios'
import { ApiError } from '../api/errores'
import type { Beneficiario, Negocio } from '../api/tipos'
import { esquemaBeneficiario, type DatosFormularioBeneficiario } from '../paginas/esquemas'
import { hoyEnZona } from '../utilidades/fechas'
import { aplicarErroresDelServidor } from '../utilidades/formularios'
import { Aviso } from './Aviso'
import { Boton } from './Boton'
import { Campo } from './Campo'
import estilos from './FormularioMenor.module.css'

const CAMPOS = ['nombre', 'apellidos', 'fechaNacimiento'] as const

interface Props {
  negocio: Negocio
  /** Para corregir uno que ya existe; sin él, se añade uno nuevo. */
  menor?: Beneficiario
  onGuardado: (menor: Beneficiario) => void
  onCancelar: () => void
}

/** Añadir o corregir un menor a cargo (CU-009). Lo usan /menores y /reservar. */
export function FormularioMenor({ negocio, menor, onGuardado, onCancelar }: Props) {
  const queryClient = useQueryClient()
  const hoy = hoyEnZona(negocio.zonaHoraria)
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormularioBeneficiario>({
    resolver: zodResolver(esquemaBeneficiario(hoy, negocio.edadMinimaCuenta)),
    defaultValues: {
      nombre: menor?.nombre ?? '',
      apellidos: menor?.apellidos ?? '',
      fechaNacimiento: menor?.fechaNacimiento ?? '',
    },
  })

  async function enviar(datos: DatosFormularioBeneficiario) {
    setErrorGeneral(null)
    try {
      const guardado = menor ? await corregirBeneficiario(menor.id, datos) : await crearBeneficiario(datos)
      await queryClient.invalidateQueries({ queryKey: consultaBeneficiarios.queryKey })
      onGuardado(guardado)
    } catch (error) {
      if (error instanceof ApiError && error.status === 422) {
        // "Ya tiene la edad para crear su propia cuenta."
        setError('fechaNacimiento', { type: 'servidor', message: error.message }, { shouldFocus: true })
      } else {
        setErrorGeneral(aplicarErroresDelServidor(error, setError, CAMPOS))
      }
    }
  }

  const titulo = menor ? `Corregir los datos de ${menor.nombre}` : 'Añadir un menor a cargo'

  return (
    <form className={estilos.formulario} onSubmit={handleSubmit(enviar)} noValidate aria-label={titulo}>
      <h3 className={estilos.titulo}>{titulo}</h3>
      {errorGeneral && <Aviso tipo="error">{errorGeneral}</Aviso>}
      <div className={estilos.campos}>
        <Campo etiqueta="Nombre" autoComplete="off" error={errors.nombre?.message} {...register('nombre')} />
        <Campo etiqueta="Apellidos" autoComplete="off" error={errors.apellidos?.message} {...register('apellidos')} />
      </div>
      <Campo
        etiqueta="Fecha de nacimiento"
        type="date"
        max={hoy}
        ayuda={`Menor de ${negocio.edadMinimaCuenta} años; a partir de esa edad necesita su propia cuenta.`}
        error={errors.fechaNacimiento?.message}
        {...register('fechaNacimiento')}
      />
      <div className={estilos.botones}>
        <Boton type="submit" enviando={isSubmitting} textoEnviando="Guardando…">
          {menor ? 'Guardar cambios' : 'Añadir'}
        </Boton>
        <Boton variante="secundario" onClick={onCancelar} disabled={isSubmitting}>
          Cancelar
        </Boton>
      </div>
    </form>
  )
}
