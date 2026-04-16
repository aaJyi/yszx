<template>
  <div class="page-data">
    <header class="page-head panel float-in">
      <div>
        <p class="sub">医视智行 · 健康数据中心</p>
        <h2>健康数据</h2>
      </div>
    </header>

    <section class="panel content-panel float-in">
      <div class="toolbar">
        <el-button type="primary" round :loading="loading" @click="loadHealth">刷新</el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        class="data-table health-table"
        empty-text="暂无健康数据"
        :row-class-name="rowClassName"
      >
        <el-table-column type="index" label="序号" width="64" :index="indexMethod" />
        <el-table-column label="openid" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="openid">{{ row.openid || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="姓名" min-width="110" />
        <el-table-column label="体检报告" min-width="200">
          <template #default="{ row }">
            <HealthCell
              :user-id="row.userId"
              data-type="REPORT"
              label="体检报告"
              :cell="row.report"
              :cache="recordCache"
              :load-records="fetchRecords"
            />
          </template>
        </el-table-column>
        <el-table-column label="就诊记录" min-width="200">
          <template #default="{ row }">
            <HealthCell
              :user-id="row.userId"
              data-type="MEDICAL_RECORD"
              label="就诊记录"
              :cell="row.medicalRecord"
              :cache="recordCache"
              :load-records="fetchRecords"
            />
          </template>
        </el-table-column>
        <el-table-column label="皮肤信息" min-width="200">
          <template #default="{ row }">
            <HealthCell
              :user-id="row.userId"
              data-type="SKIN"
              label="皮肤信息"
              :cell="row.skin"
              :cache="recordCache"
              :load-records="fetchRecords"
            />
          </template>
        </el-table-column>
        <el-table-column label="检查信息" min-width="200">
          <template #default="{ row }">
            <HealthCell
              :user-id="row.userId"
              data-type="LAB"
              label="检查信息"
              :cell="row.lab"
              :cache="recordCache"
              :load-records="fetchRecords"
            />
          </template>
        </el-table-column>
        <el-table-column label="三餐信息" min-width="200">
          <template #default="{ row }">
            <HealthCell
              :user-id="row.userId"
              data-type="MEAL"
              label="三餐信息"
              :cell="row.meal"
              :cache="recordCache"
              :load-records="fetchRecords"
            />
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadHealth"
          @size-change="loadHealth"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getAdminHealthOverview, getAdminHealthRecords } from '@/api'
import HealthCell from '@/components/HealthDataCell.vue'

const route = useRoute()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const recordCache = ref({})
/** 从「用户信息」跳转时高亮对应行 */
const highlightUserId = ref(null)

function indexMethod(index) {
  return (pageNum.value - 1) * pageSize.value + index + 1
}

function rowClassName({ row }) {
  if (highlightUserId.value == null) return ''
  return String(row.userId) === String(highlightUserId.value) ? 'health-row-highlight' : ''
}

async function loadHealth() {
  recordCache.value = {}
  loading.value = true
  try {
    const res = await getAdminHealthOverview({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    const data = res?.data ?? res
    rows.value = data?.records ?? []
    total.value = data?.total ?? 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function syncHighlightFromRoute() {
  const q = route.query.userId
  highlightUserId.value = q != null && q !== '' ? String(q) : null
}

async function fetchRecords(userId, dataType) {
  const key = `${userId}_${dataType}`
  if (recordCache.value[key]) return
  const res = await getAdminHealthRecords({ userId, dataType })
  const list = res?.data ?? res ?? []
  recordCache.value = { ...recordCache.value, [key]: Array.isArray(list) ? list : [] }
}

onMounted(() => {
  syncHighlightFromRoute()
  loadHealth()
})

watch(
  () => route.query.userId,
  () => {
    syncHighlightFromRoute()
  }
)
</script>

<style scoped>
.page-data {
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
  max-width: 900px;
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
  margin-bottom: 12px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.data-table {
  --el-table-bg-color: rgba(15, 23, 42, 0.35);
  --el-table-tr-bg-color: rgba(15, 23, 42, 0.25);
  --el-table-header-bg-color: rgba(13, 148, 136, 0.2);
  --el-table-border-color: rgba(71, 85, 105, 0.45);
  --el-table-text-color: #e2e8f0;
  --el-table-header-text-color: #f1f5f9;
  --el-table-row-hover-bg-color: rgba(45, 212, 191, 0.12);
  --el-table-current-row-bg-color: rgba(45, 212, 191, 0.18);
}

.data-table :deep(.el-table__body tr.el-table__row--striped > td.el-table__cell) {
  background-color: rgba(30, 41, 59, 0.45);
}

.data-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.health-table {
  width: 100%;
}

.openid {
  font-size: 12px;
  color: #bae6fd;
  word-break: break-all;
}

.health-table :deep(tr.health-row-highlight > td.el-table__cell) {
  background-color: rgba(45, 212, 191, 0.22) !important;
  box-shadow: inset 0 0 0 1px rgba(94, 234, 212, 0.45);
}
</style>
