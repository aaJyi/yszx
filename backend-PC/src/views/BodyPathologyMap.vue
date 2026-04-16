<template>
  <div class="page-body-map">
    <header class="page-head panel float-in">
      <div>
        <p class="sub">医视智行 · 部位风险预览</p>
        <h2>病机图谱</h2>
      </div>
    </header>

    <section class="panel content-panel float-in">
      <div v-if="loading" class="loading-box">
        <el-icon class="loading-icon is-loading" :size="28"><Loading /></el-icon>
        <p>正在加载内脏示意图（首次需从网络拉取 SVG）…</p>
      </div>

      <div v-else class="chart-stage">
        <figure class="figure-block">
          <div v-if="errFront" class="err-box">
            示意图加载失败。请将 <code>organ-front.svg</code> 放到 <code>public/images/</code>，或检查本机能否访问
            Wikimedia；开发环境已启用 <code>/wm</code> 代理，请用 <code>npm run dev</code> 启动。
          </div>
          <div v-else ref="frontWrap" class="svg-host" v-html="frontHtml" />
        </figure>
      </div>
    </section>

    <Teleport to="body">
      <div
        v-if="selected"
        class="detail-overlay"
        @click.self="clearSelection"
      >
        <aside
          class="detail-panel detail-panel--float"
          :class="selected.detailSide === 'left' ? 'is-left' : 'is-right'"
          @click.stop
        >
          <button type="button" class="detail-close" aria-label="关闭" @click="clearSelection">×</button>
          <div class="detail-head">
            <span class="face-badge is-front">内脏</span>
            <h3 class="detail-title">{{ selected.name }}</h3>
          </div>
          <p class="detail-sub">疾病详述与治疗</p>
          <div v-if="knowledgeLoading" class="knowledge-loading">
            <el-icon class="is-loading" :size="20"><Loading /></el-icon>
            <span>正在加载知识库…</span>
          </div>
          <template v-else-if="knowledgeBundle?.items?.length">
            <div
              v-for="(it, idx) in knowledgeBundle.items"
              :key="idx"
              class="knowledge-block"
            >
              <h4 class="knowledge-name">{{ it.diseaseName }}</h4>
              <p v-if="it.description" class="knowledge-desc">{{ it.description }}</p>
              <div v-if="it.treatmentMeasures" class="knowledge-treat">
                <span class="k-label">治疗措施</span>
                <ul class="treat-lines">
                  <li v-for="(line, j) in splitTreatmentLines(it.treatmentMeasures)" :key="j">
                    {{ line }}
                  </li>
                </ul>
              </div>
            </div>
          </template>
          <template v-else>
            <p v-if="knowledgeError" class="knowledge-fallback-hint">知识库暂不可用，以下为示意图标签示例。</p>
            <ul class="disease-list">
              <li v-for="(d, i) in selected.diseases" :key="i">
                <span class="dot" />
                {{ d }}
              </li>
            </ul>
          </template>
          <el-alert type="info" :closable="false" show-icon class="tip-alert">
            内容仅供教学示意；临床诊断与治疗请以医疗机构为准。
          </el-alert>
        </aside>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { Loading, Pointer } from '@element-plus/icons-vue'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getAdminOrganDiseases } from '@/api/index.js'
import { organDetailFromElement, processOrganSvg } from '@/utils/organSvg.js'
import { WM_PATH_FRONT, resolveWikimediaSvgUrl } from '@/utils/organUrls.js'

const loading = ref(true)
const frontHtml = ref('')
const errFront = ref(false)
const frontWrap = ref(null)
const selected = ref(null)

const knowledgeBundle = ref(null)
const knowledgeLoading = ref(false)
const knowledgeError = ref(false)

function splitTreatmentLines(text) {
  if (!text) return []
  return text
    .split(/\n+|；|;/)
    .map((s) => s.trim())
    .filter(Boolean)
}

async function loadKnowledge(sel) {
  if (!sel?.key) {
    knowledgeBundle.value = null
    knowledgeLoading.value = false
    knowledgeError.value = false
    return
  }
  knowledgeLoading.value = true
  knowledgeError.value = false
  try {
    const res = await getAdminOrganDiseases(sel.key)
    const bundle = res?.data ?? res
    knowledgeBundle.value =
      bundle && Array.isArray(bundle.items) ? bundle : { items: [] }
  } catch {
    knowledgeBundle.value = null
    knowledgeError.value = true
  } finally {
    knowledgeLoading.value = false
  }
}

function clearSelection() {
  selected.value = null
}

watch(
  selected,
  (v) => {
    document.body.style.overflow = v ? 'hidden' : ''
    if (v) loadKnowledge(v)
    else {
      knowledgeBundle.value = null
      knowledgeLoading.value = false
      knowledgeError.value = false
    }
  },
  { flush: 'sync' }
)

function onSvgClick(e) {
  const t = e.target.closest('text.organ-hit')
  if (!t) return
  const detail = organDetailFromElement(t)
  if (!detail) return
  const wrap = frontWrap.value
  if (!wrap) return
  const rect = wrap.getBoundingClientRect()
  if (rect.width <= 0) return
  const mid = rect.left + rect.width / 2
  const detailSide = e.clientX < mid ? 'left' : 'right'
  selected.value = { ...detail, detailSide }
}

let offFront = null

onMounted(async () => {
  try {
    await loadFront()
  } finally {
    loading.value = false
  }
  await nextTick()
  attachSvgClickHandlers()
})

onBeforeUnmount(() => {
  offFront?.()
  document.body.style.overflow = ''
})

function attachSvgClickHandlers() {
  offFront?.()
  if (frontWrap.value) {
    const fn = (e) => onSvgClick(e)
    frontWrap.value.addEventListener('click', fn)
    offFront = () => frontWrap.value?.removeEventListener('click', fn)
  }
}

async function loadFront() {
  errFront.value = false
  try {
    const txt = await loadOrganSvgText('organ-front.svg', WM_PATH_FRONT)
    const html = processOrganSvg(txt)
    if (!html?.trim()) throw new Error('empty svg html')
    frontHtml.value = html
  } catch (e) {
    console.error('[病机图谱] 内脏示意图加载失败', e)
    errFront.value = true
    frontHtml.value = ''
  }
}

function countSvgTextTags(svgText) {
  if (!svgText) return 0
  const m = svgText.match(/<text\b/gi)
  return m ? m.length : 0
}

async function loadOrganSvgText(localName, wmPath) {
  const base = import.meta.env.BASE_URL || '/'
  const localUrl = `${base}images/${localName}`.replace(/([^:]\/)\/+/g, '$1')
  let localTxt = null
  try {
    const res = await fetch(localUrl, { cache: 'no-store' })
    if (res.ok) {
      const txt = await res.text()
      if (txt && /<svg[\s>]/i.test(txt)) localTxt = txt
    }
  } catch {
    /* 本地不存在 */
  }
  if (localTxt && countSvgTextTags(localTxt) > 0) return localTxt
  try {
    const remote = resolveWikimediaSvgUrl(wmPath)
    return await fetchTextWithTimeout(remote, 30000)
  } catch (e) {
    if (localTxt) return localTxt
    throw e
  }
}

async function fetchTextWithTimeout(url, ms) {
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), ms)
  try {
    const res = await fetch(url, { signal: ctrl.signal, cache: 'no-store' })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const txt = await res.text()
    if (!txt || !/<svg[\s>]/i.test(txt)) throw new Error('响应不是有效 SVG')
    return txt
  } finally {
    clearTimeout(timer)
  }
}
</script>

<style scoped>
.page-body-map {
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
}

.page-head {
  padding: 18px 20px;
  margin-bottom: 18px;
}

.page-head h2 {
  margin: 6px 0 0;
  color: #f8fafc;
  font-size: 22px;
  letter-spacing: 1px;
}

.sub {
  margin: 0;
  color: #5eead4;
  font-size: 13px;
}

.desc {
  margin: 12px 0 0;
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.65;
  max-width: none;
}

.desc code {
  font-size: 12px;
  color: #99f6e4;
  background: rgba(15, 23, 42, 0.55);
  padding: 2px 6px;
  border-radius: 4px;
}

.panel {
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.88) 100%);
  border: 1px solid rgba(94, 234, 212, 0.14);
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(2, 6, 23, 0.35);
}

.content-panel {
  padding: 16px 20px 22px;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.float-in {
  animation: float-in 0.45s ease-out both;
}

@keyframes float-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chart-stage {
  width: 100%;
  max-width: 960px;
  margin-left: auto;
  margin-right: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  align-self: center;
}

.figure-block {
  margin: 0;
  padding: 12px;
  width: 100%;
  max-width: 960px;
  margin-left: auto;
  margin-right: auto;
  background: #f1f5f9;
  border-radius: 12px;
  border: 1px solid rgba(71, 85, 105, 0.25);
  min-height: 200px;
  box-sizing: border-box;
  overflow: visible;
}

.svg-host {
  width: 100%;
  max-width: 100%;
  overflow: visible;
  text-align: center;
}

.svg-host :deep(svg) {
  display: block;
  width: 100%;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
}

.svg-host :deep(text.organ-hit) {
  cursor: pointer !important;
}

.svg-host :deep(text.organ-hit:hover) {
  fill: #0d9488 !important;
}

.err-box {
  padding: 24px;
  font-size: 13px;
  color: #64748b;
  background: #fff;
  border-radius: 8px;
}

.err-box code {
  font-size: 12px;
  color: #0f766e;
}

.loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  width: 100%;
  max-width: 960px;
  padding: 48px 24px;
  color: #94a3b8;
  font-size: 14px;
  background: rgba(15, 23, 42, 0.35);
  border-radius: 12px;
  border: 1px dashed rgba(94, 234, 212, 0.25);
  box-sizing: border-box;
}

.loading-icon.is-loading {
  animation: spin-icon 0.9s linear infinite;
  color: #5eead4;
}

@keyframes spin-icon {
  to {
    transform: rotate(360deg);
  }
}

.svg-hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: #64748b;
  text-align: center;
}

.svg-hint--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* 浮层：挂到 body，需较高 z-index 盖住侧栏 */
.detail-overlay {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(15, 23, 42, 0.58);
  backdrop-filter: blur(3px);
}

.detail-panel--float {
  position: absolute;
  top: 50%;
  width: min(420px, calc(100vw - 32px));
  max-height: min(85vh, 880px);
  overflow-y: auto;
  margin: 0;
  padding: 20px;
  padding-top: 44px;
  background: rgba(15, 23, 42, 0.92);
  border: 1px solid rgba(94, 234, 212, 0.28);
  border-radius: 14px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.45);
}

.detail-panel--float.is-left {
  left: max(16px, env(safe-area-inset-left));
  transform: translateY(-50%);
}

.detail-panel--float.is-right {
  right: max(16px, env(safe-area-inset-right));
  transform: translateY(-50%);
}

.detail-close {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 8px;
  font-size: 22px;
  line-height: 1;
  color: #94a3b8;
  background: rgba(30, 41, 59, 0.8);
  cursor: pointer;
}

.detail-close:hover {
  color: #e2e8f0;
  background: rgba(51, 65, 85, 0.95);
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.face-badge {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 600;
  background: rgba(148, 163, 184, 0.2);
  color: #e2e8f0;
  border: 1px solid rgba(148, 163, 184, 0.35);
}

.face-badge.is-front {
  background: rgba(45, 212, 191, 0.15);
  border-color: rgba(45, 212, 191, 0.35);
  color: #99f6e4;
}

.detail-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #f1f5f9;
  letter-spacing: 2px;
}

.detail-sub {
  margin: 0 0 16px;
  font-size: 13px;
  color: #94a3b8;
}

.disease-list {
  margin: 0 0 16px;
  padding: 0;
  list-style: none;
}

.disease-list li {
  position: relative;
  padding: 10px 12px 10px 28px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #e2e8f0;
  line-height: 1.5;
  background: rgba(30, 41, 59, 0.55);
  border-radius: 10px;
  border: 1px solid rgba(71, 85, 105, 0.35);
}

.disease-list .dot {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #5eead4;
  opacity: 0.85;
}

.knowledge-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 13px;
  color: #94a3b8;
}

.knowledge-fallback-hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: #fbbf24;
}

.knowledge-block {
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(30, 41, 59, 0.65);
  border-radius: 10px;
  border: 1px solid rgba(71, 85, 105, 0.45);
}

.knowledge-name {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #5eead4;
  letter-spacing: 1px;
}

.knowledge-desc {
  margin: 0 0 10px;
  font-size: 13px;
  color: #cbd5e1;
  line-height: 1.6;
}

.knowledge-treat {
  font-size: 13px;
  color: #e2e8f0;
}

.knowledge-treat .k-label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: #94a3b8;
  font-size: 12px;
}

.treat-lines {
  margin: 0;
  padding-left: 18px;
  color: #e2e8f0;
  line-height: 1.55;
}

.treat-lines li {
  margin-bottom: 4px;
}

.tip-alert {
  --el-alert-bg-color: rgba(30, 41, 59, 0.6);
  --el-alert-border-color: rgba(94, 234, 212, 0.25);
  --el-alert-title-font-size: 12px;
  color: #94a3b8;
}
</style>
