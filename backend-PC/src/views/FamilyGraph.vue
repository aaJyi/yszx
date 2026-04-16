<template>
  <div class="page-family-graph">
    <header class="page-head panel float-in">
      <div>
        <p class="sub">医视智行 · 关系视图</p>
        <h2>家人图谱</h2>
      </div>
    </header>

    <section class="panel content-panel float-in">
      <div class="toolbar">
        <el-button type="primary" round :loading="loading" @click="loadGraph">刷新</el-button>
        <span v-if="statsText" class="stats">{{ statsText }}</span>
      </div>
      <div v-if="!loading && empty" class="empty-wrap">
        <el-empty description="暂无家人关系数据" />
      </div>
      <div v-show="!empty || loading" ref="chartRef" class="chart-host" />
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getAdminFamilyGraph } from '@/api'

const loading = ref(false)
const chartRef = ref(null)
const nodes = ref([])
const links = ref([])
let chart = null

const empty = computed(() => !nodes.value.length)

const statsText = computed(() => {
  const n = nodes.value.length
  const e = links.value.length
  if (!n && !e) return ''
  return `共 ${n} 个节点、${e} 条关系`
})

function buildOption() {
  const nodeData = nodes.value.map((n) => ({
    id: n.id,
    name: n.name,
    category: n.category,
    symbolSize: n.category === 0 ? 44 : 36,
    itemStyle:
      n.category === 0
        ? { color: '#2dd4bf', borderColor: 'rgba(255,255,255,0.2)', borderWidth: 1 }
        : { color: '#a78bfa', borderColor: 'rgba(255,255,255,0.2)', borderWidth: 1 },
  }))

  const linkData = links.value.map((l) => ({
    source: l.source,
    target: l.target,
    relation: l.relation,
    value: 1,
    lineStyle: { width: 1.2, curveness: 0.12, color: 'rgba(94, 234, 212, 0.42)' },
  }))

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.94)',
      borderColor: 'rgba(94, 234, 212, 0.35)',
      textStyle: { color: '#e2e8f0' },
      formatter: (p) => {
        if (p.dataType === 'edge') {
          const rel = p.data?.relation || ''
          return rel ? `关系：${rel}` : ''
        }
        const cat = p.data?.category === 0 ? '注册用户' : '家人(未注册)'
        return `${p.name}<br/><span style="opacity:.85">${cat}</span>`
      },
    },
    legend: {
      show: true,
      bottom: 8,
      textStyle: { color: '#94a3b8' },
      data: ['注册用户', '家人(未注册)'],
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        animation: true,
        data: nodeData,
        links: linkData,
        categories: [
          { name: '注册用户' },
          { name: '家人(未注册)' },
        ],
        roam: true,
        draggable: true,
        label: {
          show: true,
          position: 'right',
          color: '#e2e8f0',
          fontSize: 12,
          distance: 6,
        },
        labelLayout: { hideOverlap: true },
        lineStyle: { opacity: 0.85 },
        emphasis: {
          focus: 'adjacency',
          blurScope: 'coordinateSystem',
          lineStyle: { width: 3, color: '#5eead4' },
          itemStyle: { shadowBlur: 12, shadowColor: 'rgba(94, 234, 212, 0.45)' },
        },
        edgeSymbol: ['none', 'none'],
        edgeLabel: {
          show: true,
          color: '#94a3b8',
          fontSize: 11,
          formatter: (params) => params?.data?.relation || '',
        },
        force: {
          repulsion: 520,
          gravity: 0.06,
          edgeLength: [80, 220],
          friction: 0.86,
          layoutAnimation: true,
        },
      },
    ],
  }
}

function resize() {
  chart?.resize()
}

async function loadGraph() {
  loading.value = true
  try {
    const res = await getAdminFamilyGraph()
    const data = res?.data ?? res
    nodes.value = Array.isArray(data?.nodes) ? data.nodes : []
    links.value = Array.isArray(data?.links) ? data.links : []
    await nextTick()
    if (!chartRef.value) return
    if (!chart) {
      chart = echarts.init(chartRef.value, null, { renderer: 'canvas' })
    }
    if (empty.value) {
      chart.clear()
    } else {
      chart.setOption(buildOption(), true)
    }
    resize()
  } catch {
    nodes.value = []
    links.value = []
    chart?.clear()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadGraph()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.page-family-graph {
  max-width: 1600px;
  margin: 0 auto;
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
  max-width: 960px;
}

.desc code {
  font-size: 12px;
  color: #99f6e4;
  background: rgba(15, 23, 42, 0.6);
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
  padding: 16px 18px 20px;
  min-height: 560px;
  display: flex;
  flex-direction: column;
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.stats {
  font-size: 13px;
  color: #94a3b8;
}

.chart-host {
  flex: 1;
  width: 100%;
  min-height: 520px;
  border-radius: 12px;
  background: radial-gradient(ellipse at 50% 35%, rgba(13, 148, 136, 0.12) 0%, rgba(15, 23, 42, 0.35) 55%, rgba(2, 6, 23, 0.25) 100%);
  border: 1px solid rgba(71, 85, 105, 0.35);
}

.empty-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 480px;
}

.empty-wrap :deep(.el-empty__description) {
  color: #94a3b8;
}
</style>
