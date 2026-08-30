import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'
import { defineConfig } from 'vite'

export default defineConfig(() => {
  const backendTarget = process.env.FORECASTR_PROXY_TARGET ?? 'http://127.0.0.1:8080'

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(import.meta.dirname, './src'),
      },
    },
    server: {
      proxy: {
        '/users': backendTarget,
        '/events': backendTarget,
        '/feed': backendTarget,
        '/stats': backendTarget,
        '/admin': backendTarget,
        '/ws': {
          target: backendTarget,
          ws: true,
        },
      },
    },
  }
})
