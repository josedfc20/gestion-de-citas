import { QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'

import './estilos/variables.css'
import './estilos/base.css'
import { crearQueryClient } from './consultas'
import { rutas } from './rutas'
import { SesionProvider } from './sesion/SesionProvider'

const queryClient = crearQueryClient()
const router = createBrowserRouter(rutas)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <SesionProvider>
        <RouterProvider router={router} />
      </SesionProvider>
    </QueryClientProvider>
  </StrictMode>,
)
