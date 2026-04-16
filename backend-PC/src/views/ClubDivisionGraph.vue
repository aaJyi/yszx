<template>
  <div class="page-club-static">
    <section class="screen-header panel-head">
      <div>
        <p class="sub-title">医视智行 · NCSS 社团划分</p>
        <h2>社团划分结果分析</h2>
      </div>
      <div class="header-right">
        <span class="time">{{ loadedAtText }}</span>
        <el-button type="primary" round :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="success" round plain :loading="recomputing" @click="recompute">重新计算</el-button>
      </div>
    </section>

    <div v-loading="loading" class="dashboard-stack">
      <section class="dash-cell panel-tile">
        <h4 class="tile-title">社团规模分布</h4>
        <p v-if="!emptyGraph" class="chart-scroll-hint">← 左右滑动查看全部社团</p>
        <div v-if="emptyGraph" class="charts-empty">暂无数据</div>
        <div v-show="!emptyGraph" class="chart-scroll-x">
          <div ref="chartBarRef" class="bar-chart-el" :style="chartBarStyle" />
        </div>
      </section>
      <section class="dash-cell panel-tile disease-table-tile">
        <h4 class="tile-title">疾病在各社团的分布</h4>
        <p v-if="heatmapReady" class="chart-scroll-hint">表格内可横向、纵向滚动；悬停数字查看该病在该社团的成员</p>
        <div v-if="!heatmapReady" class="charts-empty">暂无疾病分布数据</div>
        <div v-show="heatmapReady" class="chart-scroll-both disease-table-scroll">
          <table class="disease-matrix-table" border="0" cellspacing="0" cellpadding="0">
            <thead>
              <tr>
                <th class="sticky-col corner">疾病 \\ 社团</th>
                <th v-for="(colLabel, ci) in heatClubColLabels" :key="'hc-' + ci" class="num-head">{{ colLabel }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(label, di) in heatmapDiseaseLabels" :key="'dr-' + di">
                <th class="sticky-col disease-name">{{ label }}</th>
                <td
                  v-for="(cell, ci) in heatmapMatrix[di] || []"
                  :key="'c-' + di + '-' + ci"
                  class="heat-cell-wrap"
                  :style="heatCellStyle(cell, heatMax)"
                >
                  <el-tooltip
                    placement="top"
                    :show-after="200"
                    :disabled="Number(cell) <= 0"
                    effect="dark"
                  >
                    <template #content>
                      <div class="heat-tip-block">
                        <div v-for="(line, li) in heatCellLines(di, ci)" :key="li" class="heat-tip-line">
                          {{ line }}
                        </div>
                      </div>
                    </template>
                    <span class="heat-cell-num">{{ cell }}</span>
                  </el-tooltip>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
      <section class="dash-cell panel-tile stats-tile">
        <h4 class="tile-title">网络统计信息</h4>
        <div v-if="!dashboardStats" class="charts-empty">暂无数据</div>
        <pre v-else class="stats-box">{{ statsText }}</pre>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getAdminClubDivisionLatest, getAdminClubDivisionMembers, postAdminClubDivisionRecompute } from '@/api'

/** Canvas 默认字体不含中文时热力图纵轴会乱码，显式指定系统常见黑体 */
const CHART_FONT = '"Microsoft YaHei","PingFang SC","Noto Sans SC","Hiragino Sans GB",sans-serif'

const loading = ref(false)
const recomputing = ref(false)
const overview = ref(null)
const members = ref([])
const loadedAtText = ref('')

const chartBarRef = ref(null)
let chartBar = null

const emptyGraph = computed(() => !((overview.value?.graphNodes || []).length > 0))

const heatmapReady = computed(() => {
  const labels = overview.value?.heatmapDiseaseLabels
  const matrix = overview.value?.heatmapMatrix
  return Array.isArray(labels) && labels.length > 0 && Array.isArray(matrix) && matrix.length > 0
})

const dashboardStats = computed(() => overview.value?.dashboardStats || null)

const statsText = computed(() => {
  const d = dashboardStats.value
  if (!d) return ''
  return [
    `节点总数：${d.totalNodes ?? '-'}`,
    `边总数：${d.totalEdges ?? '-'}`,
    `社团总数：${d.totalClubs ?? '-'}`,
    `最大社团规模：${d.maxClubSize ?? '-'}`,
    `最小社团规模：${d.minClubSize ?? '-'}`,
    `平均社团规模：${d.avgClubSize ?? '-'}`,
    `图密度：${d.graphDensity ?? '-'}`,
  ].join('\n')
})

/** 柱状图高度与横向可滚动宽度 */
const CHART_H = 460

function clampPx(val, min, max) {
  return `${Math.max(min, Math.min(max, val))}px`
}

const chartBarStyle = computed(() => {
  const n = (overview.value?.clubSizes || []).length
  const w = 120 + n * 76
  /* 社团多时拉宽画布，由外层 chart-scroll-x 横向滚动；上限放宽避免柱被挤在一起 */
  return { minWidth: clampPx(w, 440, 12000), height: `${CHART_H}px` }
})

const heatmapDiseaseLabels = computed(() => overview.value?.heatmapDiseaseLabels || [])
const heatmapMatrix = computed(() => overview.value?.heatmapMatrix || [])
const heatClubCount = computed(() => {
  const m = heatmapMatrix.value
  if (!m.length) return 0
  return m[0]?.length || 0
})

const heatClubColLabels = computed(() => {
  const n = heatClubCount.value
  if (n <= 0) return []
  return Array.from({ length: n }, (_, i) => `社团${i + 1}`)
})

/** 按社团序号分组成员（供疾病分布格子弹窗） */
function membersForClubIndex(clubIdx) {
  const list = members.value || []
  return list
    .filter((m) => Number(m.clubIndex) === clubIdx)
    .sort((a, b) => (Number(a.userId) || 0) - (Number(b.userId) || 0))
}

function heatCellLines(di, ci) {
  const label = heatmapDiseaseLabels.value[di]
  const cell = heatmapMatrix.value[di]?.[ci]
  const n = Number(cell) || 0
  if (n <= 0) return []
  const disMap = overview.value?.heatmapUserDiseases || {}
  const rows = membersForClubIndex(ci).filter((m) => {
    const uid = String(m.userId)
    const diseases = disMap[uid]
    return Array.isArray(diseases) && diseases.includes(label)
  })
  const lines = [`疾病：${label}`, `社团：社团${ci + 1}`, `人数：${n}`, '成员：']
  if (!rows.length) {
    lines.push('（暂无明细，请刷新页面）')
    return lines
  }
  const cap = 40
  rows.slice(0, cap).forEach((r) => {
    lines.push(`${r.nickname || `用户${r.userId}`}（ID ${r.userId}）`)
  })
  if (rows.length > cap) lines.push(`…共 ${rows.length} 人`)
  return lines
}

const heatMax = computed(() => {
  const m = heatmapMatrix.value
  let maxV = 1
  for (const row of m) {
    if (!row) continue
    for (const v of row) {
      const n = Number(v) || 0
      if (n > maxV) maxV = n
    }
  }
  return maxV
})

function heatCellStyle(cell, maxV) {
  const v = Number(cell) || 0
  const m = maxV || 1
  const t = Math.min(1, v / m)
  const r = Math.round(254 + (127 - 254) * t)
  const g = Math.round(243 + (29 - 243) * t)
  const b = Math.round(199 + (29 - 199) * t)
  const color = v <= 0 ? 'rgba(15,23,42,0.35)' : `rgb(${r},${g},${b})`
  const fg = t > 0.55 ? '#f8fafc' : '#0f172a'
  return {
    background: color,
    color: fg,
  }
}

function buildBarOption() {
  const sizes = overview.value?.clubSizes || []
  const n = sizes.length
  const bottom = n > 14 ? 88 : n > 8 ? 68 : 48
  const data = sizes.map((v, i) => ({
    value: v,
    itemStyle: { color: ['#5470c6', '#c09050', '#91cc75', '#fac858', '#ee6666'][i % 5] },
  }))
  const xLabels = sizes.map((_, i) => `社团${i + 1}`)
  return {
    backgroundColor: 'transparent',
    animation: false,
    textStyle: { fontFamily: CHART_FONT },
    tooltip: { show: false },
    grid: { left: 52, right: 20, top: 28, bottom },
    xAxis: {
      type: 'category',
      data: xLabels,
      axisLabel: {
        color: '#cbd5e1',
        fontFamily: CHART_FONT,
        rotate: n > 6 ? 32 : 0,
        interval: 0,
        fontSize: n > 16 ? 10 : 11,
      },
      axisLine: { lineStyle: { color: '#475569' } },
    },
    yAxis: {
      type: 'value',
      name: '成员数量',
      nameTextStyle: { color: '#94a3b8', fontFamily: CHART_FONT },
      axisLabel: { color: '#cbd5e1', fontFamily: CHART_FONT },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.2)' } },
    },
    series: [{ type: 'bar', data, label: { show: true, position: 'top', color: '#e2e8f0', fontFamily: CHART_FONT } }],
  }
}

function resizeAll() {
  chartBar?.resize()
}

async function renderCharts() {
  await nextTick()
  if (emptyGraph.value) {
    chartBar?.clear()
    return
  }
  if (chartBarRef.value && !chartBar) chartBar = echarts.init(chartBarRef.value, null, { renderer: 'canvas' })
  chartBar?.setOption(buildBarOption(), true)
  await nextTick()
  await nextTick()
  resizeAll()
}

async function loadOverview() {
  loading.value = true
  try {
    const res = await getAdminClubDivisionLatest()
    const data = res?.data ?? res
    overview.value = data || null
    loadedAtText.value = new Date().toLocaleString('zh-CN', { hour12: false })
    await renderCharts()
  } catch (e) {
    overview.value = null
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMembers() {
  try {
    const res = await getAdminClubDivisionMembers()
    const data = res?.data ?? res
    members.value = Array.isArray(data) ? data : []
  } catch {
    members.value = []
  }
}

async function loadAll() {
  await Promise.all([loadOverview(), loadMembers()])
}

async function recompute() {
  recomputing.value = true
  try {
    await postAdminClubDivisionRecompute({ similarityThreshold: 0.45 })
    ElMessage.success('计算完成')
    await loadAll()
  } catch (e) {
    const msg = String(e?.message || '')
    if (msg.includes('timeout') || msg.includes('exceeded')) {
      ElMessage.warning('计算仍在后台执行（请求超时），请稍后点击「刷新」查看结果')
      return
    }
    ElMessage.error(e?.message || '计算失败')
  } finally {
    recomputing.value = false
  }
}

watch(
  () => [overview.value?.snapshotId, members.value.length],
  () => {
    renderCharts()
  }
)

onMounted(() => {
  loadAll()
  window.addEventListener('resize', resizeAll)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  chartBar?.dispose()
  chartBar = null
})
</script>

<style scoped>
.page-club-static {
  width: 100%;
  max-width: none;
  margin: 0 auto;
  min-height: 100vh;
  padding: 16px 16px 48px;
  background: linear-gradient(165deg, #0c4a6e 0%, #1e3a5f 35%, #172554 100%);
  box-sizing: border-box;
}

.panel-head {
  background: rgba(15, 23, 42, 0.45);
  border: 1px solid rgba(147, 197, 253, 0.25);
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.sub-title {
  font-size: 13px;
  color: rgba(186, 230, 253, 0.9);
  margin: 0 0 6px;
}

h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #f8fafc;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.time {
  font-size: 13px;
  color: rgba(186, 230, 253, 0.85);
}

.intro {
  font-size: 13px;
  color: rgba(186, 230, 253, 0.8);
  margin: 0 0 16px;
  line-height: 1.5;
}

.intro-hint {
  margin-top: -8px;
  margin-bottom: 16px;
  font-size: 12px;
  color: rgba(186, 230, 253, 0.72);
}

.dashboard-stack {
  display: flex;
  flex-direction: column;
  gap: 22px;
  width: 100%;
  margin-bottom: 20px;
}

.dash-cell {
  min-height: 0;
}

.chart-scroll-hint {
  margin: 0 0 8px;
  font-size: 12px;
  color: rgba(186, 230, 253, 0.55);
  letter-spacing: 0.02em;
}

.chart-scroll-x {
  overflow-x: auto;
  overflow-y: hidden;
  width: 100%;
  max-width: 100%;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.28);
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.5) rgba(15, 23, 42, 0.3);
}

.chart-scroll-x::-webkit-scrollbar {
  height: 10px;
}

.chart-scroll-x::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.35);
  border-radius: 5px;
}

.chart-scroll-x::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.45);
  border-radius: 5px;
}

/* 热力图：疾病行多时可纵向滚动，社团列多时可横向滚动 */
.chart-scroll-both {
  overflow: auto;
  max-height: min(880px, 85vh);
  width: 100%;
  max-width: 100%;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.28);
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
}

.panel-tile {
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(147, 197, 253, 0.2);
  border-radius: 12px;
  padding: 12px 14px 10px;
  box-sizing: border-box;
}

.tile-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #e0f2fe;
}

/* 柱状图：内联 style 的 minWidth 与 width:100% 取较大值，社团多时宽于视口则父级出现横向滚动 */
.bar-chart-el {
  display: block;
  width: 100%;
  box-sizing: border-box;
}

.stats-tile .stats-box {
  margin: 8px 0 0;
  padding: 18px 20px;
  min-height: 160px;
  background: rgba(254, 243, 199, 0.12);
  border: 1px solid rgba(251, 191, 36, 0.25);
  border-radius: 10px;
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  font-family: 'Microsoft YaHei', 'PingFang SC', ui-monospace, monospace;
}

.charts-empty {
  padding: 40px 16px;
  text-align: center;
  color: rgba(186, 230, 253, 0.65);
  font-size: 13px;
}

.disease-table-scroll {
  max-height: min(720px, 80vh);
}

.disease-matrix-table {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Noto Sans SC', sans-serif;
  color: #e2e8f0;
}

.disease-matrix-table th,
.disease-matrix-table td {
  border: 1px solid rgba(147, 197, 253, 0.18);
  padding: 8px 10px;
  text-align: center;
  white-space: nowrap;
}

.disease-matrix-table .sticky-col {
  position: sticky;
  left: 0;
  z-index: 2;
  background: rgba(15, 23, 42, 0.98);
  text-align: left;
  min-width: 140px;
  max-width: 280px;
  white-space: normal;
  word-break: break-word;
}

.disease-matrix-table thead .sticky-col {
  z-index: 3;
}

.disease-matrix-table .corner {
  min-width: 120px;
}

.disease-matrix-table .disease-name {
  font-weight: 500;
  color: #e0f2fe;
}

.disease-matrix-table .num-head {
  min-width: 72px;
  background: rgba(30, 41, 59, 0.85);
}

.disease-matrix-table .heat-cell-wrap {
  font-variant-numeric: tabular-nums;
  min-width: 56px;
  padding: 6px 8px;
}

.disease-matrix-table .heat-cell-num {
  display: inline-block;
  min-width: 1.5em;
  cursor: default;
}

.heat-tip-block {
  max-width: 340px;
  max-height: min(380px, 52vh);
  overflow-y: auto;
  text-align: left;
  line-height: 1.55;
  font-size: 12px;
}

.heat-tip-line {
  white-space: normal;
  word-break: break-word;
}
</style>
