import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://1.14.191.118:8080'
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 5174,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
        '/club-division-img': {
          target: proxyTarget,
          changeOrigin: true,
        },
        /** 病机图谱：开发环境代理 Wikimedia SVG，避免直连被墙或跨域失败导致白屏 */
        '/wm': {
          target: 'https://upload.wikimedia.org',
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path.replace(/^\/wm/, ''),
        },
      },
    },
  }
})
