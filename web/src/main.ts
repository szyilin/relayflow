import './assets/css/main.css'
import './assets/css/workspace.css'

import { createApp } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { createRouter, createWebHistory } from 'vue-router'
import { routes, handleHotUpdate } from 'vue-router/auto-routes'
import { setupLayouts } from 'virtual:generated-layouts'
import { createPinia } from 'pinia'
import ui from '@nuxt/ui/vue-plugin'

import App from './App.vue'
import { setupAdminGuards } from './router/guards'

const app = createApp(App)
const pinia = createPinia()

const router = createRouter({
  routes: setupLayouts(routes as RouteRecordRaw[]),
  history: createWebHistory()
})

app.use(pinia)
setupAdminGuards(router)
app.use(router)
app.use(ui)

app.mount('#app')

if (import.meta.hot) {
  handleHotUpdate(router)
}
