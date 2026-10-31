import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { cliente, problema, sesionDe } from '../pruebas/datos'
import { API, servidor } from '../pruebas/servidor'
import { establecerSesion, escucharSesion, pedir, renovarSesion, tokenActual } from './cliente'
import { ApiError } from './errores'

describe('cliente de la API', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('envía el access token en la cabecera Authorization', async () => {
    let cabecera: string | null = null
    servidor.use(
      http.get(`${API}/me`, ({ request }) => {
        cabecera = request.headers.get('Authorization')
        return HttpResponse.json({ ok: true })
      }),
    )
    establecerSesion(sesionDe(cliente, 'abc'))

    await pedir('/me')

    expect(cabecera).toBe('Bearer abc')
  })

  it('con un 401 renueva la sesión y repite la petición una vez con el token nuevo', async () => {
    const tokensRecibidos: (string | null)[] = []
    servidor.use(
      http.get(`${API}/me`, ({ request }) => {
        const token = request.headers.get('Authorization')
        tokensRecibidos.push(token)
        return token === 'Bearer nuevo' ? HttpResponse.json({ id: 5 }) : HttpResponse.json(problema(401, 'Token caducado'), { status: 401 })
      }),
      http.post(`${API}/auth/refresh`, () => HttpResponse.json(sesionDe(cliente, 'nuevo'))),
    )
    establecerSesion(sesionDe(cliente, 'caducado'))

    const respuesta = await pedir<{ id: number }>('/me')

    expect(respuesta).toEqual({ id: 5 })
    expect(tokensRecibidos).toEqual(['Bearer caducado', 'Bearer nuevo'])
    expect(tokenActual()).toBe('nuevo')
  })

  it('dos peticiones con 401 a la vez hacen un solo refresh (la rotación cerraría todas las sesiones)', async () => {
    let refrescos = 0
    servidor.use(
      http.get(`${API}/me/:recurso`, ({ request }) =>
        request.headers.get('Authorization') === 'Bearer nuevo'
          ? HttpResponse.json({ ok: true })
          : HttpResponse.json(problema(401, 'Token caducado'), { status: 401 }),
      ),
      http.post(`${API}/auth/refresh`, async () => {
        refrescos++
        await new Promise((resolver) => setTimeout(resolver, 20))
        return HttpResponse.json(sesionDe(cliente, 'nuevo'))
      }),
    )
    establecerSesion(sesionDe(cliente, 'caducado'))

    await Promise.all([pedir('/me/citas'), pedir('/me/beneficiarios')])

    expect(refrescos).toBe(1)
  })

  it('si el refresh falla, borra la sesión, avisa y devuelve el 401', async () => {
    servidor.use(http.get(`${API}/me`, () => HttpResponse.json(problema(401, 'Token caducado'), { status: 401 })))
    establecerSesion(sesionDe(cliente, 'caducado'))
    const oyente = vi.fn<(sesion: unknown) => void>()
    const dejarDeEscuchar = escucharSesion(oyente)

    await expect(pedir('/me')).rejects.toMatchObject({ status: 401 })

    expect(tokenActual()).toBeNull()
    expect(oyente).toHaveBeenCalledWith(null)
    dejarDeEscuchar()
  })

  it('sin token, un 401 no intenta renovar', async () => {
    let refrescos = 0
    servidor.use(
      http.post(`${API}/auth/login`, () => HttpResponse.json(problema(401, 'Correo o contraseña no válidos.'), { status: 401 })),
      http.post(`${API}/auth/refresh`, () => {
        refrescos++
        return HttpResponse.json(sesionDe(cliente))
      }),
    )

    await expect(pedir('/auth/login', { metodo: 'POST', cuerpo: {} })).rejects.toBeInstanceOf(ApiError)
    expect(refrescos).toBe(0)
  })

  it('el refresh va dentro del candado del navegador para que dos pestañas no renueven a la vez', async () => {
    const request = vi.fn<(nombre: string, funcion: () => Promise<unknown>) => Promise<unknown>>((_nombre, funcion) => funcion())
    vi.stubGlobal('navigator', { ...navigator, locks: { request } })
    servidor.use(http.post(`${API}/auth/refresh`, () => HttpResponse.json(sesionDe(cliente, 'nuevo'))))

    const sesion = await renovarSesion()

    expect(sesion?.usuario).toEqual(cliente)
    expect(request).toHaveBeenCalledWith('gestion-de-citas:refresh', expect.any(Function))
  })

  it('convierte un Problem Details en ApiError con los errores de cada campo', async () => {
    servidor.use(
      http.post(`${API}/auth/registro`, () =>
        HttpResponse.json(
          problema(400, 'Hay datos con formato incorrecto.', {
            errores: [
              { campo: 'password', mensaje: 'Es una contraseña demasiado común.' },
              { campo: 'confirmacionPasswordCoincide', mensaje: 'Las contraseñas no coinciden.' },
            ],
          }),
          { status: 400 },
        ),
      ),
    )

    const error = await pedir('/auth/registro', { metodo: 'POST', cuerpo: {} }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      message: 'Hay datos con formato incorrecto.',
      errores: [
        { campo: 'password', mensaje: 'Es una contraseña demasiado común.' },
        { campo: 'confirmacionPassword', mensaje: 'Las contraseñas no coinciden.' },
      ],
    })
  })

  it('sin conexión da un error de red con un mensaje claro', async () => {
    servidor.use(http.get(`${API}/servicios`, () => HttpResponse.error()))

    const error = await pedir('/servicios').catch((e: unknown) => e)

    expect(error).toMatchObject({ status: 0, esDeRed: true })
    expect((error as ApiError).message).toMatch(/No se puede conectar/)
  })

  it('omite los parámetros vacíos de la URL', async () => {
    let busqueda = ''
    servidor.use(
      http.get(`${API}/profesionales`, ({ request }) => {
        busqueda = new URL(request.url).search
        return HttpResponse.json([])
      }),
    )

    await pedir('/profesionales', { parametros: { servicioId: undefined, activo: true } })

    expect(busqueda).toBe('?activo=true')
  })
})
