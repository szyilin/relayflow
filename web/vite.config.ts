import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueRouter from 'vue-router/vite'
import vueLayouts from 'vite-plugin-vue-layouts'
import ui from '@nuxt/ui/vite'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vueRouter({
      dts: 'src/route-map.d.ts'
    }),
    vueLayouts(),
    vue(),
    ui({
      ui: {
        colors: {
          primary: 'teal',
          neutral: 'zinc'
        },
        dashboardPanel: {
          slots: {
            root: 'relative flex flex-col min-w-0 h-full min-h-0 overflow-hidden lg:not-last:border-e lg:not-last:border-default shrink-0',
            body: 'flex flex-col gap-4 sm:gap-6 flex-1 min-h-0 overflow-y-auto p-4 sm:p-6'
          }
        }
      }
    })
  ],
  server: {
    proxy: {
      '/admin-api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/app-api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/infra/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
