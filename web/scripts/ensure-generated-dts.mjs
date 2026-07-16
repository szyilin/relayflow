/**
 * 确保 unplugin 生成的 auto-imports.d.ts / components.d.ts 存在。
 * 二者 gitignore（含 pnpm hash 路径，不宜入库）；typecheck / CI 前按需用 Vite 插件生成。
 */
import { existsSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const autoImports = resolve(root, 'auto-imports.d.ts')
const components = resolve(root, 'components.d.ts')

if (existsSync(autoImports) && existsSync(components)) {
  process.exit(0)
}

const server = await createServer({
  root,
  configFile: resolve(root, 'vite.config.ts'),
  server: { middlewareMode: true }
})

try {
  // 触发 unplugin-auto-import / unplugin-vue-components 写 dts
  await server.pluginContainer.buildStart({})
  await server.transformRequest('/src/main.ts')
} finally {
  await server.close()
}

if (!existsSync(autoImports) || !existsSync(components)) {
  console.error('Failed to generate auto-imports.d.ts / components.d.ts. Run `pnpm build` once.')
  process.exit(1)
}
