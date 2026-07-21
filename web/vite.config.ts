import { createRequire } from 'node:module'
import { existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Plugin } from 'vite'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueRouter from 'vue-router/vite'
import vueLayouts from 'vite-plugin-vue-layouts'
import ui from '@nuxt/ui/vite'

const require = createRequire(import.meta.url)
const rootDir = dirname(fileURLToPath(import.meta.url))
const nuxtUiUseToast = join(rootDir, 'node_modules/@nuxt/ui/dist/runtime/composables/useToast.js')
const appUseToast = join(rootDir, 'src/composables/useToast.ts')

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

/** Route Nuxt UI useToast → app wrapper (icons + close/progress defaults). */
function relayflowToastWrapper(): Plugin {
  return {
    name: 'relayflow-toast-wrapper',
    enforce: 'pre',
    resolveId(id) {
      if (id === 'relayflow-nuxt-use-toast') {
        return nuxtUiUseToast
      }
      if (
        id.includes('@nuxt/ui')
        && id.includes('runtime/composables/useToast')
        && !id.includes('relayflow-nuxt-use-toast')
      ) {
        return appUseToast
      }
      return null
    }
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    relayflowToastWrapper(),
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
        // Nuxt UI 表单控件默认 inline-flex / 内容宽度；卡片/弹窗内需撑满才自然。
        // 紧凑场景用 class="w-auto" / max-w-* / flex-1 覆盖即可。
        formField: {
          slots: {
            root: 'w-full',
            container: 'w-full'
          }
        },
        input: {
          slots: {
            root: 'w-full'
          }
        },
        inputNumber: {
          slots: {
            root: 'w-full'
          }
        },
        textarea: {
          slots: {
            root: 'w-full'
          }
        },
        select: {
          slots: {
            base: 'w-full'
          }
        },
        selectMenu: {
          slots: {
            base: 'w-full'
          }
        },
        inputMenu: {
          slots: {
            base: 'w-full'
          }
        },
        dashboardPanel: {
          slots: {
            root: 'relative flex flex-col min-w-0 h-full min-h-0 overflow-hidden lg:not-last:border-e lg:not-last:border-default shrink-0',
            body: 'flex flex-col gap-4 sm:gap-6 flex-1 min-h-0 overflow-y-auto p-4 sm:p-6'
          }
        },
        // Compact top-center bubble: opaque fills; center on page midline (not left-aligned in sm:w-96).
        toaster: {
          slots: {
            // Default uses inset-x-0 → full viewport width, so w-fit bubbles stick left. Pin to midline instead.
            base: 'pointer-events-auto absolute left-1/2 -translate-x-1/2 w-max max-w-[calc(100%-2rem)] z-(--index) transform-(--transform) data-[expanded=false]:data-[front=false]:h-(--front-height) data-[expanded=false]:data-[front=false]:*:opacity-0 data-[front=false]:*:transition-opacity data-[front=false]:*:duration-100 data-[state=closed]:animate-[toast-closed_200ms_ease-in-out] data-[state=closed]:data-[expanded=false]:data-[front=false]:animate-[toast-collapsed-closed_200ms_ease-in-out] data-[state=open]:data-[pulsing=odd]:animate-[toast-pulse-a_300ms_ease-out] data-[state=open]:data-[pulsing=even]:animate-[toast-pulse-b_300ms_ease-out] data-[swipe=move]:transition-none transition-[transform,translate,height] duration-200 ease-out'
          }
        },
        toast: {
          slots: {
            root: 'relative group overflow-hidden bg-default shadow-sm rounded-lg ring-1 ring-default px-3 py-2 flex items-center gap-2 w-max max-w-[min(20rem,calc(100vw-2rem))]',
            wrapper: 'w-auto min-w-0 flex flex-col justify-center',
            title: 'text-sm font-medium text-highlighted leading-5',
            description: 'text-xs text-muted leading-4',
            icon: 'shrink-0 size-4',
            avatar: 'shrink-0',
            avatarSize: 'xs',
            actions: 'flex gap-1.5 shrink-0',
            progress: 'hidden',
            close: 'hidden'
          },
          variants: {
            color: {
              success: {
                root: 'bg-default ring-success',
                icon: 'text-success'
              },
              warning: {
                root: 'bg-default ring-warning',
                icon: 'text-warning'
              },
              error: {
                root: 'bg-default ring-error',
                icon: 'text-error'
              },
              info: {
                root: 'bg-default ring-info',
                icon: 'text-info'
              },
              primary: {
                root: 'bg-default ring-primary',
                icon: 'text-primary'
              },
              secondary: {
                root: 'bg-default ring-secondary',
                icon: 'text-secondary'
              },
              neutral: {
                root: 'bg-default ring-default',
                icon: 'text-muted'
              }
            },
            orientation: {
              vertical: {
                root: 'items-center',
                actions: 'items-center mt-0'
              },
              horizontal: {
                root: 'items-center',
                actions: 'items-center'
              }
            },
            title: {
              true: {
                description: 'mt-0.5'
              }
            }
          },
          defaultVariants: {
            color: 'neutral'
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
