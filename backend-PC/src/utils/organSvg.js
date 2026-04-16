import { getOrganByKey, lookupOrganLabel } from '@/data/organLabelMap'

/**
 * 解析人体内脏 / 体表投影 SVG：合并 tspan 后匹配英文器官名，替换为中文并标记可点选。
 */
export function processOrganSvg(svgText) {
  const doc = new DOMParser().parseFromString(svgText, 'image/svg+xml')
  if (doc.querySelector('parsererror')) {
    throw new Error('SVG 解析失败')
  }
  const root = doc.documentElement
  if (root && root.localName === 'svg') {
    ensureSvgViewBoxFromDimensions(root)
    root.setAttribute('preserveAspectRatio', 'xMidYMid meet')
    root.removeAttribute('width')
    root.removeAttribute('height')
  }
  const texts = doc.querySelectorAll('text')
  texts.forEach((textEl) => {
    const full = joinTextContent(textEl)
    let hit = lookupOrganLabel(full)
    if (hit) {
      applyHit(textEl, hit)
      return
    }
    textEl.querySelectorAll('tspan').forEach((ts) => {
      const raw = ts.textContent || ''
      const h = lookupOrganLabel(raw.trim())
      if (h) {
        ts.textContent = h.zh
        textEl.setAttribute('data-organ-key', h.key)
        textEl.classList.add('organ-hit')
      }
    })
  })
  return new XMLSerializer().serializeToString(doc.documentElement)
}

/**
 * Wikimedia/Inkscape 部分 SVG 仅有 width/height 无 viewBox；去掉像素尺寸后若仍无 viewBox，
 * 浏览器无法用 height:auto 算出高度，图会塌成一条线。根据原尺寸补全 viewBox。
 */
function ensureSvgViewBoxFromDimensions(svgEl) {
  if (svgEl.getAttribute('viewBox')) return
  const w = parseSvgLength(svgEl.getAttribute('width'))
  const h = parseSvgLength(svgEl.getAttribute('height'))
  if (w > 0 && h > 0) {
    svgEl.setAttribute('viewBox', `0 0 ${w} ${h}`)
  }
}

function parseSvgLength(raw) {
  if (raw == null || raw === '') return NaN
  const n = parseFloat(String(raw).replace(/^\s*|\s*$/g, '').replace(/px$/i, ''))
  return Number.isFinite(n) && n > 0 ? n : NaN
}

function joinTextContent(textEl) {
  const tspans = textEl.querySelectorAll('tspan')
  let raw = ''
  if (!tspans.length) raw = textEl.textContent || ''
  else raw = Array.from(tspans).map((ts) => ts.textContent || '').join(' ')
  return raw.replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim()
}

function applyHit(textEl, hit) {
  const tspans = textEl.querySelectorAll('tspan')
  if (tspans.length <= 1) {
    if (tspans.length === 1) tspans[0].textContent = hit.zh
    else textEl.textContent = hit.zh
  } else {
    tspans.forEach((ts, i) => {
      if (i === 0) ts.textContent = hit.zh
      else ts.remove()
    })
  }
  textEl.setAttribute('data-organ-key', hit.key)
  textEl.classList.add('organ-hit')
}

export function organDetailFromElement(textEl) {
  const key = textEl.getAttribute('data-organ-key')
  if (!key) return null
  const row = getOrganByKey(key)
  if (!row) return null
  return {
    key: row.key,
    name: row.zh,
    diseases: row.diseases,
  }
}
