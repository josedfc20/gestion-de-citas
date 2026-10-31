import { QueryClient } from '@tanstack/react-query'

import { ApiError } from './api/errores'

/** Los 4xx no se reintentan (no van a cambiar); los errores de red y 5xx, una vez. */
export function crearQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: (fallos, error) => fallos < 1 && !(error instanceof ApiError && error.status >= 400 && error.status < 500),
        refetchOnWindowFocus: false,
      },
    },
  })
}
