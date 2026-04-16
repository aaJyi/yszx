<template>
  <el-popover
    placement="top-start"
    :width="440"
    trigger="hover"
    :show-after="180"
    popper-class="health-popover-dark"
    @show="onShow"
  >
    <template #reference>
      <span class="cell-ref" :class="{ empty: !preview }">
        {{ preview || '—' }}
        <span v-if="cell?.totalCount > 1" class="badge">{{ cell.totalCount }} 条</span>
      </span>
    </template>
    <div v-loading="loading" class="pop-body pop-body--dark">
      <div class="pop-title">{{ label }} · 全部记录</div>
      <template v-if="list.length">
        <ul class="pop-list">
          <li v-for="item in list" :key="item.id" class="pop-item">
            <div class="line1">
              <span class="fname">{{ item.fileName || '未命名文件' }}</span>
              <el-tag size="small" type="info" effect="dark">{{ item.formatType || '-' }}</el-tag>
            </div>
            <div class="line2">
              <span>ID: {{ item.id }}</span>
              <span>{{ formatTime(item.uploadTime) }}</span>
              <span v-if="item.fileSize != null">大小: {{ formatSize(item.fileSize) }}</span>
            </div>
          </li>
        </ul>
      </template>
      <div v-else class="pop-empty">暂无该类型记录</div>
    </div>
  </el-popover>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  userId: { type: Number, required: true },
  dataType: { type: String, required: true },
  label: { type: String, default: '' },
  cell: { type: Object, default: null },
  cache: { type: Object, required: true },
  loadRecords: { type: Function, required: true },
})

const loading = ref(false)

const cacheKey = computed(() => `${props.userId}_${props.dataType}`)

const preview = computed(() => {
  const latest = props.cell?.latest
  if (!latest) return ''
  const name = latest.fileName || '未命名'
  const t = formatTime(latest.uploadTime)
  return `${name} · ${t}`
})

const list = computed(() => props.cache[cacheKey.value] || [])

watch(
  () => props.cache[cacheKey.value],
  (v) => {
    if (v) loading.value = false
  }
)

async function onShow() {
  if (props.cache[cacheKey.value]) return
  loading.value = true
  try {
    await props.loadRecords(props.userId, props.dataType)
  } finally {
    loading.value = false
  }
}

function formatTime(v) {
  if (!v) return '—'
  const d = typeof v === 'string' ? new Date(v) : v
  if (Number.isNaN(d?.getTime?.())) return String(v)
  return d.toLocaleString('zh-CN', { hour12: false })
}

function formatSize(bytes) {
  if (bytes == null) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<style scoped>
.cell-ref {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  cursor: help;
  color: #cbd5e1;
  font-size: 13px;
  line-height: 1.35;
  border-bottom: 1px dashed rgba(94, 234, 212, 0.35);
  padding-bottom: 1px;
}

.cell-ref.empty {
  color: #64748b;
  border-bottom: none;
  cursor: default;
}

.badge {
  font-size: 11px;
  color: #5eead4;
  background: rgba(13, 148, 136, 0.25);
  padding: 1px 6px;
  border-radius: 999px;
  flex-shrink: 0;
}

.pop-body {
  min-height: 48px;
}

.pop-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}

.pop-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 280px;
  overflow-y: auto;
}

.pop-item {
  padding: 8px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.pop-item:last-child {
  border-bottom: none;
}

.line1 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.fname {
  font-size: 13px;
  word-break: break-all;
}

.line2 {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
}

.pop-empty {
  font-size: 13px;
}
</style>

<style>
/* 深色浮层：teleport 到 body，需非 scoped */
.health-popover-dark.el-popper {
  background: linear-gradient(155deg, #1e293b 0%, #0f172a 100%) !important;
  border: 1px solid rgba(94, 234, 212, 0.25) !important;
  border-radius: 12px !important;
  box-shadow: 0 16px 48px rgba(2, 6, 23, 0.65) !important;
}

.health-popover-dark .el-popper__arrow::before {
  background: #1e293b !important;
  border: 1px solid rgba(94, 234, 212, 0.2) !important;
}

.pop-body--dark .pop-title {
  color: #5eead4;
}

.pop-body--dark .fname {
  color: #f1f5f9;
}

.pop-body--dark .line2 {
  color: #94a3b8;
}

.pop-body--dark .pop-empty {
  color: #64748b;
}

.health-popover-dark .el-loading-mask {
  background-color: rgba(15, 23, 42, 0.65) !important;
}
</style>
