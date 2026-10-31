import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'

import { crearQueryClient } from '../consultas'
import { rutas } from '../rutas'
import { SesionProvider } from '../sesion/SesionProvider'

/** Monta la web completa (mismas rutas que en el navegador) empezando en `ruta`. */
export function renderizarApp(ruta = '/') {
  const queryClient = crearQueryClient()
  queryClient.setDefaultOptions({ queries: { ...queryClient.getDefaultOptions().queries, retry: false } })
  const router = createMemoryRouter(rutas, { initialEntries: [ruta] })
  const usuario = userEvent.setup()
  const resultado = render(
    <QueryClientProvider client={queryClient}>
      <SesionProvider>
        <RouterProvider router={router} />
      </SesionProvider>
    </QueryClientProvider>,
  )
  return { ...resultado, router, usuario, queryClient }
}
