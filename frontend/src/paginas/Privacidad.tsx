import { useQuery } from '@tanstack/react-query'

import { consultaNegocio } from '../api/catalogo'
import { useTitulo } from '../utilidades/useTitulo'
import estilos from './Paginas.module.css'

/**
 * Política de privacidad (RNF-007). Texto provisional que se revisará antes del
 * despliegue (fase 7) con los datos reales del responsable.
 */
export function Privacidad() {
  useTitulo('Política de privacidad')
  const { data: negocio } = useQuery(consultaNegocio)
  const responsable = negocio?.nombre ?? 'el negocio'

  return (
    <article className={estilos.texto}>
      <h1>Política de privacidad</h1>
      <p className={estilos.suave}>Versión provisional. Se completará con los datos de contacto del responsable antes de publicar la web.</p>

      <h2>Quién trata tus datos</h2>
      <p>El responsable es {responsable}. Usamos tus datos solo para gestionar tus citas.</p>

      <h2>Qué datos guardamos y para qué</h2>
      <ul>
        <li>Nombre, apellidos, correo y, si lo das, teléfono: para identificarte y avisarte de cambios en tus citas.</li>
        <li>Fecha de nacimiento: para comprobar que tienes la edad mínima para crear una cuenta ({negocio?.edadMinimaCuenta ?? 14} años).</li>
        <li>Datos de los menores a tu cargo (nombre, apellidos y fecha de nacimiento): solo para reservar sus citas.</li>
        <li>Tus citas: servicio, profesional, fecha, precio y estado.</li>
      </ul>
      <p>La base legal es tu consentimiento al registrarte y la gestión de las citas que pides. No cedemos tus datos a terceros ni los usamos para publicidad.</p>

      <h2>Cuánto tiempo los guardamos</h2>
      <p>Mientras tengas la cuenta. Si la eliminas, tus datos personales se anonimizan; las citas pasadas se conservan sin datos que te identifiquen.</p>

      <h2>Tus derechos</h2>
      <p>Desde tu perfil puedes consultar y corregir tus datos, descargarlos en un archivo y eliminar tu cuenta. También puedes ejercer tus derechos de acceso, rectificación, supresión, oposición, limitación y portabilidad contactando con el negocio, y reclamar ante la Agencia Española de Protección de Datos (aepd.es).</p>

      <h2>Seguridad</h2>
      <p>Las contraseñas se guardan cifradas con un algoritmo de un solo sentido y la sesión se mantiene con una cookie segura que no pueden leer otras páginas.</p>
    </article>
  )
}
