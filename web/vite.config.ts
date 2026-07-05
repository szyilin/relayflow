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
        }
      }
    })
  ],
  server: {
    proxy: {
      '/admin-api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
