/** Wikimedia：正面内脏轮廓图 */
export const WM_PATH_FRONT = '/wikipedia/commons/8/8d/Man_shadow_anatomy.svg'

/**
 * 开发环境走 Vite 代理 /wm → upload.wikimedia.org；生产环境直连（或仅使用 public/images 本地文件）
 */
export function resolveWikimediaSvgUrl(path) {
  if (import.meta.env.DEV) {
    return `/wm${path}`
  }
  return `https://upload.wikimedia.org${path}`
}
