import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'

// Configuración de Vite y de las pruebas (Vitest).
// En desarrollo, /api se reenvía al backend: para el navegador la web y la API
// tienen la misma dirección y la cookie del refresh token viaja sin CORS.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const destinoApi = env.VITE_API_PROXY || 'http://localhost:8080'

  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': { target: destinoApi, changeOrigin: false },
      },
    },
    preview: {
      port: 4173,
      proxy: {
        '/api': { target: destinoApi, changeOrigin: false },
      },
    },
    test: {
      environment: 'jsdom',
      globals: false,
      setupFiles: ['./src/pruebas/setup.ts'],
      css: { modules: { classNameStrategy: 'non-scoped' } },
      restoreMocks: true,
      // Cobertura (fase 5): npm run test:cobertura deja el informe en coverage/.
      coverage: {
        provider: 'v8',
        include: ['src/**/*.{ts,tsx}'],
        exclude: ['src/**/*.test.{ts,tsx}', 'src/pruebas/**', 'src/main.tsx', 'src/api/tipos.ts'],
        reporter: ['text-summary', 'html', 'json-summary'],
      },
    },
  }
})
