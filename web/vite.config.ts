import { createRequire } from 'node:module'
import { existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueRouter from 'vue-router/vite'
import vueLayouts from 'vite-plugin-vue-layouts'
import ui from '@nuxt/ui/vite'

const require = createRequire(import.meta.url)

/**
 * TipTap / Nuxt UI: ProseMirror uses `instanceof` across modules.
 * Multiple physical copies → Enter fails with:
 * "Can not convert <> to a Fragment (looks like multiple versions of prosemirror-model were loaded)"
 * @see https://ui.nuxt.com/docs/components/editor
 */
const prosemirrorSingletons = [
  'prosemirror-model',
  'prosemirror-state',
  'prosemirror-view',
  'prosemirror-transform',
  'prosemirror-keymap',
  'prosemirror-commands',
  'prosemirror-schema-list',
  'prosemirror-history',
  'prosemirror-dropcursor',
  'prosemirror-gapcursor'
] as const

function packageRoot(name: string): string {
  let dir = dirname(require.resolve(name))
  while (dir !== '/' && !existsSync(join(dir, 'package.json'))) {
    dir = dirname(dir)
  }
  return dir
}

const prosemirrorAlias = Object.fromEntries(
  prosemirrorSingletons.map(name => [name, packageRoot(name)])
)

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
  resolve: {
    alias: prosemirrorAlias,
    dedupe: [...prosemirrorSingletons]
  },
  optimizeDeps: {
    include: [...prosemirrorSingletons]
  },
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
